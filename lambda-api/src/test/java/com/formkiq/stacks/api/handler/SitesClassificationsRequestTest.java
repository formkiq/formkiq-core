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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeClassification;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.Classification;
import com.formkiq.client.model.ClassificationSummary;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.GetClassificationsResponse;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetClassificationRequest;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumberAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static com.formkiq.testutils.aws.FkqSchemaService.createSchemaAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /sites/{siteId}/schema/document. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class SitesClassificationsRequestTest extends AbstractApiClientRequestTest {

  /**
   * GET /sites/{siteId}/classifications.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testGetClassifications01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      final int limit = 4;
      final int count = 5;
      setBearerToken(siteId);

      addAttribute(siteId, "invoice");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);

      List<String> ids = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        AddClassificationRequest req = new AddClassificationRequest()
            .classification(new AddClassification().name("test_" + i).attributes(attr0));
        String classificationId =
            this.schemasApi.addClassification(siteId, req).getClassificationId();
        ids.add(classificationId);
      }

      // when
      GetClassificationsResponse response =
          this.schemasApi.getSitesClassifications(siteId, "" + limit, null);
      List<ClassificationSummary> attributes = notNull(response.getClassifications());

      // then
      int i = 0;
      assertEquals(limit, attributes.size());
      assertTrue(ids.contains(attributes.get(i).getClassificationId()));
      assertEquals("joesmith", attributes.get(i).getUserId());
      assertNotNull(attributes.get(i).getInsertedDate());
      assertEquals("test_0", attributes.get(i++).getName());
      assertEquals("test_1", attributes.get(i++).getName());
      assertEquals("test_2", attributes.get(i++).getName());
      assertEquals("test_3", attributes.get(i).getName());

      attributes = notNull(this.schemasApi
          .getSitesClassifications(siteId, "" + limit, response.getNext()).getClassifications());
      assertEquals(1, attributes.size());
      assertEquals("test_4", attributes.get(0).getName());
    }
  }

  private void addAttribute(final String siteId, final String key) throws ApiException {
    addAttribute(siteId, key, null);
  }

  private void addAttribute(final String siteId, final String key, final AttributeDataType dataType)
      throws ApiException {
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(key).dataType(dataType));
    this.attributesApi.addAttribute(req, siteId);
  }

  /**
   * POST /sites/{siteId}/classifications. Invalid Attribute.
   *
   */
  @Test
  public void testAddClassifications01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr0));

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"invoice\"," + "\"error\":\"attribute 'invoice' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /sites/{siteId}/classifications. Duplicate Name.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddClassifications02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoice");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr0));

      this.schemasApi.addClassification(siteId, req);

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is already used\"}]}",
            e.getResponseBody());
      }

      // given
      SetClassificationRequest sreq = new SetClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr0));

      // when
      try {
        this.schemasApi.setClassification(siteId, ID.uuid(), sreq);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is already used\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /sites/{siteId}/classifications. Invalid Body.
   *
   */
  @Test
  public void testAddClassifications03() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddClassificationRequest req = new AddClassificationRequest();

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"invalid request body\"}", e.getResponseBody());
      }

      // given
      req = new AddClassificationRequest().classification(new AddClassification());

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"'name' is required\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /sites/{siteId}/classifications with override siteschema required attribute with optional.
   *
   */
  @Test
  public void testAddClassifications04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);
      setSiteSchema(siteId, attr0);

      SetSchemaAttributes attr1 = createSchemaAttributes(null, List.of("documentType"));
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr1));

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"documentType\","
                + "\"error\":\"attribute cannot override site schema attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /sites/{siteId}/classifications with override siteschema optional attribute with required.
   *
   */
  @Test
  public void testAddClassifications05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr0 = createSchemaAttributes(null, List.of("documentType"));
      setSiteSchema(siteId, attr0);

      SetSchemaAttributes attr1 = createSchemaAttributes(List.of("documentType"), null);
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr1));

      // when
      String classificationId =
          this.schemasApi.addClassification(siteId, req).getClassificationId();

      // then
      assertNotNull(this.schemasApi.getClassification(siteId, classificationId, null));
    }
  }

  /**
   * POST /sites/{siteId}/classifications invalid defaultValue.
   *
   */
  @Test
  public void testAddClassifications06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr1 = createSchemaAttributes(List.of("documentType"), null);
      List<AddAttributeSchemaRequired> required = notNull(attr1.getRequired());
      required.get(0).setDefaultValue("123");
      required.get(0).setAllowedValues(List.of("1", "2"));
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr1));

      // when
      try {
        this.schemasApi.addClassification(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"defaultValue\","
                + "\"error\":\"defaultValue must be part of allowed values\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /sites/{siteId}/classifications.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteClassifications01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      assertNotNull(
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification());

      // when
      DeleteResponse deleteResponse =
          this.schemasApi.deleteClassification(siteId, classificationId);

      // then
      assertEquals("Classification '" + classificationId + "' deleted",
          deleteResponse.getMessage());

      try {
        this.schemasApi.getClassification(siteId, classificationId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Classification '" + classificationId + "' not found\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /sites/{siteId}/classifications. In Use.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteClassifications02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttribute attribute = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      addDocument(siteId, List.of(new AddDocumentAttribute(classification), attribute));

      // when
      try {
        this.schemasApi.deleteClassification(siteId, classificationId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Classification '" + classificationId + "' in use\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/classifications/{classificationId}.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutClassifications01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      // when
      Classification classification =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      // then
      assertNotNull(classification);
      assertNotNull(classification.getAttributes());

      assertEquals("doc", classification.getName());
      List<AttributeSchemaRequired> required =
          notNull(classification.getAttributes().getRequired());
      assertEquals(1, required.size());

      List<AttributeSchemaOptional> optional =
          notNull(classification.getAttributes().getOptional());
      assertEquals(0, optional.size());

      // given
      attr0 = createSchemaAttributes(null, List.of("documentType"));
      SetClassificationRequest setReq = new SetClassificationRequest()
          .classification(new AddClassification().name("doc").attributes(attr0));

      // when
      this.schemasApi.setClassification(siteId, classificationId, setReq);

      // then
      classification =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      assertNotNull(classification);
      assertNotNull(classification.getAttributes());

      assertEquals("doc", classification.getName());
      required = notNull(classification.getAttributes().getRequired());
      assertEquals(0, required.size());

      optional = notNull(classification.getAttributes().getOptional());
      assertEquals(1, optional.size());
    }
  }

  /**
   * PUT /sites/{siteId}/classifications/{classificationId} for similar names.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutClassifications02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      // when
      Classification classification =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      // then
      assertNotNull(classification);
      assertEquals("doc", classification.getName());

      // given
      SetClassificationRequest setReq = new SetClassificationRequest()
          .classification(new AddClassification().name("doc123").attributes(attr0));

      // when
      this.schemasApi.setClassification(siteId, classificationId, setReq);

      // then
      classification =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      assertNotNull(classification);
      assertEquals("doc123", classification.getName());

      // given
      // when
      classificationId = addClassification(siteId, "d", attr0);

      // then
      classification =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      assertNotNull(classification);
      assertEquals("d", classification.getName());
    }
  }

  /**
   * Add document with classification.
   */
  @Test
  void testAddDocument01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttribute attribute = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      String documentId =
          addDocument(siteId, List.of(new AddDocumentAttribute(classification), attribute));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(2, documentAttributes.size());

      assertEquals("Classification", documentAttributes.get(0).getKey());
      assertEquals(classificationId, documentAttributes.get(0).getStringValue());
      assertEquals(AttributeValueType.CLASSIFICATION, documentAttributes.get(0).getValueType());
      assertEquals("invoiceNumber", documentAttributes.get(1).getKey());
      assertEquals("INV-001", documentAttributes.get(1).getStringValue());
      assertEquals(AttributeValueType.STRING, documentAttributes.get(1).getValueType());

      DocumentSearchAttribute item0 =
          new DocumentSearchAttribute().key("Classification").eq(classificationId);
      List<SearchResultDocument> documents = search(siteId, item0);
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).getDocumentId());
    }
  }

  private @NotNull List<SearchResultDocument> search(final String siteId,
      final DocumentSearchAttribute... items) throws ApiException {
    DocumentSearch ds = new DocumentSearch();
    for (DocumentSearchAttribute item : items) {
      ds.addAttributesItem(item);
    }

    DocumentSearchRequest req = new DocumentSearchRequest().query(ds);
    return notNull(this.searchApi.documentSearch(req, siteId, null, null, null).getDocuments());
  }

  /**
   * Add document with classification, missing required attribute.
   */
  @Test
  void testAddDocument02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      try {
        addDocument(siteId, List.of(new AddDocumentAttribute(classification)));
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"invoiceNumber\","
                + "\"error\":\"missing required attribute 'invoiceNumber'\"}]}",
            e.getResponseBody());
      }
    }
  }

  // test with classification with default value
  /**
   * Add document with classification, missing required attribute has default value.
   */
  @Test
  void testAddDocument03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
      assertNotNull(attr0.getRequired());
      attr0.getRequired().get(0).setDefaultValue("12345");

      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      String documentId = addDocument(siteId, List.of(new AddDocumentAttribute(classification)));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(2, documentAttributes.size());

      assertEquals("Classification", documentAttributes.get(0).getKey());
      assertEquals(classificationId, documentAttributes.get(0).getStringValue());
      assertEquals(AttributeValueType.CLASSIFICATION, documentAttributes.get(0).getValueType());

      assertEquals("invoiceNumber", documentAttributes.get(1).getKey());
      assertEquals("12345", documentAttributes.get(1).getStringValue());
      assertEquals(AttributeValueType.STRING, documentAttributes.get(1).getValueType());
    }
  }

  /**
   * Test with classification and site schema with required attributes.
   */
  @Test
  void testAddDocument04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr = createSchemaAttributes(List.of("other"), null);
      setSiteSchema(siteId, attr);

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttribute attribute0 = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      try {
        addDocument(siteId, List.of(new AddDocumentAttribute(classification), attribute0));
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"other\",\"error\":\"missing required attribute 'other'\"}]}",
            e.getResponseBody());
      }

      // given
      AddDocumentAttribute attribute1 = createStringAttribute("other", "thing");

      // when
      String documentId = addDocument(siteId,
          List.of(new AddDocumentAttribute(classification), attribute0, attribute1));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 3;
      assertEquals(expected, documentAttributes.size());

      assertEquals("Classification", documentAttributes.get(0).getKey());
      assertEquals(classificationId, documentAttributes.get(0).getStringValue());
      assertEquals(AttributeValueType.CLASSIFICATION, documentAttributes.get(0).getValueType());

      assertEquals("invoiceNumber", documentAttributes.get(1).getKey());
      assertEquals("INV-001", documentAttributes.get(1).getStringValue());
      assertEquals(AttributeValueType.STRING, documentAttributes.get(1).getValueType());

      assertEquals("other", documentAttributes.get(2).getKey());
      assertEquals("thing", documentAttributes.get(2).getStringValue());
      assertEquals(AttributeValueType.STRING, documentAttributes.get(2).getValueType());
    }
  }

  private void setSiteSchema(final String siteId, final SetSchemaAttributes attr)
      throws ApiException {
    SetSitesSchemaRequest setSiteSchema = new SetSitesSchemaRequest().name("test").attributes(attr);
    this.schemasApi.setSitesSchema(siteId, setSiteSchema);
  }

  /**
   * Test with classification with composite keys.
   */
  @Test
  void testAddDocument05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes schemaAttributes =
          createSchemaAttributes(List.of("invoiceNumber", "other"), null);
      schemaAttributes.addCompositeKeysItem(
          new AttributeSchemaCompositeKey().attributeKeys(List.of("invoiceNumber", "other")));
      String classificationId = addClassification(siteId, "doc", schemaAttributes);

      AddDocumentAttribute attribute0 = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttribute attribute1 = createStringAttribute("other", "stuff");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      String documentId = addDocument(siteId,
          List.of(new AddDocumentAttribute(classification), attribute0, attribute1));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 4;
      assertEquals(expected, documentAttributes.size());

      int i = 0;
      assertDocumentAttributes(documentAttributes.get(i++), "Classification", classificationId);
      assertDocumentAttributes(documentAttributes.get(i++), "invoiceNumber", "INV-001");
      assertDocumentAttributes(documentAttributes.get(i++), "invoiceNumber::other",
          "INV-001::stuff");
      assertDocumentAttributes(documentAttributes.get(i), "other", "stuff");

      DocumentSearchAttribute item0 = new DocumentSearchAttribute().key("other").eq("stuff");
      DocumentSearchAttribute item1 =
          new DocumentSearchAttribute().key("invoiceNumber").eq("INV-001");
      List<SearchResultDocument> docs = search(siteId, item0, item1);
      assertEquals(1, docs.size());
    }
  }

  private void assertDocumentAttributes(final DocumentAttribute da,
      final String expectedAttributeKey, final String expectedStringValue) {
    assertEquals(expectedAttributeKey, da.getKey());
    assertEquals(expectedStringValue, da.getStringValue());
    assertNotNull(da.getInsertedDate());
    assertNotNull(da.getUserId());
  }

  /**
   * Test with classification with changed composite keys.
   */
  @Test
  void testAddDocument06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "type");
      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes schemaAttributes0 =
          createSchemaAttributes(List.of("invoiceNumber", "other"), null);
      schemaAttributes0.addCompositeKeysItem(
          new AttributeSchemaCompositeKey().attributeKeys(List.of("invoiceNumber", "other")));
      String classificationId = addClassification(siteId, "doc", schemaAttributes0);

      SetSchemaAttributes schemaAttributes1 =
          createSchemaAttributes(List.of("invoiceNumber", "other"), null);
      schemaAttributes1.addCompositeKeysItem(
          new AttributeSchemaCompositeKey().attributeKeys(List.of("invoiceNumber", "type")));
      setClassification(siteId, classificationId, schemaAttributes1);

      AddDocumentAttribute attribute0 = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttribute attribute1 = createStringAttribute("other", "stuff");
      AddDocumentAttribute attribute2 = createStringAttribute("type", "important");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      String documentId = addDocument(siteId,
          List.of(new AddDocumentAttribute(classification), attribute0, attribute1, attribute2));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 5;
      assertEquals(expected, documentAttributes.size());

      int i = 0;
      assertDocumentAttributes(documentAttributes.get(i++), "Classification", classificationId);
      assertDocumentAttributes(documentAttributes.get(i++), "invoiceNumber", "INV-001");
      assertDocumentAttributes(documentAttributes.get(i++), "invoiceNumber::type",
          "INV-001::important");
      assertDocumentAttributes(documentAttributes.get(i++), "other", "stuff");
      assertDocumentAttributes(documentAttributes.get(i), "type", "important");

      DocumentSearchAttribute item0 = new DocumentSearchAttribute().key("type").eq("important");
      DocumentSearchAttribute item1 =
          new DocumentSearchAttribute().key("invoiceNumber").eq("INV-001");
      List<SearchResultDocument> docs = search(siteId, item0, item1);
      assertEquals(1, docs.size());
    }
  }

  /**
   * Add document with classification but site schema has required attribute.
   */
  @Test
  void testAddDocument07() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr = createSchemaAttributes(List.of("other"), null);
      setSiteSchema(siteId, attr);

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
      String classificationId = addClassification(siteId, "doc", attr0);

      AddDocumentAttribute attribute = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      try {
        addDocument(siteId, List.of(new AddDocumentAttribute(classification), attribute));
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"other\","
            + "\"error\":\"missing required attribute 'other'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Add document with classification, same attributes different allowed values.
   */
  @Test
  void testAddDocument08() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
      notNull(attr0.getRequired()).get(0).addAllowedValuesItem("123");

      setSiteSchema(siteId, attr0);

      SetSchemaAttributes attr1 = createSchemaAttributes(List.of("invoiceNumber"), null);
      notNull(attr1.getRequired()).get(0).addAllowedValuesItem("INV-001");
      String classificationId = addClassification(siteId, attr1);

      AddDocumentAttribute attribute = createStringAttribute("invoiceNumber", "INV-001");
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      String documentId =
          addDocument(siteId, List.of(new AddDocumentAttribute(classification), attribute));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 2;
      assertEquals(expected, documentAttributes.size());

      int i = 0;
      assertEquals("Classification", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber", documentAttributes.get(i).getKey());
    }
  }

  /**
   * Add document classification after the document is already created.
   */
  @Test
  void testAddDocument09() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // given
      addAttribute(siteId, "reviewByDate");

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of("reviewByDate"), null);
      String classificationId = addClassification(siteId, attr0);

      AddDocumentAttribute attribute = createStringAttribute("reviewByDate", "2025-09-30");

      // when
      String documentId = addDocument(siteId, List.of(attribute));

      // then
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, documentAttributes.size());
      assertEquals("reviewByDate", documentAttributes.get(0).getKey());

      // given
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      addDocumentClassification(siteId, documentId, classification);

      // then
      documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 2;
      assertEquals(expected, documentAttributes.size());
      assertEquals("Classification", documentAttributes.get(0).getKey());
      assertEquals("reviewByDate", documentAttributes.get(1).getKey());
    }
  }

  /**
   * Add document classification after the document is already created.
   */
  @Test
  void testAddDocument10() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // given
      addAttribute(siteId, "invoiceCurrency");
      addAttribute(siteId, "invoiceDate");
      addAttribute(siteId, "invoiceNumber");
      addAttribute(siteId, "invoiceVendorName");
      addAttribute(siteId, "invoice", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "invoiceTotalAmount", AttributeDataType.NUMBER);

      SetSchemaAttributes attr0 = createSchemaAttributes(
          List.of("invoiceDate", "invoiceNumber", "invoiceTotalAmount", "invoiceVendorName"),
          List.of("invoiceCurrency"));
      String classificationId = addClassification(siteId, attr0);

      String documentId = addDocument(siteId, null);

      AddDocumentAttribute a0 =
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key("invoice"));
      AddDocumentAttribute a1 = createStringAttribute("invoiceCurrency", "USD");
      AddDocumentAttribute a2 = createStringAttribute("invoiceDate", "2023-05-01");
      AddDocumentAttribute a3 = createStringAttribute("invoiceNumber", "45102");
      AddDocumentAttribute a4 = createNumberAttribute("invoiceTotalAmount", new BigDecimal(1));
      AddDocumentAttribute a5 =
          createStringAttribute("invoiceVendorName", "Mascareene Beef Company");
      addDocumentAttributes(siteId, documentId, List.of(a0, a1, a2, a3, a4, a5));

      // then
      final int expected = 6;
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(expected, documentAttributes.size());

      // given
      AddDocumentAttributeClassification classification =
          new AddDocumentAttributeClassification().classificationId(classificationId);

      // when
      addDocumentClassification(siteId, documentId, classification);

      // then
      documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      int i = 0;
      assertEquals(expected + 1, documentAttributes.size());
      assertEquals("Classification", documentAttributes.get(i++).getKey());
      assertEquals("invoice", documentAttributes.get(i++).getKey());
      assertEquals("invoiceCurrency", documentAttributes.get(i++).getKey());
      assertEquals("invoiceDate", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber", documentAttributes.get(i++).getKey());
      assertEquals("invoiceTotalAmount", documentAttributes.get(i++).getKey());
      assertEquals("invoiceVendorName", documentAttributes.get(i).getKey());
    }
  }

  /**
   * Set Document Classification, missing required attributes.
   */
  @Test
  void testSetDocumentClassification01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      // given
      setBearerToken(siteId);

      String documentId = addDocument(siteId, null);
      addAttribute(siteId, "TestBoolean", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "TestBoolean2", AttributeDataType.BOOLEAN);

      SetSchemaAttributes sattr0 = createSchemaAttributes(List.of("TestBoolean"), null);
      String classificationId0 = addClassification(siteId, sattr0);

      AddDocumentAttributeClassification attr0 =
          new AddDocumentAttributeClassification().classificationId(classificationId0);
      SetDocumentAttributesRequest setReq0 =
          new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(attr0));

      AddDocumentAttributeStandard attr1 = new AddDocumentAttributeStandard()
          .key(AttributeKeyReserved.CLASSIFICATION.getKey()).stringValue(classificationId0);
      SetDocumentAttributesRequest setReq1 =
          new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(attr1));

      for (SetDocumentAttributesRequest setReq : List.of(setReq0, setReq1)) {
        // when
        try {
          this.documentAttributesApi.setDocumentAttributes(documentId, setReq, siteId);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
          assertEquals(
              "{\"errors\":[{\"key\":\"TestBoolean\","
                  + "\"error\":\"missing required attribute 'TestBoolean'\"}]}",
              e.getResponseBody());
        }
      }

      // given
      SetSchemaAttributes sattr1 = createSchemaAttributes(List.of("TestBoolean2"), null);
      String classificationId1 = addClassification(siteId, "doc1", sattr1);
      SetDocumentAttributeRequest setReqValue =
          new SetDocumentAttributeRequest().attribute(new AddDocumentAttributeValue()
              .stringValues(List.of(classificationId0, classificationId1)));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributeValue(documentId,
            AttributeKeyReserved.CLASSIFICATION.getKey(), setReqValue, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"TestBoolean\","
                + "\"error\":\"missing required attribute 'TestBoolean'\"},"
                + "{\"key\":\"TestBoolean2\","
                + "\"error\":\"missing required attribute 'TestBoolean2'\"}]}",
            e.getResponseBody());
      }
    }
  }

  private void addDocumentAttributes(final String siteId, final String documentId,
      final List<AddDocumentAttribute> attributes) throws ApiException {
    AddDocumentAttributesRequest req = new AddDocumentAttributesRequest().attributes(attributes);
    this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
  }

  private void addDocumentClassification(final String siteId, final String documentId,
      final AddDocumentAttributeClassification classification) throws ApiException {
    this.documentAttributesApi.addDocumentAttributes(documentId, new AddDocumentAttributesRequest()
        .addAttributesItem(new AddDocumentAttribute(classification)), siteId, null);
  }

  private @Nullable String addDocument(final String siteId,
      final List<AddDocumentAttribute> attributes) throws ApiException {
    AddDocumentRequest areq = new AddDocumentRequest().content("adasd").attributes(attributes);
    return this.documentsApi.addDocument(areq, siteId, null).getDocumentId();
  }

  private String addClassification(final String siteId, final SetSchemaAttributes attr0)
      throws ApiException {
    return addClassification(siteId, "doc", attr0);
  }

  private String addClassification(final String siteId, final String name,
      final SetSchemaAttributes attr0) throws ApiException {
    AddClassificationRequest req = new AddClassificationRequest()
        .classification(new AddClassification().name(name).attributes(attr0));
    return this.schemasApi.addClassification(siteId, req).getClassificationId();
  }

  private void setClassification(final String siteId, final String classificationId,
      final SetSchemaAttributes attr0) throws ApiException {
    SetClassificationRequest req = new SetClassificationRequest()
        .classification(new AddClassification().name("setDoc").attributes(attr0));
    this.schemasApi.setClassification(siteId, classificationId, req);
  }

  /**
   * Add Site Schema with allowed values.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testAllowedValues01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      String attributeKey = "invoiceNumber";

      addAttribute(siteId, attributeKey);

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of(attributeKey), null);
      notNull(attr0.getRequired()).get(0).setAllowedValues(List.of("123", "A", "B"));

      this.schemasApi.setSitesSchema(siteId,
          new SetSitesSchemaRequest().name("test").attributes(attr0));

      // when
      List<String> allowedValues = notNull(
          this.attributesApi.getAttributeAllowedValues(attributeKey, siteId).getAllowedValues());

      // then
      final int expected = 3;
      assertEquals(expected, allowedValues.size());
      assertEquals("123,A,B", String.join(",", allowedValues));
    }
  }

  /**
   * Missing attribute.
   *
   */
  @Test
  void testAllowedValues02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      try {
        this.attributesApi.getAttributeAllowedValues("test", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Attribute test not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Add Classification with allowed values.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testAllowedValues03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      String attributeKey = "invoiceNumber";

      addAttribute(siteId, attributeKey);

      SetSchemaAttributes attr0 = createSchemaAttributes(List.of(attributeKey), null);
      notNull(attr0.getRequired()).get(0).setAllowedValues(List.of("123", "A", "B"));
      this.schemasApi.setSitesSchema(siteId,
          new SetSitesSchemaRequest().name("test").attributes(attr0));

      SetSchemaAttributes attr1 = createSchemaAttributes(List.of(attributeKey), null);
      notNull(attr1.getRequired()).get(0).addAllowedValuesItem("INV-001");
      String classificationId = addClassification(siteId, attr1);

      SetSchemaAttributes attr2 = createSchemaAttributes(List.of(attributeKey), null);
      notNull(attr2.getRequired()).get(0).addAllowedValuesItem("OTHER");
      addClassification(siteId, "doc2", attr2);

      // when
      final List<String> allowedValues0 = notNull(
          this.attributesApi.getAttributeAllowedValues(attributeKey, siteId).getAllowedValues());

      final List<String> allowedValues1 = notNull(this.schemasApi
          .getSitesSchemaAttributeAllowedValues(siteId, attributeKey, null).getAllowedValues());

      final List<String> allowedValues2 = notNull(this.schemasApi
          .getClassificationAttributeAllowedValues(siteId, classificationId, attributeKey, null)
          .getAllowedValues());

      // then
      final int expected0 = 5;
      assertEquals(expected0, allowedValues0.size());
      assertEquals("123,A,B,INV-001,OTHER", String.join(",", allowedValues0));

      final int expected1 = 3;
      assertEquals(expected1, allowedValues1.size());
      assertEquals("123,A,B", String.join(",", allowedValues1));

      final int expected2 = 4;
      assertEquals(expected2, allowedValues2.size());
      assertEquals("123,A,B,INV-001", String.join(",", allowedValues2));
    }
  }

  /**
   * Delete Document attributes with composite keys.
   */
  @Test
  void testDeleteDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "test1");
      addAttribute(siteId, "test2");

      SetSchemaAttributes schemaAttributes =
          new SetSchemaAttributes().required(null).optional(null).addCompositeKeysItem(
              new AttributeSchemaCompositeKey().attributeKeys(List.of("test1", "test2")));
      schemasApi.setSitesSchema(siteId,
          new SetSitesSchemaRequest().name("test").attributes(schemaAttributes));

      AddDocumentRequest req = new AddDocumentRequest().deepLinkPath("https://www.google.com")
          .addAttributesItem(createAttribute("test1", "222"))
          .addAttributesItem(createAttribute("test2", "333"));

      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      List<DocumentAttribute> documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 3;
      assertEquals(expected, documentAttributes.size());
      int i = 0;
      assertDocumentAttributes(documentAttributes.get(i++), "test1", "222");
      assertDocumentAttributes(documentAttributes.get(i++), "test1::test2", "222::333");
      assertDocumentAttributes(documentAttributes.get(i), "test2", "333");

      // when
      DeleteResponse deleteResponse =
          this.documentAttributesApi.deleteDocumentAttribute(documentId, "test1", siteId);

      // then
      assertEquals("attribute 'test1' removed from document '" + documentId + "'",
          deleteResponse.getMessage());

      documentAttributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, documentAttributes.size());
      assertDocumentAttributes(documentAttributes.get(0), "test2", "333");
    }
  }

  private AddDocumentAttribute createAttribute(final String attributeKey,
      final String stringValue) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(attributeKey).stringValue(stringValue));
  }
}
