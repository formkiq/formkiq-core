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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClientBuilder;

/**
 * 
 * Cognito Connection Builder.
 *
 */
public class CognitoConnectionBuilder {

  /** {@link CognitoIdentityClientBuilder}. */
  private CognitoIdentityClientBuilder clientBuilder;
  /** {@link CognitoIdentityProviderClientBuilder}. */
  private CognitoIdentityProviderClientBuilder providerBuilder;
  /** {@link CognitoIdentityProviderClient}. */
  private CognitoIdentityProviderClient provider;
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
  public CognitoConnectionBuilder(final String cognitoClientId, final String cognitoUserPoolId,
      final String cognitoIdentityPoolId) {

    System.setProperty("software.amazon.awssdk.http.service.impl",
        "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

    this.providerBuilder =
        CognitoIdentityProviderClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder())
            .credentialsProvider(AnonymousCredentialsProvider.create());

    this.clientBuilder =
        CognitoIdentityClient.builder().credentialsProvider(AnonymousCredentialsProvider.create());

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
   * Build {@link CognitoIdentityProviderClient}.
   * 
   * @return {@link CognitoIdentityProviderClient}
   */
  public CognitoIdentityProviderClient buildProvider() {
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
   * @return {@link CognitoConnectionBuilder}
   */
  public CognitoConnectionBuilder setCredentials(final AwsCredentialsProvider credentials) {
    this.providerBuilder = this.providerBuilder.credentialsProvider(credentials);
    this.clientBuilder = this.clientBuilder.credentialsProvider(credentials);
    return this;
  }

  /**
   * Set Credentials.
   * 
   * @param credentials {@link String}
   * @return {@link CognitoConnectionBuilder}
   */
  public CognitoConnectionBuilder setCredentials(final String credentials) {

    try (ProfileCredentialsProvider prov =
        ProfileCredentialsProvider.builder().profileName(credentials).build()) {
      return this.setCredentials(prov);
    }
  }

  /**
   * Set Endpoint Override.
   * 
   * @param uri {@link String}
   * @return {@link CognitoConnectionBuilder}
   * @throws URISyntaxException URISyntaxException
   */
  public CognitoConnectionBuilder setEndpointOverride(final String uri) throws URISyntaxException {
    this.providerBuilder = this.providerBuilder.endpointOverride(new URI(uri));
    this.clientBuilder = this.clientBuilder.endpointOverride(new URI(uri));
    return this;
  }

  /**
   * Set Region.
   * 
   * @param r {@link Region}
   * @return {@link CognitoConnectionBuilder}
   */
  public CognitoConnectionBuilder setRegion(final Region r) {
    this.region = r;
    this.providerBuilder = this.providerBuilder.region(this.region);
    this.clientBuilder = this.clientBuilder.region(this.region);
    return this;
  }
}
