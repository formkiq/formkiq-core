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

import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static com.formkiq.testutils.aws.TypeSenseExtension.API_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestContext;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginEmpty;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.LambdaLoggerRecorder;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypeSenseExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.utils.IoUtils;


/** Abstract class for testing API Requests. */
public abstract class AbstractRequestHandler {

  /** Port to run Test server. */
  private static final int PORT = 8080;

  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;

  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate" + UUID.randomUUID();
  /** SQS Create Url. */
  private static String snsDocumentEvent;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentEventUrl;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;

  /**
   * Before All Tests.
   * 
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {
    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection(null);
    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);
    ssmService.putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    SqsService sqsService = new SqsService(TestServices.getSqsConnection(null));
    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    SnsService snsService = new SnsService(TestServices.getSnsConnection(null));
    snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();
    snsService.subscribe(snsDocumentEvent, "sqs", sqsDocumentEventUrl);
  }

  /** {@link CoreAwsServiceCache}. */
  private CoreAwsServiceCache awsServices;

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  /** {@link LambdaLogger}. */
  private LambdaLoggerRecorder logger = (LambdaLoggerRecorder) this.context.getLogger();
  /** System Environment Map. */
  private Map<String, String> map = new HashMap<>();
  /** {@link ClientAndServer}. */
  private ClientAndServer mockServer = null;

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
      Map<String, String> queryMap = new HashMap<>();
      if (event.getQueryStringParameters() != null) {
        queryMap.putAll(event.getQueryStringParameters());
      }

