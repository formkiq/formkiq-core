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
package com.formkiq.testutils.aws;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Jwt Token Encoder.
 *
 */
public class JwtTokenDecoder {

  /** Groups. */
  private List<String> groups;
  /** Username. */
  private String username;

  /**
   * constructor.
   * 
   * @param jwt {@link String}
   */
  @SuppressWarnings("unchecked")
  public JwtTokenDecoder(final String jwt) {
    String[] split = jwt.split("\\.");
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String s = new String(Base64.getDecoder().decode(split[1]), StandardCharsets.UTF_8);
    Map<String, Object> map = gson.fromJson(s, Map.class);

    this.groups = (List<String>) map.get("cognito:groups");
    this.username = (String) map.get("cognito:username");
  }

  /**
   * Get Groups.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getGroups() {
    return this.groups;
  }

  /**
   * Get Username.
   * 
   * @return {@link String}
   */
  public String getUsername() {
    return this.username;
  }

}
