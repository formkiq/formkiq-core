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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaOptional;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.ChecksumType;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSync;
import com.formkiq.client.model.DocumentSyncType;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.SetDocumentRestoreResponse;

/** Unit Tests for request /documents/{documentId}. */
public class DocumentsIdRequestTest extends AbstractApiClientRequestTest {

  /** Test Timeout. */
  private static final int TEST_TIMEOUT = 10;

  @BeforeEach
  void beforeEach() {
    clearSqsMessages();
  }

  /**
   * DELETE /documents/{documentId} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().path("test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.FALSE);

      // then
      List<Document> softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(0, softDeletedDocuments.size());

      List<Document> documents = getDocuments(siteId);
      assertEquals(0, documents.size());
    }
  }

  /**
   * DELETE /documents/{documentId} deeplink document request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete02() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().deepLinkPath("https://www.google.com");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.FALSE);

      // then
      List<Document> softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(0, softDeletedDocuments.size());

      List<Document> documents = getDocuments(siteId);
      assertEquals(0, documents.size());
    }
  }

  /**
   * DELETE /documents/{documentId} deeplink document soft delete request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete03() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().deepLinkPath("https://www.google.com");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.TRUE);

      // then
      List<Document> softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(1, softDeletedDocuments.size());
      assertEquals(documentId, softDeletedDocuments.get(0).getDocumentId());

      List<Document> documents = getDocuments(siteId);
      assertEquals(0, documents.size());

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.FALSE);

      // then
      softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(0, softDeletedDocuments.size());

      documents = getDocuments(siteId);
      assertEquals(0, documents.size());
    }
  }

  /**
   * DELETE /documents/{documentId} document not found.
   *
   */
  @Test
  public void testDocumentDelete04() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = ID.uuid();

      // when
      try {
        this.documentsApi.deleteDocument(documentId, siteId, Boolean.FALSE);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  private List<Document> getDocuments(final String siteId) throws ApiException {
    return notNull(this.documentsApi.getDocuments(siteId, null, null, null, null, null, null, null)
        .getDocuments());
  }

  private List<Document> getDocuments(final String siteId, final int expected)
      throws ApiException, InterruptedException {
    List<Document> documents = getDocuments(siteId);
    while (documents.size() != expected) {
      TimeUnit.SECONDS.sleep(1);
      documents = getDocuments(siteId);
    }

    return documents;
  }

  private List<Document> getSoftDeletedDocuments(final String siteId) throws ApiException {
    return notNull(this.documentsApi
        .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, null).getDocuments());
  }


  /**
   * PUT /documents/{documentId}/restore request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testHandleSetDocumentRestore01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String path = ID.uuid() + ".txt";
      AddDocumentUploadRequest req = new AddDocumentUploadRequest().path(path);
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.TRUE);

      // then
      List<Document> softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(1, softDeletedDocuments.size());
      assertEquals(path, softDeletedDocuments.get(0).getPath());

      List<Document> documents = getDocuments(siteId, 0);
      assertEquals(0, documents.size());

      // when
      SetDocumentRestoreResponse restore = this.documentsApi.setDocumentRestore(documentId, siteId);

      // then
      assertEquals("document restored", restore.getMessage());
      softDeletedDocuments = getSoftDeletedDocuments(siteId);
      assertEquals(0, softDeletedDocuments.size());
      documents = getDocuments(siteId, 1);
      assertEquals(1, documents.size());
      assertEquals(path, documents.get(0).getPath());

      List<DocumentSync> syncs =
          notNull(this.documentsApi.getDocumentSyncs(documentId, siteId, null, null).getSyncs());
      assertEquals(2, syncs.size());
      assertEquals(DocumentSyncType.SOFT_DELETE_METADATA, syncs.get(0).getType());
      assertEquals(DocumentSyncType.METADATA, syncs.get(1).getType());
    }
  }

  /**
   * PUT /documents/{documentId}/restore request, document not found.
   *
   */
  @Test
  public void testHandleSetDocumentRestore02() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();

