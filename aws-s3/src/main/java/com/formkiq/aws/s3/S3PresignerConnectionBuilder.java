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

  /** Builder. */
  private Builder presignerBuilder;

  /**
   * constructor.
   * 
   */
  public S3PresignerConnectionBuilder() {
    this.presignerBuilder = S3Presigner.builder();
  }

  /**
   * Build {@link S3Presigner}.
   * 
   * @return {@link S3Presigner}s
   */
  public S3Presigner build() {
    return this.presignerBuilder.build();
  }

  /**
   * Enable Path Style Access.
   * 
   * @param enabled {@link Boolean}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder pathStyleAccessEnabled(final Boolean enabled) {
    S3Configuration conf = S3Configuration.builder().pathStyleAccessEnabled(Boolean.TRUE).build();
    this.presignerBuilder = this.presignerBuilder.serviceConfiguration(conf);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.presignerBuilder = this.presignerBuilder.credentialsProvider(cred);
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
   * @param endpoint {@link URI}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setEndpointOverride(final URI endpoint) {
    this.presignerBuilder = this.presignerBuilder.endpointOverride(endpoint);
    return this;
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link S3PresignerConnectionBuilder}
   */
  public S3PresignerConnectionBuilder setRegion(final Region region) {
    this.presignerBuilder = this.presignerBuilder.region(region);
    return this;
  }
}
