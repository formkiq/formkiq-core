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
package com.formkiq.aws.cognito.transformers;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link Function} to convert {@link AuthenticationResultType} to {@link Map}.
 */
public class AuthenticationResultTypeToMap
    implements Function<AuthenticationResultType, Map<String, Object>> {
  @Override
  public Map<String, Object> apply(final AuthenticationResultType result) {
    Map<String, Object> map = new HashMap<>();

    if (result != null) {
      map.put("accessToken", result.accessToken());
      map.put("idToken", result.idToken());
      map.put("refreshToken", result.refreshToken());
      map.put("tokenType", result.tokenType());
      map.put("expiresIn", result.expiresIn());
    }

    return map;
  }
}
