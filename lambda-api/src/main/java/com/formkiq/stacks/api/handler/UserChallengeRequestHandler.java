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

/** {@link ApiGatewayRequestHandler} for "/challenge". */
public class UserChallengeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserChallengeRequestHandler} URL. */
  public static final String URL = "/challenge";

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, Object> map = fromBodyToMap(event);
    return post(awsservice, map);
  }

  protected ApiRequestHandlerResponse post(final AwsServiceCache awsservice,
      final Map<String, Object> map) throws ValidationException, BadException {
    validate(map);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    try {
      String challengeName = (String) map.get("challengeName");

      Map<String, String> challengeResponses = createChallengeResponse(map);
      String session = (String) map.get("session");

      RespondToAuthChallengeResponse response =
          service.responseToAuthChallenge(session, challengeName, challengeResponses);

      Map<String, Object> data = transform(response);
      ApiMapResponse resp = new ApiMapResponse(data);
      return new ApiRequestHandlerResponse(SC_OK, resp);

    } catch (NotAuthorizedException e) {
      throw new BadException("Incorrect username or password");
    }
  }

  private Map<String, String> createChallengeResponse(final Map<String, Object> map) {
    String username = (String) map.get("username");
    String softwareTokenMfaCode = (String) map.get("softwareTokenMfaCode");
    String newPassword = (String) map.get("newPassword");

    Map<String, String> challengeResponses = new HashMap<>();
    challengeResponses.put("USERNAME", username);
    challengeResponses.put("SOFTWARE_TOKEN_MFA_CODE", softwareTokenMfaCode);
    challengeResponses.put("NEW_PASSWORD", newPassword);
    return challengeResponses;
  }

  private Map<String, Object> transform(final RespondToAuthChallengeResponse response) {

    Map<String, Object> result = new HashMap<>();

    result.put("challengeName", response.challengeName());
    result.put("session", response.session());

    AuthenticationResultType login = response.authenticationResult();
    Map<String, Object> authenticationResult = new AuthenticationResultTypeToMap().apply(login);

    if (!authenticationResult.isEmpty()) {
      result.put("authenticationResult", authenticationResult);
    }

    return result;
  }

  private void validate(final Map<String, Object> map) throws ValidationException {

    String challengeName = (String) map.get("challengeName");
    if (isEmpty(challengeName)) {
      throw new ValidationException(
          List.of(new ValidationErrorImpl().error("'challengeName' is required")));
    }

    switch (challengeName) {
      case "SOFTWARE_TOKEN_MFA" -> validateSoftwareToken(map);
      case "NEW_PASSWORD_REQUIRED" -> validateNewPasswordRequired(map);
      default -> throw new ValidationException(List.of(
          new ValidationErrorImpl().error("Unsupported challengeName '" + challengeName + "'")));
    }
  }

  private void validateSoftwareToken(final Map<String, Object> map) throws ValidationException {
    String username = (String) map.get("username");
    String session = (String) map.get("session");
    String softwareTokenMfaCode = (String) map.get("softwareTokenMfaCode");

    if (isEmpty(username) || isEmpty(session) || isEmpty(softwareTokenMfaCode)) {
      throw new ValidationException(List.of(new ValidationErrorImpl()
          .error("'username', 'session' and 'softwareTokenMfaCode' are required")));
    }
  }

  private void validateNewPasswordRequired(final Map<String, Object> map)
      throws ValidationException {
    String username = (String) map.get("username");
    String session = (String) map.get("session");
    String newPassword = (String) map.get("newPassword");

    if (isEmpty(username) || isEmpty(session) || isEmpty(newPassword)) {
      throw new ValidationException(List.of(
          new ValidationErrorImpl().error("'username', 'session' and 'newPassword' are required")));
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
