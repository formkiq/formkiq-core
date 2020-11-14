/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Unit Tests for {@link ConsoleInstallHandler}. */
public class ConsoleInstallHandlerTest {

  /** {@link ConsoleInstallHandler}. */
  private ConsoleInstallHandler handler;

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context = new LambdaContextRecorder();
  /** {@link LambdaLogger}. */
  private LambdaLoggerRecorder logger = this.context.getLoggerRecorder();
  /** {@link HttpURLConnection}. */
  private static HttpUrlConnectionRecorder connection;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Connection;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmConnection;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link SsmService}. */
  private static SsmService ssmService;

  /**
   * Before Class.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException, InterruptedException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    ssmConnection = new SsmConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:4566");

    s3Connection = new S3ConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:4566");

    s3 = new S3Service(s3Connection);
    ssmService = new SsmServiceImpl(ssmConnection);

    try (S3Client s = s3.buildClient()) {

      s3.createBucket(s, "distrobucket");
      s3.createBucket(s, "destbucket");

      try (InputStream is = LambdaContextRecorder.class.getResourceAsStream("/test.zip")) {
        s3.putObject(s, "distrobucket", "formkiq-console/0.1/formkiq-console.zip", is, null);
      }
    }

    connection = new HttpUrlConnectionRecorder(new URL("http://localhost"));

  }

  /** before. */
  @Before
  public void before() {

    Map<String, String> map = new HashMap<>();
    map.put("appenvironment", "prod");
    map.put("appversion", "0.0.4");
    map.put("consoleversion", "0.1");
    map.put("version", "0.0.1");
    map.put("REGION", "us-east-1");
    map.put("distributionbucket", "distrobucket");

    this.handler = new ConsoleInstallHandler(map, s3Connection, ssmConnection) {

      @Override
      protected HttpURLConnection getConnection(final String responseUrl) throws IOException {
        return ConsoleInstallHandlerTest.this.getConnection();
      }
    };
  }

  /**
   * Create CloudFormation Input such as.
   *
   * @param requestType {@link String}
   * 
   *        <pre>
   * { "RequestType": "Create",
   * "ServiceToken": "arn:aws:lambda:...:function:route53Dependency",
   * "ResponseURL": "https://cloudformation-custom-resource", "StackId":
   * "arn:aws:cloudformation:eu-west-1:...", "RequestId":
   * "afd8d7c5-9376-4013-8b3b-307517b8719e", "LogicalResourceId": "Route53",
   * "ResourceType": "Custom::Route53Dependency", "ResourceProperties": {
   * "ServiceToken": "arn:aws:lambda:...:function:route53Dependency",
   * "DomainName": "example.com" } }
   *        </pre>
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
   * expect Send Response.
   *
   * @param contentLength int
   * @throws IOException IOException
   */
  private void verifySendResponse(final int contentLength) throws IOException {

    assertTrue(this.logger.containsString("Response Code: 200"));
    assertTrue(connection.getDoOutput());
    assertEquals("", connection.getRequestProperty("Content-Type"));
    assertEquals("" + contentLength, connection.getRequestProperty("Content-Length"));
    assertEquals("PUT", connection.getRequestMethod());
  }

  /**
   * Verify Config File is written.
   */
  private void verifyConfigWritten() {
    String config = String
        .format("{%n\"version\":\"0.1\",%n\"cognito\": {%n\"userPoolId\":\"us-east-1_TVZnIdg4o\",%n"
            + "\"region\": \"us-east-1\",%n\"clientId\":\"31bp0cnujj1fluhdjmcc77gpaf\",%n"
            + "\"identityPoolId\":\"kasdasdassa\",%n" + "\"allowUserSelfRegistration\":false"
            + "},\"apigateway\": {%n"
            + "\"url\": \"https://me.execute-api.us-east-1.amazonaws.com/prod/\"%n}%n}");

    assertTrue(this.logger.containsString("writing Cognito config: " + config));
  }

  /** Expect write console config. */
  private void expectWriteConfig() {

    ssmService.putParameter("/formkiq/prod/cognito/UserPoolId", "us-east-1_TVZnIdg4o");
    ssmService.putParameter("/formkiq/prod/cognito/UserPoolClientId", "31bp0cnujj1fluhdjmcc77gpaf");
    ssmService.putParameter("/formkiq/prod/cognito/IdentityPoolId", "kasdasdassa");
    ssmService.putParameter("/formkiq/prod/api/DocumentsHttpUrl",
        "https://me.execute-api.us-east-1.amazonaws.com/prod/");
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
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest01() throws Exception {
    // given
    final int contentlength = 105;
    final Map<String, Object> input = createInput("Create");

    // when
    this.logger.log(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Create}");
    this.logger.log("unpacking formkiq-console/0.1/formkiq-console.zip "
        + "from bucket distrobucket to bucket destbucket");
    this.logger.log("sending SUCCESS to https://cloudformation-custom-resource");
    this.logger.log("Request Create was successful!");

    ssmService.putParameter("/formkiq/prod/s3/Console", "destbucket");

    expectWriteConfig();

    // replayAll();
    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    verifyConfigWritten();

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Create was successful!\""));

    try (S3Client s = s3.buildClient()) {
      assertEquals("font/woff2",
          s3.getObjectMetadata(s, "destbucket", "0.1/font.woff2").getContentType());

      assertEquals("text/css",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.css").getContentType());

      assertEquals("application/vnd.ms-fontobject",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.eot").getContentType());

      assertEquals("image/x-icon",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.ico").getContentType());

      assertEquals("application/javascript",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.js").getContentType());

      assertEquals("image/svg+xml",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.svg").getContentType());

      assertEquals("font/ttf",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.ttf").getContentType());

      assertEquals("text/plain",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.txt").getContentType());

      assertEquals("font/woff",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.woff").getContentType());
    }
  }

  /**
   * Test Handle Request 'UPDATE'.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest02() throws Exception {
    // given
    final int contentlength = 105;
    final Map<String, Object> input = createInput("Update");

    // when
    ssmService.putParameter("/formkiq/prod/s3/Console", "destbucket");

    expectWriteConfig();

    this.handler.handleRequest(input, this.context);

    // then
    verifySendResponse(contentlength);
    verifyConfigWritten();

    assertTrue(this.logger.containsString(
        "received input: {ResponseURL=https://cloudformation-custom-resource, RequestType=Update}"));

    assertTrue(this.logger.containsString("unpacking formkiq-console/0.1/formkiq-console.zip "
        + "from bucket distrobucket to bucket destbucket"));
    assertTrue(
        this.logger.containsString("sending SUCCESS to https://cloudformation-custom-resource"));
    assertTrue(this.logger.containsString("Request Update was successful!"));

    assertTrue(connection.contains("\"Status\":\"SUCCESS\""));
    assertTrue(connection.contains("\"Data\":{\"Message\":\"Request Update was successful!\""));

    try (S3Client s = s3.buildClient()) {
      assertEquals("font/woff2",
          s3.getObjectMetadata(s, "destbucket", "0.1/font.woff2").getContentType());

      assertEquals("text/css",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.css").getContentType());

      assertEquals("application/vnd.ms-fontobject",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.eot").getContentType());

      assertEquals("image/x-icon",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.ico").getContentType());

      assertEquals("application/javascript",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.js").getContentType());

      assertEquals("image/svg+xml",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.svg").getContentType());

      assertEquals("font/ttf",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.ttf").getContentType());

      assertEquals("text/plain",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.txt").getContentType());

      assertEquals("font/woff",
          s3.getObjectMetadata(s, "destbucket", "0.1/test.woff").getContentType());
    }
  }

  /**
   * Test Handle Request 'DELETE'.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest03() throws Exception {
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
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest04() throws Exception {
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
}
