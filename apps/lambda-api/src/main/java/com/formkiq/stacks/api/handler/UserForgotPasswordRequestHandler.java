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
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.strings.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for "/forgotPassword". */
public class UserForgotPasswordRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link UserForgotPasswordRequestHandler} URL. */
  public static final String URL = "/forgotPassword";
  /** Temporary password length. */
  private static final int TEMP_PASSWORD_LENGTH = 32;
  /** Lowercase chars. */
  private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
  /** Uppercase chars. */
  private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  /** Numeric chars. */
  private static final String NUMBERS = "23456789";
  /** Symbol chars. */
  private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}";
  /** Allowed chars. */
  private static final String ALL = LOWERCASE + UPPERCASE + NUMBERS + SYMBOLS;
  /** Secure random generator. */
  private static final SecureRandom RANDOM = new SecureRandom();

  private String generateTemporaryPassword() {
    List<Character> chars = new ArrayList<>();
    chars.add(randomChar(LOWERCASE));
    chars.add(randomChar(UPPERCASE));
    chars.add(randomChar(NUMBERS));
    chars.add(randomChar(SYMBOLS));

    while (chars.size() < TEMP_PASSWORD_LENGTH) {
      chars.add(randomChar(ALL));
    }

    Collections.shuffle(chars, RANDOM);

    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (Character c : chars) {
      sb.append(c.charValue());
    }

    return sb.toString();
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

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, Object> map = JsonToObject.fromJson(awsservice, event, Map.class);
    validate(map);

    CognitoIdentityProviderService service =
        awsservice.getExtension(CognitoIdentityProviderService.class);

    String username = (String) map.get("username");
    sendPasswordReset(service, username);

    return ApiRequestHandlerResponse.builder().ok().body("message", "Password reset sent").build();
  }

  private char randomChar(final String chars) {
    return chars.charAt(RANDOM.nextInt(chars.length()));
  }

  private void sendPasswordReset(final CognitoIdentityProviderService service,
      final String username) {
    try {
      String userStatus = service.getUser(username).userStatusAsString();
      if ("FORCE_CHANGE_PASSWORD".equals(userStatus)) {
        service.setUserPassword(username, generateTemporaryPassword(), true);
      }

      service.forgotPassword(username);
    } catch (UserNotFoundException e) {
      // ignore
    }
  }

  private void validate(final Map<String, Object> map) throws ValidationException {
    String username = (String) map.get("username");

    if (isEmpty(username)) {
      throw new ValidationException(
          List.of(new ValidationErrorImpl().error("'username' are required")));
    }
  }
}
