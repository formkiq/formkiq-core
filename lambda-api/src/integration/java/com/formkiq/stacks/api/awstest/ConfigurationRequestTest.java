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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.SetConfigRequest;
import com.formkiq.client.model.SetConfigResponse;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Configuration;
import com.formkiq.stacks.client.requests.UpdateConfigurationRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /configuration tests
 * </p>
 *
 */
public class ConfigurationRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test GET /configuration.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGetConfiguration01() throws Exception {

    // given
    final int expected = 3;
    List<FormKiqClientV1> clients = getFormKiqClients(null);
    assertEquals(expected, clients.size());

    for (FormKiqClientV1 client : Arrays.asList(clients.get(0), clients.get(1))) {

      // when
      Configuration c = client.getConfiguration();

      // then
      assertNotNull(c.chatGptApiKey());
    }

    // given
    FormKiqClientV1 fc = clients.get(2);

    // when
    Configuration configuation = fc.getConfiguration();

    // then
    assertEquals("", configuation.maxContentLengthBytes());
    assertEquals("", configuation.maxDocuments());
  }

  /**
   * Test GET /configuration as readuser user.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGetConfiguration02() throws Exception {
    // given
    AuthenticationResultType token = login(FINANCE_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);

    // when
    Configuration configuation = client.getConfiguration();

    // then
    assertEquals("", configuation.maxContentLengthBytes());
    assertEquals("", configuation.maxDocuments());
  }

  /**
   * Test PATCH /configuration as readuser user.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPatchConfiguration01() throws Exception {
    // given
    AuthenticationResultType token = login(FINANCE_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().config(new Configuration());

    // when
    HttpResponse<String> response = client.updateConfigurationAsHttpResponse(req);

    // then
    assertEquals("401", String.valueOf(response.statusCode()));
    assertEquals("{\"message\":\"user is unauthorized\"}", response.body());
  }

  /**
   * Test PATCH /configuration as admin.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPatchConfiguration02() throws Exception {
    // given
    final int expected = 3;
    List<FormKiqClientV1> clients = getFormKiqClients(null);
    assertEquals(expected, clients.size());

    String chatGptKey = "aklsdjsalkdjsakldjsadjadad";
    UpdateConfigurationRequest req =
        new UpdateConfigurationRequest().config(new Configuration().chatGptApiKey(chatGptKey));

    for (FormKiqClientV1 client : Arrays.asList(clients.get(0), clients.get(1))) {
      // when
      HttpResponse<String> response = client.updateConfigurationAsHttpResponse(req);

      // then
      assertEquals("200", String.valueOf(response.statusCode()));
      assertEquals("{\"message\":\"Config saved\"}", response.body());

      Configuration configuration = client.getConfiguration();
      assertEquals("akls*******adad", configuration.chatGptApiKey());
    }

    // given
    FormKiqClientV1 client = clients.get(2);

    // when
    HttpResponse<String> response = client.updateConfigurationAsHttpResponse(req);

    // then
    assertEquals("401", String.valueOf(response.statusCode()));
    assertEquals("{\"message\":\"user is unauthorized\"}", response.body());
  }

  /**
   * PUT /config notification email.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutConfiguration03() throws Exception {
    // given
    String siteId = null;
    List<ApiClient> clients = getApiClients(null);

    String adminEmail =
        getParameterStoreValue("/formkiq/" + getAppenvironment() + "/console/AdminEmail");
    SetConfigRequest req = new SetConfigRequest().notificationEmail(adminEmail);

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {
      SystemManagementApi api = new SystemManagementApi(client);

      // when
      SetConfigResponse updateConfiguration = api.updateConfiguration(req, siteId);

      // then
      assertEquals("", updateConfiguration.getMessage());
    }
  }

  /**
   * PUT /config invalid notification email.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutConfiguration04() throws Exception {
    // given
    String siteId = null;
    List<ApiClient> clients = getApiClients(null);

    String email = "test@formkiq.com";
    SetConfigRequest req = new SetConfigRequest().notificationEmail(email);

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {
      SystemManagementApi api = new SystemManagementApi(client);

      // when
      try {
        api.updateConfiguration(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("", e.getResponseBody());
      }
    }
  }
}
