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
package com.formkiq.aws.dynamodb;

import java.net.URI;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder;
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
  private DynamoDbClient dbClient = null;

  /**
   * constructor.
   * 
   * @param enableAwsXray Enables AWS X-Ray
   */
  public DynamoDbConnectionBuilder(final boolean enableAwsXray) {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");
    Builder clientConfig = ClientOverrideConfiguration.builder();

    if (enableAwsXray) {
      clientConfig.addExecutionInterceptor(new TracingInterceptor());
    }

    this.builder = DynamoDbClient.builder().overrideConfiguration(clientConfig.build());
  }

  /**
   * Build {@link DynamoDbClient}.
   * 
   * @return {@link DynamoDbClient}
   */
  public DynamoDbClient build() {
    initDbClient();
    return this.dbClient;
  }

  /**
   * Initializes the {@link DynamoDbClient}.
   */
  public void initDbClient() {
    if (this.dbClient == null) {
      this.dbClient = this.builder.build();
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
   * @param uri {@link URI}
   * @return {@link DynamoDbConnectionBuilder}
   */
  public DynamoDbConnectionBuilder setEndpointOverride(final URI uri) {
    this.builder = this.builder.endpointOverride(uri);
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
