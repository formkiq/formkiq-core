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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/syncs/{service}". */
public class DocumentsSyncsServiceRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentsSyncsServiceRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/syncs/{service}";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    // String siteId = authorizer.getSiteId();
    // String documentId = event.getPathParameters().get("documentId");
    // String userId = getCallingCognitoUsername(event);
    //
    // Map<String, Object> body = fromBodyToMap(logger, event);
    //
    // List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("actions");
    // List<Action> actions = toActions(list, userId);
    //
    // ActionsValidator validator = new ActionsValidatorImpl();
    //
    // List<Collection<ValidationError>> errors = validator.validation(actions);
    //
    // Optional<Collection<ValidationError>> firstError =
    // errors.stream().filter(e -> !e.isEmpty()).findFirst();
    //
    // if (firstError.isEmpty()) {
    //
    // ActionsService service = awsservice.getExtension(ActionsService.class);
    // int idx = service.getActions(siteId, documentId).size();
    //
    // for (Action a : actions) {
    // service.saveAction(siteId, documentId, a, idx);
    // idx++;
    // }

    ApiMapResponse resp = new ApiMapResponse();
    resp.setMap(Map.of("message", "Actions saved"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }
}
