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

import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMessageResponse;

/** {@link RequestHandler} for GET "/version". */
public class VersionRequestHandler implements RequestHandler {

  /**
   * constructor.
   */
  public VersionRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String key = "/formkiq/" + awsservice.appEnvironment() + "/version";
    String version = awsservice.ssmService().getParameterValue(key);
    ApiMessageResponse resp = new ApiMessageResponse(version);
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
