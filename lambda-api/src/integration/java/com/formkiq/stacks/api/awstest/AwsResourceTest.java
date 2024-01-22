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
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.cognito.CognitoIdentityConnectionBuilder;
import com.formkiq.aws.cognito.CognitoIdentityProviderConnectionBuilder;
import com.formkiq.aws.cognito.CognitoIdentityProviderService;
import com.formkiq.aws.cognito.CognitoIdentityService;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;


/**
 * Test CloudFormation.
 */
public class AwsResourceTest extends AbstractAwsIntegrationTest {
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 80;
  /** Temporary Cognito Password. */
  private static final String USER_TEMP_PASSWORD = "TEMPORARY_PASSWORd1!";
  /** Cognito User Pool Id. */
  private static String cognitoUserPoolId;
  /** Cognito Client Id. */
  private static String cognitoClientId;
  /** Cognito Identity Pool. */
  private static String cognitoIdentitypool;
  /** Cognito User Email. */
  private static final String READONLY_EMAIL = "readonly5857@formkiq.com";
  /** Cognito User Email. */
  private static final String USER_EMAIL = "testuser5857@formkiq.com";
  /** Cognito FINANCE User Email. */
  private static final String FINANCE_EMAIL = "testfinance5857@formkiq.com";
  /** Finance Group. */
  private static final String FINANCE_GROUP = "testfinance5857";

  /**
   * BeforeAll.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws IOException, InterruptedException, URISyntaxException {
    AbstractAwsIntegrationTest.beforeClass();

    cognitoUserPoolId =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/cognito/UserPoolId");

    cognitoClientId =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/cognito/UserPoolClientId");

    cognitoIdentitypool =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/cognito/IdentityPoolId");

    addAndLoginCognito(READONLY_EMAIL, Arrays.asList("default_read"));
    addAndLoginCognito(USER_EMAIL, Arrays.asList(DEFAULT_SITE_ID));
    addAndLoginCognito(FINANCE_EMAIL, Arrays.asList(FINANCE_GROUP));
  }

  /**
   * Test Having Admin add new user to group.
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAdminAddUserToGroup() {
    // given
    String email = UUID.randomUUID() + "@formkiq.com";
    String group =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/cognito/AdminGroup");
    Credentials cred = getAdminCognitoIdentityService().getCredentials(getAdminToken());

    AwsCredentials basic =
        AwsSessionCredentials.create(cred.accessKeyId(), cred.secretKey(), cred.sessionToken());

    CognitoIdentityProviderConnectionBuilder userBuilder =
        new CognitoIdentityProviderConnectionBuilder(cognitoClientId, cognitoUserPoolId)
            .setRegion(getAwsregion()).setCredentials(StaticCredentialsProvider.create(basic));

    CognitoIdentityProviderService userCognitoService =
        new CognitoIdentityProviderService(userBuilder);

    // when
    userCognitoService.addUser(email, USER_TEMP_PASSWORD);
    userCognitoService.addUserToGroup(email, group);

    // then
    GetUserResponse user = userCognitoService.getUser(getAdminToken());
    assertNotNull(user.username());
    assertEquals("testadminuser123@formkiq.com", user.userAttributes().stream()
        .filter(f -> f.name().equals("email")).findFirst().get().value());
  }

  private CognitoIdentityService getAdminCognitoIdentityService() {

    CognitoIdentityConnectionBuilder adminIdentityBuilder =
        new CognitoIdentityConnectionBuilder(cognitoClientId, cognitoUserPoolId,
            cognitoIdentitypool).setCredentials(getAwsprofile()).setRegion(getAwsregion());

    return new CognitoIdentityService(adminIdentityBuilder);
  }

  /**
   * Tests hitting apirequesthandler.
   * 
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testApiDocuments01() throws ApiException {
    // given
    for (ApiClient apiClient : getApiClients(null)) {

      // given
      DocumentsApi api = new DocumentsApi(apiClient);

      // when
      GetDocumentsResponse documents =
          api.getDocuments(null, null, null, "2010-01-01", "+0500", null, null, null);

      // then
      assertTrue(documents.getDocuments().isEmpty());
    }
  }

  /**
   * Tests Getting a Presign URL which will create a record in DynamoDB for the document. Then we 1)
   * search for the /documents by TZ.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPresignedUrl01() throws Exception {
    for (ApiClient apiClient : getApiClients(null)) {
      // given
      String siteId = null;
      DocumentsApi api = new DocumentsApi(apiClient);
      final String documentId = addDocument(apiClient, siteId, "test.txt",
          "content".getBytes(StandardCharsets.UTF_8), "text/plain", null);

      try {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date lastHour =
            Date.from(LocalDateTime.now().minusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        String tz = String.format("%tz", Instant.now().atZone(ZoneId.systemDefault()));

        List<Document> list = Collections.emptyList();

        // when
        while (list.isEmpty()) {
          GetDocumentsResponse documents =
              api.getDocuments(siteId, null, null, df.format(lastHour), tz, null, null, null);

          // then
          list = documents.getDocuments();

          if (list.isEmpty()) {
            TimeUnit.SECONDS.sleep(1);
          }
        }

        assertFalse(list.isEmpty());
        assertNotNull(list.get(0).getDocumentId());

      } finally {
        api.deleteDocument(documentId, siteId, Boolean.FALSE);
      }
    }
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testSsmParameters() {
    assertTrue(
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/api/DocumentsHttpUrl")
            .endsWith(getAwsregion() + ".amazonaws.com"));
    assertTrue(getSsm()
        .getParameterValue("/formkiq/" + getAppenvironment() + "/api/DocumentsPublicHttpUrl")
        .endsWith(getAwsregion() + ".amazonaws.com"));
    assertTrue(
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/api/DocumentsIamUrl")
            .endsWith(getAwsregion() + ".amazonaws.com"));
    assertTrue(getSsm()
        .getParameterValue("/formkiq/" + getAppenvironment() + "/lambda/DocumentsApiRequests")
        .contains("-DocumentsApiRequests"));
    assertEquals("Admins",
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/cognito/AdminGroup"));
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test
  public void testUserAddSelfToAdmin() {
    try {
      getAdminCognitoIdentityService().getCredentials(login(USER_EMAIL, USER_PASSWORD));
      fail();
    } catch (NotAuthorizedException e) {
      assertTrue(true);
    }
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test
  public void testFinanceUserAddSelfToAdmin() {
    try {
      getAdminCognitoIdentityService().getCredentials(login(FINANCE_EMAIL, USER_PASSWORD));
      fail();
    } catch (NotAuthorizedException e) {
      assertTrue(true);
    }
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test
  public void testReadonlyUserAddSelfToAdmin() {
    try {
      getAdminCognitoIdentityService().getCredentials(login(READONLY_EMAIL, USER_PASSWORD));
      fail();
    } catch (NotAuthorizedException e) {
      assertTrue(true);
    }
  }

  private AuthenticationResultType login(final String username, final String password) {
    return getCognito().login(username, password);
  }
}
