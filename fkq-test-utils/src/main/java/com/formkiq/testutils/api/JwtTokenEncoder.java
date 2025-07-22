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
package com.formkiq.testutils.api;

import io.jsonwebtoken.Jwts;

import java.util.List;
import java.util.Map;

/**
 * 
 * Jwt Token Encoder.
 *
 */
public class JwtTokenEncoder {

  /**
   * Encode Cognito Api Gateway Jwt Token.
   * 
   * @param groups {@link String}
   * @param username {@link String}
   * @return {@link String}
   */
  public static String encodeCognito(final String[] groups, final String username) {
    return Jwts.builder().subject("FormKiQ").claim("cognito:groups", groups)
        .claim("cognito:username", username).compact();
  }

  /**
   * Encode Cognito Api Gateway Jwt Token.
   *
   * @param groups {@link List} {@link String}
   * @param permissions {@link Map}
   * @param username {@link String}
   * @return {@link String}
   */
  public static String encodeExplicitSites(final List<String> groups,
      final Map<String, List<String>> permissions, final String username) {
    Map<String, Object> sitesClaims =
        Map.of("cognito:groups", groups.toArray(new String[0]), "permissionsMap", permissions);
    return Jwts.builder().subject("FormKiQ").claim("sitesClaims", sitesClaims)
        .claim("cognito:username", username).compact();
  }
}
