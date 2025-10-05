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
package com.formkiq.stacks.api.handler.groups;

import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.GroupsResponseToMap;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;

import java.util.Map;
import java.util.Optional;

/** {@link ApiGatewayRequestHandler} for "/groups/{groupName}". */
public class GroupRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link GroupRequestHandler} URL. */
  public static final String URL = "/groups/{groupName}";

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String groupName = event.getPathParameter("groupName");
    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);
    service.deleteGroup(groupName);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Group " + groupName + " deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    String groupName = event.getPathParameter("groupName");
    GetGroupResponse response = service.getGroup(groupName);

    Map<String, Object> group = new GroupsResponseToMap().apply(response.group());
    return ApiRequestHandlerResponse.builder().ok().body("group", group).build();
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.ADMIN);
    return !"get".equalsIgnoreCase(method) ? Optional.of(access) : Optional.empty();
  }
}
