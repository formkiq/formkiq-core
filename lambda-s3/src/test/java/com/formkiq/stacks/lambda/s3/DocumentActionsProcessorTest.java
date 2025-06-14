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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.eventbridge.EventBridgeAwsServiceRegistry;
import com.formkiq.aws.eventbridge.EventBridgeService;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceImpl;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
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
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceDynamoDb;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.AttributeType;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.config.SiteConfigurationGoogle;
import com.formkiq.stacks.dynamodb.documents.DocumentPublicationRecord;
import com.formkiq.stacks.dynamodb.mappings.Mapping;
import com.formkiq.stacks.dynamodb.mappings.MappingAttribute;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeLabelMatchingType;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeMetadataField;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeSourceType;
import com.formkiq.stacks.dynamodb.mappings.MappingService;
import com.formkiq.stacks.dynamodb.mappings.MappingServiceDynamodb;
import com.formkiq.stacks.lambda.s3.actions.AddOcrAction;
import com.formkiq.stacks.lambda.s3.util.FileUtils;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import joptsimple.internal.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.utils.IoUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.model.DocumentSyncRecordBuilder.MESSAGE_ADDED_METADATA;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentActionsProcessorTest implements DbKeys {

  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static final RequestRecordExpectationResponseCallback CALLBACK =
      new RequestRecordExpectationResponseCallback();
  /** {@link AwsBasicCredentials}. */
  private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create("asd", "asd");
  /** Full text 404 document. */
  private static final String DOCUMENT_ID_404 = ID.uuid();
  /** Document Id with OCR. */
  private static final String DOCUMENT_ID_OCR = ID.uuid();
  /** Document Id with OCR Key/Value. */
  private static final String DOCUMENT_ID_OCR_KEY_VALUE = ID.uuid();
  /** {@link Gson}. */
  private static final Gson GSON = GsonUtil.getInstance();
  /** Port to run Test server. */
  private static final int PORT = 8888;
  /** Sns Document Event. */
  private static final String SNS_DOCUMENT_EVENT_TOPIC = "SNS_DOCUMENT_EVENT";
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;
  /** Search Limit. */
  private static final int LIMIT = 100;
  /** Test Timeout. */
  private static final int TEST_TIMEOUT = 10;
  /** {@link TypesenseExtension}. */
  @RegisterExtension
  static TypesenseExtension typesenseExtension = new TypesenseExtension();
  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** {@link MappingService}. */
  private static MappingService mappingService;
  /** {@link ConfigService}. */
  private static ConfigService configService;
  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** {@link DocumentService}. */
  private static DocumentSyncService documentSyncService;
  /** {@link DocumentSearchService}. */
  private static DocumentSearchService documentSearchService;
  /** {@link ClientAndServer}. */
  private static ClientAndServer mockServer;
  /** {@link DocumentActionsProcessor}. */
  private static DocumentActionsProcessor processor;
  /** {@link S3Service}. */
  private static S3Service s3Service;
  /** Sns Document Event Topic Arn. */
  private static String snsDocumentEventTopicArn;
  /** {@link TypeSenseService}. */
  private static TypeSenseService typesense;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;
  /** {@link EventBridgeService}. */
  private static EventBridgeService eventBridgeService;
  /** {@link SqsService}. */
  private static SqsService sqsService;
  /** {@link SnsService}. */
  private static SnsService sns;
  /** Valid image formats. */
  private static final List<String> VALID_IMAGE_FORMATS =
      List.of("bmp", "gif", "jpeg", "png", "tif");

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

    ApiAuthorization.login(new ApiAuthorization().username("System"));

    DynamoDbConnectionBuilder dbBuilder = DynamoDbTestServices.getDynamoDbConnection();
    DynamoDbService db = new DynamoDbServiceImpl(dbBuilder, DOCUMENTS_TABLE);
    DocumentVersionService versionService = new DocumentVersionServiceNoVersioning();

    documentService =
        new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE, versionService);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    mappingService = new MappingServiceDynamodb(db);
    attributeService = new AttributeServiceDynamodb(db);
    documentSearchService =
        new DocumentSearchServiceImpl(dbBuilder, documentService, DOCUMENTS_TABLE);
    documentSyncService =
        new DocumentSyncServiceDynamoDb(db, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE);
    createMockServer();

    s3Service = new S3Service(TestServices.getS3Connection(null));
    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection(null);

    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    String typeSenseHost = "http://localhost:" + typesenseExtension.getFirstMappedPort();
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/TypesenseEndpoint",
        typeSenseHost);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/typesense/ApiKey", API_KEY);

    typesense = new TypeSenseServiceImpl(typeSenseHost, API_KEY, Region.US_EAST_1, CREDENTIALS);

    configService = new ConfigServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);

    sns = new SnsService(TestServices.getSnsConnection(null));
    snsDocumentEventTopicArn = sns.createTopic(SNS_DOCUMENT_EVENT_TOPIC).topicArn();

    SqsConnectionBuilder sqsBuilder = TestServices.getSqsConnection(null);
    sqsService = new SqsServiceImpl(sqsBuilder);
  }

  /**
   * Create Mock Server.
   *
   * @throws IOException IOException
   */
  private static void createMockServer() throws IOException {

    mockServer = startClientAndServer(PORT);

    final int status = 200;

    for (String item : Arrays.asList("1", "2", "3", "4", "5", "6")) {
      String text = FileUtils.loadFile(mockServer, "/chatgpt/response" + item + ".json");
      mockServer.when(request().withMethod("POST").withPath("/chatgpt" + item))
          .respond(org.mockserver.model.HttpResponse.response(text).withStatusCode(status));
    }

    final int notFound = 404;
    mockServer
        .when(request().withMethod("PATCH").withPath("/documents/" + DOCUMENT_ID_404 + "/fulltext"))
        .respond(org.mockserver.model.HttpResponse.response("").withStatusCode(notFound));

    mockServer
        .when(request().withMethod("POST").withPath("/documents/" + DOCUMENT_ID_404 + "/fulltext"))
        .respond(CALLBACK);

    mockServer.when(request().withMethod("GET").withPath("/documents/" + DOCUMENT_ID_OCR + "/ocr*"))
        .respond(org.mockserver.model.HttpResponse
            .response("{\"contentUrls\":[\"" + URL + "/" + DOCUMENT_ID_OCR + "\"]}"));

    addKeyValueOcrMock();

    mockServer.when(request().withMethod("PATCH")).respond(CALLBACK);
    mockServer.when(request().withMethod("POST")).respond(CALLBACK);
    mockServer.when(request().withMethod("PUT")).respond(CALLBACK);
    mockServer.when(request().withMethod("GET")).respond(CALLBACK);
  }

  private static void addKeyValueOcrMock() {
    mockServer
        .when(request().withMethod("GET")
            .withPath("/documents/" + DOCUMENT_ID_OCR_KEY_VALUE + "/ocr*"))
        .respond(org.mockserver.model.HttpResponse.response("""
            {"ocrEngine": "TEXTRACT","ocrStatus": "SUCCESSFUL","keyValues":
                [
                    {
                        "key": "Date",
                        "values": ["07/21/2024"]
                    },
                    {
                        "key": "Customer first name",
                        "values": ["John"]
                    },
                    {
                        "key": "City",
                        "values": ["Los Angeles", "New York"]
                    },
                    {
                        "key": "Anthem plan code (numbers found on ID card)",
                        "values": ["987654321"]
                    },
                    {
                        "key": "4. Customer certificate or ID no.",
                        "values": ["28937423"]
                    },
                    {
                        "key": "25. Total charges",
                        "values": ["$150"]
                    },
                    {
                        "key": "Age",
                        "values": ["54"]
                    }
                ],
                "contentType": "text/plain","userId": "joe"}"""));
  }

  private static void initProcessor(final String module, final String chatgptUrl,
      final String eventBusName) {
    Map<String, String> env = buildEnvironment(module, chatgptUrl, eventBusName);

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    serviceCache =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SsmAwsServiceRegistry(),
                new SesAwsServiceRegistry(), new EventBridgeAwsServiceRegistry())
            .build();

    processor = new DocumentActionsProcessor(serviceCache);
    eventBridgeService = serviceCache.getExtension(EventBridgeService.class);
  }

  private static Map<String, String> buildEnvironment(final String module, final String chatgptUrl,
      final String eventBusName) {
    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", AWS_REGION.toString());
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
    env.put("MODULE_" + module, "true");
    env.put("SNS_DOCUMENT_EVENT", snsDocumentEventTopicArn);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    env.put("CHATGPT_API_COMPLETIONS_URL", URL + "/" + chatgptUrl);
    env.put("DOCUMENT_EVENTS_BUS", eventBusName);
    return env;
  }

  /**
   * BeforeEach.
   *
   */
  @BeforeEach
  public void beforeEach() {
    // null = new LambdaContextRecorder();
    CALLBACK.reset();

    initProcessor("opensearch", "chatgpt1", null);
    s3Service.deleteAllFiles(BUCKET_NAME);
  }

  /**
   * Handle documentTagging ChatApt Action missing GptKey.
   *
   */
  @Test
  public void testDocumentTaggingAction01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      List<Action> list = actionsService.getActions(siteId, documentId);
      assertEquals(1, list.size());
      assertEquals(ActionStatus.FAILED, list.get(0).status());
      assertEquals("missing config 'ChatGptApiKey'", list.get(0).message());
    }
  }

  /**
   * Handle documentTagging ChatApt Action.
   *
   */
  @Test
  public void testDocumentTaggingAction02() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      final int expectedSize = 6;
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

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Handle documentTagging invalid engine.
   *
   */
  @Test
  public void testDocumentTaggingAction03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "unknown", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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
   */
  @Test
  public void testDocumentTaggingAction04() {

    initProcessor("opensearch", "chatgpt2", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      final int expectedSize = 7;
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

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action with a non JSON repsonse from ChatGPT.
   *
   */
  @Test
  public void testDocumentTaggingAction05() {

    initProcessor("opensearch", "chatgpt3", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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
      assertEquals("New York, NY 12240,Cambutdigo, MA 12210",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Job Smith", tags.getResults().get(i++).getValue());

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("Frontend eaar brake cabies,New set of podal arms,Labor shrs 500",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action extra quotes.
   *
   */
  @Test
  public void testDocumentTaggingAction06() {

    initProcessor("opensearch", "chatgpt4", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      final int expectedSize = 5;
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

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action extra quotes.
   *
   */
  @Test
  public void testDocumentTaggingAction07() {

    initProcessor("opensearch", "chatgpt5", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(new Action().type(ActionType.DOCUMENTTAGGING)
          .userId("joe").parameters(Map.of("engine", "chatgpt", "tags",
              "document type,meeting date,chairperson,secretary,board members,resolutions")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      final int expectedSize = 7;
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

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Handle documentTagging ChatApt Action with a gpt-3.5-turbo-instruct model response.
   *
   */
  @Test
  public void testDocumentTaggingAction08() {

    initProcessor("opensearch", "chatgpt6", null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setChatGptApiKey("asd");
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      documentService.addTags(siteId, documentId, List.of(new DocumentTag(documentId, "untagged",
          "", new Date(), "joe", DocumentTagType.SYSTEMDEFINED)), null);

      List<Action> actions = Collections.singletonList(
          new Action().type(ActionType.DOCUMENTTAGGING).userId("joe").parameters(Map.of("engine",
              "chatgpt", "tags", "Organization,location,person,subject,sentiment,document type")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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

      assertEquals("location", tags.getResults().get(i).getKey());
      assertEquals("New York, NY,Cambutdigo, MA",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("person", tags.getResults().get(i).getKey());
      assertEquals("Job Smith", tags.getResults().get(i++).getValue());

      assertEquals("subject", tags.getResults().get(i).getKey());
      assertEquals("Receipt,Frontend eaar brake cabies,New set of podal arms,Labor shrs",
          String.join(",", tags.getResults().get(i++).getValues()));

      assertEquals("untagged", tags.getResults().get(i).getKey());
      assertEquals("", tags.getResults().get(i).getValue());
    }
  }

  /**
   * Test converting Ocr Parse Types.
   */
  @Test
  public void testGetOcrParseTypes01() {
    AddOcrAction ocrAction = new AddOcrAction(serviceCache);
    assertEquals("[TEXT]", ocrAction.getOcrParseTypes(new Action()).toString());

    // invalid
    Map<String, String> parameters = Map.of("ocrParseTypes", "ADAD,IUJK");
    assertEquals("[ADAD, IUJK]",
        ocrAction.getOcrParseTypes(new Action().parameters(parameters)).toString());

    parameters = Map.of("ocrParseTypes", "tEXT, forms, TABLES");
    assertEquals("[TEXT, FORMS, TABLES]",
        ocrAction.getOcrParseTypes(new Action().parameters(parameters)).toString());
  }

  /**
   * Handle OCR Action.
   *
   */
  @Test
  public void testHandle01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      List<Action> actions = Collections.singletonList(new Action().type(ActionType.OCR)
          .userId("joe").parameters(Map.of("addPdfDetectedCharactersAsText", "true",
              "ocrNumberOfPages", "2", "ocrOutputType", "CSV")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/ocr"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertEquals("[TEXT]", resultmap.get("parseTypes").toString());
      assertEquals("true", resultmap.get("addPdfDetectedCharactersAsText").toString());
      assertEquals("2", resultmap.get("ocrNumberOfPages").toString());
      assertEquals("CSV", resultmap.get("ocrOutputType").toString());

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
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/fulltext"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
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
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle03() throws ValidationException {

    // given
    String documentId = DOCUMENT_ID_OCR;

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/fulltext"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
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
   */
  @Test
  public void testHandle04() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle05() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions = Collections.singletonList(new Action().type(ActionType.WEBHOOK)
          .userId("joe").parameters(Map.of("url", URL + "/callback")));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());

      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/callback"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
      List<Map<String, String>> documents = (List<Map<String, String>>) resultmap.get("documents");
      assertEquals(1, documents.size());

      assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID),
          documents.get(0).get("siteId"));

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

  /**
   * Handle WEBHOOK + ANTIVIRUS Action.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle06() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

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
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(2, actions.size());
      assertEquals(ActionStatus.COMPLETE, actions.get(0).status());
      assertEquals(ActionStatus.COMPLETE, actions.get(1).status());

      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/callback"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
      List<Map<String, String>> documents = (List<Map<String, String>>) resultmap.get("documents");
      assertEquals(1, documents.size());

      assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID),
          documents.get(0).get("siteId"));

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

  /**
   * Handle Fulltext(Typesense) plain/text document.
   * 
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle07() throws IOException, ValidationException {
    initProcessor("typesense", "chatgpt1", null);

    String content = "this is some data";

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertNull(lastRequest);

      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());

      HttpResponse<String> response = typesense.getDocument(siteId, documentId);
      assertEquals("200", String.valueOf(response.statusCode()));

      Map<String, String> body = GSON.fromJson(response.body(), Map.class);
      assertEquals(documentId, body.get("id"));
      assertEquals(content, body.get("content"));
    }
  }

  /**
   * Handle RUNNING action in progress.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle08() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

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
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(2, actions.size());
      assertEquals(ActionStatus.RUNNING, actions.get(0).status());
      assertEquals(ActionStatus.PENDING, actions.get(1).status());

      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertNull(lastRequest);
    }
  }

  /**
   * Handle FAILED and PENDING action.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandle09() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

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
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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

      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertNotNull(lastRequest);
    }
  }

  /**
   * Handle Fulltext that needs OCR Action.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandleFulltext01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandleFulltext02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions = Arrays.asList(
          new Action().type(ActionType.OCR).status(ActionStatus.COMPLETE).userId("joe"),
          new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

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


  /**
   * Handle Fulltext(Opensearch) plain/text PATCH document not found 404, POST works.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandleFulltext03() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = DOCUMENT_ID_404;

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.FULLTEXT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/fulltext"));
      Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertNotNull(resultmap.get("contentUrls").toString());

      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());
    }
  }

  /**
   * Handle Invalid Request.
   *
   */
  @Test
  public void testInvalidRequest() {
    // given
    Map<String, Object> map = new HashMap<>();

    // when
    processor.handleRequest(map, null);

    // then
  }

  /**
   * Handle Queue Action.
   *
   */
  @Test
  public void testQueueAction01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      String name = "testqueue#" + documentId;

      List<Action> actions = Collections
          .singletonList(new Action().type(ActionType.QUEUE).userId("joe").queueId(name));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      assertEquals(ActionStatus.IN_QUEUE,
          actionsService.getActions(siteId, documentId).get(0).status());
      assertSyncsCount(siteId, documentId, 0);
    }
  }

  private String addTextToBucket(final String siteId, final String text) {
    String documentId = ID.uuid();

    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
    s3Service.putObject(BUCKET_NAME, s3Key, text.getBytes(StandardCharsets.UTF_8), "text/plain");

    return documentId;
  }

  private String addPdfToBucket(final String siteId) {

    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, DOCUMENT_ID_OCR_KEY_VALUE);
    s3Service.putObject(BUCKET_NAME, s3Key, "abc".getBytes(StandardCharsets.UTF_8),
        "application/pdf");

    return DocumentActionsProcessorTest.DOCUMENT_ID_OCR_KEY_VALUE;
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute STRING_VALUE.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp01() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
            null);

        Mapping mapping =
            createMapping("invoice", "P.O. Number", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        // when
        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());
        assertEquals("6200041751", record.getStringValue());

        List<DocumentSyncRecord> syncs = assertSyncsCount(siteId, documentId, 2);
        assertSyncEquals(syncs.get(0), "updated Document Metadata");
        assertSyncEquals(syncs.get(1), MESSAGE_ADDED_METADATA);
      }
    }
  }

  private void assertSyncEquals(final DocumentSyncRecord sync, final String message) {
    assertEquals(DocumentSyncServiceType.EVENTBRIDGE, sync.getService());
    assertNull(sync.getSyncDate());
    assertNotNull(sync.getInsertedDate());
    assertEquals(DocumentSyncType.METADATA, sync.getType());
    assertEquals(message, sync.getMessage());
    assertEquals("System", sync.getUserId());
    assertEquals(DocumentSyncStatus.PENDING, sync.getStatus());
  }

  private List<DocumentSyncRecord> assertSyncsCount(final String siteId, final String documentId,
      final int expected) {
    List<DocumentSyncRecord> syncs =
        notNull(documentSyncService.getSyncs(siteId, documentId, null, 2).getResults());
    assertEquals(expected, syncs.size());
    return syncs;
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute NUMBER_VALUE.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp02() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (FileInputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.NUMBER, null);

        Mapping mapping =
            createMapping("invoice", "P.O. Number", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());
        assertEquals("6.200041751E9", record.getNumberValue().toString());

        List<DocumentSyncRecord> syncs = assertSyncsCount(siteId, documentId, 2);
        assertSyncEquals(syncs.get(0), "updated Document Metadata");
        assertSyncEquals(syncs.get(1), MESSAGE_ADDED_METADATA);
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute missing.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp03() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (FileInputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.NUMBER, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(0, results.size());

        List<DocumentSyncRecord> syncs = assertSyncsCount(siteId, documentId, 1);
        assertSyncEquals(syncs.get(0), MESSAGE_ADDED_METADATA);
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute default value.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp04() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.STRING, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, "somevalue", null, null);

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());
        assertEquals("somevalue", record.getStringValue());
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute default values.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp05() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.STRING, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, null, Arrays.asList("123", "abc"), null);

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(2, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());
        assertEquals("123", record.getStringValue());

        record = results.get(1);
        assertEquals("invoice", record.getKey());
        assertEquals("abc", record.getStringValue());

        List<DocumentSyncRecord> syncs = assertSyncsCount(siteId, documentId, 2);
        assertSyncEquals(syncs.get(0), "updated Document Metadata");
        assertSyncEquals(syncs.get(1), MESSAGE_ADDED_METADATA);
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, MappingAttributeLabelMatchingType contains.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp06() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
            null);

        Mapping mapping =
            createMapping("invoice", "P.O. NO.:", MappingAttributeLabelMatchingType.CONTAINS,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());
        assertEquals("6200041751", record.getStringValue());
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, MappingAttributeLabelMatchingType begins with,
   * METADATA.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp07() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text01.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "path", null, null);

        Mapping mapping = createMapping("path", "j", MappingAttributeLabelMatchingType.BEGINS_WITH,
            MappingAttributeSourceType.METADATA, null, null,
            MappingAttributeMetadataField.USERNAME);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("path", record.getKey());
        assertEquals("joe", record.getStringValue());
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute STRING_VALUE.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp08() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String text = """
          From:
          DEMO - Sliced Invoices
          Order Number 12345
          Invoice Number INV-3337
          123 Somewhere Street Your City AZ 12345 admin@slicedinvoices.com""";

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
          null);

      for (String validationRegex : Arrays.asList(null, "INV-\\d+")) {

        String documentId = addTextToBucket(siteId, text);

        Mapping mapping =
            createMapping("invoice", "invoice", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        mapping.getAttributes().get(0).setValidationRegex(validationRegex);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("invoice", record.getKey());

        if (validationRegex != null) {
          assertEquals("INV-3337", record.getStringValue());
        } else {
          assertEquals("Number", record.getStringValue());
        }
      }
    }
  }

  /**
   * Handle Idp with Mapping Action text/plain, Attribute STRING_VALUE.
   *
   * @throws IOException IOException
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp09() throws IOException, ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      try (InputStream is = new FileInputStream("src/test/resources/text/text02.txt")) {
        String text = IoUtils.toUtf8String(is);
        String documentId = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId,
            "certificate_number", null, null);

        Mapping mapping = createMapping("certificate_number", "Customer certificate",
            MappingAttributeLabelMatchingType.FUZZY, MappingAttributeSourceType.CONTENT, null, null,
            null);
        mapping.getAttributes().get(0).setValidationRegex("\\d+");

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, documentId, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("certificate_number", record.getKey());
        assertEquals("100232", record.getStringValue());
      }
    }
  }

  /**
   * Handle Idp with Mapping Action application/pdf and CONTENT_KEY_VALUE FUZZY.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp10() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = addPdfToBucket(siteId);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", "Customer certificate",
          MappingAttributeLabelMatchingType.FUZZY, MappingAttributeSourceType.CONTENT_KEY_VALUE,
          null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      // run twice
      for (int i = 0; i < 2; i++) {
        processIdpRequest(siteId, documentId, "application/pdf", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, documentId).get(0);
        assertNull(action.message());
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertEquals(ActionType.IDP, action.type());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        List<DocumentAttributeRecord> results =
            documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
        assertEquals(1, results.size());
        DocumentAttributeRecord record = results.get(0);
        assertEquals("certificate_number", record.getKey());
        assertEquals("28937423", record.getStringValue());
      }
    }
  }

  /**
   * Handle Idp with Mapping Action application/pdf and CONTENT_KEY_VALUE EXACT.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp11() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = addPdfToBucket(siteId);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping =
          createMapping("certificate_number", "Age", MappingAttributeLabelMatchingType.EXACT,
              MappingAttributeSourceType.CONTENT_KEY_VALUE, null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, documentId, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertNull(action.message());
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertEquals(ActionType.IDP, action.type());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());

      List<DocumentAttributeRecord> results =
          documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
      assertEquals(1, results.size());
      DocumentAttributeRecord record = results.get(0);
      assertEquals("certificate_number", record.getKey());
      assertEquals("54", record.getStringValue());
    }
  }

  /**
   * Handle Idp with Mapping Action application/pdf and CONTENT_KEY_VALUE, EXACT missing.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp12() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = addPdfToBucket(siteId);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", "Customer certificate",
          MappingAttributeLabelMatchingType.EXACT, MappingAttributeSourceType.CONTENT_KEY_VALUE,
          null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, documentId, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertNull(action.message());
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertEquals(ActionType.IDP, action.type());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());

      List<DocumentAttributeRecord> results =
          documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();
      assertEquals(0, results.size());
    }
  }

  /**
   * Handle Idp with Mapping Action application/pdf and SourceType MANUAL.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp13() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = addPdfToBucket(siteId);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", null, null,
          MappingAttributeSourceType.MANUAL, "123", List.of("111", "222"), null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, documentId, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertNull(action.message());
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertEquals(ActionType.IDP, action.type());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());

      List<DocumentAttributeRecord> results =
          documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();

      final int expected = 3;
      assertEquals(expected, results.size());

      int i = 0;
      DocumentAttributeRecord record = results.get(i++);
      assertEquals("certificate_number", record.getKey());
      assertEquals("111", record.getStringValue());

      record = results.get(i++);
      assertEquals("certificate_number", record.getKey());
      assertEquals("123", record.getStringValue());

      record = results.get(i);
      assertEquals("certificate_number", record.getKey());
      assertEquals("222", record.getStringValue());
    }
  }

  /**
   * Handle Idp with Mapping Action application/pdf and SourceType MANUAL and dataonly attribute.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testIdp14() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = addPdfToBucket(siteId);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          AttributeDataType.KEY_ONLY, null);

      Mapping mapping = createMapping("certificate_number", null, null,
          MappingAttributeSourceType.MANUAL, null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, documentId, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertNull(action.message());
      assertEquals(ActionStatus.COMPLETE, action.status());
      assertEquals(ActionType.IDP, action.type());
      assertNotNull(action.startDate());
      assertNotNull(action.insertedDate());
      assertNotNull(action.completedDate());

      List<DocumentAttributeRecord> results =
          documentService.findDocumentAttributes(siteId, documentId, null, LIMIT).getResults();

      assertEquals(1, results.size());

      DocumentAttributeRecord record = results.get(0);
      assertEquals("certificate_number", record.getKey());
      assertNull(record.getStringValue());
    }
  }

  private void processIdpRequest(final String siteId, final String documentId,
      final String contentType, final MappingRecord mappingRecord) throws ValidationException {
    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    item.setContentType(contentType);
    documentService.saveDocument(siteId, item, null);

    List<Action> actions = Collections.singletonList(new Action().type(ActionType.IDP).userId("joe")
        .parameters(Map.of("mappingId", mappingRecord.getDocumentId())));
    actionsService.saveNewActions(siteId, documentId, actions);

    Map<String, Object> map =
        SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

    // when
    processor.handleRequest(map, null);
  }

  private Mapping createMapping(final String attributeKey, final String labelText,
      final MappingAttributeLabelMatchingType matchingType,
      final MappingAttributeSourceType sourceType, final String value, final List<String> values,
      final MappingAttributeMetadataField metadataField) {

    List<String> labelTexts = labelText != null ? List.of(labelText) : null;

    MappingAttribute a0 = new MappingAttribute().setAttributeKey(attributeKey)
        .setSourceType(sourceType).setLabelMatchingType(matchingType).setLabelTexts(labelTexts)
        .setDefaultValue(value).setDefaultValues(values).setMetadataField(metadataField);

    return new Mapping().setName("test").setAttributes(Collections.singletonList(a0));
  }

  /**
   * Handle Publish Action.
   *
   */
  @Test
  public void testPublishAction01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");
      documentService.saveDocument(siteId, item, null);

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      String content = "this is some data";
      s3Service.putObject(BUCKET_NAME, s3Key, content.getBytes(StandardCharsets.UTF_8),
          "text/plain");

      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.PUBLISH).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionType.PUBLISH, action.type());
      assertEquals(ActionStatus.COMPLETE, action.status());

      DocumentPublicationRecord pv = documentService.findPublishDocument(siteId, documentId);
      assertEquals(documentId, pv.getDocumentId());
      assertEquals("text/plain", pv.getContentType());
      assertEquals(documentId, pv.getPath());
      assertEquals("joe", pv.getUserId());
      assertNotNull(pv.getS3version());

      AttributeRecord attribute =
          attributeService.getAttribute(siteId, AttributeKeyReserved.PUBLICATION.getKey());
      assertNotNull(attribute);

      SearchAttributeCriteria attr =
          new SearchAttributeCriteria().key(AttributeKeyReserved.PUBLICATION.getKey());
      SearchQuery req = new SearchQuery().attribute(attr);
      List<DynamicDocumentItem> docs =
          notNull(documentSearchService.search(siteId, req, null, null, 2).getResults());
      assertEquals(1, docs.size());
    }
  }

  /**
   * Handle Pdf Export Action Google not configured.
   *
   */
  @Test
  public void testPdfExportAction01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.PDFEXPORT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionType.PDFEXPORT, action.type());
      assertEquals(ActionStatus.FAILED, action.status());
      assertEquals("Google Workload Identity is not configured", action.message());
    }
  }

  /**
   * Handle Pdf Export on non Google Deeplink Action.
   *
   */
  @Test
  public void testPdfExportAction02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setGoogle(new SiteConfigurationGoogle().setWorkloadIdentityAudience("abc")
          .setWorkloadIdentityServiceAccount("123"));
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("text/plain");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.PDFEXPORT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionType.PDFEXPORT, action.type());
      assertEquals(ActionStatus.FAILED, action.status());
      assertEquals("PdfExport only supports Google DeepLink", action.message());
    }
  }

  /**
   * Handle Pdf Export Action.
   *
   */
  @Test
  public void testPdfExportAction03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setGoogle(new SiteConfigurationGoogle().setWorkloadIdentityAudience("abc")
          .setWorkloadIdentityServiceAccount("123"));
      configService.save(siteId, siteConfig);

      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setDeepLinkPath(
          "https://docs.google.com/document/d/1Vtwhg36ViJVoO4VHTzHv-uMIpw1hqMR2ttB8EhxXHzA/edit");
      documentService.saveDocument(siteId, item, null);

      List<Action> actions =
          Collections.singletonList(new Action().type(ActionType.PDFEXPORT).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionType.PDFEXPORT, action.type());
      assertEquals(ActionStatus.COMPLETE, action.status());

      HttpRequest lastRequest = CALLBACK.getLastRequest();
      assertEquals("/integrations/google/drive/documents/" + documentId + "/export",
          lastRequest.getPath().getValue());
      assertEquals("{\"outputType\": \"PDF\"}", lastRequest.getBodyAsString());

      if (siteId != null) {
        assertEquals(siteId, lastRequest.getFirstQueryStringParameter("siteId"));
      }

      List<DocumentSyncRecord> syncs = assertSyncsCount(siteId, documentId, 1);
      assertSyncEquals(syncs.get(0), MESSAGE_ADDED_METADATA);
    }
  }

  /**
   * Handle Export Bridge Action.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testEventBridge01() throws Exception {
    // given
    String sqsDocumentQueueUrl = sqsService.createQueue("sqssnsCreate1" + ID.uuid()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentQueueUrl);
    String eventBusName = createEventBus(sqsQueueArn);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "category",
          AttributeDataType.STRING, AttributeType.STANDARD);

      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");

      DocumentAttributeRecord attr0 = new DocumentAttributeRecord().setDocumentId(documentId)
          .setUserId("joe").setKey("category").setStringValue("person").updateValueType();

      DocumentAttributeRecord attr1 = new DocumentAttributeRecord().setDocumentId(documentId)
          .setUserId("joe").setKey("category").setStringValue("other").updateValueType();

      List<DocumentAttributeRecord> attributes = List.of(attr0, attr1);
      documentService.saveDocument(siteId, item, null, attributes, new SaveDocumentOptions());

      List<Action> actions = Collections.singletonList(new Action().type(ActionType.EVENTBRIDGE)
          .parameters(Map.of("eventBusName", eventBusName)).userId("joe"));
      actionsService.saveNewActions(siteId, documentId, actions);

      Map<String, Object> map =
          SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

      // when
      processor.handleRequest(map, null);

      // then
      Action action = actionsService.getActions(siteId, documentId).get(0);
      assertEquals(ActionType.EVENTBRIDGE, action.type());
      assertEquals(ActionStatus.COMPLETE, action.status());

      Message message = getMessage(sqsDocumentQueueUrl);

      List<Map<String, Object>> documents =
          assertEventBridgeMessage(message, "Document Action Event");

      validateAttributes(documents);
    }
  }

  private String createEventBus(final String sqsQueueArn) {

    String eventBusName = "test_" + UUID.randomUUID();
    eventBridgeService.createEventBridge(eventBusName);

    String eventPattern = "{\"source\":[\"formkiq.test\"]}";
    eventBridgeService.createRule(eventBusName, "sqs", eventPattern, "test", sqsQueueArn);
    return eventBusName;
  }

  /**
   * Handle DynamoDb Create Metadata Sync Event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testDynamoDbSyncEvent01() throws Exception {
    // given
    String sqsDocumentQueueUrl = setupDynamoDbSync();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = createDocument(siteId);

      // when
      handleDynamodb(siteId, documentId);

      // then
      assertEquals(0, actionsService.getActions(siteId, documentId).size());

      List<DocumentSyncRecord> syncs = getDocumentSyncs(siteId, documentId);
      assertEquals(1, syncs.size());
      assertEventBridge(syncs.get(0), DocumentSyncType.METADATA);

      Message message = getMessage(sqsDocumentQueueUrl);

      assertEventBridgeMessage(message, "Document Create Metadata");
    }
  }

  /**
   * Handle DynamoDb Content Sync Event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testDynamoDbSyncEvent02() throws Exception {
    // given
    String sqsDocumentQueueUrl = setupDynamoDbSync();

    String sqsDocumentQueueUrl2 = sqsService.createQueue("sqssnsCreate2" + ID.uuid()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentQueueUrl2);
    sns.subscribe(snsDocumentEventTopicArn, "sqs", sqsQueueArn);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = createDocument(siteId);

      String contentType = "text/plain";
      Map<String, AttributeValue> attributes =
          Map.of("contentType", AttributeValue.fromS(contentType));
      documentService.updateDocument(siteId, documentId, attributes);

      // when
      handleDynamodb(siteId, documentId);

      // then
      assertEquals(0, actionsService.getActions(siteId, documentId).size());

      List<DocumentSyncRecord> syncs = getDocumentSyncs(siteId, documentId);
      assertEquals(2, syncs.size());
      assertEventBridge(syncs.get(0), DocumentSyncType.CONTENT);
      assertEventBridge(syncs.get(1), DocumentSyncType.METADATA);

      Message message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeMessage(message, "Document Create Content");

      message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeMessage(message, "Document Create Metadata");

      message = getMessage(sqsDocumentQueueUrl2);
      assertSnsMessage(message, "create", siteId);
    }
  }

  /**
   * Handle DynamoDb Delete Sync Event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testDynamoDbSyncEvent03() throws Exception {
    // given
    String sqsDocumentQueueUrl = setupDynamoDbSync();

    String sqsDocumentQueueUrl2 = sqsService.createQueue("sqssnsCreate2" + ID.uuid()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentQueueUrl2);
    sns.subscribe(snsDocumentEventTopicArn, "sqs", sqsQueueArn);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = createDocument(siteId);
      TimeUnit.MILLISECONDS.sleep(2);

      documentService.deleteDocument(siteId, documentId, false);

      // when
      handleDynamodb(siteId, documentId);

      // then
      assertEquals(0, actionsService.getActions(siteId, documentId).size());

      List<DocumentSyncRecord> syncs = getDocumentSyncs(siteId, documentId);
      assertEquals(2, syncs.size());
      assertEventBridge(syncs.get(0), DocumentSyncType.DELETE);
      assertEventBridge(syncs.get(1), DocumentSyncType.METADATA);

      Message message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeDeleteMessage(message, "Document Delete Metadata");

      message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeDeleteMessage(message, "Document Create Metadata");

      message = getMessage(sqsDocumentQueueUrl2);
      assertSnsMessage(message, "delete", siteId);
    }
  }

  /**
   * Handle DynamoDb Soft Delete Sync Event.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testDynamoDbSyncEvent04() throws Exception {
    // given
    String sqsDocumentQueueUrl = setupDynamoDbSync();

    String sqsDocumentQueueUrl2 = sqsService.createQueue("sqssnsCreate2" + ID.uuid()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentQueueUrl2);
    sns.subscribe(snsDocumentEventTopicArn, "sqs", sqsQueueArn);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String documentId = createDocument(siteId);
      TimeUnit.MILLISECONDS.sleep(2);
      documentService.deleteDocument(siteId, documentId, true);

      // when
      handleDynamodb(siteId, documentId);

      // then
      assertEquals(0, actionsService.getActions(siteId, documentId).size());

      List<DocumentSyncRecord> syncs = getDocumentSyncs(siteId, documentId);
      assertEquals(2, syncs.size());
      assertEventBridge(syncs.get(0), DocumentSyncType.SOFT_DELETE);
      assertEventBridge(syncs.get(1), DocumentSyncType.METADATA);

      Message message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeDeleteMessage(message, "Document Soft Delete Metadata");

      message = getMessage(sqsDocumentQueueUrl);
      assertEventBridgeDeleteMessage(message, "Document Create Metadata");

      message = getMessage(sqsDocumentQueueUrl2);
      assertSnsMessage(message, "softDelete", siteId);
    }
  }

  @Test
  public void testPath() throws IOException, ValidationException {
    Map<String, String> parameters = Map.of("width", "300", "height", "200", "path", "resized.png");
    testResizeTemplate(parameters, 300, 200, "png");
  }

  @Test
  public void testResizeWithFixedWidthAndHeight() throws IOException, ValidationException {
    Map<String, String> parameters = Map.of("width", "300", "height", "200");
    testResizeTemplate(parameters, 300, 200, "png");
  }

  @Test
  public void testResizeWithFixedWidthAndAutoHeight() throws IOException, ValidationException {
    Map<String, String> parameters = Map.of("width", "100", "height", "auto");
    testResizeTemplate(parameters, 100, 75, "png");
  }

  @Test
  public void testResizeWithAutoWidthAndFixedHeight() throws IOException, ValidationException {
    Map<String, String> parameters = Map.of("width", "auto", "height", "100");
    testResizeTemplate(parameters, 133, 100, "png");
  }

  @Test
  public void testResizeBmpToAllFormats() throws IOException, ValidationException {
    testResizeToAllFormatsTemplate("bmp", VALID_IMAGE_FORMATS);
  }

  @Test
  public void testResizeGifToAllFormats() throws IOException, ValidationException {
    testResizeToAllFormatsTemplate("gif", VALID_IMAGE_FORMATS);
  }

  @Test
  public void testResizeJpgToAllFormats() throws IOException, ValidationException {
    testResizeToAllFormatsTemplate("jpg", VALID_IMAGE_FORMATS);
  }

  @Test
  public void testResizePngToAllFormats() throws IOException, ValidationException {
    testResizeToAllFormatsTemplate("png", VALID_IMAGE_FORMATS);
  }

  @Test
  public void testResizeTifToAllFormats() throws IOException, ValidationException {
    testResizeToAllFormatsTemplate("tif", List.of("gif", "png", "tif"));
  }

  private void testResizeToAllFormatsTemplate(final String srcImageFormat,
      final List<String> resImageFormats) throws IOException, ValidationException {
    for (String resImageFormat : resImageFormats) {
      Map<String, String> parameters =
          Map.of("width", "300", "height", "200", "outputType", resImageFormat);
      testResizeTemplate(parameters, 300, 200, srcImageFormat);
    }
  }

  public void testResizeTemplate(final Map<String, String> parameters, final int expectedWidth,
      final int expectedHeight, final String srcImageFormat)
      throws IOException, ValidationException {
    // given
    String siteId = ID.uuid();
    String documentId = setupImageDocument(siteId, srcImageFormat);
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

    // when
    processResizeAction(siteId, documentId, parameters);

    // then
    verifySuccessfulResize(siteId, documentId, s3Key, expectedWidth, expectedHeight, srcImageFormat,
        parameters);
    removeAllS3Objects();
  }

  // Helper method to set up an image document for testing.
  private String setupImageDocument(final String siteId, final String imageFormat)
      throws IOException, ValidationException {
    String contentType = imageFormatToMimeType(imageFormat);
    String documentId = ID.uuid();

    // Save test image to S3 bucket
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
    try (InputStream is = new FileInputStream("src/test/resources/resize/input." + imageFormat)) {
      byte[] imageBytes = IoUtils.toByteArray(is);
      s3Service.putObject(BUCKET_NAME, s3Key, imageBytes, contentType);
    }

    // Save document metadata to DynamoDB
    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    item.setContentType(contentType);
    item.setPath("test." + imageFormat);
    documentService.saveDocument(siteId, item, null);

    return documentId;
  }

  private static String imageFormatToMimeType(final String format) {
    return "image/" + ("tif".equals(format) ? "tiff" : format);
  }

  // Helper method to create and process a resize action.
  private void processResizeAction(final String siteId, final String documentId,
      final Map<String, String> parameters) {
    List<Action> actions = Collections
        .singletonList(new Action().type(ActionType.RESIZE).userId("joe").parameters(parameters));
    actionsService.saveNewActions(siteId, documentId, actions);

    Map<String, Object> map =
        SqsEventBuilder.builder().siteId(siteId).documentId(documentId).build();

    processor.handleRequest(map, null);
  }

  // Helper method to verify successful resize operation.
  private void verifySuccessfulResize(final String siteId, final String documentId,
      final String s3Key, final int expectedWidth, final int expectedHeight,
      final String srcImageFormat, final Map<String, String> parameters) throws IOException {
    String imageKey = getResizedImageKey(s3Key);
    String resImageFormat = parameters.getOrDefault("outputType", srcImageFormat);

    verifyAction(siteId, documentId);
    verifyDocumentAttributes(siteId, documentId);
    verifyData(expectedWidth, expectedHeight, imageKey, resImageFormat);

    String path = parameters.getOrDefault("path",
        "test-" + expectedWidth + "x" + expectedHeight + "." + resImageFormat);
    verifyMetadata(expectedWidth, expectedHeight, imageKey, path);
  }

  private void verifyDocumentAttributes(final String siteId, final String documentId) {
    List<DocumentAttributeRecord> attributes =
        documentService.findDocumentAttributes(siteId, documentId, null, 2).getResults();
    assertEquals(1, attributes.size());
    DocumentAttributeRecord attribute = attributes.get(0);
    assertEquals("Relationships", attribute.getKey());
    assertTrue(attribute.getStringValue().startsWith("RENDITION#"));
  }

  private static String getResizedImageKey(final String s3Key) {
    // S3 bucket should contain 2 objects: original and resized
    List<S3Object> s3Objects = s3Service.listObjects(BUCKET_NAME, null).contents();
    assertEquals(2, s3Objects.size());

    // find original image key, and make it the first element in the list
    if (s3Objects.get(1).key().equals(s3Key)) {
      s3Objects = List.of(s3Objects.get(1), s3Objects.get(0));
    }

    // verify original image key
    assertEquals(s3Key, s3Objects.get(0).key());

    return s3Objects.get(1).key();
  }

  private static void verifyAction(final String siteId, final String documentId) {
    Action action = actionsService.getActions(siteId, documentId).get(0);

    assertEquals(ActionType.RESIZE, action.type());
    assertEquals(ActionStatus.COMPLETE, action.status());
    assertNotNull(action.startDate());
    assertNotNull(action.insertedDate());
    assertNotNull(action.completedDate());
  }

  private static void verifyData(final int expectedWidth, final int expectedHeight,
      final String imageKey, final String imageFormat) throws IOException {
    assertEquals(imageFormatToMimeType(imageFormat),
        s3Service.getObjectMetadata(BUCKET_NAME, imageKey, null).getContentType());

    byte[] resizedImage = s3Service.getContentAsBytes(BUCKET_NAME, imageKey);
    ByteArrayInputStream bais = new ByteArrayInputStream(resizedImage);
    BufferedImage bufferedImage = ImageIO.read(bais);

    assertEquals(expectedWidth, bufferedImage.getWidth());
    assertEquals(expectedHeight, bufferedImage.getHeight());
  }

  private static void verifyMetadata(final int expectedWidth, final int expectedHeight,
      final String imageKey, final String path) {
    String[] splitted = imageKey.split("/");
    DocumentItem item = documentService.findDocument(splitted[0], splitted[1]);

    assertEquals(path, item.getPath());
    assertEquals(Integer.toString(expectedWidth), item.getWidth());
    assertEquals(Integer.toString(expectedHeight), item.getHeight());
  }

  private void removeAllS3Objects() {
    s3Service.deleteAllFiles(BUCKET_NAME);
  }

  private Message getMessage(final String sqsDocumentQueueUrl) throws InterruptedException {
    ReceiveMessageResponse response = getReceiveMessageResponse(sqsDocumentQueueUrl);

    assertEquals(1, response.messages().size());
    return response.messages().get(0);
  }

  private void assertEventBridge(final DocumentSyncRecord sync, final DocumentSyncType syncType) {
    assertEquals(DocumentSyncServiceType.EVENTBRIDGE, sync.getService());
    assertEquals(syncType, sync.getType());
    assertEquals(DocumentSyncStatus.COMPLETE, sync.getStatus());
    assertNotNull(sync.getSyncDate());
  }

  private void assertSnsMessage(final Message m, final String eventType, final String siteId) {

    Map<String, String> map = GSON.fromJson(m.body(), Map.class);
    String message = map.get("Message");

    map = GSON.fromJson(message, Map.class);
    assertNotNull(map.get("documentId"));
    assertEquals(eventType, map.get("type"));
    assertNotNull(map.get("path"));

    assertTrue(map.get("url").contains("testbucket"));
    assertNull(map.get("content"));

    if (!"delete".equals(eventType) && !"softDelete".equals(eventType)) {
      assertNotNull(map.get("userId"));
    }

    assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID), map.get("siteId"));
  }

  private List<Map<String, Object>> assertEventBridgeMessage(final Message message,
      final String detailType) {
    Map<String, Object> data = GSON.fromJson(message.body(), Map.class);
    assertEquals("formkiq.test", data.get("source"));
    assertEquals(detailType, data.get("detail-type"));
    assertTrue(data.get("time").toString().endsWith("Z"));

    data = (Map<String, Object>) data.get("detail");
    List<Map<String, Object>> documents = (List<Map<String, Object>>) data.get("documents");
    assertEquals(1, documents.size());
    assertNotNull(documents.get(0).get("documentId"));
    assertNotNull(documents.get(0).get("url"));
    assertNotNull(documents.get(0).get("path"));

    return documents;
  }

  private void assertEventBridgeDeleteMessage(final Message message, final String detailType) {
    Map<String, Object> data = GSON.fromJson(message.body(), Map.class);
    assertEquals("formkiq.test", data.get("source"));
    assertEquals(detailType, data.get("detail-type"));
    assertTrue(data.get("time").toString().endsWith("Z"));

    data = (Map<String, Object>) data.get("detail");
    List<Map<String, Object>> documents = (List<Map<String, Object>>) data.get("documents");
    assertEquals(1, documents.size());
    assertNotNull(documents.get(0).get("documentId"));
    assertNull(documents.get(0).get("url"));
    assertNull(documents.get(0).get("path"));

  }

  private void handleDynamodb(final String siteId, final String documentId) {
    List<DocumentSyncRecord> syncs = getDocumentSyncs(siteId, documentId);

    for (DocumentSyncRecord sync : syncs) {
      String pk = SiteIdKeyGenerator.createDatabaseKey(siteId, "docs#" + documentId);

      Map<String, Object> map = DynamoDbStreamEventBuilder.builder().pk(pk).sk(sync.sk())
          .documentId(documentId).type(sync.getType().name()).build();

      processor.handleRequest(map, null);
    }
  }

  private List<DocumentSyncRecord> getDocumentSyncs(final String siteId, final String documentId) {
    return notNull(
        documentSyncService.getSyncs(siteId, documentId, null, MAX_RESULTS).getResults());
  }

  private String createDocument(final String siteId) throws ValidationException {
    String documentId = ID.uuid();
    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    documentService.saveDocument(siteId, item, null);
    return documentId;
  }

  private String setupDynamoDbSync() {
    String sqsDocumentQueueUrl = sqsService.createQueue("sqssnsCreate1" + ID.uuid()).queueUrl();
    String sqsQueueArn = sqsService.getQueueArn(sqsDocumentQueueUrl);
    String eventBusName = createEventBus(sqsQueueArn);

    initProcessor(null, null, eventBusName);
    return sqsDocumentQueueUrl;
  }

  private void validateAttributes(final List<Map<String, Object>> documents) {
    Collection<Map<String, Object>> attrList =
        (Collection<Map<String, Object>>) documents.get(0).get("attributes");
    assertEquals(1, attrList.size());

    Map<String, Object> attrMap = attrList.iterator().next();
    final int expected = 3;
    assertEquals(expected, attrMap.size());
    assertEquals("category", attrMap.get("key"));
    assertEquals("STRING", attrMap.get("valueType"));
    assertEquals("[other, person]", attrMap.get("stringValues").toString());
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
}
