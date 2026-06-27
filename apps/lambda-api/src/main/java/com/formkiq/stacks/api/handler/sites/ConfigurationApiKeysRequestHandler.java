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
package com.formkiq.stacks.api.handler.sites;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.AdminRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKey;
import com.formkiq.stacks.dynamodb.ApiKeyPermission;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/apiKeys". */
public class ConfigurationApiKeysRequestHandler
    implements AdminRequestHandler, ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public ConfigurationApiKeysRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);

    int limit = getLimit(awsservice.getLogger(), event);
    String nextToken = event.getQueryStringParameter("next");

    String siteId = getPathParameterSiteId(event);
    Pagination<ApiKey> list = apiKeysService.list(siteId, nextToken, limit);

    AttributeValueToMapConfig config =
        AttributeValueToMapConfig.builder().removeDbKeys(true).build();
    Map<String, Object> map = Map.of("apiKeys", list.getResults().stream()
        .map(a -> new AttributeValueToMap(config).apply(a.getAttributes())).toList());
    return ApiRequestHandlerResponse.builder().ok().body(map).next(list.getNextToken()).build();
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/apiKeys";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = getPathParameterSiteId(event);

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    AddApiKeyRequest req = JsonToObject.fromJson(awsservice, event, AddApiKeyRequest.class);
    validate(req);

    Collection<ApiKeyPermission> permissions = req.permissions();
    if (Objects.isEmpty(permissions)) {
      permissions =
          Arrays.asList(ApiKeyPermission.READ, ApiKeyPermission.WRITE, ApiKeyPermission.DELETE);
    }

    String apiKey = apiKeysService.createApiKey(siteId, req.name(), permissions, req.groups());

    return ApiRequestHandlerResponse.builder().ok().body("apiKey", apiKey).build();
  }

  /**
   * Validate.
   *
   * @param req {@link AddApiKeyRequest}
   * @throws ValidationException ValidationException
   */
  private void validate(final AddApiKeyRequest req) throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("name", req.name());
    vb.check();
  }
}
