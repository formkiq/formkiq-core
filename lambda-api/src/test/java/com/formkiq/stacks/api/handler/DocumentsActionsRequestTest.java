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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.client.model.DocumentActionStatus.FAILED;
import static com.formkiq.client.model.DocumentActionStatus.FAILED_RETRY;
import static com.formkiq.client.model.DocumentActionStatus.PENDING;
import static com.formkiq.module.actions.ActionStatus.MAX_RETRIES_REACHED;
import static com.formkiq.module.actions.ActionStatus.RUNNING;
import static com.formkiq.module.actions.ActionStatus.WAITING_FOR_RETRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.entity.LlmPromptPresetEntity;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.client.model.OcrOutputType;
import com.formkiq.client.model.TextractQuery;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.testutils.api.documents.AddDocumentActionsRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentActionsRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentsRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.EngineEnum;
import com.formkiq.client.model.AddActionParameters.NotificationTypeEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.Queue;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.validation.ValidationException;
import org.junit.jupiter.api.Timeout;

/** Unit Tests for request /documents/{documentId}/actions. */
public class DocumentsActionsRequestTest extends AbstractApiClientRequestTest {

  /** Timeout. */
  private static final long TIMEOUT = 10;
  /** {@link ConfigService}. */
  private ConfigService configService;
  /** {@link DynamoDbService}. */
  private DynamoDbService db;
  /** {@link ActionsService}. */
  private ActionsService service;

  private void assertDocumentAction(final DocumentAction action, final DocumentActionType type,
      final DocumentActionStatus status, final String message) {
    assertEquals(type, action.getType());
    assertEquals(status, action.getStatus());
    if (message != null) {
      assertEquals(message, action.getMessage());
    } else {
      assertNull(action.getMessage());
    }

  }

  /**
   * Before.
   *
   */
  @BeforeEach
  public void before() {

    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServices.register(DynamoDbService.class, new DynamoDbServiceExtension());
    awsServices.register(ActionsService.class, new ActionsServiceExtension());
    awsServices.register(DocumentService.class, new DocumentServiceExtension());
    awsServices.register(ConfigService.class, new ConfigServiceExtension());

    this.db = awsServices.getExtension(DynamoDbService.class);
    this.service = awsServices.getExtension(ActionsService.class);
    this.configService = awsServices.getExtension(ConfigService.class);
  }

  private ActionBuilder createAction(final DocumentArtifact document, final ActionType actionType) {
    return new ActionBuilder().document(document).type(actionType).userId("joe").indexUlid();
  }

  private ActionBuilder createAction(final String documentId, final ActionType actionType) {
    return new ActionBuilder().document(DocumentArtifact.of(documentId, null)).type(actionType)
        .userId("joe").indexUlid();
  }

  private List<DocumentAction> getDocumentActions(final String siteId,
      final DocumentArtifact document) {
    return getDocumentActions(siteId, document, null);
  }

  private List<DocumentAction> getDocumentActions(final String siteId,
      final DocumentArtifact document, final String next) {
    var resp = new GetDocumentActionsRequestBuilder(document).limit(null).next(next).submit(client,
        siteId);
    assertFalse(resp.isError());
    return notNull(resp.response().getActions());
  }

  private List<DocumentAction> getDocumentActions(final String siteId, final String documentId) {
    return getDocumentActions(siteId, DocumentArtifact.of(documentId, null), null);
  }

  private List<Document> getFailedActionDocuments(final String siteId) throws ApiException {
    return notNull(new GetDocumentsRequestBuilder().actionStatus(FAILED.name())
        .submit(client, siteId).throwIfError().response().getDocuments());
  }

  private Collection<PresetEntity> getPresets() {
    return getAwsServices().getExtensions(PresetEntity.class);
  }

  private DocumentArtifact saveArtifactDocument(final String siteId, final String documentId)
      throws ApiException {
    AddDocumentResponse resp = new AddDocumentRequestBuilder().content().documentId(documentId)
        .artifacts(true).submit(client, siteId).throwIfError().response();
    return DocumentArtifact.of(resp.getDocumentId(), resp.getArtifactId());
  }

