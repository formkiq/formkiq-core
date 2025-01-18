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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKeysService;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/apiKeys/{apiKey}". */
public class ConfigurationApiKeyRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public ConfigurationApiKeyRequestHandler() {}

  @Override
  public void beforeDelete(final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {
    checkPermissions(event, authorization);
  }

  private void checkPermissions(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws UnauthorizedException {
    String siteId = event.getPathParameters().get("siteId");
    if (!authorization.getPermissions(siteId).contains(ApiPermission.ADMIN)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = event.getPathParameters().get("siteId");
    String apiKey = event.getPathParameters().get("apiKey");

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    boolean deleteApiKey = apiKeysService.deleteApiKey(siteId, apiKey);

    if (!deleteApiKey) {
      throw new NotFoundException("apikey '" + apiKey + "' not found");
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("message", "ApiKey deleted")));
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/apiKeys/{apiKey}";
  }
}
