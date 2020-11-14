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
package com.formkiq.aws.lambda;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

/**
 * 
 * S3 Connection Builder.
 *
 */
public class LambdaConnectionBuilder {

  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;
  /** S3 Region. */
  private Region region;
  /** {@link LambdaClientBuilder}. */
  private LambdaClientBuilder builder;

  /**
   * constructor.
   */
  public LambdaConnectionBuilder() {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.builder = LambdaClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder());
  }

  /**
   * Build {@link LambdaClient}.
   * 
   * @return {@link LambdaClient}
   */
  public LambdaClient build() {
    return this.builder.build();
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
   * Init Lambda Client.
   */
  public void initLambdaClient() {
    build().close();
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link LambdaConnectionBuilder}
   */
  public LambdaConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.credentials = cred.resolveCredentials();
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentialName {@link String}
   * @return {@link LambdaConnectionBuilder}
   */
  public LambdaConnectionBuilder setCredentials(final String credentialName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentialName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param endpoint {@link String}
   * @return {@link LambdaConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public LambdaConnectionBuilder setEndpointOverride(final String endpoint)
      throws URISyntaxException {
    URI uri = new URI(endpoint);
    this.builder = this.builder.endpointOverride(uri);
    return this;
  }

  /**
   * Set Region.
   * 
   * @param regionname {@link String}
   * @return {@link LambdaConnectionBuilder}
   */
  public LambdaConnectionBuilder setRegion(final String regionname) {
    this.region = Region.of(regionname);
    this.builder = this.builder.region(this.region);
    return this;
  }
}
