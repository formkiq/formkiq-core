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
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Api Gateway Request Event.
 *
 */
@Reflectable
public class ApiGatewayRequestEvent {

  /** Request Resource. */
  private String resource;

  /** Request Path. */
  private String path;

  /** Request Method. */
  private String httpMethod;

  /** Request Headers. */
  private Map<String, String> headers;

  /** Request Query Parameters. */
  private Map<String, String> queryStringParameters;

  /** Request Path Parameters. */
  private Map<String, String> pathParameters;

  /** Request Context. */
  private ApiGatewayRequestContext requestContext;

  /** Request Body. */
  private String body;

  /** Is Request Body Base64 Encoded. */
  private Boolean isBase64Encoded;

  /**
   * constructor.
   */
  public ApiGatewayRequestEvent() {}

  /**
   * Add HTTP Header.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  public void addHeader(final String key, final String value) {
    if (this.headers == null) {
      this.headers = new HashMap<>();
    }

    this.headers.put(key, value);
  }

  /**
   * Get Request Body.
   * 
   * @return {@link String}
   */
  public String getBody() {
    return this.body;
  }

  /**
   * Get Request Headers.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * Get Http Method.
   * 
   * @return {@link String}
   */
  public String getHttpMethod() {
    return this.httpMethod;
  }

  /**
   * Is Request Base64 Encoded.
   * 
   * @return {@link Boolean}
   */
  public Boolean getIsBase64Encoded() {
    return this.isBase64Encoded;
  }

  /**
   * Get Request Path.
   * 
   * @return {@link String}
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Get Path Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getPathParameters() {
    return this.pathParameters;
  }

  /**
   * Get Path Parameter.
   * 
   * @param key {@link String}
   * @return String
   */
  public String getPathParameter(final String key) {
    return this.pathParameters != null ? this.pathParameters.get(key) : null;
  }

  /**
   * Get Header Value.
   *
   * @param key {@link String}
   * @return String
   */
  public String getHeaderValue(final String key) {
    return this.headers != null ? this.headers.get(key) : null;
  }

  /**
   * Get Query Parameter.
   *
   * @param key {@link String}
   * @param defaultValue {@link String}
   * @return {@link String}
   */
  public String getQueryStringParameter(final String key, final String defaultValue) {
    return getQueryStringParameters() != null
        ? getQueryStringParameters().getOrDefault(key, defaultValue)
        : null;
  }

  /**
   * Get Query Parameter.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  public String getQueryStringParameter(final String key) {
    return getQueryStringParameter(key, null);
  }

  /**
   * Get Query String Parameters.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getQueryStringParameters() {
    return this.queryStringParameters;
  }

  /**
   * Get Request Context.
   * 
   * @return {@link ApiGatewayRequestContext}
   */
  public ApiGatewayRequestContext getRequestContext() {
    return this.requestContext;
  }

  /**
   * Get Request Resource.
   * 
   * @return {@link String}
   */
  public String getResource() {
    return this.resource;
  }

  /**
   * Set Request Body.
   * 
   * @param requestBody {@link String}
   */
  public void setBody(final String requestBody) {
    this.body = requestBody;
  }

  /**
   * Set Request Headers.
   * 
   * @param map {@link Map}
   */
  public void setHeaders(final Map<String, String> map) {
    this.headers = map;
  }

  /**
   * Set Http Method.
   * 
   * @param method {@link String}
   */
  public void setHttpMethod(final String method) {
    this.httpMethod = method;
  }

  /**
   * Set Request Base64 Encoded.
   * 
   * @param isBase64 {@link Boolean}
   */
  public void setIsBase64Encoded(final Boolean isBase64) {
    this.isBase64Encoded = isBase64;
  }

  /**
   * Set Request Path.
   * 
   * @param requestPath {@link String}
   */
  public void setPath(final String requestPath) {
    this.path = requestPath;
  }

  /**
   * Set Path Parameters.
   * 
   * @param map {@link Map}
   */
  public void setPathParameters(final Map<String, String> map) {
    this.pathParameters = map;
  }

  /**
   * Set Query String Parameters.
   * 
   * @param map {@link Map}
   */
  public void setQueryStringParameters(final Map<String, String> map) {
    this.queryStringParameters = map;
  }

  /**
   * Set Request Context.
   * 
   * @param context {@link ApiGatewayRequestContext}
   */
  public void setRequestContext(final ApiGatewayRequestContext context) {
    this.requestContext = context;
  }

  /**
   * Set Resource.
   * 
   * @param requestResource {@link String}
   */
  public void setResource(final String requestResource) {
    this.resource = requestResource;
  }
}
