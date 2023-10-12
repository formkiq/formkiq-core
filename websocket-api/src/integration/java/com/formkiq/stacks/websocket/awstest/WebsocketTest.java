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

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocumentTag;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupExistsException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * 
 * Test Sending Emails.
 *
 */
public class WebsocketTest extends AbstractAwsIntegrationTest {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** Cognito Group. */
  private static final String GROUP = "test9843";
  /** Test Timeout. */
  private static final int TIMEOUT = 15;
  /** {@link AuthenticationResultType}. */
  private static AuthenticationResultType token;
  /** Cognito User Email. */
  private static final String USER_EMAIL = "testuser14@formkiq.com";
  /** Web Connections Table. */
  private static String webconnectionsTable;
  /** WebSocket SQS Url. */
  private static String websocketSqsUrl;
  /** WebSocket URL. */
  private static String websocketUrl;

  /**
   * beforeclass.
   * 
   * @throws URISyntaxException IOException
   * @throws InterruptedException InterruptedException
   * @throws IOException URISyntaxException
   */
  @BeforeAll
  public static void beforeClass() throws IOException, InterruptedException, URISyntaxException {
    AbstractAwsIntegrationTest.beforeClass();

    getCognito().addUser(USER_EMAIL, USER_PASSWORD);

    try {
      getCognito().addGroup(GROUP);
    } catch (GroupExistsException e) {
      // ignore
    }

    getCognito().addUserToGroup(USER_EMAIL, GROUP);

    token = getCognito().login(USER_EMAIL, USER_PASSWORD);

    websocketSqsUrl =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/sqs/WebsocketUrl");

    websocketUrl =
        getSsm().getParameterValue("/formkiq/" + getAppenvironment() + "/api/WebsocketUrl");

    webconnectionsTable = getSsm()
        .getParameterValue("/formkiq/" + getAppenvironment() + "/dynamodb/WebConnectionsTableName");
    dbConnection = new DynamoDbConnectionBuilder(false).setCredentials(getAwsprofile())
        .setRegion(getAwsregion());
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
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TIMEOUT)
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
      SendMessageResponse sendMessage = getSqs().sendMessage(websocketSqsUrl,
          "{\"siteId\":\"" + GROUP + "\",\"message\":\"this is a test\"}");

      // then
      assertNotNull(sendMessage.messageId());
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
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TIMEOUT)
  @SuppressWarnings("unchecked")
  public void testWebNotify01() throws Exception {
    // given
    String siteId = GROUP;
    final int sleep = 500;
    final String content = "sample content";
    WebSocketClientImpl client = new WebSocketClientImpl(new URI(websocketUrl));
    client.addHeader("Authentication", token.idToken());
    client.connectBlocking();

    List<ApiClient> apiClients = getApiClients(siteId);

    // when
    String documentId =
        addDocument(apiClients.get(0), siteId, "content.txt", content, "text/plain", null);
    waitForDocumentContent(apiClients.get(0), siteId, documentId);
    addDocumentTag(apiClients.get(0), siteId, documentId, "test", "somevalue");

    // then
    while (true) {
      if (!client.getMessages().isEmpty()) {
        assertEquals(1, client.getMessages().size());
        String s = client.getMessages().get(0);

        Map<String, Object> map = this.gson.fromJson(s, Map.class);
        s = map.get("message").toString();

        map = this.gson.fromJson(s, Map.class);
        assertEquals(GROUP, map.get("siteId"));
        assertEquals(documentId, map.get("documentId"));

        s = map.get("message").toString();
        map = this.gson.fromJson(s, Map.class);
        List<Map<String, String>> tags = (List<Map<String, String>>) map.get("tags");
        assertEquals(1, tags.size());
        assertEquals("test", tags.get(0).get("key"));
        assertEquals("somevalue", tags.get(0).get("value"));
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
