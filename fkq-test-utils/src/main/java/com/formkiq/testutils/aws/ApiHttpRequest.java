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
package com.formkiq.testutils.aws;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Wrapper for Http Request data.
 *
 */
public class ApiHttpRequest {

  /** Http Body. */
  private String body;

  /** Http Method. */
  private String httpMethod;

  /** Is Request Body Base64 Encoded. */
  private Boolean isBase64Encoded;
  /** Http path. */
  private String path;
  /** {@link Map}. */
  private Map<String, String> pathParameters;
  /** {@link Map}. */
  private Map<String, String> queryStringParameters;
  /** {@link Map}. */
  private Map<String, Object> requestContext;
  /** Http Resource. */
  private String resource;

  /**
   * constructor.
   */
  public ApiHttpRequest() {
    this.requestContext = new HashMap<>();

    Map<String, Object> authorizer = new HashMap<>();
    authorizer.put("claims", new HashMap<>());
    this.requestContext.put("authorizer", authorizer);
  }

  /**
   * Get Http Body.
   * 
   * @return {@link String}
   */
  public String body() {
    return this.body;
  }

  /**
   * Set Http Body.
   * 
   * @param httpBody {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest body(final String httpBody) {
    this.body = httpBody;
    return this;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getClaims() {
    Map<String, Object> authorizer = (Map<String, Object>) this.requestContext.get("authorizer");
    return (Map<String, Object>) authorizer.get("claims");
  }

  /**
   * Set Http Group.
   * 
   * @param group {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest group(final String group) {
    getClaims().put("cognito:groups", "[" + group + "]");
    return this;
  }

  /**
   * Get HTTP method.
   * 
   * @return {@link String}
   */
  public String httpMethod() {
    return this.httpMethod;
  }

  /**
   * Set HTTP method.
   * 
   * @param method {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest httpMethod(final String method) {
    this.httpMethod = method;
    return this;
  }

  /**
   * Is Request Base64 Encoded.
   * 
   * @return {@link Boolean}
   */
  public Boolean isBase64Encoded() {
    return this.isBase64Encoded;
  }

  /**
   * Set Request Base64 Encoded.
   * 
   * @param isBase64 {@link Boolean}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest isBase64Encoded(final Boolean isBase64) {
    this.isBase64Encoded = isBase64;
    return this;
  }

  /**
   * Get HTTP path.
   * 
   * @return {@link String}
   */
  public String path() {
    return this.path;
  }

  /**
   * Set HTTP path.
   * 
   * @param httpPath {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest path(final String httpPath) {
    this.path = httpPath;
    return this;
  }

  /**
   * Get Path Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> pathParameters() {
    return this.pathParameters;
  }

  /**
   * Set Path Parameters.
   * 
   * @param parameters {@link Map}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest pathParameters(final Map<String, String> parameters) {
    this.pathParameters = parameters;
    return this;
  }

  /**
   * Get Query Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> queryParameters() {
    return this.queryStringParameters;
  }

  /**
   * Set Query Parameters.
   * 
   * @param parameters {@link Map}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest queryParameters(final Map<String, String> parameters) {
    this.queryStringParameters = parameters;
    return this;
  }

  /**
   * Get Request Context.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> requestContext() {
    return this.requestContext;
  }

  /**
   * Get HTTP resource.
   * 
   * @return {@link String}
   */
  public String resource() {
    return this.resource;
  }

  /**
   * Set HTTP resource.
   * 
   * @param httpResource {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest resource(final String httpResource) {
    this.resource = httpResource;
    return this;
  }

  /**
   * Set Username.
   * 
   * @param username {@link String}
   * @return {@link ApiHttpRequest}
   */
  public ApiHttpRequest user(final String username) {
    getClaims().put("cognito:username", username);
    return this;
  }
}
