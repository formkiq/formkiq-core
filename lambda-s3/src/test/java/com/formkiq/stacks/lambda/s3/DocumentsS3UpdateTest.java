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

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamicDocumentTag;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.DynamoDbHelper;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.stacks.lambda.s3.util.LambdaLoggerRecorder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** {@link DocumentsS3Update} Unit Tests. */
public class DocumentsS3UpdateTest {

  /** Test Timeout. */
  private static final long TEST_TIMEOUT = 30000L;
  /** Bucket Key. */
  private static final String BUCKET_KEY = "b53c92cf-f7b9-4787-9541-76574ec70d71";
  /** LocalStack Endpoint. */
  private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
  /** DynamoDB Endpoint. */
  private static final String DYNAMODB_ENDPOINT = "http://localhost:8000";
  /** SQS Error Queue. */
  private static final String ERROR_SQS_QUEUE = "sqserror";
  /** SQS Sns Update Queue. */
  private static final String SNS_SQS_CREATE_QUEUE = "sqssnsCreate";

  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;

  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link SnsConnectionBuilder}. */
  private static SnsConnectionBuilder snsBuilder;
  /** {@link SqsConnectionBuilder}. */
  private static SqsConnectionBuilder sqsBuilder;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link S3ConnectionBuilder}. */
  private static S3ConnectionBuilder s3Builder;
  /** {@link S3Service}. */
  private static S3Service s3service;
  /** {@link SnsService}. */
  private static SnsService snsService;
  /** SQS Create Url. */
  private static String snsDocumentEvent;
  /** SQS Sns Create QueueUrl. */
  private static String sqsDocumentEventUrl;

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

    if (!sqsService.exists(ERROR_SQS_QUEUE)) {
      sqsService.createQueue(ERROR_SQS_QUEUE);
    }

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    s3service = new S3Service(s3Builder);
    try (S3Client s3 = s3service.buildClient()) {
      s3service.createBucket(s3, "example-bucket");
    }

    snsService = new SnsService(snsBuilder);

    snsDocumentEvent = snsService.createTopic("createDocument").topicArn();
    snsService.subscribe(snsDocumentEvent, "sqs", sqsDocumentEventUrl);

    dbHelper = new DynamoDbHelper(dbBuilder);
    if (!dbHelper.isDocumentsTableExists()) {
      dbHelper.createDocumentsTable();
      dbHelper.createCacheTable();
    }
  }

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;
  /** {@link LambdaLoggerRecorder}. */
  private LambdaLoggerRecorder logger;

  /** {@link DocumentsS3Update}. */
  private DocumentsS3Update handler;

  /**
   * Assert Publish SNS Topic.
   * 
   * @param siteId {@link String}
   * @param sqsQueueUrl {@link String}
   * @param eventType {@link String}
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private void assertPublishSnsMessage(final String siteId, final String sqsQueueUrl,
      final String eventType) throws InterruptedException {

    List<Message> msgs = sqsService.receiveMessages(sqsQueueUrl).messages();
    while (msgs.size() != 1) {
      Thread.sleep(SLEEP);
    }
    assertEquals(1, msgs.size());

    sqsService.deleteMessage(sqsQueueUrl, msgs.get(0).receiptHandle());
    Map<String, String> map = this.gson.fromJson(msgs.get(0).body(), Map.class);
    String message = map.get("Message");
    
    map = this.gson.fromJson(message, Map.class);
    assertNotNull(map.get("documentId"));
    assertEquals(eventType, map.get("type"));

    if (siteId != null) {
      assertEquals(siteId, map.get("siteId"));
    } else {
      assertNull(map.get("siteId"));
    }
  }

  /**
   * before.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @Before
  public void before() throws URISyntaxException {

    try (S3Client s3 = s3service.buildClient()) {
      s3service.deleteAllFiles(s3, "example-bucket");
      s3service.deleteAllFiles(s3, "example-bucket");
    }

    dbHelper.truncateDocumentsTable();

    Map<String, String> map = new HashMap<>();
    map.put("SQS_ERROR_URL", LOCALSTACK_ENDPOINT + "/queue/" + ERROR_SQS_QUEUE);
    map.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);

    this.context = new LambdaContextRecorder();
    this.logger = (LambdaLoggerRecorder) this.context.getLogger();
    this.handler = new DocumentsS3Update(map, service, s3Builder, sqsBuilder, snsBuilder);

    for (String queue : Arrays.asList(sqsDocumentEventUrl)) {
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
   * Create Document Request without existing Tags/Formats.
   *
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testHandleRequest01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.getRecordedMessages().clear();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);
      service.saveDocumentItemWithTag(siteId, doc);

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map, key);

      // then
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      assertEquals(2, tags.getResults().size());
      assertEquals("CLAMAV_SCAN_STATUS", tags.getResults().get(0).getKey());
      assertEquals("GOOD", tags.getResults().get(0).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(0).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(0).getType());
      assertNull(tags.getResults().get(0).getUserId());
      assertNotNull(tags.getResults().get(0).getInsertedDate());

      assertEquals("sample", tags.getResults().get(1).getKey());
      assertEquals("12345", tags.getResults().get(1).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(1).getDocumentId());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(1).getType());
      assertNull(tags.getResults().get(1).getUserId());
      assertNotNull(tags.getResults().get(1).getInsertedDate());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item);
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, "create");
    }
  }

  /**
   * Update Document Request with existing Tags.
   *
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testHandleRequest02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.getRecordedMessages().clear();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectupdate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);

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

      // when
      final DocumentItem item = handleRequest(siteId, BUCKET_KEY, map, key);

      // then
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      final int size = 3;
      assertEquals(size, tags.getResults().size());
      assertEquals("CLAMAV_SCAN_STATUS", tags.getResults().get(0).getKey());
      assertEquals("GOOD", tags.getResults().get(0).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(0).getType());
      assertEquals("person", tags.getResults().get(1).getKey());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(1).getType());
      assertEquals("12345", tags.getResults().get(2).getValue());
      assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(2).getType());
      assertEquals("12345", tags.getResults().get(2).getValue());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());

      verifyDocumentSaved(siteId, item);
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, "update");
    }
  }

  /**
   * Delete Document Request.
   *
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testHandleRequest03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.getRecordedMessages().clear();

      String key = createDatabaseKey(siteId, BUCKET_KEY);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectremove-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);

      DynamicDocumentTag tag = new DynamicDocumentTag(Map.of("documentId", BUCKET_KEY, "key",
          "person", "value", "category", "insertedDate", new Date(), "userId", "asd"));
      doc.put("tags", Arrays.asList(tag));

      service.saveDocumentItemWithTag(siteId, doc);

      // when
      DocumentItem item = handleRequest(siteId, BUCKET_KEY, map, key);

      // then
      assertNull(item);
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, "delete");
    }
  }

  /**
   * Create Document Request on child document.
   *
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testHandleRequest04() throws Exception {

    String documentId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      this.logger.getRecordedMessages().clear();

      String key = createDatabaseKey(siteId, documentId);
      final Map<String, Object> map =
          loadFileAsMap(this, "/objectcreate-event1.json", BUCKET_KEY, key);

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of());
      doc.setInsertedDate(new Date());
      doc.setDocumentId(BUCKET_KEY);

      DynamicDocumentItem child = new DynamicDocumentItem(Map.of());
      child.setInsertedDate(new Date());
      child.setDocumentId(documentId);
      doc.put("documents", Arrays.asList(child));

      service.saveDocumentItemWithTag(siteId, doc);

      // when
      final DocumentItem item = handleRequest(siteId, child.getDocumentId(), map, key);

      // then
      assertNotNull(item.getBelongsToDocumentId());
      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, BUCKET_KEY, null, MAX_RESULTS);

      assertEquals(1, tags.getResults().size());
      assertEquals("untagged", tags.getResults().get(0).getKey());
      assertEquals("true", tags.getResults().get(0).getValue());
      assertEquals(BUCKET_KEY, tags.getResults().get(0).getDocumentId());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(0).getType());
      assertNull(tags.getResults().get(0).getUserId());
      assertNotNull(tags.getResults().get(0).getInsertedDate());

      assertEquals(0,
          service.findDocumentFormats(siteId, BUCKET_KEY, null, MAX_RESULTS).getResults().size());
      verifyDocumentSaved(siteId, item);
      assertPublishSnsMessage(siteId, sqsDocumentEventUrl, "create");
    }
  }

  /**
   * Handle Request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param map {@link Map}
   * @param key {@link String}
   * 
   * @return {@link DocumentItem}
   * @throws IOException IOException
   */
  private DocumentItem handleRequest(final String siteId, final String documentId,
      final Map<String, Object> map, final String key) throws IOException {

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", "pdf");

    try (S3Client s3 = s3service.buildClient()) {
      s3service.putObject(s3, "example-bucket", key, "testdata".getBytes(StandardCharsets.UTF_8),
          "pdf", metadata);
      s3service.setObjectTags(s3, "example-bucket", key,
          Arrays.asList(Tag.builder().key("sample").value("12345").build(),
              Tag.builder().key("CLAMAV_SCAN_STATUS").value("GOOD").build()));
    }

    // when
    this.handler.handleRequest(map, this.context);

    // then
    return service.findDocument(siteId, documentId);
  }

  /**
   * Verify {@link DocumentItem}.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link DocumentItem}
   */
  private DocumentItem verifyDocumentSaved(final String siteId, final DocumentItem item) {
    assertTrue(this.logger
        .containsString("saving document " + createDatabaseKey(siteId, item.getDocumentId())));

    assertEquals("pdf", item.getContentType());
    assertEquals("8", item.getContentLength().toString());
    assertNull(item.getPath());

    try (S3Client s3 = s3service.buildClient()) {
      s3service.deleteAllObjectTags(s3, "example-bucket", item.getDocumentId());
      service.deleteDocumentTags(siteId, item.getDocumentId());
    }

    return item;
  }
}
