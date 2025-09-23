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

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.client.invoker.ApiClient;

import java.util.Arrays;
import java.util.function.BiFunction;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link BiFunction} to setup SetBearer.
 */
public class SetBearers implements BiFunction<ApiClient, String[], Void> {
  /** Default Username. */
  private static final String DEFAULT_USERNAME = "joesmith";
  /** Username. */
  private final String user;

  /** Username. */
  public SetBearers() {
    this(DEFAULT_USERNAME);
  }

  /**
   * Username.
   * 
   * @param username {@link String}
   */
  public SetBearers(final String username) {
    this.user = !isEmpty(username) ? username : DEFAULT_USERNAME;
  }


  @Override
  public Void apply(final ApiClient client, final String[] groups) {

    String[] a = Arrays.stream(groups).map(m -> m != null ? m : SiteIdKeyGenerator.DEFAULT_SITE_ID)
        .toArray(String[]::new);
    String jwt = JwtTokenEncoder.encodeCognito(a, user);
    client.addDefaultHeader("Authorization", jwt);
    return null;
  }
}
