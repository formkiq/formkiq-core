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
package com.formkiq.module.actions;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Unit Tests for {@link ActionsServiceDynamoDbTest}. */
@ExtendWith(DynamoDbExtension.class)
public class ActionsServiceDynamoDbTest {

  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** Limit. */
  private static final int LIMIT = 10;
  /** {@link ActionsService}. */
  private static ActionsService service;

  /**
   * BeforeAll.
   * 
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    ApiAuthorization.login(new ApiAuthorization().username("System"));
    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection();
    service = new ActionsServiceDynamoDb(db, DOCUMENTS_TABLE);
    documentService =
        new DocumentServiceImpl(db, DOCUMENTS_TABLE, new DocumentVersionServiceNoVersioning());
  }

  private ActionBuilder createAction(final DocumentArtifact document, final ActionType actionType) {
    return new ActionBuilder().document(document).indexUlid().type(actionType).userId("joe");
  }

  private ActionBuilder createAction(final String documentId, final ActionType actionType) {
    return createAction(DocumentArtifact.of(documentId, null), actionType);
  }

  /**
   * Has Actions.
   */
  @Test
  public void hasActions01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId0 = ID.uuid();
      String documentId1 = ID.uuid();

      Action action = createAction(documentId0, ActionType.OCR).build(siteId);

      // when
      service.saveNewActions(List.of(action));

