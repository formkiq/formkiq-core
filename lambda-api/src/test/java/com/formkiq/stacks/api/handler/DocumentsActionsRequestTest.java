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

import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.services.lambda.GsonUtil;
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
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.sqs.model.Message;

/** Unit Tests for request /documents/{documentId}/actions. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsActionsRequestTest extends AbstractApiClientRequestTest {

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
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.db = getAwsServices().getExtension(DynamoDbService.class);
    this.service = getAwsServices().getExtension(ActionsService.class);
    this.documentService = getAwsServices().getExtension(DocumentService.class);
    this.configService = getAwsServices().getExtension(ConfigService.class);
  }

  /**
   * Save Document.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   * @throws ValidationException ValidationException
   */
  private String saveDocument(final String siteId) throws ValidationException {
    String documentId = UUID.randomUUID().toString();

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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId, Arrays.asList(new Action().userId("joe")
          .status(ActionStatus.COMPLETE).parameters(Map.of("test", "this")).type(ActionType.OCR)));

      // when
      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      // then
      List<DocumentAction> actions = response.getActions();
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", actions.get(0).getParameters().toString());
    }
  }

  /**
   * POST /documents/{documentId}/actions request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentActions01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId,
          Arrays.asList(new Action().userId("joe").status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.FULLTEXT)));

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Arrays.asList(
          new AddAction().type(DocumentActionType.OCR)
              .parameters(new AddActionParameters().addPdfDetectedCharactersAsText("true")
                  .ocrExportToCsv("true").ocrParseTypes("text")),
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
      assertEquals("{ocrParseTypes=text, ocrExportToCsv=true, addPdfDetectedCharactersAsText=true}",
          actions.get(i++).parameters().toString());

      assertEquals(ActionType.WEBHOOK, actions.get(i).type());
      assertEquals(ActionStatus.PENDING, actions.get(i).status());
      assertEquals("{url=https://localhost}", actions.get(i++).parameters().toString());

      List<Message> sqsMessages = getSqsMessages();
      assertEquals(1, sqsMessages.size());

      Map<String, String> map =
          GsonUtil.getInstance().fromJson(sqsMessages.get(0).body(), Map.class);

      map = GsonUtil.getInstance().fromJson(map.get("Message"), Map.class);
      assertNotNull(map.get("siteId"));
      assertEquals(documentId, map.get("documentId"));
      assertEquals("actions", map.get("type"));
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'type'.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Arrays
          .asList(new AddAction().parameters(new AddActionParameters().ocrParseTypes("text"))));

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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)));

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
      this.configService.save(siteId, new DynamicObject(Map.of(CHATGPT_API_KEY, "asd")));

      req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.DOCUMENTTAGGING)
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
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions04() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest().actions(Arrays.asList(
          new AddAction().type(DocumentActionType.NOTIFICATION).parameters(new AddActionParameters()
              .notificationType(NotificationTypeEnum.EMAIL).notificationToCc("test@formkiq.com"))));

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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      this.configService.save(siteId,
          new DynamicObject(Map.of("NotificationEmail", "test@formkiq.com")));

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.NOTIFICATION)
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
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions06() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.QUEUE)));

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
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions07() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken("Admins");
      String documentId = saveDocument(siteId);

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.QUEUE).queueId("terst")));

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
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentActions08() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken("Admins");

      String queueId = UUID.randomUUID().toString();
      this.db.putItem(new Queue().documentId(queueId).name("test").getAttributes(siteId));

      String documentId = saveDocument(siteId);

      this.configService.save(siteId,
          new DynamicObject(Map.of("NotificationEmail", "test@formkiq.com")));

      AddDocumentActionsRequest req = new AddDocumentActionsRequest()
          .actions(Arrays.asList(new AddAction().type(DocumentActionType.QUEUE).queueId(queueId)));

      setBearerToken(siteId);

      // when
      AddDocumentActionsResponse response =
          this.documentActionsApi.addDocumentActions(documentId, siteId, req);

      // then
      assertEquals("Actions saved", response.getMessage());
      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionType.QUEUE, actions.get(0).type());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. Nothing failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId, Arrays.asList(new Action().userId("joe")
          .status(ActionStatus.COMPLETE).parameters(Map.of("test", "this")).type(ActionType.OCR)));

      // when
      AddDocumentActionsRetryResponse retry =
          this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      List<DocumentAction> actions = response.getActions();
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.COMPLETE, actions.get(0).getStatus());
      assertEquals("{test=this}", actions.get(0).getParameters().toString());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = saveDocument(siteId);

      this.service.saveNewActions(siteId, documentId,
          Arrays.asList(new Action().userId("joe").status(ActionStatus.FAILED)
              .message("some message").parameters(Map.of("test", "this")).type(ActionType.OCR)));

      // when
      AddDocumentActionsRetryResponse retry =
          this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      assertEquals("Actions retrying", retry.getMessage());

      GetDocumentActionsResponse response =
          this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);

      List<DocumentAction> actions = response.getActions();
      assertEquals(2, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.FAILED_RETRY, actions.get(0).getStatus());
      assertEquals("some message", actions.get(0).getMessage());

      assertEquals(DocumentActionType.OCR, actions.get(1).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(1).getStatus());
      assertNull(actions.get(1).getMessage());

      // when - 2nd time
      this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
      actions = response.getActions();
      assertEquals(2, actions.size());
    }
  }

  /**
   * POST /documents/{documentId}/actions/retry request. multiple Failed
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddtDocumentActionsRetry03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
      List<DocumentAction> actions = response.getActions();
      assertEquals(expected, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(i).getType());
      assertEquals(DocumentActionStatus.FAILED_RETRY, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.FULLTEXT, actions.get(i).getType());
      assertEquals(DocumentActionStatus.FAILED_RETRY, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.OCR, actions.get(i).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(i++).getStatus());

      assertEquals(DocumentActionType.FULLTEXT, actions.get(i).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(i++).getStatus());

      // when - 2nd time
      this.documentActionsApi.addDocumentRetryAction(documentId, siteId);

      // then
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
      actions = response.getActions();
      assertEquals(expected, actions.size());

      // then - limits
      response = this.documentActionsApi.getDocumentActions(documentId, siteId, "1", null, null);
      assertEquals(1, response.getActions().size());
      assertEquals(DocumentActionType.OCR, response.getActions().get(0).getType());

      response = this.documentActionsApi.getDocumentActions(documentId, siteId, null, null,
          response.getNext());
      assertEquals(1, response.getActions().size());
      assertEquals(DocumentActionType.FULLTEXT, response.getActions().get(0).getType());
    }
  }
}
