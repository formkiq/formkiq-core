/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.stacks.lambda.s3;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3MultipartUploader;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DocumentCompressor {
  /**
   * Used to determine whether to use S3 multipart upload.
   */
  private static final long MAX_IN_MEMORY_CHUNK_SIZE = 64 * 1024 * 1024;
  /**
   * Used to determine whether to use S3 multipart upload.
   */
  private final long maxInMemoryChunkSize;
  /**
   * S3 Service.
   */
  private final S3Service s3;
  /**
   * For chunked upload of large files.
   */
  private final S3MultipartUploader multipartUploader;
  /**
   * To get documents S3 object keys.
   */
  private final DocumentService documentService;

  public DocumentCompressor(final Map<String, String> envVars, final S3ConnectionBuilder s3Builder,
      final DynamoDbConnectionBuilder dbBuilder, final Long maxChunkSize) {
    this.s3 = new S3Service(s3Builder);
    this.multipartUploader = new S3MultipartUploader(s3Builder);
    final String documentsTable = envVars.get("DOCUMENTS_TABLE");
    AwsServiceCache serviceCache = new AwsServiceCache().environment(envVars);
    AwsServiceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(dbBuilder));
    DocumentVersionServiceExtension dsExtension = new DocumentVersionServiceExtension();
    DocumentVersionService versionService = dsExtension.loadService(serviceCache);
    this.documentService = new DocumentServiceImpl(dbBuilder, documentsTable, versionService);
    this.maxInMemoryChunkSize = maxChunkSize != null ? maxChunkSize : MAX_IN_MEMORY_CHUNK_SIZE;
  }

  public void compressDocuments(final String siteId, final String docsBucket,
      final String archiveBucket, final String archiveKey, final List<String> documentIds)
      throws Exception {
    final Map<DocumentItem, Long> documentContentSize =
        this.getDocumentContentSizeMap(siteId, docsBucket, documentIds);

    this.archiveS3Objects(siteId, docsBucket, archiveBucket, archiveKey, documentContentSize);
  }

  private Map<DocumentItem, Long> getDocumentContentSizeMap(final String siteId,
      final String bucket, final List<String> documentIds) throws Exception {
    final List<DocumentItem> documents = this.documentService.findDocuments(siteId, documentIds);

    if (documents == null) {
      throw new Exception("Can't find documents");
    } else if (documentIds.size() != documents.size()) {
      final List<String> missingDocs = documentIds.stream()
          .filter(docId -> documents.stream().noneMatch(item -> item.getDocumentId().equals(docId)))
          .collect(Collectors.toList());
      final String missingDocsStr =
          missingDocs.stream().map(Object::toString).collect(Collectors.joining(", "));
      throw new Exception(String.format("Can't find documents: %s", missingDocsStr));
    }

    return documents.stream().collect(Collectors.toMap(item -> item,
        item -> this.s3.getObjectMetadata(bucket,
            SiteIdKeyGenerator.createS3Key(siteId, item.getDocumentId()), null)
            .getContentLength()));
  }

  private void archiveS3Objects(final String siteId, final String docsBucket,
      final String archiveBucket, final String archiveKey,
      final Map<DocumentItem, Long> documentSizeMap) throws IOException {
    String multipartUploadId = null;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream;
    final Long totalFilesSize = documentSizeMap.values().stream().reduce(0L, Long::sum);
    final boolean isMultiPartUpload = totalFilesSize > this.maxInMemoryChunkSize;

    if (isMultiPartUpload) {
      multipartUploadId = this.multipartUploader.initializeUpload(archiveBucket, archiveKey);
    }

    zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

    for (Map.Entry<DocumentItem, Long> docSizePair : documentSizeMap.entrySet()) {
      final DocumentItem document = docSizePair.getKey();
      final Long objectSize = docSizePair.getValue();
      final String s3Key = SiteIdKeyGenerator.createS3Key(siteId, document.getDocumentId());
      final ZipEntry zipEntry = new ZipEntry(document.getPath());

      zipOutputStream.putNextEntry(zipEntry);
      if (isMultiPartUpload) {
        this.transferObjectToZipInChunks(zipOutputStream, docsBucket, s3Key, byteArrayOutputStream,
            multipartUploadId, objectSize);
      } else {
        this.transferObjectToZip(zipOutputStream, docsBucket, s3Key);
      }

      zipOutputStream.closeEntry();
    }

    zipOutputStream.close();
    if (!isMultiPartUpload) {
      this.s3.putObject(archiveBucket, archiveKey, byteArrayOutputStream.toByteArray(),
          "application/zip");
    } else {
      zipOutputStream.flush();
      this.uploadChunkIfNeeded(multipartUploadId, byteArrayOutputStream, true);
      this.multipartUploader.completeUpload(multipartUploadId);
    }
  }

  private void transferObjectToZip(final ZipOutputStream outputStream, final String bucket,
      final String key) throws IOException {
    final InputStream content = this.s3.getContentAsInputStream(bucket, key);
    try {
      content.transferTo(outputStream);
    } catch (IOException e) {
      content.close();
      throw e;
    }
  }

  private void transferObjectToZipInChunks(final ZipOutputStream zipOutputStream,
      final String bucket, final String key, final ByteArrayOutputStream byteOutputStream,
      final String uploadId, final Long objectSize) throws IOException {
    final long maxChunkSize = this.maxInMemoryChunkSize;
    final long lastByte = objectSize - 1;
    long start = 0L;
    long end = Math.min(start + maxChunkSize, lastByte);
    for (; start <= lastByte; start += maxChunkSize + 1, end =
        Math.min(start + maxChunkSize, lastByte)) {
      final String rangeHeader = String.format("bytes=%d-%d", start, end);
      final InputStream chunk = this.s3.getContentPartAsInputStream(bucket, key, rangeHeader);
      try {
        chunk.transferTo(zipOutputStream);
        zipOutputStream.flush();
        this.uploadChunkIfNeeded(uploadId, byteOutputStream, false);
      } catch (IOException e) {
        this.multipartUploader.abortMultipartUpload(uploadId);
        chunk.close();
        throw e;
      }
    }
  }

  private void uploadChunkIfNeeded(final String uploadId, final ByteArrayOutputStream outputStream,
      final boolean isFinal) throws IOException {
    // The minimum S3 part size for non-final chunk is 5MB
    final long minS3MultipartUploadChunkSize = 5 * 1024 * 1024;
    if (isFinal || outputStream.size() > minS3MultipartUploadChunkSize) {
      this.multipartUploader.uploadChunk(uploadId, outputStream.toByteArray());
      outputStream.reset();
    }
  }
}
