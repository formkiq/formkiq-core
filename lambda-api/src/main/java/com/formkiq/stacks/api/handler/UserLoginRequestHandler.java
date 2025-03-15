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
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.strings.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for "/login". */
public class UserLoginRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserLoginRequestHandler} URL. */
  public static final String URL = "/login";

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, Object> map = fromBodyToMap(event);
    validate(map);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      InitiateAuthResponse login =
          service.loginUserPasswordAuth((String) map.get("username"), (String) map.get("password"));

      Map<String, Object> data = transform(login);
      ApiMapResponse resp = new ApiMapResponse(data);
      return new ApiRequestHandlerResponse(SC_OK, resp);

    } catch (NotAuthorizedException e) {
      throw new BadException("Incorrect username or password");
    }
  }

  private Map<String, Object> transform(final InitiateAuthResponse response) {

    Map<String, Object> result = new HashMap<>();

    result.put("challengeName", response.challengeName());
    result.put("session", response.session());

    AuthenticationResultType login = response.authenticationResult();
    if (login != null) {
      Map<String, Object> authenticationResult = new HashMap<>();
      authenticationResult.put("accessToken", login.accessToken());
      authenticationResult.put("idToken", login.idToken());
      authenticationResult.put("refreshToken", login.refreshToken());
      authenticationResult.put("tokenType", login.tokenType());
      authenticationResult.put("expiresIn", login.expiresIn());
      result.put("authenticationResult", authenticationResult);
    }

    return result;
  }

  private void validate(final Map<String, Object> map) throws ValidationException {
    String username = (String) map.get("username");
    String password = (String) map.get("password");

    if (isEmpty(username) || isEmpty(password)) {
      throw new ValidationException(
          List.of(new ValidationErrorImpl().error("'username' and 'password' are required")));
    }
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    return Optional.of(true);
  }
}