      // then
      assertTrue(service.hasActions(siteId, documentId0));
      assertFalse(service.hasActions(siteId, documentId1));
    }
  }

  /**
   * Test Delete Document & Document Actions.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocument() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      documentService.saveDocument(siteId, item, null);

      Action action0 =
          createAction(documentId, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));

      // when
      documentService.deleteDocument(siteId, DocumentArtifact.of(documentId, null), false);

      // then
      List<Action> actions = service.getActions(siteId, documentId);
      assertEquals(0, actions.size());
    }
  }

  /**
   * Test Document Actions.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testDeleteDocumentActions() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      documentService.saveDocument(siteId, item, null);

      Action action0 =
          createAction(documentId, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));

      // when
      service.deleteActions(siteId, documentId);

      // then
      List<Action> actions = service.getActions(siteId, documentId);
      assertEquals(0, actions.size());
    }
  }

  /**
   * Test Inserting OCR into fulltext list.
   */
  @Test
  public void testInsertAction01() {
    // given
    String documentId = ID.uuid();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Action action0 = createAction(documentId, ActionType.DOCUMENTTAGGING)
          .parameters(Map.of("tags", "type")).status(ActionStatus.COMPLETE).build(siteId);

      Action action1 = createAction(documentId, ActionType.FULLTEXT).build(siteId);
      ActionBuilder insertedAction = createAction(documentId, ActionType.OCR);

      List<Action> actions = Arrays.asList(action0, action1);
      service.saveNewActions(actions);
      assertEquals(2, service.getActions(siteId, documentId).size());

      // when
      service.insertBeforeAction(siteId, action1, insertedAction);

      // then
      final int expected = 3;
      List<Action> list = service.getActions(siteId, documentId);
      assertEquals(expected, list.size());

      int i = 0;
      assertEquals(ActionType.DOCUMENTTAGGING, list.get(i).type());
      assertEquals(ActionStatus.COMPLETE, list.get(i++).status());

      assertEquals(ActionType.OCR, list.get(i).type());
      assertEquals(ActionStatus.PENDING, list.get(i++).status());

      assertEquals(ActionType.FULLTEXT, list.get(i).type());
      assertEquals(ActionStatus.PENDING, list.get(i).status());
    }
  }

  /**
   * Test Action.
   *
   */
  @Test
  public void testSave01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final String userId0 = "joe";
      final String userId1 = "jane";
      String documentId0 = ID.uuid();
      String documentId1 = ID.uuid();

      Action action0 = createAction(documentId0, ActionType.OCR).parameters(Map.of("test", "1234"))
          .build(siteId);
      Action action1 = createAction(documentId1, ActionType.OCR).userId(userId1)
          .status(ActionStatus.COMPLETE).build(siteId);

      // when
      final List<Map<String, AttributeValue>> list = service.saveNewActions(List.of(action0));
      service.saveNewActions(List.of(action1));

      // then
      assertEquals(1, list.size());
      if (siteId != null) {
        assertEquals(siteId + "/docs#" + documentId0, list.get(0).get("PK").s());
      } else {
        assertEquals("docs#" + documentId0, list.get(0).get("PK").s());
      }
      String sk = list.get(0).get("SK").s();
      assertTrue(sk.startsWith("action#"));
      assertTrue(sk.endsWith("#OCR"));

      List<Action> results = service.getActions(siteId, documentId0);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.PENDING, results.get(0).status());
      assertEquals(ActionType.OCR, results.get(0).type());
      assertEquals(userId0, results.get(0).userId());
      assertEquals("{test=1234}", results.get(0).parameters().toString());

      results = service.getActions(siteId, documentId1);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.COMPLETE, results.get(0).status());
      assertEquals(ActionType.OCR, results.get(0).type());
      assertEquals(userId1, results.get(0).userId());
      assertNull(results.get(0).parameters());

      // given
      Action action = new ActionBuilder().action(action0).status(ActionStatus.FAILED).build(siteId);

      // when
      service.updateAction(action);

      // then
      results = service.getActions(siteId, documentId0);
      assertEquals(ActionStatus.FAILED, results.get(0).status());
    }
  }

  /**
   * Test save more than 10 actions.
   */
  @Test
  public void testSave02() {
    // given
    final int numberOfActions = 15;
    String documentId = ID.uuid();
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<Action> actions = new ArrayList<>();
      for (int i = 0; i < numberOfActions; i++) {
        actions.add(createAction(documentId, ActionType.DOCUMENTTAGGING)
            .parameters(Map.of("tags", "" + i)).build(siteId));
      }

      // when
      service.saveNewActions(actions);

      // then
      int i = 0;
      List<Action> list = service.getActions(siteId, documentId);
      for (Action action : list) {
        assertEquals("" + i, action.parameters().get("tags"));
        i++;
      }
    }
  }

  /**
   * Test WAIT Action.
   *
   */
  @Test
  public void testSave03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final String userId0 = "joe";
      String name = "test94832";
      String documentId = ID.uuid();

      Action action0 = createAction(documentId, ActionType.QUEUE).queueId(name).build(siteId);

      // when
      service.saveNewActions(List.of(action0));

      // then
      List<Action> results = service.getActions(siteId, documentId);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.PENDING, results.get(0).status());
      assertEquals(ActionType.QUEUE, results.get(0).type());
      assertEquals(userId0, results.get(0).userId());
      assertEquals("test94832", results.get(0).queueId());

      assertEquals(0, service.findDocumentsInQueue(siteId, name, null, 2).getResults().size());
      assertNull(service.findActionInQueue(siteId, documentId, name));
    }
  }

  @Test
  void testSaveActionsMlid() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String docId = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(docId, null);
      Action action0 = new ActionBuilder().document(document).index("0").type(ActionType.QUEUE)
          .userId("joe").queueId("A").build(siteId);

      // when
      service.saveNewActions(List.of(action0));

      // then
      List<Action> actions = service.getActions(siteId, docId);
      assertEquals(1, actions.size());
      assertEquals("0", actions.get(0).index());

      // given - Ulid index
      Action ulid0 = new ActionBuilder().document(document).indexUlid().type(ActionType.OCR)
          .userId("joe").insertedDate(new Date()).build(siteId);
      Action ulid1 = new ActionBuilder().document(document).indexUlid().type(ActionType.IDP)
          .userId("joe").insertedDate(new Date()).build(siteId);
      Action ulid2 = new ActionBuilder().document(document).indexUlid().type(ActionType.FULLTEXT)
          .userId("joe").insertedDate(new Date()).build(siteId);

      // when
      service.saveNewActions(List.of(ulid2, ulid1, ulid0));

      // then
      actions = service.getActions(siteId, docId);

      final int expected = 4;
      assertEquals(expected, actions.size());

      int i = 0;
      assertEquals(ActionType.QUEUE, actions.get(i++).type());
      assertEquals(ActionType.OCR, actions.get(i++).type());
      assertEquals(ActionType.IDP, actions.get(i++).type());
      assertEquals(ActionType.FULLTEXT, actions.get(i).type());
    }
  }

  /**
   * Update Action Status.
   */
  @Test
  public void testUpdateActionStatus01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      Action action0 =
          createAction(documentId, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      Action action =
          new ActionBuilder().action(action0).status(ActionStatus.COMPLETE).build(siteId);
      // action0.status(ActionStatus.COMPLETE);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.COMPLETE, service.getActions(siteId, documentId).get(0).status());
      Pagination<String> results =
          service.findDocumentsWithStatus(siteId, ActionStatus.FAILED, null, LIMIT);
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Update WAIT Action COMPLETE Status.
   */
  @Test
  public void testUpdateActionStatus02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String queueId = "queue1234";
      String documentId = ID.uuid();
      Action action0 = createAction(documentId, ActionType.QUEUE).queueId(queueId).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      Action action =
          new ActionBuilder().action(action0).status(ActionStatus.IN_QUEUE).build(siteId);

      // action0.status(ActionStatus.IN_QUEUE);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.IN_QUEUE, service.getActions(siteId, documentId).get(0).status());
      Pagination<Action> docs = service.findDocumentsInQueue(siteId, queueId, null, LIMIT);
      assertEquals(1, docs.getResults().size());
      assertEquals(queueId, docs.getResults().get(0).queueId());
      assertNotNull(service.findActionInQueue(siteId, documentId, queueId));

      Pagination<String> results =
          service.findDocumentsWithStatus(siteId, ActionStatus.IN_QUEUE, null, LIMIT);
      assertEquals(1, results.getResults().size());
      assertEquals(documentId, results.getResults().get(0));

      // given
      Action a = new ActionBuilder().action(action0).status(ActionStatus.COMPLETE).build(siteId);
      // action0.status(ActionStatus.COMPLETE);

      // when
      service.updateAction(a);

      // then
      assertEquals(ActionStatus.COMPLETE, service.getActions(siteId, documentId).get(0).status());
      docs = service.findDocumentsInQueue(siteId, queueId, null, LIMIT);
      assertEquals(0, docs.getResults().size());
      assertNull(service.findActionInQueue(siteId, documentId, queueId));

      results = service.findDocumentsWithStatus(siteId, ActionStatus.COMPLETE, null, LIMIT);
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Update WAIT Action FAILED Status.
   */
  @Test
  public void testUpdateActionStatus03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String name = "queue1234";
      String documentId = ID.uuid();
      Action action0 = createAction(documentId, ActionType.QUEUE).queueId(name).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      Action action = new ActionBuilder().action(action0).status(ActionStatus.FAILED).build(siteId);
      // action0.status(ActionStatus.FAILED);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.FAILED, service.getActions(siteId, documentId).get(0).status());
      Pagination<Action> docs = service.findDocumentsInQueue(siteId, name, null, LIMIT);
      assertEquals(0, docs.getResults().size());
      assertNull(service.findActionInQueue(siteId, documentId, name));

      Pagination<String> results =
          service.findDocumentsWithStatus(siteId, ActionStatus.FAILED, null, LIMIT);
      assertEquals(1, results.getResults().size());
      assertEquals(documentId, results.getResults().get(0));

      results = service.findDocumentsWithStatus(siteId, ActionStatus.PENDING, null, LIMIT);
      assertEquals(0, results.getResults().size());
    }
  }
}
