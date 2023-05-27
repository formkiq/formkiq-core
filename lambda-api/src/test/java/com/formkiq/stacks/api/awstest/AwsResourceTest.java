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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.formkiq.aws.cognito.CognitoConnectionBuilder;
import com.formkiq.aws.cognito.CognitoService;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentsRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

/**
 * Test CloudFormation.
 */
public class AwsResourceTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 80000;
  /** Temporary Cognito Password. */
  private static final String USER_TEMP_PASSWORD = "TEMPORARY_PASSWORd1!";

  /**
   * Test Having Admin add new user to group.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testAdminAddUserToGroup() {
    // given
    String email = UUID.randomUUID() + "@formkiq.com";
    String group =
        getParameterStoreValue("/formkiq/" + getAppenvironment() + "/cognito/AdminGroup");
    Credentials cred = getAdminCognitoService().getCredentials(getAdminToken());

    AwsCredentials basic =
        AwsSessionCredentials.create(cred.accessKeyId(), cred.secretKey(), cred.sessionToken());

    CognitoConnectionBuilder userBuilder =
        createCognitoConnectionBuilder().setCredentials(StaticCredentialsProvider.create(basic));

    CognitoService userCognitoService = new CognitoService(userBuilder);

    // when
    GetUserResponse user = userCognitoService.getUser(getAdminToken());
    userCognitoService.addUser(email, USER_TEMP_PASSWORD);
    userCognitoService.addUserToGroup(email, group);

    // then
    assertNotNull(user.username());
    assertEquals("testadminuser@formkiq.com", user.userAttributes().stream()
        .filter(f -> f.name().equals("email")).findFirst().get().value());
  }

  /**
   * Tests hitting apirequesthandler.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testApiDocuments01() throws IOException, URISyntaxException, InterruptedException {
    // given
    final int year = 2019;
    final int month = 8;
    final int day = 15;
    LocalDate localDate = LocalDate.of(year, month, day);

    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
      GetDocumentsRequest request = new GetDocumentsRequest().date(date).tz("+0500");

      // when
      HttpResponse<String> response = client.getDocumentsAsHttpResponse(request);

      // then
      assertRequestCorsHeaders(response.headers());
      final int status = 200;
      assertEquals(status, response.statusCode());
      assertEquals("{\"documents\":[]}", response.body());
    }
  }



  /**
   * Tests Getting a Presign URL which will create a record in DynamoDB for the document. Then we 1)
   * search for the /documents by TZ.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test(timeout = TEST_TIMEOUT)
  public void testPresignedUrl01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      final String documentId = addDocumentWithoutFile(client, null, null);

      try {
        Date lastHour =
            Date.from(LocalDateTime.now().minusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        String tz = String.format("%tz", Instant.now().atZone(ZoneId.systemDefault()));

        GetDocumentsRequest request = new GetDocumentsRequest().date(lastHour).tz(tz);
        List<Map<String, Map<String, String>>> list = Collections.emptyList();

        // when
        while (list.isEmpty()) {
          HttpResponse<String> response = client.getDocumentsAsHttpResponse(request);

          // then
          final int status = 200;
          assertRequestCorsHeaders(response.headers());
          assertEquals(status, response.statusCode());

          Map<String, Object> map = toMap(response);
          list = (List<Map<String, Map<String, String>>>) map.get("documents");

          if (list.isEmpty()) {
            TimeUnit.SECONDS.sleep(1);
          }
        }

        assertFalse(list.isEmpty());
        assertNotNull(list.get(0).get("documentId"));

      } finally {
        deleteDocument(client, documentId);
      }
    }
  }

  /**
   * Test SSM Parameter Store.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSsmParameters() {
    assertTrue(getParameterStoreValue("/formkiq/" + getAppenvironment() + "/api/DocumentsHttpUrl")
        .endsWith(getAwsRegion() + ".amazonaws.com"));
    assertTrue(
        getParameterStoreValue("/formkiq/" + getAppenvironment() + "/api/DocumentsPublicHttpUrl")
            .endsWith(getAwsRegion() + ".amazonaws.com"));
    assertTrue(getParameterStoreValue("/formkiq/" + getAppenvironment() + "/api/DocumentsIamUrl")
        .endsWith(getAwsRegion() + ".amazonaws.com"));
    assertTrue(
        getParameterStoreValue("/formkiq/" + getAppenvironment() + "/lambda/DocumentsApiRequests")
            .contains("-DocumentsApiRequests"));
    assertEquals("Admins",
        getParameterStoreValue("/formkiq/" + getAppenvironment() + "/cognito/AdminGroup"));
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testUserAddSelfToAdmin() {
    getAdminCognitoService().getCredentials(login(USER_EMAIL, USER_PASSWORD));
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFinanceUserAddSelfToAdmin() {
    getAdminCognitoService().getCredentials(login(FINANCE_EMAIL, USER_PASSWORD));
  }

  /**
   * Test Having User try and add itself to the admin group. Should fail.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testReadonlyUserAddSelfToAdmin() {
    getAdminCognitoService().getCredentials(login(READONLY_EMAIL, USER_PASSWORD));
  }
}
