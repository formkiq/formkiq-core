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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentSync;
import com.formkiq.client.model.AddDocumentSyncRequest;
import com.formkiq.client.model.AddDocumentSyncService;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.DocumentSync;
import com.formkiq.client.model.DocumentSyncService;
import com.formkiq.client.model.DocumentSyncStatus;
import com.formkiq.client.model.DocumentSyncType;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.client.model.GetDocumentSyncResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}/syncs. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
// @Execution(ExecutionMode.CONCURRENT)
public class ApiDocumentSyncRequestHandlerTest extends AbstractApiClientRequestTest {

  /**
   * Get /documents/{documentId}/syncs request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetDocumentSyncs01() throws Exception {

    String userId = "joe";
    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(DocumentService.class, new DocumentServiceExtension());
    awsServices.register(com.formkiq.stacks.dynamodb.DocumentSyncService.class,
        new DocumentSyncServiceExtension());
    awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    DocumentService service = awsServices.getExtension(DocumentService.class);
    com.formkiq.stacks.dynamodb.DocumentSyncService syncService =
        awsServices.getExtension(com.formkiq.stacks.dynamodb.DocumentSyncService.class);

    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      service.saveDocument(siteId, item, null);

      syncService.saveSync(siteId, documentId,
          com.formkiq.aws.dynamodb.model.DocumentSyncServiceType.OPENSEARCH,
          com.formkiq.aws.dynamodb.model.DocumentSyncStatus.COMPLETE,
          com.formkiq.aws.dynamodb.model.DocumentSyncType.METADATA, false);
      TimeUnit.SECONDS.sleep(1);
      syncService.saveSync(siteId, documentId,
          com.formkiq.aws.dynamodb.model.DocumentSyncServiceType.TYPESENSE,
          com.formkiq.aws.dynamodb.model.DocumentSyncStatus.FAILED,
          com.formkiq.aws.dynamodb.model.DocumentSyncType.METADATA, false);

      // when
      List<DocumentSync> list = getDocumentSyncs(siteId, documentId);

      // then
      final int expected = 3;
      assertEquals(expected, list.size());

      assertEquals(com.formkiq.client.model.DocumentSyncService.TYPESENSE,
          list.get(0).getService());
      assertEquals(com.formkiq.client.model.DocumentSyncStatus.FAILED, list.get(0).getStatus());
      assertEquals(DocumentSyncType.METADATA, list.get(0).getType());
      assertNotNull(list.get(0).getSyncDate());

      assertEquals(com.formkiq.client.model.DocumentSyncService.OPENSEARCH,
          list.get(1).getService());
      assertEquals(com.formkiq.client.model.DocumentSyncStatus.COMPLETE, list.get(1).getStatus());
      assertEquals(com.formkiq.client.model.DocumentSyncType.METADATA, list.get(1).getType());
      assertNotNull(list.get(1).getSyncDate());

      assertEquals(com.formkiq.client.model.DocumentSyncService.EVENTBRIDGE,
          list.get(2).getService());
      assertEquals(com.formkiq.client.model.DocumentSyncStatus.PENDING, list.get(2).getStatus());
      assertEquals(com.formkiq.client.model.DocumentSyncType.METADATA, list.get(2).getType());
      assertNull(list.get(2).getSyncDate());
    }
  }

  private List<DocumentSync> getDocumentSyncs(final String siteId, final String documentId)
      throws ApiException {
    GetDocumentSyncResponse response =
        this.documentsApi.getDocumentSyncs(documentId, siteId, null, null);
    return notNull(response.getSyncs());
  }

  private List<DocumentAction> getDocumentActions(final String siteId, final String documentId)
      throws ApiException {
    GetDocumentActionsResponse response =
        this.documentActionsApi.getDocumentActions(documentId, siteId, null, null, null);
    return notNull(response.getActions());
  }


  /**
   * POST /documents/{documentId}/syncs request. Invalid Request.
   */
  @Test
  public void testAddDocumentSyncs01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String documentId = ID.uuid();

      // when
      AddDocumentSyncRequest req = new AddDocumentSyncRequest();

