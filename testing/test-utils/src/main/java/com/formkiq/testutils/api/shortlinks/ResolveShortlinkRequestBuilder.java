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
package com.formkiq.testutils.api.shortlinks;

import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Resolves a public shortlink without following its redirect. */
public class ResolveShortlinkRequestBuilder implements HttpRequestBuilder<HttpResponse<String>> {

  /** First HTTP error status code. */
  private static final int ERROR_STATUS = 400;
  /** Shortlink slug. */
  private final String slug;

  /**
   * Constructor.
   *
   * @param shortlinkSlug Shortlink slug
   */
  public ResolveShortlinkRequestBuilder(final String shortlinkSlug) {
    this.slug = shortlinkSlug;
  }

  @Override
  public ApiHttpResponse<HttpResponse<String>> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> {
      String url =
          apiClient.getBasePath() + "/s/" + URLEncoder.encode(this.slug, StandardCharsets.UTF_8);
      try {
        var response = new HttpServiceJdk11().get(url,
            Optional.of(new HttpHeaders().add("Authorization", "dummy")), Optional.empty());
        if (response.statusCode() >= ERROR_STATUS) {
          throw new ApiException(response.statusCode(), response.headers().map(), response.body());
        }
        return response;
      } catch (IOException e) {
        throw new ApiException(e);
      }
    });
  }
}
