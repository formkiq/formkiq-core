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
package com.formkiq.aws.services.lambda;

import java.util.HashMap;
import java.util.Map;

/** {@link ApiResponse} for Api Request Handler. */
public class ApiRequestHandlerResponse {

  /** HTTP Headers. */
  private Map<String, String> headers = new HashMap<>();
  /** {@link ApiResponseStatus}. */
  private final ApiResponseStatus status;
  /** {@link ApiResponse}. */
  private final ApiResponse response;

  /**
   * Constructor.
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
   * Get HTTP Headers.
   *
   * @return headers map
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
   * Get {@link ApiResponseStatus}.
   *
   * @return {@link ApiResponseStatus}
   */
  public ApiResponseStatus getStatus() {
    return this.status;
  }

  /**
   * Replace all HTTP headers.
   *
   * @param map new headers map
   */
  public void setHeaders(final Map<String, String> map) {
    this.headers = map;
  }

  /**
   * {@link Builder}.
   * 
   * @return a new Builder for ApiRequestHandlerResponse
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link ApiRequestHandlerResponse}.
   */
  public static class Builder {
    /** {@link ApiResponseStatus}. */
    private ApiResponseStatus status;
    /** Data. */
    private final Map<String, Object> data = new HashMap<>();

    /**
     * Sets the response responseStatus.
     *
     * @param responseStatus the API response responseStatus
     * @return this builder
     */
    public Builder status(final ApiResponseStatus responseStatus) {
      this.status = responseStatus;
      return this;
    }

    /**
     * Builds a new Data.
     * 
     * @param key {@link String}
     * @param value {@link String}
     * @return Builder
     */
    public Builder data(final String key, final Object value) {
      this.data.put(key, value);
      return this;
    }

    /**
     * Builds a new {@link ApiRequestHandlerResponse} instance.
     *
     * @return built ApiRequestHandlerResponse
     */
    public ApiRequestHandlerResponse build() {
      return new ApiRequestHandlerResponse(this.status, new ApiMapResponse(this.data));
    }
  }
}
