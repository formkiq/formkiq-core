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
package com.formkiq.lambda.apigateway;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.exception.NotFoundException;

/**
 * 
 * API Gateway Request Handler.
 *
 */
public interface ApiGatewayRequestHandler {

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

  /**
   * Is Method a Readonly method.
   * 
   * @param method {@link String}
   * @return boolean
   */
  default boolean isReadonly(final String method) {
    return "get".equals(method) || "head".equals(method);
  }
}
