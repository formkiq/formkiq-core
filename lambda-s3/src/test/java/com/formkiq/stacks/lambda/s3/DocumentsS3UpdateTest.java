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
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceDynamoDb;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
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
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
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

/** {@link DocumentsS3Update} Unit Tests. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsS3UpdateTest implements DbKeys {

  /** Limit. */
  private static final int LIMIT = 100;
  /**
   * {@link DocumentService}.
   */
  private static ActionsService actionsService;
  /**
   * App Environment.
   */
  private static final String APP_ENVIRONMENT = "test";
  /**
   * Bucket Key.
   */
  private static final String BUCKET_KEY = "b53c92cf-f7b9-4787-9541-76574ec70d71";
  /**
   * {@link DynamoDbClient}.
   */
  private static DynamoDbClient db;
  /**
   * {@link DynamoDbConnectionBuilder}.
   */
  private static DynamoDbConnectionBuilder dbBuilder;

  /**
   * {@link DynamoDbHelper}.
   */
  private static DynamoDbHelper dbHelper;

  /**
   * SQS Error Queue.
   */
  private static final String ERROR_SQS_QUEUE = "sqserror";
  /**
   * {@link DocumentsS3Update}.
   */
  private static DocumentsS3Update handler;
  /**
   * Request OK Status.
   */
  private static final int OK = 200;
  /**
   * Port to run Test server.
   */
  private static final int PORT = 8888;
  /**
   * {@link S3Service}.
   */
  private static S3Service s3service;
  /**
   * {@link DocumentService}.
   */
  private static DocumentServiceImpl service;
  /**
   * 500 SECONDS.
   */
  private static final long SLEEP = 500L;
  /**
   * {@link SqsService}.
   */
  private static SqsService sqsService;

  /**
   * Test server URL.
   */
  private static final String URL = "http://localhost:" + PORT;
  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService;

  /**
   * After All.
   */
  @AfterAll
  public static void afterAll() {
    db.close();
  }

  private void login() {
    ApiAuthorization authorization = new ApiAuthorization().username("firstwriter");
    ApiAuthorization.login(authorization);
  }

  /**
   * Before Class.
   *
   * @throws URISyntaxException URISyntaxException
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws URISyntaxException, IOException {

    dbBuilder = DynamoDbTestServices.getDynamoDbConnection();
    db = dbBuilder.build();
    dbHelper = DynamoDbTestServices.getDynamoDbHelper();

    SqsConnectionBuilder sqsBuilder = TestServices.getSqsConnection(null);
    sqsService = new SqsServiceImpl(sqsBuilder);

    if (!sqsService.exists(ERROR_SQS_QUEUE)) {
      sqsService.createQueue(ERROR_SQS_QUEUE);
    }

    S3ConnectionBuilder s3Builder = TestServices.getS3Connection(null);
    s3service = new S3Service(s3Builder);
    s3service.createBucket("example-bucket");

    SnsConnectionBuilder snsBuilder = TestServices.getSnsConnection(null);
    SnsService snsService = new SnsService(snsBuilder);

    final String snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();

    dbHelper = new DynamoDbHelper(dbBuilder);

    try (DynamoDbClient dbClient = dbBuilder.build()) {
      DocumentSchema schema = new DocumentSchema(dbClient);
      schema.createDocumentsTable(DOCUMENTS_TABLE);
      schema.createCacheTable(CACHE_TABLE);
    }

    service = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE,
        new DocumentVersionServiceNoVersioning());
    syncService = new DocumentSyncServiceDynamoDb(dbBuilder, DOCUMENT_SYNCS_TABLE);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);

    Map<String, String> map = new HashMap<>();
    map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    map.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    map.put("CACHE_TABLE", CACHE_TABLE);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    map.put("AWS_REGION", AWS_REGION.id());
    map.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    map.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    AwsServiceCache awsServices =
        new AwsServiceCacheBuilder(map, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(),
                new SsmAwsServiceRegistry())
            .build();

    handler = new DocumentsS3Update(awsServices);

    SsmService ssmService = awsServices.getExtension(SsmService.class);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);
  }

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

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

  private void assertDocumentSyncs(final String siteId, final String documentId) {
    List<DocumentSyncRecord> syncs =
        notNull(syncService.getSyncs(siteId, documentId, null, LIMIT).getResults());
    assertEquals(2, syncs.size());

    assertDocumentSync(syncs.get(0), DocumentSyncType.CONTENT, "added Document Content");
    assertDocumentSync(syncs.get(1), DocumentSyncType.METADATA, "added Document Metadata");
  }

  private void assertDocumentSync(final DocumentSyncRecord sync, final DocumentSyncType syncType,
      final String message) {
    assertNull(sync.getSyncDate());
    assertEquals(DocumentSyncServiceType.EVENTBRIDGE, sync.getService());
    assertEquals(DocumentSyncStatus.PENDING, sync.getStatus());
    assertEquals(syncType, sync.getType());
    assertNotNull(sync.getInsertedDate());
    assertEquals(message, sync.getMessage());
  }

  /**
   * Assert Publish SNS Topic.
   *
   * @param siteId {@link String}
   * @param sqsQueueUrl {@link String}
   * @param eventType {@link String}
   * @param childDoc boolean
   * @throws InterruptedException InterruptedException
   */
  private void assertPublishSnsMessage2(final String siteId, final String sqsQueueUrl,
      final String eventType, final boolean childDoc) throws InterruptedException {

    List<Message> msgs = sqsService.receiveMessages(sqsQueueUrl).messages();
    while (msgs.size() != 1) {
      Thread.sleep(SLEEP);
      msgs = sqsService.receiveMessages(sqsQueueUrl).messages();
    }
    assertEquals(1, msgs.size());

    sqsService.deleteMessage(sqsQueueUrl, msgs.get(0).receiptHandle());
    Map<String, String> map = this.gson.fromJson(msgs.get(0).body(), Map.class);
    String message = map.get("Message");

    map = this.gson.fromJson(message, Map.class);
    assertEquals(eventType, map.get("type"));
    assertNotNull(map.get("documentId"));

    if (!childDoc) {
      assertNotNull(map.get("path"));
    }

    assertTrue(map.get("url").contains("example-bucket"));
    assertNull(map.get("content"));

    if (!"delete".equals(eventType)) {
      assertNotNull(map.get("userId"));
    }

    assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID), map.get("siteId"));
  }

  /**
   * before.
   */
  @BeforeEach
  public void before() {

    login();

    s3service.deleteAllFiles("example-bucket");
    s3service.deleteAllFiles("example-bucket");

    dbHelper.truncateTable(DOCUMENTS_TABLE);
    service.setLastShortDate(null);
  }

  private Date createDate2DaysAgo() {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate localDate = LocalDate.now().minusDays(2);
    return Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());
  }

  /**
   * Create Mock Server.
   *
   * @param statusCode int
   */
  private void createMockServer(final int statusCode) {

    this.mockServer = startClientAndServer(PORT);

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

    DynamicDocumentItem doc = new DynamicDocumentItem(
        Map.of("documentId", ID.uuid(), "userId", username, "insertedDate", now));
    doc.setContentType("text/plain");
    doc.put("tags",
        List.of(Map.of("documentId", doc.getDocumentId(), "key", "category", "value", "none",
            "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    DynamicDocumentItem doc1 = new DynamicDocumentItem(
        Map.of("documentId", ID.uuid(), "userId", username, "insertedDate", now));
    doc1.setContentType("text/html");
    doc1.put("tags", List.of(Map.of("documentId", doc1.getDocumentId(), "key", "category1",
        "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    DynamicDocumentItem doc2 = new DynamicDocumentItem(
        Map.of("documentId", ID.uuid(), "userId", username, "insertedDate", now));
    doc2.setContentType("application/json");
    doc2.put("tags", List.of(Map.of("documentId", doc2.getDocumentId(), "key", "category2",
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
   * @return {@link DocumentItem}
   */
  private DocumentItem handleRequest(final String siteId, final String documentId,
      final Map<String, Object> map) {

    // when
    handler.handleRequest(map, null);

    // then
    login();
    return service.findDocument(siteId, documentId);
  }

  /**
   * Create Document Request without existing Tags/Formats.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest01() throws Exception {

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      String content = "testdata";
      addS3File(key, "text/plain", false, content);

      // when
      DocumentItem item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertEquals("text/plain", item.getContentType());
      assertEquals(content.length(), item.getContentLength());
      assertNotNull(item.getS3version());
      assertEquals("joe", item.getUserId());

      assertFalse(item.getChecksum().startsWith("\""));
      assertFalse(item.getChecksum().endsWith("\""));
      assertNotNull(item.getS3version());

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      assertEquals(0, tags.getResults().size());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "text/plain", "8");
      assertDocumentSyncs(siteId, item.getDocumentId());

      assertNotNull(service.findMostDocumentDate());
    }
  }

  /**
   * Update Document Request with existing Tags.
   *
   * @throws Exception Exception
   */
  @Test
  // @Timeout(value = TEST_TIMEOUT)
  public void testHandleRequest02() throws Exception {

    // given
    Date date = createDate2DaysAgo();
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectupdate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(date);
      doc.setDocumentId(documentId);
      doc.setUserId("asd");
      doc.setPath("test.txt");
      doc.setChecksum("ASD");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", documentId, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", List.of(tag));
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", true, "testdata");

      TimeUnit.SECONDS.sleep(1);

      // when
      final DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      assertEquals(item.getInsertedDate(), item.getLastModifiedDate());

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);

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
      assertEquals("12345", tags.getResults().get(i).getValue());

      assertEquals(0,
          service.findDocumentFormats(siteId, documentId, null, MAX_RESULTS).getResults().size());

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertDocumentSyncs(siteId, item.getDocumentId());
      assertNotNull(service.findMostDocumentDate());
    }
  }

  /**
   * Delete Document Request - S3 File exists (deleting version document).
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest03() throws Exception {

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setPath("test.txt");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", List.of(tag));

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
  public void testHandleRequest04() throws Exception {
    // given
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setPath("test.txt");

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", documentId, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", List.of(tag));

      service.saveDocumentItemWithTag(siteId, doc);

      // when
      DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      assertNull(item);
      List<DocumentSyncRecord> syncs =
          notNull(syncService.getSyncs(siteId, documentId, null, 2).getResults());
      assertEquals(2, syncs.size());

      assertDocumentSync(syncs.get(0), DocumentSyncType.DELETE_METADATA,
          "deleted Document Metadata");
      assertNotNull(syncs.get(0).getTimeToLive());
      assertDocumentSync(syncs.get(1), DocumentSyncType.METADATA, "added Document Metadata");
      assertNull(syncs.get(1).getTimeToLive());
    }
  }

  /**
   * Create Document Request on child document.
   *
   * @throws Exception Exception
   */
  @Test
  // @Timeout(value = TEST_TIMEOUT)
  public void testHandleRequest05() throws Exception {

    // given
    String documentId = ID.uuid();
    String childDocumentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, childDocumentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setPath("test.txt");
      doc.setUserId("joe");

      DynamicDocumentItem child = new DynamicDocumentItem(Map.of());
      child.setInsertedDate(new Date());
      child.setDocumentId(childDocumentId);
      child.setUserId("joe");
      doc.put("documents", List.of(child));

      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, child.getDocumentId(), map);

      // then
      assertNotNull(item.getBelongsToDocumentId());
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, childDocumentId, null, MAX_RESULTS);

      try (DynamoDbClient client = dbBuilder.build()) {
        Map<String, AttributeValue> m =
            client.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
                .key(keysDocument(siteId, child.getDocumentId())).build()).item();
        assertNull(m.get(GSI1_PK));
      }

      assertEquals(0, tags.getResults().size());

      assertEquals(0, service.findDocumentFormats(siteId, childDocumentId, null, MAX_RESULTS)
          .getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");

      List<DocumentSyncRecord> syncs =
          notNull(syncService.getSyncs(siteId, documentId, null, LIMIT).getResults());
      assertEquals(1, syncs.size());
      assertDocumentSync(syncs.get(0), DocumentSyncType.METADATA, "added Document Metadata");

      assertDocumentSyncs(siteId, childDocumentId);

      tags = service.findDocumentTags(siteId, childDocumentId, null, MAX_RESULTS);
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
  public void testHandleRequest06() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
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
  public void testHandleRequest07() throws Exception {

    // given
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "text/plain", false, "testdata");

      // when
      final DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "8");
      assertDocumentSyncs(siteId, item.getDocumentId());
    }
  }

  /**
   * Create Document Request with Text Content TOO LARGE.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest08() throws Exception {
    // given
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      String content = loadFile(this, "/256kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertDocumentSyncs(siteId, item.getDocumentId());
    }
  }

  /**
   * Create Document Request with Text Content+Payload TOO LARGE.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest09() throws Exception {

    // given
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);

      String content = loadFile(this, "/255kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertDocumentSyncs(siteId, item.getDocumentId());
    }
  }

  /**
   * Create Document Request without existing Tags/Formats and TTL.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest10() throws Exception {
    // given
    String ttl = "1612061365";
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(documentId);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      doc.put("TimeToLive", ttl);
      service.saveDocumentItemWithTag(siteId, doc);

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentItem item = handleRequest(siteId, documentId, map);

      // then
      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.getDocumentId()))
          .tableName(DOCUMENTS_TABLE).build();

      Map<String, AttributeValue> result = db.getItem(r).item();
      assertEquals(ttl, result.get("TimeToLive").n());

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertDocumentSyncs(siteId, item.getDocumentId());
    }
  }

  /**
   * Create Document Request with COMPLETED ACTIONS.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest11() throws Exception {

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      doc.setUserId("joe");
      doc.setPath("test.txt");
      service.saveDocumentItemWithTag(siteId, doc);
      actionsService.saveNewActions(siteId, doc.getDocumentId(), Collections.singletonList(
          new Action().type(ActionType.OCR).userId("joe").status(ActionStatus.COMPLETE)));

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
  public void testHandleRequest12() throws Exception {

    createMockServer(DocumentsS3Update.SERVER_ERROR);
    before();

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
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
  public void testHandleRequest13() throws Exception {

    createMockServer(OK);
    before();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
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
  public void testHandleRequest14() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
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
      actionsService.saveNewActions(siteId, doc.getDocumentId(), Collections.singletonList(
          new Action().type(ActionType.OCR).userId("joe").status(ActionStatus.RUNNING)));

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
   * Invalid Request.
   */
  @Test
  public void testInvalidRequest01() {
    // given
    Map<String, Object> map = new HashMap<>();

    // when
    handleRequest(null, BUCKET_KEY, map);

    // then
  }

  /**
   * Verify {@link DocumentItem}.
   *
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @param contentType {@link String}
   * @param contentLength {@link String}
   */
  private void verifyDocumentSaved(final String siteId, final DocumentItem item,
      final String contentType, final String contentLength) {

    assertEquals(contentType, item.getContentType());
    assertEquals(contentLength, item.getContentLength().toString());

    s3service.deleteAllObjectTags("example-bucket", item.getDocumentId());
    service.deleteDocumentTags(siteId, item.getDocumentId());
  }
}
