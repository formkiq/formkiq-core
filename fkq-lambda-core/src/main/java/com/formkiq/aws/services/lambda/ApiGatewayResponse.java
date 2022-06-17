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

import java.util.Map;

/**
 * 
 * API Gateway Response.
 *
 */
public class ApiGatewayResponse {
  /** Headers. */
  private Map<String, String> headers;
  /** Response Body. */
  private String body;
  /** Response Status Code. */
  private int statusCode;

  /**
   * constructor.
   */
  public ApiGatewayResponse() {

  }

  /**
   * Get Body.
   * 
   * @return {@link String}
   */
  public String getBody() {
    return this.body;
  }

  /**
   * Get Headers.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * Get Status Code.
   * 
   * @return int
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Set Body.
   * 
   * @param s {@link String}
   */
  public void setBody(final String s) {
    this.body = s;
  }

  /**
   * Set Headers.
   * 
   * @param map {@link Map}
   */
  public void setHeaders(final Map<String, String> map) {
    this.headers = map;
  }

  /**
   * Set Status Code.
   * 
   * @param status int
   */
  public void setStatusCode(final int status) {
    this.statusCode = status;
  }
}
