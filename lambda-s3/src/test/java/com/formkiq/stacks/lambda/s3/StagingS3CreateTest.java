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
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
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
import com.formkiq.aws.dynamodb.schema.DocumentSchema;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sns.SnsServiceExtension;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.apimodels.AddDocumentTag;
import com.formkiq.stacks.dynamodb.apimodels.MatchDocumentTag;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequest;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestMatch;
import com.formkiq.stacks.dynamodb.apimodels.UpdateMatchingDocumentTagsRequestUpdate;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.stacks.lambda.s3.util.LambdaLoggerRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
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
  /** {@link DocumentService}. */
  private static DocumentService service;
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

    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", Region.US_EAST_1.id());
    env.put("DOCUMENTS_S3_BUCKET", DOCUMENTS_BUCKET);
    env.put("SNS_DELETE_TOPIC", snsDeleteTopic);
    env.put("SNS_CREATE_TOPIC", snsCreateTopic);
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("SNS_DOCUMENT_EVENT", snsDocumentEvent);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    awsServices = new AwsServiceCacheBuilder(Collections.unmodifiableMap(env),
        TestServices.getEndpointMap(), credentialsProvider)
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(), new SmsAwsServiceRegistry())
        .build();

    awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServices.register(SnsService.class, new SnsServiceExtension());
    awsServices.register(SsmService.class, new SsmServiceExtension());
    awsServices.register(S3Service.class, new S3ServiceExtension());
    awsServices.register(DocumentSyncService.class, new DocumentSyncServiceExtension());
    awsServices.register(ActionsService.class, new ActionsServiceExtension());
    awsServices.register(SqsService.class, new SqsServiceExtension());
    awsServices.register(DocumentService.class, new DocumentServiceExtension());
    awsServices.register(FolderIndexProcessor.class, new FolderIndexProcessorExtension());

    SsmService ssmService = awsServices.getExtension(SsmService.class);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    s3 = awsServices.getExtension(S3Service.class);
    syncService = awsServices.getExtension(DocumentSyncService.class);
    sqsService = awsServices.getExtension(SqsService.class);
    actionsService = awsServices.getExtension(ActionsService.class);

    if (!sqsService.exists(SNS_SQS_CREATE_QUEUE)) {
      sqsDocumentEventUrl = sqsService.createQueue(SNS_SQS_CREATE_QUEUE).queueUrl();
    }

    snsService = awsServices.getExtension(SnsService.class);
    snsDocumentEvent = snsService.createTopic("createDocument1").topicArn();
    awsServices.environment().put("SNS_DOCUMENT_EVENT", snsDocumentEvent);

    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentEventUrl);
    snsService.subscribe(snsDocumentEvent, "sqs", sqsQueueArn);

    service = awsServices.getExtension(DocumentService.class);

    folderIndexProcesor = awsServices.getExtension(FolderIndexProcessor.class);

    dbHelper = new DynamoDbHelper(awsServices.getExtension(DynamoDbConnectionBuilder.class));
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

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  // /** Environment Map. */
  // private Map<String, String> env = new HashMap<>();

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
  void before() {

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

  private void createDocument(final String siteId, final String userId, final byte[] content,
      final String docId) {
    DynamicDocumentItem item = new DynamicDocumentItem(new HashMap<>());
    item.setDocumentId(docId == null ? UUID.randomUUID().toString() : docId);
    item.setUserId(userId);
    item.setInsertedDate(new Date());
    final String documentId = item.getDocumentId();
    service.saveDocument(siteId, item, null);

    final String key = createS3Key(siteId, documentId);
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
    item.setDocumentId(UUID.randomUUID().toString());
    item.put("content", Base64.getEncoder().encodeToString(content.getBytes(UTF_8)));
    item.setContentLength(Long.valueOf(content.length()));
    item.setContentType("plain/text");
    item.setInsertedDate(new Date());
    item.setPath("test.txt");
    item.setUserId("joe");

    return item;
  }

  private Map<String, Object> createRequestMap(final String s3Key) {

    Map<String, Object> record = Map.of("eventName", "ObjectCreated", "s3",
        Map.of("bucket", Map.of("name", STAGING_BUCKET), "object", Map.of("key", s3Key)));
    String body = gson.toJson(Map.of("Records", Arrays.asList(record)));
    List<Map<String, Object>> records = Arrays.asList(Map.of("body", body));

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
    final StagingS3Create handler = new StagingS3Create(awsServices);

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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testCopyFile01() throws Exception {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testCopyFile02() throws Exception {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testCopyFile03() throws Exception {
    final String documentId = "something/where/test.pdf";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      testCopyFile(siteId, documentId, "something/where/test.pdf");
    }
  }

  /**
   * Tests document compression event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
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
      this.createDocument("default", "JohnDoe", content, docId);
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension01() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension02() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension03() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension04() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension05() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension06() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension07() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension08() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension09() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension10() throws IOException {

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
          Arrays.asList(
              Map.of("type", "ocr", "status", "PENDING", "parameters", Map.of("test", "1234")),
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
      assertEquals("System", actions.get(0).userId());
      assertNotNull(actions.get(0).insertedDate());

      assertEquals("WEBHOOK", actions.get(1).type().name());
      assertEquals("PENDING", actions.get(1).status().name());
      assertEquals("joesmith", actions.get(1).userId());
      assertNotNull(actions.get(1).insertedDate());

      actions.get(0).status(ActionStatus.COMPLETE);
      actionsService.saveNewActions(siteId, documentId, actions);

      // given
      data.put("actions", Arrays.asList(
          Map.of("type", "ocr", "status", "PENDING", "parameters", Map.of("test", "1234"))));
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension12() throws Exception {
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
          new Action().type(ActionType.FULLTEXT).userId("joe").status(ActionStatus.COMPLETE), 0);

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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension13() throws IOException {
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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension14() throws IOException {

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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension15() throws IOException {

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
   * Test Add .fkb64 file deep link.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension16() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DynamicDocumentItem item = createDocumentItem();
      item.remove("content");
      item.put("tags", Arrays.asList(Map.of("key", "category", "value", "document")));
      item.setDeepLinkPath("http://google.com/sample.pdf");
      item.setContentType("application/pdf");

      processFkB64File(siteId, item, null);
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("http://google.com/sample.pdf", doc.getDeepLinkPath());
      assertEquals("sample.pdf", doc.getPath());
      assertEquals("application/pdf", doc.getContentType());
    }
  }

  /**
   * Test Update .fkb64 file deep link with content.
   *
   * @throws IOException IOException
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testFkB64Extension17() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DynamicDocumentItem item = createDocumentItem();
      item.put("tags", Arrays.asList(Map.of("key", "category", "value", "document")));
      item.setDeepLinkPath("http://google.com/sample.pdf");
      service.saveDocument(siteId, item, null);

      item.put("content", "this is some content");

      processFkB64File(siteId, item, "14");
      DocumentItem doc = service.findDocument(siteId, item.getDocumentId());
      assertEquals("", doc.getDeepLinkPath());
    }
  }

  /**
   * Test processing S3 file from PATCH /documents/tags.
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testPatchDocumentsTags01() {
    // given
    final int maxDocuments = 150;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String key = "category";
      String value = "person";

      String newKey = "person";
      String newValue = "111";

      List<String> documentIds = new ArrayList<>();
      for (int i = 0; i < maxDocuments; i++) {
        DynamicDocumentItem item = createDocumentItem();
        documentIds.add(item.getDocumentId());

        Collection<DocumentTag> tags =
            Arrays.asList(new DocumentTag(item.getDocumentId(), key, value, new Date(), "joe"));
        service.saveDocument(siteId, item, tags);
      }

      List<AddDocumentTag> tags = Arrays.asList(new AddDocumentTag().key(newKey).value(newValue));
      UpdateMatchingDocumentTagsRequest req = new UpdateMatchingDocumentTagsRequest()
          .match(new UpdateMatchingDocumentTagsRequestMatch()
              .tag(new MatchDocumentTag().key(key).eq(value)))
          .update(new UpdateMatchingDocumentTagsRequestUpdate().tags(tags));

      byte[] data = gson.toJson(req).getBytes(StandardCharsets.UTF_8);

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
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testPatchDocumentsTags02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String key = "category";
      String value = "person";

      String newKey0 = "person";
      String newValue0 = "111";

      String newKey1 = "player";
      String newValue1 = "222";

      DynamicDocumentItem item = createDocumentItem();

      service.saveDocument(siteId, item,
          Arrays.asList(new DocumentTag(item.getDocumentId(), key, value, new Date(), "joe")));

      List<AddDocumentTag> tags = Arrays.asList(new AddDocumentTag().key(newKey0).value(newValue0),
          new AddDocumentTag().key(newKey1).value(newValue1));

      UpdateMatchingDocumentTagsRequest req = new UpdateMatchingDocumentTagsRequest()
          .match(new UpdateMatchingDocumentTagsRequestMatch()
              .tag(new MatchDocumentTag().key(key).eq(value)))
          .update(new UpdateMatchingDocumentTagsRequestUpdate().tags(tags));

      byte[] data = gson.toJson(req).getBytes(StandardCharsets.UTF_8);

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
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testTempFilesZipEvent() throws Exception {
    final Map<String, Object> map = loadFileAsMap(this, "/temp-files-zip-created-event.json");
    final String key = "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.zip";
    this.handleRequest(map);

    assertTrue(this.logger.containsString(String.format("skipping event for key %s", key)));

    verifySqsMessages(0, 0, 0);
  }

  /**
   * S3 Object Unknown Event Unit Test.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testUnknownEvent01() throws Exception {
    // given
    final Map<String, Object> map = loadFileAsMap(this, "/objectunknown-event1.json");

    final StagingS3Create handler = new StagingS3Create(awsServices);

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
