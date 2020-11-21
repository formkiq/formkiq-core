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
package com.formkiq.aws.sqs;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

/**
 * 
 * Sqs Connection Builder.
 *
 */
public class SqsConnectionBuilder {

  /** {@link SqsClientBuilder}. */
  private SqsClientBuilder builder;
  /** {@link SqsClient}. */
  private SqsClient sqsClient;

  /**
   * constructor.
   */
  public SqsConnectionBuilder() {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.builder = SqsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link SqsConnectionBuilder}
   */
  public SqsConnectionBuilder setRegion(final Region region) {
    this.builder = this.builder.region(region);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link SqsConnectionBuilder}
   */
  public SqsConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link SqsConnectionBuilder}
   */
  public SqsConnectionBuilder setCredentials(final String credentials) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link SqsConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public SqsConnectionBuilder setEndpointOverride(final String uri) throws URISyntaxException {
    this.builder = this.builder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Build {@link SqsClient}.
   * 
   * @return {@link SqsClient}
   */
  public SqsClient build() {
    if (this.sqsClient == null) {
      this.sqsClient = this.builder.build();
    }

    return this.sqsClient;
  }

  /**
   * Close {@link SqsClient} if one exists.
   */
  public void close() {
    if (this.sqsClient != null) {
      this.sqsClient.close();
    }
  }
}
