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
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.model.DocumentSyncServiceType.FORMKIQ_CLI;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.StagingS3Create.FORMKIQ_B64_EXT;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
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
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentSync;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3ObjectMetadata;
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
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexProcessorImpl;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.stacks.lambda.s3.util.LambdaLoggerRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** Unit Tests for {@link StagingS3Create}. */
@ExtendWith(DynamoDbExtension.class)
public class StagingS3CreateTest implements DbKeys {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbBuilder;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** Documents S3 Bucket. */
  private static final String DOCUMENTS_BUCKET = "documentsbucket";
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor folderIndexProcesor;
  /** {@link Gson}. */
  private static Gson gson =
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
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Builder;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate";
  /** SQS Sns Queue. */
  private static final String SNS_SQS_DELETE_QUEUE = "sqssnsDelete";
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
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
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmBuilder;
  /** Document S3 Staging Bucket. */
  private static final String STAGING_BUCKET = "example-bucket";
  /** {@link DocumentSyncService}. */
  private static DocumentSyncService syncService;

  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;

  /** UUID 1. */
  private static final String UUID1 = "b53c92cf-f7b9-4787-9541-76574ec70d71";

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
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws URISyntaxException, InterruptedException, IOException {

    s3Builder = TestServices.getS3Connection(null);
    ssmBuilder = TestServices.getSsmConnection(null);
    dbBuilder = DynamoDbTestServices.getDynamoDbConnection();
    dbHelper = DynamoDbTestServices.getDynamoDbHelper(null);
    snsBuilder = TestServices.getSnsConnection(null);

    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    service = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());

    s3 = new S3Service(s3Builder);
    syncService = new DocumentSyncServiceDynamoDb(dbBuilder, DOCUMENT_SYNCS_TABLE);

    SqsConnectionBuilder sqsBuilder = TestServices.getSqsConnection(null);
    sqsService = new SqsService(sqsBuilder);

    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    folderIndexProcesor = new FolderIndexProcessorImpl(dbBuilder, DOCUMENTS_TABLE);

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    snsService = new SnsService(snsBuilder);
    snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();
    snsService.subscribe(snsDocumentEvent, "sqs", sqsDocumentEventUrl);

    createResources();

