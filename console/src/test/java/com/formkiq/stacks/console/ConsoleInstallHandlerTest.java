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
package com.formkiq.stacks.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link ConsoleInstallHandler}. */
@ExtendWith(LocalStackExtension.class)
public class ConsoleInstallHandlerTest {

  /** {@link HttpURLConnection}. */
  private static HttpUrlConnectionRecorder connection;

  /** Console Bucket. */
  private static final String CONSOLE_BUCKET = "destbucket";
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Connection;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * Before Class.
   *
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws IOException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    Map<String, String> env = Map.of("AWS_REGION", "us-east-1");
    AwsServiceCache serviceCache =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), cred)
            .addService(new S3AwsServiceRegistry()).build();
    serviceCache.register(S3Service.class, new S3ServiceExtension());

    s3Connection = serviceCache.getExtension(S3ConnectionBuilder.class);
    s3 = serviceCache.getExtension(S3Service.class);

    s3.createBucket("distrobucket");
    s3.createBucket(CONSOLE_BUCKET);

    try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/test.zip")) {
      s3.putObject("distrobucket", "formkiq-console/0.1/formkiq-console.zip", is, null);
    }

    connection = new HttpUrlConnectionRecorder(new URL("http://localhost"));

  }

  /** {@link LambdaContextRecorder}. */
  private final LambdaContextRecorder context = new LambdaContextRecorder();
  /** {@link ConsoleInstallHandler}. */
  private ConsoleInstallHandler handler;

  /** {@link LambdaLogger}. */
  private final LambdaLoggerRecorder logger = this.context.getLoggerRecorder();

  /** before. */
  @BeforeEach
  public void before() {
    Map<String, String> map = createEnvironment();
    createHandler(map);
  }

  private Map<String, String> createEnvironment() {
    Map<String, String> map = new HashMap<>();
    map.put("CONSOLE_VERSION", "0.1");
    map.put("REGION", "us-east-1");
    map.put("DISTRIBUTION_BUCKET", "distrobucket");
    map.put("CONSOLE_BUCKET", CONSOLE_BUCKET);
    map.put("API_URL",
        "https://chartapi.24hourcharts.com.execute-api.us-east-1.amazonaws.com/prod/");
    map.put("API_IAM_URL", "https://auth.execute-api.us-east-1.amazonaws.com/iam/");
    map.put("API_KEY_URL", "https://auth.execute-api.us-east-1.amazonaws.com/key/");
    map.put("API_AUTH_URL", "https://auth.execute-api.us-east-1.amazonaws.com/prod/");
    map.put("API_WEBSOCKET_URL", "wss://me.execute-api.us-east-1.amazonaws.com/prod/");
    map.put("BRAND", "24hourcharts");
    map.put("ALLOW_ADMIN_CREATE_USER_ONLY", "false");
    map.put("COGNITO_HOSTED_UI", "https://test2111111111111111.auth.us-east-2.amazoncognito.com");
    map.put("USER_AUTHENTICATION", "cognito");
    map.put("COGNITO_CONFIG_BUCKET", CONSOLE_BUCKET);
    map.put("DOMAIN", "dev");
    map.put("COGNITO_USER_POOL_ID", "us-east-2_blGeBpyLg");
    map.put("COGNITO_USER_POOL_CLIENT_ID", "7223423m2pfgf34qnfokb2po2l");
    return map;
  }

  private void createHandler(final Map<String, String> map) {
    this.handler = new ConsoleInstallHandler(map, Region.US_EAST_2, s3Connection, s3Connection) {

      @Override
      protected HttpURLConnection getConnection(final String responseUrl) {
        return ConsoleInstallHandlerTest.this.getConnection();
      }
    };
  }

  /**
   * Create CloudFormation Input such as.
   *
   * @param requestType {@link String}
   *
   * @return {@link Map}
   */
  private Map<String, Object> createInput(final String requestType) {
    Map<String, Object> map = new HashMap<>();
    map.put("RequestType", requestType);
    map.put("ResponseURL", "https://cloudformation-custom-resource");
    return map;
  }

  /**
   * Get Connection.
   *
   * @return {@link HttpURLConnection}
   */
  protected HttpURLConnection getConnection() {
    return connection;
  }

  /**
   * Test Handle Request 'CREATE'.
   *
   */
  @Test
  public void testHandleRequest01() {
    // given
    final int contentlength = 105;
    Map<String, Object> input = createInput("Create");
    input.put("CONSOLE_BUCKET", CONSOLE_BUCKET);

    // when
    this.logger.log(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Create}");
    this.logger.log("unpacking formkiq-console/0.1/formkiq-console.zip "
        + "from bucket distrobucket to bucket destbucket");
    this.logger.log("sending SUCCESS to https://cloudformation-custom-resource");
    this.logger.log("Request Create was successful!");

    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    verifyConfigWritten("");
    verifyCognitoConfig();

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Create was successful!\""));

    assertEquals("font/woff2",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/font.woff2", null).getContentType());

    assertEquals("text/css",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.css", null).getContentType());

    assertEquals("application/vnd.ms-fontobject",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.eot", null).getContentType());

    assertEquals("image/x-icon",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ico", null).getContentType());

    assertTrue(s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.js", null).getContentType()
        .endsWith("/javascript"));

    assertEquals("image/svg+xml",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.svg", null).getContentType());

    assertEquals("font/ttf",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ttf", null).getContentType());

    assertEquals("text/plain",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.txt", null).getContentType());

    assertEquals("font/woff",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.woff", null).getContentType());

    // given
    input = createInput("Delete");
    input.put("CONSOLE_BUCKET", CONSOLE_BUCKET);

    // when
    this.handler.handleRequest(input, this.context);

    // then
    assertTrue(s3.listObjects(CONSOLE_BUCKET, null).contents().isEmpty());
  }

  /**
   * Test Handle Request 'UPDATE'.
   *
   */
  @Test
  public void testHandleRequest02() {
    // given
    final int contentlength = 105;
    final Map<String, Object> input = createInput("Update");

    // when
    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    verifyConfigWritten("");
    verifyCognitoConfig();

    assertTrue(this.logger.containsString(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Update}"));

    assertTrue(this.logger.containsString("unpacking formkiq-console/0.1/formkiq-console.zip "
        + "from bucket distrobucket to bucket destbucket"));
    assertTrue(
        this.logger.containsString("sending SUCCESS to https://cloudformation-custom-resource"));
    assertTrue(this.logger.containsString("Request Update was successful!"));

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Update was successful!\""));

    assertEquals("font/woff2",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/font.woff2", null).getContentType());

    assertEquals("text/css",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.css", null).getContentType());

    assertEquals("application/vnd.ms-fontobject",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.eot", null).getContentType());

    assertEquals("image/x-icon",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ico", null).getContentType());

    assertTrue(s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.js", null).getContentType()
        .endsWith("/javascript"));

    assertEquals("image/svg+xml",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.svg", null).getContentType());

    assertEquals("font/ttf",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ttf", null).getContentType());

    assertEquals("text/plain",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.txt", null).getContentType());

    assertEquals("font/woff",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.woff", null).getContentType());
  }

  private void verifyCognitoConfig() {
    assertTrue(s3.getObjectMetadata(CONSOLE_BUCKET,
        "formkiq/cognito/dev/CustomMessage_AdminCreateUser/Message", null).isObjectExists());
    assertTrue(s3
        .getObjectMetadata(CONSOLE_BUCKET, "formkiq/cognito/dev/CustomMessage_SignUp/Message", null)
        .isObjectExists());
    assertTrue(s3.getObjectMetadata(CONSOLE_BUCKET,
        "formkiq/cognito/dev/CustomMessage_ForgotPassword/Message", null).isObjectExists());
  }

  /**
   * Test Handle Request 'DELETE'.
   *
   */
  @Test
  public void testHandleRequest03() {
    // given
    final int contentlength = 105;
    final Map<String, Object> input = createInput("Delete");

    // when
    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);

    assertTrue(this.logger.containsString(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Delete}"));

    assertTrue(this.logger.containsString("Request Delete was successful!"));
    assertTrue(
        this.logger.containsString("sending SUCCESS to https://cloudformation-custom-resource"));

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Delete was successful!\""));
  }

  /**
   * Test Handle Request 'UNKNOWN'.
   *
   */
  @Test
  public void testHandleRequest04() {
    // given
    final int contentlength = 122;
    final Map<String, Object> input = createInput("UNKNOWN");

    // when
    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    assertTrue(this.logger
        .containsString("received input: {ResponseURL=https://cloudformation-custom-resource, "
            + "RequestType=UNKNOWN}"));

    assertTrue(this.logger.containsString("received RequestType UNKNOWN skipping unpacking"));
    assertTrue(
        this.logger.containsString("sending FAILURE to https://cloudformation-custom-resource"));

    assertTrue(connection.contains("\"Status\":\"FAILURE\""));
  }

  /**
   * Test Handle Request 'CREATE'.
   *
   */
  @Test
  public void testHandleRequest05() {
    // given
    String cognitoSingleSignOnUrl =
        "https://something.auth.us-east-2.amazoncognito.com/oauth2/authorize";
    Map<String, String> map = createEnvironment();
    map.put("COGNITO_SINGLE_SIGN_ON_URL", cognitoSingleSignOnUrl);
    createHandler(map);

    final int contentlength = 105;
    Map<String, Object> input = createInput("Create");
    input.put("CONSOLE_BUCKET", CONSOLE_BUCKET);

    // when
    this.logger.log(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Create}");
    this.logger.log("unpacking formkiq-console/0.1/formkiq-console.zip "
        + "from bucket distrobucket to bucket destbucket");
    this.logger.log("sending SUCCESS to https://cloudformation-custom-resource");
    this.logger.log("Request Create was successful!");

    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    verifyConfigWritten(cognitoSingleSignOnUrl);
    verifyCognitoConfig();

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Create was successful!\""));

    assertEquals("font/woff2",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/font.woff2", null).getContentType());

    assertEquals("text/css",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.css", null).getContentType());

    assertEquals("application/vnd.ms-fontobject",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.eot", null).getContentType());

    assertEquals("image/x-icon",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ico", null).getContentType());

    assertTrue(s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.js", null).getContentType()
        .endsWith("/javascript"));

    assertEquals("image/svg+xml",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.svg", null).getContentType());

    assertEquals("font/ttf",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.ttf", null).getContentType());

    assertEquals("text/plain",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.txt", null).getContentType());

    assertEquals("font/woff",
        s3.getObjectMetadata(CONSOLE_BUCKET, "0.1/test.woff", null).getContentType());

    // given
    input = createInput("Delete");
    input.put("CONSOLE_BUCKET", CONSOLE_BUCKET);

    // when
    this.handler.handleRequest(input, this.context);

    // then
    assertTrue(s3.listObjects(CONSOLE_BUCKET, null).contents().isEmpty());
  }

  /**
   * Verify Config File is written.
   *
   * @param cognitoSingleSignOnUrl {@link String}
   */
  private void verifyConfigWritten(final String cognitoSingleSignOnUrl) {

    String json = s3.getContentAsString("destbucket", "0.1/assets/config.json", null);
    Map<String, String> o = this.gson.fromJson(json, Map.class);
    assertEquals("https://chartapi.24hourcharts.com.execute-api.us-east-1.amazonaws.com/prod/",
        o.get("documentApi"));
    assertEquals("us-east-2_blGeBpyLg", o.get("userPoolId"));
    assertEquals("7223423m2pfgf34qnfokb2po2l", o.get("clientId"));
    assertEquals("0.1", o.get("consoleVersion"));
    assertEquals("24hourcharts", o.get("brand"));
    assertEquals("us-east-2", o.get("awsRegion"));
    assertEquals("https://test2111111111111111.auth.us-east-2.amazoncognito.com",
        o.get("cognitoHostedUi"));
    assertEquals(cognitoSingleSignOnUrl, o.get("cognitoSingleSignOnUrl"));
    assertEquals("https://auth.execute-api.us-east-1.amazonaws.com/iam/", o.get("apiIamUrl"));
    assertEquals("https://auth.execute-api.us-east-1.amazonaws.com/key/", o.get("apiKeyUrl"));
  }

  /**
   * expect Send Response.
   *
   * @param contentLength int
   */
  private void verifySendResponse(final int contentLength) {

    assertTrue(this.logger.containsString("Response Code: 200"));
    assertTrue(connection.getDoOutput());
    assertEquals("", connection.getRequestProperty("Content-Type"));
    assertEquals("" + contentLength, connection.getRequestProperty("Content-Length"));
    assertEquals("PUT", connection.getRequestMethod());
  }
}
