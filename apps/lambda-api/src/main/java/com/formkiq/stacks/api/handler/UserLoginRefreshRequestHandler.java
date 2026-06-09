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

import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationChecks;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

/** {@link ApiGatewayRequestHandler} for "/login/refresh". */
public class UserLoginRefreshRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserLoginRefreshRequestHandler} URL. */
  public static final String URL = "/login/refresh";

  @Override
  public String getRequestUrl() {
    return URL;
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    return Optional.of(true);
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, Object> map = JsonToObject.fromJson(awsservice, event, Map.class);
    validate(map);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      AuthenticationResultType login = service.refreshToken((String) map.get("refreshToken"));
      Map<String, Object> data = new AuthenticationResultToMap().apply(login);
      return ApiRequestHandlerResponse.builder().ok().body(data).build();

    } catch (NotAuthorizedException e) {
      throw new BadException("Incorrect refresh token");
    }
  }

  private void validate(final Map<String, Object> map) {
    ValidationChecks.checkNotNull("refreshToken", (String) map.get("refreshToken"));
  }
}
