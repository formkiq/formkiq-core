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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Send Http Request.
 */
public class SendHttpRequest {
  /** {@link HttpService}. */
  private final HttpService http;
  /** Documents IAM Url. */
  private final String documentsIamUrl;

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public SendHttpRequest(final AwsServiceCache serviceCache) {
    this.http = serviceCache.getExtension(HttpService.class);
    this.documentsIamUrl = serviceCache.environment("documentsIamUrl");
  }

  /**
   * Send Http Request.
   * 
   * @param siteId {@link String}
   * @param method {@link String}
   * @param url {@link String}
   * @param payload {@link String}
   * @throws IOException IOException
   */
  public void sendRequest(final String siteId, final String method, final String url,
      final String payload) throws IOException {

    String u = this.documentsIamUrl + url;

    Optional<Map<String, String>> parameters = Optional.empty();

    if (siteId != null) {
      parameters = Optional.of(Map.of("siteId", siteId));
    }

    HttpResponse<String> response;
    if ("put".equalsIgnoreCase(method)) {
      response = this.http.put(u, Optional.empty(), parameters, payload);
    } else if ("post".equalsIgnoreCase(method)) {
      response = this.http.post(u, Optional.empty(), parameters, payload);
    } else if ("patch".equalsIgnoreCase(method)) {
      response = this.http.patch(u, Optional.empty(), parameters, payload);
    } else {
      throw new UnsupportedOperationException("unsupported method '" + method + "'");
    }

    if (!HttpResponseStatus.is2XX(response)) {
      throw new IOException(url + " returned " + response.statusCode());
    }
  }
}
