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
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
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
    documentService = new DocumentServiceImpl(db, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE,
        new DocumentVersionServiceNoVersioning());
  }

  /**
   * Has Actions.
   */
  @Test
  public void hasActions01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String userId0 = "joe";
      String documentId0 = ID.uuid();
      String documentId1 = ID.uuid();

      Action action0 = new Action().type(ActionType.OCR).userId(userId0);

      // when
      service.saveNewActions(siteId, documentId0, List.of(action0));

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
          new Action().type(ActionType.OCR).userId("joe").parameters(Map.of("test", "1234"));
      service.saveNewActions(siteId, documentId, List.of(action0));

      // when
      documentService.deleteDocument(siteId, documentId, false);

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
          new Action().type(ActionType.OCR).userId("joe").parameters(Map.of("test", "1234"));
      service.saveNewActions(siteId, documentId, List.of(action0));

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
    String user = "joe";

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Action action0 = new Action().type(ActionType.DOCUMENTTAGGING).userId(user)
          .parameters(Map.of("tags", "type")).status(ActionStatus.COMPLETE);

      Action action1 = new Action().type(ActionType.FULLTEXT).userId(user);
      Action insertedAction = new Action().type(ActionType.OCR).userId(user);

      List<Action> actions = Arrays.asList(action0, action1);
      service.saveNewActions(siteId, documentId, actions);
      assertEquals(2, service.getActions(siteId, documentId).size());

      // when
      service.insertBeforeAction(siteId, documentId, action1, insertedAction);

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
      String userId0 = "joe";
      String userId1 = "jane";
      String documentId0 = ID.uuid();
      String documentId1 = ID.uuid();

      Action action0 =
          new Action().type(ActionType.OCR).userId(userId0).parameters(Map.of("test", "1234"));
      Action action1 =
          new Action().type(ActionType.OCR).userId(userId1).status(ActionStatus.COMPLETE);

      // when
      final List<Map<String, AttributeValue>> list =
          service.saveNewActions(siteId, documentId0, List.of(action0));
      service.saveNewActions(siteId, documentId1, List.of(action1));

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
      action0.status(ActionStatus.FAILED);

      // when
      service.updateActionStatus(siteId, documentId0, action0);

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
    String user = "joe";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<Action> actions = new ArrayList<>();
      for (int i = 0; i < numberOfActions; i++) {
        actions.add(new Action().type(ActionType.DOCUMENTTAGGING).userId(user)
            .parameters(Map.of("tags", "" + i)));
      }

      // when
      service.saveNewActions(siteId, documentId, actions);

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
      String userId0 = "joe";
      String name = "test94832";
      String documentId = ID.uuid();

      Action action0 = new Action().type(ActionType.QUEUE).userId(userId0).queueId(name);

      // when
      service.saveNewActions(siteId, documentId, List.of(action0));

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
      Action action0 = new Action().documentId(docId).index("0").type(ActionType.QUEUE)
          .userId("joe").queueId("A");

      // when
      service.saveAction(siteId, action0);

      // then
      List<Action> actions = service.getActions(siteId, docId);
      assertEquals(1, actions.size());
      assertEquals("0", actions.get(0).index());

      // given - Ulid index
      Action ulid0 = new Action().documentId(docId).indexUlid().type(ActionType.OCR).userId("joe")
          .insertedDate(new Date());
      Action ulid1 = new Action().documentId(docId).indexUlid().type(ActionType.IDP).userId("joe")
          .insertedDate(new Date());
      Action ulid2 = new Action().documentId(docId).indexUlid().type(ActionType.FULLTEXT)
          .userId("joe").insertedDate(new Date());

      // when
      service.saveActions(siteId, List.of(ulid2, ulid1, ulid0));

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
      String userId0 = "joe";
      Action action0 =
          new Action().type(ActionType.OCR).userId(userId0).parameters(Map.of("test", "1234"));
      service.saveNewActions(siteId, documentId, List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      action0.status(ActionStatus.COMPLETE);

      // when
      service.updateActionStatus(siteId, documentId, action0);

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
      String userId0 = "joe";
      Action action0 = new Action().type(ActionType.QUEUE).userId(userId0).queueId(queueId);
      service.saveNewActions(siteId, documentId, List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      action0.status(ActionStatus.IN_QUEUE);

      // when
      service.updateActionStatus(siteId, documentId, action0);

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
      action0.status(ActionStatus.COMPLETE);

      // when
      service.updateActionStatus(siteId, documentId, action0);

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
      String userId0 = "joe";
      Action action0 = new Action().type(ActionType.QUEUE).userId(userId0).queueId(name);
      service.saveNewActions(siteId, documentId, List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, documentId).get(0).status());

      action0.status(ActionStatus.FAILED);

      // when
      service.updateActionStatus(siteId, documentId, action0);

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
