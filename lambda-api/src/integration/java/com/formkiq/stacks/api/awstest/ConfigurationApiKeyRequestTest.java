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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddApiKeyRequest.PermissionsEnum;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.GetApiKeysResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * Process Urls.
 * <p>
 * GET /configuration/apiKeys integration tests
 * </p>
 *
 */
public class ConfigurationApiKeyRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /** {@link ApiClient}. */
  private ApiClient jwtApiClient = null;
  /** {@link SystemManagementApi}. */
  private SystemManagementApi jwtSystemApi = null;

  /** {@link ApiClient}. */
  private ApiClient keyApiClient = null;
  /** {@link DocumentsApi}. */
  private DocumentsApi keyDocumentsApi = null;

  /** Cognito FINANCE User Email. */
  private static final String FINANCE_EMAIL = "testfinance912345@formkiq.com";

  /**
   * Before Each.
   */
  @BeforeEach
  public void beforeEach() {
    this.jwtApiClient = new ApiClient().setReadTimeout(0).setBasePath(getCognito().getRootJwtUrl());
    this.jwtSystemApi = new SystemManagementApi(this.jwtApiClient);

    this.keyApiClient = new ApiClient().setReadTimeout(0).setBasePath(getCognito().getRootKeyUrl());
    this.keyDocumentsApi = new DocumentsApi(this.keyApiClient);
  }

  /**
   * Test GET /configuration/apiKeys.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testApiKey01() throws Exception {
    // given
    String name = "My API";
    final int expected = 3;

    List<ApiClient> apiClients = getApiClients(null);
    assertEquals(expected, apiClients.size());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (ApiClient client : Arrays.asList(apiClients.get(0), apiClients.get(1))) {
        com.formkiq.client.model.AddApiKeyRequest apiReq =
            new com.formkiq.client.model.AddApiKeyRequest().name(name);

        SystemManagementApi api = new SystemManagementApi(client);

        // when
        api.addApiKey(apiReq, siteId);

        // then
        GetApiKeysResponse apiKeys = api.getApiKeys(siteId);
        assertFalse(apiKeys.getApiKeys().isEmpty());
      }
    }

    // given
    ApiClient c = apiClients.get(2);
    SystemManagementApi api = new SystemManagementApi(c);

    // when
    try {
      api.getApiKeys(null);
      // then
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
    }
  }

  /**
   * Test GET /configuration/apiKeys as readuser user.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testApiKey02() throws Exception {
    // given
    addAndLoginCognito(FINANCE_EMAIL, Arrays.asList("finance"));

    AuthenticationResultType token = getCognito().login(FINANCE_EMAIL, USER_PASSWORD);

    this.jwtApiClient.addDefaultHeader("Authorization", token.accessToken());
    SystemManagementApi api = new SystemManagementApi(this.jwtApiClient);

    // when
    try {
      api.getApiKeys(null);

      // then
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
    }
  }

  /**
   * Test Add READONLY/WRITE Api Key.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testApiKey03() throws Exception {
    // given
    String name = "My Read API";

    AuthenticationResultType token = getAdminToken();

    com.formkiq.client.model.AddApiKeyRequest req = new com.formkiq.client.model.AddApiKeyRequest()
        .name(name).addPermissionsItem(PermissionsEnum.READ);

    for (String siteId : Arrays.asList("default")) {

      // when
      this.jwtApiClient.addDefaultHeader("Authorization", token.accessToken());
      String apiKey = this.jwtSystemApi.addApiKey(req, siteId).getApiKey();

      // then
      this.keyApiClient.addDefaultHeader("Authorization", apiKey);
      AddDocumentRequest docReq = new AddDocumentRequest().content("test");

      try {
        this.keyDocumentsApi.addDocument(docReq, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}",
            e.getResponseBody());
      }

      // given
      req = new com.formkiq.client.model.AddApiKeyRequest().name(name)
          .addPermissionsItem(PermissionsEnum.WRITE);
      apiKey = this.jwtSystemApi.addApiKey(req, siteId).getApiKey();

      // when
      this.keyApiClient.addDefaultHeader("Authorization", apiKey);
      AddDocumentResponse response = this.keyDocumentsApi.addDocument(docReq, siteId, null);

      // then
      assertNotNull(response.getDocumentId());
    }
  }
}
