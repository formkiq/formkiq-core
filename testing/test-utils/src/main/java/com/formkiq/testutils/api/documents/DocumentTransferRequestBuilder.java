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
package com.formkiq.testutils.api.documents;

import java.util.HashMap;
import java.util.Map;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.testutils.api.ApiHttpClient;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.stream.Collectors;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import com.google.gson.reflect.TypeToken;

/** Builds transfer requests with arbitrary query values for API validation tests. */
public class DocumentTransferRequestBuilder implements HttpRequestBuilder<Map<String, Object>> {
  /** First HTTP error status. */
  private static final int ERROR_STATUS = 400;
  /** HTTP method. */
  private final String method;
  /** API path. */
  private final String path;
  /** Query parameters. */
  private final Map<String, String> query = new HashMap<>();
  /** Request body. */
  private Map<String, Object> body;

  /**
   * Construct a request.
   * 
   * @param httpMethod HTTP method
   * @param requestPath API path
   */
  public DocumentTransferRequestBuilder(final String httpMethod, final String requestPath) {
    this.method = httpMethod;
    this.path = requestPath;
  }

  /**
   * Set the request body.
   * 
   * @param requestBody Body
   * @return this builder
   */
  public DocumentTransferRequestBuilder body(final Map<String, Object> requestBody) {
    this.body = requestBody;
    return this;
  }

  /**
   * Set a query parameter.
   * 
   * @param name Parameter name
   * @param value Parameter value
   * @return this builder
   */
  public DocumentTransferRequestBuilder query(final String name, final String value) {
    this.query.put(name, value);
    return this;
  }

  @Override
  public ApiHttpResponse<Map<String, Object>> submit(final ApiClient client, final String siteId) {
    Map<String, String> parameters = new HashMap<>(this.query);
    if (siteId != null) {
      parameters.put("siteId", siteId);
    }
    String queryString = parameters.entrySet().stream()
        .map(e -> client.escapeString(e.getKey()) + "=" + client.escapeString(e.getValue()))
        .collect(Collectors.joining("&"));
    String url = client.getBasePath() + this.path + "?" + queryString;
    Gson gson = new Gson();
    try {
      var response = ApiHttpClient.send(siteId != null ? siteId : "default", url, this.method,
          this.body != null ? gson.toJson(this.body) : null);
      if (response.statusCode() >= ERROR_STATUS) {
        return new ApiHttpResponse<>(null, new ApiException(response.statusCode(), response.body(),
            response.headers().map(), response.body()));
      }
      Map<String, Object> value =
          gson.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType());
      return new ApiHttpResponse<>(value, null);
    } catch (IOException e) {
      return new ApiHttpResponse<>(null, new ApiException(e));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ApiHttpResponse<>(null, new ApiException(e));
    }
  }
}
