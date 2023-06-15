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
package com.formkiq.aws.ssm;

import java.net.URI;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder;
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
  private SsmClient ssmClient = null;

  /**
   * constructor.
   * 
   * @param enableAwsXray Enable Aws X-Ray
   */
  public SsmConnectionBuilder(final boolean enableAwsXray) {
    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    Builder clientConfig = ClientOverrideConfiguration.builder();

    if (enableAwsXray) {
      clientConfig.addExecutionInterceptor(new TracingInterceptor());
    }

    this.builder = SsmClient.builder().overrideConfiguration(clientConfig.build())
        .httpClientBuilder(UrlConnectionHttpClient.builder())
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
   * @param uri {@link URI}
   * @return {@link SsmConnectionBuilder}
   */
  public SsmConnectionBuilder setEndpointOverride(final URI uri) {
    this.builder = this.builder.endpointOverride(uri);
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
