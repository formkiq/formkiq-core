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
package com.formkiq.module.lambdaservices;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;

/**
 * {@link AwsServiceCache} builder.
 */
public class AwsServiceCacheBuilder {

  /** {@link AwsServiceCache}. */
  private AwsServiceCache serviceCache;
  /** {@link Map}. */
  private Map<String, URI> serviceEndpoints;
  /** {@link AwsCredentialsProvider}. */
  private AwsCredentialsProvider credentialsProvider;

  /**
   * constructor.
   * 
   * @param enviroment {@link Map}
   * @param awsServiceEndpoints {@link Map}
   * @param awsCredentialsProvider {@link AwsCredentialsProvider}
   */
  public AwsServiceCacheBuilder(final Map<String, String> enviroment,
      final Map<String, URI> awsServiceEndpoints,
      final AwsCredentialsProvider awsCredentialsProvider) {

    this.serviceEndpoints = awsServiceEndpoints;
    this.credentialsProvider = awsCredentialsProvider;

    this.serviceCache =
        new AwsServiceCache().environment(enviroment).debug("true".equals(enviroment.get("DEBUG")))
            .enableXray("true".equals(enviroment.get("ENABLE_AWS_X_RAY")))
            .region(Region.of(enviroment.get("AWS_REGION")));

    if (awsCredentialsProvider != null) {
      try {
        this.serviceCache.register(AwsCredentials.class,
            new ClassServiceExtension<>(awsCredentialsProvider.resolveCredentials()));
      } catch (SdkClientException e) {
        // ignore
      }
    }
  }

  /**
   * Add Service.
   * 
   * @param services {@link AwsServiceRegistry}
   * @return {@link AwsServiceCacheBuilder}
   */
  public AwsServiceCacheBuilder addService(final AwsServiceRegistry... services) {
    for (int i = 0; i < services.length; i++) {
      services[i].initService(this.serviceCache, this.serviceEndpoints, this.credentialsProvider);
    }
    return this;
  }

  /**
   * Build.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache build() {
    return this.serviceCache;
  }
}
