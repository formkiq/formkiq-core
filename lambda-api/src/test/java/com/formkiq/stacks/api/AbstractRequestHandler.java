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
package com.formkiq.stacks.api;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmServiceImpl;
import com.formkiq.lambda.apigateway.ApiGatewayRequestContext;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.DynamoDbHelper;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.utils.IoUtils;

/** Abstract class for testing API Requests. */
public abstract class AbstractRequestHandler {

  /** SQS Document Formats Queue. */
  private static final String SQS_DOCUMENT_FORMATS_QUEUE = "documentFormats";
  /** {@link String}. */
  private static String bucketName = "testbucket";
  /** {@link String}. */
  private static String stages3bucket = "stagebucket";
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbConnection;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Connection;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmConnection;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsConnection;
  /** Documents Table. */
  private static String documentsTable = "Documents";
  /** Cache Table. */
  private static String cacheTable = "Cache";
  /** App Environment. */
  private static String appenvironment;
  /** Aws Region. */
  private static Region awsRegion;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentFormatsQueueUrl;

  /**
   * Before Class.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException, InterruptedException {

    appenvironment = System.getProperty("testappenvironment");
    awsRegion = Region.of(System.getProperty("testregion"));

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    s3Connection = new S3ConnectionBuilder().setCredentials(cred).setRegion(awsRegion)
        .setEndpointOverride("http://localhost:4566");

    ssmConnection = new SsmConnectionBuilder().setCredentials(cred).setRegion(awsRegion)
        .setEndpointOverride("http://localhost:4566");

    sqsConnection = new SqsConnectionBuilder().setCredentials(cred).setRegion(awsRegion)
        .setEndpointOverride("http://localhost:4566");

    dbConnection = new DynamoDbConnectionBuilder().setRegion(awsRegion).setCredentials(cred)
        .setEndpointOverride("http://localhost:8000");

    S3Service s3service = new S3Service(s3Connection);
    try (S3Client s3 = s3service.buildClient()) {

      if (!s3service.exists(s3, bucketName)) {
        s3service.createBucket(s3, bucketName);
      }

      if (!s3service.exists(s3, stages3bucket)) {
        s3service.createBucket(s3, stages3bucket);
      }
    }

    DynamoDbHelper dbHelper = new DynamoDbHelper(dbConnection);
    if (!dbHelper.isDocumentsTableExists()) {
      dbHelper.createDocumentsTable();
    }

    if (!dbHelper.isCacheTableExists()) {
      dbHelper.createCacheTable();
    }
    
    SqsService sqsservice = new SqsService(sqsConnection);
    if (!sqsservice.exists(SQS_DOCUMENT_FORMATS_QUEUE)) {
      sqsDocumentFormatsQueueUrl = sqsservice.createQueue(SQS_DOCUMENT_FORMATS_QUEUE).queueUrl();
    }

    new SsmServiceImpl(ssmConnection).putParameter("/formkiq/" + appenvironment + "/version",
        "1.1");
  }

  /**
   * Get App Environment.
   * 
   * @return {@link String}
   */
  public static String getAppenvironment() {
    return appenvironment;
  }

  /**
   * Get Aws Region.
   * 
   * @return {@link Region}
   */
  public static Region getAwsRegion() {
    return awsRegion;
  }

  /**
   * Get Sqs Document Formats Queue Url.
   * 
   * @return {@link String}
   */
  public static String getSqsDocumentFormatsQueueUrl() {
    return sqsDocumentFormatsQueueUrl;
  }

  /** System Environment Map. */
  private Map<String, String> map = new HashMap<>();
  /** {@link CoreRequestHandler}. */
  private CoreRequestHandler handler;
  /** {@link ByteArrayOutputStream}. */
  private ByteArrayOutputStream outstream = new ByteArrayOutputStream();

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  /** {@link LambdaLogger}. */
  private LambdaLoggerRecorder logger = (LambdaLoggerRecorder) this.context.getLogger();
  /** {@link DynamoDbHelper}. */
  private DynamoDbHelper dbhelper;