      queryMap.put(parameter, value);
      event.setQueryStringParameters(queryMap);
    }
  }

  /**
   * After Each Test.
   */
  @AfterEach
  public void after() {
    stopMockServer();
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
  @BeforeEach
  public void before() throws Exception {

    this.map.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    this.map.put("APP_ENVIRONMENT", FORMKIQ_APP_ENVIRONMENT);
    this.map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    this.map.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    this.map.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    this.map.put("CACHE_TABLE", CACHE_TABLE);
    this.map.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
    this.map.put("STAGE_DOCUMENTS_S3_BUCKET", STAGE_BUCKET_NAME);
    this.map.put("OCR_S3_BUCKET", OCR_BUCKET_NAME);
    this.map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    this.map.put("AWS_REGION", AWS_REGION.toString());
    this.map.put("DEBUG", "true");
    this.map.put("SQS_DOCUMENT_FORMATS",
        TestServices.getSqsDocumentFormatsQueueUrl(TestServices.getSqsConnection(null)));
    this.map.put("DISTRIBUTION_BUCKET", "formkiq-distribution-us-east-pro");
    this.map.put("FORMKIQ_TYPE", "core");
    this.map.put("USER_AUTHENTICATION", "cognito");
    this.map.put("WEBSOCKET_SQS_URL",
        TestServices.getSqsWebsocketQueueUrl(TestServices.getSqsConnection(null)));
    this.map.put("TYPESENSE_HOST", "http://localhost:" + TypeSenseExtension.getMappedPort());
    this.map.put("TYPESENSE_API_KEY", API_KEY);

    createApiRequestHandler(this.map);

    this.awsServices = CoreAwsServiceCache.cast(new CoreRequestHandler().getAwsServices());

    SqsService sqsservice = this.awsServices.getExtension(SqsService.class);

    for (String queue : Arrays.asList(TestServices.getSqsDocumentFormatsQueueUrl(null))) {
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
   * @throws URISyntaxException URISyntaxException
   */
  public void createApiRequestHandler(final Map<String, String> prop) throws URISyntaxException {
    AwsCredentials creds = AwsBasicCredentials.create("asd", "asd");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    Map<String, URI> endpoints = TestServices.getEndpointMap();

    AbstractCoreRequestHandler.configureHandler(prop, AWS_REGION, credentialsProvider, endpoints,
        new DocumentTagSchemaPluginEmpty());
  }

  /**
   * Create {@link ApiGatewayRequestEvent}.
   * 
   * @param file {@link String}
   * @param siteId {@link String}
   * @param username {@link String}
   * @param cognitoGroups {@link String}
   * @return {@link ApiGatewayRequestEvent}
   * @throws IOException IOException
   */
  protected ApiGatewayRequestEvent createRequest(final String file, final String siteId,
      final String username, final String cognitoGroups) throws IOException {
    ApiGatewayRequestEvent event = toRequestEvent(file);
    addParameter(event, "siteId", siteId);

    if (username != null) {
      setUsername(event, username);
    }

    if (cognitoGroups != null) {
      setCognitoGroup(event, cognitoGroups);
    }
    return event;
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
   * Get {@link CoreAwsServiceCache}.
   * 
   * @return {@link CoreAwsServiceCache}
   */
  public CoreAwsServiceCache getAwsServices() {
    return this.awsServices;
  }

  /**
   * Get {@link DocumentService}.
   *
   * @return {@link DocumentService}
   */
  public DocumentService getDocumentService() {
    return this.awsServices.getExtension(DocumentService.class);
  }

  /**
   * Get {@link CoreRequestHandler}.
   *
   * @return {@link CoreRequestHandler}
   */
  public CoreRequestHandler getHandler() {
    return new CoreRequestHandler();
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
    return Collections.unmodifiableMap(this.map);
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
   * Get {@link ClientAndServer}.
   * 
   * @return {@link ClientAndServer}
   */
  public ClientAndServer getMockServer() {
    return this.mockServer;
  }

  /**
   * Get {@link S3Service}.
   *
   * @return {@link S3Service}
   */
  public S3Service getS3() {
    return this.awsServices.getExtension(S3Service.class);
  }

  /**
   * Get Sqs Messages.
   * 
   * @return {@link List} {@link Message}
   * @throws InterruptedException InterruptedException
   */
  public List<Message> getSqsMessages() throws InterruptedException {

    SqsService sqsService = this.awsServices.getExtension(SqsService.class);

    List<Message> msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    while (msgs.isEmpty()) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    }

    for (Message msg : msgs) {
      sqsService.deleteMessage(sqsDocumentEventUrl, msg.receiptHandle());
    }

    return msgs;
  }

  /**
   * Get SSM Parameter.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  public String getSsmParameter(final String key) {
    return getSsmService().getParameterValue(key);
  }

  /**
   * Get {@link SsmService}.
   * 
   * @return {@link SsmService}
   */
  private SsmService getSsmService() {
    return this.awsServices.getExtension(SsmService.class);
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
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();

    new CoreRequestHandler().handleRequest(is, outstream, getMockContext());

    String response = new String(outstream.toByteArray(), "UTF-8");
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

    ApiGatewayRequestEvent event = createRequest(file, siteId, username, cognitoGroups);

    String response = handleRequest(event);

    return new DynamicObject(fromJson(response, Map.class));
  }

  /**
   * Add Environment Variable.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  public void putEnvironmentVariable(final String key, final String value) {
    this.map.put(key, value);
  }

  /**
   * Put SSM Parameter.
   * 
   * @param name {@link String}
   * @param value {@link String}
   */
  public void putSsmParameter(final String name, final String value) {
    getSsmService().putParameter(name, value);
  }

  /**
   * Remove SSM Parameter.
   * 
   * @param name {@link String}
   */
  public void removeSsmParameter(final String name) {
    try {
      getSsmService().removeParameter(name);
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

    ApiGatewayRequestContext requestContext = event.getRequestContext();
    Map<String, Object> authorizer = requestContext.getAuthorizer();
    if (authorizer == null) {
      authorizer = new HashMap<>();
    }

    Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
    if (claims == null) {
      claims = new HashMap<>();
      authorizer.put("claims", claims);
    }

    claims.put("cognito:groups", cognitoGroups);
    requestContext.setAuthorizer(authorizer);
    event.setRequestContext(requestContext);
  }

  /**
   * Set Environment Variable.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  public void setEnvironment(final String key, final String value) {
    this.map.put(key, value);
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

    Map<String, String> pathmap = new HashMap<>(event.getPathParameters());
    pathmap.put(parameter, value);
    event.setPathParameters(pathmap);
  }

  /**
   * Set Cognito Group.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param username {@link String}
   */
  @SuppressWarnings("unchecked")
  public void setUsername(final ApiGatewayRequestEvent event, final String username) {
    ApiGatewayRequestContext requestContext = event.getRequestContext();
    Map<String, Object> authorizer = requestContext.getAuthorizer();
    if (authorizer == null) {
      authorizer = new HashMap<>();
    }

    Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
    if (claims == null) {
      claims = new HashMap<>();
      authorizer.put("claims", claims);
    }

    claims.put("cognito:username", username);

    requestContext.setAuthorizer(authorizer);
    event.setRequestContext(requestContext);
  }

  /**
   * Start Mock Server.
   */
  public void startMockServer() {
    this.mockServer = startClientAndServer(Integer.valueOf(PORT));
  }

  /**
   * Stop Mock Server.
   */
  public void stopMockServer() {
    if (this.mockServer != null) {
      this.mockServer.stop();
    }
    this.mockServer = null;
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
