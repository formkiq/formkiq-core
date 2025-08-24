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
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceExtension;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentSync;
import com.formkiq.client.model.AddDocumentSyncRequest;
import com.formkiq.client.model.AddDocumentSyncService;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Document;
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
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/** Unit Tests for request /documents/{documentId}/syncs. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentSyncRequestHandlerTest extends AbstractApiClientRequestTest
    implements DbKeys {

  /**
   * Get /documents/{documentId}/syncs request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetDocumentSyncs01() throws Exception {

    String userId = "joe";
    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(DynamoDbService.class, new DynamoDbServiceExtension());
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
      final int expected = 2;
      assertEquals(expected, list.size());
      assertDocumentSync(list.get(0), com.formkiq.client.model.DocumentSyncService.TYPESENSE,
          com.formkiq.client.model.DocumentSyncStatus.FAILED, DocumentSyncType.METADATA);
      assertDocumentSync(list.get(1), com.formkiq.client.model.DocumentSyncService.OPENSEARCH,
          DocumentSyncStatus.COMPLETE, DocumentSyncType.METADATA);
    }
  }

  private void assertDocumentSync(final DocumentSync sync, final DocumentSyncService service,
      final DocumentSyncStatus status, final DocumentSyncType type) {
    assertEquals(service, sync.getService());
    assertEquals(status, sync.getStatus());
    assertEquals(type, sync.getType());
    if (DocumentSyncStatus.PENDING.equals(status)) {
      assertNull(sync.getSyncDate());
    } else {
      assertNotNull(sync.getSyncDate());
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
        assertNotNull(documentId);

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.CONTENT));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(1, actions.size());
        assertEquals(DocumentActionType.FULLTEXT, actions.get(0).getType());
      }
    }
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
          .service(AddDocumentSyncService.FULLTEXT));

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
   * POST /documents/{documentId}/syncs request. Sync Metadata with attributes.
   */
  @Test
  public void testAddDocumentSyncs06() throws ApiException, URISyntaxException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (AddDocumentSyncService service : getAddDocumentSyncServices()) {

        setBearerToken(siteId);
        String attributeKey = "myattr_" + ID.uuid();
        this.attributesApi.addAttribute(
            new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

        AddDocumentRequest addReq = new AddDocumentRequest().content("asd")
            .addAttributesItem(new AddDocumentAttribute(
                new AddDocumentAttributeStandard().key(attributeKey).stringValue("555")))
            .addTagsItem(new AddDocumentTag().key("mytag").value("123"));

        String documentId = this.documentsApi.addDocument(addReq, siteId, null).getDocumentId();
        assertNotNull(documentId);

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.METADATA));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(0, actions.size());

        if (AddDocumentSyncService.FULLTEXT.equals(service)) {
          verifyStreamTriggeredDate(siteId, documentId);
        }
      }
    }
  }

  private void verifyStreamTriggeredDate(final String siteId, final String documentId)
      throws URISyntaxException {
    try (DynamoDbClient db = DynamoDbTestServices.getDynamoDbConnection().build()) {
      String pk = keysDocument(siteId, documentId).get(PK).s();
      QueryRequest query = DynamoDbQueryBuilder.builder().pk(pk).build(DOCUMENTS_TABLE);
      QueryResponse response = db.query(query);

      final int expected = 3;
      assertEquals(expected, response.items().size());

      int i = 0;
      assertNotNull(response.items().get(i).get("streamTriggeredDate").s());
      assertTrue(response.items().get(i++).get(SK).s().startsWith("attr#myattr"));
      assertNotNull(response.items().get(i).get("streamTriggeredDate").s());
      assertEquals("document", response.items().get(i++).get(SK).s());
      assertNotNull(response.items().get(i).get("streamTriggeredDate").s());
      assertEquals("tags#mytag", response.items().get(i).get(SK).s());
    }
  }

  private static List<AddDocumentSyncService> getAddDocumentSyncServices() {
    return List.of(AddDocumentSyncService.FULLTEXT);
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
        assertNotNull(documentId);

        // when
        AddDocumentSyncRequest req = new AddDocumentSyncRequest()
            .sync(new AddDocumentSync().service(service).type(DocumentSyncType.METADATA));
        AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, siteId, req);

        // then
        assertEquals("Added Document sync", addResponse.getMessage());
        List<DocumentAction> actions = getDocumentActions(siteId, documentId);
        assertEquals(0, actions.size());
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
        assertNotNull(documentId);

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
   * Test Add Document Syncs retry.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentSyncs10() throws ApiException {
    // given
    AwsServiceCache awsServices = getAwsServices();
    DynamoDbConnectionBuilder connection =
        awsServices.getExtension(DynamoDbConnectionBuilder.class);
    DynamoDbService db = new DynamoDbServiceImpl(connection, DOCUMENT_SYNCS_TABLE);

    setBearerToken((String) null);

    String documentId = this.documentsApi
        .addDocument(new AddDocumentRequest().content("test"), null, null).getDocumentId();
    assertNotNull(documentId);

    createSyncRecords(db, documentId);

    // when
    List<DocumentSync> syncs =
        notNull(this.documentsApi.getDocumentSyncs(documentId, null, null, null).getSyncs());

    // then
    assertEquals(2, syncs.size());
    assertDocumentSync(syncs.get(0), DocumentSyncService.TYPESENSE, DocumentSyncStatus.FAILED,
        DocumentSyncType.METADATA);
    assertDocumentSync(syncs.get(1), DocumentSyncService.TYPESENSE, DocumentSyncStatus.COMPLETE,
        DocumentSyncType.METADATA);

    List<Document> docs = notNull(this.documentsApi
        .getDocuments(null, null, "FULLTEXT_METADATA_FAILED", null, null, null, null, null, null)
        .getDocuments());
    assertEquals(1, docs.size());
    assertEquals(documentId, docs.get(0).getDocumentId());

    // when
    AddResponse addResponse = this.documentsApi.addDocumentSync(documentId, null,
        new AddDocumentSyncRequest().sync(new AddDocumentSync().type(DocumentSyncType.METADATA)
            .service(AddDocumentSyncService.FULLTEXT)));

    // then
    assertEquals("Added Document sync", addResponse.getMessage());

    syncs = notNull(this.documentsApi.getDocumentSyncs(documentId, null, null, null).getSyncs());
    assertEquals(2, syncs.size());

    assertDocumentSync(syncs.get(0), DocumentSyncService.TYPESENSE, DocumentSyncStatus.FAILED_RETRY,
        DocumentSyncType.METADATA);
    assertDocumentSync(syncs.get(1), DocumentSyncService.TYPESENSE, DocumentSyncStatus.COMPLETE,
        DocumentSyncType.METADATA);

    docs = notNull(this.documentsApi
        .getDocuments(null, null, "FULLTEXT_METADATA_FAILED", null, null, null, null, null, null)
        .getDocuments());
    assertEquals(0, docs.size());
  }

  private void createSyncRecords(final DynamoDbService db, final String documentId) {
    LocalDate date = LocalDate.now();

    final int days = 3;
    List<DocumentSyncRecord> records = List.of(
        createSyncRecord(documentId, DocumentSyncServiceType.TYPESENSE,
            com.formkiq.aws.dynamodb.model.DocumentSyncStatus.FAILED,
            com.formkiq.aws.dynamodb.model.DocumentSyncType.METADATA, date.plusDays(days)),
        createSyncRecord(documentId, DocumentSyncServiceType.TYPESENSE,
            com.formkiq.aws.dynamodb.model.DocumentSyncStatus.COMPLETE,
            com.formkiq.aws.dynamodb.model.DocumentSyncType.METADATA, date.plusDays(days - 1)));

    db.putItems(records.stream().map(r -> r.getAttributes(null)).toList());
  }

  private DocumentSyncRecord createSyncRecord(final String documentId,
      final DocumentSyncServiceType service,
      final com.formkiq.aws.dynamodb.model.DocumentSyncStatus status,
      final com.formkiq.aws.dynamodb.model.DocumentSyncType type, final LocalDate localDate) {
    ZoneId zone = ZoneId.of("UTC");
    Date date = Date.from(localDate.atStartOfDay(zone).toInstant());
    return new DocumentSyncRecord().setDocumentId(documentId).setService(service).setStatus(status)
        .setInsertedDate(date).setType(type).setSyncDate(date);
  }

  private static List<AddDocumentSyncService> getNotEventBridgeServices() {
    return List.of(AddDocumentSyncService.FULLTEXT);
  }
}
