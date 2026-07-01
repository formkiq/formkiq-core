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
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.aws.dynamodb.actions.ActionBuilder;
import com.formkiq.aws.dynamodb.actions.ActionStatus;
import com.formkiq.aws.dynamodb.actions.ActionType;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.testutils.aws.TestEnvironment;
import com.formkiq.testutils.aws.TestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
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
   */
  @BeforeAll
  public static void beforeAll() {

    var awsCredentialsProvider = TestEnvironment.createCredentials();
    var environment = TestEnvironment.builder().build();
    var awsServiceCache = new AwsServiceCacheBuilder(environment, TestServices.getEndpointMap(),
        awsCredentialsProvider).addService(new DynamoDbAwsServiceRegistry())
        .addService(new S3AwsServiceRegistry()).build();

    awsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    awsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    awsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServiceCache.register(S3Service.class, new S3ServiceExtension());

    ApiAuthorization.login(new ApiAuthorization().username("System"));

    service = awsServiceCache.getExtension(ActionsService.class);
    documentService = awsServiceCache.getExtension(DocumentService.class);
  }

  private ActionBuilder createAction(final DocumentArtifact document, final ActionType actionType) {
    return new ActionBuilder().document(document).indexUlid().type(actionType).userId("joe");
  }

  /**
   * Has Actions.
   */
  @Test
  public void hasActions01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentArtifact documentId0 = DocumentArtifact.of(ID.uuid(), null);
      DocumentArtifact documentId1 = DocumentArtifact.of(ID.uuid(), null);

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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      documentService.saveDocument(siteId, item, null);

      Action action0 =
          createAction(document, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));

      // when
      documentService.deleteDocument(siteId, DocumentArtifact.of(documentId, null), false);

      // then
      List<Action> actions = service.getActions(siteId, document);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      documentService.saveDocument(siteId, item, null);

      Action action0 =
          createAction(document, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));

      // when
      service.deleteActions(siteId, document);

      // then
      List<Action> actions = service.getActions(siteId, document);
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
    DocumentArtifact document = DocumentArtifact.of(documentId, null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      Action action0 = createAction(document, ActionType.DOCUMENTTAGGING)
          .parameters(Map.of("tags", "type")).status(ActionStatus.COMPLETE).build(siteId);

      Action action1 = createAction(document, ActionType.FULLTEXT).build(siteId);
      ActionBuilder insertedAction = createAction(document, ActionType.OCR);

      List<Action> actions = Arrays.asList(action0, action1);
      service.saveNewActions(actions);
      assertEquals(2, service.getActions(siteId, document).size());

      // when
      service.insertBeforeAction(siteId, action1, insertedAction);

      // then
      final int expected = 3;
      List<Action> list = service.getActions(siteId, document);
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
      DocumentArtifact documentId0 = DocumentArtifact.of(ID.uuid(), null);
      DocumentArtifact documentId1 = DocumentArtifact.of(ID.uuid(), null);

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
        assertEquals(siteId + "/docs#" + documentId0.documentId(), list.getFirst().get("PK").s());
      } else {
        assertEquals("docs#" + documentId0.documentId(), list.getFirst().get("PK").s());
      }
      String sk = list.getFirst().get("SK").s();
      assertTrue(sk.startsWith("action#"));
      assertTrue(sk.endsWith("#OCR"));

      List<Action> results = service.getActions(siteId, documentId0);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.PENDING, results.getFirst().status());
      assertEquals(ActionType.OCR, results.getFirst().type());
      assertEquals(userId0, results.getFirst().userId());
      assertEquals("{test=1234}", results.getFirst().parameters().toString());

      results = service.getActions(siteId, documentId1);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.COMPLETE, results.getFirst().status());
      assertEquals(ActionType.OCR, results.getFirst().type());
      assertEquals(userId1, results.getFirst().userId());
      assertNull(results.getFirst().parameters());

      // given
      Action action = new ActionBuilder().action(action0).status(ActionStatus.FAILED).build(siteId);

      // when
      service.updateAction(action);

      // then
      results = service.getActions(siteId, documentId0);
      assertEquals(ActionStatus.FAILED, results.getFirst().status());
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
    DocumentArtifact document = DocumentArtifact.of(documentId, null);
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<Action> actions = new ArrayList<>();
      for (int i = 0; i < numberOfActions; i++) {
        actions.add(createAction(document, ActionType.DOCUMENTTAGGING)
            .parameters(Map.of("tags", "" + i)).build(siteId));
      }

      // when
      service.saveNewActions(actions);

      // then
      int i = 0;
      List<Action> list = service.getActions(siteId, document);
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      Action action0 = createAction(document, ActionType.QUEUE).queueId(name).build(siteId);

      // when
      service.saveNewActions(List.of(action0));

      // then
      List<Action> results = service.getActions(siteId, document);
      assertEquals(1, results.size());
      assertEquals(ActionStatus.PENDING, results.getFirst().status());
      assertEquals(ActionType.QUEUE, results.getFirst().type());
      assertEquals(userId0, results.getFirst().userId());
      assertEquals("test94832", results.getFirst().queueId());

      assertEquals(0, service.findDocumentsInQueue(siteId, name, null, 2).getResults().size());
      assertNull(service.findActionInQueue(siteId, document, name));
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
      List<Action> actions = service.getActions(siteId, document);
      assertEquals(1, actions.size());
      assertEquals("0", actions.getFirst().index());

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
      actions = service.getActions(siteId, document);

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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      Action action0 =
          createAction(document, ActionType.OCR).parameters(Map.of("test", "1234")).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, document).getFirst().status());

      Action action =
          new ActionBuilder().action(action0).status(ActionStatus.COMPLETE).build(siteId);
      // action0.status(ActionStatus.COMPLETE);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.COMPLETE, service.getActions(siteId, document).getFirst().status());
      Pagination<DocumentArtifact> results =
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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      Action action0 = createAction(document, ActionType.QUEUE).queueId(queueId).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, document).getFirst().status());

      Action action =
          new ActionBuilder().action(action0).status(ActionStatus.IN_QUEUE).build(siteId);

      // action0.status(ActionStatus.IN_QUEUE);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.IN_QUEUE, service.getActions(siteId, document).getFirst().status());
      Pagination<Action> docs = service.findDocumentsInQueue(siteId, queueId, null, LIMIT);
      assertEquals(1, docs.getResults().size());
      assertEquals(queueId, docs.getResults().getFirst().queueId());
      assertNotNull(service.findActionInQueue(siteId, document, queueId));

      Pagination<DocumentArtifact> results =
          service.findDocumentsWithStatus(siteId, ActionStatus.IN_QUEUE, null, LIMIT);
      assertEquals(1, results.getResults().size());
      assertEquals(documentId, results.getResults().getFirst().documentId());

      // given
      Action a = new ActionBuilder().action(action0).status(ActionStatus.COMPLETE).build(siteId);
      // action0.status(ActionStatus.COMPLETE);

      // when
      service.updateAction(a);

      // then
      assertEquals(ActionStatus.COMPLETE, service.getActions(siteId, document).getFirst().status());
      docs = service.findDocumentsInQueue(siteId, queueId, null, LIMIT);
      assertEquals(0, docs.getResults().size());
      assertNull(service.findActionInQueue(siteId, document, queueId));

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
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      Action action0 = createAction(document, ActionType.QUEUE).queueId(name).build(siteId);
      service.saveNewActions(List.of(action0));
      assertEquals(ActionStatus.PENDING, service.getActions(siteId, document).getFirst().status());

      Action action = new ActionBuilder().action(action0).status(ActionStatus.FAILED).build(siteId);
      // action0.status(ActionStatus.FAILED);

      // when
      service.updateAction(action);

      // then
      assertEquals(ActionStatus.FAILED, service.getActions(siteId, document).getFirst().status());
      Pagination<Action> docs = service.findDocumentsInQueue(siteId, name, null, LIMIT);
      assertEquals(0, docs.getResults().size());
      assertNull(service.findActionInQueue(siteId, document, name));

      Pagination<DocumentArtifact> results =
          service.findDocumentsWithStatus(siteId, ActionStatus.FAILED, null, LIMIT);
      assertEquals(1, results.getResults().size());
      assertEquals(documentId, results.getResults().getFirst().documentId());

      results = service.findDocumentsWithStatus(siteId, ActionStatus.PENDING, null, LIMIT);
      assertEquals(0, results.getResults().size());
    }
  }
}
