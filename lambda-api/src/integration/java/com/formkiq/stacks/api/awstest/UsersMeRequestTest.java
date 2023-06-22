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
package com.formkiq.stacks.api.awstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.UsersMe;
import com.formkiq.stacks.client.requests.GetUsersMeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /users/me tests
 * </p>
 *
 */
public class UsersMeRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test GET /users/me.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testUsersMe01() throws Exception {
    // given
    AuthenticationResultType token = login(ACME_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    GetUsersMeRequest req = new GetUsersMeRequest().siteId("acme");

    // when
    UsersMe me = client.getUsersMe(req);

    // then
    assertEquals(ACME_EMAIL, me.username());
    assertEquals("acme", me.siteId());
    assertEquals("accounting", String.join(",", me.groups()));
  }


  /**
   * Test GET /users/me.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testUsersMe02() throws Exception {
    // given
    GetUsersMeRequest req = new GetUsersMeRequest();
    AuthenticationResultType token = login(ACME_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);

    // when
    HttpResponse<String> me = client.getUsersMeAsHttpResponse(req);

    // then
    assertEquals("400", String.valueOf(me.statusCode()));
    assertEquals("{\"errors\":[{\"key\":\"siteId\","
        + "\"error\":\"parameter required - multiple siteIds found\"}]}", me.body());
  }
}
