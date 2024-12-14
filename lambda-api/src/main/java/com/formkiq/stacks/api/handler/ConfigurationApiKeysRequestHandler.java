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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKey;
import com.formkiq.stacks.dynamodb.ApiKeyPermission;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/apiKeys". */
public class ConfigurationApiKeysRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public ConfigurationApiKeysRequestHandler() {}

  @Override
  public void beforeGet(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(event, authorization);
  }

  @Override
  public void beforePost(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
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
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);

    int limit = getLimit(logger, event);
    String nextToken = event.getQueryStringParameter("next");

    String siteId = event.getPathParameters().get("siteId");
    Pagination<ApiKey> list = apiKeysService.list(siteId, nextToken, limit);

    Map<String, Object> map = Map.of("apiKeys", list.getResults());
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map, list.getNextToken()));
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/apiKeys";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = event.getPathParameters().get("siteId");

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    DynamicObject body = fromBodyToDynamicObject(event);
    validate(body);

    String name = body.get("name").toString();
    Collection<ApiKeyPermission> permissions;

    List<String> permissionList = body.getStringList("permissions");
    if (!Objects.isEmpty(permissionList)) {
      permissions = permissionList.stream().map(p -> ApiKeyPermission.valueOf(p.toUpperCase()))
          .collect(Collectors.toList());
    } else {
      permissions =
          Arrays.asList(ApiKeyPermission.READ, ApiKeyPermission.WRITE, ApiKeyPermission.DELETE);
    }

    String userId = authorization.getUsername();
    String apiKey = apiKeysService.createApiKey(siteId, name, permissions, userId);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("apiKey", apiKey)));
  }

  /**
   * Validate.
   *
   * @param body {@link Map}
   * @throws ValidationException ValidationException
   */
  private void validate(final Map<String, Object> body) throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    if (!body.containsKey("name")) {
      errors.add(new ValidationErrorImpl().key("name").error("is required"));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
