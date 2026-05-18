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
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.eventbridge.EventBridgeAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ses.SesAwsServiceRegistry;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sns.SnsService;
import com.formkiq.aws.sns.SnsServiceImpl;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.dynamodb.GsonUtil;
import com.formkiq.stacks.lambda.s3.actions.MalwareScanResponse;
import com.formkiq.stacks.lambda.s3.actions.MalwareScanResult;
import com.formkiq.stacks.lambda.s3.event.AwsEvent;
import com.formkiq.stacks.lambda.s3.util.FileUtils;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpStatusCode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.formkiq.module.actions.ActionType.DATA_CLASSIFICATION;
import static com.formkiq.module.actions.ActionType.LLMPROMPT;
import static com.formkiq.module.actions.ActionType.METADATA_EXTRACTION;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentActionsMetaDataExtractionProcessorTest implements DbKeys {

  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static final RequestRecordExpectationResponseCallback CALLBACK =
      new RequestRecordExpectationResponseCallback(200, "{\"contentUrls\":[]}");
  /** {@link RequestRecordExpectationResponseCallback} 429. */
  private static final RequestRecordExpectationResponseCallback CALLBACK429 =
      new RequestRecordExpectationResponseCallback(429, "");
  /** Full text 404 document. */
  private static final String DOCUMENT_ID_404 = ID.uuid();
  /** Full text 429 document. */
  private static final String DOCUMENT_ID_429 = ID.uuid();
  /** Document Id with OCR. */
  private static final String DOCUMENT_ID_OCR = ID.uuid();
  /** Document Id for Data Classification. */
  private static final String DOCUMENT_ID_DATACLASSIFICATION = ID.uuid();
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
  /** {@link TypesenseExtension}. */
  @RegisterExtension
  static TypesenseExtension typesenseExtension = new TypesenseExtension();
  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** {@link ClientAndServer}. */
  private static ClientAndServer mockServer;
  /** {@link DocumentActionsProcessor}. */
  private static DocumentActionsProcessor processor;
  /** {@link S3Service}. */
  private static S3Service s3Service;
  /** Sns Document Event Topic Arn. */
  private static String snsDocumentEventTopicArn;

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
    new DynamoDbServiceImpl(dbBuilder, DOCUMENTS_TABLE);
    DocumentVersionService versionService = new DocumentVersionServiceNoVersioning();

    documentService = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, versionService);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    createMockServer();

    s3Service = new S3Service(TestServices.getS3Connection(null));
    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection(null);

    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    String typeSenseHost = "http://localhost:" + typesenseExtension.getFirstMappedPort();
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/TypesenseEndpoint",
        typeSenseHost);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/typesense/ApiKey", API_KEY);

    SnsService sns = new SnsServiceImpl(TestServices.getSnsConnection(null));
    snsDocumentEventTopicArn = sns.createTopic(SNS_DOCUMENT_EVENT_TOPIC).topicArn();
  }

  private static AwsEvent buildAwsEvent(final String siteId, final DocumentArtifact document) {
    return SqsEventBuilder.builder().siteId(siteId).documentId(document.documentId())
        .artifactId(document.artifactId()).build();
  }

  private static Map<String, String> buildEnvironment() {
    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", AWS_REGION.toString());
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
    env.put("MODULE_" + "opensearch", "true");
    env.put("SNS_DOCUMENT_EVENT", snsDocumentEventTopicArn);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());
    env.put("OPERATIONAL_MODE", "ACTIVE");
    return env;
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

    mockServer
        .when(request().withMethod("PATCH").withPath("/documents/" + DOCUMENT_ID_429 + "/fulltext"))
        .respond(CALLBACK429);

    mockServer
        .when(request().withMethod("PATCH").withPath("/documents/" + DOCUMENT_ID_404 + "/fulltext"))
        .respond(org.mockserver.model.HttpResponse.response("")
            .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()));

    mockServer
        .when(request().withMethod("POST").withPath("/documents/" + DOCUMENT_ID_404 + "/fulltext"))
        .respond(CALLBACK);

    mockServer.when(request().withMethod("GET").withPath("/documents/" + DOCUMENT_ID_OCR + "/ocr*"))
        .respond(org.mockserver.model.HttpResponse
            .response("{\"contentUrls\":[\"" + URL + "/" + DOCUMENT_ID_OCR + "\"]}"));

    Map<String, Object> dataClassification = Map.of("dataClassifications",
        List.of(Map.of("attributes", List.of(Map.of("key", "certificate_number", "value", "12"),
            Map.of("key", "certificate_number", "value", "12")))));
    mockServer
        .when(request().withMethod("GET")
            .withPath("/documents/" + DOCUMENT_ID_DATACLASSIFICATION + "/dataClassification*"))
        .respond(org.mockserver.model.HttpResponse.response(GSON.toJson(dataClassification)));

    Map<String, Object> metadataExtractions = Map.of("metadataExtractions",
        List.of(Map.of("attributes", List.of(Map.of("key", "certificate_number", "value", "12"),
            Map.of("key", "certificate_number", "value", "12")))));

    mockServer
        .when(request().withMethod("GET")
            .withPath("/documents/" + DOCUMENT_ID_DATACLASSIFICATION
                + "/metadataExtractionResults/Another%20Prompt"))
        .respond(org.mockserver.model.HttpResponse.response(GSON.toJson(metadataExtractions)));

    MalwareScanResponse malwareScanResponse =
        new MalwareScanResponse(List.of(new MalwareScanResult("CLEAN", "", "", "")));
    mockServer
        .when(request().withMethod("GET")
            .withPath("/documents/" + DOCUMENT_ID_DATACLASSIFICATION + "/malwareScan*"))
        .respond(org.mockserver.model.HttpResponse.response(GSON.toJson(malwareScanResponse)));

    addKeyValueOcrMock();

    mockServer.when(request().withMethod("PATCH")).respond(CALLBACK);
    mockServer.when(request().withMethod("POST")).respond(CALLBACK);
    mockServer.when(request().withMethod("PUT")).respond(CALLBACK);
    mockServer.when(request().withMethod("DELETE")).respond(CALLBACK);
    mockServer.when(request().withMethod("GET")).respond(CALLBACK);
  }

  private static void initProcessor() {
    Map<String, String> env = buildEnvironment();

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    AwsServiceCache serviceCache =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SsmAwsServiceRegistry(),
                new SesAwsServiceRegistry(), new EventBridgeAwsServiceRegistry())
            .build();

    processor = new DocumentActionsProcessor(serviceCache);
  }

  /**
   * BeforeEach.
   *
   */
  @BeforeEach
  public void beforeEach() {
    // null = new LambdaContextRecorder();
    CALLBACK.reset();

    initProcessor();
    s3Service.deleteAllFiles(BUCKET_NAME);
  }

  private DocumentArtifact createDocument(final String siteId, final DocumentArtifact document,
      final String contentType) {
    DocumentItem item = new DocumentItemDynamoDb(document.documentId(), new Date(), "joe");
    item.setContentType(contentType);
    documentService.saveDocument(siteId, item, null);
    return document;
  }

  private DocumentArtifact createDocument(final String siteId, final String contentType) {
    DocumentArtifact document = DocumentArtifact.of(ID.uuid(), null);
    return createDocument(siteId, document, contentType);
  }

  /**
   * Handle Data_Classification / METADATA_EXTRACTION Action.
   *
   */
  @Test
  public void testHandleDataClassification01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentArtifact document = createDocument(siteId, "text/plain");

      for (ActionType type : List.of(DATA_CLASSIFICATION, METADATA_EXTRACTION, LLMPROMPT)) {

        Map<String, Object> parameters = new HashMap<>(Map.of("llmPromptEntityName", "My prompt"));
        if (LLMPROMPT.equals(type)) {
          parameters.put("modelId", "us.amazon.nova-2-lite-v1:0");
        }

        List<Action> actions = List.of(new ActionBuilder().type(type).userId("joe")
            .parameters(parameters).document(document).indexUlid().build(siteId));
        actionsService.saveNewActions(actions);

        AwsEvent map = buildAwsEvent(siteId, document);

        // when
        processor.handleRequest(map, null);

        // then
        HttpRequest lastRequest = CALLBACK.getLastRequest();

        if (DATA_CLASSIFICATION.equals(type)) {
          assertTrue(lastRequest.getPath().toString().endsWith("/dataClassification"));
          assertEquals("PUT", lastRequest.getMethod().getValue());

          Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
          assertEquals("My prompt", resultmap.get("llmPromptEntityName").toString());

        } else if (METADATA_EXTRACTION.equals(type)) {
          assertEquals("POST", lastRequest.getMethod().getValue());
          assertTrue(lastRequest.getPath().toString().endsWith(
              "/documents/" + document.documentId() + "/metadataExtractionResults/My%20prompt"));
          Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
          assertTrue(resultmap.isEmpty());
        } else {
          assertEquals("POST", lastRequest.getMethod().getValue());
          assertTrue(lastRequest.getPath().toString()
              .endsWith("/documents/" + document.documentId() + "/ai/prompts/My%20prompt"));
          Map<String, Object> resultmap = GSON.fromJson(lastRequest.getBodyAsString(), Map.class);
          assertEquals("us.amazon.nova-2-lite-v1:0", resultmap.get("modelId").toString());
        }

        Action action = actionsService.getActions(siteId, document).get(0);
        assertEquals(ActionStatus.COMPLETE, action.status());
        assertNotNull(action.startDate());
        assertNotNull(action.insertedDate());
        assertNotNull(action.completedDate());

        CALLBACK.reset();
      }
    }
  }

  /**
   * Handle Data_classification / meta data extraction that needs OCR Action.
   *
   * @throws ValidationException ValidationException
   */
  @Test
  public void testHandleDataClassification02() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      for (ActionType type : List.of(DATA_CLASSIFICATION, METADATA_EXTRACTION, LLMPROMPT)) {

        DocumentArtifact document = createDocument(siteId, "application/pdf");

        List<Action> actions = List.of(new ActionBuilder().type(type).userId("joe")
            .parameters(Map.of("llmPromptEntityName", "Myprompt")).document(document).indexUlid()
            .build(siteId));
        actionsService.saveNewActions(actions);

        AwsEvent map = buildAwsEvent(siteId, document);

        // when
        processor.handleRequest(map, null);

        // then
        List<Action> list = actionsService.getActions(siteId, document);
        assertEquals(2, list.size());
        assertEquals(ActionType.OCR, list.get(0).type());
        assertEquals("{ocrEngine=tesseract}", list.get(0).parameters().toString());
        assertEquals(ActionStatus.PENDING, list.get(0).status());
        assertEquals(type, list.get(1).type());
        assertEquals(ActionStatus.PENDING, list.get(1).status());
      }
    }
  }
}
