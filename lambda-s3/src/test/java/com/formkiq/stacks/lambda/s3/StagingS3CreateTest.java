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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.model.DocumentSyncServiceType.FORMKIQ_CLI;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.StagingS3Create.FORMKIQ_B64_EXT;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsByteArray;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3PresignerServiceExtension;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.module.lambdaservices.logger.LoggerRecorder;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.schema.DocumentSchema;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sns.SnsServiceExtension;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.apimodels.AddDocumentTag;
import com.formkiq.stacks.dynamodb.apimodels.MatchDocumentTag;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequest;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestMatch;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestUpdate;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** Unit Tests for {@link StagingS3Create}. */
@ExtendWith(DynamoDbExtension.class)
public class StagingS3CreateTest implements DbKeys {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** Documents S3 Bucket. */
  private static final String DOCUMENTS_BUCKET = "documentsbucket";
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor folderIndexProcesor;

  /** {@link Gson}. */
  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().setDateFormat(DateUtil.DATE_FORMAT).create();

  /** Register LocalStack extension. */
  @RegisterExtension
  static LocalStackExtension localStack = new LocalStackExtension();
  /** {@link ClientAndServer}. */
  private static ClientAndServer mockServer;
  /** Port to run Test server. */
  private static final int PORT = 8888;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link S3PresignerService}. */
  private static S3PresignerService presignerService;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate";
  /** SQS Sns Queue. */
  private static final String SNS_SQS_DELETE_QUEUE = "sqssnsDelete";
  /** SQS Create Url. */
  private static String snsCreateTopic;
  /** SNS Update Topic Arn. */
  private static String snsDeleteTopic;
  /** SQS Create Url. */
  private static String snsDocumentEvent;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** SQS Sns Create QueueUrl. */
  private static String snsSqsCreateQueueUrl;
  /** SQS Sns Delete QueueUrl. */
  private static String snsSqsDeleteQueueUrl;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentEventUrl;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** Document S3 Staging Bucket. */
  private static final String STAGING_BUCKET = "example-bucket";
  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService;
  /** Test TImeout. */
  private static final long TEST_TIMEOUT = 30;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;
  /** UUID 1. */
  private static final String UUID1 = "b53c92cf-f7b9-4787-9541-76574ec70d71";
  /** {@link StagingS3Create}. */
  private static StagingS3Create handler;

  /** {@link LoggerRecorder}. */
  private static LoggerRecorder logger;

  /**
   * After Class.
   *
   */
  @AfterAll
  public static void afterClass() {
    mockServer.stop();
  }

  /**
   * Before Class.
   *
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws URISyntaxException, IOException {

    createAwService();
    awsServices.register(SnsService.class, new SnsServiceExtension());
    awsServices.register(SsmService.class, new SsmServiceExtension());

    snsService = awsServices.getExtension(SnsService.class);
    snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();
    awsServices.environment().put("SNS_DOCUMENT_EVENT", snsDocumentEvent);

    SsmService ssmService = awsServices.getExtension(SsmService.class);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    createAwService();

    handler = new StagingS3Create(awsServices);

    awsServices.register(SqsService.class, new SqsServiceExtension());
    awsServices.register(S3PresignerService.class, new S3PresignerServiceExtension());

    s3 = awsServices.getExtension(S3Service.class);
    presignerService = awsServices.getExtension(S3PresignerService.class);
    syncService = awsServices.getExtension(DocumentSyncService.class);
    sqsService = awsServices.getExtension(SqsService.class);
    actionsService = awsServices.getExtension(ActionsService.class);
    attributeService = awsServices.getExtension(AttributeService.class);

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentEventUrl);
    snsService.subscribe(snsDocumentEvent, "sqs", sqsQueueArn);

    service = awsServices.getExtension(DocumentService.class);

    folderIndexProcesor = awsServices.getExtension(FolderIndexProcessor.class);

    dbHelper = new DynamoDbHelper(awsServices.getExtension(DynamoDbConnectionBuilder.class));
    createResources();

    createMockServer();
  }

  private static void createAwService() {
    Map<String, String> env = getEnvironment();

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    awsServices = new AwsServiceCacheBuilder(Collections.unmodifiableMap(env),
        TestServices.getEndpointMap(), credentialsProvider)
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(), new SsmAwsServiceRegistry())
        .build().setLogger(new LoggerRecorder());
    logger = (LoggerRecorder) awsServices.getLogger();
  }

  /**
   * Create Mock Server.
   */
  private static void createMockServer() {

    mockServer = startClientAndServer(PORT);

    final String documentId = "12345";
    mockServer.when(request().withMethod("POST").withPath("/documents/" + documentId + "/tags"))
        .respond(httpRequest -> {

          Map<String, Object> map = GSON.fromJson(httpRequest.getBodyAsString(), Map.class);
          List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("tags");
          assertEquals(2, list.size());
          assertEquals("category", list.get(0).get("key"));
          assertEquals("document", list.get(0).get("value"));
          assertEquals("status", list.get(1).get("key"));
          assertEquals("[active, notactive]", list.get(1).get("values").toString());

          String siteId = null;
          Optional<Parameter> p = httpRequest.getQueryStringParameterList().stream()
              .filter(s -> "siteId".equals(s.getName().getValue())).findFirst();
          if (p.isPresent()) {
            siteId = p.get().getValues().get(0).getValue();
          }

          Collection<DocumentTag> tags =
              List.of(new DocumentTag(documentId, "test", "novalue", new Date(), "joe"));
          service.addTags(siteId, documentId, tags, null);

          return HttpResponse.response("{}");
        });
  }

  /**
   * Creates AWS Resources.
   *
   * @throws URISyntaxException URISyntaxException
   */
  private static void createResources() throws URISyntaxException {

    s3.createBucket(DOCUMENTS_BUCKET);
    s3.createBucket(STAGING_BUCKET);


    if (!sqsService.exists(SNS_SQS_DELETE_QUEUE)) {
      snsSqsDeleteQueueUrl = sqsService.createQueue(SNS_SQS_DELETE_QUEUE).queueUrl();
    }

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      snsSqsCreateQueueUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    snsService = new SnsService(TestServices.getSnsConnection(null));

    snsDeleteTopic = snsService.createTopic("deleteDocument").topicArn();
    snsService.subscribe(snsDeleteTopic, "sqs",
        "arn:aws:sqs:us-east-1:000000000000:" + SNS_SQS_DELETE_QUEUE);
    awsServices.environment().put("SNS_DELETE_TOPIC", snsDeleteTopic);

    snsCreateTopic = snsService.createTopic("createDocument").topicArn();
    snsService.subscribe(snsCreateTopic, "sqs",
        "arn:aws:sqs:us-east-1:000000000000:" + SNS_SQS_CREATE_QUEUE);
    awsServices.environment().put("SNS_CREATE_TOPIC", snsCreateTopic);

    DynamoDbConnectionBuilder dbBuilder = awsServices.getExtension(DynamoDbConnectionBuilder.class);
    try (DynamoDbClient dbClient = dbBuilder.build()) {
      DocumentSchema schema = new DocumentSchema(dbClient);
      schema.createDocumentsTable(DOCUMENTS_TABLE);
      schema.createCacheTable(CACHE_TABLE);
    }
  }

  private static Map<String, String> getEnvironment() {
    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", Region.US_EAST_1.id());
    env.put("DOCUMENTS_S3_BUCKET", DOCUMENTS_BUCKET);
    env.put("LOG_LEVEL", "TRACE");
    env.put("SNS_DELETE_TOPIC", snsDeleteTopic);
    env.put("SNS_CREATE_TOPIC", snsCreateTopic);
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    return env;
  }

  /**
   * Assert {@link DocumentTag} Equals.
   *
   * @param tag {@link DocumentTag}
   * @param attributes {@link Map}
   */
  private void assertEqualsTag(final DocumentTag tag, final Map<String, Object> attributes) {
    assertEquals(attributes.get("documentId"), tag.getDocumentId());
    assertEquals(attributes.get("key"), tag.getKey());
    assertNotNull(tag.getInsertedDate());
    assertEquals(attributes.get("type"), tag.getType().name());
    assertEquals(attributes.get("userId"), tag.getUserId());

    if (attributes.containsKey("values")) {
      assertEquals(attributes.get("values"), tag.getValues());
    } else {
      assertEquals(attributes.get("value"), tag.getValue());
    }
  }

  private void assertPublishedSnsMessage() throws InterruptedException {
    List<Message> msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    while (msgs.size() != 1) {
      TimeUnit.SECONDS.sleep(1);
      msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    }
    assertEquals(1, msgs.size());
  }

  /**
   * before.
   */
  @BeforeEach
  void before() {

    // this.context = new LambdaContextRecorder();

    dbHelper.truncateTable(DOCUMENTS_TABLE);

    s3.deleteAllFiles(STAGING_BUCKET);
    s3.deleteAllFiles(DOCUMENTS_BUCKET);

    for (String queue : Arrays.asList(snsSqsCreateQueueUrl, snsSqsDeleteQueueUrl)) {
      ReceiveMessageResponse response = sqsService.receiveMessages(queue);
      while (!response.messages().isEmpty()) {
        for (Message msg : response.messages()) {
          sqsService.deleteMessage(queue, msg.receiptHandle());
        }

        response = sqsService.receiveMessages(queue);
      }
    }
  }

  private void createDocument(final byte[] content, final String docId) throws ValidationException {
    DynamicDocumentItem item = new DynamicDocumentItem(new HashMap<>());
    item.setDocumentId(docId == null ? ID.uuid() : docId);
    item.setUserId("JohnDoe");
    item.setInsertedDate(new Date());
    final String documentId = item.getDocumentId();
    service.saveDocument(DEFAULT_SITE_ID, item, null);

    final String key = createS3Key(DEFAULT_SITE_ID, documentId);
    s3.putObject(DOCUMENTS_BUCKET, key, content, null, null);
  }

  /**
   * Create {@link DynamicDocumentItem}.
   *
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createDocumentItem() {
    String content = "This is a test";

    DynamicDocumentItem item = new DynamicDocumentItem(Collections.emptyMap());
    item.setDocumentId(ID.uuid());
    item.put("content", Base64.getEncoder().encodeToString(content.getBytes(UTF_8)));
    item.setContentLength((long) content.length());
    item.setContentType("plain/text");
    item.setInsertedDate(new Date());
    item.setPath("test.txt");
    item.setUserId("joe");

    return item;
  }

  private Map<String, Object> createRequestMap(final String s3Key) {

    Map<String, Object> record = Map.of("eventName", "ObjectCreated", "s3",
        Map.of("bucket", Map.of("name", STAGING_BUCKET), "object", Map.of("key", s3Key)));
    String body = GSON.toJson(Map.of("Records", List.of(record)));
    List<Map<String, Object>> records = List.of(Map.of("body", body));

    return new HashMap<>(Map.of("Records", records));
  }

  /**
   * Find DocumentId from Logger.
   *
   * @param siteId {@link String}
   * @return {@link String}
   */
  private String findDocumentIdFromLogger(final String siteId) {

    Optional<String> uuid = Optional.empty();

    List<String> msgs = logger.getMessages();

    Optional<String> copy =
        msgs.stream().filter(s -> s.startsWith("Copying") || s.startsWith("Inserted")).findFirst();

    if (copy.isPresent()) {

      uuid =
          Arrays.stream(copy.get().split(" ")).map(s -> resetDatabaseKey(siteId, s)).filter(s -> {
            try {
              int pos = s.indexOf("/");
              UUID.fromString(pos > 0 ? s.substring(pos + 1) : s);
              return true;
            } catch (IllegalArgumentException e) {
              return false;
            }
          }).findFirst();
    }

    return uuid.orElse(null);
  }

  /**
   * Find {@link DocumentTag}.
   *
   * @param tags {@link List} {@link DocumentTag}
   * @param key {@link String}
   * @return {@link DocumentTag}
   */
  private Optional<DocumentTag> findTag(final List<DocumentTag> tags, final String key) {
    return tags.stream().filter(s -> s.getKey().equals(key)).findFirst();
  }

  private ReceiveMessageResponse getReceiveMessageResponse(final String sqsQueueUrl)
      throws InterruptedException {
    ReceiveMessageResponse response = sqsService.receiveMessages(sqsQueueUrl);
    while (response.messages().isEmpty()) {
      TimeUnit.SECONDS.sleep(1);
      response = sqsService.receiveMessages(sqsQueueUrl);
    }

    response.messages().forEach(m -> sqsService.deleteMessage(sqsQueueUrl, m.receiptHandle()));
    return response;
  }

  /**
   * Handle {@link StagingS3Create} request.
   *
   * @param map {@link Map}
   */
  private void handleRequest(final Map<String, Object> map) {
    Void result = handler.handleRequest(map, null);
    assertNull(result);
  }

  /**
   * Test .fkb64 file.
   *
   * @param siteId {@link String}
   * @param docitem {@link DynamicDocumentItem}
   * @throws IOException IOException
   */
  private void processFkB64File(final String siteId, final DynamicDocumentItem docitem)
      throws IOException {

    String documentId = docitem.getDocumentId();
    logger.getMessages().clear();

    String key = createDatabaseKey(siteId, documentId + FORMKIQ_B64_EXT);

    byte[] content = GSON.toJson(docitem).getBytes(UTF_8);
    s3.putObject(STAGING_BUCKET, key, content, null, null);

    handleRequest(loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key));
  }

  /**
   * Test .fkb64 file.
   *
   * @param siteId {@link String}
   * @param docitem {@link DynamicDocumentItem}
   * @param expectedContentLength {@link String}
   * @throws IOException IOException
   */
  private void processFkB64File(final String siteId, final DynamicDocumentItem docitem,
      final String expectedContentLength) throws IOException {

    // given
    String documentId = ID.uuid();
    logger.getMessages().clear();

    String key = createDatabaseKey(siteId, documentId + FORMKIQ_B64_EXT);

    byte[] content = GSON.toJson(docitem).getBytes(UTF_8);
    s3.putObject(STAGING_BUCKET, key, content, null, null);

    // when
    handleRequest(loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key));

    // then
    String destDocumentId = findDocumentIdFromLogger(siteId);

    if (destDocumentId != null) {
      assertTrue(logger.containsString("Removing " + key + " from bucket example-bucket."));
      assertTrue(logger.containsString("Inserted " + docitem.getPath()
          + " into bucket documentsbucket as " + createDatabaseKey(siteId, destDocumentId)));

      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      DocumentItem item = service.findDocument(siteId, destDocumentId);

      if (expectedContentLength != null) {
        assertEquals(expectedContentLength, "" + item.getContentLength());
      } else {
        assertNull(item.getContentLength());
      }
      assertEquals("plain/text", item.getContentType());
      assertNotNull(item.getInsertedDate());
      assertNotNull(item.getChecksum());
      assertEquals("test.txt", item.getPath());
      assertEquals("joe", item.getUserId());

      boolean hasTags = docitem.containsKey("tags");

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, destDocumentId, null, MAX_RESULTS).getResults();
      int tagcount = hasTags ? docitem.getList("tags").size() : 0;
      assertEquals(tagcount, tags.size());

      if (hasTags) {

        for (DynamicObject tag : docitem.getList("tags")) {
          Optional<DocumentTag> dtag = findTag(tags, tag.getString("key"));
          assertTrue(dtag.isPresent());
          assertEquals(tag.getString("value"), dtag.get().getValue());
        }
      }
    }
  }

  /**
   * Add Webhook.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testAddWebhook01() throws Exception {
    // given
    Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event5.json");

    String key = "92779fb5-ee0e-4c72-85e9-84ad11a8b73c.fkb64";
    String content = """
        {
            "path": "webhooks/20292ffc-9d4f-46e8-b65d-aaf32501e384",
            "TimeToLive": "",
            "documentId": "92779fb5-ee0e-4c72-85e9-84ad11a8b73c",
            "contentType": "text/plain",
            "userId": "webhook/paypal",
            "content": "{\\"name\\":\\"John Smith\\"}"
        }""";

    s3.putObject(STAGING_BUCKET, key, content.getBytes(UTF_8), null, Map.of());

    // when
    handleRequest(map);

    // then
    List<S3Object> staging = s3.listObjects(STAGING_BUCKET, null).contents();
    assertEquals(0, staging.size());

    List<S3Object> docs = s3.listObjects(DOCUMENTS_BUCKET, null).contents();
    assertEquals(1, docs.size());

    assertNotNull(service.findDocument(null, "92779fb5-ee0e-4c72-85e9-84ad11a8b73c"));
  }

  /**
   * Test CopyFile.
   *
   * @param siteId {@link String}
   * @param path {@link String}
   * @param expectedPath {@link String}
   * @throws IOException IOException
   */
  private void testCopyFile(final String siteId, final String path, final String expectedPath)
      throws IOException {

    // given
    logger.getMessages().clear();

    String key = siteId != null ? siteId + "/" + path : path;

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", "application/pdf");
    metadata.put("userId", "1234");

    Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event1.json", UUID1, key);

    s3.putObject(STAGING_BUCKET, key, "testdata".getBytes(UTF_8), "application/pdf", metadata);

    // when
    handleRequest(map);

    // then
    String destDocumentId = findDocumentIdFromLogger(siteId);

    assertTrue(logger.containsString("Removing " + key + " from bucket example-bucket."));
    assertTrue(logger.containsString("handling 1 record(s)."));
    assertTrue(logger.containsString("Copying " + key + " from bucket example-bucket to "
        + createDatabaseKey(siteId, destDocumentId) + " in bucket " + DOCUMENTS_BUCKET + "."));
    assertTrue(logger.containsString("Removing " + key + " from bucket example-bucket."));

    assertFalse(s3.getObjectMetadata(STAGING_BUCKET, path, null).isObjectExists());

    DocumentItem item = service.findDocument(siteId, destDocumentId);
    assertNotNull(item);
    assertNull(item.getContentLength());
    assertNull(item.getContentType());
    assertNotNull(item.getChecksum());
    assertNotNull(item.getInsertedDate());
    assertEquals(item.getInsertedDate(), item.getLastModifiedDate());

    assertEquals(item.getPath(), expectedPath != null ? expectedPath : item.getDocumentId());
    assertEquals("1234", item.getUserId());

    List<DocumentTag> tags =
        service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();

    assertEquals(0, tags.size());

    verifyS3Metadata(siteId, item);
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testCopyFile01() throws Exception {
    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {
      String documentId = ID.uuid();
      testCopyFile(siteId, documentId, documentId);
    }
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testCopyFile02() throws Exception {
    int i = 0;
    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {
      final String documentId = "test" + i + ".pdf";
      testCopyFile(siteId, documentId, "test" + i + ".pdf");
      i++;
    }
  }

  /**
   * S3 Object Create Event Unit Test where filename long path.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testCopyFile03() throws Exception {
    final String documentId = "something/where/test.pdf";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      testCopyFile(siteId, documentId, "something/where/test.pdf");
    }
  }

  /**
   * Create folder.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testCreateFolder01() throws Exception {
    // given
    String key = "testsite/";
    Map<String, Object> map = loadFileAsMap(this, "/sqs-event-create01.json");

    s3.putObject(STAGING_BUCKET, key, "".getBytes(UTF_8), null, Map.of());

    // when
    handleRequest(map);

    // then
    List<S3Object> staging = s3.listObjects(STAGING_BUCKET, null).contents();
    assertEquals(1, staging.size());
    assertEquals(key, staging.get(0).key());

    List<S3Object> docs = s3.listObjects(DOCUMENTS_BUCKET, null).contents();
    assertEquals(0, docs.size());

    assertEquals(0, dbHelper.getDocumentItemCount(DOCUMENTS_TABLE));
  }

  /**
   * Tests document event callback.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testDocumentEventCallback() throws Exception {
    // given
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {

      String queueUrl = sqsService.createQueue("sqssnsCreate1" + ID.uuid()).queueUrl();
      String sqsQueueArn = sqsService.getQueueArn(queueUrl);
      snsService.subscribe(snsDocumentEvent, "sqs", sqsQueueArn);

      String documentId = ID.uuid();
      String s3Key = "tempfiles/eventcallback/" + siteId + "/" + documentId;
      URL url = presignerService.presignPutUrl(STAGING_BUCKET, s3Key, Duration.ofDays(1), null,
          null, Optional.empty(), Map.of());
      new HttpServiceJdk11().put(url.toString(), Optional.empty(), Optional.empty(), "");

      // when
      Map<String, Object> map = loadFileAsMap(this, "/documents-compress-event.json",
          "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.json", s3Key);
      this.handleRequest(map);

      // then
      assertTrue(s3.getObjectMetadata(STAGING_BUCKET, s3Key, null).isObjectExists());
      ReceiveMessageResponse response = getReceiveMessageResponse(queueUrl);
      assertEquals(1, response.messages().size());
      Message msg = response.messages().get(0);
      Map<String, Object> json = GSON.fromJson(msg.body(), Map.class);
      json = GSON.fromJson((String) json.get("Message"), Map.class);
      assertEquals(siteId, json.get("siteId"));
      assertEquals(documentId, json.get("documentId"));
      assertEquals("actions", json.get("type"));
    }
  }

  /**
   * Tests document compression event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testDocumentsCompress() throws Exception {
    // given
    String fileContent = loadFile(this, "/compression-request-file.json");
    s3.putObject(STAGING_BUCKET, "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.json",
        fileContent.getBytes(UTF_8), null, null);
    List<String> filePathsToCompress =
        Arrays.asList("/255kb-text.txt", "/256kb-text.txt", "/multipart01.txt", "/multipart02.txt");
    List<String> docIds = Arrays.asList("56dbfd71-bdd4-4fa8-96ca-4cf69fe93cb8",
        "6e775220-ff21-4bb0-a9e5-4d5f383c8881", "758d5107-e50f-4c62-b9b9-fd0347aa242b",
        "b37c138e-9782-40da-8e22-23412fc75035");

    Map<String, Long> expectedChecksums = new HashMap<>();
    for (int i = 0; i < docIds.size(); ++i) {
      final String docId = docIds.get(i);
      final String path = filePathsToCompress.get(i);
      final byte[] content = loadFileAsByteArray(this, path);
      this.createDocument(content, docId);
      expectedChecksums.put(docId, DocumentCompressorTest.getContentChecksum(content));
    }
    assertFalse(expectedChecksums.isEmpty());

    // when
    Map<String, Object> map = loadFileAsMap(this, "/documents-compress-event.json");
    this.handleRequest(map);

    // then
    String zipKey = "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.zip";
    try (InputStream zipContent = s3.getContentAsInputStream(STAGING_BUCKET, zipKey)) {
      DocumentCompressorTest.validateZipContent(zipContent, expectedChecksums);
    }
  }

  /**
   * Test .fkb64 file with TAGS.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension01() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    item.put("tags",
        List.of(Map.of("documentId", item.getDocumentId(), "key", "category", "value", "person",
            "insertedDate", new Date(), "userId", "joe", "type",
            DocumentTagType.USERDEFINED.name())));

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file without TAGS.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension02() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file without CONTENT.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension03() throws IOException {
    DynamicDocumentItem item = createDocumentItem();
    item.put("content", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);
    }

    assertTrue(logger.containsString("Skipping " + item.getPath() + " no content"));
  }

  /**
   * Test .fkb64 file without DocumentId.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension04() throws IOException {
    DynamicDocumentItem item = createDocumentItem();
    item.setDocumentId(null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file with multiple documents.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension05() throws IOException {
    String documentId0 = "0d1a788d-9a70-418a-8c33-a9ee9c1a0173";
    String documentId1 = "24af57ca-f61d-4ff8-b8a0-d7666073560e";
    String documentId2 = "f2416702-6b3c-4d29-a217-82a43b16b964";

    ZonedDateTime nowDate = ZonedDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = createDatabaseKey(siteId, documentId0 + FORMKIQ_B64_EXT);

      String json = loadFile(this, "/document_multiple" + FORMKIQ_B64_EXT);
      s3.putObject(STAGING_BUCKET, key, json.getBytes(UTF_8), null, null);

      // when
      handleRequest(loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key));

      // then
      assertEquals(1,
          service.findDocumentsByDate(siteId, nowDate, null, MAX_RESULTS).getResults().size());

      DocumentItem i =
          service.findDocument(siteId, documentId0, true, null, MAX_RESULTS).getResult();
      assertNull(i.getContentType());
      verifyBelongsToDocument(i, Arrays.asList(documentId1, documentId2));

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId0, null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("formName", tags.get(0).getKey());
      assertEquals("Job Application Form", tags.get(0).getValue());

      String k = createDatabaseKey(siteId, i.getDocumentId());
      assertFalse(s3.getObjectMetadata(DOCUMENTS_BUCKET, k, null).isObjectExists());

      i = service.findDocument(siteId, documentId1, true, null, MAX_RESULTS).getResult();
      assertEquals("application/json", i.getContentType());
      assertEquals(documentId0, i.getBelongsToDocumentId());
      tags = service.findDocumentTags(siteId, documentId1, null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("formData", tags.get(0).getKey());
      assertEquals("", tags.get(0).getValue());

      k = createDatabaseKey(siteId, i.getDocumentId());
      assertEquals("application/json",
          s3.getObjectMetadata(DOCUMENTS_BUCKET, k, null).getContentType());

      i = service.findDocument(siteId, documentId2);
      assertEquals(documentId0, i.getBelongsToDocumentId());
      assertNull(i.getContentType());
      tags = service.findDocumentTags(siteId, documentId2, null, MAX_RESULTS).getResults();
      assertEquals(2, tags.size());
      assertEquals("attachmentField", tags.get(0).getKey());
      assertEquals("resume", tags.get(0).getValue());
      assertEquals("category", tags.get(1).getKey());
      assertEquals("", tags.get(1).getValue());

      k = createDatabaseKey(siteId, i.getDocumentId());
      assertFalse(s3.getObjectMetadata(DOCUMENTS_BUCKET, k, null).isObjectExists());
    }
  }

  /**
   * Test .fkb64 file with TTL.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension06() throws IOException {
    String timeToLive = "1612061365";
    DynamicDocumentItem item = createDocumentItem();
    item.put("TimeToLive", timeToLive);

    item.put("tags",
        List.of(Map.of("documentId", item.getDocumentId(), "key", "category", "value", "person",
            "insertedDate", new Date(), "userId", "joe", "type",
            DocumentTagType.USERDEFINED.name())));

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);

      String key = createDatabaseKey(siteId, item.getDocumentId());
      String content = s3.getContentAsString(DOCUMENTS_BUCKET, key, null);
      assertEquals("VGhpcyBpcyBhIHRlc3Q=", content);

      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.getDocumentId()))
          .tableName(DOCUMENTS_TABLE).build();

      DynamoDbConnectionBuilder dbBuilder =
          awsServices.getExtension(DynamoDbConnectionBuilder.class);
      try (DynamoDbClient dbClient = dbBuilder.build()) {
        Map<String, AttributeValue> result = dbClient.getItem(r).item();
        assertEquals(timeToLive, result.get("TimeToLive").n());
      }
    }
  }

  /**
   * Test .fkb64 file minimum attributes.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension07() throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      logger.getMessages().clear();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      String documentId = findDocumentIdFromLogger(siteId);
      assertEquals(documentId, UUID.fromString(documentId).toString());
      assertTrue(logger.containsString("Removing " + key + " from bucket example-bucket."));

      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      DocumentItem item = service.findDocument(siteId, documentId);
      // assertEquals("14", item.getContentLength().toString());
      assertNull(item.getContentLength());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getInsertedDate());
      assertNotNull(item.getChecksum());
      // assertTrue(item.getChecksum() != null && item.getInsertedDate() != null);
      assertEquals(item.getDocumentId(), item.getPath());
      assertEquals("joesmith", item.getUserId());

      final int count = 2;
      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();

      int i = 0;
      assertEquals(count, tags.size());

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "category", "value",
          "document", "type", "USERDEFINED", "userId", "joesmith"));

      assertEqualsTag(tags.get(i), Map.of("documentId", documentId, "key", "status", "values",
          Arrays.asList("active", "notactive"), "type", "USERDEFINED", "userId", "joesmith"));
    }
  }

  /**
   * Test .fkb64 file with tagschema & composite key.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension08() throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("tagSchemaId", ID.uuid());
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.TRUE);
    data.put("newCompositeTags", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      logger.getMessages().clear();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      String documentId = findDocumentIdFromLogger(siteId);
      assertEquals(documentId, UUID.fromString(documentId).toString());
      assertTrue(logger.containsString("Removing " + key + " from bucket example-bucket."));

      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      DocumentItem item = service.findDocument(siteId, documentId);
      assertNull(item.getContentLength());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getInsertedDate());
      assertNotNull(item.getChecksum());
      assertEquals(item.getDocumentId(), item.getPath());
      assertEquals("joesmith", item.getUserId());

      int i = 0;
      final int count = 2;
      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();
      assertEquals(count, tags.size());

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "category", "value",
          "document", "type", "USERDEFINED", "userId", "joesmith"));

      assertEqualsTag(tags.get(i), Map.of("documentId", documentId, "key", "status", "values",
          Arrays.asList("active", "notactive"), "type", "USERDEFINED", "userId", "joesmith"));
    }
  }

  /**
   * Test .fkb64 file with tagschema & without composite key.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension09() throws IOException {
    final String documentId = "12345";
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("tagSchemaId", ID.uuid());
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      logger.getMessages().clear();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      DocumentItem item = service.findDocument(siteId, documentId);
      assertNull(item.getContentLength());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getChecksum());
      assertNotNull(item.getInsertedDate());
      assertEquals(item.getDocumentId(), item.getPath());
      assertEquals("joesmith", item.getUserId());

      int i = 0;
      final int count = 3;
      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();
      assertEquals(count, tags.size());

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "category", "value",
          "document", "type", "USERDEFINED", "userId", "joesmith"));

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "status", "values",
          Arrays.asList("active", "notactive"), "type", "USERDEFINED", "userId", "joesmith"));

      assertEqualsTag(tags.get(i), Map.of("documentId", documentId, "key", "test", "value",
          "novalue", "type", "USERDEFINED", "userId", "joe"));
    }
  }

  /**
   * Test .fkb64 file with actions.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension10() throws IOException {

    final String documentId = "12345";
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("tagSchemaId", ID.uuid());
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      logger.getMessages().clear();

      data.put("actions",
          Arrays.asList(
              Map.of("type", "ocr", "status", "PENDING", "parameters", Map.of("test", "1234")),
              Map.of("type", "webhook", "userId", "joesmith")));

      DynamicDocumentItem ditem = new DynamicDocumentItem(data);

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      List<Action> actions = actionsService.getActions(siteId, documentId);
      assertEquals(2, actions.size());
      assertEquals("OCR", actions.get(0).type().name());
      assertEquals("PENDING", actions.get(0).status().name());
      assertEquals("System", actions.get(0).userId());
      assertNotNull(actions.get(0).insertedDate());

      assertEquals("WEBHOOK", actions.get(1).type().name());
      assertEquals("PENDING", actions.get(1).status().name());
      assertEquals("joesmith", actions.get(1).userId());
      assertNotNull(actions.get(1).insertedDate());

      actions.get(0).status(ActionStatus.COMPLETE);
      actionsService.saveNewActions(siteId, documentId, actions);

      // given
      data.put("actions", List
          .of(Map.of("type", "ocr", "status", "PENDING", "parameters", Map.of("test", "1234"))));
      ditem = new DynamicDocumentItem(data);
      content = GSON.toJson(ditem).getBytes(UTF_8);

      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when - run a 2nd time
      handleRequest(map);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals("OCR", actions.get(0).type().name());
      assertEquals("PENDING", actions.get(0).status().name());
      assertEquals("System", actions.get(0).userId());
    }
  }

  /**
   * Test .fkb64 files with existing path and CLI agent.
   *
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension11() throws IOException, InterruptedException {

    String path = "sample/test.txt";
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("path", path);
    data.put("agent", DocumentSyncServiceType.FORMKIQ_CLI.name());
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.FALSE);
    data.put("content", "this is first data");

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      logger.getMessages().clear();

      String key = createDatabaseKey(siteId, path + FORMKIQ_B64_EXT);
      if (siteId == null) {
        key = "default/" + key;
      }

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      // when - send the same path twice
      for (int i = 0; i < 2; i++) {
        ditem.put("content", "this is some content: " + i);
        byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
        s3.putObject(STAGING_BUCKET, key, content, null, null);

        handleRequest(map);
        TimeUnit.SECONDS.sleep(1);
      }

      // then
      Map<String, Object> index = folderIndexProcesor.getIndex(siteId, path);

      String documentId = (String) index.get("documentId");
      DocumentItem item = service.findDocument(siteId, documentId);
      assertNull(item.getContentLength());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getChecksum());
      assertNotNull(item.getInsertedDate());
      assertEquals(path, item.getPath());
      assertEquals("joesmith", item.getUserId());

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();
      assertEquals(0, tags.size());

      verifyCliSyncs(siteId, documentId);
    }
  }

  /**
   * Test add tags to existing Document with .fkb64 file and NO content and FULLTEXT action.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension12() throws Exception {
    final Date now = new Date();
    final String userId = "joesmith";
    final long contentLength = 1000;
    String documentId = ID.uuid();
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", userId);
    data.put("tags", List.of(Map.of("key", "category", "value", "document")));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      item.setContentLength(contentLength);

      service.saveDocument(siteId, item,
          Arrays.asList(new DocumentTag(documentId, "playerId", "1234", new Date(), userId),
              new DocumentTag(documentId, "category", "person", new Date(), userId)));

      actionsService.saveAction(siteId, documentId,
          new Action().type(ActionType.FULLTEXT).userId("joe").status(ActionStatus.COMPLETE), 0);

      TimeUnit.SECONDS.sleep(1);

      String key = createDatabaseKey(siteId, documentId + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = GSON.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      TimeUnit.SECONDS.sleep(1);

      // when
      handleRequest(map);

      // then
      final int count = 2;
      item = service.findDocument(siteId, documentId);

      assertEquals(userId, item.getUserId());
      assertEquals(contentLength, item.getContentLength().longValue());
      assertNotEquals(item.getInsertedDate(), item.getLastModifiedDate());

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();
      assertEquals(count, tags.size());

      int i = 0;
      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "category", "value",
          "document", "type", "USERDEFINED", "userId", userId));

      assertEqualsTag(tags.get(i), Map.of("documentId", documentId, "key", "playerId", "value",
          "1234", "type", "USERDEFINED", "userId", userId));

      List<Action> actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.PENDING, actions.get(0).status());
      assertPublishedSnsMessage();
    }
  }

  /**
   * Test .fkb64 file with Metadata.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension13() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
        Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      processFkB64File(siteId, item, null);
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals(2, doc.getMetadata().size());
      Iterator<DocumentMetadata> itr = doc.getMetadata().iterator();
      DocumentMetadata md = itr.next();
      assertEquals("category", md.getKey());
      assertEquals("person", md.getValue());
      md = itr.next();
      assertEquals("playerId", md.getKey());
      assertEquals("[111, 222]", md.getValues().toString());
    }
  }

  /**
   * Test .fkb64 file add Metadata.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension14() throws IOException, ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DynamicDocumentItem item = createDocumentItem();

      item.put("tags", new ArrayList<>());
      item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
          Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));
      service.saveDocument(siteId, item, null);

      item.put("metadata", List.of(Map.of("key", "playerId", "value", "333")));

      processFkB64File(siteId, item, "14");
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals(2, doc.getMetadata().size());
      Iterator<DocumentMetadata> itr = doc.getMetadata().iterator();
      DocumentMetadata md = itr.next();
      assertEquals("category", md.getKey());
      assertEquals("person", md.getValue());
      md = itr.next();
      assertEquals("playerId", md.getKey());
      assertNull(md.getValues());
      assertEquals("333", md.getValue());
    }
  }

  /**
   * Test .fkb64 file removing Metadata.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension15() throws IOException, ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DynamicDocumentItem item = createDocumentItem();

      item.put("tags", new ArrayList<>());
      item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
          Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));
      service.saveDocument(siteId, item, null);

      item.put("metadata", List.of(Map.of("key", "playerId")));

      processFkB64File(siteId, item, "14");
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals(1, doc.getMetadata().size());
      Iterator<DocumentMetadata> itr = doc.getMetadata().iterator();
      DocumentMetadata md = itr.next();
      assertEquals("category", md.getKey());
      assertEquals("person", md.getValue());
    }
  }

  /**
   * Test Add .fkb64 file deep link.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension16() throws IOException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DynamicDocumentItem item = createDocumentItem();
      item.setPath(null);
      item.remove("content");
      item.put("tags", List.of(Map.of("key", "category", "value", "document")));
      item.setDeepLinkPath("https://google.com/sample.pdf");
      item.setContentType("application/pdf");

      processFkB64File(siteId, item, null);
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("https://google.com/sample.pdf", doc.getDeepLinkPath());
      assertEquals("sample.pdf", doc.getPath());
      assertEquals("application/pdf", doc.getContentType());
    }
  }

  /**
   * Test Add .fkb64 file deep link with path.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension17() throws IOException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DynamicDocumentItem item = createDocumentItem();
      item.remove("content");
      item.put("tags", List.of(Map.of("key", "category", "value", "document")));
      item.setDeepLinkPath("https://google.com/sample.pdf");
      item.setContentType("application/pdf");

      processFkB64File(siteId, item, null);
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("https://google.com/sample.pdf", doc.getDeepLinkPath());
      assertEquals("test.txt", doc.getPath());
      assertEquals("application/pdf", doc.getContentType());
    }
  }

  /**
   * Test Update .fkb64 file deep link with content.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension18() throws IOException, ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DynamicDocumentItem item = createDocumentItem();
      item.put("tags", List.of(Map.of("key", "category", "value", "document")));
      item.setDeepLinkPath("https://google.com/sample.pdf");
      service.saveDocument(siteId, item, null);

      item.put("content", "this is some content");

      processFkB64File(siteId, item, "14");
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("", doc.getDeepLinkPath());
    }
  }

  /**
   * Test Update .fkb64 file attributes.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testFkB64Extension19() throws IOException {
    // given
    final int limit = 10;
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "security",
          AttributeDataType.STRING, AttributeType.STANDARD);

      String documentId = ID.uuid();
      String json =
          "{\"metadata\":[],\"newCompositeTags\":false,\"accessAttributes\":[],\"documents\":[],"
              + "\"attributes\":[{\"key\":\"security\",\"stringValue\":\"confidential\","
              + "\"stringValues\":[],\"numberValues\":[]}],\"documentId\":\"" + documentId
              + "\",\"actions\":[],\"contentType\":\"application/octet-stream\","
              + "\"userId\":\"joesmith\",\"content\":\"test\",\"tags\":[]}";

      Map<String, Object> map = GSON.fromJson(json, Map.class);
      DynamicDocumentItem item = new DynamicDocumentItem(map);

      // when
      processFkB64File(siteId, item);

      // then
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("joesmith", doc.getUserId());

      PaginationResults<DocumentAttributeRecord> documentAttributes =
          service.findDocumentAttributes(siteId, documentId, null, limit);
      assertEquals(1, documentAttributes.getResults().size());

      DocumentAttributeRecord r = documentAttributes.getResults().get(0);
      assertEquals("security", r.getKey());
      assertEquals(DocumentAttributeValueType.STRING, r.getValueType());
      assertEquals("confidential", r.getStringValue());
      assertNull(r.getBooleanValue());
      assertNull(r.getNumberValue());
    }
  }

  /**
   * Test invalid request.
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testInvalidRequest() {
    // given
    Map<String, Object> requestMap = new HashMap<>();

    // when
    handleRequest(requestMap);

    // then
  }

  /**
   * Test processing S3 file from PATCH /documents/tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testPatchDocumentsTags01() throws ValidationException {
    // given
    final int maxDocuments = 150;

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = "category";
      String value = "person";

      String newKey = "person";
      String newValue = "111";

      List<String> documentIds = new ArrayList<>();
      for (int i = 0; i < maxDocuments; i++) {
        DynamicDocumentItem item = createDocumentItem();
        documentIds.add(item.getDocumentId());

        Collection<DocumentTag> tags =
            List.of(new DocumentTag(item.getDocumentId(), key, value, new Date(), "joe"));
        service.saveDocument(siteId, item, tags);
      }

      List<AddDocumentTag> tags =
          Collections.singletonList(new AddDocumentTag().key(newKey).value(newValue));
      UpdateMatchingDocumentTagsRequest req = new UpdateMatchingDocumentTagsRequest()
          .match(new UpdateMatchingDocumentTagsRequestMatch()
              .tag(new MatchDocumentTag().key(key).eq(value)))
          .update(new UpdateMatchingDocumentTagsRequestUpdate().tags(tags));

      byte[] data = GSON.toJson(req).getBytes(StandardCharsets.UTF_8);

      String s3Key =
          createS3Key(siteId, "patch_documents_tags_" + UUID.randomUUID() + FORMKIQ_B64_EXT);
      s3.putObject(STAGING_BUCKET, s3Key, data, "application/json");

      Map<String, Object> requestMap = createRequestMap(s3Key);

      // when
      handleRequest(requestMap);

      // then
      for (String documentId : documentIds) {
        assertEquals(newValue, service.findDocumentTag(siteId, documentId, newKey).getValue());
      }

      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, s3Key, null).isObjectExists());
    }
  }

  /**
   * Test processing S3 file from PATCH /documents/tags multiple tags.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testPatchDocumentsTags02() throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = "category";
      String value = "person";

      String newKey0 = "person";
      String newValue0 = "111";

      String newKey1 = "player";
      String newValue1 = "222";

      DynamicDocumentItem item = createDocumentItem();

      service.saveDocument(siteId, item,
          List.of(new DocumentTag(item.getDocumentId(), key, value, new Date(), "joe")));

      List<AddDocumentTag> tags = Arrays.asList(new AddDocumentTag().key(newKey0).value(newValue0),
          new AddDocumentTag().key(newKey1).value(newValue1));

      UpdateMatchingDocumentTagsRequest req = new UpdateMatchingDocumentTagsRequest()
          .match(new UpdateMatchingDocumentTagsRequestMatch()
              .tag(new MatchDocumentTag().key(key).eq(value)))
          .update(new UpdateMatchingDocumentTagsRequestUpdate().tags(tags));

      byte[] data = GSON.toJson(req).getBytes(StandardCharsets.UTF_8);

      String s3Key =
          createS3Key(siteId, "patch_documents_tags_" + UUID.randomUUID() + FORMKIQ_B64_EXT);
      s3.putObject(STAGING_BUCKET, s3Key, data, "application/json");

      Map<String, Object> requestMap = createRequestMap(s3Key);

      // when
      handleRequest(requestMap);

      // then
      assertEquals(newValue0,
          service.findDocumentTag(siteId, item.getDocumentId(), newKey0).getValue());
      assertEquals(newValue1,
          service.findDocumentTag(siteId, item.getDocumentId(), newKey1).getValue());

      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, s3Key, null).isObjectExists());
    }
  }

  /**
   * Test ZIP file upload event in temp files is skipped.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testTempFilesZipEvent() throws Exception {
    final Map<String, Object> map = loadFileAsMap(this, "/temp-files-zip-created-event.json");
    final String key = "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.zip";
    this.handleRequest(map);

    assertTrue(logger.containsString(String.format("skipping event for key %s", key)));

    verifySqsMessages();
  }

  /**
   * S3 Object Unknown Event Unit Test.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void testUnknownEvent01() throws Exception {
    // given
    final Map<String, Object> map = loadFileAsMap(this, "/objectunknown-event1.json");

    // when
    Void result = handler.handleRequest(map, null);

    // then
    assertNull(result);

    assertTrue(logger.containsString("handling 1 record(s)."));
    assertTrue(logger.containsString("skipping event ObjectUnknwn:Delete"));

    verifySqsMessages();
  }

  private void verifyBelongsToDocument(final DocumentItem item, final List<String> documentIds) {
    assertEquals(documentIds.size(), item.getDocuments().size());

    for (int i = 0; i < documentIds.size(); i++) {
      assertEquals(documentIds.get(i), item.getDocuments().get(i).getDocumentId());
      assertNotNull(item.getDocuments().get(i).getInsertedDate());
      assertNotNull(item.getDocuments().get(i).getBelongsToDocumentId());
    }
  }

  private void verifyCliSyncs(final String siteId, final String documentId) {
    int i = 0;
    PaginationResults<DocumentSyncRecord> syncs =
        syncService.getSyncs(siteId, documentId, null, MAX_RESULTS);

    final int expected = 2;
    List<DocumentSyncRecord> results = syncs.getResults();
    assertEquals(expected, results.size());

    assertEquals(documentId, results.get(i).getDocumentId());
    assertEquals(FORMKIQ_CLI, results.get(i).getService());
    assertEquals(DocumentSyncStatus.COMPLETE, results.get(i).getStatus());
    assertEquals(DocumentSyncType.CONTENT, results.get(i).getType());
    assertEquals("updated Document Content", results.get(i).getMessage());
    assertNotNull(results.get(i++).getSyncDate());

    assertEquals(documentId, results.get(i).getDocumentId());
    assertEquals(FORMKIQ_CLI, results.get(i).getService());
    assertEquals(DocumentSyncStatus.COMPLETE, results.get(i).getStatus());
    assertEquals(DocumentSyncType.CONTENT, results.get(i).getType());
    assertEquals("added Document Content", results.get(i).getMessage());
    assertNotNull(results.get(i).getSyncDate());
  }

  private void verifyS3Metadata(final String siteId, final DocumentItem item) {
    String key = createDatabaseKey(siteId, item.getDocumentId());
    S3ObjectMetadata objectMetadata = s3.getObjectMetadata(DOCUMENTS_BUCKET, key, null);
    assertTrue(objectMetadata.isObjectExists());

    assertNotNull(objectMetadata.getMetadata().get("checksum"));
  }

  /**
   * Verify SQS Messages.
   */
  private void verifySqsMessages() {

    final List<Message> create = sqsService.receiveMessages(snsSqsCreateQueueUrl).messages();
    final List<Message> delete = sqsService.receiveMessages(snsSqsDeleteQueueUrl).messages();

    assertEquals(0, create.size());
    assertEquals(0, delete.size());
  }
}
