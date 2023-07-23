/**
 * MIT License
 * <p>
 * Copyright (c) 2018 - 2020 FormKiQ
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.stacks.lambda.s3;

import alex.mojaki.s3upload.MultiPartOutputStream;
import alex.mojaki.s3upload.StreamTransferManager;
import com.amazonaws.services.s3.AmazonS3;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DocumentCompressor {
    private final static long MAX_IN_MEMORY_FILE_SIZE_50MB = 50 * 1024 * 1024;
    private final S3Service s3;
    private final DocumentService documentService;

    public DocumentCompressor(final Map<String, String> envVars, final S3ConnectionBuilder s3Builder, final DynamoDbConnectionBuilder dbBuilder) {
        this.s3 = new S3Service(s3Builder);
        final String documentsTable = envVars.get("DOCUMENTS_TABLE");
        AwsServiceCache serviceCache = new AwsServiceCache().environment(envVars);
        AwsServiceCache.register(DynamoDbConnectionBuilder.class, new DynamoDbConnectionBuilderExtension(dbBuilder));
        DocumentVersionServiceExtension dsExtension = new DocumentVersionServiceExtension();
        DocumentVersionService versionService = dsExtension.loadService(serviceCache);
        this.documentService = new DocumentServiceImpl(dbBuilder, documentsTable, versionService);
    }

    public void compressDocuments(final String siteId, final String srcBucket, final String destBucket, final String archiveKey, final List<String> documentIds) throws Exception {
        final Map<String, String> documentIdObjectKeyMap = this.getDocumentObjectKeys(siteId, documentIds);
        if (documentIds.size() != documentIdObjectKeyMap.size()) {
            // TODO: log missing docs
            final List<String> missingDocuments = documentIds.stream().filter(docId -> !documentIdObjectKeyMap.containsKey(docId)).collect(Collectors.toList());
            throw new Exception("Can't find all documents");
        }

        final ArrayList<String> objectKeys = new ArrayList<>(documentIdObjectKeyMap.values());
        final Map<String, Long> objectKeySizeMap = this.getObjectKeySizeMap(srcBucket, objectKeys);
        this.archiveS3Objects(destBucket, siteId, archiveKey, objectKeySizeMap);
    }

    private Map<String, String> getDocumentObjectKeys(final String siteId, final List<String> documentIds) {
        final List<DocumentItem> documents = this.documentService.findDocuments(siteId, documentIds);
        return documents.stream().collect(Collectors.toMap(DocumentItem::getDocumentId, DocumentItem::getPath));
    }

    private Map<String, Long> getObjectKeySizeMap(final String bucket, final ArrayList<String> keys) {
        return keys.stream().collect(Collectors.toMap(String::new, key -> this.s3.getObjectMetadata(bucket, key, null).getContentLength()));
    }

    private void archiveS3Objects(final String bucket, final String siteId, final String archiveKey, Map<String, Long> objectKeySizeMap) throws IOException {
        MultiPartOutputStream s3OutputStream;
        ByteArrayOutputStream byteArrayOutputStream;
        ZipOutputStream zipOutputStream;
        final Long totalFilesSize = objectKeySizeMap.values().stream().reduce(0L, Long::sum);
        final boolean isMultiPartUpload = totalFilesSize > MAX_IN_MEMORY_FILE_SIZE_50MB;

        if (isMultiPartUpload) {
            // TODO: Provide with S3 client
            s3OutputStream = this.getS3MultiPartUploadStream(bucket, archiveKey, null);
            zipOutputStream = new ZipOutputStream(s3OutputStream);
        } else {
            byteArrayOutputStream = new ByteArrayOutputStream();
            zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        }

        objectKeySizeMap.forEach((objectKey, objectSize) -> {
            final String fileName = Strings.getFilename(objectKey);
            final ZipEntry zipEntry = new ZipEntry(fileName);

            try {
                zipOutputStream.putNextEntry(zipEntry);
                if (isMultiPartUpload) {
                    this.transferObjectToZipByChunks(zipOutputStream, bucket, objectKey, objectSize);
                } else {
                    this.transferObjectToZip(zipOutputStream, bucket, objectKey);
                }
                zipOutputStream.closeEntry();
            } catch (IOException e) {
                // TODO: Close streams?
            }
        });
    }

    private MultiPartOutputStream getS3MultiPartUploadStream(final String bucket, final String key, final AmazonS3 client) {
        final StreamTransferManager manager = new StreamTransferManager(bucket, key, client);
        return manager.getMultiPartOutputStreams().get(0);
    }

    private void transferObjectToZipByChunks(final ZipOutputStream outputStream, final String bucket, final String key, final Long objectSize) throws IOException {
        final long lastByte = objectSize - 1;
        long start = 0L;
        long end = Math.min(start + MAX_IN_MEMORY_FILE_SIZE_50MB, lastByte);
        for (; end < lastByte; start += MAX_IN_MEMORY_FILE_SIZE_50MB, end = Math.min(start + MAX_IN_MEMORY_FILE_SIZE_50MB, lastByte)) {
            final String rangeHeader = String.format("bytes=%d-%d", start, end);
            final InputStream chunk = this.s3.getContentPartAsInputStream(bucket, key, rangeHeader);
            try {
                chunk.transferTo(outputStream);
            } catch (IOException e) {
                chunk.close();
                throw e;
            }
        }
    }

    private void transferObjectToZip(final ZipOutputStream outputStream, final String bucket, final String key) throws IOException {
        final InputStream content = this.s3.getContentAsInputStream(bucket, key);
        content.transferTo(outputStream);
    }
}