      // when
      try {
        this.documentsApi.setDocumentRestore(documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        final int code = 404;
        assertEquals(code, e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET /documents/{documentId} request, deeplink.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().deepLinkPath("s3://test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);
      String documentId = response.getDocumentId();

      // when
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);

      // then
      assertEquals("s3://test.txt", document.getDeepLinkPath());
      assertEquals("test.txt", document.getPath());
    }
  }

  /**
   * GET /documents/{documentId} request, deeplink url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String deepLink = "https://docs.google.com/document/d/sdflhsdfjeiwrwr";
      AddDocumentUploadRequest req = new AddDocumentUploadRequest().deepLinkPath(deepLink);
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);
      String documentId = response.getDocumentId();

      // when
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);

      // then
      assertEquals(deepLink, document.getDeepLinkPath());
      assertEquals("sdflhsdfjeiwrwr", document.getPath());
    }
  }


  /**
   * GET /documents/{documentId} request, deeplink url and path.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String deepLink = "https://docs.google.com/document/d/sdflhsdfjeiwrwr";
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().deepLinkPath(deepLink).path("apath.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);
      String documentId = response.getDocumentId();

      // when
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);

      // then
      assertEquals(deepLink, document.getDeepLinkPath());
      assertEquals("apath.txt", document.getPath());
    }
  }

  /**
   * POST /documents request, deeplink.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String deepLinkPath = "https://google.com/test/sample.pdf";

      AddDocumentRequest req =
          new AddDocumentRequest().deepLinkPath(deepLinkPath).contentType("application/pdf");

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("sample.pdf", document.getPath());
      assertEquals("https://google.com/test/sample.pdf", document.getDeepLinkPath());
      assertEquals("application/pdf", document.getContentType());
    }
  }

  /**
   * POST /documents request, with action queue.
   *
   */
  @Test
  public void testHandleAddDocument03() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      AddDocumentRequest req =
          new AddDocumentRequest().content("SKADJASKDSA").contentType("text/plain")
              .addActionsItem(new AddAction().type(DocumentActionType.QUEUE).queueId("test"));

      // when
      try {
        this.documentsApi.addDocument(req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"queueId\",\"error\":\"'queueId' does not exist\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Update Document Content.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate01() throws Exception {
    // given
    String content0 = "test data";
    String content1 = "new data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content0)
          .width("100").height("200");

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();
      assertEquals(content0,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

      GetDocumentResponse doc = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("100", doc.getWidth());
      assertEquals("200", doc.getHeight());

      // given
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().content(content1).width("300").height("400");

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      assertEquals(content1,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

      doc = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("300", doc.getWidth());
      assertEquals("400", doc.getHeight());
    }
  }

  /**
   * Update Document actions.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate02() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content0);

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();
      assertEquals(content0,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

      // given
      UpdateDocumentRequest updateReq = new UpdateDocumentRequest()
          .addActionsItem(new AddAction().type(DocumentActionType.FULLTEXT));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      List<DocumentAction> actions = notNull(this.documentActionsApi
          .getDocumentActions(documentId, siteId, null, null, null).getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.FULLTEXT, actions.get(0).getType());
    }
  }

  /**
   * Update invalid Document actions.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate03() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content0);

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();

      // given
      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().addActionsItem(new AddAction());

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"type\",\"error\":\"action 'type' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Update Document with invalid attributes.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate04() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String attributeKey = "test";
      addAttribute(siteId, attributeKey);

      AddDocumentRequest req = new AddDocumentRequest().content(content0);
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().addAttributesItem(
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key(attributeKey)));

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"test\","
            + "\"error\":\"attribute only support string value\"}]}", e.getResponseBody());
      }
    }
  }

  private void addAttribute(final String siteId, final String attributeKey) throws ApiException {
    this.attributesApi.addAttribute(
        new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);
  }

  /**
   * Update Document with invalid schema allowed value.
   */
  @Test
  public void testUpdate05() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String attributeKey = "test";

      addAttribute(siteId, attributeKey);

      SetSitesSchemaRequest sitesSchema = new SetSitesSchemaRequest().name("test")
          .attributes(new SetSchemaAttributes().addOptionalItem(new AddAttributeSchemaOptional()
              .attributeKey(attributeKey).addAllowedValuesItem("abc")));
      this.schemasApi.setSitesSchema(siteId, sitesSchema);

      AddDocumentRequest req = new AddDocumentRequest().content(content0);
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey).stringValue("123")));

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"test\","
                + "\"error\":\"invalid attribute value 'test', only allowed values are abc\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Update Document with attributes, check composite keys.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate06() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      String attributeKey0 = "category";
      String attributeKey1 = "documentType";