  /**
   * Save Document.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  private String saveDocument(final String siteId) throws ApiException {
    return saveDocument(siteId, null);
  }

  /**
   * Save Document with path.
   *
   * @param siteId {@link String}
   * @param path {@link String}
   * @return {@link String}
   */
  private String saveDocument(final String siteId, final String path)
      throws ValidationException, ApiException {
    AddDocumentResponse response = new AddDocumentRequestBuilder().content().path(path)
        .submit(client, siteId).throwIfError().response();
    return response.getDocumentId();
  }

  /**
   * GET /documents?actionStatus request.
   *
   */
  @Test
  public void testActionStatus() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      var path = "mypath_" + ID.ulid();
      var documentId = saveDocument(siteId, path);

      for (var status : ActionStatus.values()) {
        this.service.saveNewActions(List.of(createAction(documentId, ActionType.OCR).queueId("1")
            .status(status).parameters(Map.of("test", "this")).build(siteId)));
      }

      for (var status : ActionStatus.values()) {
        // when
        var resp =
            new GetDocumentsRequestBuilder().actionStatus(status.name()).submit(client, siteId);

        // then
        if (ActionStatus.COMPLETE == status) {
          var e = resp.exception();
          assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
          assertEquals("{\"message\":\"invalid actionStatus 'COMPLETE'\"}", e.getResponseBody());
        } else {
          assertFalse(resp.isError());
          List<Document> docs = notNull(resp.response().getDocuments());
          assertEquals(1, docs.size());
          assertEquals(documentId, docs.get(0).getDocumentId());
          assertEquals(path, docs.get(0).getPath());
        }
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for Data classification.
   *
   */
  @Test
  public void testDataClassificationMissingPrompt() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.DATA_CLASSIFICATION)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"parameters.llmPromptEntityName\","
                + "\"error\":\"'llmPromptEntityName' parameter is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for Data classification.
   *
   */
  @Test
  public void testDataClassificationPrompt() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      EntityTypeRecord entityTypeRecord = EntityTypeRecord.builderWithPresets(getPresets())
          .documentId(ID.uuid()).namespace(EntityTypeNamespace.PRESET)
          .name(LlmPromptPresetEntity.ENTITY_NAME).build(siteId);
      db.putItem(entityTypeRecord.getAttributes());

      EntityRecord entity = EntityRecord.builder().entityTypeId(entityTypeRecord.documentId())
          .documentId(ID.uuid()).name("Myllm").build(siteId);
      db.putItem(entity.getAttributes());

      AddActionParameters param = new AddActionParameters().llmPromptEntityName("Myllm");

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(
          List.of(new AddAction().type(DocumentActionType.DATA_CLASSIFICATION).parameters(param)));

