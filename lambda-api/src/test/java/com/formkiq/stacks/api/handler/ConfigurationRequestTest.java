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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocusignConfig;
import com.formkiq.client.model.GetConfigurationResponse;
import com.formkiq.client.model.GoogleConfig;
import com.formkiq.client.model.OcrConfig;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.client.model.UpdateConfigurationResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.testutils.api.SetBearers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /sites/{siteId}/configuration. */
public class ConfigurationRequestTest extends AbstractApiClientRequestTest {

  /** {@link ConfigService}. */
  private ConfigService config;
  /** Test RSA Private Key. */
  private static final String RSA_PRIVATE_KEY = """
      -----BEGIN RSA PRIVATE KEY-----
      MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDil6aKnJO2L7ih
      B0VP8D2mDnYaX/RmAe66nNQDX/fvnVD+sYk0Rf2uBXlpKmcJ/ZPFdXG7vNOgx7de
      V6tNVxbq7kmC7+KEuBMnUQW5dvUm3yFRaWrfmerSUJWcBGzXZhoH8WSsJ1P86vh4
      kkOXLa8zggGA3GKBjQvXjXwJKEZOSlGi/MUOUofLPU74TCeSg3LCpUC4+WZ0IY0T
      w3mnX83PkZFykSFVsxFsR2IRE6CLVHrQNExRrjoBcfXHesfn6bJs82/UUkDjXhIK
      I2ChgE9WUVDQTtv5YqOUAxNpk2iQu/+eMQIl0orJGQXkvQPsGC4wgyLpaIHJiwkK
      yYdCKNtTAgMBAAECggEAFbi479+7r0La3aDvTY73sfP/8V5SdPbpdj0ze9FW2MMJ
      cSj+wKKXA3gl3+V/NC95W3v7N6aN2QNcOjCITOU03reSF3m8isGEoIe9Vz6mmJ/a
      N042PxIntxqfhPHNp0Zz52AGKRSqEfxKbnCDBzqLaZIkZ8B4tveY84RuKAiS2M1L
      4AA+Pyd7JejyK1SuczigQEUcVoakLECxfO9hVd3CIuCWTCiqQRrukhSW0jqTrTna
      fgNOOEn1exkpTDlwjdQkCUMqgJ9F2HsyMXQ4eX6u6+GXr16HsGzA61ZVG31dZf9I
      xWZk9fUXSe1Pzw02p3jWbZ5P2CpofXJ0YMrHhS+S1QKBgQD1JfgnzlQIPWMuM2il
      jml6TzPI2AaJ8owTpRBSWMhqj7CRJgQrxVk78H5aQ/NDU6wGt9g60vSvxNgr6Ff5
      dWYwhrP24VMx0gHaWdjOP3nNnj4jxeXw0/QJeb/qOY0PXgFHgfecTvQIUmfNeJPS
      G0fBh0h6HcEkX1eSBqFNx5rGHQKBgQDsn2eU0b2n28I0pJTfecycgdAFFfFDmmzh
      IZGq/q4032YOl3J5+/WGePJfmboR4dddtaKdPJ/BfSPTt4ocpwJ76dNWKLDs+L8l
      lyICjb3gaj7VuBHL9JPPs/lt79kAFnFXXeIPVk6JWW5X2Ej9d5i0moeX4s8MFmeh
      LjPoEL+sLwKBgBshcZ5OKmSjDpftXpZ79VZw74U5yzd3HWOLMAw9ASkx79OQhoOl
      mqOUkRdCT+jSmMZBkG+qKyRMv7PUSfA0uvOB5Obctw1bdZMJwIHK6psD+VKSM0l8
      25Q04jV02xSpTbDxREsLPdyx6gUGZC2rkTxs0WuaYWa6GoHxs+ZcwddNAoGBANju
      TzUlkN16YLKIjJ/Q92AotsBi3Hyg7976OqTstmNsyBDqkZ35+5+b9IDm26qXRS35
      XqsOsFvgUV9BblJUXrehqAneZk3qwrtAsoJq1kAOx6qCBXbZtEWAd1VtxaEJ8kEp
      ph1vf7L2FW5dsJUH9yzkWxlJa45mX/1p8VZ5PHArAoGAZxeL5r6SX0Rv/hLkE1Cs
      v7od0OhmDPCMGm8djdpVMEFkhr0636c/r61IbycWI/c3k6njd4hhMSWfHmwDvO/a
      9i7pWanM1abM4es/tdATp56uTWntoa2ZQTHieFtSkYlufDbgLaCIb4HCStimbzye
      xm4Xo6+jDbifGOqFT4Ofeyc=
      -----END RSA PRIVATE KEY-----
           \s""";

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
    String siteId = DEFAULT_SITE_ID;
    String group = "Admins";
    SiteConfiguration siteConfig = new SiteConfiguration().setChatGptApiKey("somevalue");

    this.config.save(siteId, siteConfig);

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
    String siteId = DEFAULT_SITE_ID;

    SiteConfiguration siteConfig = new SiteConfiguration().setChatGptApiKey("somevalue");
    this.config.save(siteId, siteConfig);

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
    String siteId = ID.uuid();
    String group = "Admins";
    setBearerToken(group);

    SiteConfiguration siteConfig = new SiteConfiguration().setChatGptApiKey("somevalue");
    this.config.save(null, siteConfig);

    // when
    GetConfigurationResponse response = this.systemApi.getConfiguration(siteId);

