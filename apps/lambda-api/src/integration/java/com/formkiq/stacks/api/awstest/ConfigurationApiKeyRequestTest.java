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

import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.WaitForDocumentContentRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.AddApiKeyRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.DeleteApiKeyRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.GetApiKeysRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.GetSitesRequestBuilder;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.ApiKey;
import com.formkiq.client.model.ApiPermissionGrant;
import com.formkiq.client.model.ApiPermissionRoleType;
import com.formkiq.client.model.GetApiKeysResponse;
import com.formkiq.client.model.GetSitesResponse;
import com.formkiq.client.model.Site;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
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

  private static String getSortedRoles(final GetSitesResponse sitesResponse) {
    return String.join(",",
        notNull(Objects.requireNonNull(sitesResponse.getRoles()).stream().sorted().toList()));
  }

  /** {@link ApiClient}. */
  private ApiClient jwtApiClient = null;

  /** {@link ApiClient}. */
  private ApiClient keyApiClient = null;

  /** {@link DocumentsApi}. */
  private DocumentsApi keyDocumentsApi = null;

  private AddApiKeyResponse addApiKey(final String siteId, final ApiPermissionGrant permission,
      final List<String> groups) throws ApiException {
    ApiClient apiClient = getApiClients(siteId).getFirst();
    AddApiKeyRequest req =
        new AddApiKeyRequest().name(ID.uuid()).addPermissionsItem(permission).groups(groups);
    return new AddApiKeyRequestBuilder().withAddApiKeyRequest(req).submitOk(apiClient, siteId)
        .response();
  }

  /**
   * Before Each.
   */
  @BeforeEach
  public void beforeEach() {
    this.jwtApiClient = new ApiClient().setReadTimeout(0).setBasePath(getCognito().getRootJwtUrl());

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
  public void testAddApiKeyGovern() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, "aa" + ID.uuid())) {

      // when
      AddApiKeyResponse response = addApiKey(siteId, ApiPermissionGrant.GOVERN, List.of());

      // then
      ApiClient apiClientWithToken = getApiClientWithToken(response.getApiKey());
      GetSitesResponse sitesResponse =
          new GetSitesRequestBuilder().submitOk(apiClientWithToken, null).response();
      List<Site> sites = notNull(sitesResponse.getSites());
      assertEquals(1, sites.size());
      assertEquals(siteId, sites.getFirst().getSiteId());
      assertEquals("GOVERN", String.join(",",
          notNull(sites.getFirst().getPermissions()).stream().map(Enum::name).toList()));
      assertEquals("API_KEY," + siteId, getSortedRoles(sitesResponse));
    }
  }

  /**
   * API-key custom claims are available to an authenticated caller through GET /sites.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT * 2)
  public void testAddApiKeyWithCustomClaims() throws Exception {
    // given
    String siteId = ID.uuid();
    String name = "custom-claims-" + ID.ulid();
    Map<String, Object> claims = Map.of("custom:department", "Legal", "customerId", "acme", "roles",
        List.of("reviewer"), "active", true, "limit", 42.5, "count", 7.0, "adjustment", -3.25,
        "profile", Map.of("region", "north", "weights", List.of(0.0, -2.5)));
    this.jwtApiClient.addDefaultHeader("Authorization", getAdminToken().accessToken());
    AddApiKeyRequest request =
        new AddApiKeyRequest().name(name).roleType(ApiPermissionRoleType.READ).customClaims(claims);

    // when
    var apiKey = new AddApiKeyRequestBuilder().withAddApiKeyRequest(request)
        .getApiKey(this.jwtApiClient, siteId);

    // then
    assertNotNull(apiKey);
    try {
      // when
      ApiClient apiKeyClient = getApiClientWithToken(apiKey);
      GetSitesResponse sites = new GetSitesRequestBuilder().getSites(apiKeyClient, null);

      // then
      assertEquals(claims, sites.getUserClaims());
      assertEquals(name, sites.getUsername());
      assertEquals(siteId, notNull(sites.getSites()).getFirst().getSiteId());
    } finally {
      new DeleteApiKeyRequestBuilder().withApiKey(apiKey).submitOk(this.jwtApiClient, siteId);
    }
  }

  /**
   * Test Add API key witb groups.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddApiKeyWithGroups() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, "aa" + ID.uuid())) {

      // when
      AddApiKeyResponse response =
          addApiKey(siteId, ApiPermissionGrant.READ, List.of("test1", "test2"));

      // then
      ApiClient apiClientWithToken = getApiClientWithToken(response.getApiKey());
      GetSitesResponse sitesResponse =
          new GetSitesRequestBuilder().submitOk(apiClientWithToken, null).response();
      List<Site> sites = notNull(sitesResponse.getSites());
      assertEquals(1, sites.size());

      Site site = sites.stream().filter(s -> siteId.equals(s.getSiteId())).findFirst().orElse(null);
      assertNotNull(site);

      assertEquals(siteId, site.getSiteId());
      assertEquals("READ",
          String.join(",", notNull(site.getPermissions()).stream().map(Enum::name).toList()));
      assertEquals("API_KEY," + siteId + ",test1,test2", getSortedRoles(sitesResponse));
    }
  }

  /**
   * A WRITE_DOCUMENTS-only API key lists its exact grant and can create a document but cannot read
   * it.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddApiKeyWithWriteDocumentsOnly() throws Exception {
    // given
    String siteId = ID.uuid();
    this.jwtApiClient.addDefaultHeader("Authorization", getAdminToken().accessToken());
    AddApiKeyRequest request =
        new AddApiKeyRequest().name(ID.uuid()).roleType(ApiPermissionRoleType.CUSTOM)
            .permissions(Set.of(ApiPermissionGrant.WRITE_DOCUMENTS));

    // when
    AddApiKeyResponse created = new AddApiKeyRequestBuilder().withAddApiKeyRequest(request)
        .submitOk(this.jwtApiClient, siteId).response();
    List<ApiKey> keys = new GetApiKeysRequestBuilder().getApiKeys(this.jwtApiClient, siteId);

    // then
    assertNotNull(created.getApiKey());
    assertEquals(1, keys.size());
    assertEquals(ApiPermissionRoleType.CUSTOM, keys.getFirst().getRoleType());
    assertEquals(Set.of(ApiPermissionGrant.WRITE_DOCUMENTS), keys.getFirst().getPermissions());

    String documentId = null;
    try {
      // given
      ApiClient writer = getApiClientWithToken(created.getApiKey());

      // when
      GetSitesResponse sitesResponse =
          new GetSitesRequestBuilder().submitOk(writer, null).response();

      // then
      List<Site> sites = notNull(sitesResponse.getSites());
      assertEquals(1, sites.size());
      assertEquals(siteId, sites.getFirst().getSiteId());
      assertEquals(List.of("WRITE_DOCUMENTS"),
          notNull(sites.getFirst().getPermissions()).stream().map(Enum::name).toList());

      // when
      AddDocumentResponse document = new AddDocumentRequestBuilder().content("write-only content")
          .contentType("text/plain").submitOk(writer, siteId).response();
      documentId = document.getDocumentId();

      // then
      assertNotNull(documentId);

      // given
      new WaitForDocumentContentRequestBuilder(documentId).content("write-only content")
          .submitOk(this.jwtApiClient, siteId);

      // when
      var denied = new GetDocumentRequestBuilder(documentId).submitError(writer, siteId);

      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), denied.exception().getCode());
    } finally {
      if (documentId != null) {
        new DeleteDocumentRequestBuilder(documentId).submitOk(this.jwtApiClient, siteId);
      }
      new DeleteApiKeyRequestBuilder().withApiKey(created.getApiKey()).submitOk(this.jwtApiClient,
          siteId);
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

    AddApiKeyRequest req =
        new AddApiKeyRequest().name(name).addPermissionsItem(ApiPermissionGrant.READ);

    for (String siteId : List.of(DEFAULT_SITE_ID)) {

      // when
      this.jwtApiClient.addDefaultHeader("Authorization", token.accessToken());
      String apiKey = new AddApiKeyRequestBuilder().withAddApiKeyRequest(req)
          .submitOk(this.jwtApiClient, siteId).response().getApiKey();

      // then
      this.keyApiClient.addDefaultHeader("Authorization", apiKey);
      AddDocumentRequest docReq = new AddDocumentRequest().content("test");

      try {
        new AddDocumentRequestBuilder(docReq).submitOk(this.keyDocumentsApi.getApiClient(), siteId);
        fail();
      } catch (ApiException e) {
        assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}",
            e.getResponseBody());
      }

      // given
      req = new AddApiKeyRequest().name(name).addPermissionsItem(ApiPermissionGrant.WRITE);
      apiKey = new AddApiKeyRequestBuilder().withAddApiKeyRequest(req)
          .submitOk(this.jwtApiClient, siteId).response().getApiKey();

      // when
      this.keyApiClient.addDefaultHeader("Authorization", apiKey);
      AddDocumentResponse response = new AddDocumentRequestBuilder(docReq)
          .submitOk(this.keyDocumentsApi.getApiClient(), siteId).response();

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
        assertNotNull(response.getApiKey());

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

    ApiClient client = getApiClients(siteId).getFirst();
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
