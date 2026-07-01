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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
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
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.base64.MapToBase64;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.aws.dynamodb.model.DocumentTagRecordBuilder;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsServiceImpl;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.dynamodb.actions.ActionBuilder;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.testutils.aws.TestEnvironment;
import com.formkiq.testutils.aws.s3.S3EventJsonBuilder;
import com.formkiq.testutils.aws.sqs.SqsMessageReceiver;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
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
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.sqs.model.Message;

/** {@link DocumentsS3Update} Unit Tests. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsS3UpdateTest implements DbKeys {

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
  /** {@link CacheService}. */
  private static CacheService cacheService;
  /**
   * {@link DocumentService}.
   */
  private static DocumentServiceImpl service;
  /** {@link Gson}. */
  private static final Gson GSON = GsonUtil.getInstance();

  /**
   * Test server URL.
   */
  private static final String URL = "http://localhost:" + PORT;
  /** {@link SqsMessageReceiver}. */
  private static SqsMessageReceiver eventQueue;

  /**
   * After All.
   */
  @AfterAll
  public static void afterAll() {
    db.close();
  }

  private static void assertCreateDocumentSnsMessage(final String siteId, final String eventType)
      throws InterruptedException {
    assertCreateDocumentSnsMessage(siteId, eventType, 1);
  }

  private static void assertCreateDocumentSnsMessage(final String siteId, final String eventType,
      final int expected) throws InterruptedException {
    List<Message> messages = eventQueue.get(List.of("\\\"type\\\":\\\"" + eventType + "\\\""));
    assertEquals(expected, messages.size());
    Message m = messages.getFirst();

    Map<String, String> map = GSON.fromJson(m.body(), Map.class);
    String message = map.get("Message");

    map = GSON.fromJson(message, Map.class);
    assertNotNull(map.get("documentId"));
    assertEquals(eventType, map.get("type"));

    assertNull(map.get("content"));

    if (!"delete".equals(eventType) && !"softDelete".equals(eventType)) {
      assertTrue(map.get("url").contains("example-bucket"));
      assertNotNull(map.get("userId"));
      assertNotNull(map.get("path"));
    }

    assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID), map.get("siteId"));
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

    S3ConnectionBuilder s3Builder = TestServices.getS3Connection(null);
    s3service = new S3Service(s3Builder);
    s3service.createBucket("example-bucket");

    SnsConnectionBuilder snsBuilder = TestServices.getSnsConnection(null);
    SnsService snsService = new SnsServiceImpl(snsBuilder);

    final String snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();

    dbHelper = new DynamoDbHelper(dbBuilder);

    try (DynamoDbClient dbClient = dbBuilder.build()) {
      DocumentSchema schema = new DocumentSchema(dbClient);
      schema.createDocumentsTable(DOCUMENTS_TABLE);
      schema.createCacheTable(CACHE_TABLE);
    }

    Map<String, String> map = new HashMap<>();
    map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    map.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    map.put("CACHE_TABLE", CACHE_TABLE);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    map.put("AWS_REGION", AWS_REGION.id());
    map.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    map.put("OPERATIONAL_MODE", "ACTIVE");
    map.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    var credentialsProvider = TestEnvironment.createCredentials();

    var awsServices =
        new AwsServiceCacheBuilder(map, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(),
                new SsmAwsServiceRegistry())
            .build();

    awsServices.register(ActionsService.class, new ActionsServiceExtension());
    awsServices.register(DocumentService.class, new DocumentServiceExtension());
    awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServices.register(S3Service.class, new S3ServiceExtension());

    service = (DocumentServiceImpl) awsServices.getExtension(DocumentService.class);
    actionsService = awsServices.getExtension(ActionsService.class);

    handler = new DocumentsS3Update(awsServices);
    awsServices.register(SqsService.class, new SqsServiceExtension());
    cacheService = awsServices.getExtension(CacheService.class);

    SsmService ssmService = awsServices.getExtension(SsmService.class);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    String eventSnsQueue = TestServices.createSqsSubscriptionToSnsTopic(snsDocumentEvent);
    eventQueue = new SqsMessageReceiver(awsServices, eventSnsQueue);
  }

  private static ActionBuilder createAction(final DocumentArtifact document) {
    return new ActionBuilder().document(document).type(ActionType.OCR).indexUlid().userId("joe");
  }

  private static Map<String, Object> createS3Map(final String siteId, final DocumentRecordSet doc) {
    return createS3Map(siteId, doc, "example-bucket");
  }

  private static Map<String, Object> createS3Map(final String siteId, final DocumentRecordSet doc,
      final String bucket) {
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, doc.documentRecord().documentId(),
        doc.documentRecord().artifactId());
    return new S3EventJsonBuilder()
        .addRecord(new S3EventJsonBuilder.RecordBuilder().withEventName("ObjectCreated:Put")
            .withS3(new S3EventJsonBuilder.S3Builder().withBucket(bucket).withObject(s3Key)))
        .build();
  }

  /** {@link ClientAndServer}. */
  private ClientAndServer mockServer;

  private void addS3File(final String bucket, final String key, final String contentType,
      final boolean addTags, final String content) {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", contentType);

    s3service.putObject(bucket, key, content.getBytes(StandardCharsets.UTF_8), contentType,
        metadata);

    if (addTags) {
      s3service.setObjectTags(bucket, key,
          Arrays.asList(Tag.builder().key("sample").value("12345").build(),
              Tag.builder().key("CLAMAV_SCAN_STATUS").value("GOOD").build()));
    }
  }

  private void addS3File(final String key, final String contentType, final boolean addTags,
      final String content) {
    addS3File("example-bucket", key, contentType, addTags, content);
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

  private void assertHandleContentType(final String path, final String contentType,
      final String s3ContenType, final String expectedContentType) throws ValidationException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      String key = createDatabaseKey(siteId, documentId);

      DocumentRecordSet doc = createDocument(siteId, documentId, path, contentType);
      addS3File(key, s3ContenType, false, "testdata");

      Map<String, Object> map = createS3Map(siteId, doc);

      // when
      DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      assertEquals(expectedContentType, item.contentType());
    }
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

    eventQueue.clear();
  }

  private Date createDate2DaysAgo() {
    ZoneId defaultZoneId = ZoneId.systemDefault();
    LocalDate localDate = LocalDate.now().minusDays(2);
    return Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());
  }

  private DocumentRecordSet createDocument(final String siteId, final String documentId,
      final String path, final String contentType) throws ValidationException {
    DocumentRecord documentRecord = DocumentRecord.builder().path(path).contentType(contentType)
        .userId("joe").documentId(documentId).build(siteId);
    DocumentRecordSet documentRecordSet = new DocumentRecordSet(documentRecord, null, null, null);
    service.saveDocument(siteId, documentRecordSet,
        new SaveDocumentOptions().saveDocumentDate(true));
    return documentRecordSet;
  }

  /**
   * Create Mock Server.
   */
  private void createMockServer() {

    this.mockServer = startClientAndServer(PORT);

    ExpectationStatusResponseCallback callback =
        new ExpectationStatusResponseCallback(DocumentsS3UpdateTest.OK);
    this.mockServer.when(request().withMethod("DELETE")).respond(callback);
  }

  /**
   * Create {@link DynamicDocumentItem} with Child Documents.
   *
   * @param now {@link Date}
   * @return {@link DynamicDocumentItem}
   */
  private DocumentRecordSet createSubDocuments(final Date now) {
    String username = UUID.randomUUID() + "@formkiq.com";

    DocumentRecord documentRecord = DocumentRecord.builder().documentId(ID.uuid()).userId(username)
        .insertedDate(now).contentType("text/plain").build((String) null);
    List<DocumentTagRecord> tags = DocumentTagRecord.builder()
        .documentId(documentRecord.documentId()).tagKey("category").tagValue("none")
        .userId(username).type(DocumentTagType.USERDEFINED).build((String) null);

    DocumentRecord documentRecord1 = DocumentRecord.builder().documentId(ID.uuid()).userId(username)
        .insertedDate(now).contentType("text/html").build((String) null);
    List<DocumentTagRecord> tags1 =
        DocumentTagRecord.builder().documentId(documentRecord1.documentId()).tagKey("category1")
            .userId(username).type(DocumentTagType.USERDEFINED).build((String) null);
    DocumentRecordSet doc1 = new DocumentRecordSet(documentRecord1, null, tags1, null);

    DocumentRecord documentRecord2 = DocumentRecord.builder().documentId(ID.uuid()).userId(username)
        .insertedDate(now).contentType("application/json").build((String) null);
    List<DocumentTagRecord> tags2 =
        DocumentTagRecord.builder().documentId(documentRecord1.documentId()).tagKey("category2")
            .userId(username).type(DocumentTagType.USERDEFINED).build((String) null);
    DocumentRecordSet doc2 = new DocumentRecordSet(documentRecord2, null, tags2, null);

    return new DocumentRecordSet(documentRecord, null, tags, List.of(doc1, doc2));
  }

  /**
   * Handle Request.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param map {@link Map}
   * @return {@link DocumentItem}
   */
  private DocumentRecord handleRequest(final String siteId, final String documentId,
      final Map<String, Object> map) {

    // when
    handler.handleRequest(map, null);

    // then
    login();
    return service.findDocument(siteId, DocumentArtifact.of(documentId, null));
  }

  private void login() {
    ApiAuthorization authorization = new ApiAuthorization().username("firstwriter");
    ApiAuthorization.login(authorization);
  }

  /**
   * Create Document Request without existing Tags/Formats.
   *
   */
  @Test
  public void testHandleRequest01() throws InterruptedException {

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, BUCKET_KEY);

      DocumentRecordSet doc = createDocument(siteId, BUCKET_KEY, "test.txt", null);

      String content = "testdata";
      addS3File(key, "text/plain", false, content);

      Map<String, Object> map = createS3Map(siteId, doc);

      // when
      DocumentRecord item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertEquals("text/plain", item.contentType());
      assertEquals(content.length(), item.contentLength());
      assertNotNull(item.s3version());
      assertEquals("joe", item.userId());

      assertFalse(item.checksum().startsWith("\""));
      assertFalse(item.checksum().endsWith("\""));
      assertNotNull(item.s3version());

      Pagination<DocumentTag> tags = service.findDocumentTags(siteId,
          DocumentArtifact.of(BUCKET_KEY, null), null, MAX_RESULTS);

      assertEquals(0, tags.getResults().size());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item, "text/plain", "8");

      assertNotNull(service.findMostDocumentDate());

      assertCreateDocumentSnsMessage(siteId, "create");
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

      DocumentRecord documentRecord = new DocumentRecordBuilder().insertedDate(date)
          .documentId(documentId).userId("asd").path("test.txt").checksum("ASD").build(siteId);

      Collection<DocumentTagRecord> addTags = new DocumentTagRecordBuilder().documentId(documentId)
          .tagKey("person").tagValue("category").userId("asd").build(siteId);
      DocumentRecordSet documentRecordSet =
          new DocumentRecordSet(documentRecord, null, addTags, null);
      service.saveDocument(siteId, documentRecordSet,
          new SaveDocumentOptions().saveDocumentDate(true));

      addS3File(key, "pdf", true, "testdata");

      TimeUnit.SECONDS.sleep(1);

      // when
      final DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      assertEquals(item.insertedDate(), item.lastModifiedDate());

      Pagination<DocumentTag> tags = service.findDocumentTags(siteId,
          DocumentArtifact.of(documentId, null), null, MAX_RESULTS);

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
      assertNotNull(service.findMostDocumentDate());

      assertCreateDocumentSnsMessage(siteId, "update");
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

      DocumentRecord documentRecord = DocumentRecord.builder().path("test.txt").userId("asd")
          .documentId(BUCKET_KEY).build(siteId);

      Collection<DocumentTagRecord> tags = new DocumentTagRecordBuilder().documentId(BUCKET_KEY)
          .tagKey("person").tagValue("category").userId("asd").build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, tags, null),
          new SaveDocumentOptions());

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentRecord item = handleRequest(siteId, BUCKET_KEY, map);

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

      DocumentRecord documentRecord = DocumentRecord.builder().path("test.txt").userId("asd")
          .documentId(documentId).build(siteId);

      Collection<DocumentTagRecord> tags = new DocumentTagRecordBuilder().documentId(documentId)
          .tagKey("person").tagValue("category").userId("asd").build(siteId);

      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, tags, null),
          new SaveDocumentOptions());

      // when
      DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      assertNull(item);
      assertCreateDocumentSnsMessage(siteId, "delete");
    }
  }

  /**
   * Create Document Request on child document.
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest05() throws Exception {

    // given
    String documentId = ID.uuid();
    String childDocumentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String key = createDatabaseKey(siteId, childDocumentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DocumentRecord documentRecord = DocumentRecord.builder().path("test.txt").userId("joe")
          .documentId(documentId).build(siteId);

      DocumentRecord childDoc = DocumentRecord.builder().userId("joe")
          .belongsToDocumentId(documentId).documentId(childDocumentId).build(siteId);
      DocumentRecordSet child = new DocumentRecordSet(childDoc, null, null, null);

      service.saveDocument(siteId,
          new DocumentRecordSet(documentRecord, null, null, List.of(child)),
          new SaveDocumentOptions().saveDocumentDate(true));

      addS3File(key, "pdf", false, "testdata");

      // when
      final DocumentRecord item = handleRequest(siteId, childDoc.documentId(), map);

      // then
      DocumentArtifact childDocument = new DocumentArtifact(childDocumentId, null);
      assertNotNull(item.belongsToDocumentId());
      Pagination<DocumentTag> tags =
          service.findDocumentTags(siteId, childDocument, null, MAX_RESULTS);

      try (DynamoDbClient client = dbBuilder.build()) {
        Map<String, AttributeValue> m =
            client.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
                .key(keysDocument(siteId, childDoc.documentId())).build()).item();
        assertNull(m.get(GSI1_PK));
      }

      assertEquals(0, tags.getResults().size());

      assertEquals(0, service.findDocumentFormats(siteId, childDocumentId, null, MAX_RESULTS)
          .getResults().size());
      verifyDocumentSaved(siteId, item, "pdf", "8");

      tags = service.findDocumentTags(siteId, childDocument, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());

      assertNotNull(service.findMostDocumentDate());
      assertCreateDocumentSnsMessage(siteId, "create");
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
      DocumentRecordSet doc = createSubDocuments(now);
      String documentId = doc.documentRecord().documentId();
      final DocumentArtifact document = new DocumentArtifact(documentId, null);
      service.saveDocument(siteId, doc, new SaveDocumentOptions().saveDocumentDate(true));

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentRecord item = handleRequest(siteId, documentId, map);
      DocumentRecord itemchild = handleRequest(siteId,
          doc.children().iterator().next().documentRecord().documentId(), map);

      // then
      Map<String, AttributeValue> m = db.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
          .key(keysDocument(siteId, documentId)).build()).item();
      assertNotNull(m.get(GSI1_PK));

      Map<String, AttributeValue> mchild =
          db.getItem(GetItemRequest.builder().tableName(DOCUMENTS_TABLE)
              .key(keysDocument(siteId, itemchild.documentId())).build()).item();
      assertNull(mchild.get(GSI1_PK));

      assertEquals(documentId, item.documentId());
      Pagination<DocumentTag> tags = service.findDocumentTags(siteId, document, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("category").setValue("none")
          .setType(DocumentTagType.USERDEFINED).setDocumentId(documentId),
          tags.getResults().getFirst());

      DocumentArtifact childDocument = new DocumentArtifact(itemchild.documentId(), null);
      tags = service.findDocumentTags(siteId, childDocument, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertDocumentTagEquals(new DocumentTag().setKey("category1").setValue("")
          .setType(DocumentTagType.USERDEFINED).setDocumentId(itemchild.documentId()),
          tags.getResults().getFirst());

      assertCreateDocumentSnsMessage(siteId, "create", 2);
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

      createDocument(siteId, documentId, "test.txt", null);

      addS3File(key, "text/plain", false, "testdata");

      // when
      final DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "8");
      assertCreateDocumentSnsMessage(siteId, "create");
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

      createDocument(siteId, documentId, "test.txt", null);

      String content = loadFile(this, "/256kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertCreateDocumentSnsMessage(siteId, "create");
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

      createDocument(siteId, documentId, "test.txt", null);

      String content = loadFile(this, "/255kb-text.txt");
      addS3File(key, "text/plain", false, content);

      // when
      final DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      verifyDocumentSaved(siteId, item, "text/plain", "" + content.length());
      assertCreateDocumentSnsMessage(siteId, "create");
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

      DocumentRecord documentRecord = DocumentRecord.builder().documentId(documentId).userId("joe")
          .path("test.txt").timeToLive(ttl).build(siteId);
      DocumentRecordSet doc = new DocumentRecordSet(documentRecord, null, null, null);
      service.saveDocument(siteId, doc, new SaveDocumentOptions());

      addS3File(key, "pdf", false, "testdata");

      // when
      DocumentRecord item = handleRequest(siteId, documentId, map);

      // then
      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.documentId()))
          .tableName(DOCUMENTS_TABLE).build();

      Map<String, AttributeValue> result = db.getItem(r).item();
      assertEquals(ttl, result.get("TimeToLive").n());

      verifyDocumentSaved(siteId, item, "pdf", "8");
      assertCreateDocumentSnsMessage(siteId, "create");
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

      DocumentRecordSet doc = createDocument(siteId, BUCKET_KEY, "test.txt", null);
      DocumentArtifact document = new DocumentArtifact(doc.documentRecord().documentId(), null);
      actionsService.saveNewActions(
          List.of(createAction(document).status(ActionStatus.COMPLETE).build(siteId)));

      addS3File(key, "pdf", false, "testdata");

      // when
      handleRequest(siteId, BUCKET_KEY, map);

      // then
      List<Action> actions = actionsService.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.COMPLETE, actions.getFirst().status());
      assertEquals(ActionType.OCR, actions.getFirst().type());

      assertCreateDocumentSnsMessage(siteId, "create");
    }
  }

  /**
   * Delete Document Request - OCR / Fulltext (200).
   *
   * @throws Exception Exception
   */
  @Test
  public void testHandleRequest13() throws Exception {

    createMockServer();
    before();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DocumentRecord documentRecord = DocumentRecord.builder().documentId(BUCKET_KEY).build(siteId);
      service.saveDocument(siteId, new DocumentRecordSet(documentRecord, null, null, null),
          new SaveDocumentOptions());

      // when
      DocumentRecord item = handleRequest(siteId, BUCKET_KEY, map);

      // then
      assertNull(item);
      assertCreateDocumentSnsMessage(siteId, "delete");
    }
  }

  /**
   * Create Document Request with 'running' ACTIONS.
   *
   */
  @Test
  public void testHandleRequest14() throws InterruptedException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      String key = createDatabaseKey(siteId, documentId);

      DocumentRecordSet doc = createDocument(siteId, documentId, "test.txt", null);
      DocumentArtifact document = new DocumentArtifact(documentId, null);
      actionsService.saveNewActions(
          List.of(createAction(document).status(ActionStatus.RUNNING).build(siteId)));

      addS3File(key, "pdf", false, "testdata");

      Map<String, Object> map = createS3Map(siteId, doc);

      // when
      handleRequest(siteId, documentId, map);

      // then
      List<Action> actions = actionsService.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.RUNNING, actions.getFirst().status());
      assertEquals(ActionType.OCR, actions.getFirst().type());
      assertCreateDocumentSnsMessage(siteId, "create");
    }
  }

  /**
   * Create Document Request without contentType.
   *
   */
  @Test
  public void testHandleRequest15() {
    assertHandleContentType(ID.uuid(), null, null, "application/octet-stream");
    assertHandleContentType(ID.uuid(), null, "application/pdf", "application/pdf");
    assertHandleContentType(ID.uuid(), "application/pdf", null, "application/pdf");
    assertHandleContentType("test.pdf", null, null, "application/pdf");
    assertHandleContentType("test.txt", "text/plain", "binary/octet-stream", "text/plain");
    assertHandleContentType("test.txt", null, "binary/octet-stream", "text/plain");
  }

  @Test
  public void testHandleRequestSetsGovernanceObjectLockFromPresignedUrlCache()
      throws ValidationException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String bucket = "object-lock-" + ID.uuid();
      s3service.createBucket(bucket, true);

      String documentId = ID.uuid();
      String key = createDatabaseKey(siteId, documentId);
      DocumentRecordSet doc = createDocument(siteId, documentId, "test.txt", null);
      addS3File(bucket, key, "text/plain", false, "testdata");

      String retainUntilDate =
          DateUtil.getIsoDateFormatter().format(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));
      cacheService.write("s3PresignedUrl#" + bucket + "#" + key,
          new MapToBase64().apply(Map.of("username", "joe", "objectLockRetentionMode", "GOVERNANCE",
              "objectLockRetainUntilDate", retainUntilDate)),
          7);

      // when
      handler.handleRequest(createS3Map(siteId, doc, bucket), null);

      // then
      GetObjectRetentionResponse retention = s3service.getObjectRetention(bucket, key, null);
      assertNotNull(retention.retention());
      assertEquals(ObjectLockRetentionMode.GOVERNANCE, retention.retention().mode());
      assertEquals(retainUntilDate, DateUtil.getIsoDateFormatter()
          .format(Date.from(retention.retention().retainUntilDate())));
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
  private void verifyDocumentSaved(final String siteId, final DocumentRecord item,
      final String contentType, final String contentLength) {

    assertEquals(contentType, item.contentType());
    assertEquals(contentLength, item.contentLength().toString());

    s3service.deleteAllObjectTags("example-bucket", item.documentId());
    service.deleteDocumentTags(siteId, DocumentArtifact.of(item.documentId(), null));
  }
}
