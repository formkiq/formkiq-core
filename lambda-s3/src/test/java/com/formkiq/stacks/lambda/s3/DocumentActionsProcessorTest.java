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

import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceImpl;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.lambda.s3.util.FileUtils;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import joptsimple.internal.Strings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
@ExtendWith(TypesenseExtension.class)
public class DocumentActionsProcessorTest implements DbKeys {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static RequestRecordExpectationResponseCallback callback =
      new RequestRecordExpectationResponseCallback();
  /** {@link ConfigService}. */
  private static ConfigService configService;
  /** {@link AwsBasicCredentials}. */
  private static AwsBasicCredentials credentials = AwsBasicCredentials.create("asd", "asd");
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbBuilder;
  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** {@link Gson}. */
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  /** {@link ClientAndServer}. */
  private static ClientAndServer mockServer;
  /** Port to run Test server. */
  private static final int PORT = 8888;
  /** {@link DocumentActionsProcessor}. */
  private static DocumentActionsProcessor processor;
  /** {@link S3Service}. */
  private static S3Service s3Service;
  /** Sns Document Event. */
  private static final String SNS_DOCUMENT_EVENT_TOPIC = "SNS_DOCUMENT_EVENT";
  /** Sns Document Event Topic Arn. */
  private static String snsDocumentEventTopicArn;
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** {@link TypeSenseService}. */
  private static TypeSenseService typesense;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;
  /** Document Id with OCR. */
  private static final String DOCUMENT_ID_OCR = UUID.randomUUID().toString();

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
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeClass() throws Exception {

    dbBuilder = DynamoDbTestServices.getDynamoDbConnection();

    DocumentVersionService versionService = new DocumentVersionServiceNoVersioning();

    documentService = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, versionService);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    createMockServer();

