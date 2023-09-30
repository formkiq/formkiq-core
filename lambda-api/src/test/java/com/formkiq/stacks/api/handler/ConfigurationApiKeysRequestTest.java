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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddApiKeyRequest;
import com.formkiq.client.model.AddApiKeyResponse;
import com.formkiq.client.model.DeleteApiKeyResponse;
import com.formkiq.client.model.GetApiKeysResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /configuration/apiKeys. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ConfigurationApiKeysRequestTest extends AbstractApiClientRequestTest {

  /** Unauthorized (401). */
  private static final int STATUS_UNAUTHORIZED = 401;

  /**
   * Delete /configuration/apiKeys default as User.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteApiKeys01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      // when
      setBearerToken(siteId);

      try {
        this.systemApi.deleteApiKey("ABC", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(STATUS_UNAUTHORIZED, e.getCode());
        assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /configuration/apiKeys default as User.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostApiKeys01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      AddApiKeyRequest req = new AddApiKeyRequest().name("test key");

      // when
      setBearerToken(siteId);

      try {
        this.systemApi.addApiKey(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(STATUS_UNAUTHORIZED, e.getCode());
        assertEquals("{\"message\":\"user is unauthorized\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Get/POST/DELETE /configuration/apiKeys default as Admin without permissions.
   *
   * @throws Exception an error has occurred
   */
  // @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetApiKeys01() throws Exception {
    // given
    String group = "Admins";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String apiKeyName = "test key";
      AddApiKeyRequest req = new AddApiKeyRequest().name(apiKeyName);

      // when
      setBearerToken(group);
      AddApiKeyResponse response = this.systemApi.addApiKey(req, siteId);

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
          .map(p -> p.name()).sorted().collect(Collectors.joining(",")));
      assertEquals("joesmith", apiKeys.getApiKeys().get(0).getUserId());
      assertEquals(siteId != null ? siteId : "default", apiKeys.getApiKeys().get(0).getSiteId());

      // given
      String apiKey = apiKeys.getApiKeys().get(0).getApiKey();

      // when
      DeleteApiKeyResponse delResponse = this.systemApi.deleteApiKey(apiKey, siteId);

      // then
      assertEquals("ApiKey deleted", delResponse.getMessage());
      apiKeys = this.systemApi.getApiKeys(siteId);
      assertEquals(0, apiKeys.getApiKeys().size());
    }
  }

  /**
   * Get /configuration/apiKeys default as User.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetApiKeys02() throws Exception {
    // given
    String siteId = null;
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
}
