/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.aws.s3;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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

  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;
  /** S3 Region. */
  private Region region;
  /** {@link S3ClientBuilder}. */
  private S3ClientBuilder builder;
  /** Builder. */
  private Builder presignerBuilder;

  /**
   * constructor.
   */
  public S3ConnectionBuilder() {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");
    System.setProperty("aws.s3UseUsEast1RegionalEndpoint", "regional");

    this.builder = S3Client.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
    this.presignerBuilder = S3Presigner.builder();
  }

  /**
   * Build {@link S3Client}.
   * 
   * @return {@link S3Client}
   */
  public S3Client build() {
    return this.builder.build();
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
   * Get {@link AwsCredentials}.
   * 
   * @return {@link AwsCredentials}
   */
  public AwsCredentials getCredentials() {
    return this.credentials;
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
    build().close();
    buildPresigner().close();
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link S3ConnectionBuilder}
   */
  public S3ConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.credentials = cred.resolveCredentials();
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
   * @param endpoint {@link String}
   * @return {@link S3ConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public S3ConnectionBuilder setEndpointOverride(final String endpoint) throws URISyntaxException {
    URI uri = new URI(endpoint);
    this.builder = this.builder.endpointOverride(uri);
    this.presignerBuilder = this.presignerBuilder.endpointOverride(uri);
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
