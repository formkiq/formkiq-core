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
import com.formkiq.aws.cognito.transformers.AuthenticationResultTypeToMap;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.strings.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for "/login/mfa". */
public class UserMfaLoginRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserMfaLoginRequestHandler} URL. */
  public static final String URL = "/login/mfa";

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, Object> map = fromBodyToMap(event);
    validate(map);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      String username = (String) map.get("username");
      String session = (String) map.get("session");
      String softwareTokenMfaCode = (String) map.get("softwareTokenMfaCode");

      RespondToAuthChallengeResponse response =
          service.responseToAuthChallenge(session, "SOFTWARE_TOKEN_MFA",
              Map.of("USERNAME", username, "SOFTWARE_TOKEN_MFA_CODE", softwareTokenMfaCode));

      Map<String, Object> data = transform(response);
      ApiMapResponse resp = new ApiMapResponse(data);
      return new ApiRequestHandlerResponse(SC_OK, resp);

    } catch (NotAuthorizedException e) {
      throw new BadException("Incorrect username or password");
    }
  }

  private Map<String, Object> transform(final RespondToAuthChallengeResponse response) {

    Map<String, Object> result = new HashMap<>();

    AuthenticationResultType login = response.authenticationResult();
    Map<String, Object> authenticationResult = new AuthenticationResultTypeToMap().apply(login);

    if (!authenticationResult.isEmpty()) {
      result.put("authenticationResult", authenticationResult);
    }

    return result;
  }

  private void validate(final Map<String, Object> map) throws ValidationException {
    String username = (String) map.get("username");
    String session = (String) map.get("session");
    String softwareTokenMfaCode = (String) map.get("softwareTokenMfaCode");

    if (isEmpty(username) || isEmpty(session) || isEmpty(softwareTokenMfaCode)) {
      throw new ValidationException(List.of(new ValidationErrorImpl()
          .error("'username', 'session' and 'softwareTokenMfaCode' are required")));
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