  /** {@link AwsServiceCache}. */
  private AwsServiceCache awsServices;

  /**
   * Add header to {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @param value {@link String}
   */
  public void addHeader(final ApiGatewayRequestEvent event, final String key, final String value) {

    if (value != null) {

      Map<String, String> header = event.getHeaders();
      if (header == null) {
        header = new HashMap<>();
      }

      header.put(key, value);
      event.setHeaders(header);
    }
  }

  /**
   * Add parameter to {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param parameter {@link String}
   * @param value {@link String}
   */
  public void addParameter(final ApiGatewayRequestEvent event, final String parameter,
      final String value) {
    if (value != null) {
      Map<String, String> queryMap = event.getQueryStringParameters();
      if (queryMap == null) {
        queryMap = new HashMap<>();
      }

      queryMap.put(parameter, value);
      event.setQueryStringParameters(queryMap);
    }
  }

  /**
   * Assert Response Headers.
   * 
   * @param obj {@link DynamicObject}
   * 
   */
  public void assertHeaders(final DynamicObject obj) {

    assertEquals("*", obj.getString("Access-Control-Allow-Origin"));
    assertEquals("*", obj.getString("Access-Control-Allow-Methods"));
    assertEquals("Content-Type,X-Amz-Date,Authorization,X-Api-Key",
        obj.getString("Access-Control-Allow-Headers"));
    assertEquals("*", obj.getString("Access-Control-Allow-Origin"));
    assertEquals("application/json", obj.getString("Content-Type"));
  }

  /**
   * before.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {

    this.dbhelper = new DynamoDbHelper(dbConnection);
    this.dbhelper.truncateDocumentsTable();
    this.dbhelper.truncateWebhooks();
    this.dbhelper.truncateConfig();

    this.map.put("APP_ENVIRONMENT", appenvironment);
    this.map.put("DOCUMENTS_TABLE", documentsTable);
    this.map.put("CACHE_TABLE", cacheTable);
    this.map.put("DOCUMENTS_S3_BUCKET", bucketName);
    this.map.put("STAGE_DOCUMENTS_S3_BUCKET", stages3bucket);
    this.map.put("AWS_REGION", "us-east-1");
    this.map.put("DEBUG", "true");
    this.map.put("SQS_DOCUMENT_FORMATS", sqsDocumentFormatsQueueUrl);
    this.map.put("DISTRIBUTION_BUCKET", "formkiq-distribution-us-east-pro");
    this.map.put("FORMKIQ_TYPE", "core");

    createApiRequestHandler(this.map);

    this.awsServices = this.handler.getAwsServices();

    SqsService sqsservice = this.awsServices.sqsService();
    for (String queue : Arrays.asList(sqsDocumentFormatsQueueUrl)) {
      ReceiveMessageResponse response = sqsservice.receiveMessages(queue);
      while (response.messages().size() > 0) {
        for (Message msg : response.messages()) {
          sqsservice.deleteMessage(queue, msg.receiptHandle());
        }

        response = sqsservice.receiveMessages(queue);
      }
    }
  }

  /**
   * Create Api Request Handler.
   * 
   * @param prop {@link Map}
   */
  public void createApiRequestHandler(final Map<String, String> prop) {
    CoreRequestHandler.setUpHandler(prop, dbConnection, s3Connection, ssmConnection, sqsConnection);
    this.handler = new CoreRequestHandler();
  }