      // when
      AddResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);

      // then
      assertEquals("Actions saved", response.getMessage());
    }
  }

  /**
   * POST /documents/{documentId}/actions for Data classification does not exist.
   *
   */
  @Test
  public void testDataClassificationPromptDoesNotExist() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);

      AddActionParameters param = new AddActionParameters().llmPromptEntityName("myllm");

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(
          List.of(new AddAction().type(DocumentActionType.DATA_CLASSIFICATION).parameters(param)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"entityTypeId\","
            + "\"error\":\"EntityType 'LlmPrompt' is not found\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for Textract queries, missing ocrTextractQueries.
   *
   */
  @Test
  public void testDocumentActionsWithQueriesMissingOcrTextractQueries() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      AddActionParameters param = new AddActionParameters().ocrParseTypes("QUERIES");
      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.OCR).parameters(param)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"parameters.ocrTextractQueries\","
                + "\"error\":\"action 'ocrTextractQueries' parameter is required\"}]}",
            e.getResponseBody());
      }

      // given
      param.addOcrTextractQueriesItem(new TextractQuery());

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"parameters.ocrTextractQueries.text\","
                + "\"error\":\"action 'ocrTextractQueries.text' parameter is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocumentActionsRetry02() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      // when
      this.service.saveNewActions(List.of(createAction(documentId, ActionType.OCR).userId("joe")
          .status(ActionStatus.FAILED).message("some message").parameters(Map.of("test", "this"))
          .type(ActionType.OCR).build(siteId)));

      List<Document> failed = getFailedActionDocuments(siteId);
      assertEquals(1, failed.size());
      assertEquals(documentId, failed.get(0).getDocumentId());

      // then
      List<DocumentAction> actions = getDocumentActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR, FAILED, "some message");

      // when
      AddResponse retry = this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      actions = getDocumentActions(siteId, documentId);

      assertEquals(2, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR, FAILED_RETRY, "some message");
      assertDocumentAction(actions.get(1), DocumentActionType.OCR, PENDING, null);

      // when - 2nd time
      try {
        this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Failed action not found\"}", e.getResponseBody());
      }

      // then
      actions = getDocumentActions(siteId, documentId);
      assertEquals(2, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR, FAILED_RETRY, "some message");
      assertDocumentAction(actions.get(1), DocumentActionType.OCR, PENDING, null);
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. multiple Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocumentActionsRetry03() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      this.service.saveNewActions(Arrays.asList(
          createAction(documentId, ActionType.OCR).userId("joe").status(ActionStatus.FAILED)
              .type(ActionType.OCR).build(siteId),
          createAction(documentId, ActionType.OCR).userId("joe").status(ActionStatus.FAILED)
              .type(ActionType.FULLTEXT).build(siteId)));

      // when
      AddResponse retry = this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      List<DocumentAction> actions = getDocumentActions(siteId, documentId);

      int i = 0;
      final int expected = 3;

      assertEquals(expected, actions.size());
      assertDocumentAction(actions.get(i++), DocumentActionType.OCR, FAILED, null);
      assertDocumentAction(actions.get(i++), DocumentActionType.FULLTEXT, FAILED_RETRY, null);
      assertDocumentAction(actions.get(i), DocumentActionType.FULLTEXT, PENDING, null);

      // when - 2nd time
      this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);

      // then
      actions = getDocumentActions(siteId, documentId);
      assertEquals(expected + 1, actions.size());

      i = 0;
      assertDocumentAction(actions.get(i++), DocumentActionType.OCR, FAILED_RETRY, null);
      assertDocumentAction(actions.get(i++), DocumentActionType.FULLTEXT, FAILED_RETRY, null);
      assertDocumentAction(actions.get(i++), DocumentActionType.OCR, PENDING, null);
      assertDocumentAction(actions.get(i), DocumentActionType.FULLTEXT, PENDING, null);

      // then - limits
      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, "1", null, null);
      actions = notNull(response.getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());

      actions = getDocumentActions(siteId, document, response.getNext());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.FULLTEXT, actions.get(0).getType());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request with artifactId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocumentActionsRetryArtifact01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      DocumentArtifact document = saveArtifactDocument(siteId, documentId);

      this.service.saveNewActions(List.of(
          createAction(documentId, ActionType.FULLTEXT).userId("joe").status(ActionStatus.FAILED)
              .build(siteId),
          createAction(document, ActionType.OCR).userId("joe").status(ActionStatus.FAILED)
              .build(siteId)));

      // when
      AddResponse retry = this.documentActionsApi.addDocumentRetryAction(document.documentId(),
          siteId, document.artifactId());

      // then
      assertEquals("Actions retrying", retry.getMessage());

      List<DocumentAction> actions = getDocumentActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.FULLTEXT, FAILED, null);

      actions = getDocumentActions(siteId, document);
      assertEquals(2, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR, FAILED_RETRY, null);
      assertDocumentAction(actions.get(1), DocumentActionType.OCR, PENDING, null);
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. Nothing failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(List
          .of(createAction(documentId, ActionType.OCR).userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.OCR).build(siteId)));
      List<Document> failed = getFailedActionDocuments(siteId);
      assertEquals(0, failed.size());

      // when
      try {
        this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Failed action not found\"}", e.getResponseBody());
      }

      // then
      List<DocumentAction> actions =
          getDocumentActions(siteId, DocumentArtifact.of(documentId, null));
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", notNull(actions.get(0).getParameters()).toString());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. RUNNING / MAX RETRIES / RETRYING.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry04() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      for (ActionStatus status : List.of(RUNNING, MAX_RETRIES_REACHED, WAITING_FOR_RETRY)) {
        String documentId = saveDocument(siteId);

        Action a0 = createAction(documentId, ActionType.OCR).userId("joe").status(status)
            .type(ActionType.OCR).build(siteId);
        Action a1 = createAction(documentId, ActionType.OCR).userId("joe")
            .status(ActionStatus.PENDING).type(ActionType.ANTIVIRUS).build(siteId);
        Action a2 = createAction(documentId, ActionType.OCR).userId("joe")
            .status(ActionStatus.PENDING).type(ActionType.MALWARE_SCAN).build(siteId);
        List<Action> addActions = Arrays.asList(a0, a1, a2);

        // when
        this.service.saveNewActions(addActions);

        AddResponse retry =
            this.documentActionsApi.addDocumentRetryAction(documentId, siteId, null);

        // then
        assertEquals("Actions retrying", retry.getMessage());

        List<DocumentAction> actions = getDocumentActions(siteId, documentId);

        int i = 0;
        assertDocumentAction(actions.get(i++), DocumentActionType.OCR, FAILED_RETRY, null);
        assertDocumentAction(actions.get(i++), DocumentActionType.OCR, PENDING, null);
        assertDocumentAction(actions.get(i++), DocumentActionType.ANTIVIRUS, PENDING, null);
        assertDocumentAction(actions.get(i), DocumentActionType.MALWARE_SCAN, PENDING, null);
      }
    }
  }

  /**
   * Get /documents/{documentId}/actions request.
   *
   */
  @Test
  public void testHandleGetDocumentActions01() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(List
          .of(createAction(documentId, ActionType.OCR).userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.OCR).build(siteId)));

      // when
      List<DocumentAction> actions = getDocumentActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", notNull(actions.get(0).getParameters()).toString());
    }
  }

  /**
   * Get /documents/{documentId}/actions request with artifactId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentActionsArtifact01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      DocumentArtifact document = saveArtifactDocument(siteId, documentId);

      new AddDocumentActionsRequestBuilder(documentId).addAction(DocumentActionType.FULLTEXT)
          .submit(client, siteId).throwIfError();

      new AddDocumentActionsRequestBuilder(document).addAction(DocumentActionType.OCR)
          .submit(client, siteId).throwIfError();

      // when
      final List<DocumentAction> actions0 = getDocumentActions(siteId, documentId);
      final List<DocumentAction> actions1 = getDocumentActions(siteId, document);

      // then
      assertEquals(1, actions0.size());
      assertEquals(DocumentActionType.FULLTEXT, actions0.get(0).getType());
      assertEquals(PENDING, actions0.get(0).getStatus());

      assertEquals(1, actions1.size());
      assertEquals(DocumentActionType.OCR, actions1.get(0).getType());
      assertEquals(PENDING, actions1.get(0).getStatus());
    }
  }

  /**
   * POST /documents/{documentId}/actions request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(TIMEOUT)
  public void testHandlePostDocumentActions01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      this.service.saveNewActions(List.of(createAction(documentId, ActionType.FULLTEXT)
          .status(ActionStatus.COMPLETE).parameters(Map.of("test", "this")).build(siteId)));

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Arrays.asList(
          new AddAction().type(DocumentActionType.OCR)
              .parameters(new AddActionParameters().addPdfDetectedCharactersAsText("true")
                  .ocrOutputType(OcrOutputType.CSV).ocrParseTypes("text")),
          new AddAction().type(DocumentActionType.WEBHOOK)
              .parameters(new AddActionParameters().url("https://localhost"))));

      // when
      AddResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);

      // then
      assertEquals("Actions saved", response.getMessage());

      int i = 0;
      List<Action> actions = this.service.getActions(siteId, document);

      assertEquals(ActionType.FULLTEXT, actions.get(i).type());
      assertEquals(ActionStatus.COMPLETE, actions.get(i).status());
      assertEquals("{test=this}", actions.get(i++).parameters().toString());

      assertEquals(ActionType.OCR, actions.get(i).type());
      assertEquals(ActionStatus.PENDING, actions.get(i).status());
      assertEquals("{ocrParseTypes=text, ocrOutputType=CSV, addPdfDetectedCharactersAsText=true}",
          actions.get(i++).parameters().toString());

      assertEquals(ActionType.WEBHOOK, actions.get(i).type());
      assertEquals(ActionStatus.PENDING, actions.get(i).status());
      assertEquals("{url=https://localhost}", actions.get(i).parameters().toString());

      Map<String, Object> sqsMessage = getSqsMessages("actions", document.documentId());
      assertNotNull(sqsMessage.get("siteId"));
      assertEquals(document.documentId(), sqsMessage.get("documentId"));
      assertEquals("actions", sqsMessage.get("type"));
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'type'.
   *
   */
  @Test
  public void testHandlePostDocumentActions02() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(
          List.of(new AddAction().parameters(new AddActionParameters().ocrParseTypes("text"))));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {

        final int status = 400;
        assertEquals(status, e.getCode());

        Collection<Map<String, Object>> validation = getValidationErrors(e);
        assertEquals(1, validation.size());

        Map<String, Object> i = validation.iterator().next();
        assertEquals("'type' is required", i.get("error"));
      }

      List<Action> actions = this.service.getActions(siteId, document);
      assertEquals(0, actions.size());
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'parameters' for documenttagging.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions03() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {

        // then
        final int status = 400;
        assertEquals(status, e.getCode());

        Collection<Map<String, Object>> validation = getValidationErrors(e);
        assertEquals(2, validation.size());

        Iterator<Map<String, Object>> itr = validation.iterator();
        Map<String, Object> i = itr.next();
        assertEquals("action 'tags' parameter is required", i.get("error"));

        i = itr.next();
        assertEquals("action 'engine' parameter is required", i.get("error"));
      }

      List<Action> actions = this.service.getActions(siteId, document);
      assertEquals(0, actions.size());

      // given - engine
      SiteConfiguration conf = SiteConfiguration.builder().chatGptApiKey("ABC").build(siteId);
      this.configService.save(siteId, conf);

      req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)
              .parameters(new AddActionParameters().engine(EngineEnum.CHATGPT).tags("something"))));

      // when - correct parameters
      AddResponse response = this.documentActionsApi.addDocumentActions(document.documentId(),
          siteId, document.artifactId(), req);

      // then
      assertEquals("Actions saved", response.getMessage());

      actions = this.service.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionType.DOCUMENTTAGGING, actions.get(0).type());
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'parameters' for notification.
   *
   */
  @Test
  public void testHandlePostDocumentActions04() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(List.of(
          new AddAction().type(DocumentActionType.NOTIFICATION).parameters(new AddActionParameters()
              .notificationType(NotificationTypeEnum.EMAIL).notificationToCc("test@formkiq.com"))));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {

        // then
        Collection<Map<String, Object>> validation = getValidationErrors(e);
        assertEquals(1, validation.size());

        Map<String, Object> i = validation.iterator().next();
        assertEquals("parameters.notificationEmail", i.get("key"));

        List<Action> actions = this.service.getActions(siteId, document);
        assertEquals(0, actions.size());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for notification.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions05() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      SiteConfiguration config =
          SiteConfiguration.builder().notificationEmail("test@formkiq.com").build(siteId);
      this.configService.save(siteId, config);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.NOTIFICATION)
              .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
                  .notificationToCc("test@formkiq.com").notificationSubject("test subject")
                  .notificationText("some text"))));

      setBearerToken(siteId);

      // when
      AddResponse response = this.documentActionsApi.addDocumentActions(document.documentId(),
          siteId, document.artifactId(), req);

      // then
      assertEquals("Actions saved", response.getMessage());
      List<Action> actions = this.service.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionType.NOTIFICATION, actions.get(0).type());
    }
  }

  /**
   * POST /documents/{documentId}/actions for Queues missing queueId.
   *
   */
  @Test
  public void testHandlePostDocumentActions06() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.QUEUE)));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"queueId\",\"error\":\"'queueId' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for Queues invalid queueId.
   *
   */
  @Test
  public void testHandlePostDocumentActions07() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.QUEUE).queueId("terst")));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"queueId\",\"error\":\"'queueId' does not exist\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for Queues queueId.
   *
   */
  @Test
  public void testHandlePostDocumentActions08() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");

      String queueId = ID.uuid();
      this.db.putItem(new Queue().documentId(queueId).name("test").getAttributes(siteId));

      String documentId = saveDocument(siteId);
      SiteConfiguration config =
          SiteConfiguration.builder().notificationEmail("test@formkiq.com").build(siteId);
      this.configService.save(siteId, config);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.QUEUE).queueId(queueId)));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"type\"," + "\"error\":\"action type cannot be 'QUEUE'\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for IDP invalid / missing mappingId.
   *
   */
  @Test
  public void testHandlePostDocumentActions09() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);

      AddAction action = new AddAction().type(DocumentActionType.IDP);
      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(List.of(action));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"mappingId\"," + "\"error\":\"'mappingId' is required\"}]}",
            e.getResponseBody());
      }

      // given
      action.parameters(new AddActionParameters().mappingId("1"));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"mappingId\"," + "\"error\":\"'mappingId' does not exist\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/actions for IDP.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions10() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String attributeKey = "tag";

      this.attributesApi.addAttribute(
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      AddMapping addMapping = new AddMapping().name("Document Invoice")
          .addAttributesItem(new MappingAttribute().attributeKey(attributeKey)
              .sourceType(MappingAttributeSourceType.CONTENT)
              .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
              .labelTexts(List.of("invoice", "invoice no")));

      String mappingId = this.mappingsApi
          .addMapping(new AddMappingRequest().mapping(addMapping), siteId).getMappingId();

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.IDP)
              .parameters(new AddActionParameters().mappingId(mappingId))));

      // when
      AddResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, null, req);

      // then
      assertEquals("Actions saved", response.getMessage());
      List<Action> actions = this.service.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionType.IDP, actions.get(0).type());
      assertEquals(mappingId, actions.get(0).parameters().get("mappingId"));
    }
  }

  /**
   * POST /documents/{documentId}/actions request with artifactId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActionsArtifact01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);
      DocumentArtifact document = saveArtifactDocument(siteId, documentId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(List.of(new AddAction().type(DocumentActionType.OCR)
              .parameters(new AddActionParameters().ocrParseTypes("text"))));

      // when
      AddResponse response = this.documentActionsApi.addDocumentActions(document.documentId(),
          siteId, document.artifactId(), req);

      // then
      assertEquals("Actions saved", response.getMessage());

      List<Action> actions = this.service.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals(ActionType.OCR, actions.get(0).type());
      assertEquals(ActionStatus.PENDING, actions.get(0).status());
      assertEquals("{ocrParseTypes=text}", actions.get(0).parameters().toString());

      assertEquals(0, getDocumentActions(siteId, documentId).size());
    }
  }

  /**
   * POST /documents/{documentId}/actions for Textract queries.
   *
   */
  @Test
  public void testHandlePostDocumentActionsWithQueries() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      final DocumentArtifact document = DocumentArtifact.of(documentId, null);

      TextractQuery query =
          new TextractQuery().alias("query").text("mytext").pages(List.of("1", "5"));
      AddActionParameters param =
          new AddActionParameters().ocrParseTypes("QUERIES").addOcrTextractQueriesItem(query);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.OCR).parameters(param)));

      // when
      AddResponse response = this.documentActionsApi.addDocumentActions(document.documentId(),
          siteId, document.artifactId(), req);

      // then
      assertEquals("Actions saved", response.getMessage());

      List<DocumentAction> actions =
          notNull(this.documentActionsApi.getDocumentActions(document.documentId(), siteId,
              document.artifactId(), null, null, null).getActions());
      assertEquals(1, actions.size());
      DocumentAction action = actions.get(0);
      Map<String, Object> params = notNull(action.getParameters());
      assertEquals("QUERIES", params.get("ocrParseTypes"));

      List<Map<String, Object>> queries =
          (List<Map<String, Object>>) params.get("ocrTextractQueries");
      assertEquals(1, queries.size());
      assertEquals("mytext", queries.get(0).get("text"));
      assertEquals("query", queries.get(0).get("alias"));
      assertEquals("1,5", String.join(",", ((List<String>) queries.get(0).get("pages"))));
    }
  }
}
