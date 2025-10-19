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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.ApiKey;
import com.formkiq.client.model.GetApiKeysResponse;
import com.formkiq.client.model.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddApiKeyRequest.PermissionsEnum;
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

  /** Cognito FINANCE User Email. */
  private static final String FINANCE_EMAIL = "testfinance912345@formkiq.com";
  /** {@link ApiClient}. */
  private ApiClient jwtApiClient = null;

  /** {@link SystemManagementApi}. */
  private SystemManagementApi jwtSystemApi = null;
  /** {@link ApiClient}. */
  private ApiClient keyApiClient = null;

  /** {@link DocumentsApi}. */
  private DocumentsApi keyDocumentsApi = null;

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
   * Test Add API key.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddApiKey01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      ApiClient apiClient = getApiClients(siteId).get(0);
      SystemManagementApi api = new SystemManagementApi(apiClient);

      AddApiKeyRequest req =
          new AddApiKeyRequest().name(ID.uuid()).addPermissionsItem(PermissionsEnum.GOVERN);

      // when
      AddApiKeyResponse response = api.addApiKey(siteId, req);

      // then
      ApiClient apiClientWithToken = getApiClientWithToken(response.getApiKey());
      api = new SystemManagementApi(apiClientWithToken);
      List<Site> sites = notNull(api.getSites(null).getSites());
      assertEquals(1, sites.size());
      assertEquals(siteId, sites.get(0).getSiteId());
      assertEquals("GOVERN", String.join(",",
          notNull(sites.get(0).getPermissions()).stream().map(Enum::name).toList()));
    }
  }

  /**
   * Test Add READONLY/WRITE Api Key.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testApiKey03() throws Exception {
    // given
    String name = "My Read API";

    AuthenticationResultType token = getAdminToken();

    com.formkiq.client.model.AddApiKeyRequest req = new com.formkiq.client.model.AddApiKeyRequest()
        .name(name).addPermissionsItem(PermissionsEnum.READ);

    for (String siteId : List.of(DEFAULT_SITE_ID)) {

      // when
      this.jwtApiClient.addDefaultHeader("Authorization", token.accessToken());
      String apiKey = this.jwtSystemApi.addApiKey(siteId, req).getApiKey();

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
      apiKey = this.jwtSystemApi.addApiKey(siteId, req).getApiKey();

      // when
      this.keyApiClient.addDefaultHeader("Authorization", apiKey);
      AddDocumentResponse response = this.keyDocumentsApi.addDocument(docReq, siteId, null);

      // then
      assertNotNull(response.getDocumentId());
    }
  }

  /**
   * Test GET /configuration/apiKeys.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetApiKey01() throws Exception {
    // given
    String name = "My API";
    final int expected = 3;

    List<ApiClient> apiClients = getApiClients(null);
    assertEquals(expected, apiClients.size());

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (ApiClient client : Arrays.asList(apiClients.get(0), apiClients.get(1))) {
        AddApiKeyRequest apiReq = new AddApiKeyRequest().name(name);

        SystemManagementApi api = new SystemManagementApi(client);

        // when
        AddApiKeyResponse response = api.addApiKey(siteId, apiReq);

        // then
        GetApiKeysResponse apiKeys = api.getApiKeys(siteId, null, null);
        assertFalse(notNull(apiKeys.getApiKeys()).isEmpty());

        api.deleteApiKey(siteId, response.getApiKey());
      }
    }

    // given
    ApiClient c = apiClients.get(2);
    SystemManagementApi api = new SystemManagementApi(c);

    // when
    try {
      api.getApiKeys(DEFAULT_SITE_ID, null, null);
      // then
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied " + "(groups: default (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Test GET /configuration/apiKeys as readuser user.
   *
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetApiKey02() {
    // given
    addAndLoginCognito(FINANCE_EMAIL, List.of("finance"));

    AuthenticationResultType token = getCognito().login(FINANCE_EMAIL, USER_PASSWORD);

    this.jwtApiClient.addDefaultHeader("Authorization", token.accessToken());
    SystemManagementApi api = new SystemManagementApi(this.jwtApiClient);

    // when
    try {
      api.getApiKeys(DEFAULT_SITE_ID, null, null);

      // then
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied to siteId (default)\"}", e.getResponseBody());
    }
  }

  /**
   * Test GET /configuration/apiKeys with next token.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetApiKey03() throws Exception {
    // given
    String siteId = ID.uuid();
    final int count = 5;

    ApiClient client = getApiClients(siteId).get(0);
    SystemManagementApi api = new SystemManagementApi(client);

    for (int i = 0; i < count; i++) {
      AddApiKeyRequest apiReq = new AddApiKeyRequest().name("test_" + i);
      api.addApiKey(siteId, apiReq);
    }

    // when
    GetApiKeysResponse response = api.getApiKeys(siteId, null, "2");

    // then
    assertNotNull(response.getNext());

    List<ApiKey> apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("My Api Key", apiKeys.get(0).getName());
    assertEquals("test_0", apiKeys.get(1).getName());

    response = api.getApiKeys(siteId, response.getNext(), "2");
    apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("test_1", apiKeys.get(0).getName());
    assertEquals("test_2", apiKeys.get(1).getName());
  }
}
