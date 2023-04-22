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
package com.formkiq.aws.sqs;

import java.net.URI;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder;
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
  private SqsClient sqsClient = null;

  /**
   * constructor.
   * 
   * @param enableAwsXray Enable Aws X-Ray
   */
  public SqsConnectionBuilder(final boolean enableAwsXray) {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    Builder clientConfig = ClientOverrideConfiguration.builder();

    if (enableAwsXray) {
      clientConfig.addExecutionInterceptor(new TracingInterceptor());
    }

    this.builder = SqsClient.builder().overrideConfiguration(clientConfig.build())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
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
   * @param uri {@link URI}
   * @return {@link SqsConnectionBuilder}
   */
  public SqsConnectionBuilder setEndpointOverride(final URI uri) {
    this.builder = this.builder.endpointOverride(uri);
    return this;
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
}
