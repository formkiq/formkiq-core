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

import static com.formkiq.stacks.dynamodb.DocumentService.DATE_FORMAT;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFile;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.DynamoDbHelper;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.stacks.lambda.s3.util.LambdaLoggerRecorder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** Unit Tests for {@link StagingS3Create}. */
public class StagingS3CreateTest {

  /** SQS Sns Queue. */
  private static final String SNS_SQS_DELETE_QUEUE = "sqssnsDelete";
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate";
  /** SQS Sns Queue. */
  private static final String ERROR_SQS_QUEUE = "sqserror";
  /** UUID 1. */
  private static final String UUID1 = "b53c92cf-f7b9-4787-9541-76574ec70d71";

  /** Documents S3 Bucket. */
  private static final String DOCUMENTS_BUCKET = "documentsbucket";

  /** Document S3 Staging Bucket. */
  private static final String STAGING_BUCKET = "example-bucket";

  /** LocalStack Endpoint. */
  private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";

  /** DynamoDB Endpoint. */
  private static final String DYNAMODB_ENDPOINT = "http://localhost:8000";

  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsBuilder;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Builder;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** SNS Update Topic Arn. */
  private static String snsDeleteTopic;
  /** SQS Sns Delete QueueUrl. */
  private static String snsSqsDeleteQueueUrl;
  /** SQS Sns Create QueueUrl. */
  private static String snsSqsCreateQueueUrl;
  /** SQS Create Url. */
  private static String snsCreateTopic;
  /** SQS Error Url. */
  private static String sqsErrorUrl;

  /**
   * Before Class.
   * 
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @BeforeClass
  public static void beforeClass() throws URISyntaxException, InterruptedException, IOException {

    Region region = Region.US_EAST_1;
    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    snsBuilder = new SnsConnectionBuilder().setEndpointOverride(LOCALSTACK_ENDPOINT)
        .setRegion(region).setCredentials(cred);

    sqsBuilder = new SqsConnectionBuilder().setEndpointOverride(LOCALSTACK_ENDPOINT)
        .setRegion(region).setCredentials(cred);
    sqsService = new SqsService(sqsBuilder);

    s3Builder = new S3ConnectionBuilder().setEndpointOverride(LOCALSTACK_ENDPOINT).setRegion(region)
        .setCredentials(cred);

    DynamoDbConnectionBuilder dbBuilder = new DynamoDbConnectionBuilder().setRegion(region)
        .setEndpointOverride(DYNAMODB_ENDPOINT).setCredentials(cred);
    service = new DocumentServiceImpl(dbBuilder, "Documents");
    dbHelper = new DynamoDbHelper(dbBuilder);

    s3 = new S3Service(s3Builder);

    try (S3Client c = s3.buildClient()) {
      s3.createBucket(c, DOCUMENTS_BUCKET);
      s3.createBucket(c, STAGING_BUCKET);
    }

    if (!sqsService.exists(SNS_SQS_DELETE_QUEUE)) {
      snsSqsDeleteQueueUrl = sqsService.createQueue(SNS_SQS_DELETE_QUEUE).queueUrl();
    }

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      snsSqsCreateQueueUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    if (!sqsService.exists(ERROR_SQS_QUEUE)) {
      sqsErrorUrl = sqsService.createQueue(ERROR_SQS_QUEUE).queueUrl();
    }

    snsService = new SnsService(snsBuilder);
    snsDeleteTopic = snsService.createTopic("deleteDocument").topicArn();
    snsService.subscribe(snsDeleteTopic, "sqs",
        "arn:aws:sqs:us-east-1:000000000000:" + SNS_SQS_DELETE_QUEUE);

    snsCreateTopic = snsService.createTopic("createDocument").topicArn();
    snsService.subscribe(snsCreateTopic, "sqs",
        "arn:aws:sqs:us-east-1:000000000000:" + SNS_SQS_CREATE_QUEUE);

    if (!dbHelper.isDocumentsTableExists()) {
      dbHelper.createDocumentsTable();
      dbHelper.createCacheTable();
    }
  }

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().setDateFormat(DATE_FORMAT).create();

  /** Environment Map. */
  private Map<String, String> env = new HashMap<>();

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /** {@link LambdaLoggerRecorder}. */
  private LambdaLoggerRecorder logger;

