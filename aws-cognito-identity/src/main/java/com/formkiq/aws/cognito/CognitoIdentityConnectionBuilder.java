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
package com.formkiq.aws.cognito;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClientBuilder;

/**
 * 
 * Cognito Connection Builder.
 *
 */
public class CognitoIdentityConnectionBuilder {

  /** {@link CognitoIdentityClientBuilder}. */
  private CognitoIdentityClientBuilder clientBuilder;
  /** {@link String}. */
  private String clientId;
  /** {@link String}. */
  private String userPoolId;
  /** {@link String}. */
  private String identityPool;
  /** {@link CognitoIdentityClient}. */
  private CognitoIdentityClient client;
  /** {@link Region}. */
  private Region region;

  /**
   * constructor.
   * 
   * @param cognitoClientId {@link String}
   * @param cognitoUserPoolId {@link String}
   * @param cognitoIdentityPoolId {@link String}
   */
  public CognitoIdentityConnectionBuilder(final String cognitoClientId,
      final String cognitoUserPoolId, final String cognitoIdentityPoolId) {

    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.clientBuilder =
        CognitoIdentityClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
            .credentialsProvider(AnonymousCredentialsProvider.create());

    this.clientId = cognitoClientId;
    this.userPoolId = cognitoUserPoolId;
    this.identityPool = cognitoIdentityPoolId;
  }

  /**
   * Build {@link CognitoIdentityClient}.
   * 
   * @return {@link CognitoIdentityClient}
   */
  public CognitoIdentityClient buildClient() {
    if (this.client == null) {
      this.client = this.clientBuilder.build();
    }

    return this.client;
  }

  /**
   * Get Cognito Client Id.
   * 
   * @return {@link String}
   */
  public String getClientId() {
    return this.clientId;
  }

  /**
   * Get Identity Pool.
   * 
   * @return {@link String}
   */
  public String getIdentityPool() {
    return this.identityPool;
  }

  /**
   * Get {@link Region}.
   * 
   * @return {@link Region}
   */
  public Region getRegion() {
    return this.region;
  }

  /**
   * Get User Pool Id.
   * 
   * @return {@link String}
   */
  public String getUserPoolId() {
    return this.userPoolId;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link AwsCredentialsProvider}
   * @return {@link CognitoIdentityConnectionBuilder}
   */
  public CognitoIdentityConnectionBuilder setCredentials(final AwsCredentialsProvider credentials) {
    this.clientBuilder = this.clientBuilder.credentialsProvider(credentials);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link CognitoIdentityConnectionBuilder}
   */
  public CognitoIdentityConnectionBuilder setCredentials(final String credentials) {

    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return this.setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link CognitoIdentityConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public CognitoIdentityConnectionBuilder setEndpointOverride(final String uri)
      throws URISyntaxException {
    this.clientBuilder = this.clientBuilder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Set Region.
   * 
   * @param r {@link Region}
   * @return {@link CognitoIdentityConnectionBuilder}
   */
  public CognitoIdentityConnectionBuilder setRegion(final Region r) {
    this.region = r;
    this.clientBuilder = this.clientBuilder.region(this.region);
    return this;
  }
}
