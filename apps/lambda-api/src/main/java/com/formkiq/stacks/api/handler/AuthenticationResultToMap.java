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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/** {@link Function} for converting {@link AuthenticationResultType} to a response body. */
public class AuthenticationResultToMap
    implements Function<AuthenticationResultType, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(final AuthenticationResultType login) {
    Map<String, Object> authenticationResult = new HashMap<>();
    authenticationResult.put("AccessToken", login.accessToken());
    authenticationResult.put("IdToken", login.idToken());
    authenticationResult.put("RefreshToken", login.refreshToken());
    authenticationResult.put("TokenType", login.tokenType());
    authenticationResult.put("ExpiresIn", login.expiresIn());
    return Map.of("AuthenticationResult", authenticationResult);
  }
}
