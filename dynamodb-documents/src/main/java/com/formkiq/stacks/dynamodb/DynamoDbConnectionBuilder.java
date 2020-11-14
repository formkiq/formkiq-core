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
package com.formkiq.stacks.dynamodb;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * 
 * DynamoDB Connection Builder.
 *
 */
public class DynamoDbConnectionBuilder {

  /** {@link DynamoDbClientBuilder}. */
  private DynamoDbClientBuilder builder;
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;

  /**
   * constructor.
   */
  public DynamoDbConnectionBuilder() {
    this.builder = DynamoDbClient.builder()
        .overrideConfiguration(ClientOverrideConfiguration.builder().build());
  }

  /**
   * Initializes the {@link DynamoDbClient}.
   */
  public void initDbClient() {
    build().close();
  }

  /**
   * Build {@link DynamoDbClient}.
   * 
   * @return {@link DynamoDbClient}
   */
  public DynamoDbClient build() {
    if (this.dbClient == null) {
      this.dbClient = this.builder.build();
    }

    return this.dbClient;
  }

  /**
   * Close {@link DynamoDbClient} if one exists.
   */
  public void close() {
    if (this.dbClient != null) {
      this.dbClient.close();
    }
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link DynamoDbConnectionBuilder}
   */
  public DynamoDbConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link DynamoDbConnectionBuilder}
   */
  public DynamoDbConnectionBuilder setCredentials(final String credentials) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link DynamoDbConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public DynamoDbConnectionBuilder setEndpointOverride(final String uri) throws URISyntaxException {
    this.builder = this.builder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link DynamoDbConnectionBuilder}
   */
  public DynamoDbConnectionBuilder setRegion(final Region region) {
    this.builder = this.builder.region(region);
    return this;
  }
}
