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

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.DocusignConfig;
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

/** Unit Tests for request /sites/{siteId}/configuration. */
public class ConfigurationRequestTest extends AbstractApiClientRequestTest {

  /** {@link ConfigService}. */
  private ConfigService config;
  /** Test RSA Private Key. */
  private static final String RSA_PRIVATE_KEY = """
      -----BEGIN RSA PRIVATE KEY-----
      MIIEpAIBAAKCAQEArZ1qkA2Aav9OlH6j8C3K5f3Z+zBkRv/VjdW3f7f0VVtslkfj
      i8o6d7lM7gYjp91G9TlG8dOkdJHgZ1lVOjV+Gh8Zg+ReyQ8V2dG3yqMK6qUbiwQ+
      xO5g9mD0W6qmfDeL4Ye6HfN7yZPx5/KQ3R5xF8qBf5kZ3QdrKvLwXs5SFi4kH9Yr
      OqB9rV74PxC/5KggCv9+9DnI5+aU6nb6oxR1+oGxqg8RWiwFMzrcFvptbg6+24EB
      3PMrjkhOScT9Y4l/9LgR0X5+wU/72J4UCqlWh6gW5h9Pz/S5cR3C+KCo4gZiZ/mz
      hrGb0r5M5zTisj6HwtFup/HCMaCZ2tcHb28d1QIDAQABAoIBAD5IQZEq5OEWOfPz
      J5/aGakPvmMxsbPZB0+RaZJfxKAvWOUF24B2oF5RdnH2yOGtFeU5QuGg/2K0Eih4
      5X3j6lnK7TtGFUPj2zyAxBoUzR2emDPkcBbY9k/MW3h1JvOFbvdCgCnOrC2Tdcjx
      vzk12GuP6FtI9Th7VCiZdOBfg9ZhvUeMcmj0RrVhPOzEyN5uLXMZBzDhllIsTzZu
      G7pRhxA/Z3Lv2FtL9Bsg1GYymVXXgMfhm3EErVFn9OHhp1av1G4QON0kWkJ0ZmX3
      hMl8gk0YVJIrWFHwN2ue7b71kF2SMcvU+LoMBLvLEo9tJtDi4OfD6o3leUE6IgdO
      D1ZKPnUCgYEA2F+E6vjBRvcAIKz80MbmjNpuMRFmxh8EFMs+9UtPYW3GnGnIQHcJ
      MQXZqJmMSM6we7MPzVkTfijvOaCF3MQjoEXC0hC+yTQkdm+hyPoJggtLq7sz6I/p
      P66y14+V2v9oF5Ohm0dQg3Jm6b+bQOZgWiLNgO8w5cy+H4WQ0tvw7NcCgYEAyO+D
      FrV+Fb5cxsMEivpkkbItt9J4MdCTIhz1XMfKETvW3VKiOZLIFpAWGXYmOL9UlQa3
      cNs+oubsL8wTn+Kn4T4C5ngW2nYUVk+2yAcCx2oA2Xf0/Eup7fDqTiM5k7cI6hsZ
      nHek5mLQir6oHtGo2bQ1MwQmWlJ5nyvnAbqG+jcCgYEAtTeg9hr4FYAwOf/3Z5Vb
      T4MeUbJ5HH7lgpg7ccLKi1kTxR9ZpCfyZo44H9NivqVto8LzAjFyfbWzKBLB3f0l
      GUtQ6X8MxV1iSG7V1yq8SlCnAZlqf/NxOTjJ0rAyo23tX8eY84LCzhll7W4p/U+a
      kbvXB2fCX7ZfoWmY7KbZBe8CgYEAzEdpKeWFTAgFkM+vTX7wFhU6OoCQbSMc6GTZ
      EcfZrg+/WeXzJ8G0zXxqDlNhbTEU5PEzMf0FxbMj7ETtseJyxgri/CHDLmLfAtXV
      Mu8MZZqXyDFAFTldZTHM60iY/q0Xo4FFlfKi3CaBtLK5PRmZ7DPizSZRlIlC1GJz
      yOLdQpECgYB5R53Jl/mWBY7YB7mN7hnxRLLMwzszLtZWsoC7rHMQHbGbn/suj1Pb
      0wdSNEX4zzpFb2YBmhnJ5yMspe7KhAYLrA1yIouQXvdK2PVU2E5gVhFt8Jumg3zd
      +hDj+3W+ptjLh1mMaDW9KMUC82gDBH+fB0EJiaDxPF5ux/vJczj8FQKBgQC4t5WR
      /npH1BHCgDeXDOeH5ojjtTmS/jI/xyEFxyHiF9FeMiAlxZoPOD/U6He4W7wZCOqH
      fEoZXhNR4DFQnvfjUkrMr/JmRzHlt2yVhQUwtyvJ8cVZ0yhsqCQEOLdE2F0XnKkv
      23ybvqMt27quf7bB6FKNBJihmwoCSMREfI7Ebw==
      -----END RSA PRIVATE KEY-----
      """;

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

  /**
   * PUT google invalid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePutConfiguration04() {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req =
        new UpdateConfigurationRequest().google(new GoogleConfig().workloadIdentityAudience("123"));

    // when
    try {
      this.systemApi.updateConfiguration(siteId, req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"google\"," + "\"error\":\"all 'googleWorkloadIdentityAudience', "
              + "'googleWorkloadIdentityServiceAccount' " + "are required for google setup\"}]}",
          e.getResponseBody());
    }
  }

  /**
   * PUT google invalid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePutConfiguration05() {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest()
        .docusign(new DocusignConfig().integrationKey("111").rsaPrivateKey("222"));

    // when
    try {
      this.systemApi.updateConfiguration(siteId, req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"docusign\","
          + "\"error\":\"all 'docusignUserId', 'docusignIntegrationKey', 'docusignRsaPrivateKey' "
          + "are required for docusign setup\"}]}", e.getResponseBody());
    }
  }

  /**
   * PUT docusign valid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePutConfiguration06() throws ApiException {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().docusign(
        new DocusignConfig().userId("123").integrationKey("111").rsaPrivateKey(RSA_PRIVATE_KEY));

    this.systemApi.updateConfiguration(siteId, req);

    // when
    GetConfigurationResponse configuration = this.systemApi.getConfiguration(siteId);

    // then
    assertNotNull(configuration.getDocusign());
    assertEquals("111", configuration.getDocusign().getIntegrationKey());
    assertEquals("123", configuration.getDocusign().getUserId());
    assertEquals("""
        -----BEGIN RSA PRIVATE KEY-----
        MIIEpAIB*******EfI7Ebw==
        -----END RSA PRIVATE KEY-----
        """, configuration.getDocusign().getRsaPrivateKey());
  }

  /**
   * PUT docusign invalid RSA key configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePutConfiguration07() throws ApiException {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().docusign(
        new DocusignConfig().userId("123").integrationKey("111").rsaPrivateKey("3423432432432423"));

    this.systemApi.updateConfiguration(siteId, req);

    // when
    GetConfigurationResponse configuration = this.systemApi.getConfiguration(siteId);

    // then
    assertNotNull(configuration.getDocusign());
    assertEquals("111", configuration.getDocusign().getIntegrationKey());
    assertEquals("123", configuration.getDocusign().getUserId());
    assertEquals("34234*******32423", configuration.getDocusign().getRsaPrivateKey());
  }
}
