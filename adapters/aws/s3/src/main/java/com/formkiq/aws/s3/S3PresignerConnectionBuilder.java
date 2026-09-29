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

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

/**
 * 
 * S3 Connection Builder.
 *
 */
public class S3PresignerConnectionBuilder {

  /** Credentials used by both endpoint modes. */
  private AwsCredentialsProvider credentials;
  /** Signing region. */
  private Region region;
  /** Custom endpoint, used for local deployments. */
  private URI endpoint;
  /** Whether path-style access is forced. */
  private Boolean pathStyle;
  /** Bucket eligible for acceleration; null disables acceleration. */
  private String acceleratedBucket;

  /**
   * constructor.
   * 
   */
  public S3PresignerConnectionBuilder() {

  }

  /**
   * Configure the sole bucket eligible for acceleration.
   * 
   * @param bucket Bucket name, or null to disable
   * @return this builder
   */
  public S3PresignerConnectionBuilder acceleratedBucket(final String bucket) {
    this.acceleratedBucket = bucket;
    return this;
  }

  /**
   * Build {@link S3Presigner}.
   * 
   * @return {@link S3Presigner}s
   */
  public S3Presigner build() {
    return build(false);
  }

  /**
   * Build an independent signer for a request.
   * 
   * @param accelerate Whether to use the accelerated endpoint
   * @return S3Presigner
   */
  public S3Presigner build(final boolean accelerate) {
    if (accelerate) {
      validateAcceleration(this.acceleratedBucket);
    }
    S3Configuration configuration = S3Configuration.builder().pathStyleAccessEnabled(this.pathStyle)
        .accelerateModeEnabled(accelerate).checksumValidationEnabled(false).build();
    Builder builder = S3Presigner.builder().serviceConfiguration(configuration);
    if (this.credentials != null) {
      builder.credentialsProvider(this.credentials);
    }
    if (this.region != null) {
      builder.region(this.region);
    }
    if (this.endpoint != null) {
      builder.endpointOverride(this.endpoint);
    }
    return builder.build();
  }

  /**
   * Enable Path Style Access.
   * 
   * @param enabled {@link Boolean}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder pathStyleAccessEnabled(final Boolean enabled) {
    this.pathStyle = enabled;
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.credentials = cred;
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentialName {@link String}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setCredentials(final String credentialName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentialName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param endpointOverride {@link URI}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setEndpointOverride(final URI endpointOverride) {
    this.endpoint = endpointOverride;
    return this;
  }

  /**
   * Set Region.
   * 
   * @param signingRegion {@link Region}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setRegion(final Region signingRegion) {
    this.region = signingRegion;
    return this;
  }

  /**
   * Validate acceleration before any document mutation.
   * 
   * @param bucket Target bucket
   */
  public void validateAcceleration(final String bucket) {
    if (bucket == null || !bucket.equals(this.acceleratedBucket)) {
      throw new IllegalArgumentException("S3 transfer acceleration is not enabled for this bucket");
    }
    if (bucket.contains(".") || !bucket.matches("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")) {
      throw new IllegalArgumentException(
          "S3 transfer acceleration requires a DNS bucket name without periods");
    }
    if (this.endpoint != null || Boolean.TRUE.equals(this.pathStyle)) {
      throw new IllegalArgumentException(
          "S3 transfer acceleration cannot use a custom endpoint or path-style access");
    }
  }
}
