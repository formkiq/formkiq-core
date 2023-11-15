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

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

/**
 * Class to handle S3 Presigned Urls.
 */
public class S3PresignerService {

  /** {@link S3ConnectionBuilder}. */
  private S3PresignerConnectionBuilder builder;

  /**
   * Constructor.
   * 
   * @param s3PresignerBuilder {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerService(final S3PresignerConnectionBuilder s3PresignerBuilder) {
    this.builder = s3PresignerBuilder;
  }

  /**
   * Generate a S3 Signed Url for getting an object.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @param versionId {@link String}
   * @param config {@link PresignGetUrlConfig}
   * @return {@link URL}
   */
  public URL presignGetUrl(final String bucket, final String key, final Duration duration,
      final String versionId, final PresignGetUrlConfig config) {

    try (S3Presigner signer = this.builder.build()) {

      GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key)
          .versionId(versionId).responseContentType(config.contentType())
          .responseContentDisposition(config.contentDisposition()).build();

      GetObjectPresignRequest getRequest = GetObjectPresignRequest.builder()
          .signatureDuration(duration).getObjectRequest(getObjectRequest).build();

      PresignedGetObjectRequest req = signer.presignGetObject(getRequest);
      URL url = req.url();

      return url;
    }
  }

  /**
   * Generate a S3 Signed Url for creating an object using POST request.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @param contentLength {@link Optional} {@link Long}
   * @return {@link URL}
   */
  public URL presignPostUrl(final String bucket, final String key, final Duration duration,
      final Optional<Long> contentLength) {

    try (S3Presigner signer = this.builder.build()) {

      UploadPartRequest.Builder uploadBuilder = UploadPartRequest.builder().bucket(bucket).key(key);

      if (contentLength.isPresent()) {
        uploadBuilder = uploadBuilder.contentLength(contentLength.get());
      }

      UploadPartPresignRequest prereq = UploadPartPresignRequest.builder()
          .signatureDuration(duration).uploadPartRequest(uploadBuilder.build()).build();

      PresignedUploadPartRequest req = signer.presignUploadPart(prereq);
      return req.url();
    }
  }

  /**
   * Generate a S3 Signed Url for creating an object using PUT request.
   *
   * @param bucket {@link String}
   * @param key {@link String}
   * @param duration {@link Duration}
   * @param contentLength {@link Optional} {@link Long}
   * @param metadata {@link Map}
   * @return {@link URL}
   */
  public URL presignPutUrl(final String bucket, final String key, final Duration duration,
      final Optional<Long> contentLength, final Map<String, String> metadata) {

    try (S3Presigner signer = this.builder.build()) {

      PutObjectRequest.Builder putObjectRequest =
          PutObjectRequest.builder().bucket(bucket).key(key);

      if (contentLength.isPresent()) {
        putObjectRequest = putObjectRequest.contentLength(contentLength.get());
      }

      if (metadata != null) {
        AwsRequestOverrideConfiguration.Builder override =
            AwsRequestOverrideConfiguration.builder();

        for (Map.Entry<String, String> e : metadata.entrySet()) {
          override = override.putRawQueryParameter(e.getKey(), e.getValue());
        }
        putObjectRequest = putObjectRequest.overrideConfiguration(override.build());
      }

      PutObjectPresignRequest putRequest = PutObjectPresignRequest.builder()
          .signatureDuration(duration).putObjectRequest(putObjectRequest.build()).build();

      PresignedPutObjectRequest req = signer.presignPutObject(putRequest);
      URL url = req.url();

      return url;
    }
  }
}
