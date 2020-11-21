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
