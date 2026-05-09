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
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.eventbridge.EventBridgeAwsServiceRegistry;
import com.formkiq.aws.eventbridge.EventBridgeService;
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
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.mappings.Mapping;
import com.formkiq.stacks.dynamodb.mappings.MappingAttribute;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeLabelMatchingType;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeMetadataField;
import com.formkiq.stacks.dynamodb.mappings.MappingAttributeSourceType;
import com.formkiq.stacks.dynamodb.mappings.MappingService;
import com.formkiq.stacks.dynamodb.mappings.MappingServiceDynamodb;
import com.formkiq.stacks.lambda.s3.event.AwsEvent;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentActionsMappingsProcessorTest implements DbKeys {

  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static final RequestRecordExpectationResponseCallback CALLBACK =
      new RequestRecordExpectationResponseCallback(200, "{\"contentUrls\":[]}");
  /** Document Id with OCR Key/Value. */
  private static final String DOCUMENT_ID_OCR_KEY_VALUE = ID.uuid();
  /** Port to run Test server. */
  private static final int PORT = 8888;
  /** Sns Document Event. */
  private static final String SNS_DOCUMENT_EVENT_TOPIC = "SNS_DOCUMENT_EVENT";
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;
  /** Search Limit. */
  private static final int LIMIT = 100;
  /** {@link TypesenseExtension}. */
  @RegisterExtension
  static TypesenseExtension typesenseExtension = new TypesenseExtension();
  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** {@link AttributeService}. */
  private static AttributeService attributeService;
  /** {@link MappingService}. */
  private static MappingService mappingService;
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

  private static void assertActionCompleted(final Action action) {
    assertNull(action.message());
    assertEquals(ActionStatus.COMPLETE, action.status());
    assertEquals(ActionType.IDP, action.type());
    assertNotNull(action.startDate());
    assertNotNull(action.insertedDate());
    assertNotNull(action.completedDate());
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

    documentService = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, versionService);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    mappingService = new MappingServiceDynamodb(db);
    attributeService = new AttributeServiceDynamodb(db);
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
    env.put("CHATGPT_API_COMPLETIONS_URL", URL + "/" + "chatgpt1");
    env.put("OPERATIONAL_MODE", "ACTIVE");
    return env;
  }

  /**
   * Create Mock Server.
   *
   */
  private static void createMockServer() {

    mockServer = startClientAndServer(PORT);

    addKeyValueOcrMock();
  }

  private static List<DocumentAttributeRecord> findDocumentAttributes(final String siteId,
      final DocumentArtifact document) {
    return notNull(
        documentService.findDocumentAttributes(siteId, document, null, LIMIT).getResults());
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
    serviceCache.getExtension(EventBridgeService.class);
  }

  private String addPdfToBucket(final String siteId) {

    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, DOCUMENT_ID_OCR_KEY_VALUE, null);
    s3Service.putObject(BUCKET_NAME, s3Key, "abc".getBytes(StandardCharsets.UTF_8),
        "application/pdf");

    return DocumentActionsMappingsProcessorTest.DOCUMENT_ID_OCR_KEY_VALUE;
  }

  private DocumentArtifact addTextToBucket(final String siteId, final String text) {
    String documentId = ID.uuid();

    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId, null);
    s3Service.putObject(BUCKET_NAME, s3Key, text.getBytes(StandardCharsets.UTF_8), "text/plain");

    return DocumentArtifact.of(documentId, null);
  }

  private void assertDocumentAttributeEquals(final DocumentAttributeRecord record, final String key,
      final String stringValue, final String numberValue) {
    assertEquals(key, record.getKey());
    assertEquals(stringValue, record.getStringValue());
    assertEquals(numberValue,
        record.getNumberValue() != null ? record.getNumberValue().toString() : null);
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

  private ActionBuilder createAction(final DocumentArtifact document) {
    return new ActionBuilder().type(ActionType.IDP).document(document).indexUlid().userId("joe");
  }

  private void createDocument(final String siteId, final DocumentArtifact document,
      final String contentType) {
    DocumentItem item = new DocumentItemDynamoDb(document.documentId(), new Date(), "joe");
    item.setContentType(contentType);
    documentService.saveDocument(siteId, item, null);
  }

  private Mapping createMapping(final String attributeKey, final String labelText,
      final MappingAttributeLabelMatchingType matchingType,
      final MappingAttributeSourceType sourceType, final String value, final List<String> values,
      final MappingAttributeMetadataField metadataField) {

    List<String> labelTexts = labelText != null ? List.of(labelText) : null;

    MappingAttribute a0 = new MappingAttribute().setAttributeKey(attributeKey)
        .setSourceType(sourceType).setLabelMatchingType(matchingType).setLabelTexts(labelTexts)
        .setDefaultValue(value).setDefaultValues(values).setMetadataField(metadataField);

    return new Mapping("test", null, List.of(a0), null);
  }

  private void processIdpRequest(final String siteId, final DocumentArtifact document,
      final String contentType, final MappingRecord mappingRecord) throws ValidationException {
    createDocument(siteId, document, contentType);

    List<Action> actions = List.of(createAction(document)
        .parameters(Map.of("mappingId", mappingRecord.getDocumentId())).build(siteId));
    actionsService.saveNewActions(actions);

    AwsEvent map = buildAwsEvent(siteId, document);

    // when
    processor.handleRequest(map, null);
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
            null);

        Mapping mapping =
            createMapping("invoice", "P.O. Number", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        // when
        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "invoice", "6200041751", null);
      }
    }
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.NUMBER, null);

        Mapping mapping =
            createMapping("invoice", "P.O. Number", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "invoice", null, "6.200041751E9");
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.NUMBER, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(0, results.size());
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.STRING, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, "somevalue", null, null);

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "invoice", "somevalue", null);
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice",
            AttributeDataType.STRING, null);

        Mapping mapping =
            createMapping("invoice", "abcdef", MappingAttributeLabelMatchingType.EXACT,
                MappingAttributeSourceType.CONTENT, null, Arrays.asList("123", "abc"), null);

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(2, results.size());
        assertDocumentAttributeEquals(results.get(0), "invoice", "123", null);
        assertDocumentAttributeEquals(results.get(1), "invoice", "abc", null);
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "invoice", null,
            null);

        Mapping mapping =
            createMapping("invoice", "P.O. NO.:", MappingAttributeLabelMatchingType.CONTAINS,
                MappingAttributeSourceType.CONTENT, null, null, null);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "invoice", "6200041751", null);
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "path", null, null);

        Mapping mapping = createMapping("path", "j", MappingAttributeLabelMatchingType.BEGINS_WITH,
            MappingAttributeSourceType.METADATA, null, null,
            MappingAttributeMetadataField.USERNAME);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "path", "joe", null);
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

        DocumentArtifact document = addTextToBucket(siteId, text);

        Mapping mapping =
            createMapping("invoice", "invoice", MappingAttributeLabelMatchingType.FUZZY,
                MappingAttributeSourceType.CONTENT, null, null, null);
        mapping.attributes().get(0).setValidationRegex(validationRegex);
        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
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
        DocumentArtifact document = addTextToBucket(siteId, text);

        attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId,
            "certificate_number", null, null);

        Mapping mapping = createMapping("certificate_number", "Customer certificate",
            MappingAttributeLabelMatchingType.FUZZY, MappingAttributeSourceType.CONTENT, null, null,
            null);
        mapping.attributes().get(0).setValidationRegex("\\d+");

        MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

        processIdpRequest(siteId, document, "text/plain", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "certificate_number", "100232", null);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", "Customer certificate",
          MappingAttributeLabelMatchingType.FUZZY, MappingAttributeSourceType.CONTENT_KEY_VALUE,
          null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      // run twice
      for (int i = 0; i < 2; i++) {
        processIdpRequest(siteId, document, "application/pdf", mappingRecord);

        // then
        Action action = actionsService.getActions(siteId, document).get(0);
        assertActionCompleted(action);

        List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
        assertEquals(1, results.size());
        assertDocumentAttributeEquals(results.get(0), "certificate_number", "28937423", null);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping =
          createMapping("certificate_number", "Age", MappingAttributeLabelMatchingType.EXACT,
              MappingAttributeSourceType.CONTENT_KEY_VALUE, null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, document, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, document).get(0);
      assertActionCompleted(action);

      List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
      assertEquals(1, results.size());
      assertDocumentAttributeEquals(results.get(0), "certificate_number", "54", null);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", "Customer certificate",
          MappingAttributeLabelMatchingType.EXACT, MappingAttributeSourceType.CONTENT_KEY_VALUE,
          null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, document, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, document).get(0);
      assertActionCompleted(action);

      List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          null, null);

      Mapping mapping = createMapping("certificate_number", null, null,
          MappingAttributeSourceType.MANUAL, "123", List.of("111", "222"), null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, document, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, document).get(0);
      assertActionCompleted(action);

      List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);

      final int expected = 3;
      assertEquals(expected, results.size());

      int i = 0;
      assertDocumentAttributeEquals(results.get(i++), "certificate_number", "111", null);
      assertDocumentAttributeEquals(results.get(i++), "certificate_number", "123", null);
      assertDocumentAttributeEquals(results.get(i), "certificate_number", "222", null);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      attributeService.addAttribute(AttributeValidationAccess.CREATE, siteId, "certificate_number",
          AttributeDataType.KEY_ONLY, null);

      Mapping mapping = createMapping("certificate_number", null, null,
          MappingAttributeSourceType.MANUAL, null, null, null);

      MappingRecord mappingRecord = mappingService.saveMapping(siteId, null, mapping);

      processIdpRequest(siteId, document, "application/pdf", mappingRecord);

      // then
      Action action = actionsService.getActions(siteId, document).get(0);
      assertActionCompleted(action);

      List<DocumentAttributeRecord> results = findDocumentAttributes(siteId, document);

      assertEquals(1, results.size());
      assertDocumentAttributeEquals(results.get(0), "certificate_number", null, null);
    }
  }
}