    // then
    assertEquals("", response.getChatGptApiKey());
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
    String siteId = ID.uuid();
    String group = "Admins";
    setBearerToken(group);

    SiteConfiguration siteConfig0 = new SiteConfiguration().setChatGptApiKey("somevalue");
    this.config.save(null, siteConfig0);

    SiteConfiguration siteConfig1 = new SiteConfiguration().setChatGptApiKey("anothervalue");
    this.config.save(siteId, siteConfig1);

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
   * Get /config for Global SiteId.
   *
   */
  @Test
  public void testHandleGetConfigurationGlobal() {
    // given
    new SetBearers().apply(this.client, new String[] {"Admins", "other"});

    // when
    try {
      this.systemApi.getConfiguration("global");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"'global' siteId is reserved\"}", e.getResponseBody());
    }
  }

  /**
   * Get /config for API_KEY SiteId.
   *
   */
  @Test
  public void testHandleGetConfigurationApiKey() {
    // given
    new SetBearers().apply(this.client, new String[] {"Admins", "other"});

    // when
    try {
      this.systemApi.getConfiguration("API_KEY");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"'API_KEY' siteId is reserved\"}", e.getResponseBody());
    }
  }

  /**
   * PUT /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutConfiguration01() throws Exception {
    // given
    String siteId = DEFAULT_SITE_ID;
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
    assertNotNull(response.getOcr());
    assertEquals("-1", Objects.formatDouble(java.util.Objects
        .requireNonNull(response.getOcr().getMaxPagesPerTransaction()).doubleValue()));
    assertEquals("-1", Objects.formatDouble(
        java.util.Objects.requireNonNull(response.getOcr().getMaxTransactions()).doubleValue()));
  }

  /**
   * PATCH /config default as user.
   *
   */
  @Test
  public void testHandlePatchConfiguration02() {
    // given
    setBearerToken(DEFAULT_SITE_ID);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().chatGptApiKey("anotherkey")
        .maxContentLengthBytes("1000000").maxDocuments("1000").maxWebhooks("5");

    // when
    try {
      this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);
      fail();
    } catch (ApiException e) {
      final int code = 401;
      assertEquals(code, e.getCode());
    }
  }

  /**
   * PATCH google configuration /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchConfiguration03() throws Exception {
    // given
    String siteId = DEFAULT_SITE_ID;
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
   * PATCH google invalid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePatchConfiguration04() {
    // given
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req =
        new UpdateConfigurationRequest().google(new GoogleConfig().workloadIdentityAudience("123"));

    // when
    try {
      this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);
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
   * PATCH google invalid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePatchConfiguration05() {
    // given
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest()
        .docusign(new DocusignConfig().integrationKey("111").rsaPrivateKey("222"));

    // when
    try {
      this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);
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
   * PATCH docusign valid configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePatchConfiguration06() throws ApiException {
    // given
    String siteId = DEFAULT_SITE_ID;
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest()
        .docusign(new DocusignConfig().userId("123").integrationKey("111")
            .rsaPrivateKey(RSA_PRIVATE_KEY).hmacSignature("222ljasdlksjakldjsadlsa"));

    this.systemApi.updateConfiguration(siteId, req);

    // when
    GetConfigurationResponse configuration = this.systemApi.getConfiguration(siteId);

    // then
    assertNotNull(configuration.getDocusign());
    assertEquals("111", configuration.getDocusign().getIntegrationKey());
    assertEquals("123", configuration.getDocusign().getUserId());
    assertEquals("222l*******dlsa", configuration.getDocusign().getHmacSignature());
    assertEquals("""
        -----BEGIN RSA PRIVATE KEY-----
        MIIEvQIB*******qFT4Ofeyc=
        -----END RSA PRIVATE KEY-----""", configuration.getDocusign().getRsaPrivateKey());
  }

  /**
   * PATCH docusign invalid RSA key configuration /config default as Admin.
   *
   */
  @Test
  public void testHandlePatchConfiguration07() {
    // given
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().docusign(
        new DocusignConfig().userId("123").integrationKey("111").rsaPrivateKey("3423432432432423"));

    // when
    try {
      this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"docusignRsaPrivateKey\","
          + "\"error\":\"invalid RSA Private Key\"}]}", e.getResponseBody());
    }
  }

  /**
   * PATCH OCR.
   *
   */
  @Test
  public void testHandlePatchConfiguration08() throws ApiException {
    // given
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest().ocr(new OcrConfig()
        .maxPagesPerTransaction(new BigDecimal(2)).maxTransactions(new BigDecimal(1)));

    // when
    UpdateConfigurationResponse response = this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);

    // then
    assertEquals("Config saved", response.getMessage());

    GetConfigurationResponse c = this.systemApi.getConfiguration(DEFAULT_SITE_ID);
    assertNotNull(c.getOcr());
    assertEquals("2", Objects.formatDouble(
        java.util.Objects.requireNonNull(c.getOcr().getMaxPagesPerTransaction()).doubleValue()));
    assertEquals("1", Objects.formatDouble(
        java.util.Objects.requireNonNull(c.getOcr().getMaxTransactions()).doubleValue()));
  }

  /**
   * PATCH empty request.
   *
   */
  @Test
  public void testHandlePatchConfiguration09() {
    // given
    String group = "Admins";
    setBearerToken(group);

    UpdateConfigurationRequest req = new UpdateConfigurationRequest();

    // when
    try {
      this.systemApi.updateConfiguration(DEFAULT_SITE_ID, req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"missing required body parameters\"}", e.getResponseBody());
    }
  }
}
