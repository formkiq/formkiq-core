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
package com.formkiq.lambda.apigateway;

import java.util.HashMap;
import java.util.Map;

/** {@link ApiResponse} for Api Request Handler. */
public class ApiRequestHandlerResponse {

  /** HTTP Headers. */
  private Map<String, String> headers = new HashMap<>();
  /** {@link ApiResponseStatus}. */
  private ApiResponseStatus status;
  /** {@link ApiResponse}. */
  private ApiResponse response;

  /**
   * constructor.
   *
   * @param responseStatus {@link ApiResponseStatus}
   * @param apiResponse {@link ApiResponse}
   */
  public ApiRequestHandlerResponse(final ApiResponseStatus responseStatus,
      final ApiResponse apiResponse) {
    this.status = responseStatus;
    this.response = apiResponse;
  }

  /**
   * Add HTTP Headers.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  public void addHeader(final String key, final String value) {
    this.headers.put(key, value);
  }

  /**
   * Get HTTP Headers.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * Get {@link ApiResponse}.
   *
   * @return {@link ApiResponse}
   */
  public ApiResponse getResponse() {
    return this.response;
  }

  /**
   * Get {@link ApiResponse} Status.
   *
   * @return {@link ApiResponseStatus}
   */
  public ApiResponseStatus getStatus() {
    return this.status;
  }

  /**
   * Set Http Headers.
   * 
   * @param map {@link Map}
   */
  public void setHeaders(final Map<String, String> map) {
    this.headers = map;
  }
}
