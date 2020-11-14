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
package com.formkiq.aws.ssm;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

/**
 * 
 * Ssm Connection Builder.
 *
 */
public class SsmConnectionBuilder {

  /** {@link SsmClientBuilder}. */
  private SsmClientBuilder builder;
  /** {@link SsmClient}. */
  private SsmClient ssmClient;

  /**
   * constructor.
   */
  public SsmConnectionBuilder() {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.builder = SsmClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
  }

  /**
   * Build {@link SsmClient}.
   * 
   * @return {@link SsmClient}
   */
  public SsmClient build() {
    if (this.ssmClient == null) {
      this.ssmClient = this.builder.build();
    }

    return this.ssmClient;
  }

  /**
   * Close {@link SsmClient} if one exists.
   */
  public void close() {
    if (this.ssmClient != null) {
      this.ssmClient.close();
    }
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link SsmConnectionBuilder}
   */
  public SsmConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link SsmConnectionBuilder}
   */
  public SsmConnectionBuilder setCredentials(final String credentials) {
    try (ProfileCredentialsProvider profile = ProfileCredentialsProvider.create(credentials)) {
      return setCredentials(profile);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}s
   * @return {@link SsmConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public SsmConnectionBuilder setEndpointOverride(final String uri) throws URISyntaxException {
    this.builder = this.builder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link SsmConnectionBuilder}
   */
  public SsmConnectionBuilder setRegion(final Region region) {
    this.builder = this.builder.region(region);
    return this;
  }
}
