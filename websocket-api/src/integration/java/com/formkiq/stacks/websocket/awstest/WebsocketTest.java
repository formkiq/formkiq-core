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
package com.formkiq.stacks.websocket.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.cognito.CognitoConnectionBuilder;
import com.formkiq.aws.cognito.CognitoService;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * 
 * Test Sending Emails.
 *
 */
public class WebsocketTest {

  /** {@link CognitoService}. */
  private static CognitoService adminCognitoService;

  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** Cognito Group. */
  private static final String GROUP = "test9843";
  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  /** FormKiQ Http API Client. */
  private static FormKiqClientV1 httpClient;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** Temporary Cognito Password. */
  private static final String TEMP_USER_PASSWORD = "TEMPORARY_PASSWORd1!";
  /** Test Timeout. */
  private static final int TIMEOUT = 15000;
  /** {@link AuthenticationResultType}. */
  private static AuthenticationResultType token;
  /** Cognito User Email. */
  private static final String USER_EMAIL = "testuser14@formkiq.com";
  /** Cognito User Password. */
  private static final String USER_PASSWORD = TEMP_USER_PASSWORD + "!";
  /** Web Connections Table. */
  private static String webconnectionsTable;
  /** WebSocket SQS Url. */
  private static String websocketSqsUrl;
  /** WebSocket URL. */
  private static String websocketUrl;

  /**
   * Add User and/or Login Cognito.
   * 
   * @param username {@link String}
   * @param groupName {@link String}
   */
  private static void addAndLoginCognito(final String username, final String groupName) {

    if (!adminCognitoService.isUserExists(username)) {

      adminCognitoService.addUser(username, TEMP_USER_PASSWORD);
      adminCognitoService.loginWithNewPassword(username, TEMP_USER_PASSWORD, USER_PASSWORD);

      if (groupName != null) {
        adminCognitoService.addGroup(groupName);
        adminCognitoService.addUserToGroup(username, groupName);
      }

    } else {

      AdminGetUserResponse user = adminCognitoService.getUser(username);
      if (UserStatusType.FORCE_CHANGE_PASSWORD.equals(user.userStatus())) {
        adminCognitoService.loginWithNewPassword(username, TEMP_USER_PASSWORD, USER_PASSWORD);
      }
    }
  }

  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {

    Region awsregion = Region.of(System.getProperty("testregion"));
    String awsprofile = System.getProperty("testprofile");
    String app = System.getProperty("testappenvironment");

    SqsConnectionBuilder sqsConnection =
        new SqsConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion);

    sqsService = new SqsService(sqsConnection);

    SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion);

    SsmService ssmService = new SsmServiceImpl(ssmBuilder);

    websocketSqsUrl = ssmService.getParameterValue("/formkiq/" + app + "/sqs/WebsocketUrl");

    websocketUrl = ssmService.getParameterValue("/formkiq/" + app + "/api/WebsocketUrl");

    String cognitoUserPoolId =
        ssmService.getParameterValue("/formkiq/" + app + "/cognito/UserPoolId");

    String cognitoClientId =
        ssmService.getParameterValue("/formkiq/" + app + "/cognito/UserPoolClientId");

    String cognitoIdentitypool =
        ssmService.getParameterValue("/formkiq/" + app + "/cognito/IdentityPoolId");

    CognitoConnectionBuilder adminBuilder =
        new CognitoConnectionBuilder(cognitoClientId, cognitoUserPoolId, cognitoIdentitypool)
            .setCredentials(awsprofile).setRegion(awsregion);
    adminCognitoService = new CognitoService(adminBuilder);

    addAndLoginCognito(USER_EMAIL, GROUP);
    token = adminCognitoService.login(USER_EMAIL, USER_PASSWORD);

    String rootHttpUrl = ssmService.getParameterValue("/formkiq/" + app + "/api/DocumentsHttpUrl");

    FormKiqClientConnection connection = new FormKiqClientConnection(rootHttpUrl)
        .cognitoIdToken(token.idToken()).header("Origin", Arrays.asList("http://localhost"))
        .header("Access-Control-Request-Method", Arrays.asList("GET"));

    httpClient = new FormKiqClientV1(connection);

    webconnectionsTable =
        ssmService.getParameterValue("/formkiq/" + app + "/dynamodb/WebConnectionsTableName");
    dbConnection =
        new DynamoDbConnectionBuilder(false).setCredentials(awsprofile).setRegion(awsregion);
  }

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Add Document Tag.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void addDocumentTag(final FormKiqClientV1 client, final String documentId)
      throws IOException, InterruptedException {
    AddDocumentTagRequest request = new AddDocumentTagRequest().documentId(documentId)
        .tagKey("test").tagValue("somevalue").webnotify(true);
    HttpResponse<String> response = client.addDocumentTagAsHttpResponse(request);
    assertEquals("201", String.valueOf(response.statusCode()));
  }

  /**
   * Add "file" but this just creates DynamoDB record and not the S3 file.
   * 
   * @param client {@link FormKiqClientV1}
   * @return {@link String}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private String addDocumentWithoutFile(final FormKiqClientV1 client)
      throws IOException, URISyntaxException, InterruptedException {
    // given
    final int status = 200;
    final String content = "sample content";
    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().contentLength(content.length());

    // when
    HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

    // then
    assertEquals(status, response.statusCode());

    Map<String, Object> map = GSON.fromJson(response.body(), Map.class);
    assertNotNull(map.get("documentId"));
    assertNotNull(map.get("url"));

    String s3url = map.get("url").toString();
    response =
        this.http.send(HttpRequest.newBuilder(new URI(s3url)).header("Content-Type", "text/plain")
            .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());

    assertEquals(status, response.statusCode());

    return map.get("documentId").toString();
  }

  /**
   * Test Connecting with missing Authentication header.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testConnectMissingAuthenticationHeader() throws Exception {
    // given
    WebSocketClientImpl client = new WebSocketClientImpl(new URI(websocketUrl));

    // when
    client.connectBlocking();

    // then
    assertFalse(client.isOnOpen());
    assertTrue(client.isOnClose());
    assertEquals(0, client.getMessages().size());
    assertEquals(0, client.getErrors().size());
    assertEquals("1002", String.valueOf(client.getCloseCode()));

    // given
    // when
    client.closeBlocking();

    // then
    assertFalse(client.isOnOpen());
    assertTrue(client.isOnClose());
    assertEquals(0, client.getMessages().size());
    assertEquals(0, client.getErrors().size());
    assertEquals("1002", String.valueOf(client.getCloseCode()));
  }

  /**
   * Test Connecting with valid credentials.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TIMEOUT)
  public void testConnectValidAuthentication01() throws Exception {

    for (Boolean useHeader : Arrays.asList(Boolean.TRUE, Boolean.FALSE)) {
      // given
      final int sleep = 500;
      WebSocketClientImpl client = null;

      if (useHeader.booleanValue()) {
        client = new WebSocketClientImpl(new URI(websocketUrl));
        client.addHeader("Authentication", token.idToken());
      } else {
        client =
            new WebSocketClientImpl(new URI(websocketUrl + "?Authentication=" + token.idToken()));
      }

      // when
      client.connectBlocking();

      // then
      assertTrue(client.isOnOpen());
      assertFalse(client.isOnClose());
      assertEquals(0, client.getMessages().size());
      assertEquals(0, client.getErrors().size());
      assertEquals("-1", String.valueOf(client.getCloseCode()));

      // given

      // when
      sqsService.sendMessage(websocketSqsUrl,
          "{\"siteId\":\"" + GROUP + "\",\"message\":\"this is a test\"}");

      // then
      while (true) {
        if (!client.getMessages().isEmpty()) {
          assertEquals(1, client.getMessages().size());
          assertEquals("{\"message\":\"{\\\"siteId\\\":\\\"test9843\\\","
              + "\\\"message\\\":\\\"this is a test\\\"}\"}", client.getMessages().get(0));
          break;
        }

        Thread.sleep(sleep);
      }

      // given
      // when
      client.closeBlocking();

      // then
      assertTrue(client.isOnOpen());
      assertTrue(client.isOnClose());
      assertEquals(1, client.getMessages().size());
      assertEquals("{\"message\":\"{\\\"siteId\\\":\\\"test9843\\\","
          + "\\\"message\\\":\\\"this is a test\\\"}\"}", client.getMessages().get(0));
      assertEquals(0, client.getErrors().size());
      assertEquals("1000", String.valueOf(client.getCloseCode()));

      Thread.sleep(sleep * 2);
      verifyDbConnections();
    }
  }

  /**
   * Test Receiving Web Notify.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TIMEOUT)
  public void testWebNotify01() throws Exception {
    // given
    final int sleep = 500;
    WebSocketClientImpl client = new WebSocketClientImpl(new URI(websocketUrl));
    client.addHeader("Authentication", token.idToken());
    client.connectBlocking();

    // when
    String documentId = addDocumentWithoutFile(httpClient);
    addDocumentTag(httpClient, documentId);

    // then
    while (true) {
      if (!client.getMessages().isEmpty()) {
        assertEquals(1, client.getMessages().size());
        assertEquals("{\"message\":\"{\\\"siteId\\\":\\\"test9843\\\"," + "\\\"documentId\\\":\\\""
            + documentId + "\\\","
            + "\\\"message\\\":\\\"{\\\\\\\"value\\\\\\\":\\\\\\\"somevalue\\\\\\\","
            + "\\\\\\\"key\\\\\\\":\\\\\\\"test\\\\\\\"}\\\"}\"}", client.getMessages().get(0));
        break;
      }

      Thread.sleep(sleep);
    }

    assertEquals(0, client.getErrors().size());
  }

  private void verifyDbConnections() {
    try (DynamoDbClient client = dbConnection.build()) {
      ScanResponse response =
          client.scan(ScanRequest.builder().tableName(webconnectionsTable).build());
      assertEquals(0, response.count().intValue());
    }
  }
}