      try {
        this.documentsApi.addDocumentSync(documentId, siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Invalid request\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. Invalid Request.
   */
  @Test
  public void testAddDocumentSyncs02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String documentId = ID.uuid();

      AddDocumentSyncRequest req = new AddDocumentSyncRequest();
      req.setSync(new AddDocumentSync());

      // when
      try {
        this.documentsApi.addDocumentSync(documentId, siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Invalid Sync Service\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. Sync Content.
   */
  @Test
  public void testAddDocumentSyncs03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<AddDocumentSyncService> services = getNotEventBridgeServices();
      for (AddDocumentSyncService service : services) {

        setBearerToken(siteId);
        String documentId = this.documentsApi
            .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.CONTENT));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(1, actions.size());
        assertEquals(DocumentActionType.FULLTEXT, actions.get(0).getType());

        List<DocumentSync> list = getDocumentSyncs(siteId, documentId);
        assertEquals(1, list.size());

        assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.METADATA);
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. Invalid Request Combo.
   */
  @Test
  public void testAddDocumentSyncs04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String documentId = this.documentsApi
          .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

      AddDocumentSyncRequest req = new AddDocumentSyncRequest();
      req.setSync(new AddDocumentSync().type(DocumentSyncType.CONTENT)
          .service(AddDocumentSyncService.EVENTBRIDGE));

      // when
      AddResponse response = this.documentsApi.addDocumentSync(documentId, siteId, req);

      // then
      assertEquals("Added Document sync", response.getMessage());
      List<DocumentSync> list = getDocumentSyncs(siteId, documentId);
      assertEquals(2, list.size());
      assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.CONTENT);
      assertDocumentSyncEventBridge(list.get(1), DocumentSyncType.METADATA);
    }
  }

  private void assertDocumentSyncEventBridge(final DocumentSync sync, final DocumentSyncType type) {
    assertEquals(DocumentSyncService.EVENTBRIDGE, sync.getService());
    assertEquals(DocumentSyncStatus.PENDING, sync.getStatus());
    assertEquals(type, sync.getType());
  }

  /**
   * POST /documents/{documentId}/syncs request. Missing Document.
   */
  @Test
  public void testAddDocumentSyncs05() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String documentId = ID.uuid();

      AddDocumentSyncRequest req = new AddDocumentSyncRequest();
      req.setSync(new AddDocumentSync().type(DocumentSyncType.CONTENT)
          .service(AddDocumentSyncService.EVENTBRIDGE));

      // when
      try {
        this.documentsApi.addDocumentSync(documentId, siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. Sync Metadata.
   */
  @Test
  public void testAddDocumentSyncs06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (AddDocumentSyncService service : getAddDocumentSyncServices()) {

        setBearerToken(siteId);
        String documentId = this.documentsApi
            .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.METADATA));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(0, actions.size());

        List<DocumentSync> list = getDocumentSyncs(siteId, documentId);
        if (AddDocumentSyncService.EVENTBRIDGE.equals(service)) {

          assertEquals(2, list.size());

          assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.METADATA);

          assertDocumentSyncEventBridge(list.get(1), DocumentSyncType.METADATA);

        } else {
          assertEquals(1, list.size());

          assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.METADATA);
        }
      }
    }
  }

  private static List<AddDocumentSyncService> getAddDocumentSyncServices() {
    return List.of(AddDocumentSyncService.FULLTEXT, AddDocumentSyncService.EVENTBRIDGE);
  }

  /**
   * POST /documents/{documentId}/syncs request. Sync Metadata.
   */
  @Test
  public void testAddDocumentSyncs07() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (AddDocumentSyncService service : getAddDocumentSyncServices()) {

        setBearerToken(siteId);
        String documentId = this.documentsApi
            .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.METADATA));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(0, actions.size());

        List<DocumentSync> list = getDocumentSyncs(siteId, documentId);
        if (AddDocumentSyncService.EVENTBRIDGE.equals(service)) {

          assertEquals(2, list.size());

          assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.METADATA);

          assertDocumentSyncEventBridge(list.get(1), DocumentSyncType.METADATA);

        } else {
          assertEquals(1, list.size());

          assertDocumentSyncEventBridge(list.get(0), DocumentSyncType.METADATA);
        }
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. DELETE / SOFT_DELETE not EVENTBRIDGE.
   */
  @Test
  public void testAddDocumentSyncs08() throws ApiException {
    // given
    List<AddDocumentSyncService> services = getNotEventBridgeServices();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (AddDocumentSyncService service : services) {

        setBearerToken(siteId);
        String documentId = this.documentsApi
            .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.DELETE));

        try {
          // when
          this.documentsApi.addDocumentSync(documentId, siteId, req);
          fail();
        } catch (ApiException e) {
          assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
          assertEquals(
              "{\"errors\":[{\"key\":\"type\","
                  + "\"error\":\"unsupport type 'DELETE' for service 'TYPESENSE'\"}]}",
              e.getResponseBody());
        }
      }
    }
  }

  /**
   * POST /documents/{documentId}/syncs request. DELETE / SOFT_DELETE with EVENTBRIDGE.
   */
  @Test
  public void testAddDocumentSyncs09() throws ApiException {
    // given
    AddDocumentSyncService service = AddDocumentSyncService.EVENTBRIDGE;
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (DocumentSyncType type : List.of(DocumentSyncType.DELETE, DocumentSyncType.SOFT_DELETE)) {

        setBearerToken(siteId);
        String documentId = this.documentsApi
            .addDocument(new AddDocumentRequest().content("asd"), siteId, null).getDocumentId();

        AddDocumentSyncRequest req =
            new AddDocumentSyncRequest().sync(new AddDocumentSync().service(service).type(type));

        // when
        AddResponse response = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", response.getMessage());

        List<DocumentSync> syncs = getDocumentSyncs(siteId, documentId);
        assertEquals(2, syncs.size());
        assertDocumentSyncEventBridge(syncs.get(0), type);
      }
    }
  }

  private static List<AddDocumentSyncService> getNotEventBridgeServices() {
    return List.of(AddDocumentSyncService.FULLTEXT);
  }
}
