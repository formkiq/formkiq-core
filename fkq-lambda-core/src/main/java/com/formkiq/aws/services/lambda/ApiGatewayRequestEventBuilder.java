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
 * {@link ApiGatewayRequestEvent} Builder.
 *
 */
public class ApiGatewayRequestEventBuilder {

  /** {@link ApiGatewayRequestContext}. */
  private ApiGatewayRequestContext context = new ApiGatewayRequestContext();
  /** {@link ApiGatewayRequestEvent}. */
  private ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();

  /**
   * Set Body.
   * 
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder body(final String body) {
    this.event.setBody(body);
    return this;
  }

  /**
   * Set Body.
   * 
   * @param body {@link String}
   * @param isBase64 boolean
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder body(final String body, final boolean isBase64) {
    this.event.setBody(body);
    this.event.setIsBase64Encoded(Boolean.valueOf(isBase64));
    return this;
  }

  /**
   * Builds {@link ApiGatewayRequestEvent}.
   * 
   * @return {@link ApiGatewayRequestEvent}
   */
  public ApiGatewayRequestEvent build() {
    return this.event;
  }

  /**
   * Set Cognito Group.
   * 
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder group(final String group) {

    if (group != null) {
      this.context.setAuthorizer(Map.of("claims", Map.of("cognito:groups", "[" + group + "]")));
      this.event.setRequestContext(this.context);
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
    this.event.setHttpMethod(method);
    return this;
  }

  /**
   * Set Path.
   * 
   * @param path {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder path(final String path) {
    this.event.setPath(path);
    return this;
  }

  /**
   * Set ̨Path Parameters.
   * 
   * @param map {@link Map}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder pathParameters(final Map<String, String> map) {
    this.event.setPathParameters(map);
    return this;
  }

  /**
   * Set ̨Query Parameters.
   * 
   * @param map {@link Map}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder queryParameters(final Map<String, String> map) {
    this.event.setQueryStringParameters(map);
    return this;
  }

  /**
   * Set Resource.
   * 
   * @param resource {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder resource(final String resource) {
    this.event.setResource(resource);
    return this;
  }

  /**
   * Set User.
   * 
   * @param user {@link String}
   * @return {@link ApiGatewayRequestEventBuilder}
   */
  public ApiGatewayRequestEventBuilder user(final String user) {
    this.context.setIdentity(Map.of("user", user));
    this.event.setRequestContext(this.context);
    return this;
  }
}