  /**
   * Convert JSON to Object.
   * 
   * @param <T> Class Type
   * @param json {@link String}
   * @param clazz {@link Class}
   * @return {@link Object}
   */
  protected <T> T fromJson(final String json, final Class<T> clazz) {
    return GsonUtil.getInstance().fromJson(json, clazz);
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {
    return this.awsServices;
  }

  /**
   * Documents Bucket Name.
   *
   * @return {@link String}
   */
  public String getBucketName() {
    return bucketName;
  }

  /**
   * Get {@link DynamoDbHelper}.
   * 
   * @return {@link DynamoDbHelper}
   */
  public DynamoDbHelper getDbhelper() {
    return this.dbhelper;
  }

  /**
   * Get {@link DocumentService}.
   *
   * @return {@link DocumentService}
   */
  public DocumentService getDocumentService() {
    return this.awsServices.documentService();
  }

  /**
   * Get {@link CoreRequestHandler}.
   *
   * @return {@link CoreRequestHandler}
   */
  public CoreRequestHandler getHandler() {
    return this.handler;
  }

  /**
   * Get Response Headers.
   *
   * @return {@link String}
   */
  public String getHeaders() {
    return "\"headers\":{" + "\"Access-Control-Allow-Origin\":\"*\","
        + "\"Access-Control-Allow-Methods\":\"*\"," + "\"Access-Control-Allow-Headers\":"
        + "\"Content-Type,X-Amz-Date,Authorization,X-Api-Key\","
        + "\"Content-Type\":\"application/json\"}";
  }

  /**
   * Get {@link LambdaLoggerRecorder}.
   *
   * @return {@link LambdaLoggerRecorder}
   */
  public LambdaLoggerRecorder getLogger() {
    return this.logger;
  }

  /**
   * Get Environment {@link Map}.
   *
   * @return {@link Map}
   */
  public Map<String, String> getMap() {
    return this.map;
  }

  /**
   * Get Mock {@link Context}.
   *
   * @return {@link Context}
   */
  public Context getMockContext() {
    return this.context;
  }

  /**
   * Get {@link ByteArrayOutputStream} for API Request Results.
   *
   * @return {@link ByteArrayOutputStream}
   */
  public ByteArrayOutputStream getOutstream() {
    return this.outstream;
  }

  /**
   * Get {@link S3Service}.
   *
   * @return {@link S3Service}
   */
  public S3Service getS3() {
    return this.awsServices.s3Service();
  }

  /**
   * Get SSM Parameter.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  public String getSsmParameter(final String key) {
    return this.awsServices.ssmService().getParameterValue(key);
  }

  /**
   * Get Staging Document Bucket Name.
   *
   * @return {@link String}
   */
  public String getStages3bucket() {
    return stages3bucket;
  }

  /**
   * Handle Request.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   * @throws IOException IOException
   */
  public String handleRequest(final ApiGatewayRequestEvent event) throws IOException {

    String s = GsonUtil.getInstance().toJson(event);
    InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));

    this.handler.handleRequest(is, this.outstream, getMockContext());

