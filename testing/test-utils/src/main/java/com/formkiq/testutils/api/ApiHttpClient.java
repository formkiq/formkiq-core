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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Send API requests using {@link HttpClient}.
 */
public class ApiHttpClient {

  /**
   * Send API request.
   * 
   * @param group {@link String}
   * @param basePath {@link String}
   * @param method {@link String}
   * @param body {@link String}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  public static HttpResponse<String> send(final String group, final String basePath,
      final String method, final String body) throws IOException, InterruptedException {

    String jwt = JwtTokenEncoder.encodeCognito(new String[] {group}, "joesmith");

    try (HttpClient http = HttpClient.newHttpClient()) {
      HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(basePath))
          .method(method, body != null ? HttpRequest.BodyPublishers.ofString(body)
              : HttpRequest.BodyPublishers.noBody())
          .header("Authorization", jwt);
      return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
  }
}
