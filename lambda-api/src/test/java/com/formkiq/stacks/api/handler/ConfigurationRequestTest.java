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

import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Map;
import java.util.UUID;

import com.formkiq.client.model.GoogleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetConfigurationResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.client.model.UpdateConfigurationResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;

/** Unit Tests for request /configuration. */
public class ConfigurationRequestTest extends AbstractApiClientRequestTest {

  /** {@link ConfigService}. */
  private ConfigService config;

  /**
   * Before Each.
   */
  @BeforeEach
  public void beforeEach() {
    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(ConfigService.class, new ConfigServiceExtension());
    this.config = awsServices.getExtension(ConfigService.class);
  }

  /**
   * Get /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetConfiguration01() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";

    this.config.save(siteId, new DynamicObject(Map.of("chatGptApiKey", "somevalue")));

    setBearerToken(group);

    // when
    UpdateConfigurationResponse updateConfig = this.systemApi.updateConfiguration(siteId,
        new UpdateConfigurationRequest().chatGptApiKey("anothervalue"));
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("Config saved", updateConfig.getMessage());
    assertEquals("anot*******alue", response.getChatGptApiKey());
    assertEquals("", response.getMaxContentLengthBytes());
    assertEquals("", response.getMaxDocuments());
    assertEquals("", response.getMaxWebhooks());
    assertEquals("", response.getNotificationEmail());
  }

  /**
   * Get /config default as User.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetConfiguration02() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;

    this.config.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "somevalue")));

    String group = "Admins";
    setBearerToken(group);

    // when
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("some*******alue", response.getChatGptApiKey());
    assertEquals("", response.getMaxContentLengthBytes());
    assertEquals("", response.getMaxDocuments());
    assertEquals("", response.getMaxWebhooks());
    assertEquals("", response.getNotificationEmail());
  }

  /**
   * Get /config for siteId, Config in default.
   *
   * @throws Exception an error has occurred
   */

  @Test
  public void testHandleGetConfiguration03() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();
    String group = "Admins";
    setBearerToken(group);

    this.config.save(null, new DynamicObject(Map.of(CHATGPT_API_KEY, "somevalue")));

    // when
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("some*******alue", response.getChatGptApiKey());
    assertEquals("", response.getMaxContentLengthBytes());
    assertEquals("", response.getMaxDocuments());
    assertEquals("", response.getMaxWebhooks());
    assertEquals("", response.getNotificationEmail());
  }

  /**
   * Get /config for siteId, Config in siteId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetConfiguration04() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();
    String group = "Admins";
    setBearerToken(group);

    this.config.save(null, new DynamicObject(Map.of(CHATGPT_API_KEY, "somevalue")));
    this.config.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "anothervalue")));

    // when
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("anot*******alue", response.getChatGptApiKey());
    assertEquals("", response.getMaxContentLengthBytes());
    assertEquals("", response.getMaxDocuments());
    assertEquals("", response.getMaxWebhooks());
    assertEquals("", response.getNotificationEmail());
  }

  /**
   * PUT /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutConfiguration01() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().chatGptApiKey("anotherkey")
        .maxContentLengthBytes("1000000").maxDocuments("1000").maxWebhooks("5");

    // when
    UpdateConfigurationResponse configResponse = this.systemApi.updateConfiguration(siteId, req);
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("Config saved", configResponse.getMessage());

    assertEquals("anot*******rkey", response.getChatGptApiKey());
    assertEquals("1000000", response.getMaxContentLengthBytes());
    assertEquals("1000", response.getMaxDocuments());
    assertEquals("5", response.getMaxWebhooks());
    assertEquals("", response.getNotificationEmail());
  }

  /**
   * PUT /config default as user.
   *
   */
  @Test
  public void testHandlePutConfiguration02() {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "default";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().chatGptApiKey("anotherkey")
        .maxContentLengthBytes("1000000").maxDocuments("1000").maxWebhooks("5");

    // when
    try {
      this.systemApi.updateConfiguration(siteId, req);
      fail();
    } catch (ApiException e) {
      final int code = 401;
      assertEquals(code, e.getCode());
    }
  }

  /**
   * PUT google configuration /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutConfiguration03() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().google(
        new GoogleConfig().workloadIdentityAudience("123").workloadIdentityServiceAccount("444"));

    // when
    UpdateConfigurationResponse configResponse = this.systemApi.updateConfiguration(siteId, req);
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("Config saved", configResponse.getMessage());
    GoogleConfig google = response.getGoogle();

    assertNotNull(google);

    assertEquals("123", google.getWorkloadIdentityAudience());
    assertEquals("444", google.getWorkloadIdentityServiceAccount());
  }
}
