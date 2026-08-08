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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetConfigurationResponse;
import com.formkiq.client.model.NotificationConfig;
import com.formkiq.client.model.NotificationEmailProvider;
import com.formkiq.client.model.NotificationEmailSmtpConfig;
import com.formkiq.client.model.NotificationEmailSmtpConnectionSecurity;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.client.model.UpdateConfigurationResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;

/**
 * Process Urls.
 * <p>
 * GET /configuration tests
 * </p>
 *
 */
public class ConfigurationRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /** Cognito FINANCE User Email. */
  private static final String FINANCE_EMAIL = "test13555@formkiq.com";

  /**
   * Test GET /configuration.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetConfiguration01() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    final int expected = 3;
    List<ApiClient> clients = getApiClients(siteId);
    assertEquals(expected, clients.size());

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {

      SystemManagementApi api = new SystemManagementApi(client);

      // when
      GetConfigurationResponse c = api.getConfiguration(siteId);

      // then
      assertNotNull(c.getChatGptApiKey());
    }

    // given
    ApiClient fc = clients.get(2);
    SystemManagementApi api = new SystemManagementApi(fc);

    // when
    GetConfigurationResponse c = api.getConfiguration(siteId);

    // then
    assertEquals("", c.getMaxContentLengthBytes());
    assertEquals("", c.getMaxDocuments());
  }

  /**
   * Test GET /configuration and PATCH /configuration as readuser user.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetConfiguration02() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    String chatGptApiKey = "1239123123";
    addAndLoginCognito(FINANCE_EMAIL, List.of("default_read"));

    List<ApiClient> apiClients = getApiClients(siteId);
    SystemManagementApi api = new SystemManagementApi(apiClients.getFirst());
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().chatGptApiKey(chatGptApiKey);
    api.updateConfiguration(siteId, req);

    assertEquals("1239*******3123", api.getConfiguration(siteId).getChatGptApiKey());

    ApiClient readOnlyClient = getApiClientForUser(FINANCE_EMAIL, USER_PASSWORD);
    api = new SystemManagementApi(readOnlyClient);

    // when
    GetConfigurationResponse configuation = api.getConfiguration(siteId);

    // then
    assertEquals("1239*******3123", configuation.getChatGptApiKey());

    // when - update config
    try {
      api.updateConfiguration(siteId, req);
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }

  }

  /**
   * Test PATCH /configuration as admin.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPatchConfiguration02() throws Exception {
    // given
    final int expected = 3;
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    List<ApiClient> clients = getApiClients(null);
    assertEquals(expected, clients.size());

    String chatGptKey = "aklsdjsalkdjsakldjsadjadad";
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().chatGptApiKey(chatGptKey);

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {
      SystemManagementApi api = new SystemManagementApi(client);

      // when
      UpdateConfigurationResponse response = api.updateConfiguration(siteId, req);

      // then
      assertEquals("Config saved", response.getMessage());

      GetConfigurationResponse config = api.getConfiguration(siteId);
      assertEquals("akls*******adad", config.getChatGptApiKey());
    }

    // given
    ApiClient client = clients.get(2);
    SystemManagementApi api = new SystemManagementApi(client);

    // when
    try {
      api.updateConfiguration(siteId, req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }

  /**
   * PUT /config notification email.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPatchConfiguration03() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    List<ApiClient> clients = getApiClients(null);

    String adminEmail =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/console/AdminEmail");
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().notificationEmail(adminEmail);

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {
      SystemManagementApi api = new SystemManagementApi(client);

      // when
      UpdateConfigurationResponse updateConfiguration = api.updateConfiguration(siteId, req);

      // then
      assertEquals("Config saved", updateConfiguration.getMessage());
    }
  }

  /**
   * PUT /config invalid notification email.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPatchConfiguration04() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    List<ApiClient> clients = getApiClients(null);

    String email = "test@formkiq.com";
    UpdateConfigurationRequest req = new UpdateConfigurationRequest().notificationEmail(email);

    for (ApiClient client : Arrays.asList(clients.get(0), clients.get(1))) {
      SystemManagementApi api = new SystemManagementApi(client);

      // when
      try {
        api.updateConfiguration(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        String expected = "{\"errors\":[{\"key\":\"notificationEmail\","
            + "\"error\":\"'notificationEmail' is not setup in AWS SES\"}]}";
        assertEquals(expected, e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /config with an SES notification provider.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPatchConfiguration05() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    ApiClient client = getApiClients(null).getFirst();
    SystemManagementApi api = new SystemManagementApi(client);
    String adminEmail =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/console/AdminEmail");
    NotificationConfig notification =
        new NotificationConfig().email(adminEmail).provider(NotificationEmailProvider.SES);
    UpdateConfigurationRequest request =
        new UpdateConfigurationRequest().notification(notification);

    // when
    UpdateConfigurationResponse update = api.updateConfiguration(siteId, request);
    GetConfigurationResponse configuration = api.getConfiguration(siteId);

    // then
    assertEquals("Config saved", update.getMessage());
    NotificationConfig actual = configuration.getNotification();
    assertNotNull(actual);
    assertEquals(adminEmail, actual.getEmail());
    assertEquals(NotificationEmailProvider.SES, actual.getProvider());
  }

  /**
   * PATCH /config with an SMTP notification provider.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPatchConfiguration06() throws Exception {
    // given
    String siteId = SiteIdKeyGenerator.DEFAULT_SITE_ID;
    ApiClient client = getApiClients(null).getFirst();
    SystemManagementApi api = new SystemManagementApi(client);
    String email = "notifications@example.com";
    UpdateConfigurationResponse update;
    GetConfigurationResponse configuration;
    String secretArn;
    try (
        ProfileCredentialsProvider credentials = ProfileCredentialsProvider.create(getAwsprofile());
        SecretsManagerClient secrets = SecretsManagerClient.builder()
            .credentialsProvider(credentials).region(getAwsregion()).build()) {
      secretArn =
          secrets.createSecret(CreateSecretRequest.builder().name("formkiq-smtp-test-" + ID.ulid())
              .secretString("{\"username\":\"user\",\"password\":\"password\"}").build()).arn();
      NotificationEmailSmtpConfig smtp = new NotificationEmailSmtpConfig().host("smtp.gmail.com")
          .port(587).connectionSecurity(NotificationEmailSmtpConnectionSecurity.STARTTLS)
          .credentialsSecretArn(secretArn);
      NotificationConfig notification =
          new NotificationConfig().email(email).provider(NotificationEmailProvider.SMTP).smtp(smtp);
      UpdateConfigurationRequest request =
          new UpdateConfigurationRequest().notification(notification);

      // when
      try {
        update = api.updateConfiguration(siteId, request);
        configuration = api.getConfiguration(siteId);
      } finally {
        secrets.deleteSecret(DeleteSecretRequest.builder().secretId(secretArn)
            .forceDeleteWithoutRecovery(true).build());
      }
    }

    // then
    assertEquals("Config saved", update.getMessage());
    NotificationConfig actual = configuration.getNotification();
    assertNotNull(actual);
    assertEquals(email, actual.getEmail());
    assertEquals(NotificationEmailProvider.SMTP, actual.getProvider());
    NotificationEmailSmtpConfig actualSmtp = actual.getSmtp();
    assertNotNull(actualSmtp);
    assertEquals("smtp.gmail.com", actualSmtp.getHost());
    assertEquals(587, actualSmtp.getPort());
    assertEquals(NotificationEmailSmtpConnectionSecurity.STARTTLS,
        actualSmtp.getConnectionSecurity());
    assertEquals(secretArn, actualSmtp.getCredentialsSecretArn());
  }
}
