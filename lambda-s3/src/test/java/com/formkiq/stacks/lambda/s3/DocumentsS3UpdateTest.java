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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.module.events.document.DocumentEventType.CREATE;
import static com.formkiq.module.events.document.DocumentEventType.DELETE;
import static com.formkiq.module.events.document.DocumentEventType.UPDATE;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.schema.DocumentSchema;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceImpl;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.DynamicDocumentTag;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.stacks.lambda.s3.util.LambdaLoggerRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** {@link DocumentsS3Update} Unit Tests. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsS3UpdateTest implements DbKeys {

  /** {@link DocumentService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** Bucket Key. */
  private static final String BUCKET_KEY = "b53c92cf-f7b9-4787-9541-76574ec70d71";
  /** {@link DynamoDbClient}. */
  private static DynamoDbClient db;
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbBuilder;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;

  /** SQS Error Queue. */
  private static final String ERROR_SQS_QUEUE = "sqserror";

  /** Request OK Status. */
  private static final int OK = 200;
  /** Port to run Test server. */
  private static final int PORT = 8888;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Builder;
  /** {@link S3Service}. */
  private static S3Service s3service;
  /** {@link DocumentService}. */
  private static DocumentServiceImpl service;
  /** 500 SECONDS. */
  private static final long SLEEP = 500L;
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate1";
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** SQS Create Url. */
  private static String snsDocumentEvent;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentEventUrl;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
  /** Test Timeout. */
  private static final long TEST_TIMEOUT = 30;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;

  /**
   * AfterAll.
   */
  @AfterAll
  public static void afterAll() {
    db.close();
  }

  /**
   * Before Class.
   * 
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws URISyntaxException, InterruptedException, IOException {

    s3Builder = TestServices.getS3Connection(null);
    dbBuilder = DynamoDbTestServices.getDynamoDbConnection();
    db = dbBuilder.build();
    dbHelper = DynamoDbTestServices.getDynamoDbHelper(null);
    snsBuilder = TestServices.getSnsConnection(null);

    SqsConnectionBuilder sqsBuilder = TestServices.getSqsConnection(null);
    sqsService = new SqsServiceImpl(sqsBuilder);

    if (!sqsService.exists(ERROR_SQS_QUEUE)) {
      sqsService.createQueue(ERROR_SQS_QUEUE);
    }

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    s3service = new S3Service(s3Builder);
    s3service.createBucket("example-bucket");

    snsService = new SnsService(snsBuilder);

    snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();

    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentEventUrl);
    snsService.subscribe(snsDocumentEvent, "sqs", sqsQueueArn);

    dbHelper = new DynamoDbHelper(dbBuilder);

    try (DynamoDbClient dbClient = dbBuilder.build()) {
      DocumentSchema schema = new DocumentSchema(dbClient);
      schema.createDocumentsTable(DOCUMENTS_TABLE);
      schema.createCacheTable(CACHE_TABLE);
    }

    service = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);

    Map<String, String> map = new HashMap<>();
    map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    map.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    map.put("AWS_REGION", AWS_REGION.id());
    map.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    map.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    awsServices =
        new AwsServiceCacheBuilder(map, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(),
                new SmsAwsServiceRegistry())
            .build();

    handler = new DocumentsS3Update(awsServices);

    SsmService ssmService = awsServices.getExtension(SsmService.class);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link DocumentsS3Update}. */
  private static DocumentsS3Update handler;

  /** {@link LambdaLoggerRecorder}. */
  private LambdaLoggerRecorder logger;
  /** {@link ClientAndServer}. */
  private ClientAndServer mockServer;

  private void addS3File(final String key, final String contentType, final boolean addTags,
      final String content) {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", contentType);

    s3service.putObject("example-bucket", key, content.getBytes(StandardCharsets.UTF_8),
        contentType, metadata);

    if (addTags) {
      s3service.setObjectTags("example-bucket", key,
          Arrays.asList(Tag.builder().key("sample").value("12345").build(),
              Tag.builder().key("CLAMAV_SCAN_STATUS").value("GOOD").build()));
    }
  }

  /**
   * After Class.
   * 
   */
  @AfterEach
  public void afterClass() {
    if (this.mockServer != null) {
      this.mockServer.stop();
    }
    this.mockServer = null;
  }

  /**
   * Assert {@link DocumentTag}.
   * 
   * @param expected {@link DocumentTag}
   * @param actual {@link DocumentTag}
   */
  private void assertDocumentTagEquals(final DocumentTag expected, final DocumentTag actual) {
    assertEquals(expected.getKey(), actual.getKey());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getDocumentId(), actual.getDocumentId());
    assertEquals(expected.getType(), actual.getType());

    if (expected.getUserId() != null) {
      assertEquals(expected.getUserId(), actual.getUserId());
    } else {
      assertNotNull(actual.getUserId());
    }

    assertNotNull(actual.getInsertedDate());
  }

  /**
   * Assert Publish SNS Topic.
   * 
   * @param siteId {@link String}
   * @param sqsQueueUrl {@link String}
   * @param eventType {@link String}
   * @param hasContent boolean
   * @param childDoc boolean
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private void assertPublishSnsMessage(final String siteId, final String sqsQueueUrl,
      final String eventType, final boolean hasContent, final boolean childDoc)
      throws InterruptedException {

    List<Message> msgs = sqsService.receiveMessages(sqsQueueUrl).messages();
    while (msgs.size() != 1) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsDocumentEventUrl).messages();
    }
    assertEquals(1, msgs.size());

    sqsService.deleteMessage(sqsQueueUrl, msgs.get(0).receiptHandle());
    Map<String, String> map = this.gson.fromJson(msgs.get(0).body(), Map.class);
    String message = map.get("Message");

    map = this.gson.fromJson(message, Map.class);
    assertNotNull(map.get("documentId"));
    assertEquals(eventType, map.get("type"));

    if (!childDoc) {
      assertNotNull(map.get("path"));
    }

    if (hasContent) {
      assertEquals("text/plain", map.get("contentType"));
      assertNotNull(map.get("content"));
    } else {
      assertNull(map.get("content"));
    }

    if (!"delete".equals(eventType)) {
      assertNotNull(map.get("userId"));
    }

    if (siteId != null) {
      assertEquals(siteId, map.get("siteId"));
    } else {
      assertEquals("default", map.get("siteId"));
    }
  }

  /**
   * before.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeEach
  public void before() throws URISyntaxException {

    s3service.deleteAllFiles("example-bucket");
    s3service.deleteAllFiles("example-bucket");

    dbHelper.truncateTable(DOCUMENTS_TABLE);
    service.setLastShortDate(null);

    this.context = new LambdaContextRecorder();
    this.logger = (LambdaLoggerRecorder) this.context.getLogger();

    for (String queue : Arrays.asList(sqsDocumentEventUrl)) {
      ReceiveMessageResponse response = sqsService.receiveMessages(queue);
      while (!response.messages().isEmpty()) {
        for (Message msg : response.messages()) {
          sqsService.deleteMessage(queue, msg.receiptHandle());
        }

        response = sqsService.receiveMessages(queue);
      }
    }
  }

  private Date createDate2DaysAgo() {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate localDate = LocalDate.now().minusDays(2);
    Date date = Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());
    return date;
  }

  /**
   * Create Mock Server.
   * 
   * @param statusCode int
   */
  private void createMockServer(final int statusCode) {

    this.mockServer = startClientAndServer(Integer.valueOf(PORT));

    ExpectationStatusResponseCallback callback = new ExpectationStatusResponseCallback(statusCode);
    this.mockServer.when(request().withMethod("DELETE")).respond(callback);
  }

  /**
   * Create {@link DynamicDocumentItem} with Child Documents.
   * 
   * @param now {@link Date}
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createSubDocuments(final Date now) {
    String username = UUID.randomUUID() + "@formkiq.com";

    DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc.setContentType("text/plain");
    doc.put("tags",
        Arrays.asList(Map.of("documentId", doc.getDocumentId(), "key", "category", "value", "none",
            "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    DynamicDocumentItem doc1 = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc1.setContentType("text/html");
    doc1.put("tags", Arrays.asList(Map.of("documentId", doc1.getDocumentId(), "key", "category1",
        "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    DynamicDocumentItem doc2 = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc2.setContentType("application/json");
    doc2.put("tags", Arrays.asList(Map.of("documentId", doc2.getDocumentId(), "key", "category2",
        "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    doc.put("documents", Arrays.asList(doc1, doc2));

    return doc;
  }

  /**
   * Handle Request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param map {@link Map}
   * 
   * @return {@link DocumentItem}
   * @throws IOException IOException
   */
  private DocumentItem handleRequest(final String siteId, final String documentId,
      final Map<String, Object> map) throws IOException {

    // when
    handler.handleRequest(map, this.context);

    // then
    return service.findDocument(siteId, documentId);
  }

  /**
   * Create Document Request without existing Tags/Formats.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertFalse(item.getChecksum().startsWith("\""));
      assertFalse(item.getChecksum().endsWith("\""));
      assertNotNull(item.getS3version());

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      final int count = 1;
      assertEquals(count, tags.getResults().size());

      int i = 0;
      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("true", tags.getResults().get(i).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(i).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i).getType());
      assertEquals("joe", tags.getResults().get(i).getUserId());
      assertNotNull(tags.getResults().get(i++).getInsertedDate());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);

      assertNotNull(service.findMostDocumentDate());
    }
  }

  /**
   * Update Document Request with existing Tags.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest02() throws Exception {

    Date date = createDate2DaysAgo();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectupdate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(date);
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("asd");
      doc.setPath("test.txt");
      doc.setChecksum("ASD");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", Arrays.asList(tag));
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", true, "testdata");

      TimeUnit.SECONDS.sleep(1);

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertNotEquals(item.getInsertedDate(), item.getLastModifiedDate());

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      final int size = 3;
      int i = 0;
      assertEquals(size, tags.getResults().size());
      assertEquals("CLAMAV_SCAN_STATUS", tags.getResults().get(i).getKey());
      assertEquals("GOOD", tags.getResults().get(i).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i++).getType());
      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(i++).getType());
      assertEquals("12345", tags.getResults().get(i).getValue());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(i).getType());
      assertEquals("12345", tags.getResults().get(i++).getValue());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, UPDATE, false, false);
      assertNotNull(service.findMostDocumentDate());
    }
  }

  /**
   * Delete Document Request - S3 File exists (deleting version document).
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setPath("test.txt");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", Arrays.asList(tag));

      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertNotNull(item);
    }
  }

  /**
   * Delete Document Request - S3 File not exists. S3 Main document file was deleted.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest04() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setPath("test.txt");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", Arrays.asList(tag));

      service.saveDocumentItemWithTag(siteId, doc);

      // when
      DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertNull(item);
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, DELETE, false, true);
    }
  }

  /**
   * Create Document Request on child document.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest05() throws Exception {

    String documentId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setPath("test.txt");
      doc.setUserId("joe");

      DynamicDocumentItem child = new DynamicDocumentItem(Map.of());
      child.setInsertedDate(new Date());
      child.setDocumentId(documentId);
      child.setUserId("joe");
      doc.put("documents", Arrays.asList(child));

      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, child.getDocumentId(), map);

      // then
      assertNotNull(item.getBelongsToDocumentId());
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      try (DynamoDbClient client = dbBuilder.build()) {
        Map<String, AttributeValue> m =
            client.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
                .key(keysDocument(siteId, child.getDocumentId())).build()).item();
        assertNull(m.get(GSI1_PK));
      }

      final int count = 1;
      int i = 0;
      assertEquals(count, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("untagged").setValue("true")
          .setDocumentId(BUCKET_KEY).setType(DocumentTagType.SYSTEMDEFINED).setUserId("joe"),
          tags.getResults().get(i++));

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, true);

      tags = service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());

      assertNotNull(service.findMostDocumentDate());
    }
  }

  /**
   * Test Processing Document with sub documents.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();
      Date now = new Date();
      DynamicDocumentItem doc = createSubDocuments(now);
      service.saveDocumentItemWithTag(siteId, doc);

      String key = createDatabaseKey(siteId, doc.getDocumentId());
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentItem item = handleRequest(siteId, doc.getDocumentId(), map);
      DocumentItem itemchild =
          handleRequest(siteId, doc.getDocuments().get(0).getDocumentId(), map);

      // then
      Map<String, AttributeValue> m = db.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
          .key(keysDocument(siteId, doc.getDocumentId())).build()).item();
      assertNotNull(m.get(GSI1_PK));

      Map<String, AttributeValue> mchild =
          db.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
              .key(keysDocument(siteId, itemchild.getDocumentId())).build()).item();
      assertNull(mchild.get(GSI1_PK));

      assertEquals(doc.getDocumentId(), item.getDocumentId());
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, doc.getDocumentId(), null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("category").setValue("none")
          .setType(DocumentTagType.USERDEFINED).setDocumentId(doc.getDocumentId()),
          tags.getResults().get(0));

      tags = service.findDocumentTags(siteId, itemchild.getDocumentId(), null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("category1").setValue("")
          .setType(DocumentTagType.USERDEFINED).setDocumentId(itemchild.getDocumentId()),
          tags.getResults().get(0));
    }
  }

  /**
   * Create Document Request with Text Content.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest07() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "text/plain", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);
    }
  }

  /**
   * Create Document Request with Text Content TOO LARGE.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest08() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      String content = loadFile(this, "/256kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);
    }
  }

  /**
   * Create Document Request with Text Content+Payload TOO LARGE.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest09() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      String content = loadFile(this, "/255kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);
    }
  }

  /**
   * Create Document Request without existing Tags/Formats and TTL.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest10() throws Exception {
    String ttl = "1612061365";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      doc.put("TimeToLive", ttl);
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.getDocumentId()))
          .tableName(DOCUMENTS_TABLE).build();

      Map<String, AttributeValue> result = db.getItem(r).item();
      assertEquals(ttl, result.get("TimeToLive").n());

      for (String tagKey : Arrays.asList("untagged")) {
        r = GetItemRequest.builder().key(keysDocumentTag(siteId, item.getDocumentId(), tagKey))
            .tableName(DOCUMENTS_TABLE).build();

        result = db.getItem(r).item();
        assertEquals(ttl, result.get("TimeToLive").n());
      }

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);
    }
  }

  /**
   * Create Document Request with COMPLETED ACTIONS.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest11() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);
      actionsService.saveNewActions(siteId, doc.getDocumentId(), Arrays
          .asList(new Action().type(ActionType.OCR).userId("joe").status(ActionStatus.COMPLETE)));

      addS3File(key, "pdf", false, "testdata");

      // when
      handleRequest(siteId, BUCKET_KEY, map);

      // then
      List<Action> actions = actionsService.getActions(siteId, doc.getDocumentId());
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
      assertEquals(ActionType.OCR, actions.get(0).type());
    }
  }

  /**
   * Delete Document Request - OCR / Fulltext (500).
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest12() throws Exception {

    createMockServer(DocumentsS3Update.SERVER_ERROR);
    before();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      service.saveDocumentItemWithTag(siteId, doc);

      // when
      try {
        handleRequest(siteId, BUCKET_KEY, map);
      } catch (Exception e) {
        // then
        assertTrue(e.getMessage().contains("Unable to delete document"));

        DocumentItem item = service.findDocument(siteId, doc.getDocumentId());
        assertNotNull(item);
      }
    }
  }

  /**
   * Delete Document Request - OCR / Fulltext (200).
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest13() throws Exception {

    createMockServer(OK);
    before();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      service.saveDocumentItemWithTag(siteId, doc);

      // when
      DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertNull(item);
    }
  }

  /**
   * Create Document Request with 'running' ACTIONS.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest14() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);
      actionsService.saveNewActions(siteId, doc.getDocumentId(), Arrays
          .asList(new Action().type(ActionType.OCR).userId("joe").status(ActionStatus.RUNNING)));

      addS3File(key, "pdf", false, "testdata");

      // when
      handleRequest(siteId, BUCKET_KEY, map);

      // then
      List<Action> actions = actionsService.getActions(siteId, doc.getDocumentId());
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.RUNNING, actions.get(0).status());
      assertEquals(ActionType.OCR, actions.get(0).type());
    }
  }

  /**
   * Verify {@link DocumentItem}.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param contentType {@link String}
   * @param contentLength {@link String}
   * @return {@link DocumentItem}
   */
  private DocumentItem verifyDocumentSaved(final String siteId, final DocumentItem item,
      final String contentType, final String contentLength) {

    assertEquals(contentType, item.getContentType());
    assertEquals(contentLength, item.getContentLength().toString());

    s3service.deleteAllObjectTags("example-bucket", item.getDocumentId());
    service.deleteDocumentTags(siteId, item.getDocumentId());

    return item;
  }
}
