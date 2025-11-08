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
package com.formkiq.module.httpsigv4;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

/**
 * {@link Function} to sigv4 URL.
 */
public class SignUrl implements Function<String, String> {
  /** Signing Name. */
  private final String signingName;
  /** Signing Region. */
  private final Region signingRegion;
  /** Signing Credentials. */
  private final AwsCredentials signingCredentials;

  /**
   * constructor.
   * 
   * @param serviceName {@link String}
   * @param region {@link Region}
   * @param credentials {@link AwsCredentials}
   */
  public SignUrl(final String serviceName, final Region region, final AwsCredentials credentials) {
    this.signingName = serviceName;
    this.signingRegion = region;
    this.signingCredentials = credentials;
  }

  @Override
  public String apply(final String url) {
    try {
      SdkHttpFullRequest.Builder requestBuilder =
          SdkHttpFullRequest.builder().uri(new URI(url)).method(SdkHttpMethod.GET);
      SdkHttpFullRequest req =
          new PresignSdkHttpFullRequest(signingName, signingRegion, signingCredentials)
              .apply(requestBuilder);

      return new SdkHttpRequestToUrl().apply(req);

    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
