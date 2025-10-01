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
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.ApiKey;
import com.formkiq.client.model.DeleteApiKeyResponse;
import com.formkiq.client.model.GetApiKeysResponse;
import org.junit.jupiter.api.Test;
import com.formkiq.client.invoker.ApiException;

/** Unit Tests for request /sites/{siteId}/apiKeys. */
public class ConfigurationApiKeysRequestTest extends AbstractApiClientRequestTest {

  /**
   * Delete /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandleDeleteApiKeys01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      // when
      setBearerToken(siteId);

      try {
        this.systemApi.deleteApiKey("ABC", siteId);
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
        this.systemApi.addApiKey(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
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
      AddApiKeyResponse response = this.systemApi.addApiKey(siteId, req);

      // then
      assertNotNull(response.getApiKey());

      // when
      GetApiKeysResponse apiKeysResponse = this.systemApi.getApiKeys(siteId, null, null);

      // then
      List<ApiKey> apiKeys = notNull(apiKeysResponse.getApiKeys());
      assertEquals(1, apiKeys.size());
      assertTrue(Objects.requireNonNull(apiKeys.get(0).getApiKey()).contains("**************"));
      assertNotNull(apiKeys.get(0).getInsertedDate());
      assertEquals(apiKeyName, apiKeys.get(0).getName());
      assertEquals("DELETE,READ,WRITE", notNull(apiKeys.get(0).getPermissions()).stream()
          .map(Enum::name).sorted().collect(Collectors.joining(",")));
      assertEquals("joesmith", apiKeys.get(0).getUserId());
      assertEquals(siteId, apiKeys.get(0).getSiteId());

      // given
      String apiKey = apiKeys.get(0).getApiKey();

      // when
      DeleteApiKeyResponse delResponse = this.systemApi.deleteApiKey(siteId, apiKey);

      // then
      assertEquals("ApiKey deleted", delResponse.getMessage());
      apiKeysResponse = this.systemApi.getApiKeys(siteId, null, null);
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
      this.systemApi.getApiKeys(DEFAULT_SITE_ID, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
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
    GetApiKeysResponse response = this.systemApi.getApiKeys(DEFAULT_SITE_ID, null, null);

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
      AddApiKeyRequest req = new AddApiKeyRequest().name(apiKeyName)
          .addPermissionsItem(AddApiKeyRequest.PermissionsEnum.GOVERN);

      // when
      AddApiKeyResponse response = this.systemApi.addApiKey(siteId, req);

      // then
      assertNotNull(response.getApiKey());

      // when
      GetApiKeysResponse getApiKeys = this.systemApi.getApiKeys(siteId, null, null);

      // then
      List<ApiKey> apiKeys = notNull(getApiKeys.getApiKeys());
      assertEquals(1, apiKeys.size());
      assertTrue(Objects.requireNonNull(apiKeys.get(0).getApiKey()).contains("**************"));
      assertNotNull(apiKeys.get(0).getInsertedDate());
      assertEquals(apiKeyName, apiKeys.get(0).getName());
      assertEquals("GOVERN", notNull(apiKeys.get(0).getPermissions()).stream().map(Enum::name)
          .sorted().collect(Collectors.joining(",")));
      assertEquals(siteId, apiKeys.get(0).getSiteId());
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
      this.systemApi.addApiKey(siteId, new AddApiKeyRequest().name("test_" + i));
    }

    // when
    GetApiKeysResponse response = this.systemApi.getApiKeys(siteId, null, "2");

    // then
    assertNotNull(response.getNext());
    List<ApiKey> apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("test_0", apiKeys.get(0).getName());
    assertEquals("test_1", apiKeys.get(1).getName());

    response = this.systemApi.getApiKeys(siteId, response.getNext(), "2");
    apiKeys = notNull(response.getApiKeys());
    assertEquals(2, apiKeys.size());
    assertEquals("test_2", apiKeys.get(0).getName());
    assertEquals("test_3", apiKeys.get(1).getName());
  }
}
