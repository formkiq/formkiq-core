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

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeClassification;
import com.formkiq.client.model.AddDocumentRequest;
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
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetClassificationRequest;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      final int limit = 4;
      final int count = 5;
      setBearerToken(siteId);

      addAttribute(siteId, "invoice");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);

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
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(key).dataType(null));
    this.attributesApi.addAttribute(req, siteId);
  }

  private static SchemaAttributes createSchemaAttributes(final List<String> requiredKeys,
      final List<String> optionalKeys) {
    List<AttributeSchemaRequired> required = notNull(requiredKeys).stream()
        .map(k -> new AttributeSchemaRequired().attributeKey(k)).toList();
    List<AttributeSchemaOptional> optional = notNull(optionalKeys).stream()
        .map(k -> new AttributeSchemaOptional().attributeKey(k)).toList();
    return new SchemaAttributes().required(required).optional(optional);
  }

  /**
   * POST /sites/{siteId}/classifications. Invalid Attribute.
   *
   */
  @Test
  public void testAddClassifications01() {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoice");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoice"), null);
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
        this.schemasApi.setClassification(siteId, UUID.randomUUID().toString(), sreq);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
   * DELETE /sites/{siteId}/classifications.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteClassifications01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      assertNotNull(
          this.schemasApi.getClassification(siteId, classificationId).getClassification());

      // when
      DeleteResponse deleteResponse =
          this.schemasApi.deleteClassification(siteId, classificationId);

      // then
      assertEquals("Classification '" + classificationId + "' deleted",
          deleteResponse.getMessage());

      try {
        this.schemasApi.getClassification(siteId, classificationId);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      // when
      Classification classification =
          this.schemasApi.getClassification(siteId, classificationId).getClassification();

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
          this.schemasApi.getClassification(siteId, classificationId).getClassification();

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("documentType"), null);

      String classificationId = addClassification(siteId, "doc", attr0);

      // when
      Classification classification =
          this.schemasApi.getClassification(siteId, classificationId).getClassification();

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
          this.schemasApi.getClassification(siteId, classificationId).getClassification();

      assertNotNull(classification);
      assertEquals("doc123", classification.getName());

      // given
      // when
      classificationId = addClassification(siteId, "d", attr0);

      // then
      classification =
          this.schemasApi.getClassification(siteId, classificationId).getClassification();

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr = createSchemaAttributes(List.of("other"), null);
      SetSitesSchemaRequest setSiteSchema =
          new SetSitesSchemaRequest().name("test").attributes(attr);
      this.schemasApi.setSitesSchema(siteId, setSiteSchema);

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);

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

  /**
   * Test with classification with composite keys.
   */
  @Test
  void testAddDocument05() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes schemaAttributes =
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
      assertEquals("Classification", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber#other", documentAttributes.get(i++).getKey());
      assertEquals("other", documentAttributes.get(i).getKey());

      DocumentSearchAttribute item0 = new DocumentSearchAttribute().key("other").eq("stuff");
      DocumentSearchAttribute item1 =
          new DocumentSearchAttribute().key("invoiceNumber").eq("INV-001");
      List<SearchResultDocument> docs = search(siteId, item0, item1);
      assertEquals(1, docs.size());
    }
  }

  /**
   * Test with classification with changed composite keys.
   */
  @Test
  void testAddDocument06() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "type");
      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes schemaAttributes0 =
          createSchemaAttributes(List.of("invoiceNumber", "other"), null);
      schemaAttributes0.addCompositeKeysItem(
          new AttributeSchemaCompositeKey().attributeKeys(List.of("invoiceNumber", "other")));
      String classificationId = addClassification(siteId, "doc", schemaAttributes0);

      SchemaAttributes schemaAttributes1 =
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
      assertEquals("Classification", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber", documentAttributes.get(i++).getKey());
      assertEquals("invoiceNumber#type", documentAttributes.get(i++).getKey());
      assertEquals("other", documentAttributes.get(i++).getKey());
      assertEquals("type", documentAttributes.get(i).getKey());

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "other");
      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr = createSchemaAttributes(List.of("other"), null);
      SetSitesSchemaRequest setSiteSchema =
          new SetSitesSchemaRequest().name("test").attributes(attr);
      this.schemasApi.setSitesSchema(siteId, setSiteSchema);

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoiceNumber");

      SchemaAttributes attr0 = createSchemaAttributes(List.of("invoiceNumber"), null);
      Objects.requireNonNull(attr0.getRequired()).get(0).addAllowedValuesItem("123");

      SetSitesSchemaRequest setSiteSchema =
          new SetSitesSchemaRequest().name("test").attributes(attr0);
      this.schemasApi.setSitesSchema(siteId, setSiteSchema);

      SchemaAttributes attr1 = createSchemaAttributes(List.of("invoiceNumber"), null);
      Objects.requireNonNull(attr1.getRequired()).get(0).addAllowedValuesItem("INV-001");
      String classificationId = addClassification(siteId, "doc", attr1);

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

  private @Nullable String addDocument(final String siteId,
      final List<AddDocumentAttribute> attributes) throws ApiException {
    AddDocumentRequest areq = new AddDocumentRequest().content("adasd").attributes(attributes);
    return this.documentsApi.addDocument(areq, siteId, null).getDocumentId();
  }

  private String addClassification(final String siteId, final String name,
      final SchemaAttributes attr0) throws ApiException {
    AddClassificationRequest req = new AddClassificationRequest()
        .classification(new AddClassification().name(name).attributes(attr0));
    return this.schemasApi.addClassification(siteId, req).getClassificationId();
  }

  private void setClassification(final String siteId, final String classificationId,
      final SchemaAttributes attr0) throws ApiException {
    SetClassificationRequest req = new SetClassificationRequest()
        .classification(new AddClassification().name("setDoc").attributes(attr0));
    this.schemasApi.setClassification(siteId, classificationId, req);
  }
}
