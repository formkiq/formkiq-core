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
package com.formkiq.aws.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * S3 Multipart Uploader.
 */
public class S3MultipartUploader {
  /**
   * S3 client.
   */
  private final S3Client s3;
  /**
   * Maps uploadId to its uploaded parts.
   */
  private final HashMap<String, Collection<CompletedPart>> uploadIdCompletedParts;
  /**
   * Maps uploadId to its request metadata (bucket, object key etc.).
   */
  private final HashMap<String, CreateMultipartUploadResponse> uploadIdMetadata;
  /**
   * Maps uploadId to its recently uploaded part number.
   */
  private final HashMap<String, Integer> uploadIdPartNumber;

  /**
   * constructor.
   * 
   * @param builder {@link S3ConnectionBuilder}
   */
  public S3MultipartUploader(final S3ConnectionBuilder builder) {
    this.s3 = builder.build();
    this.uploadIdCompletedParts = new HashMap<>();
    this.uploadIdMetadata = new HashMap<>();
    this.uploadIdPartNumber = new HashMap<>();
  }

  /**
   * Initialize Multipart upload.
   * 
   * @param bucket {@link String}
   * @param key {@link String}
   * @return {@link String}
   */
  public String initializeUpload(final String bucket, final String key) {
    CreateMultipartUploadRequest uploadRequest =
        CreateMultipartUploadRequest.builder().bucket(bucket).key(key).build();
    final CreateMultipartUploadResponse uploadMetadata =
        this.s3.createMultipartUpload(uploadRequest);
    final String uploadId = uploadMetadata.uploadId();
    this.uploadIdMetadata.put(uploadId, uploadMetadata);
    this.uploadIdCompletedParts.put(uploadId, new ArrayList<>());
    this.uploadIdPartNumber.put(uploadId, Integer.valueOf(1));
    return uploadId;
  }

  /**
   * Upload Chunk.
   * 
   * @param uploadId {@link String}
   * @param chunk byte[]
   */
  public void uploadChunk(final String uploadId, final byte[] chunk) {
    final CreateMultipartUploadResponse metadata = this.uploadIdMetadata.get(uploadId);
    final String bucketName = metadata.bucket();
    final String objectKey = metadata.key();

    try {
      int partNumber = this.uploadIdPartNumber.get(uploadId).intValue();
      UploadPartRequest uploadPartRequest = UploadPartRequest.builder().bucket(bucketName)
          .key(objectKey).uploadId(uploadId).partNumber(Integer.valueOf(partNumber))
          .contentLength(Long.valueOf(chunk.length)).build();

      UploadPartResponse uploadPartResponse =
          this.s3.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunk));

      this.uploadIdCompletedParts.get(uploadId).add(CompletedPart.builder()
          .partNumber(Integer.valueOf(partNumber)).eTag(uploadPartResponse.eTag()).build());
      this.uploadIdPartNumber.put(uploadId, Integer.valueOf(++partNumber));
    } catch (Exception e) {
      this.abortMultipartUpload(uploadId);
      throw e;
    }
  }

  /**
   * Complete Upload.
   * 
   * @param uploadId {@link String}
   */
  public void completeUpload(final String uploadId) {
    final CreateMultipartUploadResponse metadata = this.uploadIdMetadata.get(uploadId);
    final String bucketName = metadata.bucket();
    final String objectKey = metadata.key();
    final Collection<CompletedPart> completedParts = this.uploadIdCompletedParts.get(uploadId);

    CompletedMultipartUpload completedMultipartUpload =
        CompletedMultipartUpload.builder().parts(completedParts).build();
    CompleteMultipartUploadRequest completeMultipartUploadRequest =
        CompleteMultipartUploadRequest.builder().bucket(bucketName).key(objectKey)
            .uploadId(uploadId).multipartUpload(completedMultipartUpload).build();
    this.s3.completeMultipartUpload(completeMultipartUploadRequest);
  }

  /**
   * Abort Multipart Upload.
   * 
   * @param uploadId {@link String}
   */
  public void abortMultipartUpload(final String uploadId) {
    final CreateMultipartUploadResponse metadata = this.uploadIdMetadata.get(uploadId);
    final String bucketName = metadata.bucket();
    final String objectKey = metadata.key();

    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
        .bucket(bucketName).key(objectKey).uploadId(uploadId).build();
    this.s3.abortMultipartUpload(abortRequest);
  }
}
