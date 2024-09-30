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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.DeleteApiKeyResponse;
import com.formkiq.client.model.GetApiKeysResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites/{siteId}/apiKeys. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ConfigurationApiKeysRequestTest extends AbstractApiClientRequestTest {

  /**
   * Delete /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandleDeleteApiKeys01() {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      // when
      setBearerToken(siteId);

      try {
        this.systemApi.deleteApiKey("ABC", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"fkq access denied to siteId (ABC)\"}", e.getResponseBody());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
  // @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetApiKeys01() throws Exception {
    // given
    String group = "Admins";

    for (String siteId : Arrays.asList(SiteIdKeyGenerator.DEFAULT_SITE_ID,
        UUID.randomUUID().toString())) {

      String apiKeyName = "test key";
      AddApiKeyRequest req = new AddApiKeyRequest().name(apiKeyName);

      // when
      setBearerToken(group);
      AddApiKeyResponse response = this.systemApi.addApiKey(siteId, req);

      // then
      assertNotNull(response.getApiKey());

      // when
      GetApiKeysResponse apiKeys = this.systemApi.getApiKeys(siteId);

      // then
      assertEquals(1, apiKeys.getApiKeys().size());
      assertTrue(apiKeys.getApiKeys().get(0).getApiKey().contains("**************"));
      assertNotNull(apiKeys.getApiKeys().get(0).getInsertedDate());
      assertEquals(apiKeyName, apiKeys.getApiKeys().get(0).getName());
      assertEquals("DELETE,READ,WRITE", apiKeys.getApiKeys().get(0).getPermissions().stream()
          .map(Enum::name).sorted().collect(Collectors.joining(",")));
      assertEquals("joesmith", apiKeys.getApiKeys().get(0).getUserId());
      assertEquals(siteId, apiKeys.getApiKeys().get(0).getSiteId());

      // given
      String apiKey = apiKeys.getApiKeys().get(0).getApiKey();

      // when
      DeleteApiKeyResponse delResponse = this.systemApi.deleteApiKey(siteId, apiKey);

      // then
      assertEquals("ApiKey deleted", delResponse.getMessage());
      apiKeys = this.systemApi.getApiKeys(siteId);
      assertEquals(0, apiKeys.getApiKeys().size());
    }
  }

  /**
   * Get /sites/{siteId}/apiKeys default as User.
   *
   */
  @Test
  public void testHandleGetApiKeys02() {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "default";

    // when
    setBearerToken(group);

    try {
      this.systemApi.getApiKeys(siteId);
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
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "default";

    setBearerToken("Admins opa " + group);

    // when
    GetApiKeysResponse response = this.systemApi.getApiKeys(siteId);

    // then
    assertEquals(0, response.getApiKeys().size());
  }

  /**
   * Add API Key with Govern permission.
   *
   * @throws Exception an error has occurred
   */
  // @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetApiKeys04() throws Exception {
    // given
    String group = "Admins";

    for (String siteId : Arrays.asList(SiteIdKeyGenerator.DEFAULT_SITE_ID,
        UUID.randomUUID().toString())) {

      setBearerToken(group);

      String apiKeyName = "test key";
      AddApiKeyRequest req = new AddApiKeyRequest().name(apiKeyName)
          .addPermissionsItem(AddApiKeyRequest.PermissionsEnum.GOVERN);

      // when
      AddApiKeyResponse response = this.systemApi.addApiKey(siteId, req);

      // then
      String apiKey = response.getApiKey();
      assertNotNull(response.getApiKey());

      // when
      GetApiKeysResponse apiKeys = this.systemApi.getApiKeys(siteId);

      // then
      assertEquals(1, apiKeys.getApiKeys().size());
      assertTrue(apiKeys.getApiKeys().get(0).getApiKey().contains("**************"));
      assertNotNull(apiKeys.getApiKeys().get(0).getInsertedDate());
      assertEquals(apiKeyName, apiKeys.getApiKeys().get(0).getName());
      assertEquals("GOVERN", apiKeys.getApiKeys().get(0).getPermissions().stream().map(Enum::name)
          .sorted().collect(Collectors.joining(",")));
      assertEquals(siteId, apiKeys.getApiKeys().get(0).getSiteId());
    }
  }
}
