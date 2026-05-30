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
package com.formkiq.testutils.api;

import com.formkiq.client.invoker.ApiClient;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * {@link ApiClient} builder.
 */
public class ApiClientBuilder {

  /** Base Path. */
  private String basePath;
  /** Read Timeout. */
  private Integer readTimeout = 0;
  /** Bearer Token. */
  private String bearerToken;
  /** AWS Access Key. */
  private String awsAccessKeyId;
  /** AWS Secret Key. */
  private String awsSecretAccessKey;
  /** AWS Region. */
  private Region awsRegion;
  /** AWS Service. */
  private String awsService = "execute-api";
  /** AWS Session Token. */
  private String awsSessionToken;

  /**
   * Base path (required).
   * 
   * @param urlBasePath {@link String}
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder basePath(final String urlBasePath) {
    this.basePath = urlBasePath;
    return this;
  }

  /**
   * Configure JWT / Bearer Authorization.
   * 
   * @param token {@link String}
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder bearerToken(final String token) {
    this.bearerToken = token;
    return this;
  }

  /**
   * Build the ApiClient.
   * 
   * @return {@link ApiClient}
   */
  public ApiClient build() {
    if (basePath == null) {
      throw new IllegalStateException("basePath is required");
    }

    ApiClient client = new ApiClient().setBasePath(basePath);

    if (readTimeout != null) {
      client.setReadTimeout(readTimeout);
    }

    // JWT auth case
    if (bearerToken != null) {
      client.addDefaultHeader("Authorization", bearerToken);
      return client;
    }

    // IAM SigV4 case
    if (awsAccessKeyId != null && awsSecretAccessKey != null && awsRegion != null) {

      if (awsSessionToken != null) {
        client.setAWS4Configuration(awsAccessKeyId, awsSecretAccessKey, awsSessionToken,
            awsRegion.id(), awsService);
      } else {
        client.setAWS4Configuration(awsAccessKeyId, awsSecretAccessKey, awsRegion.id(), awsService);
      }

      return client;
    }

    throw new IllegalStateException(
        "No authentication configured. Use bearerToken(...) or iamCredentials(...).");
  }

  /**
   * Configure IAM SigV4 credentials explicitly.
   * 
   * @param credentials {@link AwsCredentials}
   * @param region {@link Region}
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder iamCredentials(final AwsCredentials credentials, final Region region) {
    this.awsAccessKeyId = credentials.accessKeyId();
    this.awsSecretAccessKey = credentials.secretAccessKey();

    if (credentials instanceof AwsSessionCredentials) {
      this.awsSessionToken = ((AwsSessionCredentials) credentials).sessionToken();
    }

    this.awsRegion = region;
    return this;
  }

  /**
   * Configure IAM SigV4 credentials via provider.
   * 
   * @param provider {@link AwsCredentialsProvider}
   * @param region {@link Region}
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder iamCredentials(final AwsCredentialsProvider provider,
      final Region region) {
    AwsCredentials c = provider.resolveCredentials();
    return iamCredentials(c, region);
  }

  /**
   * Optional override of service (default execute-api).
   * 
   * @param service {@link String}
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder iamService(final String service) {
    this.awsService = service;
    return this;
  }

  /**
   * Optional read timeout in milliseconds (0 means no timeout).
   * 
   * @param requestReadTimeout int
   * @return {@link ApiClientBuilder}
   */
  public ApiClientBuilder readTimeout(final int requestReadTimeout) {
    this.readTimeout = requestReadTimeout;
    return this;
  }
}