      createSiteSchema(siteId, attributeKey0, attributeKey1);

      AddDocumentRequest req = new AddDocumentRequest().content(content0);
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest()
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey0).stringValue("person")))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey1).stringValue("privacy")));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 3;
      assertEquals(expected, attributes.size());
      assertEquals(attributeKey0, attributes.get(0).getKey());
      assertEquals("person", attributes.get(0).getStringValue());
      assertEquals(attributeKey0 + DbKeys.COMPOSITE_KEY_DELIM + attributeKey1,
          attributes.get(1).getKey());
      assertEquals("person" + DbKeys.COMPOSITE_KEY_DELIM + "privacy",
          attributes.get(1).getStringValue());
      assertEquals(attributeKey1, attributes.get(2).getKey());
      assertEquals("privacy", attributes.get(2).getStringValue());
    }
  }

  private void createSiteSchema(final String siteId, final String attributeKey0,
      final String attributeKey1) throws ApiException {

    List<String> list = Arrays.asList("userId", "playerId", attributeKey0, attributeKey1);

    for (String attributeKey : list) {
      addAttribute(siteId, attributeKey);
    }

    AttributeSchemaCompositeKey compositeKey0 = new AttributeSchemaCompositeKey()
        .attributeKeys(Arrays.asList(attributeKey0, attributeKey1));

    AttributeSchemaCompositeKey compositeKey1 =
        new AttributeSchemaCompositeKey().attributeKeys(Arrays.asList("userId", "playerId"));

    AttributeSchemaCompositeKey compositeKey2 = new AttributeSchemaCompositeKey()
        .attributeKeys(Arrays.asList("userId", "playerId", attributeKey0, attributeKey1));

    AttributeSchemaCompositeKey compositeKey3 = new AttributeSchemaCompositeKey()
        .attributeKeys(Arrays.asList("userId", attributeKey0, attributeKey1));

    SetSitesSchemaRequest sitesSchema =
        new SetSitesSchemaRequest().name("test").attributes(new SetSchemaAttributes()
            .compositeKeys(List.of(compositeKey0, compositeKey1, compositeKey2, compositeKey3)));
    this.schemasApi.setSitesSchema(siteId, sitesSchema);
  }

  /**
   * Add Document with 1 attribute, Update Document with 1 attributes, check composite keys.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate07() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      String attributeKey0 = "category";
      String attributeKey1 = "documentType";

      createSiteSchema(siteId, attributeKey0, attributeKey1);

      AddDocumentRequest req =
          new AddDocumentRequest().content(content0).addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey0).stringValue("person")));
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest()
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey1).stringValue("privacy")))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("userId").stringValue("123")));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 5;
      int i = 0;
      assertEquals(expected, attributes.size());
      assertEquals(attributeKey0, attributes.get(i).getKey());
      assertEquals("person", attributes.get(i++).getStringValue());
      assertEquals(attributeKey0 + DbKeys.COMPOSITE_KEY_DELIM + attributeKey1,
          attributes.get(i).getKey());
      assertEquals("person" + DbKeys.COMPOSITE_KEY_DELIM + "privacy",
          attributes.get(i++).getStringValue());
      assertEquals(attributeKey1, attributes.get(i).getKey());
      assertEquals("privacy", attributes.get(i++).getStringValue());
      assertEquals("userId", attributes.get(i).getKey());
      assertEquals("123", attributes.get(i++).getStringValue());
      assertEquals("userId" + DbKeys.COMPOSITE_KEY_DELIM + attributeKey0
          + DbKeys.COMPOSITE_KEY_DELIM + attributeKey1, attributes.get(i).getKey());
      assertEquals(
          "123" + DbKeys.COMPOSITE_KEY_DELIM + "person" + DbKeys.COMPOSITE_KEY_DELIM + "privacy",
          attributes.get(i).getStringValue());
    }
  }

  /**
   * Update Document actions.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testUpdate08() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().deepLinkPath("https://www.google.com");

      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().deepLinkPath("https://www.google.com/2")
              .addActionsItem(new AddAction().type(DocumentActionType.PDFEXPORT));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      Map<String, Object> message = getSqsMessages("actions", documentId);
      assertEquals(documentId, message.get("documentId"));
      assertEquals("actions", message.get("type"));
    }
  }

  /**
   * Update Document deeplink and content.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(TEST_TIMEOUT)
  public void testUpdate09() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().deepLinkPath("https://www.google.com");

      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().deepLinkPath("https://www.google.com/2").content("test");

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"error\":\"both 'content', and 'deepLinkPath' cannot be set\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Save new File with valid SHA-256 and then update content.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUpdate10() throws ApiException {
    // given
    final String content0 = "dummy data";
    final String checksum0 = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (String checksum : Arrays.asList(null, checksum0)) {
        setBearerToken(siteId);

        AddDocumentRequest req = new AddDocumentRequest().content(content0)
            .contentType("text/plain").checksum(checksum).checksumType(ChecksumType.SHA256);

        // when
        AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

        // then
        String documentId = response.getDocumentId();
        assertNotNull(documentId);
        assertEquals(siteId, response.getSiteId());

        GetDocumentResponse doc =
            this.documentsApi.getDocument(response.getDocumentId(), siteId, null);
        assertEquals("text/plain", doc.getContentType());
        assertEquals(ChecksumType.SHA256, doc.getChecksumType());
        assertEquals(checksum0, doc.getChecksum());
        assertNotNull(doc.getPath());
        assertNotNull(doc.getDocumentId());
        assertEquals(content0,
            this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

        // given
        String content1 = "new content";
        String checksum1 = "fe32608c9ef5b6cf7e3f946480253ff76f24f4ec0678f3d0f07f9844cbff9601";
        UpdateDocumentRequest updateReq = new UpdateDocumentRequest().content(content1)
            .contentType("text/plain").checksum(checksum1).checksumType(ChecksumType.SHA256);

        // when
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

        // then
        doc = this.documentsApi.getDocument(documentId, siteId, null);
        assertEquals("text/plain", doc.getContentType());
        assertEquals(ChecksumType.SHA256, doc.getChecksumType());
        assertEquals(checksum1, doc.getChecksum());
        assertEquals(content1,
            this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());
      }
    }
  }

  /**
   * Save new File with valid SHA-256 and then update content using presigned url.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUpdate11() throws ApiException, IOException {
    // given
    final String content0 = "dummy data";
    final String checksum0 = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (String checksum : Arrays.asList(null, checksum0)) {
        setBearerToken(siteId);

        AddDocumentRequest req = new AddDocumentRequest().content(content0)
            .contentType("text/plain").checksum(checksum).checksumType(ChecksumType.SHA256);

        // when
        AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

        // then
        String documentId = response.getDocumentId();
        assertNotNull(documentId);
        assertEquals(siteId, response.getSiteId());

        GetDocumentResponse doc =
            this.documentsApi.getDocument(response.getDocumentId(), siteId, null);
        assertEquals("text/plain", doc.getContentType());
        assertEquals(ChecksumType.SHA256, doc.getChecksumType());
        assertEquals(checksum0, doc.getChecksum());
        assertNotNull(doc.getPath());
        assertNotNull(doc.getDocumentId());
        assertEquals(content0,
            this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

        // given
        String checksum1 = "fe32608c9ef5b6cf7e3f946480253ff76f24f4ec0678f3d0f07f9844cbff9601";
        UpdateDocumentRequest updateReq =
            new UpdateDocumentRequest().checksum(checksum1).checksumType(ChecksumType.SHA256);

        // when
        response = this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

        // then
        assertNotNull(response);
        assertNotNull(response.getHeaders());
        assertEquals(2, response.getHeaders().size());

        // given
        String content1 = "new content";

        // when
        putS3Request(response.getUploadUrl(), response.getHeaders(), content1);

        // then
        doc = this.documentsApi.getDocument(documentId, siteId, null);
        assertEquals("text/plain", doc.getContentType());
        assertEquals(ChecksumType.SHA256, doc.getChecksumType());
        assertEquals(checksum1, doc.getChecksum());
        assertEquals(content1,
            this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());
      }
    }
  }

  /**
   * Update path to existing path.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate12() throws Exception {
    // given
    String content = "test data";
    String dir = "somepath";
    String path = dir + "/" + ID.uuid() + ".txt";
    DocumentSearchRequest sreq = new DocumentSearchRequest()
        .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(dir)));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req =
          new AddDocumentRequest().path(path).contentType("text/plain").content(content);

      // when
      String doc0 = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      String doc1 = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // then
      assertNotEquals(doc0, doc1);
      assertEquals(path, this.documentsApi.getDocument(doc0, siteId, null).getPath());
      assertNotEquals(path, this.documentsApi.getDocument(doc1, siteId, null).getPath());

      List<SearchResultDocument> docs = search(siteId, sreq);
      assertEquals(2, docs.size());

      // given
      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path(path);

      // when
      this.documentsApi.updateDocument(doc1, updateReq, siteId, null);

      // then
      assertNotEquals(path, this.documentsApi.getDocument(doc1, siteId, null).getPath());

      docs = search(siteId, sreq);
      assertEquals(2, docs.size());
    }
  }

  /**
   * Update path to existing path different case.
   *
   * @throws Exception Exception
   */
  @Test
  public void testUpdate13() throws Exception {
    // given
    String content = "test data";
    String dir = "somepath";
    String id = ID.uuid();
    String path0 = dir + "/random" + id + ".txt";
    String path1 = dir + "/Random" + id + ".txt";
    DocumentSearchRequest sreq = new DocumentSearchRequest()
        .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(dir)));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req =
          new AddDocumentRequest().path(path0).contentType("text/plain").content(content);

      // when
      String doc0 = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // then
      assertEquals(path0, this.documentsApi.getDocument(doc0, siteId, null).getPath());

      List<SearchResultDocument> docs = search(siteId, sreq);
      assertEquals(1, docs.size());
      assertEquals(path0, docs.get(0).getPath());

      // given
      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path(path1);

      // when
      this.documentsApi.updateDocument(doc0, updateReq, siteId, null);

      // then
      assertEquals(path1, this.documentsApi.getDocument(doc0, siteId, null).getPath());

      docs = search(siteId, sreq);
      assertEquals(1, docs.size());
      assertEquals(path1, docs.get(0).getPath());
    }
  }

  private List<SearchResultDocument> search(final String siteId, final DocumentSearchRequest sreq)
      throws ApiException {
    return notNull(searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());
  }

  private void putS3Request(final String presignedUrl, final Map<String, Object> headerMap,
      final String content) throws IOException {

    HttpService http = new HttpServiceJdk11();

    HttpHeaders hds = new HttpHeaders();
    headerMap.forEach((h, v) -> hds.add(h, v.toString()));

    http.put(presignedUrl, Optional.of(hds), Optional.empty(), content);
  }
}
