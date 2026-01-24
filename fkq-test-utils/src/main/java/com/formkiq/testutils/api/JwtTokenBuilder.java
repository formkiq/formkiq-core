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

import com.formkiq.client.invoker.ApiClient;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jwt Token Builder.
 */
public class JwtTokenBuilder {

  /** {@link JwtBuilder}. */
  private final JwtBuilder jwt;
  /** Site Claims. */
  private final Map<String, Object> sitesClaims = new HashMap<>();

  /**
   * constructor.
   * 
   * @param username {@link String}
   */
  public JwtTokenBuilder(final String username) {
    jwt = Jwts.builder().subject("FormKiQ").claim("cognito:username", username);
  }

  /**
   * Build Token and add Authorization Header.
   * 
   * @param client {@link ApiClient}
   */
  public void build(final ApiClient client) {
    String token = jwt.compact();
    client.addDefaultHeader("Authorization", token);
  }

  /**
   * Add Groups.
   * 
   * @param group {@link String}
   * @return {@link JwtBuilder}
   */
  public JwtTokenBuilder group(final String group) {
    jwt.claim("cognito:groups", new String[] {group});
    return this;
  }

  /**
   * Add Groups.
   * 
   * @param groups {@link List} {@link String}
   * @return {@link JwtTokenBuilder}
   */
  public JwtTokenBuilder groups(final List<String> groups) {
    jwt.claim("cognito:groups", groups.toArray(new String[0]));
    return this;
  }

  /**
   * Add Groups.
   * 
   * @param groups {@link String}
   * @return {@link JwtBuilder}
   */
  public JwtTokenBuilder groups(final String[] groups) {
    jwt.claim("cognito:groups", groups);
    return this;
  }

  /**
   * Add Groups.
   * 
   * @param groups {@link List} {@link String}
   * @return {@link JwtTokenBuilder}
   */
  public JwtTokenBuilder samlGroups(final List<String> groups) {
    jwt.claim("samlGroups", groups.toArray(new String[0]));
    return this;
  }
}
