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
package com.formkiq.benchmarks;

import com.formkiq.client.invoker.ApiClient;
import com.formkiq.testutils.api.ApiClientBuilder;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Builds a reusable API client without putting authentication material in reports. */
public final class BenchmarkClient {

  /**
   * Apply the same transport settings to live and local verification clients.
   * 
   * @param client API client
   * @param timeoutSeconds whole-call timeout
   */
  public static void configure(final ApiClient client, final int timeoutSeconds) {
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    client.setHttpClient(client.getHttpClient().newBuilder().connectTimeout(timeout)
        .readTimeout(timeout).writeTimeout(timeout).callTimeout(timeout)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build());
  }

  /**
   * Create an authenticated client using environment secrets or AWS credentials.
   * 
   * @param config benchmark configuration
   * @param properties AWS region/profile properties
   * @param environment environment containing optional bearer token or API key
   * @return client with bounded timeouts and disabled connection retries
   */
  public static ApiClient create(final BenchmarkConfig config, final Properties properties,
      final Map<String, String> environment) {
    String token = environment.getOrDefault("FORMKIQ_BENCHMARK_TOKEN", "");
    String apiKey = environment.getOrDefault("FORMKIQ_BENCHMARK_API_KEY", "");
    if (!token.isBlank() && !apiKey.isBlank()) {
      throw new IllegalArgumentException("Set only one benchmark token or API key");
    }
    var builder = new ApiClientBuilder().basePath(config.endpoint().toString());
    ApiClient client;
    if (!token.isBlank()) {
      client = builder.bearerToken(token).build();
    } else if (!apiKey.isBlank()) {
      client = builder.bearerToken(apiKey).build();
    } else {
      client = iamClient(builder, properties);
    }
    configure(client, config.timeoutSeconds());
    return client;
  }

  private static ApiClient iamClient(final ApiClientBuilder builder, final Properties properties) {
    String region = properties.getProperty("testregion", "");
    if (region.isBlank()) {
      throw new IllegalArgumentException("IAM authentication requires -Ptestregion");
    }
    String profile = properties.getProperty("testprofile", "");
    AwsCredentialsProvider provider =
        profile.isBlank() ? DefaultCredentialsProvider.builder().build()
            : ProfileCredentialsProvider.create(profile);
    try {
      return builder.iamCredentials(provider, Region.of(region)).build();
    } finally {
      if (provider instanceof AutoCloseable closeable) {
        try {
          closeable.close();
        } catch (Exception exception) {
          throw new IllegalStateException("Unable to close credential provider", exception);
        }
      }
    }
  }

  private BenchmarkClient() {}
}
