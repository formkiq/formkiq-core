/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import java.util.HashMap;
import java.util.Map;
import com.formkiq.stacks.api.ApiResponse;

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
