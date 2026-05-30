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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.MOVED_PERMANENTLY;
import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.strings.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for "/confirmRegistration". */
public class UserConfirmRegistrationRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserConfirmRegistrationRequestHandler} URL. */
  public static final String URL = "/confirmRegistration";

  private ApiRequestHandlerResponse buildRedirect(final String redirectUri, final boolean success,
      final String username, final String userStatus, final String code, final String session) {

    StringBuilder sb = new StringBuilder();
    sb.append(redirectUri);
    sb.append(redirectUri.contains("?") ? "&" : "?");
    sb.append("success=").append(success);
    sb.append("&username=").append(encode(username));
    sb.append("&userStatus=").append(encode(userStatus));
    sb.append("&code=").append(encode(code));
    sb.append("&session=").append(encode(session));

    return ApiRequestHandlerResponse.builder().status(MOVED_PERMANENTLY)
        .header("Location", sb.toString()).build();
  }

  private String encode(final String value) {
    return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8).replace("+",
        "%20");
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    validate(event);

    String redirectUri = getRedirectUri(event, awsservice);
    String username = getParameter(event, "username");
    String code = getParameter(event, "code");
    Map<String, Object> response = new HashMap<>();
    response.put("message", "User registration confirmed");
    response.put("success", Boolean.TRUE);
    response.put("username", username);

    String userStatus = getParameter(event, "userStatus");
    if (!isEmpty(userStatus)) {
      response.put("userStatus", userStatus);
    }

    String session = "";
    try {
      CognitoIdentityProviderService service =
          awsservice.getExtension(CognitoIdentityProviderService.class);
      var auth = service.loginResponse(username, code);
      if (auth.challengeName() != null) {
        String challengeName = auth.challengeName().name();
        response.put("challengeName", challengeName);

        if ("NEW_PASSWORD_REQUIRED".equals(challengeName)) {
          userStatus = challengeName;
          response.put("userStatus", challengeName);
        }
      }

      if (!isEmpty(auth.session())) {
        session = auth.session();
        response.put("session", session);
      }

      ApiRequestHandlerResponse handlerResponse;
      if (!isEmpty(redirectUri)) {
        handlerResponse = buildRedirect(redirectUri, true, username, userStatus, code, session);
      } else {
        handlerResponse = ApiRequestHandlerResponse.builder().ok().body(response).build();
      }

      return handlerResponse;
    } catch (Exception e) {
      if (!isEmpty(redirectUri)) {
        return buildRedirect(redirectUri, false, username, userStatus, code, session);
      }
      throw e;
    }
  }

  private String getRedirectUri(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice) {
    String consoleUrlParam =
        "/formkiq/" + awsservice.environment("APP_ENVIRONMENT") + "/console/Url";
    String redirectUri =
        awsservice.getExtension(SsmService.class).getParameterValue(consoleUrlParam);
    if (isEmpty(redirectUri)) {
      throw new BadException("Missing required SSM parameter: " + consoleUrlParam);
    }

    String requestedRedirect = getParameter(event, "redirect_uri");
    if (!isEmpty(requestedRedirect)) {
      String decodedRedirect = URLDecoder.decode(requestedRedirect, StandardCharsets.UTF_8);
      if (decodedRedirect.startsWith(redirectUri)) {
        redirectUri = decodedRedirect;
      }
    }

    return redirectUri;
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

  private void validate(final ApiGatewayRequestEvent event) throws ValidationException {
    String username = getParameter(event, "username");
    String code = getParameter(event, "code");

    if (isEmpty(username) || isEmpty(code)) {
      throw new ValidationException(
          List.of(new ValidationErrorImpl().error("'username' and 'code' are required")));
    }
  }
}