  /**
   * before.
   */
  @Before
  public void before() {

    this.env = new HashMap<>();
    this.env.put("DOCUMENTS_S3_BUCKET", DOCUMENTS_BUCKET);
    this.env.put("SNS_DELETE_TOPIC", snsDeleteTopic);
    this.env.put("SNS_CREATE_TOPIC", snsCreateTopic);
    this.env.put("SQS_ERROR_URL", sqsErrorUrl);

    this.context = new LambdaContextRecorder();
    this.logger = (LambdaLoggerRecorder) this.context.getLogger();

    dbHelper.truncateDocumentsTable();

    try (S3Client c = s3.buildClient()) {
      s3.deleteAllFiles(c, STAGING_BUCKET);
      s3.deleteAllFiles(c, DOCUMENTS_BUCKET);
    }

    for (String queue : Arrays.asList(sqsErrorUrl, snsSqsCreateQueueUrl, snsSqsDeleteQueueUrl)) {
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
    item.put("content",
        Base64.getEncoder().encodeToString(content.getBytes(UTF_8)));
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
    final StagingS3Create handler = new StagingS3Create(this.env, service, s3Builder, sqsBuilder);

    Void result = handler.handleRequest(map, this.context);
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

    // given
    String documentId = UUID.randomUUID().toString();
    this.logger.getRecordedMessages().clear();

    String key = createDatabaseKey(siteId, documentId + ".fkb64");

    Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

    try (S3Client c = s3.buildClient()) {

      byte[] content = this.gson.toJson(docitem).getBytes(UTF_8);
      s3.putObject(c, STAGING_BUCKET, key, content, null, null);

      // when
      handleRequest(map);

      // then
      String destDocumentId = findDocumentIdFromLogger(siteId);

      if (destDocumentId != null) {
        assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));
        assertTrue(this.logger.containsString("handling 1 record(s)."));
        assertTrue(this.logger.containsString("Inserted " + docitem.getPath()
            + " into bucket documentsbucket as " + createDatabaseKey(siteId, destDocumentId)));

        assertFalse(s3.getObjectMetadata(c, STAGING_BUCKET, documentId).isObjectExists());

        DocumentItem item = service.findDocument(siteId, destDocumentId);
        assertEquals("20", item.getContentLength().toString());
        assertEquals("plain/text", item.getContentType());
        assertTrue(item.getChecksum() != null && item.getInsertedDate() != null);
        assertEquals("test.txt", item.getPath());
        assertEquals("joe", item.getUserId());

        boolean hasTags = docitem.containsKey("tags");

        List<DocumentTag> tags =
            service.findDocumentTags(siteId, destDocumentId, null, MAX_RESULTS).getResults();
        int tagcount = hasTags ? docitem.getList("tags").size() + 1 : 2;
        assertEquals(tagcount, tags.size());

        DocumentTag ptag = findTag(tags, "path");
        assertEquals(destDocumentId, ptag.getDocumentId());
        assertEquals(docitem.getPath(), ptag.getValue());
        assertNotNull(ptag.getInsertedDate());
        assertEquals(DocumentTagType.SYSTEMDEFINED, ptag.getType());
        assertEquals(docitem.getUserId(), ptag.getUserId());

        if (hasTags) {

          for (DynamicObject tag : docitem.getList("tags")) {
            DocumentTag dtag = findTag(tags, tag.getString("key"));
            assertEquals(tag.getString("value"), dtag.getValue());
          }

        } else {

          ptag = findTag(tags, "untagged");
          assertEquals(destDocumentId, ptag.getDocumentId());
          assertEquals("true", ptag.getValue());
          assertNotNull(ptag.getInsertedDate());
          assertEquals(DocumentTagType.SYSTEMDEFINED, ptag.getType());
          assertEquals(docitem.getUserId(), ptag.getUserId());
        }
      }
    }
  }

  /**
   * Test CopyFile.
   * 
   * @param documentId {@link String}
   * @param expectedPath {@link String}
   * @throws IOException IOException
   */
  private void testCopyFile(final String documentId, final String expectedPath) throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      this.logger.getRecordedMessages().clear();

      String key = createDatabaseKey(siteId, documentId);

      Map<String, String> metadata = new HashMap<>();
      metadata.put("Content-Type", "application/pdf");
      metadata.put("userId", "1234");

      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event1.json", UUID1, key);

      try (S3Client c = s3.buildClient()) {

        s3.putObject(c, STAGING_BUCKET, key, "testdata".getBytes(UTF_8),
            "application/pdf", metadata);

        // when
        handleRequest(map);

        // then
        String destDocumentId = findDocumentIdFromLogger(siteId);

        assertTrue(this.logger.containsString(
            "Removing " + createDatabaseKey(siteId, documentId) + " from bucket example-bucket."));
        assertTrue(this.logger.containsString("handling 1 record(s)."));
        assertTrue(this.logger.containsString("Copying " + key + " from bucket example-bucket to "
            + createDatabaseKey(siteId, destDocumentId) + " in bucket " + DOCUMENTS_BUCKET + "."));
        assertTrue(this.logger.containsString("Removing " + key + " from bucket example-bucket."));

        assertFalse(s3.getObjectMetadata(c, STAGING_BUCKET, documentId).isObjectExists());

        DocumentItem item = service.findDocument(siteId, destDocumentId);
        assertNotNull(item);
        assertEquals("8", item.getContentLength().toString());
        assertEquals("application/pdf", item.getContentType());
        assertEquals("ef654c40ab4f1747fc699915d4f70902", item.getChecksum());
        assertNotNull(item.getInsertedDate());
        assertEquals(item.getPath(), expectedPath);
        assertEquals("1234", item.getUserId());

        assertTrue(
            service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().isEmpty());
      }
    }
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  public void testCopyFile01() throws Exception {
    final String documentId = "b53c92cf-f7b9-4787-9541-76574ec70d71";
    testCopyFile(documentId, null);
  }

  /**
   * S3 Object Create Event Unit Test where filename IS UUID.
   *
   * @throws Exception Exception
   */
  @Test
  public void testCopyFile02() throws Exception {
    final String documentId = "test.pdf";
    testCopyFile(documentId, "test.pdf");
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
    //
    // item.setTags(Arrays.asList(new DocumentTag(item.getDocumentId(), "category", "person",
    // new Date(), "joe", DocumentTagType.USERDEFINED)));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      processFkB64File(siteId, item);
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
      processFkB64File(siteId, item);
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
      processFkB64File(siteId, item);
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
      processFkB64File(siteId, item);
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

      String key = createDatabaseKey(siteId, documentId0 + ".fkb64");
      Map<String, Object> map = loadFileAsMap(this, "/objectcreate-event4.json", UUID1, key);

      try (S3Client c = s3.buildClient()) {

        String json = loadFile(this, "/document_multiple.fkb64");
        s3.putObject(c, STAGING_BUCKET, key, json.getBytes(UTF_8), null, null);

        // when
        handleRequest(map);

        // then
        assertEquals(1,
            service.findDocumentsByDate(siteId, nowDate, null, MAX_RESULTS).getResults().size());

        DocumentItem i = service.findDocument(siteId, documentId0, true);
        assertNull(i.getContentType());
        verifyBelongsToDocument(i, documentId0, Arrays.asList(documentId1, documentId2));

        List<DocumentTag> tags =
            service.findDocumentTags(siteId, documentId0, null, MAX_RESULTS).getResults();
        assertEquals(1, tags.size());
        assertEquals("formName", tags.get(0).getKey());
        assertEquals("Job Application Form", tags.get(0).getValue());
        
        String k = createDatabaseKey(siteId, i.getDocumentId());
        assertFalse(s3.getObjectMetadata(c, DOCUMENTS_BUCKET, k).isObjectExists());

        i = service.findDocument(siteId, documentId1, true);
        assertEquals("application/json", i.getContentType());
        assertEquals(documentId0, i.getBelongsToDocumentId());
        tags = service.findDocumentTags(siteId, documentId1, null, MAX_RESULTS).getResults();
        assertEquals(1, tags.size());
        assertEquals("formData", tags.get(0).getKey());
        assertNull(tags.get(0).getValue());

        k = createDatabaseKey(siteId, i.getDocumentId());
        assertEquals("application/json",
            s3.getObjectMetadata(c, DOCUMENTS_BUCKET, k).getContentType());

        i = service.findDocument(siteId, documentId2);
        assertEquals(documentId0, i.getBelongsToDocumentId());
        assertNull(i.getContentType());
        tags = service.findDocumentTags(siteId, documentId2, null, MAX_RESULTS).getResults();
        assertEquals(2, tags.size());
        assertEquals("attachmentField", tags.get(0).getKey());
        assertEquals("resume", tags.get(0).getValue());
        assertEquals("category", tags.get(1).getKey());
        assertNull(tags.get(1).getValue());
        
        k = createDatabaseKey(siteId, i.getDocumentId());
        assertFalse(s3.getObjectMetadata(c, DOCUMENTS_BUCKET, k).isObjectExists());
      }
    }
  }

  private void verifyBelongsToDocument(final DocumentItem item, final String documentId,
      final List<String> documentIds) {
    assertEquals(documentIds.size(), item.getDocuments().size());
    
    for (int i = 0; i < documentIds.size(); i++) {
      assertEquals(documentIds.get(i), item.getDocuments().get(i).getDocumentId());
      assertEquals(documentId, item.getDocuments().get(i).getBelongsToDocumentId());
    }
  }

  /**
   * Handle Error.
   */
  @Test
  public void testHandleError() {
    handleRequest(null);
    ReceiveMessageResponse response = sqsService.receiveMessages(sqsErrorUrl);
    assertEquals(1, response.messages().size());
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

    final StagingS3Create handler = new StagingS3Create(this.env, service, s3Builder, sqsBuilder);

    // when
    Void result = handler.handleRequest(map, this.context);

    // then
    assertNull(result);

    assertTrue(this.logger.containsString("handling 1 record(s)."));
    assertTrue(this.logger.containsString("skipping event ObjectUnknwn:Delete"));

    verifySqsMessages(0, 0, 0);
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
    final List<Message> error = sqsService.receiveMessages(sqsErrorUrl).messages();

    assertEquals(createCount, create.size());
    assertEquals(deleteCount, delete.size());
    assertEquals(errorCount, error.size());

    List<Message> list = new ArrayList<>(create);
    list.addAll(delete);
    list.addAll(error);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> maps =
        list.stream().map(m -> (Map<String, Object>) this.gson.fromJson(m.body(), Map.class))
            .collect(Collectors.toList());

    return maps.stream()
        .map(m -> this.gson.fromJson(m.get("Message").toString(), DocumentEvent.class))
        .collect(Collectors.toList());
  }
}
