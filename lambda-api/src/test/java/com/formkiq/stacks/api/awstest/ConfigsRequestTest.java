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
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Config;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /configs tests
 * </p>
 *
 */
public class ConfigsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test GET /configs.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testConfigs01() throws Exception {

    List<FormKiqClientV1> clients = getFormKiqClients();
    assertEquals(2, clients.size());

    // given
    FormKiqClientV1 client = clients.get(0);

    // when
    Config c = client.getConfigs();

    // then
    assertEquals("", c.chatGptApiKey());

    // given
    client = clients.get(1);

    // when
    HttpResponse<String> response = client.getConfigsAsHttpResponse();

    // then
    assertEquals("401", String.valueOf(response.statusCode()));
    assertEquals("{\"message\":\"user is unauthorized\"}", response.body());
  }

  /**
   * Test GET /configs as readuser user.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testConfigs02() throws Exception {
    // given
    AuthenticationResultType token = login(FINANCE_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);

    // when
    HttpResponse<String> response = client.getConfigsAsHttpResponse();

    // then
    assertEquals("401", String.valueOf(response.statusCode()));
    assertEquals("{\"message\":\"user is unauthorized\"}", response.body());
  }
}
