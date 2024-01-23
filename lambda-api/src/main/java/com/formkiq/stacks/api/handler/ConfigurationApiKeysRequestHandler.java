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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKey;
import com.formkiq.stacks.dynamodb.ApiKeyPermission;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/configuration/apiKeys". */
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
    checkPermissions(authorization);
  }

  @Override
  public void beforePost(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorization);
  }

  private void checkPermissions(final ApiAuthorization authorization) throws UnauthorizedException {
    if (!authorization.getPermissions().contains(ApiPermission.ADMIN)) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    final int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    final PaginationMapToken token = pagination != null ? pagination.getStartkey() : null;

    String siteId = authorization.getSiteId();
    PaginationResults<ApiKey> list = apiKeysService.list(siteId, token, limit);

    Map<String, Object> map = Map.of("apiKeys", list.getResults());
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/configuration/apiKeys";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();

    ApiKeysService apiKeysService = awsservice.getExtension(ApiKeysService.class);
    DynamicObject body = fromBodyToDynamicObject(event);
    validate(body);

    String name = body.get("name").toString();
    Collection<ApiKeyPermission> permissions = Collections.emptyList();

    if (body.containsKey("permissions")) {
      permissions = body.getStringList("permissions").stream()
          .map(p -> ApiKeyPermission.valueOf(p.toUpperCase())).collect(Collectors.toList());
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
