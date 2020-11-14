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
package com.formkiq.aws.sns;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

/**
 * 
 * Sns Connection Builder.
 *
 */
public class SnsConnectionBuilder {

  /** {@link SnsClientBuilder}. */
  private SnsClientBuilder builder;
  /** {@link SnsClient}. */
  private SnsClient snsClient;

  /**
   * constructor.
   */
  public SnsConnectionBuilder() {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.builder = SnsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link SnsConnectionBuilder}
   */
  public SnsConnectionBuilder setRegion(final Region region) {
    this.builder = this.builder.region(region);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link SnsConnectionBuilder}
   */
  public SnsConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link SnsConnectionBuilder}
   */
  public SnsConnectionBuilder setCredentials(final String credentials) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link SnsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public SnsConnectionBuilder setEndpointOverride(final String uri) throws URISyntaxException {
    this.builder = this.builder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Build {@link SnsClient}.
   * 
   * @return {@link SnsClient}
   */
  public SnsClient build() {
    if (this.snsClient == null) {
      this.snsClient = this.builder.build();
    }

    return this.snsClient;
  }

  /**
   * Close {@link SnsClient} if one exists.
   */
  public void close() {
    if (this.snsClient != null) {
      this.snsClient.close();
    }
  }
}
