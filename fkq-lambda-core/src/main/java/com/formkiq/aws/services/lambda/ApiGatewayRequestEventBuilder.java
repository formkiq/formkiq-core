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

/**
 * 
 * {@link ApiGatewayRequestEvent} Builder.
 *
 */
public class ApiGatewayRequestEventBuilder {

  /** Http Resource. */
  private String resource;
  /** Http Path. */
  private String path;
  /** Http Method. */
  private String httpMethod;
  /** Http Headers. */
  private Map<String, String> headers = new HashMap<>();
  /** Http Query Parameters. */
  private Map<String, String> queryStringParameters = new HashMap<>();
  /** Http Path Parameters. */
  private Map<String, String> pathParameters = new HashMap<>();
  /** Http Body. */
  private String body;
  /** Http Body Base 64. */
  private Boolean isBase64Encoded = Boolean.FALSE;
  /** Claims. */
  private Map<String, String> claims;
  /** Http User. */
  private String user;

  /**
   * Set Body.
   * 
   * @param eventBody {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder body(final String eventBody) {
    this.body = eventBody;
    return this;
  }

  /**
   * Set Body.
   * 
   * @param eventBody {@link String}
   * @param isBase64 boolean
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder body(final String eventBody, final boolean isBase64) {
    this.body = eventBody;
    this.isBase64Encoded = isBase64;
    return this;
  }

  /**
   * Builds a new immutable {@link ApiGatewayRequestEvent}.
   *
   * @return {@link ApiGatewayRequestEvent}
   */
  public ApiGatewayRequestEvent build() {

    ApiGatewayRequestContext requestContext = new ApiGatewayRequestContext();
    requestContext.setAuthorizer(Map.of("claims", this.claims));
    requestContext.setIdentity(Map.of("user", user));

    ApiGatewayRequestEvent e = new ApiGatewayRequestEvent();
    e.setResource(resource);
    e.setPath(path);
    e.setHttpMethod(httpMethod);
    e.setHeaders(headers != null ? Map.copyOf(headers) : Map.of());
    e.setQueryStringParameters(
        queryStringParameters != null ? Map.copyOf(queryStringParameters) : Map.of());
    e.setPathParameters(pathParameters != null ? Map.copyOf(pathParameters) : Map.of());
    e.setRequestContext(requestContext);
    e.setBody(this.body);
    e.setIsBase64Encoded(isBase64Encoded != null ? isBase64Encoded : Boolean.FALSE);
    return e;
  }

  /**
   * Set Cognito Group.
   * 
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder group(final String group) {

    if (group != null) {
      this.claims = Map.of("cognito:groups", "[" + group + "]");
    }

    return this;
  }

  /**
   * Set Method.
   * 
   * @param method {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder method(final String method) {
    this.httpMethod = method;
    return this;
  }

  /**
   * Set Path.
   * 
   * @param httpPath {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder path(final String httpPath) {
    this.path = httpPath;
    return this;
  }

  /**
   * Set ̨Path Parameters.
   * 
   * @param map {@link Map}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder pathParameters(final Map<String, String> map) {
    this.pathParameters = map;
    return this;
  }

  /**
   * Set ̨Query Parameters.
   * 
   * @param map {@link Map}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder queryParameters(final Map<String, String> map) {
    this.queryStringParameters = map;
    return this;
  }

  /**
   * Set Resource.
   * 
   * @param httpResource {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder resource(final String httpResource) {
    this.resource = httpResource;
    return this;
  }

  /**
   * Set User.
   * 
   * @param httpUser {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder user(final String httpUser) {
    this.user = httpUser;
    // this.context.setIdentity(Map.of("user", user));
    // this.event.setRequestContext(this.context);
    return this;
  }
}