    createMockServer();
  }

  /**
   * Create Mock Server.
   */
  private static void createMockServer() {

    mockServer = startClientAndServer(Integer.valueOf(PORT));

    final String documentId = "12345";
    mockServer.when(request().withMethod("POST").withPath("/documents/" + documentId + "/tags"))
        .respond(new ExpectationResponseCallback() {
          @SuppressWarnings("unchecked")
          @Override
          public HttpResponse handle(final HttpRequest httpRequest) throws Exception {

            Map<String, Object> map = gson.fromJson(httpRequest.getBodyAsString(), Map.class);
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
                Arrays.asList(new DocumentTag(documentId, "test", "novalue", new Date(), "joe"));
            service.addTags(siteId, documentId, tags, null);

            return org.mockserver.model.HttpResponse.response("{}");
          }
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

    snsCreateTopic = snsService.createTopic("createDocument").topicArn();
    snsService.subscribe(snsCreateTopic, "sqs",
        "arn:aws:sqs:us-east-1:000000000000:" + SNS_SQS_CREATE_QUEUE);

    if (!dbHelper.isTableExists(DOCUMENTS_TABLE)) {
      dbHelper.createDocumentsTable(DOCUMENTS_TABLE);
      dbHelper.createCacheTable(CACHE_TABLE);
    }
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /** Environment Map. */
  private Map<String, String> env = new HashMap<>();

  /** {@link LambdaLoggerRecorder}. */
  private LambdaLoggerRecorder logger;

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
  public void before() {

    this.env = new HashMap<>();
    this.env.put("AWS_REGION", Region.US_EAST_1.id());
    this.env.put("DOCUMENTS_S3_BUCKET", DOCUMENTS_BUCKET);
    this.env.put("SNS_DELETE_TOPIC", snsDeleteTopic);
    this.env.put("SNS_CREATE_TOPIC", snsCreateTopic);
    this.env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    this.env.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    this.env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    this.env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    this.env.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    this.env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    this.context = new LambdaContextRecorder();
    this.logger = (LambdaLoggerRecorder) this.context.getLogger();

    dbHelper.truncateTable(DOCUMENTS_TABLE);

    s3.deleteAllFiles(STAGING_BUCKET);
    s3.deleteAllFiles(DOCUMENTS_BUCKET);

    for (String queue : Arrays.asList(snsSqsCreateQueueUrl, snsSqsDeleteQueueUrl)) {
      ReceiveMessageResponse response = sqsService.receiveMessages(queue);
      while (response.messages().size() > 0) {
        for (Message msg : response.messages()) {
          sqsService.deleteMessage(queue, msg.receiptHandle());
        }

        response = sqsService.receiveMessages(queue);
      }
    }
  }

  /**
   * Create {@link DynamicDocumentItem}.
   * 
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createDocumentItem() {
    String content = "This is a test";

    DynamicDocumentItem item = new DynamicDocumentItem(Collections.emptyMap());
    item.setDocumentId(UUID.randomUUID().toString());
    item.put("content", Base64.getEncoder().encodeToString(content.getBytes(UTF_8)));
    item.setContentLength(Long.valueOf(content.length()));
    item.setContentType("plain/text");
    item.setInsertedDate(new Date());
    item.setPath("test.txt");
    item.setUserId("joe");

    return item;
  }

  /**
   * Find DocumentId from Logger.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  private String findDocumentIdFromLogger(final String siteId) {

    Optional<String> uuid = Optional.empty();

    List<String> msgs = this.logger.getRecordedMessages();

    Optional<String> copy =
        msgs.stream().filter(s -> s.startsWith("Copying") || s.startsWith("Inserted")).findFirst();

    if (copy.isPresent()) {

      uuid = Arrays.asList(copy.get().split(" ")).stream().map(s -> resetDatabaseKey(siteId, s))
          .filter(s -> {
            try {
              int pos = s.indexOf("/");
              UUID.fromString(pos > 0 ? s.substring(pos + 1) : s);
              return true;
            } catch (IllegalArgumentException e) {
              return false;
            }
          }).findFirst();
    }

    return uuid.isPresent() ? uuid.get() : null;
  }

  /**
   * Find {@link DocumentTag}.
   * 
   * @param tags {@link List} {@link DocumentTag}
   * @param key {@link String}
   * @return {@link DocumentTag}
   */
  private DocumentTag findTag(final List<DocumentTag> tags, final String key) {
    return tags.stream().filter(s -> s.getKey().equals(key)).findFirst().get();
  }

  /**
   * Handle {@link StagingS3Create} request.
   *
   * @param map {@link Map}
   */
  private void handleRequest(final Map<String, Object> map) {
    final StagingS3Create handler =
        new StagingS3Create(this.env, null, dbBuilder, s3Builder, ssmBuilder, snsBuilder);

    Void result = handler.handleRequest(map, this.context);
    assertNull(result);
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
    String documentId = UUID.randomUUID().toString();
    this.logger.reset();

    String key = createDatabaseKey(siteId, documentId + FORMKIQ_B64_EXT);

    byte[] content = gson.toJson(docitem).getBytes(UTF_8);
    s3.putObject(STAGING_BUCKET, key, content, null, null);

    // when
    handleRequest(loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key));

    // then
    String destDocumentId = findDocumentIdFromLogger(siteId);

    if (destDocumentId != null) {
      assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));
      assertTrue(this.logger.containsString("Inserted " + docitem.getPath()
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
      int tagcount = hasTags ? docitem.getList("tags").size() : 1;
      assertEquals(tagcount, tags.size());
      // assertEquals(1, tags.size());

      // DocumentTag ptag = findTag(tags, "path");
      // assertEquals(destDocumentId, ptag.getDocumentId());
      // assertEquals(docitem.getPath(), ptag.getValue());
      // assertNotNull(ptag.getInsertedDate());
      // assertEquals(DocumentTagType.SYSTEMDEFINED, ptag.getType());
      // assertEquals(docitem.getUserId(), ptag.getUserId());

      if (hasTags) {

        for (DynamicObject tag : docitem.getList("tags")) {
          DocumentTag dtag = findTag(tags, tag.getString("key"));
          assertEquals(tag.getString("value"), dtag.getValue());
        }

      } else {

        DocumentTag ptag = findTag(tags, "untagged");
        assertEquals(destDocumentId, ptag.getDocumentId());
        assertEquals("true", ptag.getValue());
        assertNotNull(ptag.getInsertedDate());
        assertEquals(DocumentTagType.SYSTEMDEFINED, ptag.getType());
        assertEquals(docitem.getUserId(), ptag.getUserId());
      }
    }
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
    this.logger.reset();

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

    assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));
    assertTrue(this.logger.containsString("handling 1 record(s)."));
    assertTrue(this.logger.containsString("Copying " + key + " from bucket example-bucket to "
        + createDatabaseKey(siteId, destDocumentId) + " in bucket " + DOCUMENTS_BUCKET + "."));
    assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));

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

    assertEquals(1, tags.size());

    assertEquals("untagged", tags.get(0).getKey());
    assertEquals(DocumentTagType.SYSTEMDEFINED, tags.get(0).getType());

    verifyS3Metadata(siteId, item);
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  public void testCopyFile01() throws Exception {
    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();
      testCopyFile(siteId, documentId, documentId);
    }
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  public void testCopyFile02() throws Exception {
    int i = 0;
    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
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
  public void testCopyFile03() throws Exception {
    final String documentId = "something/where/test.pdf";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      testCopyFile(siteId, documentId, "something/where/test.pdf");
    }
  }

  /**
   * Test .fkb64 file with TAGS.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension01() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    item.put("tags",
        Arrays.asList(Map.of("documentId", item.getDocumentId(), "key", "category", "value",
            "person", "insertedDate", new Date(), "userId", "joe", "type",
            DocumentTagType.USERDEFINED.name())));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file without TAGS.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension02() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file without CONTENT.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension03() throws IOException {
    DynamicDocumentItem item = createDocumentItem();
    item.put("content", null);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item, null);
    }

    assertTrue(this.logger.containsString("Skipping " + item.getPath() + " no content"));
  }

  /**
   * Test .fkb64 file without DocumentId.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension04() throws IOException {
    DynamicDocumentItem item = createDocumentItem();
    item.setDocumentId(null);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item, null);
    }
  }

  /**
   * Test .fkb64 file with multiple documents.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension05() throws IOException {
    String documentId0 = "0d1a788d-9a70-418a-8c33-a9ee9c1a0173";
    String documentId1 = "24af57ca-f61d-4ff8-b8a0-d7666073560e";
    String documentId2 = "f2416702-6b3c-4d29-a217-82a43b16b964";

    ZonedDateTime nowDate = ZonedDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
      verifyBelongsToDocument(i, documentId0, Arrays.asList(documentId1, documentId2));

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
  public void testFkB64Extension06() throws IOException {
    String timeToLive = "1612061365";
    DynamicDocumentItem item = createDocumentItem();
    item.put("TimeToLive", timeToLive);

    item.put("tags",
        Arrays.asList(Map.of("documentId", item.getDocumentId(), "key", "category", "value",
            "person", "insertedDate", new Date(), "userId", "joe", "type",
            DocumentTagType.USERDEFINED.name())));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item, null);

      String key = createDatabaseKey(siteId, item.getDocumentId());
      String content = s3.getContentAsString(DOCUMENTS_BUCKET, key, null);
      assertEquals("VGhpcyBpcyBhIHRlc3Q=", content);

      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.getDocumentId()))
          .tableName(DOCUMENTS_TABLE).build();

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
  public void testFkB64Extension07() throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.logger.reset();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = gson.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      String documentId = findDocumentIdFromLogger(siteId);
      assertEquals(documentId, UUID.fromString(documentId).toString());
      assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));

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

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "status", "values",
          Arrays.asList("active", "notactive"), "type", "USERDEFINED", "userId", "joesmith"));
    }
  }

  /**
   * Test .fkb64 file with tagschema & composite key.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension08() throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("tagSchemaId", UUID.randomUUID().toString());
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.TRUE);
    data.put("newCompositeTags", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.logger.reset();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = gson.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      String documentId = findDocumentIdFromLogger(siteId);
      assertEquals(documentId, UUID.fromString(documentId).toString());
      assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));

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

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "status", "values",
          Arrays.asList("active", "notactive"), "type", "USERDEFINED", "userId", "joesmith"));
    }
  }

  /**
   * Test .fkb64 file with tagschema & without composite key.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension09() throws IOException {
    final String documentId = "12345";
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("tagSchemaId", UUID.randomUUID().toString());
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document"),
        Map.of("key", "status", "values", Arrays.asList("active", "notactive"))));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.logger.reset();

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = gson.toJson(ditem).getBytes(UTF_8);
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

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "test", "value",
          "novalue", "type", "USERDEFINED", "userId", "joe"));
    }
  }

  /**
   * Test .fkb64 file with actions.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testFkB64Extension10() throws IOException {

    final String documentId = "12345";
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", "joesmith");
    data.put("contentType", "text/plain");
    data.put("tagSchemaId", UUID.randomUUID().toString());
    data.put("isBase64", Boolean.TRUE);
    data.put("content", "dGhpcyBpcyBhIHRlc3Q=");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.logger.reset();

      data.put("actions",
          Arrays.asList(Map.of("type", "ocr", "status", "PENDING", "userId", "joesmith",
              "parameters", Map.of("test", "1234")),
              Map.of("type", "webhook", "userId", "joesmith")));

      DynamicDocumentItem ditem = new DynamicDocumentItem(data);

      String key = createDatabaseKey(siteId, "documentId" + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = gson.toJson(ditem).getBytes(UTF_8);
      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      assertFalse(s3.getObjectMetadata(STAGING_BUCKET, documentId, null).isObjectExists());

      List<Action> actions = actionsService.getActions(siteId, documentId);
      assertEquals(2, actions.size());
      assertEquals("OCR", actions.get(0).type().name());
      assertEquals("PENDING", actions.get(0).status().name());
      assertEquals("joesmith", actions.get(0).userId());

      assertEquals("WEBHOOK", actions.get(1).type().name());
      assertEquals("PENDING", actions.get(1).status().name());
      assertEquals("joesmith", actions.get(1).userId());

      actions.get(0).status(ActionStatus.COMPLETE);
      actionsService.saveActions(siteId, documentId, actions);

      // given
      data.put("actions", Arrays.asList(Map.of("type", "ocr", "status", "PENDING", "userId",
          "joesmith", "parameters", Map.of("test", "1234"))));
      ditem = new DynamicDocumentItem(data);
      content = gson.toJson(ditem).getBytes(UTF_8);

      s3.putObject(STAGING_BUCKET, key, content, null, null);

      // when - run a 2nd time
      handleRequest(map);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals("OCR", actions.get(0).type().name());
      assertEquals("PENDING", actions.get(0).status().name());
      assertEquals("joesmith", actions.get(0).userId());
    }
  }

  /**
   * Test .fkb64 files with existing path and CLI agent.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  @Test
  public void testFkB64Extension11() throws IOException, InterruptedException {

    String path = "sample/test.txt";
    Map<String, Object> data = new HashMap<>();
    data.put("userId", "joesmith");
    data.put("path", path);
    data.put("agent", DocumentSyncServiceType.FORMKIQ_CLI.name());
    data.put("contentType", "text/plain");
    data.put("isBase64", Boolean.FALSE);
    data.put("content", "this is first data");

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.logger.reset();

      String key = createDatabaseKey(siteId, path + FORMKIQ_B64_EXT);
      if (siteId == null) {
        key = "default/" + key;
      }

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      // when - send the same path twice
      for (int i = 0; i < 2; i++) {
        ditem.put("content", "this is some content: " + i);
        byte[] content = gson.toJson(ditem).getBytes(UTF_8);
        s3.putObject(STAGING_BUCKET, key, content, null, null);

        handleRequest(map);
        TimeUnit.SECONDS.sleep(1);
      }

      // then
      Map<String, String> index = folderIndexProcesor.getIndex(siteId, path);

      String documentId = index.get("documentId");
      DocumentItem item = service.findDocument(siteId, documentId);
      assertNull(item.getContentLength());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getChecksum());
      assertNotNull(item.getInsertedDate());
      assertEquals(path, item.getPath());
      assertEquals("joesmith", item.getUserId());

      int i = 0;
      final int count = 1;
      List<DocumentTag> tags =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults();
      assertEquals(count, tags.size());

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "untagged", "value",
          "true", "type", "SYSTEMDEFINED", "userId", "joesmith"));

      verifyCliSyncs(siteId, documentId);
    }
  }

  /**
   * Test add tags to existing Document with .fkb64 file and NO content and FULLTEXT action.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFkB64Extension12() throws Exception {
    final Date now = new Date();
    final String userId = "joesmith";
    final long contentLength = 1000;
    String documentId = UUID.randomUUID().toString();
    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);
    data.put("userId", userId);
    data.put("tags", Arrays.asList(Map.of("key", "category", "value", "document")));

    DynamicDocumentItem ditem = new DynamicDocumentItem(data);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      item.setContentLength(Long.valueOf(contentLength));

      service.saveDocument(siteId, item,
          Arrays.asList(new DocumentTag(documentId, "playerId", "1234", new Date(), userId),
              new DocumentTag(documentId, "category", "person", new Date(), userId)));

      actionsService.saveAction(siteId, documentId,
          new Action().type(ActionType.FULLTEXT).status(ActionStatus.COMPLETE), 0);

      TimeUnit.SECONDS.sleep(1);

      String key = createDatabaseKey(siteId, documentId + FORMKIQ_B64_EXT);

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      byte[] content = gson.toJson(ditem).getBytes(UTF_8);
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

      assertEqualsTag(tags.get(i++), Map.of("documentId", documentId, "key", "playerId", "value",
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
  public void testFkB64Extension13() throws IOException {
    DynamicDocumentItem item = createDocumentItem();

    item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
        Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
   */
  @Test
  public void testFkB64Extension14() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DynamicDocumentItem item = createDocumentItem();

      item.put("tags", new ArrayList<>());
      item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
          Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));
      service.saveDocument(siteId, item, null);

      item.put("metadata", Arrays.asList(Map.of("key", "playerId", "value", "333")));

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
   */
  @Test
  public void testFkB64Extension15() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DynamicDocumentItem item = createDocumentItem();

      item.put("tags", new ArrayList<>());
      item.put("metadata", Arrays.asList(Map.of("key", "category", "value", "person"),
          Map.of("key", "playerId", "values", Arrays.asList("111", "222"))));
      service.saveDocument(siteId, item, null);

      item.put("metadata", Arrays.asList(Map.of("key", "playerId")));

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
   * S3 Object Unknown Event Unit Test.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUnknownEvent01() throws Exception {
    // given
    final Map<String, Object> map = loadFileAsMap(this, "/objectunknown-event1.json");

    final StagingS3Create handler =
        new StagingS3Create(this.env, null, dbBuilder, s3Builder, ssmBuilder, snsBuilder);

    // when
    Void result = handler.handleRequest(map, this.context);

    // then
    assertNull(result);

    assertTrue(this.logger.containsString("handling 1 record(s)."));
    assertTrue(this.logger.containsString("skipping event ObjectUnknwn:Delete"));

    verifySqsMessages(0, 0, 0);
  }

  private void verifyBelongsToDocument(final DocumentItem item, final String documentId,
      final List<String> documentIds) {
    assertEquals(documentIds.size(), item.getDocuments().size());

    for (int i = 0; i < documentIds.size(); i++) {
      assertEquals(documentIds.get(i), item.getDocuments().get(i).getDocumentId());
      assertNotNull(item.getDocuments().get(i).getInsertedDate());
      assertNotNull(item.getDocuments().get(i).getBelongsToDocumentId());
    }
  }

  private void verifyCliSyncs(final String siteId, final String documentId) {
    int i = 0;
    PaginationResults<DocumentSync> syncs =
        syncService.getSyncs(siteId, documentId, null, MAX_RESULTS);
    List<DocumentSync> results = syncs.getResults();
    assertEquals(2, results.size());

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
    assertNotNull(results.get(i++).getSyncDate());
  }

  private void verifyS3Metadata(final String siteId, final DocumentItem item) {
    String key = createDatabaseKey(siteId, item.getDocumentId());
    S3ObjectMetadata objectMetadata = s3.getObjectMetadata(DOCUMENTS_BUCKET, key, null);
    assertTrue(objectMetadata.isObjectExists());

    assertNotNull(objectMetadata.getMetadata().get("checksum"));
  }

  /**
   * Verify SQS Messages.
   * 
   * @param createCount int
   * @param deleteCount int
   * @param errorCount int
   * @return {@link List} {@link DocumentEvent}
   */
  private List<DocumentEvent> verifySqsMessages(final int createCount, final int deleteCount,
      final int errorCount) {

    final List<Message> create = sqsService.receiveMessages(snsSqsCreateQueueUrl).messages();
    final List<Message> delete = sqsService.receiveMessages(snsSqsDeleteQueueUrl).messages();

    assertEquals(createCount, create.size());
    assertEquals(deleteCount, delete.size());

    List<Message> list = new ArrayList<>(create);
    list.addAll(delete);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> maps =
        list.stream().map(m -> (Map<String, Object>) gson.fromJson(m.body(), Map.class))
            .collect(Collectors.toList());

    return maps.stream().map(m -> gson.fromJson(m.get("Message").toString(), DocumentEvent.class))
        .collect(Collectors.toList());
  }
}
