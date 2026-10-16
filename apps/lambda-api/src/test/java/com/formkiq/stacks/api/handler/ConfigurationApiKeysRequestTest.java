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

import java.util.Map;
import java.util.Set;

import com.formkiq.aws.dynamodb.ApiPermissionRole;
import com.formkiq.client.model.ApiPermissionRoleType;
import com.formkiq.testutils.api.SetBearerPermissionMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.ApiPermissionGrant;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.ApiKey;
import com.formkiq.client.model.DeleteApiKeyResponse;
import com.formkiq.client.model.GetApiKeysResponse;
import org.junit.jupiter.api.Test;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.testutils.api.systemmanagement.AddApiKeyRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.DeleteApiKeyRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.GetApiKeysRequestBuilder;

/** Unit Tests for request /sites/{siteId}/apiKeys. */
public class ConfigurationApiKeysRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /sites/{siteId}/apiKeys with groups.
   *
   */
  @Test
  public void testAddApiKeyWithGroups() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");
      AddApiKeyRequest req = new AddApiKeyRequest().name("test key")
          .groups(List.of("test1", "test2")).permissions(Set.of(ApiPermissionGrant.READ));

      // when
      AddApiKeyResponse response = new AddApiKeyRequestBuilder().withAddApiKeyRequest(req)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getApiKey());

      GetApiKeysResponse keys = new GetApiKeysRequestBuilder().withNext(null).withLimit(null)
          .submitOk(this.client, siteId).response();
      List<ApiKey> apiKeys = notNull(keys.getApiKeys());
      assertEquals(1, apiKeys.size());
      assertEquals("test key", apiKeys.getFirst().getName());
      assertEquals("READ", notNull(apiKeys.getFirst().getPermissions()).stream().map(Enum::name)
          .collect(Collectors.joining(",")));
      assertEquals("test1,test2", String.join(",", notNull(apiKeys.getFirst().getGroups())));
    }
  }

  /** Attribute-only grants save successfully without a role type. */
  @Test
  public void testCreateAttributePermissionsWithoutRoleType() throws ApiException {
    // given
    String siteId = ID.uuid();
    setBearerToken("Admins");
    Set<ApiPermissionGrant> permissions = Set.of(ApiPermissionGrant.READ_ATTRIBUTES,
        ApiPermissionGrant.WRITE_ATTRIBUTES, ApiPermissionGrant.DELETE_ATTRIBUTES);
    AddApiKeyRequest request =
        new AddApiKeyRequest().name("attribute access").roleType(null).permissions(permissions);

    // when
    AddApiKeyResponse created = new AddApiKeyRequestBuilder().withAddApiKeyRequest(request)
        .submitOk(client, siteId).response();
    List<ApiKey> keys = new GetApiKeysRequestBuilder().getApiKeys(client, siteId);

    // then
    assertNotNull(created.getApiKey());
    assertEquals(1, keys.size());
    assertEquals(ApiPermissionRoleType.CUSTOM, keys.getFirst().getRoleType());
    assertEquals(permissions, keys.getFirst().getPermissions());
  }

  /** An omitted role type and CUSTOM both retain exact requested grants. */
  @Test
  public void testCreateCustomAndLegacyGrants() throws ApiException {
    // given
    setBearerToken("Admins");
    for (ApiPermissionRoleType type : Arrays.asList(null, ApiPermissionRoleType.CUSTOM)) {
      String siteId = ID.uuid();
      AddApiKeyRequest request = new AddApiKeyRequest().name("scoped").roleType(type)
          .addPermissionsItem(ApiPermissionGrant.WRITE)
          .addPermissionsItem(ApiPermissionGrant.READ_DOCUMENT_ATTRIBUTES);
      // when
      new AddApiKeyRequestBuilder().withAddApiKeyRequest(request).submitOk(client, siteId);
      ApiKey key = new GetApiKeysRequestBuilder().getApiKeys(client, siteId).getFirst();

      // then
      assertEquals(ApiPermissionRoleType.CUSTOM, key.getRoleType());
      assertEquals(Set.of(ApiPermissionGrant.WRITE, ApiPermissionGrant.READ_DOCUMENT_ATTRIBUTES),
          key.getPermissions());
    }
  }

  /**
   * All predefined roles are persisted and returned by the management API.
   * 
   * @param type predefined role
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermissionRoleType.class, mode = EnumSource.Mode.EXCLUDE,
      names = {"ADMIN", "CUSTOM"})
  public void testCreatePredefinedRole(final ApiPermissionRoleType type) throws ApiException {
    // given
    String siteId = ID.uuid();
    setBearerToken("Admins");
    AddApiKeyRequest request = new AddApiKeyRequest().name("role").roleType(type);

    // when
    new AddApiKeyRequestBuilder().withAddApiKeyRequest(request).submitOk(client, siteId);
    ApiKey key = new GetApiKeysRequestBuilder().getApiKeys(client, siteId).getFirst();

    // then
    assertEquals(type, key.getRoleType());
    assertEquals(
        ApiPermissionRole
            .builder(com.formkiq.aws.dynamodb.ApiPermissionRoleType.valueOf(type.name())).build()
            .getPermissions().stream().map(Enum::name).collect(Collectors.toSet()),
        notNull(key.getPermissions()).stream().map(Enum::name).collect(Collectors.toSet()));
  }

  /**
   * Delete /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandleDeleteApiKeys01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      try {
        // when
        new DeleteApiKeyRequestBuilder().withApiKey(siteId).submitOk(this.client, "ABC");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"fkq access denied to siteId (ABC)\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Get/POST/DELETE /sites/{siteId}/apiKeys default as Admin without permissions.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetApiKeys01() throws Exception {
    // given
    String group = "Admins";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      String apiKeyName = "test key";
      AddApiKeyRequest req = new AddApiKeyRequest().name(apiKeyName);

      // when
      setBearerToken(group);
      AddApiKeyResponse response = new AddApiKeyRequestBuilder().withAddApiKeyRequest(req)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getApiKey());

      // when
      List<ApiKey> apiKeys = new GetApiKeysRequestBuilder().withNext(null).withLimit(null)
          .getApiKeys(this.client, siteId);

      // then
      assertEquals(1, apiKeys.size());
      assertTrue(Objects.requireNonNull(apiKeys.getFirst().getApiKey()).contains("**************"));
      assertNotNull(apiKeys.getFirst().getInsertedDate());
      assertEquals(apiKeyName, apiKeys.getFirst().getName());
      assertEquals("DELETE,READ,WRITE", notNull(apiKeys.getFirst().getPermissions()).stream()
          .map(Enum::name).sorted().collect(Collectors.joining(",")));
      assertEquals("joesmith", apiKeys.getFirst().getUserId());
      assertEquals(siteId, apiKeys.getFirst().getSiteId());

      // given
      String apiKey = apiKeys.getFirst().getApiKey();
      assertNotNull(apiKey);

      // when
      DeleteApiKeyResponse delResponse = new DeleteApiKeyRequestBuilder().withApiKey(apiKey)
          .submitOk(this.client, siteId).response();

      // then
      assertEquals("ApiKey deleted", delResponse.getMessage());
      var apiKeysResponse = new GetApiKeysRequestBuilder().withNext(null).withLimit(null)
          .submitOk(this.client, siteId).response();
      assertEquals(0, notNull(apiKeysResponse.getApiKeys()).size());
    }
  }

  /**
   * Get /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandleGetApiKeys02() {
    // given
    setBearerToken(DEFAULT_SITE_ID);

    // when
    try {
      new GetApiKeysRequestBuilder().withNext(null).withLimit(null).submitOk(this.client,
          DEFAULT_SITE_ID);
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied" + " (groups: default (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Get /sites/{siteId}/apiKeys default as User.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetApiKeys03() throws Exception {
    // given
    setBearerToken("Admins opa " + DEFAULT_SITE_ID);

    // when
    GetApiKeysResponse response = new GetApiKeysRequestBuilder().withNext(null).withLimit(null)
        .submitOk(this.client, DEFAULT_SITE_ID).response();

    // then
    assertEquals(0, notNull(response.getApiKeys()).size());
  }

  /**
   * Add API Key with Govern permission.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetApiKeys04() throws Exception {
    // given
    String group = "Admins";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(group);

      String apiKeyName = "test key";
      AddApiKeyRequest req =
          new AddApiKeyRequest().name(apiKeyName).addPermissionsItem(ApiPermissionGrant.GOVERN);

      // when
      AddApiKeyResponse response = new AddApiKeyRequestBuilder().withAddApiKeyRequest(req)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getApiKey());

      // when
      var apiKeys = new GetApiKeysRequestBuilder().withNext(null).withLimit(null)
          .getApiKeys(this.client, siteId);

      // then
      assertEquals(1, apiKeys.size());
      assertTrue(Objects.requireNonNull(apiKeys.getFirst().getApiKey()).contains("**************"));
      assertNotNull(apiKeys.getFirst().getInsertedDate());
      assertEquals(apiKeyName, apiKeys.getFirst().getName());
      assertEquals("GOVERN", notNull(apiKeys.getFirst().getPermissions()).stream().map(Enum::name)
          .sorted().collect(Collectors.joining(",")));
      assertEquals(siteId, apiKeys.getFirst().getSiteId());
    }
  }

  /**
   * Get ApiKeys with nextToken.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetApiKeys05() throws Exception {
    // given
    final int count = 5;
    final String siteId = ID.uuid();

    setBearerToken("Admins");

    for (int i = 0; i < count; i++) {
      new AddApiKeyRequestBuilder().withAddApiKeyRequest(new AddApiKeyRequest().name("test_" + i))
          .submitOk(this.client, siteId);
    }

    // when
    GetApiKeysResponse response = new GetApiKeysRequestBuilder().withNext(null).withLimit("2")
        .submitOk(this.client, siteId).response();

    // then
    assertNotNull(response.getNext());
    List<ApiKey> apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("test_0", apiKeys.get(0).getName());
    assertEquals("test_1", apiKeys.get(1).getName());

    response = new GetApiKeysRequestBuilder().withNext(response.getNext()).withLimit("2")
        .submitOk(this.client, siteId).response();
    apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("test_2", apiKeys.get(0).getName());
    assertEquals("test_3", apiKeys.get(1).getName());
  }

  /**
   * POST /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandlePostApiKeys01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      AddApiKeyRequest req = new AddApiKeyRequest().name("test key");

      // when
      setBearerToken(siteId);

      try {
        new AddApiKeyRequestBuilder().withAddApiKeyRequest(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals(
            "{\"message\":\"fkq access denied (groups: " + siteId + " (DELETE,READ,WRITE))\"}",
            e.getResponseBody());
      }
    }
  }

  /** Cross-field validation rejects invalid role assignments without creating keys. */
  @Test
  public void testInvalidRoleRequests() throws ApiException {
    // given
    String siteId = ID.uuid();
    setBearerToken("Admins");
    Map<AddApiKeyRequest, String> invalid = java.util.Map.of(
        new AddApiKeyRequest().name("empty").roleType(ApiPermissionRoleType.CUSTOM),
        "'permissions' must contain at least one permission when 'roleType' is 'CUSTOM'.",
        new AddApiKeyRequest().name("mixed").roleType(ApiPermissionRoleType.READ)
            .addPermissionsItem(ApiPermissionGrant.READ_DOCUMENTS),
        "'permissions' must be omitted or empty when 'roleType' is 'READ'. "
            + "Use 'CUSTOM' to specify individual permissions.");

    for (var entry : invalid.entrySet()) {
      // when
      var response = new AddApiKeyRequestBuilder().withAddApiKeyRequest(entry.getKey())
          .submitError(client, siteId);
      // then
      assertEquals(400, response.exception().getCode());
      assertEquals("{\"message\":\"" + entry.getValue() + "\"}",
          response.exception().getResponseBody());
    }

    // when
    var keys = new GetApiKeysRequestBuilder().getApiKeys(client, siteId);

    // then
    assertTrue(keys.isEmpty());
  }

  /** System management grants can manage keys but cannot grant administrator access. */
  @Test
  public void testSystemManagementCreateKey() throws ApiException {
    // given
    String siteId = ID.uuid();
    new SetBearerPermissionMap().apply(client, List.of("manager"),
        Map.of(siteId, List.of("WRITE_SYSTEM_MANAGEMENT", "READ_SYSTEM_MANAGEMENT")), Map.of());
    AddApiKeyRequest request =
        new AddApiKeyRequest().name("scoped").roleType(ApiPermissionRoleType.READ);

    // when
    new AddApiKeyRequestBuilder().withAddApiKeyRequest(request).submitOk(client, siteId);
    var keys = new GetApiKeysRequestBuilder().getApiKeys(client, siteId);

    // then
    assertEquals(1, keys.size());

    // given
    Map<AddApiKeyRequest, String> adminRequests = Map.of(
        new AddApiKeyRequest().name("admin").roleType(ApiPermissionRoleType.ADMIN),
        "{\"errors\":[{\"key\":\"roleType\",\"error\":\"'ADMIN' roleType is not allowed\"},"
            + "{\"key\":\"roleType\",\"error\":\"'ADMIN' permission is not allowed\"}]}",
        new AddApiKeyRequest().name("custom admin").addPermissionsItem(ApiPermissionGrant.ADMIN),
        "{\"errors\":[{\"key\":\"roleType\",\"error\":\"'ADMIN' permission is not allowed\"}]}");
    for (var entry : adminRequests.entrySet()) {
      // when
      var response = new AddApiKeyRequestBuilder().withAddApiKeyRequest(entry.getKey())
          .submitError(client, siteId);
      // then
      assertEquals(400, response.exception().getCode());
      assertEquals(entry.getValue(), response.exception().getResponseBody());
    }
  }

}
