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

import java.util.Optional;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/**
 * 
 * API Gateway Request Handler.
 *
 */
public interface ApiGatewayRequestHandler {

  /**
   * Called Before "delete" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeDelete(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "get" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeGet(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "head" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforeHead(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }


  /**
   * Called Before "patch" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePatch(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "post" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePost(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }

  /**
   * Called Before "put" method is called.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * @throws Exception Exception
   */
  default void beforePut(ApiGatewayRequestEvent event, ApiAuthorization authorization,
      AwsServiceCache awsServices) throws Exception {
    // empty
  }

  default void checkPermission(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final ApiPermission permission)
      throws UnauthorizedException {
    String siteId = event.getPathParameter("siteId");
    if (!authorization.getPermissions(siteId).contains(permission)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  /**
   * DELETE Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse delete(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * Get Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse get(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
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
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse head(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * Authorization override for {@link ApiGatewayRequestHandler}.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   * @param method {@link String}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @return {@link Optional} {@link Boolean}
   * @throws Exception Exception
   */
  default Optional<Boolean> isAuthorized(AwsServiceCache awsServiceCache, String method,
      ApiGatewayRequestEvent event, ApiAuthorization authorization) throws Exception {
    return Optional.empty();
  }

  /**
   * OPTIONS Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse options(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * PATCH Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse patch(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * POST Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse post(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }

  /**
   * PUT Request Handler.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsServices {@link AwsServiceCache}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * 
   * @throws Exception Exception
   */
  default ApiRequestHandlerResponse put(ApiGatewayRequestEvent event,
      ApiAuthorization authorization, AwsServiceCache awsServices) throws Exception {
    throw new NotFoundException(
        event.getHttpMethod() + " for " + event.getResource() + " not found");
  }
}