    String response = new String(this.outstream.toByteArray(), "UTF-8");
    return response;
  }

  /**
   * Handle Request.
   * 
   * @param file {@link String}
   * @param siteId {@link String}
   * @param username {@link String}
   * @param cognitoGroups {@link String}
   * @return {@link DynamicObject}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  public DynamicObject handleRequest(final String file, final String siteId, final String username,
      final String cognitoGroups) throws IOException {
    ApiGatewayRequestEvent event = toRequestEvent(file);
    addParameter(event, "siteId", siteId);

    if (username != null) {
      setUsername(event, username);
    }

    if (cognitoGroups != null) {
      setCognitoGroup(event, cognitoGroups);
    }

    String response = handleRequest(event);

    return new DynamicObject(fromJson(response, Map.class));
  }

  /**
   * Create new Outstream.
   */
  public void newOutstream() {
    this.outstream = new ByteArrayOutputStream();
  }

  /**
   * Put SSM Parameter.
   * 
   * @param name {@link String}
   * @param value {@link String}
   */
  public void putSsmParameter(final String name, final String value) {
    this.awsServices.ssmService().putParameter(name, value);
  }

  /**
   * Remove SSM Parameter.
   * 
   * @param name {@link String}
   */
  public void removeSsmParameter(final String name) {
    try {
      this.awsServices.ssmService().removeParameter(name);
    } catch (ParameterNotFoundException e) {
      // ignore property error
    }
  }

  /**
   * Set Cognito Group.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param cognitoGroups {@link String}
   */
  @SuppressWarnings("unchecked")
  public void setCognitoGroup(final ApiGatewayRequestEvent event, final String... cognitoGroups) {
    Map<String, Object> authorizer = event.getRequestContext().getAuthorizer();
    if (authorizer == null) {
      authorizer = new HashMap<>();
      event.getRequestContext().setAuthorizer(authorizer);
    }

    Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
    if (claims == null) {
      claims = new HashMap<>();
      authorizer.put("claims", claims);
    }

    claims.put("cognito:groups", cognitoGroups);
  }


  /**
   * Set Path Parameter.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param parameter {@link String}
   * @param value {@link String}
   */
  public void setPathParameter(final ApiGatewayRequestEvent event, final String parameter,
      final String value) {
    if (event.getPathParameters() == null) {
      event.setPathParameters(new HashMap<>());
    }

    Map<String, String> pathmap = event.getPathParameters();
    pathmap.put(parameter, value);
  }

  /**
   * Set Cognito Group.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param username {@link String}
   */
  @SuppressWarnings("unchecked")
  public void setUsername(final ApiGatewayRequestEvent event, final String username) {
    Map<String, Object> authorizer = event.getRequestContext().getAuthorizer();
    if (authorizer == null) {
      authorizer = new HashMap<>();
      event.getRequestContext().setAuthorizer(authorizer);
    }

    Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
    if (claims == null) {
      claims = new HashMap<>();
      authorizer.put("claims", claims);
    }

    claims.put("cognito:username", username);
  }

  /**
   * Converts {@link String} filename to {@link ApiGatewayRequestEvent}.
   *
   * @param filename {@link String}
   * @return {@link ApiGatewayRequestEvent}
   * @throws IOException IOException
   */
  public ApiGatewayRequestEvent toRequestEvent(final String filename) throws IOException {
    try (InputStream in = this.context.getClass().getResourceAsStream(filename)) {
      return GsonUtil.getInstance().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
          ApiGatewayRequestEvent.class);
    }
  }

  /**
   * Converts {@link String} filename to {@link ApiGatewayRequestEvent}.
   *
   * @param method {@link String}
   * @param resource {@link String}
   * @param object {@link Object}
   * @return {@link ApiGatewayRequestEvent}
   * @throws IOException IOException
   */
  public ApiGatewayRequestEvent toRequestEvent(final String method, final String resource,
      final Object object) throws IOException {
    ApiGatewayRequestEvent req = new ApiGatewayRequestEvent();
    req.setHttpMethod(method);
    req.setResource(resource);

    ApiGatewayRequestContext reqContext = new ApiGatewayRequestContext();
    reqContext.setAuthorizer(Map.of("claims", Map.of("cognito:groups", "[Admins]", "username",
        "f67128c7-b804-4147-8ae9-a22fdec108e0@formkiq.com")));
    req.setRequestContext(reqContext);

    if (object != null) {
      req.setBody(GsonUtil.getInstance().toJson(object));
    }

    return req;
  }

  /**
   * Converts {@link String} filename to {@link InputStream}.
   *
   * @param filename {@link String}
   * @return {@link InputStream}
   * @throws IOException IOException
   */
  public InputStream toStream(final String filename) throws IOException {
    return toStream(filename, null, null);
  }

  /**
   * Converts {@link String} filename to {@link InputStream}.
   *
   * @param filename {@link String}
   * @param regex {@link StringIndexOutOfBoundsException}
   * @param replacement {@link String}
   * @return {@link InputStream}
   * @throws IOException IOException
   */
  public InputStream toStream(final String filename, final String regex, final String replacement)
      throws IOException {

    InputStream in = this.context.getClass().getResourceAsStream(filename);
    String input = IoUtils.toUtf8String(in);

    if (regex != null && replacement != null) {
      input = input.replaceAll(regex, replacement);
    }

    InputStream instream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    in.close();

    return instream;
  }
}
