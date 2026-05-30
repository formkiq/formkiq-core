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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClientBuilder;

/**
 * 
 * Cognito Connection Builder.
 *
 */
public class CognitoIdentityProviderConnectionBuilder {

  /** {@link String}. */
  private String clientId;
  /** {@link CognitoIdentityProviderClient}. */
  private CognitoIdentityProviderClient provider;

  /** {@link CognitoIdentityProviderClientBuilder}. */
  private CognitoIdentityProviderClientBuilder providerBuilder;
  /** {@link String}. */
  private String userPoolId;

  /**
   * constructor.
   * 
   * @param cognitoClientId {@link String}
   * @param cognitoUserPoolId {@link String}
   * 
   */
  public CognitoIdentityProviderConnectionBuilder(final String cognitoClientId,
      final String cognitoUserPoolId) {

    this.clientId = cognitoClientId;
    this.userPoolId = cognitoUserPoolId;

    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.providerBuilder =
        CognitoIdentityProviderClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
            .credentialsProvider(AnonymousCredentialsProvider.create());
  }

  /**
   * Build {@link CognitoIdentityProviderClient}.
   * 
   * @return {@link CognitoIdentityProviderClient}
   */
  public CognitoIdentityProviderClient build() {
    if (this.provider == null) {
      this.provider = this.providerBuilder.build();
    }

    return this.provider;
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
   * @return {@link CognitoIdentityProviderConnectionBuilder}
   */
  public CognitoIdentityProviderConnectionBuilder setCredentials(
      final AwsCredentialsProvider credentials) {
    this.providerBuilder = this.providerBuilder.credentialsProvider(credentials);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link CognitoIdentityProviderConnectionBuilder}
   */
  public CognitoIdentityProviderConnectionBuilder setCredentials(final String credentials) {

    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return this.setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link CognitoIdentityProviderConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public CognitoIdentityProviderConnectionBuilder setEndpointOverride(final String uri)
      throws URISyntaxException {
    this.providerBuilder = this.providerBuilder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Set Region.
   * 
   * @param region {@link Region}
   * @return {@link CognitoIdentityProviderConnectionBuilder}
   */
  public CognitoIdentityProviderConnectionBuilder setRegion(final Region region) {
    this.providerBuilder = this.providerBuilder.region(region);
    return this;
  }
}
