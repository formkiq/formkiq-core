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
import com.amazonaws.xray.interceptors.TracingInterceptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

/**
 * 
 * S3 Connection Builder.
 *
 */
public class S3ConnectionBuilder {

  /** S3 Region. */
  private Region region;
  /** {@link S3ClientBuilder}. */
  private S3ClientBuilder builder;
  /** Builder. */
  private Builder presignerBuilder;
  /** {@link S3Client}. */
  private S3Client s3Client;

  /**
   * constructor.
   * 
   * @param enableAwsXray Enable AWS X-Ray
   */
  public S3ConnectionBuilder(final boolean enableAwsXray) {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");
    System.setProperty("aws.s3UseUsEast1RegionalEndpoint", "regional");

    ClientOverrideConfiguration.Builder clientConfig = ClientOverrideConfiguration.builder();

    if (enableAwsXray) {
      clientConfig.addExecutionInterceptor(new TracingInterceptor());
    }

    this.builder = S3Client.builder().overrideConfiguration(clientConfig.build())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
    this.presignerBuilder = S3Presigner.builder();
  }

  /**
   * Build {@link S3Client}.
   * 
   * @return {@link S3Client}
   */
  public S3Client build() {
    initS3Client();
    return this.s3Client;
  }

  /**
   * Build {@link S3Presigner}.
   * 
   * @return {@link S3Presigner}s
   */
  public S3Presigner buildPresigner() {
    return this.presignerBuilder.build();
  }

  /**
   * Get Region.
   * 
   * @return {@link Region}
   */
  public Region getRegion() {
    return this.region;
  }

  /**
   * Init S3 Client.
   */
  public void initS3Client() {
    if (this.s3Client == null) {
      this.s3Client = this.builder.build();
    }
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    this.presignerBuilder = this.presignerBuilder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentialName {@link String}
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder setCredentials(final String credentialName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentialName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param endpoint {@link URI}
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder setEndpointOverride(final URI endpoint) {
    this.builder = this.builder.endpointOverride(endpoint);
    this.presignerBuilder = this.presignerBuilder.endpointOverride(endpoint);
    return this;
  }

  /**
   * Set Region.
   * 
   * @param r {@link Region}
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder setRegion(final Region r) {
    this.region = r;
    this.builder = this.builder.region(this.region);
    this.presignerBuilder = this.presignerBuilder.region(this.region);
    return this;
  }
}
