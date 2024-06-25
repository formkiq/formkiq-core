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
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsIdRequestTest extends AbstractApiClientRequestTest {

  /**
   * PUT /documents/{documentId}/restore request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSetDocumentRestore01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().path("test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.TRUE);

      // then
      List<Document> softDeletedDocuments = notNull(this.documentsApi
          .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, null).getDocuments());
      assertEquals(1, softDeletedDocuments.size());
      assertEquals("test.txt", softDeletedDocuments.get(0).getPath());

      List<Document> documents = notNull(this.documentsApi
          .getDocuments(siteId, null, null, null, null, null, null, null).getDocuments());
      assertEquals(0, documents.size());

      // when
      SetDocumentRestoreResponse restore = this.documentsApi.setDocumentRestore(documentId, siteId);

      // then
      assertEquals("document restored", restore.getMessage());
      softDeletedDocuments = notNull(this.documentsApi
          .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, null).getDocuments());
      assertEquals(0, softDeletedDocuments.size());
      documents = notNull(this.documentsApi
          .getDocuments(siteId, null, null, null, null, null, null, null).getDocuments());
      assertEquals(1, documents.size());
      assertEquals("test.txt", documents.get(0).getPath());
    }
  }

  /**
   * PUT /documents/{documentId}/restore request, document not found.
   *
   */
  @Test
  public void testHandleSetDocumentRestore02() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();

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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().deepLinkPath("test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);
      String documentId = response.getDocumentId();

      // when
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);

      // then
      assertEquals("test.txt", document.getDeepLinkPath());
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content0);

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();
      assertEquals(content0,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());

      // given
      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().content(content1);

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      assertEquals(content1,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String attributeKey = "test";
      this.attributesApi.addAttribute(
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

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

  /**
   * Update Document with invalid schema allowed value.
   */
  @Test
  public void testUpdate05() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String attributeKey = "test";

      this.attributesApi.addAttribute(
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

      SetSitesSchemaRequest sitesSchema = new SetSitesSchemaRequest().name("test")
          .attributes(new SchemaAttributes().addOptionalItem(new AttributeSchemaOptional()
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

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
      assertEquals(attributeKey0 + "#" + attributeKey1, attributes.get(0).getKey());
      assertEquals("person#privacy", attributes.get(0).getStringValue());
      assertEquals(attributeKey0, attributes.get(1).getKey());
      assertEquals("person", attributes.get(1).getStringValue());
      assertEquals(attributeKey1, attributes.get(2).getKey());
      assertEquals("privacy", attributes.get(2).getStringValue());
    }
  }

  private void createSiteSchema(final String siteId, final String attributeKey0,
      final String attributeKey1) throws ApiException {

    List<String> list = Arrays.asList("userId", "playerId", attributeKey0, attributeKey1);

    for (String attributeKey : list) {
      this.attributesApi.addAttribute(
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);
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
        new SetSitesSchemaRequest().name("test").attributes(new SchemaAttributes()
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

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
      assertEquals(attributeKey0 + "#" + attributeKey1, attributes.get(i).getKey());
      assertEquals("person#privacy", attributes.get(i++).getStringValue());
      assertEquals(attributeKey0, attributes.get(i).getKey());
      assertEquals("person", attributes.get(i++).getStringValue());
      assertEquals(attributeKey1, attributes.get(i).getKey());
      assertEquals("privacy", attributes.get(i++).getStringValue());
      assertEquals("userId", attributes.get(i).getKey());
      assertEquals("123", attributes.get(i++).getStringValue());
      assertEquals("userId#" + attributeKey0 + "#" + attributeKey1, attributes.get(i).getKey());
      assertEquals("123#person#privacy", attributes.get(i).getStringValue());
    }
  }
}
