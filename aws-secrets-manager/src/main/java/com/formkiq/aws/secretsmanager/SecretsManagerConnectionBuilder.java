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
package com.formkiq.aws.secretsmanager;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;

/**
 * 
 * Secrets Manager Connection Builder.
 *
 */
public class SecretsManagerConnectionBuilder {

  /** {@link SecretsManagerClientBuilder}. */
  private SecretsManagerClientBuilder builder;
  /** S3 Region. */
  private Region region;
  /** {@link SecretsManagerClient}. */
  private SecretsManagerClient secretsManagerClient;

  /**
   * constructor.
   *
   * @param enableAwsXray Enable AWS X-Ray
   */
  public SecretsManagerConnectionBuilder(final boolean enableAwsXray) {

    ClientOverrideConfiguration.Builder clientConfig = ClientOverrideConfiguration.builder();
    SdkHttpClient c = UrlConnectionHttpClient.builder().build();

    this.builder = software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
        .overrideConfiguration(clientConfig.build()).httpClient(c)
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
  }

  /**
   * Build {@link SecretsManagerClient}.
   * 
   * @return {@link SecretsManagerClient}
   */
  public SecretsManagerClient build() {
    initSecretsManagerClient();
    return this.secretsManagerClient;
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
  public void initSecretsManagerClient() {
    if (this.secretsManagerClient == null) {
      this.secretsManagerClient = this.builder.build();
    }
  }

  /**
   * Set Credentials.
   * 
   * @param cred {@link AwsCredentialsProvider}
   * @return {@link SecretsManagerConnectionBuilder}
   */
  public SecretsManagerConnectionBuilder setCredentials(final AwsCredentialsProvider cred) {
    this.builder = this.builder.credentialsProvider(cred);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentialName {@link String}
   * @return {@link SecretsManagerConnectionBuilder}
   */
  public SecretsManagerConnectionBuilder setCredentials(final String credentialName) {
    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentialName).build()) {
      return setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param endpoint {@link URI}
   * @return {@link SecretsManagerConnectionBuilder}
   */
  public SecretsManagerConnectionBuilder setEndpointOverride(final URI endpoint) {
    this.builder = this.builder.endpointOverride(endpoint);
    return this;
  }

  /**
   * Set Region.
   * 
   * @param r {@link Region}
   * @return {@link SecretsManagerConnectionBuilder}
   */
  public SecretsManagerConnectionBuilder setRegion(final Region r) {
    this.region = r;
    this.builder = this.builder.region(this.region);
    return this;
  }
}
