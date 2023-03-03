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

import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentActionsProcessorTest implements DbKeys {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static RequestRecordExpectationResponseCallback callback =
      new RequestRecordExpectationResponseCallback();
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
  /** {@link SsmService}. */
  private static SsmService ssmService;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;

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

    dbBuilder = DynamoDbTestServices.getDynamoDbConnection(null);

    DocumentVersionService versionService = new DocumentVersionServiceNoVersioning();

    documentService = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE, versionService);
    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    createMockServer();

    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection(null);
    ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);

    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/s3/DocumentsS3Bucket", BUCKET_NAME);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/s3/OcrBucket", BUCKET_NAME);

    Map<String, String> env = new HashMap<>();
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    processor = new DocumentActionsProcessor(env, Region.US_EAST_1, null, dbBuilder,
        TestServices.getS3Connection(null), TestServices.getSsmConnection(null),
        TestServices.getSnsConnection(null));
  }

  /**
   * Create Mock Server.
   */
  private static void createMockServer() {

    mockServer = startClientAndServer(Integer.valueOf(PORT));

    mockServer.when(request().withMethod("PATCH")).respond(callback);
    mockServer.when(request().withMethod("POST")).respond(callback);
    mockServer.when(request().withMethod("PUT")).respond(callback);
    mockServer.when(request().withMethod("GET")).respond(callback);
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /**
   * before.
   */
  @BeforeEach
  public void before() {
    this.context = new LambdaContextRecorder();
  }

  /**
   * Test converting Ocr Parse Types.
   */
  @Test
  public void testGetOcrParseTypes01() {
    assertEquals("[TEXT]", processor.getOcrParseTypes(new Action()).toString());

    // invalid
    Map<String, String> parameters = Map.of("parseTypes", "ADAD,IUJK");
    assertEquals("[TEXT]",
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
      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR)
          .parameters(Map.of("addPdfDetectedCharactersAsText", "true")));
      actionsService.saveActions(siteId, documentId, actions);

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

      assertEquals(ActionStatus.RUNNING,
          actionsService.getActions(siteId, documentId).get(0).status());
    }
  }

  /**
   * Handle Fulltext plain/text document.
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
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT));
      actionsService.saveActions(siteId, documentId, actions);

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

      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setContentType("application/pdf");

      documentService.saveDocument(siteId, item, null);
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT));
      actionsService.saveActions(siteId, documentId, actions);

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

      assertEquals(ActionStatus.COMPLETE,
          actionsService.getActions(siteId, documentId).get(0).status());
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
      List<Action> actions = Arrays.asList(new Action().type(ActionType.FULLTEXT));
      actionsService.saveActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionStatus.FAILED, actions.get(0).status());
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

        List<Action> actions = Arrays.asList(
            new Action().type(ActionType.WEBHOOK).parameters(Map.of("url", URL + "/callback")));
        actionsService.saveActions(siteId, documentId, actions);

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

        assertEquals(ActionStatus.COMPLETE,
            actionsService.getActions(siteId, documentId).get(0).status());
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

        List<Action> actions =
            Arrays.asList(new Action().type(ActionType.ANTIVIRUS).status(ActionStatus.COMPLETE),
                new Action().type(ActionType.WEBHOOK).parameters(Map.of("url", URL + "/callback")));
        actionsService.saveActions(siteId, documentId, actions);

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

        assertEquals(ActionStatus.COMPLETE,
            actionsService.getActions(siteId, documentId).get(0).status());
      }
    }
  }
}
