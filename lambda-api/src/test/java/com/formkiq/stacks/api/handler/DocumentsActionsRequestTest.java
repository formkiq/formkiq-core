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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.client.model.OcrOutputType;
import com.formkiq.client.model.TextractQuery;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddActionParameters.EngineEnum;
import com.formkiq.client.model.AddActionParameters.NotificationTypeEnum;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.AddDocumentActionsResponse;
import com.formkiq.client.model.AddDocumentActionsRetryResponse;
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
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
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
  /** {@link DocumentService}. */
  private DocumentService documentService;
  /** {@link DynamoDbService}. */
  private DynamoDbService db;
  /** {@link ActionsService}. */
  private ActionsService service;

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
    this.documentService = awsServices.getExtension(DocumentService.class);
    this.configService = awsServices.getExtension(ConfigService.class);
  }

  /**
   * Save Document.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   * @throws ValidationException ValidationException
   */
  private String saveDocument(final String siteId) throws ValidationException {
    String documentId = ID.uuid();

    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    this.documentService.saveDocument(siteId, item, null);
    return documentId;
  }

  /**
   * Get /documents/{documentId}/actions request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentActions01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId,
          Collections.singletonList(new Action().userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.OCR)));

      // when
      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      // then
      List<DocumentAction> actions = notNull(response.getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", notNull(actions.get(0).getParameters()).toString());
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

      this.service.saveNewActions(siteId, documentId,
          Collections.singletonList(new Action().userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.FULLTEXT)));

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Arrays.asList(
          new AddAction().type(DocumentActionType.OCR)
              .parameters(new AddActionParameters().addPdfDetectedCharactersAsText("true")
                  .ocrOutputType(OcrOutputType.CSV).ocrParseTypes("text")),
          new AddAction().type(DocumentActionType.WEBHOOK)
              .parameters(new AddActionParameters().url("https://localhost"))));

      // when
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());

      int i = 0;
      List<Action> actions = this.service.getActions(siteId, documentId);

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

      Map<String, Object> sqsMessage = getSqsMessages("actions", documentId);
      assertNotNull(sqsMessage.get("siteId"));
      assertEquals(documentId, sqsMessage.get("documentId"));
      assertEquals("actions", sqsMessage.get("type"));
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'type'.
   *
   */
  @Test
  public void testHandlePostDocumentActions02() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req =
          new AddDocumentActionsRequest().actions(Collections.singletonList(
              new AddAction().parameters(new AddActionParameters().ocrParseTypes("text"))));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
        fail();
      } catch (ApiException e) {

        final int status = 400;
        assertEquals(status, e.getCode());

        Collection<Map<String, Object>> validation = getValidationErrors(e);
        assertEquals(1, validation.size());

        Map<String, Object> i = validation.iterator().next();
        assertEquals("action 'type' is required", i.get("error"));
      }

      List<Action> actions = this.service.getActions(siteId, documentId);
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

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(
          Collections.singletonList(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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

      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(0, actions.size());

      // given - engine
      this.configService.save(siteId, new SiteConfiguration().setChatGptApiKey("ABC"));

      req = new AddDocumentActionsRequest().actions(
          Collections.singletonList(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)
              .parameters(new AddActionParameters().engine(EngineEnum.CHATGPT).tags("something"))));

      // when - correct parameters
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());

      actions = this.service.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionType.DOCUMENTTAGGING, actions.get(0).type());
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'parameters' for notification.
   *
   */
  @Test
  public void testHandlePostDocumentActions04() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Collections.singletonList(new AddAction().type(DocumentActionType.NOTIFICATION)
              .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
                  .notificationToCc("test@formkiq.com"))));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
        fail();
      } catch (ApiException e) {

        // then
        Collection<Map<String, Object>> validation = getValidationErrors(e);
        assertEquals(1, validation.size());

        Map<String, Object> i = validation.iterator().next();
        assertEquals("parameters.notificationEmail", i.get("key"));

        List<Action> actions = this.service.getActions(siteId, documentId);
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
      SiteConfiguration config = new SiteConfiguration().setNotificationEmail("test@formkiq.com");

      this.configService.save(siteId, config);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Collections.singletonList(new AddAction().type(DocumentActionType.NOTIFICATION)
              .parameters(new AddActionParameters().notificationType(NotificationTypeEnum.EMAIL)
                  .notificationToCc("test@formkiq.com").notificationSubject("test subject")
                  .notificationText("some text"))));

      setBearerToken(siteId);

      // when
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());
      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionType.NOTIFICATION, actions.get(0).type());
    }
  }

  /**
   * POST /documents/{documentId}/actions for Queues missing queueId.
   *
   */
  @Test
  public void testHandlePostDocumentActions06() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Collections.singletonList(new AddAction().type(DocumentActionType.QUEUE)));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
  public void testHandlePostDocumentActions07() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.QUEUE).queueId("terst")));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
  public void testHandlePostDocumentActions08() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken("Admins");

      String queueId = ID.uuid();
      this.db.putItem(new Queue().documentId(queueId).name("test").getAttributes(siteId));

      String documentId = saveDocument(siteId);
      SiteConfiguration config = new SiteConfiguration().setNotificationEmail("test@formkiq.com");

      this.configService.save(siteId, config);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.QUEUE).queueId(queueId)));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
  public void testHandlePostDocumentActions09() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = saveDocument(siteId);

      AddAction action = new AddAction().type(DocumentActionType.IDP);
      AddDocumentActionsRequest req =
          new AddDocumentActionsRequest().actions(Collections.singletonList(action));

      setBearerToken(siteId);

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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

      AddMapping addMapping = new AddMapping().name("Document Invoice")
          .addAttributesItem(new MappingAttribute().attributeKey(attributeKey)
              .sourceType(MappingAttributeSourceType.CONTENT)
              .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
              .labelTexts(List.of("invoice", "invoice no")));

      String mappingId = this.mappingsApi
          .addMapping(new AddMappingRequest().mapping(addMapping), siteId).getMappingId();

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Collections.singletonList(new AddAction().type(DocumentActionType.IDP)
              .parameters(new AddActionParameters().mappingId(mappingId))));

      // when
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());
      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionType.IDP, actions.get(0).type());
      assertEquals(mappingId, actions.get(0).parameters().get("mappingId"));
    }
  }

  /**
   * POST /documents/{documentId}/actions for Textract queries, missing ocrTextractQueries.
   *
   */
  @Test
  public void testDocumentActionsWithQueriesMissingOcrTextractQueries() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      AddActionParameters param = new AddActionParameters().ocrParseTypes("QUERIES");
      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.OCR).parameters(param)));

      // when
      try {
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
        this.documentActionsApi.addDocumentActions(documentId, siteId, req);
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
   * POST /documents/{documentId}/actions for Textract queries.
   *
   */
  @Test
  public void testHandlePostDocumentActionsWithQueries() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = saveDocument(siteId);
      TextractQuery query =
          new TextractQuery().alias("query").text("mytext").pages(List.of("1", "5"));
      AddActionParameters param =
          new AddActionParameters().ocrParseTypes("QUERIES").addOcrTextractQueriesItem(query);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Collections
          .singletonList(new AddAction().type(DocumentActionType.OCR).parameters(param)));

      // when
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());

      List<DocumentAction> actions = notNull(this.documentActionsApi
          .getDocumentActions(documentId, siteId, null, null, null).getActions());
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

      this.service.saveNewActions(siteId, documentId,
          Collections.singletonList(new Action().userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.OCR)));
      List<Document> failed = getFailedActionDocuments(siteId);
      assertEquals(0, failed.size());

      // when
      AddDocumentActionsRetryResponse retry =
          this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      List<DocumentAction> actions = notNull(response.getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", notNull(actions.get(0).getParameters()).toString());
    }
  }

  private List<Document> getFailedActionDocuments(final String siteId) throws ApiException {
    return notNull(this.documentsApi
        .getDocuments(siteId, "FAILED", null, null, null, null, null, null, null).getDocuments());
  }

  /**
   * POST /documents/{documentId}/actions/retry request. Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry02() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      // when
      this.service.saveNewActions(siteId, documentId,
          Collections.singletonList(new Action().userId("joe").status(ActionStatus.FAILED)
              .message("some message").parameters(Map.of("test", "this")).type(ActionType.OCR)));

      List<Document> failed = getFailedActionDocuments(siteId);
      assertEquals(1, failed.size());
      assertEquals(documentId, failed.get(0).getDocumentId());

      // then
      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
      List<DocumentAction> actions = notNull(response.getActions());
      assertEquals(1, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR, DocumentActionStatus.FAILED,
          "some message");

      // when
      AddDocumentActionsRetryResponse retry =
          this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      actions = notNull(response.getActions());
      assertEquals(2, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR,
          DocumentActionStatus.FAILED_RETRY, "some message");
      assertDocumentAction(actions.get(1), DocumentActionType.OCR, DocumentActionStatus.PENDING,
          null);

      // when - 2nd time
      this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
      actions = notNull(response.getActions());
      assertEquals(2, actions.size());
      assertDocumentAction(actions.get(0), DocumentActionType.OCR,
          DocumentActionStatus.FAILED_RETRY, "some message");
      assertDocumentAction(actions.get(1), DocumentActionType.OCR, DocumentActionStatus.PENDING,
          null);
    }
  }

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
   * POST /documents/{documentId}/actions/retry request. multiple Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry03() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId,
          Arrays.asList(new Action().userId("joe").status(ActionStatus.FAILED).type(ActionType.OCR),
              new Action().userId("joe").status(ActionStatus.FAILED).type(ActionType.FULLTEXT)));

      // when
      AddDocumentActionsRetryResponse retry =
          this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      int i = 0;
      final int expected = 4;
      List<DocumentAction> actions = notNull(response.getActions());
      assertEquals(expected, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(i).getType());
      assertEquals(DocumentActionStatus.FAILED_RETRY, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.FULLTEXT, actions.get(i).getType());
      assertEquals(DocumentActionStatus.FAILED_RETRY, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.OCR, actions.get(i).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.FULLTEXT, actions.get(i).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(i).getStatus());

      // when - 2nd time
      this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
      actions = notNull(response.getActions());
      assertEquals(expected, actions.size());

      // then - limits
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, "1", null, null);
      assertEquals(1, notNull(response.getActions()).size());
      assertEquals(DocumentActionType.OCR, response.getActions().get(0).getType());

      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null,
          response.getNext());
      assertEquals(1, notNull(response.getActions()).size());
      assertEquals(DocumentActionType.FULLTEXT, response.getActions().get(0).getType());
    }
  }
}
