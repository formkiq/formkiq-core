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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3MultipartUploader;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;

/**
 * Class to Compress Document Contents into a zip file.
 */
public class DocumentCompressor {
  /**
   * Used to determine whether to use S3 multipart upload.
   */
  private static final long MAX_IN_MEMORY_CHUNK_SIZE = 64 * 1024 * 1024;
  /**
   * To get documents S3 object keys.
   */
  private final DocumentService documentService;
  /**
   * Used to determine whether to use S3 multipart upload.
   */
  private final long maxInMemoryChunkSize;
  /**
   * For chunked upload of large files.
   */
  private final S3MultipartUploader multipartUploader;
  /**
   * S3 Service.
   */
  private final S3Service s3;

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public DocumentCompressor(final AwsServiceCache serviceCache) {

    this.s3 = serviceCache.getExtension(S3Service.class);
    this.multipartUploader =
        new S3MultipartUploader(serviceCache.getExtension(S3ConnectionBuilder.class));
    this.documentService = serviceCache.getExtension(DocumentService.class);
    this.maxInMemoryChunkSize = MAX_IN_MEMORY_CHUNK_SIZE;
  }

  private void archiveS3Objects(final String siteId, final String docsBucket,
      final String archiveBucket, final String archiveKey,
      final Map<DocumentItem, Long> documentSizeMap) throws IOException {

    Long totalFilesSize = documentSizeMap.values().stream().reduce(Long.valueOf(0), Long::sum);
    boolean isMultiPartUpload = totalFilesSize.longValue() > this.maxInMemoryChunkSize;

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    String multipartUploadId =
        isMultiPartUpload ? this.multipartUploader.initializeUpload(archiveBucket, archiveKey)
            : null;

    ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

    for (Map.Entry<DocumentItem, Long> docSizePair : documentSizeMap.entrySet()) {
      DocumentItem document = docSizePair.getKey();
      Long objectSize = docSizePair.getValue();
      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, document.getDocumentId());
      ZipEntry zipEntry = new ZipEntry(document.getPath());

      zipOutputStream.putNextEntry(zipEntry);

      if (isMultiPartUpload) {
        transferObjectToZipInChunks(zipOutputStream, docsBucket, s3Key, byteArrayOutputStream,
            multipartUploadId, objectSize);
      } else {
        transferObjectToZip(zipOutputStream, docsBucket, s3Key);
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

  /**
   * Compress Documents.
   * 
   * @param siteId {@link String}
   * @param docsBucket {@link String}
   * @param archiveBucket {@link String}
   * @param archiveKey {@link String}
   * @param documentIds {@link List} {@link String}
   * @throws IOException IOException
   */
  public void compressDocuments(final String siteId, final String docsBucket,
      final String archiveBucket, final String archiveKey, final List<String> documentIds)
      throws IOException {

    Map<DocumentItem, Long> documentContentSize =
        getDocumentContentSizeMap(siteId, docsBucket, documentIds);

    archiveS3Objects(siteId, docsBucket, archiveBucket, archiveKey, documentContentSize);
  }

  private Map<DocumentItem, Long> getDocumentContentSizeMap(final String siteId,
      final String bucket, final List<String> documentIds) {

    List<DocumentItem> documents = this.documentService.findDocuments(siteId, documentIds);

    return documents.stream().collect(Collectors.toMap(item -> item,
        item -> this.s3.getObjectMetadata(bucket,
            SiteIdKeyGenerator.createS3Key(siteId, item.getDocumentId()), null)
            .getContentLength()));
  }

  private void transferObjectToZip(final ZipOutputStream outputStream, final String bucket,
      final String key) throws IOException {
    try (InputStream content = this.s3.getContentAsInputStream(bucket, key)) {
      content.transferTo(outputStream);
    }
  }

  private void transferObjectToZipInChunks(final ZipOutputStream zipOutputStream,
      final String bucket, final String key, final ByteArrayOutputStream byteOutputStream,
      final String uploadId, final Long objectSize) throws IOException {

    final long maxChunkSize = this.maxInMemoryChunkSize;
    final long lastByte = objectSize.longValue() - 1;
    long start = 0L;
    long end = Math.min(start + maxChunkSize, lastByte);
    for (; start <= lastByte; start += maxChunkSize + 1, end =
        Math.min(start + maxChunkSize, lastByte)) {

      String rangeHeader = String.format("bytes=%d-%d", Long.valueOf(start), Long.valueOf(end));

      try (InputStream chunk = this.s3.getContentPartAsInputStream(bucket, key, rangeHeader)) {

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
  }

  private void uploadChunkIfNeeded(final String uploadId, final ByteArrayOutputStream outputStream,
      final boolean isFinal) {
    // The minimum S3 part size for non-final chunk is 5MB
    final long minS3MultipartUploadChunkSize = 5 * 1024 * 1024;
    if (isFinal || outputStream.size() > minS3MultipartUploadChunkSize) {
      this.multipartUploader.uploadChunk(uploadId, outputStream.toByteArray());
      outputStream.reset();
    }
  }
}
