/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.websocket.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.cognito.CognitoConnectionBuilder;
import com.formkiq.aws.cognito.CognitoService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

/**
 * 
 * Test Sending Emails.
 *
 */
public class WebsocketTest {
  
  /** Test Timeout. */
  private static final int TIMEOUT = 15000;
  /** Cognito User Email. */
  private static final String USER_EMAIL = "testuser14@formkiq.com";
  /** Cognito Group. */
  private static final String GROUP = "test9843";
  /** Temporary Cognito Password. */
  private static final String USER_TEMP_PASSWORD = "TEMPORARY_PASSWORd1!";
  /** Cognito User Password. */
  private static final String USER_PASSWORD = USER_TEMP_PASSWORD + "!";
  
  /** WebSocket SQS Url. */
  private static String websocketSqsUrl;
  /** WebSocket URL. */
  private static String websocketUrl;
  /** {@link CognitoService}. */
  private static CognitoService adminCognitoService;
  /** {@link AuthenticationResultType}. */
  private static AuthenticationResultType token;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  
  /**
   * beforeclass.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {

    System.setProperty("testregion", "us-east-1");
    System.setProperty("testprofile", "formkiqtest");
    System.setProperty("testappenvironment", "test");

    Region awsregion = Region.of(System.getProperty("testregion"));
    String awsprofile = System.getProperty("testprofile");
    String app = System.getProperty("testappenvironment");

    SqsConnectionBuilder sqsConnection =
        new SqsConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

    sqsService = new SqsService(sqsConnection);
    
    SsmConnectionBuilder ssmBuilder =
        new SsmConnectionBuilder().setCredentials(awsprofile).setRegion(awsregion);

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
  }
  
  /**
   * Add User and/or Login Cognito.
   * 
   * @param username {@link String}
   * @param groupName {@link String}
   */
  private static void addAndLoginCognito(final String username, final String groupName) {
    
    if (!adminCognitoService.isUserExists(username)) {

      adminCognitoService.addUser(username, USER_TEMP_PASSWORD);
      adminCognitoService.loginWithNewPassword(username, USER_TEMP_PASSWORD, USER_PASSWORD);

      if (groupName != null) {
        adminCognitoService.addGroup(groupName);
        adminCognitoService.addUserToGroup(username, groupName);
      }
      
    } else {

      AdminGetUserResponse user = adminCognitoService.getUser(username);
      if (UserStatusType.FORCE_CHANGE_PASSWORD.equals(user.userStatus())) {
        adminCognitoService.loginWithNewPassword(username, USER_TEMP_PASSWORD, USER_PASSWORD);
      }
    }
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
          assertEquals("{\"message\":\"this is a test\"}", client.getMessages().get(0));
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
      assertEquals("{\"message\":\"this is a test\"}", client.getMessages().get(0));
      assertEquals(0, client.getErrors().size());
      assertEquals("1000", String.valueOf(client.getCloseCode()));
    }
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
}
