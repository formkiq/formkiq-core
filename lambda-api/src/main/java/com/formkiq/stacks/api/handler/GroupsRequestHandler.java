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

import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.GroupNameComparator;
import com.formkiq.stacks.api.transformers.GroupsResponseToMap;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/groups". */
public class GroupsRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Limit. */
  private static final int DEFAULT_LIMIT = 10;
  /** {@link GroupsRequestHandler} URL. */
  public static final String URL = "/groups";

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.ADMIN);
    return !"get".equalsIgnoreCase(method) ? Optional.of(access) : Optional.empty();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String token = event.getQueryStringParameter("next");
    String limitS = event.getQueryStringParameter("limit");
    int limit = limitS != null ? Integer.parseInt(limitS) : DEFAULT_LIMIT;

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      ListGroupsResponse response = service.listGroups(token, limit);

      List<Map<String, Object>> groups = response.groups().stream()
          .sorted(new GroupNameComparator()).map(new GroupsResponseToMap()).toList();

      Map<String, Object> map = new HashMap<>();
      map.put("groups", groups);
      map.put("next", response.nextToken());

      ApiMapResponse resp = new ApiMapResponse(map);
      return new ApiRequestHandlerResponse(SC_OK, resp);
    } catch (InvalidParameterException e) {
      throw new BadException(e.getMessage());
    }
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    AddGroupRequest request = fromBodyToObject(event, AddGroupRequest.class);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      service.addGroup(request.getGroupName(), request.getGroupDescription());
    } catch (InvalidParameterException e) {
      throw new BadException(e.getMessage());
    }

    ApiMapResponse resp =
        new ApiMapResponse(Map.of("message", "Group " + request.getGroupName() + " created"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }
}
