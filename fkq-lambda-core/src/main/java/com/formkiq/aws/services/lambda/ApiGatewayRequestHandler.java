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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/**
 * 
 * API Gateway Request Handler.
 *
 */
public interface ApiGatewayRequestHandler {

  /**
   * Is caller Authorized to continue.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param method {@link String}
   * @return boolean
   */
  default boolean isAuthorized(AwsServiceCache awsServiceCache, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, String method) {
    return "options".equals(method) || hasAccess(method, event.getPath(), authorizer);
  }

  /**
   * Whether {@link ApiGatewayRequestEvent} has access.
   * 
   * @param method {@link String}
   * @param path {@link String}
   * @param authorizer {@link ApiAuthorizer}
   * @return boolean
   */
  default boolean hasAccess(String method, String path, ApiAuthorizer authorizer) {

    boolean access = false;

    if (authorizer.isCallerAssumeRole() || authorizer.isCallerIamUser() || authorizer.isUserAdmin()
        || path.startsWith("/public/")) {

      access = true;

    } else if ((isReadonly(method) && authorizer.isUserReadAccess())
        || authorizer.isUserWriteAccess()) {

      access = true;
    }

    return access;
  }

  /**
   * Called Before "delete" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeDelete(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }


  /**
   * Called Before "get" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeGet(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "head" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeHead(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "patch" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePatch(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "post" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePost(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "put" method is called.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePut(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * DELETE Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse delete(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * Get Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse get(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * Get Request Url.
   * 
   * @return {@link String}
   */
  String getRequestUrl();

  /**
   * Head Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse head(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * Is Method a Readonly method.
   * 
   * @param method {@link String}
   * @return boolean
   */
  default boolean isReadonly(final String method) {
    return "get".equals(method) || "head".equals(method);
  }

  /**
   * OPTIONS Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse options(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * PATCH Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse patch(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * POST Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse post(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * PUT Request Handler.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse put(LambdaLogger logger, ApiGatewayRequestEvent event,
      ApiAuthorizer authorizer, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }
}