    s3Service = new S3Service(TestServices.getS3Connection(null));
    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection(null);
    ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);

    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    String typeSenseHost = "http://localhost:" + TypesenseExtension.getMappedPort();
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/TypesenseEndpoint",
        typeSenseHost);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/typesense/ApiKey", API_KEY);

    typesense = new TypeSenseServiceImpl(typeSenseHost, API_KEY, Region.US_EAST_1, credentials);

    configService = new ConfigServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);

    SnsService sns = new SnsService(TestServices.getSnsConnection(null));
    snsDocumentEventTopicArn = sns.createTopic(SNS_DOCUMENT_EVENT_TOPIC).topicArn();
  }

  /**
   * Create Mock Server.
   * 
   * @throws IOException IOException
   */
  private static void createMockServer() throws IOException {

    mockServer = startClientAndServer(Integer.valueOf(PORT));

    final int status = 200;

    for (String item : Arrays.asList("1", "2", "3", "4", "5", "6")) {
      String text = FileUtils.loadFile(mockServer, "/chatgpt/response" + item + ".json");
      mockServer.when(request().withMethod("POST").withPath("/chatgpt" + item)).respond(
          org.mockserver.model.HttpResponse.response(text).withStatusCode(Integer.valueOf(status)));
    }

    mockServer.when(request().withMethod("GET").withPath("/documents/" + DOCUMENT_ID_OCR + "/ocr*"))
        .respond(org.mockserver.model.HttpResponse
            .response("{\"contentUrls\":[\"" + URL + "/" + DOCUMENT_ID_OCR + "\"]}"));
    mockServer.when(request().withMethod("PATCH")).respond(callback);
    mockServer.when(request().withMethod("POST")).respond(callback);
    mockServer.when(request().withMethod("PUT")).respond(callback);
    mockServer.when(request().withMethod("GET")).respond(callback);
  }

  private static void initProcessor(final String module, final String chatgptUrl) {
    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", AWS_REGION.toString());
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
    env.put("MODULE_" + module, "true");
    env.put("SNS_DOCUMENT_EVENT", snsDocumentEventTopicArn);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    env.put("CHATGPT_API_COMPLETIONS_URL", URL + "/" + chatgptUrl);

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    AwsServiceCache serviceCache =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SmsAwsServiceRegistry(),
                new SesAwsServiceRegistry())
            .build();

    processor = new DocumentActionsProcessor(serviceCache);
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /**
   * BeforeEach.
   * 
   * @throws Exception Exception
   */
  @BeforeEach
  public void beforeEach() throws Exception {
    this.context = new LambdaContextRecorder();
    callback.reset();

    initProcessor("opensearch", "chatgpt1");
  }

  /**
   * Handle documentTagging ChatApt Action missing GptKey.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<Action> list = actionsService.getActions(siteId, documentId);
      assertEquals(1, list.size());
      assertEquals(ActionStatus.FAILED, list.get(0).status());
      assertEquals("missing config 'ChatGptApiKey'", list.get(0).message());
    }
  }

  /**
   * Handle Queue Action.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testQueueAction01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String name = "testqueue#" + documentId;

      List<Action> actions =
          Arrays.asList(new Action().type(ActionType.QUEUE).userId("joe").queueId(name));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      assertEquals(ActionStatus.IN_QUEUE,
          actionsService.getActions(siteId, documentId).get(0).status());
    }
  }

  /**
   * Handle documentTagging ChatApt Action.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 5;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("document type", tags.getResults().get(i).getKey());
      assertEquals("Memorandum", tags.getResults().get(i++).getValue());

      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("YellowBelly Brewery Pub, St. Johns, NL", tags.getResults().get(i++).getValue());

      assertEquals("organization", tags.getResults().get(i).getKey());
      assertEquals("Great Auk Enterprises", tags.getResults().get(i++).getValue());

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Thomas Bewick,Ketill Ketilsson,Farley Mowat,Aaron Thomas",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("MINUTES OF A MEETING OF DIRECTORS", tags.getResults().get(i++).getValue());
    }
  }

  /**
   * Handle documentTagging invalid engine.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "unknown", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<Action> list = actionsService.getActions(siteId, documentId);
      assertEquals(1, list.size());
      assertEquals(ActionStatus.FAILED, list.get(0).status());
      assertEquals("Unknown engine: unknown", list.get(0).message());
    }
  }

  /**
   * Handle documentTagging ChatApt Action with a non JSON repsonse from ChatGPT.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction04() throws Exception {

    initProcessor("opensearch", "chatgpt2");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 6;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("Organization", tags.getResults().get(i).getKey());
      assertEquals("East Repair Inc", tags.getResults().get(i++).getValue());

      assertEquals("document type", tags.getResults().get(i).getKey());
      assertEquals("Receipt", tags.getResults().get(i++).getValue());

      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("New York, NY 12240; Cambutdigo, MA 12210",
          tags.getResults().get(i++).getValue());

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Job Smith", tags.getResults().get(i++).getValue());

      assertEquals("sentiment", tags.getResults().get(i).getKey());
      assertEquals("None", tags.getResults().get(i++).getValue());

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("Receipt", tags.getResults().get(i++).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action with a non JSON repsonse from ChatGPT.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction05() throws Exception {

    initProcessor("opensearch", "chatgpt3");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 5;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("Organization", tags.getResults().get(i).getKey());
      assertEquals("East Repair Inc", tags.getResults().get(i++).getValue());

      assertEquals("document type", tags.getResults().get(i).getKey());
      assertEquals("Receipt", tags.getResults().get(i++).getValue());

      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("New York, NY 12240,Cambutdigo, MA 12210",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Job Smith", tags.getResults().get(i++).getValue());

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("Frontend eaar brake cabies,New set of podal arms,Labor shrs 500",
          String.join(",", tags.getResults().get(i++).getValues()));
    }
  }

  /**
   * Handle documentTagging ChatApt Action extra quotes.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction06() throws Exception {

    initProcessor("opensearch", "chatgpt4");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 4;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("YellowBelly Brewery Pub, St. Johns, NL", tags.getResults().get(i++).getValue());

      assertEquals("organization", tags.getResults().get(i).getKey());
      assertEquals("Great Auk Enterprises", tags.getResults().get(i++).getValue());

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Thomas Bewick,Ketill Ketilsson,Farley Mowat,Aaron Thomas",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("MINUTES OF A MEETING OF DIRECTORS", tags.getResults().get(i++).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action extra quotes.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction07() throws Exception {

    initProcessor("opensearch", "chatgpt5");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(new Action().type(ActionType.DOCUMENTTAGGING)
          .userId("joe").parameters(Map.of("engine", "chatgpt", "tags",
              "document type,meeting date,chairperson,secretary,board members,resolutions")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 6;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("board members", tags.getResults().get(i).getKey());
      assertEquals("Thomas Bewick,Ketill Ketilsson,Farley Mowat",
          Strings.join(tags.getResults().get(i++).getValues(), ","));

      assertEquals("chairperson", tags.getResults().get(i).getKey());
      assertEquals("Thomas Bewick", tags.getResults().get(i++).getValue());

      assertEquals("document type", tags.getResults().get(i).getKey());
      assertEquals("Minutes of the Director's Meeting", tags.getResults().get(i++).getValue());

      assertEquals("meeting date", tags.getResults().get(i).getKey());
      assertEquals("21st day of April, 2023", tags.getResults().get(i++).getValue());

      assertEquals("resolutions", tags.getResults().get(i).getKey());
      assertEquals("Thomas Bewick", tags.getResults().get(i++).getValue());

      assertEquals("secretary", tags.getResults().get(i).getKey());
      assertEquals("Aaron Thomas", tags.getResults().get(i++).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action with a gpt-3.5-turbo-instruct model response.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDocumentTaggingAction08() throws Exception {

    initProcessor("opensearch", "chatgpt6");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, Arrays.asList(new DocumentTag(documentId,
          "untagged", "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      final int expectedSize = 4;
      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      PaginationResults<DocumentTag> tags =
          documentService.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(expectedSize, tags.getResults().size());

      int i = 0;
      assertEquals("Organization", tags.getResults().get(i).getKey());
      assertEquals("East Repair Inc", tags.getResults().get(i++).getValue());

      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("New York, NY,Cambutdigo, MA",
          tags.getResults().get(i++).getValues().stream().collect(Collectors.joining(",")));

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Job Smith", tags.getResults().get(i++).getValue());

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("Receipt,Frontend eaar brake cabies,New set of podal arms,Labor shrs",
          tags.getResults().get(i++).getValues().stream().collect(Collectors.joining(",")));
    }
  }

  /**
   * Test converting Ocr Parse Types.
   */
  @Test
  public void testGetOcrParseTypes01() {
    assertEquals("[TEXT]", processor.getOcrParseTypes(new Action()).toString());

    // invalid
    Map<String, String> parameters = Map.of("parseTypes", "ADAD,IUJK");
    assertEquals("[ADAD, IUJK]",
        processor.getOcrParseTypes(new Action().parameters(parameters)).toString());

    parameters = Map.of("parseTypes", "tEXT, forms, TABLES");
    assertEquals("[TEXT, FORMS, TABLES]",
        processor.getOcrParseTypes(new Action().parameters(parameters)).toString());
  }

  /**
   * Handle OCR Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle01() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR).userId("joe")
          .parameters(Map.of("addPdfDetectedCharactersAsText", "true")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      HttpRequest lastRequest = callback.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/ocr"));
      Map<String, Object> resultmap = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertEquals("[TEXT]", resultmap.get("parseTypes").toString());
      assertEquals("true", resultmap.get("addPdfDetectedCharactersAsText").toString());

      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionStatus.RUNNING, action.status());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNull(action.completedDate());
    }
  }

  /**
   * Handle Fulltext(Opensearch) plain/text document.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle02() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      HttpRequest lastRequest = callback.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/fulltext"));
      Map<String, Object> resultmap = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertNotNull(resultmap.get("contentUrls").toString());

      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());
    }
  }

  /**
   * Handle Fulltext application/pdf document.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle03() throws IOException, URISyntaxException {

    // given
    String documentId = DOCUMENT_ID_OCR;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      HttpRequest lastRequest = callback.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/fulltext"));
      Map<String, Object> resultmap = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertNotNull(resultmap.get("contentUrls").toString());

      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());
    }
  }

  /**
   * Handle Fulltext missing document failed Actionstatus.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testHandle04() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      Action action = actions.get(0);
      assertEquals(ActionStatus.FAILED, action.status());
      assertEquals("Cannot invoke \"com.formkiq.aws.dynamodb.model."
          + "DocumentItem.getDocumentId()\" because \"item\" is null", action.message());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());
    }
  }

  /**
   * Handle WEBHOOK Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle05() throws IOException, URISyntaxException {
    try (DynamoDbClient db = dbBuilder.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        String documentId = UUID.randomUUID().toString();

        DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
        item.setContentType("application/pdf");
        documentService.saveDocument(siteId, item, null);

        List<Action> actions = Arrays.asList(new Action().type(ActionType.WEBHOOK).userId("joe")
            .parameters(Map.of("url", URL + "/callback")));
        actionsService.saveNewActions(siteId, documentId, actions);

        Map<String, Object> map =
            loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
                documentId, "default", siteId != null ? siteId : "default");

        // when
        processor.handleRequest(map, this.context);

        // then
        actions = actionsService.getActions(siteId, documentId);
        assertEquals(1, actions.size());
        assertEquals(ActionStatus.COMPLETE, actions.get(0).status());

        HttpRequest lastRequest = callback.getLastRequest();
        assertTrue(lastRequest.getPath().toString().endsWith("/callback"));
        Map<String, Object> resultmap = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
        List<Map<String, String>> documents =
            (List<Map<String, String>>) resultmap.get("documents");
        assertEquals(1, documents.size());

        if (siteId != null) {
          assertEquals(siteId, documents.get(0).get("siteId"));
        } else {
          assertEquals("default", documents.get(0).get("siteId"));
        }

        assertEquals(documentId, documents.get(0).get("documentId"));
        assertEquals("application/pdf", documents.get(0).get("contentType"));
        assertEquals("joe", documents.get(0).get("userId"));
        assertNotNull(documents.get(0).get("insertedDate"));
        assertNotNull(documents.get(0).get("lastModifiedDate"));
        assertNotNull(documents.get(0).get("url"));

        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());
      }
    }
  }

  /**
   * Handle WEBHOOK + ANTIVIRUS Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle06() throws IOException, URISyntaxException {
    try (DynamoDbClient db = dbBuilder.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        String documentId = UUID.randomUUID().toString();

        DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
        Collection<DocumentTag> tags = Arrays.asList(
            new DocumentTag(documentId, "CLAMAV_SCAN_STATUS", "CLEAN", new Date(), "joe",
                DocumentTagType.SYSTEMDEFINED),
            new DocumentTag(documentId, "CLAMAV_SCAN_TIMESTAMP", "2022-01-01", new Date(), "joe",
                DocumentTagType.SYSTEMDEFINED));
        documentService.saveDocument(siteId, item, tags);

        List<Action> actions = Arrays.asList(
            new Action().type(ActionType.ANTIVIRUS).userId("joe").status(ActionStatus.COMPLETE),
            new Action().type(ActionType.WEBHOOK).userId("joe")
                .parameters(Map.of("url", URL + "/callback")));
        actionsService.saveNewActions(siteId, documentId, actions);

        Map<String, Object> map =
            loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
                documentId, "default", siteId != null ? siteId : "default");

        // when
        processor.handleRequest(map, this.context);

        // then
        actions = actionsService.getActions(siteId, documentId);
        assertEquals(2, actions.size());
        assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
        assertEquals(ActionStatus.COMPLETE, actions.get(1).status());

        HttpRequest lastRequest = callback.getLastRequest();
        assertTrue(lastRequest.getPath().toString().endsWith("/callback"));
        Map<String, Object> resultmap = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
        List<Map<String, String>> documents =
            (List<Map<String, String>>) resultmap.get("documents");
        assertEquals(1, documents.size());

        if (siteId != null) {
          assertEquals(siteId, documents.get(0).get("siteId"));
        } else {
          assertEquals("default", documents.get(0).get("siteId"));
        }

        assertEquals(documentId, documents.get(0).get("documentId"));
        assertEquals("CLEAN", documents.get(0).get("status"));
        assertEquals("2022-01-01", documents.get(0).get("timestamp"));

        Action action = actionsService.getActions(siteId, documentId).get(1);
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());
      }
    }
  }

  /**
   * Handle Fulltext(Typesense) plain/text document.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle07() throws IOException, URISyntaxException {
    initProcessor("typesense", "chatgpt1");

    String content = "this is some data";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      HttpRequest lastRequest = callback.getLastRequest();
      assertNull(lastRequest);

      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      HttpResponse<String> response = typesense.getDocument(siteId, documentId);
      assertEquals("200", String.valueOf(response.statusCode()));

      Map<String, String> body = gson.fromJson(response.body(), Map.class);
      assertEquals(documentId, body.get("id"));
      assertEquals(content, body.get("content"));
    }
  }

  /**
   * Handle RUNNING action in progress.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testHandle08() throws IOException, URISyntaxException {
    try (DynamoDbClient db = dbBuilder.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        String documentId = UUID.randomUUID().toString();

        DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
        item.setContentType("application/pdf");
        documentService.saveDocument(siteId, item, null);

        List<Action> actions = Arrays.asList(
            new Action().status(ActionStatus.RUNNING).type(ActionType.WEBHOOK).userId("joe")
                .parameters(Map.of("url", URL + "/callback")),
            new Action().type(ActionType.WEBHOOK).userId("joe")
                .parameters(Map.of("url", URL + "/callback2")));
        actionsService.saveNewActions(siteId, documentId, actions);

        Map<String, Object> map =
            loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
                documentId, "default", siteId != null ? siteId : "default");

        // when
        processor.handleRequest(map, this.context);

        // then
        actions = actionsService.getActions(siteId, documentId);
        assertEquals(2, actions.size());
        assertEquals(ActionStatus.RUNNING, actions.get(0).status());
        assertEquals(ActionStatus.PENDING, actions.get(1).status());

        HttpRequest lastRequest = callback.getLastRequest();
        assertNull(lastRequest);
      }
    }
  }

  /**
   * Handle FAILED and PENDING action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testHandle09() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions = Arrays.asList(
          new Action().status(ActionStatus.FAILED).type(ActionType.WEBHOOK).userId("joe")
              .parameters(Map.of("url", URL + "/callback")),
          new Action().type(ActionType.WEBHOOK).userId("joe")
              .parameters(Map.of("url", URL + "/callback2")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      actions = actionsService.getActions(siteId, documentId);

      assertEquals(2, actions.size());
      Action action = actions.get(0);
      assertEquals(ActionStatus.FAILED, action.status());
      assertNull(action.startDate());
      assertNotNull(action.insertedDate());

      action = actions.get(1);
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());

      HttpRequest lastRequest = callback.getLastRequest();
      assertNotNull(lastRequest);
    }
  }

  /**
   * Handle Fulltext that needs OCR Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testHandleFulltext01() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<Action> list = actionsService.getActions(siteId, documentId);
      assertEquals(2, list.size());
      assertEquals(ActionType.OCR, list.get(0).type());
      assertEquals("{ocrEngine=tesseract}", list.get(0).parameters().toString());
      assertEquals(ActionStatus.PENDING, list.get(0).status());
      assertEquals(ActionType.FULLTEXT, list.get(1).type());
      assertEquals(ActionStatus.PENDING, list.get(1).status());
    }
  }


  /**
   * Handle Fulltext that needs OCR Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testHandleFulltext02() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.OCR).status(ActionStatus.COMPLETE).userId("joe"),
          new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      List<Action> list = actionsService.getActions(siteId, documentId);
      assertEquals(2, list.size());
      assertEquals(ActionType.OCR, list.get(0).type());
      assertNull(list.get(0).parameters());
      assertEquals(ActionStatus.COMPLETE, list.get(0).status());
      assertNull(list.get(0).message());
      assertEquals(ActionType.FULLTEXT, list.get(1).type());
      assertEquals(ActionStatus.FAILED, list.get(1).status());
      assertEquals("no OCR document found", list.get(1).message());
    }
  }
}
