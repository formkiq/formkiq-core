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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.http.HttpResponse;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Sites;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /sites tests
 * </p>
 *
 */
public class SitesRequestTest extends AbstractApiTest {

  /** Http Status OK. */
  private static final int HTTP_STATUS_NO_CONTENT = 204;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test GET /version.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSites01() throws Exception {
    // given
    AuthenticationResultType token = login(USER_EMAIL, USER_PASSWORD);
    putParameter("/formkiq/" + getAppenvironment() + "/maildomain", "tryformkiq.com");
    FormKiqClientV1 client = createHttpClient(token);

    // when
    Sites sites = client.getSites();
    // then
    assertEquals(1, sites.sites().size());
    assertNotNull(sites.sites().get(0).siteId());
    assertEquals("READ_WRITE", sites.sites().get(0).permission());
    assertTrue(sites.sites().get(0).uploadEmail().endsWith("@tryformkiq.com"));
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test // (timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      HttpResponse<String> response = client.optionsSites();
      assertEquals(HTTP_STATUS_NO_CONTENT, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
