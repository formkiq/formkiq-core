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

import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.module.httpsigv4.HttpServiceSigv4;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit Tests for request /login, /changePassword, /resetPassword, /confirmRegistration. */
public class CognitoAuthRequestTest extends AbstractApiClientRequestTest {

  /** {@link HttpService}. */
  private final HttpService http = new HttpServiceJdk11();
  /** {@link HttpService}. */
  private final HttpService httpSigv4 =
      new HttpServiceSigv4(
          Region.US_EAST_2, StaticCredentialsProvider
              .create(AwsBasicCredentials.create("123", "123")).resolveCredentials(),
          "execute-api");

  /**
   * POST /login missing parameters.
   *
   */
  @Test
  public void testLogin01() throws IOException {
    // given
    String url = server.getBasePath() + "/login";

    // when
    HttpResponse<String> post = http.post(url, Optional.empty(), Optional.empty(), "");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"message\":\"request body is required\"}", post.body());

    // when
    post = http.post(url, Optional.empty(), Optional.empty(), "{}");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"errors\":[{\"error\":\"'username' and 'password' are required\"}]}",
        post.body());
  }

  /**
   * POST /changePassword missing parameters.
   *
   */
  @Test
  public void testChangePassword01() throws IOException {
    // given
    String url = server.getBasePath() + "/changePassword";

    // when
    HttpResponse<String> post = httpSigv4.post(url, Optional.empty(), Optional.empty(), "");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"message\":\"request body is required\"}", post.body());

    // when
    post = httpSigv4.post(url, Optional.empty(), Optional.empty(), "{}");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"errors\":[{\"error\":\"'oldPassword' and 'newPassword' are required\"}]}",
        post.body());
  }

  /**
   * POST /forgotPassword missing parameters.
   *
   */
  @Test
  public void testForgotPassword01() throws IOException {
    // given
    String url = server.getBasePath() + "/forgotPassword";

    // when
    HttpResponse<String> post = http.post(url, Optional.empty(), Optional.empty(), "");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"message\":\"request body is required\"}", post.body());

    // when
    post = http.post(url, Optional.empty(), Optional.empty(), "{}");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"errors\":[{\"error\":\"'username' are required\"}]}", post.body());
  }

  /**
   * POST /forgotPasswordConfirm missing parameters.
   *
   */
  @Test
  public void testForgotPasswordConfirm01() throws IOException {
    // given
    String url = server.getBasePath() + "/forgotPasswordConfirm";

    // when
    HttpResponse<String> post = http.post(url, Optional.empty(), Optional.empty(), "");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"message\":\"request body is required\"}", post.body());

    // when
    post = http.post(url, Optional.empty(), Optional.empty(), "{}");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"errors\":[{\"error\":\"'username', 'code', 'password' are required\"}]}",
        post.body());
  }

  /**
   * POST /confirmRegistration missing parameters.
   *
   */
  @Test
  public void testConfirmRegistration01() throws IOException {
    // given
    String url = server.getBasePath() + "/confirmRegistration";

    // when
    HttpResponse<String> post = http.post(url, Optional.empty(), Optional.empty(), "");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"message\":\"request body is required\"}", post.body());

    // when
    post = http.post(url, Optional.empty(), Optional.empty(), "{}");

    // then
    assertEquals("400", String.valueOf(post.statusCode()));
    assertEquals("{\"errors\":[{\"error\":\"'username' and 'code' are required\"}]}", post.body());
  }
}
