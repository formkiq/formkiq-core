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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.module.documentevents.DocumentEventType.ACTIONS;
import static com.formkiq.module.documentevents.DocumentEventType.CREATE;
import static com.formkiq.module.documentevents.DocumentEventType.DELETE;
import static com.formkiq.module.documentevents.DocumentEventType.UPDATE;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.documentevents.DocumentEvent;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.ssm.SsmClient;

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
  private static final int PORT = 8080;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Builder;
  /** {@link S3Service}. */
  private static S3Service s3service;
  /** {@link DocumentService}. */
  private static DocumentServiceImpl service;
  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate1";
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** SQS Create Url. */
  private static String snsDocumentEvent;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsBuilder;
  /** {@link SqsClient}. */
  private static SqsClient sqsClient;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentEventUrl;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmBuilder;
  /** Test Timeout. */
  private static final long TEST_TIMEOUT = 30000L;
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
    sqsBuilder = TestServices.getSqsConnection(null);
    dbBuilder = DynamoDbTestServices.getDynamoDbConnection(null);
    db = dbBuilder.build();
    dbHelper = DynamoDbTestServices.getDynamoDbHelper(null);
    snsBuilder = TestServices.getSnsConnection(null);
    ssmBuilder = TestServices.getSsmConnection(null);

    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);

    try (SsmClient ssmClient = ssmBuilder.build()) {
      ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);
    }

    sqsClient = sqsBuilder.build();
    sqsService = new SqsService();

    service = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);

    if (!sqsService.exists(sqsClient, ERROR_SQS_QUEUE)) {
      sqsService.createQueue(sqsClient, ERROR_SQS_QUEUE);
    }

    if (!sqsService.exists(sqsClient, SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(sqsClient, SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    s3service = new S3Service(s3Builder);
    try (S3Client s3 = s3service.buildClient()) {
      s3service.createBucket(s3, "example-bucket");
    }

    snsService = new SnsService();

    try (SnsClient snsClient = snsBuilder.build()) {
      snsDocumentEvent = snsService.createTopic(snsClient, "createDocument1").topicArn();
      snsService.subscribe(snsClient, snsDocumentEvent, "sqs", sqsDocumentEventUrl);
    }

    dbHelper = new DynamoDbHelper(dbBuilder);
    if (!dbHelper.isTableExists(DOCUMENTS_TABLE)) {
      dbHelper.createDocumentsTable(DOCUMENTS_TABLE);
      dbHelper.createCacheTable(CACHE_TABLE);
    }
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link DocumentsS3Update}. */
  private DocumentsS3Update handler;

  /** {@link LambdaLoggerRecorder}. */
  private LambdaLoggerRecorder logger;
  /** {@link ClientAndServer}. */
  private ClientAndServer mockServer;

  /** FormKiQ Modules. */
  private List<String> modules = new ArrayList<>();

  private void addS3File(final String key, final String contentType, final boolean addTags,
      final String content) {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", contentType);

    try (S3Client s3 = s3service.buildClient()) {
      s3service.putObject(s3, "example-bucket", key, content.getBytes(StandardCharsets.UTF_8),
          contentType, metadata);

      if (addTags) {
        s3service.setObjectTags(s3, "example-bucket", key,
            Arrays.asList(Tag.builder().key("sample").value("12345").build(),
                Tag.builder().key("CLAMAV_SCAN_STATUS").value("GOOD").build()));
      }
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

    List<Message> msgs = sqsService.receiveMessages(sqsClient, sqsQueueUrl).messages();
    while (msgs.size() != 1) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsClient, sqsDocumentEventUrl).messages();
    }
    assertEquals(1, msgs.size());

    sqsService.deleteMessage(sqsClient, sqsQueueUrl, msgs.get(0).receiptHandle());
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
  @SuppressWarnings("resource")
  @BeforeEach
  public void before() throws URISyntaxException {

    try (S3Client s3 = s3service.buildClient()) {
      s3service.deleteAllFiles(s3, "example-bucket");
      s3service.deleteAllFiles(s3, "example-bucket");
    }

    dbHelper.truncateTable(DOCUMENTS_TABLE);

    Map<String, String> map = new HashMap<>();
    map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    map.put("SQS_ERROR_URL",
        TestServices.getLocalStack().getEndpointOverride(Service.SQS).toString() + "/queue/"
            + ERROR_SQS_QUEUE);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    map.put("AWS_REGION", AWS_REGION.id());
    map.put("APP_ENVIRONMENT", APP_ENVIRONMENT);

    this.modules.forEach(m -> map.put("MODULE_" + m, "true"));

    this.context = new LambdaContextRecorder();
    this.logger = (LambdaLoggerRecorder) this.context.getLogger();
    this.handler =
        new DocumentsS3Update(map, null, dbBuilder, s3Builder, ssmBuilder, sqsBuilder, snsBuilder);

    for (String queue : Arrays.asList(sqsDocumentEventUrl)) {
      ReceiveMessageResponse response = sqsService.receiveMessages(sqsClient, queue);
      while (!response.messages().isEmpty()) {
        for (Message msg : response.messages()) {
          sqsService.deleteMessage(sqsClient, queue, msg.receiptHandle());
        }

        response = sqsService.receiveMessages(sqsClient, queue);
      }
    }
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
    this.handler.handleRequest(map, this.context);

    // then
    return service.findDocument(siteId, documentId);
  }

  /**
   * Create Document Request without existing Tags/Formats.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
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
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      final int count = 3;
      assertEquals(count, tags.getResults().size());

      int i = 0;
      assertEquals("path", tags.getResults().get(i).getKey());
      assertEquals("test.txt", tags.getResults().get(i).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(i).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i).getType());
      assertEquals("joe", tags.getResults().get(i).getUserId());
      assertNotNull(tags.getResults().get(i++).getInsertedDate());

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("true", tags.getResults().get(i).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(i).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i).getType());
      assertEquals("joe", tags.getResults().get(i).getUserId());
      assertNotNull(tags.getResults().get(i++).getInsertedDate());

      assertEquals("userId", tags.getResults().get(i).getKey());
      assertEquals("joe", tags.getResults().get(i).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(i).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i).getType());
      assertEquals("joe", tags.getResults().get(i).getUserId());
      assertNotNull(tags.getResults().get(i++).getInsertedDate());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, false);
    }
  }

  /**
   * Update Document Request with existing Tags.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.reset();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectupdate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("asd");
      doc.setPath("test.txt");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", Arrays.asList(tag));
      service.saveDocumentItemWithTag(siteId, doc);

      DocumentFormat format = new DocumentFormat();
      format.setContentType("application/pdf");
      format.setDocumentId(BUCKET_KEY);
      format.setInsertedDate(new Date());
      format.setUserId("asd");
      service.saveDocumentFormat(siteId, format);

      addS3File(key, "pdf", true, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      final int size = 5;
      int i = 0;
      assertEquals(size, tags.getResults().size());
      assertEquals("CLAMAV_SCAN_STATUS", tags.getResults().get(i).getKey());
      assertEquals("GOOD", tags.getResults().get(i).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i++).getType());
      assertEquals("path", tags.getResults().get(i).getKey());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i++).getType());
      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(i++).getType());
      assertEquals("12345", tags.getResults().get(i).getValue());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(i).getType());
      assertEquals("12345", tags.getResults().get(i++).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(i).getType());
      assertEquals("asd", tags.getResults().get(i++).getValue());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, UPDATE, false, false);
    }
  }

  /**
   * Delete Document Request - core.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest04() throws Exception {

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

      final int count = 3;
      int i = 0;
      assertEquals(count, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("path").setValue("test.txt")
          .setDocumentId(BUCKET_KEY).setType(DocumentTagType.SYSTEMDEFINED).setUserId("joe"),
          tags.getResults().get(i++));

      assertDocumentTagEquals(new DocumentTag().setKey("untagged").setValue("true")
          .setDocumentId(BUCKET_KEY).setType(DocumentTagType.SYSTEMDEFINED).setUserId("joe"),
          tags.getResults().get(i++));

      assertDocumentTagEquals(new DocumentTag().setKey("userId").setValue("joe")
          .setDocumentId(BUCKET_KEY).setType(DocumentTagType.SYSTEMDEFINED).setUserId("joe"),
          tags.getResults().get(i++));

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, CREATE, false, true);

      tags = service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());
    }
  }

  /**
   * Test Processing Document with sub documents.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest05() throws Exception {
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
      assertEquals(2, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("category").setValue("none")
          .setType(DocumentTagType.USERDEFINED).setDocumentId(doc.getDocumentId()),
          tags.getResults().get(0));
      assertDocumentTagEquals(
          new DocumentTag().setKey("userId").setValue(doc.getUserId())
              .setType(DocumentTagType.SYSTEMDEFINED).setDocumentId(doc.getDocumentId()),
          tags.getResults().get(1));

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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest06() throws Exception {

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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest09() throws Exception {
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

      for (String tagKey : Arrays.asList("untagged", "userId")) {
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
   * Create Document Request with ACTIONS.
   *
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest10() throws Exception {

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
      actionsService.saveActions(siteId, doc.getDocumentId(),
          Arrays.asList(new Action().type(ActionType.OCR).status(ActionStatus.COMPLETE)));

      addS3File(key, "pdf", false, "testdata");

      // when
      handleRequest(siteId, BUCKET_KEY, map);

      // then
      List<Action> actions = actionsService.getActions(siteId, doc.getDocumentId());
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.PENDING, actions.get(0).status());
      assertEquals(ActionType.OCR, actions.get(0).type());

      DocumentEvent e0 = null;
      DocumentEvent e1 = null;

      List<Message> msgs = sqsService.receiveMessages(sqsClient, sqsDocumentEventUrl).messages();

      while (e0 == null || e1 == null) {
        for (Message m : msgs) {
          Map<String, String> bodyMap = this.gson.fromJson(m.body(), Map.class);
          DocumentEvent de = this.gson.fromJson(bodyMap.get("Message"), DocumentEvent.class);
          if (de.type().equals(CREATE)) {
            e0 = de;
          } else if (de.type().equals(ACTIONS)) {
            e1 = de;
          }
        }
        Thread.sleep(SLEEP);
        msgs = sqsService.receiveMessages(sqsClient, sqsDocumentEventUrl).messages();
      }

      assertEquals(siteId != null ? siteId : "default", e0.siteId());
      assertEquals(doc.getDocumentId(), e0.documentId());
      assertEquals("test.txt", e0.path());

      assertEquals(siteId != null ? siteId : "default", e1.siteId());
      assertEquals(doc.getDocumentId(), e1.documentId());
    }
  }

  /**
   * Delete Document Request - OCR / Fulltext (500).
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest11() throws Exception {

    createMockServer(DocumentsS3Update.SERVER_ERROR);
    this.modules = Arrays.asList("ocr", "fulltext");
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
      assertNotNull(item);
    }
  }

  /**
   * Delete Document Request - OCR / Fulltext (200).
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest12() throws Exception {

    createMockServer(OK);
    this.modules = Arrays.asList("ocr", "fulltext");
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
  @Timeout(unit = TimeUnit.MILLISECONDS, value = TEST_TIMEOUT)
  public void testHandleRequest13() throws Exception {

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
      actionsService.saveActions(siteId, doc.getDocumentId(),
          Arrays.asList(new Action().type(ActionType.OCR).status(ActionStatus.RUNNING)));

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

    assertTrue(this.logger
        .containsString("saving document " + createDatabaseKey(siteId, item.getDocumentId())));

    assertEquals(contentType, item.getContentType());
    assertEquals(contentLength, item.getContentLength().toString());

    try (S3Client s3 = s3service.buildClient()) {
      s3service.deleteAllObjectTags(s3, "example-bucket", item.getDocumentId());
      service.deleteDocumentTags(siteId, item.getDocumentId());
    }

    return item;
  }
}
