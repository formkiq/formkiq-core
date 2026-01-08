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

import com.formkiq.aws.dynamodb.ApiAuthorization;

/**
 * 
 * Interceptor on the {@link ApiRequestHandlerResponse}.
 *
 */
public interface ApiRequestHandlerInterceptor {

  /**
   * Invoked after the API request has been successfully processed.
   * <p>
   * This method is called after the request has been handled and a response has been generated, but
   * before the response is returned to the client.
   * </p>
   *
   * Implementations may:
   * <ul>
   * <li>Modify or wrap the response</li>
   * <li>Add or adjust response headers</li>
   * <li>Apply post-processing such as auditing or metrics</li>
   * </ul>
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param response {@link ApiRequestHandlerResponse}
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  ApiRequestHandlerResponse afterProcessRequest(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, ApiRequestHandlerResponse response) throws Exception;

  /**
   * Invoked before the API request is processed.
   * <p>
   * This method is called after the request has been received and authorization has been resolved,
   * but before any business logic is executed.
   * </p>
   *
   * Implementations may:
   * <ul>
   * <li>Validate or enrich the incoming request</li>
   * <li>Apply request-scoped context (e.g. correlation IDs)</li>
   * <li>Modify request attributes or headers</li>
   * <li>Reject the request by throwing an exception</li>
   * </ul>
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @throws Exception Exception
   */
  void beforeProcessRequest(ApiGatewayRequestEvent event, ApiAuthorization authorization)
      throws Exception;

  /**
   * Handle / Modify API requests when an exception occurs during request processing.
   * <p>
   * This method is invoked if an exception is thrown after {@link #beforeProcessRequest} and before
   * {@link #afterProcessRequest} is called.
   * </p>
   *
   * Implementations may:
   * <ul>
   * <li>Log or transform the exception</li>
   * <li>Map the exception to an {@link ApiRequestHandlerResponse}</li>
   * <li>Modify headers or response body for error handling</li>
   * </ul>
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param response {@link ApiRequestHandlerResponse}
   * @param exception {@link Exception} thrown during request processing
   */
  default void onProcessRequestException(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, ApiRequestHandlerResponse response, Exception exception) {
    // empty
  }
}
