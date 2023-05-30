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
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKeysService;

/** {@link ApiGatewayRequestHandler} for "/configuration/apiKeys". */
public class ConfigurationApiKeysRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public ConfigurationApiKeysRequestHandler() {}

  @Override
  public void beforeDelete(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorizer);
  }

  @Override
  public void beforeGet(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorizer);
  }

  @Override
  public void beforePost(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorizer);
  }

  private void checkPermissions(final ApiAuthorizer authorizer) throws UnauthorizedException {
    if (!authorizer.isUserAdmin()) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    String apiKey = event.getQueryStringParameter("apiKey");

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    apiKeysService.deleteApiKey(siteId, apiKey);

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "ApiKey deleted")));
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);

    List<DynamicObject> list = apiKeysService.list(siteId);

    Map<String, Object> map = Map.of("apiKeys", list);
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/configuration/apiKeys";
  }

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    Map<String, String> body = fromBodyToObject(logger, event, Map.class);
    String name = body.get("name");

    if (name != null) {
      String apiKey = apiKeysService.createApiKey(siteId, name);
      return new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("name", name, "apiKey", apiKey)));
    }

    throw new BadException("missing required body parameters");
  }
}
