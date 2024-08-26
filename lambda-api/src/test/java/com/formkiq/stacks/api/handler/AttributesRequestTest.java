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
import com.formkiq.client.model.AddAttributeResponse;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeRelationship;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentRelationshipType;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentAttributeResponse;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SearchResultDocumentAttribute;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumberAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumbersAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringsAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /attributes. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class AttributesRequestTest extends AbstractApiClientRequestTest {

  /** SiteId. */
  private static final String SITE_ID = UUID.randomUUID().toString();

  private static void assertAttributeValues(final DocumentAttribute attribute, final String key,
      final String stringValue, final String stringValues, final String numberValue,
      final String numberValues, final Boolean booleanValue) {

    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals("joesmith", attribute.getUserId());

    if (stringValue != null) {
      assertEquals(stringValue, attribute.getStringValue());
    } else {
      assertNull(attribute.getStringValue());
    }

    if (stringValues != null) {
      assertEquals(stringValues, String.join(",", notNull(attribute.getStringValues())));
    } else {
      assertTrue(notNull(attribute.getStringValues()).isEmpty());
    }

    if (numberValues != null) {
      assertEquals(numberValues, String.join(",", notNull(attribute.getNumberValues()).stream()
          .map(n -> formatDouble(n.doubleValue())).toList()));
    } else {
      assertTrue(notNull(attribute.getNumberValues()).isEmpty());
    }

    if (numberValue != null && attribute.getNumberValue() != null) {
      assertEquals(numberValue, formatDouble(attribute.getNumberValue().doubleValue()));
    } else {
      assertNull(attribute.getNumberValue());
    }

    if (booleanValue != null) {
      assertEquals(booleanValue, attribute.getBooleanValue());
    } else {
      assertNull(attribute.getBooleanValue());
    }

    assertNotNull(attribute.getInsertedDate());
  }

  /** Data Type Map. */
  private final Map<String, AttributeDataType> dataTypes =
      Map.of("other", AttributeDataType.NUMBER, "flag", AttributeDataType.BOOLEAN, "keyonly",
          AttributeDataType.KEY_ONLY, "nums", AttributeDataType.NUMBER);

  private void addAttribute(final String siteId, final String key, final AttributeDataType dataType,
      final AttributeType type) throws ApiException {
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key).dataType(dataType).type(type));
    this.attributesApi.addAttribute(req, siteId);
  }

  private String addDocument(final String siteId, final String key, final String stringValue,
      final BigDecimal numberValue) throws ApiException {

    AddDocumentRequest docReq = new AddDocumentRequest().content("test");

    AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
        .stringValue(stringValue).booleanValue(null).numberValue(numberValue);
    docReq.addAttributesItem(new AddDocumentAttribute(o));

    return this.documentsApi.addDocument(docReq, siteId, null).getDocumentId();
  }

  private String addDocument(final String siteId) throws ApiException {
    AddDocumentRequest docReq = new AddDocumentRequest().content("test");
    return this.documentsApi.addDocument(docReq, siteId, null).getDocumentId();
  }

  private void addDocumentAttribute(final String siteId, final String documentId,
      final AddDocumentAttribute attribute) throws ApiException {
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attribute);
    this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
  }

  private String addDocumentAttribute(final String siteId, final String key,
      final String stringValue, final Boolean booleanValue, final BigDecimal numberValue)
      throws ApiException {

    AddDocumentUploadRequest docReq = new AddDocumentUploadRequest();

    if (key != null) {
      AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
          .stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue);
      AddDocumentAttribute attr = new AddDocumentAttribute(o);
      docReq.addAttributesItem(attr);
    }

    return this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();
  }

  private void assertInvalidSearch(final String siteId, final DocumentSearchRequest searchRequest,
      final String responseBody) {
    try {
      this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals(responseBody, e.getResponseBody());
    }
  }

  private void deleteDocumentAttributeSecurity(final String siteId, final String documentId)
      throws ApiException {
    // when
    DeleteResponse response =
        this.documentAttributesApi.deleteDocumentAttribute(documentId, "security", siteId);

    // then
    assertEquals("attribute 'security' removed from document '" + documentId + "'",
        response.getMessage());
    List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
        .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
    assertEquals(2, attributes.size());
    assertEquals("nums", attributes.get(0).getKey());
    assertEquals("100,123,200", String.join(",", notNull(attributes.get(0).getNumberValues())
        .stream().map(n -> formatDouble(n.doubleValue())).toList()));
    assertEquals("strings", attributes.get(1).getKey());
    assertEquals("abc,xyz", String.join(",", notNull(attributes.get(1).getStringValues())));
  }

  /**
   * Get {@link Attribute}.
   *
   * @param siteId {@link String}
   * @return {@link Attribute}
   * @throws ApiException ApiException
   */
  private Attribute getAttribute(final String siteId) throws ApiException {
    return this.attributesApi.getAttribute("security", siteId).getAttribute();
  }

  private DocumentAttribute getDocumentAttribute(final String siteId, final String documentId,
      final String key) throws ApiException {
    return this.documentAttributesApi.getDocumentAttribute(documentId, key, siteId).getAttribute();
  }

  /**
   * POST /attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributes01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      String key = "security_" + siteId;
      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));

      // when
      AddAttributeResponse response = this.attributesApi.addAttribute(req, siteId);

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());

      List<Attribute> attributes =
          notNull(this.attributesApi.getAttributes(siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      Attribute attribute = attributes.get(0);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertEquals(AttributeDataType.STRING, attribute.getDataType());

      attribute = this.attributesApi.getAttribute(key, siteId).getAttribute();
      assertNotNull(attribute);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertEquals(AttributeDataType.STRING, attribute.getDataType());

      try {
        this.attributesApi.addAttribute(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"key\"," + "\"error\":\"attribute '" + key
            + "' already exists\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /attributes missing key.
   *
   */
  @Test
  public void testAddAttributes02() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest();

      // when
      try {
        this.attributesApi.addAttribute(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid request body\"}", e.getResponseBody());
      }

      // given
      req = new AddAttributeRequest().attribute(new AddAttribute());
      // when
      try {
        this.attributesApi.addAttribute(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"key\",\"error\":\"'key' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /attributes reserved key.
   *
   */
  @Test
  public void testAddAttributes03() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req =
          new AddAttributeRequest().attribute(new AddAttribute().key("publication"));

      // when
      try {
        this.attributesApi.addAttribute(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"key\","
                + "\"error\":\"'publication' is a reserved attribute name\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      String documentId = addDocument(siteId, key, "confidential", null);

      // then
      GetDocumentAttributeResponse response =
          this.documentAttributesApi.getDocumentAttribute(documentId, key, siteId);
      assertEquals("confidential",
          Objects.requireNonNull(response.getAttribute()).getStringValue());
      assertEquals("joesmith", Objects.requireNonNull(response.getAttribute()).getUserId());

      assertEmptyTags(siteId, documentId);
    }
  }

  private void assertEmptyTags(final String siteId, final String documentId) throws ApiException {
    List<DocumentTag> tags =
        notNull(this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags());
    assertEquals(0, tags.size());
  }

  /**
   * POST /documents. Missing attribute key.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute02() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      try {
        addDocument(siteId, null, "confidential", null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"key\",\"error\":\"'key' is missing from attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents. Invalid attribute key.
   *
   */
  @Test
  public void testAddDocumentAttribute03() {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      // when
      try {
        addDocument(siteId, key, "confidential", null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid attribute key.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId = addDocumentAttribute(siteId, null, null, null, null);
      AddDocumentAttribute attributes = new AddDocumentAttribute();
      AddDocumentAttributesRequest req =
          new AddDocumentAttributesRequest().addAttributesItem(attributes);

      // when
      try {
        this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"no attributes found\"}]}", e.getResponseBody());
      }

      // given
      attributes.setActualInstance(new AddDocumentAttributeStandard());

      // when
      try {
        this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"key\",\"error\":\"'key' is missing from attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   */
  @Test
  public void testAddDocumentAttribute05() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();

      // when
      try {
        AddDocumentAttributesRequest req =
            new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute());
        this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Add numeric value to string attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute06() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.STRING, null);

      // when
      try {
        addDocument(siteId, key, null, new BigDecimal("100"));
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support string value\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Add string value to number attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute07() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.NUMBER, null);

      // when
      try {
        addDocument(siteId, key, "asd", null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support number value\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Add string value to boolean attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute08() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.BOOLEAN, null);

      // when
      try {
        addDocument(siteId, key, "asd", null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support boolean value\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Add string value to keys only attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute09() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.KEY_ONLY, null);

      // when
      try {
        addDocument(siteId, key, "asd", null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute does not support a value\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents with Relationships bi directional.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute10() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId0 = addDocument(siteId);

      AddDocumentAttributeRelationship o = new AddDocumentAttributeRelationship()
          .documentId(documentId0).relationship(DocumentRelationshipType.PRIMARY)
          .inverseRelationship(DocumentRelationshipType.APPENDIX);
      AddDocumentRequest docReq =
          new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute(o));

      // when
      String documentId = this.documentsApi.addDocument(docReq, siteId, null).getDocumentId();

      // then
      assertNotNull(
          this.attributesApi.getAttribute(AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId));

      GetDocumentAttributeResponse response0 = this.documentAttributesApi
          .getDocumentAttribute(documentId, AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      assertEquals("PRIMARY#" + documentId0,
          Objects.requireNonNull(response0.getAttribute()).getStringValue());
      assertEquals("joesmith", Objects.requireNonNull(response0.getAttribute()).getUserId());

      GetDocumentAttributeResponse response1 = this.documentAttributesApi
          .getDocumentAttribute(documentId0, AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      assertEquals("APPENDIX#" + documentId,
          Objects.requireNonNull(response1.getAttribute()).getStringValue());
      assertEquals("joesmith", Objects.requireNonNull(response1.getAttribute()).getUserId());
    }
  }

  /**
   * POST /documents with Relationships uni-directional.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute11() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId0 = addDocument(siteId);

      AddDocumentAttributeRelationship o = new AddDocumentAttributeRelationship()
          .documentId(documentId0).relationship(DocumentRelationshipType.PRIMARY);
      AddDocumentRequest docReq =
          new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute(o));

      // when
      String documentId = this.documentsApi.addDocument(docReq, siteId, null).getDocumentId();

      // then
      assertNotNull(
          this.attributesApi.getAttribute(AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId));
      GetDocumentAttributeResponse response0 = this.documentAttributesApi
          .getDocumentAttribute(documentId, AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      assertEquals("PRIMARY#" + documentId0,
          Objects.requireNonNull(response0.getAttribute()).getStringValue());
      assertEquals("joesmith", Objects.requireNonNull(response0.getAttribute()).getUserId());

      try {
        this.documentAttributesApi.getDocumentAttribute(documentId0,
            AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"attribute 'Relationships' not found on document '"
            + documentId0 + "'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents multiple parent with attachments.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute12() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      final String documentId0 = addDocument(siteId);
      final String documentId1 = addDocument(siteId);
      final String documentId2 = addDocument(siteId);
      final String documentId3 = addDocument(siteId);

      // when
      addRelationship(siteId, documentId0, DocumentRelationshipType.APPENDIX, documentId1);
      addRelationship(siteId, documentId0, DocumentRelationshipType.APPENDIX, documentId2);
      addRelationship(siteId, documentId0, DocumentRelationshipType.ASSOCIATED, documentId2);
      addRelationship(siteId, documentId1, DocumentRelationshipType.PRIMARY, documentId0);
      addRelationship(siteId, documentId2, DocumentRelationshipType.PRIMARY, documentId0);

      // then
      assertNotNull(
          this.attributesApi.getAttribute(AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId));

      GetDocumentAttributeResponse response = this.documentAttributesApi
          .getDocumentAttribute(documentId0, AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      List<String> stringValues =
          notNull(Objects.requireNonNull(response.getAttribute()).getStringValues());
      assertTrue(stringValues.contains("APPENDIX#" + documentId1));
      assertTrue(stringValues.contains("APPENDIX#" + documentId2));

      response = this.documentAttributesApi.getDocumentAttribute(documentId1,
          AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      assertEquals("PRIMARY#" + documentId0,
          Objects.requireNonNull(response.getAttribute()).getStringValue());

      response = this.documentAttributesApi.getDocumentAttribute(documentId2,
          AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
      assertEquals("PRIMARY#" + documentId0,
          Objects.requireNonNull(response.getAttribute()).getStringValue());

      try {
        this.documentAttributesApi.getDocumentAttribute(documentId3,
            AttributeKeyReserved.RELATIONSHIPS.getKey(), siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"attribute 'Relationships' not found on document '"
            + documentId3 + "'\"}", e.getResponseBody());
      }
    }
  }

  private void addRelationship(final String siteId, final String d0,
      final DocumentRelationshipType r0, final String d1) throws ApiException {

    AddDocumentAttributeRelationship o =
        new AddDocumentAttributeRelationship().documentId(d1).relationship(r0);

    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(o));
    this.documentAttributesApi.addDocumentAttributes(d0, req, siteId, null);
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      String documentId0 = addDocumentAttribute(siteId, key, "confidential", null, null);
      String documentId1 = addDocumentAttribute(siteId, key, "confidential", null, null);

      // then
      assertEmptyTags(siteId, documentId0);
      assertEmptyTags(siteId, documentId1);

      DocumentSearchAttribute searchAttribute = new DocumentSearchAttribute().key(key);
      DocumentSearch query = new DocumentSearch().attribute(searchAttribute);
      DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);

      for (String val : Arrays.asList(null, "confidential")) {
        searchAttribute.eq(val);
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

        assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertTrue(
            documentId0.equals(sr.getDocumentId()) || documentId1.equals(sr.getDocumentId()));
        assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
        assertEquals("confidential", sr.getMatchedAttribute().getStringValue());
      }

      query.addDocumentIdsItem(documentId1);
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      assertEquals(documentId1, response.getDocuments().get(0).getDocumentId());

      searchAttribute.eq("confidential2");
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());

      Attribute attribute = getAttribute(siteId);
      assertEquals("security", attribute.getKey());
      assertEquals(AttributeDataType.STRING, attribute.getDataType());
      assertEquals(AttributeType.STANDARD, attribute.getType());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' booleanValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute02() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.BOOLEAN, null);

      // when
      String documentId = addDocumentAttribute(siteId, key, null, Boolean.TRUE, null);

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearchRequest searchRequest =
          new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

      for (Boolean val : Arrays.asList(null, Boolean.TRUE)) {
        attribute.eq(val != null ? val.toString() : null);
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

        assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
        assertEquals(Boolean.TRUE, sr.getMatchedAttribute().getBooleanValue());
      }

      attribute.eq(Boolean.FALSE.toString());
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' numberValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute03() throws ApiException {

    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      for (String numberValue : Arrays.asList("100", "50.02")) {

        final String key = "security" + UUID.randomUUID();

        addAttribute(siteId, key, AttributeDataType.NUMBER, null);

        // when
        String documentId =
            addDocumentAttribute(siteId, key, null, null, new BigDecimal(numberValue));

        // then
        DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
        DocumentSearchRequest searchRequest =
            new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

        for (BigDecimal val : Arrays.asList(null, new BigDecimal(numberValue))) {
          attribute.eq(val != null ? val.toString() : null);
          DocumentSearchResponse response =
              this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

          assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
          SearchResultDocument sr = response.getDocuments().get(0);
          assertEquals(documentId, sr.getDocumentId());
          assertEquals(key, Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
          assertEquals(numberValue, formatDouble(
              Objects.requireNonNull(sr.getMatchedAttribute().getNumberValue()).doubleValue()));
        }

        attribute.eq("101");
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
        assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
      }
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValues.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute04() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
          .stringValues(Arrays.asList("confidential1", "confidential2"));
      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute(o));

      // when
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearchRequest searchRequest =
          new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

      for (String val : Arrays.asList(null, "confidential1", "confidential2")) {
        attribute.eq(val);
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

        assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());

        assertEquals(Objects.requireNonNullElse(val, "confidential1"),
            sr.getMatchedAttribute().getStringValue());
      }

      attribute.eq("confidential3");
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' numberValues.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute05() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, AttributeDataType.NUMBER, null);

      AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
          .numberValues(Arrays.asList(new BigDecimal("100"), new BigDecimal("200")));

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute(o));

      // when
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearchRequest searchRequest =
          new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

      for (BigDecimal val : Arrays.asList(null, new BigDecimal("100"), new BigDecimal("200"))) {
        attribute.eq(val != null ? val.toString() : null);
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

        assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());

        if (val != null) {
          assertEquals(formatDouble(val.doubleValue()), formatDouble(
              Objects.requireNonNull(sr.getMatchedAttribute().getNumberValue()).doubleValue()));
        } else {
          assertEquals("100", formatDouble(
              Objects.requireNonNull(sr.getMatchedAttribute().getNumberValue()).doubleValue()));
        }
      }

      attribute.eq("confidential3");
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'range' stringValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute06() throws ApiException {
    // given
    final String key = "date";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      final String doc0 = addDocumentAttribute(siteId, key, "2024-01-01", null, null);
      final String doc1 = addDocumentAttribute(siteId, key, "2024-01-02", null, null);
      addDocumentAttribute(siteId, key, "2024-01-03", null, null);
      final String doc3 = addDocumentAttribute(siteId, key, "2024-01-04", null, null);

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearch query = new DocumentSearch().attribute(attribute);
      DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      final int expected = 4;
      assertEquals(expected, Objects.requireNonNull(response.getDocuments()).size());

      // range with start / end
      attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
      assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
      assertEquals(doc1, response.getDocuments().get(1).getDocumentId());

      // range with documents ids
      attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
      query.addDocumentIdsItem(doc1).addDocumentIdsItem(doc3);
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      assertEquals(doc1, response.getDocuments().get(0).getDocumentId());
      query.setDocumentIds(null);

      // range with start
      for (List<String> documentIds : Arrays.asList(null,
          Collections.singletonList(UUID.randomUUID().toString()))) {
        query.setDocumentIds(documentIds);
        attribute.range(new DocumentSearchRange().start("2024-01-03"));
        assertInvalidSearch(siteId, searchRequest,
            "{\"errors\":[{\"key\":\"end\",\"error\":\"'end' is required\"}]}");
      }

      // range with end only
      attribute.range(new DocumentSearchRange().end("2024-01-03"));
      assertInvalidSearch(siteId, searchRequest,
          "{\"errors\":[{\"key\":\"start\",\"error\":\"'start' is required\"}]}");
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'beginswith' stringValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute07() throws ApiException {
    // given
    final String key = "date";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      final String documentId0 = addDocumentAttribute(siteId, key, "2024-01-01", null, null);
      final String documentId1 = addDocumentAttribute(siteId, key, "2024-01-02", null, null);
      final String documentId2 = addDocumentAttribute(siteId, key, "2024-02-03", null, null);

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearch query = new DocumentSearch().attribute(attribute);
      DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      final int expected = 3;
      assertEquals(expected, Objects.requireNonNull(response.getDocuments()).size());

      // beginsWith
      attribute.beginsWith("2024-01");
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
      SearchResultDocument doc = response.getDocuments().get(0);
      assertEquals(documentId0, doc.getDocumentId());
      assertEquals(documentId1, response.getDocuments().get(1).getDocumentId());

      // correct documentids
      query.addDocumentIdsItem(documentId1);
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      doc = response.getDocuments().get(0);
      assertEquals(key, Objects.requireNonNull(doc.getMatchedAttribute()).getKey());
      assertEquals("2024-01-02", doc.getMatchedAttribute().getStringValue());

      // incorrect document id
      query.setDocumentIds(Collections.singletonList(documentId2));
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eqOr' stringValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute08() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      for (String attribute : Arrays.asList(key, "anotherkey")) {
        addAttribute(siteId, attribute, null, null);
      }

      final String doc0 = addDocumentAttribute(siteId, key, "confidential", null, null);
      addDocumentAttribute(siteId, key, "private", null, null);
      final String doc2 = addDocumentAttribute(siteId, key, "other", null, null);
      final String doc3 = addDocumentAttribute(siteId, "anotherkey", "other", null, null);

      // when
      DocumentSearchAttribute attribute =
          new DocumentSearchAttribute().key(key).eqOr(Arrays.asList("confidential", "other"));
      DocumentSearchRequest searchRequest =
          new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      // then
      assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
      assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
      assertEquals(doc2, response.getDocuments().get(1).getDocumentId());

      // given
      searchRequest.getQuery().addDocumentIdsItem(doc2).addDocumentIdsItem(doc3);

      // when
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      // then
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      assertEquals(doc2, response.getDocuments().get(0).getDocumentId());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValue with response fields.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute09() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      for (String attribute : Arrays.asList(key, "playerId", "category")) {
        addAttribute(siteId, attribute, null, null);
      }

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(createStringAttribute(key, "confidential"))
          .addAttributesItem(createStringAttribute("playerId", "1234"))
          .addAttributesItem(createStringsAttribute("category", Arrays.asList("person", "house")));

      this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null);

      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key).eq("confidential");
      DocumentSearch query = new DocumentSearch().attribute(attribute);
      DocumentSearchRequest searchRequest =
          new DocumentSearchRequest().query(query).responseFields(new SearchResponseFields()
              .addAttributesItem(key).addAttributesItem("other").addAttributesItem("category"));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      // then
      final int expected = 2;
      Map<String, SearchResultDocumentAttribute> attributes =
          notNull(Objects.requireNonNull(response.getDocuments()).get(0).getAttributes());

      assertEquals(expected, attributes.size());
      assertEquals("confidential",
          String.join(",", notNull(attributes.get("security").getStringValues())));
      assertEquals("house,person",
          String.join(",", notNull(attributes.get("category").getStringValues())));
    }
  }

  /**
   * POST /documents/upload, with invalid attributes.
   *
   */
  @Test
  public void testAddDocumentUploadAttribute10() {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(createStringAttribute(key, "confidential"));

      // when
      try {
        this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents, than PATCH /documents/{documentId}, POST /search attributes 'eq' stringValue.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute11() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      for (String attribute : Arrays.asList(key, "playerId", "category")) {
        addAttribute(siteId, attribute, null, null);
      }

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "public"));

      // when add document
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key).eq("confidential");
      DocumentSearch query = new DocumentSearch().attribute(attribute);
      DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);

      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());

      // given
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "confidential"));

      // when patch document
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * DELETE /attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteAttributes01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      // when
      DeleteResponse response = this.attributesApi.deleteAttribute(key, siteId);

      // then
      assertEquals("Attribute 'security' deleted", response.getMessage());
    }
  }

  /**
   * DELETE /attributes missing attribute.
   *
   */
  @Test
  public void testDeleteAttributes02() {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      // when
      try {
        this.attributesApi.deleteAttribute(key, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"key\",\"error\":\"attribute 'key' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /attributes in use.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteAttributes03() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "public"));

      this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null);

      // when
      try {
        this.attributesApi.deleteAttribute(key, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"error\":\"attribute '" + key + "' is in use, cannot be deleted\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with required attribute and then attempt to delete
   * attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteAttributes04() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String key = "category";
      AddAttributeRequest areq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(areq, siteId);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key)));

      this.schemasApi.setSitesSchema(siteId, req);

      // when
      try {
        this.attributesApi.deleteAttribute(key, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"attribute '" + key + "' is used in a Schema "
            + "/ Classification, cannot be deleted\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /sites/{siteId}/classifications with required attribute and then attempt to delete
   * attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteAttributes05() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String key = "category";
      AddAttributeRequest areq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(areq, siteId);

      SchemaAttributes attr =
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key));
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr));
      this.schemasApi.addClassification(siteId, req);

      // when
      try {
        this.attributesApi.deleteAttribute(key, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"attribute '" + key + "' is used in a Schema "
            + "/ Classification, cannot be deleted\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "strings", "nums")) {
        addAttribute(siteId, a, this.dataTypes.get(a), null);
      }

      // when
      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      AddDocumentAttribute numberValues = createNumbersAttribute("nums",
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123")));
      addDocumentAttribute(siteId, documentId, numberValues);

      // then
      DocumentAttribute a = this.documentAttributesApi
          .getDocumentAttribute(documentId, "security", siteId).getAttribute();

      assert a != null;
      assertEquals("security", a.getKey());
      assertEquals("confidential", a.getStringValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assert a != null;
      assertEquals("strings", a.getKey());
      assertEquals("abc,xyz", String.join(",", Objects.requireNonNull(a.getStringValues())));

      deleteDocumentAttributeSecurity(siteId, documentId);

      // when
      DeleteResponse response0 = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "strings", "abc", siteId);
      DeleteResponse response1 = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "nums", "100", siteId);

      // then
      assertEquals(
          "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
          response0.getMessage());
      assertEquals(
          "attribute value '100' removed from attribute 'nums', document '" + documentId + "'",
          response1.getMessage());

      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assert attributes != null;
      assertEquals(2, attributes.size());
      assertEquals("nums", attributes.get(0).getKey());
      assertEquals("123,200",
          String.join(",", Objects.requireNonNull(attributes.get(0).getNumberValues()).stream()
              .map(n -> formatDouble(n.doubleValue())).toList()));
      assertEquals("strings", attributes.get(1).getKey());
      assertTrue(Objects.requireNonNull(attributes.get(1).getStringValues()).isEmpty());
      assertEquals("xyz", attributes.get(1).getStringValue());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with OPA.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttribute(documentId, "security", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute "
                + "'security' is an access attribute, can only be changed by Admin\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      this.documentAttributesApi.deleteDocumentAttribute(documentId, "security", siteId);

      // then
      try {
        getDocumentAttribute(siteId, documentId, "security");
        fail();
      } catch (ApiException e) {
        assertEquals(
            "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} with OPA.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttributeValue01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttributeAndValue(documentId, "security",
            "confidential", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute "
                + "'security' is an access attribute, can only be changed by Admin\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      this.documentAttributesApi.deleteDocumentAttributeAndValue(documentId, "security",
          "confidential", siteId);

      // then
      try {
        getDocumentAttribute(siteId, documentId, "security");
        fail();
      } catch (ApiException e) {
        assertEquals(
            "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   */
  @Test
  public void testGetDocumentAttribute01() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();

      // when
      try {
        this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "other", "flag", "keyonly", "strings", "nums")) {
        addAttribute(siteId, a, this.dataTypes.get(a), null);
      }

      // when
      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute numberValue = createNumberAttribute("other", new BigDecimal("100"));
      addDocumentAttribute(siteId, documentId, numberValue);

      AddDocumentAttribute booleanValue = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE));
      addDocumentAttribute(siteId, documentId, booleanValue);

      addDocumentAttribute(siteId, documentId,
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key("keyonly")));

      AddDocumentAttribute strings =
          createStringsAttribute("strings", Arrays.asList("abc", "xyz", "123"));
      addDocumentAttribute(siteId, documentId, strings);

      AddDocumentAttribute numberValues = createNumbersAttribute("nums",
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123")));
      addDocumentAttribute(siteId, documentId, numberValues);

      // then
      DocumentAttribute a = this.documentAttributesApi
          .getDocumentAttribute(documentId, "security", siteId).getAttribute();

      assertEquals("security", a.getKey());
      assertEquals("confidential", a.getStringValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "other", siteId)
          .getAttribute();
      assertEquals("other", Objects.requireNonNull(a).getKey());
      assertEquals("100", formatDouble(Objects.requireNonNull(a.getNumberValue()).doubleValue()));

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "flag", siteId)
          .getAttribute();
      assertEquals("flag", Objects.requireNonNull(a).getKey());
      assertEquals(Boolean.TRUE, a.getBooleanValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "keyonly", siteId)
          .getAttribute();
      assertEquals("keyonly", Objects.requireNonNull(a).getKey());
      assertNull(a.getBooleanValue());
      assertNull(a.getStringValue());
      assertNull(a.getNumberValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "nums", siteId)
          .getAttribute();
      assertEquals("nums", Objects.requireNonNull(a).getKey());
      assertEquals("100,123,200", String.join(",", Objects.requireNonNull(a.getNumberValues())
          .stream().map(n -> formatDouble(n.doubleValue())).toList()));

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assertEquals("strings", Objects.requireNonNull(a).getKey());
      assertEquals("123,abc,xyz", String.join(",", Objects.requireNonNull(a.getStringValues())));
    }
  }

  /**
   * GET /documents/{documentId}/attributes.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentUploadAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      for (String attribute : Arrays.asList(key, "other", "flag", "keyonly", "strings", "nums")) {
        addAttribute(siteId, attribute, this.dataTypes.get(attribute), null);
      }

      String documentId = addDocumentAttribute(siteId, key, "confidential", null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
          .addAttributesItem(createNumberAttribute("other", new BigDecimal("100")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE)));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest().addAttributesItem(
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key("keyonly")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute("strings", Arrays.asList("abc", "xyz", "123")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest().addAttributesItem(createNumbersAttribute("nums",
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123"))));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      // when
      GetDocumentAttributesResponse response =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      // then
      final int expected = 6;
      assertEquals(expected, Objects.requireNonNull(response.getAttributes()).size());

      int i = 0;
      assertAttributeValues(response.getAttributes().get(i++), "flag", null, null, null, null,
          Boolean.TRUE);
      assertAttributeValues(response.getAttributes().get(i++), "keyonly", null, null, null, null,
          null);
      assertAttributeValues(response.getAttributes().get(i++), "nums", null, null, null,
          "100,123,200", null);
      assertAttributeValues(response.getAttributes().get(i++), "other", null, null, "100", null,
          null);
      assertAttributeValues(response.getAttributes().get(i++), "security", "confidential", null,
          null, null, null);
      assertAttributeValues(response.getAttributes().get(i), "strings", null, "123,abc,xyz", null,
          null, null);
    }
  }

  /**
   * GET /documents/{documentId}/attributes after PUT /documents/{documentId}/attributes.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentUploadAttribute02() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, key, AttributeDataType.STRING, null);
      addAttribute(siteId, key + "!", AttributeDataType.BOOLEAN, null);

      String documentId = addDocumentAttribute(siteId, null, null, null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute(key, Arrays.asList("abc", "xyz", "123")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      // when
      GetDocumentAttributesResponse response =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      // then
      final int expected = 1;
      assertEquals(expected, Objects.requireNonNull(response.getAttributes()).size());

      assertEquals(key, response.getAttributes().get(0).getKey());
      assertEquals("123,abc,xyz",
          String.join(",", notNull(response.getAttributes().get(0).getStringValues())));

      // given
      SetDocumentAttributesRequest sreq =
          new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(key + "!").booleanValue(Boolean.TRUE)));

      // when
      this.documentAttributesApi.setDocumentAttributes(documentId, sreq, siteId);

      // then
      response = this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);
      assertEquals(expected, notNull(response.getAttributes()).size());

      assertEquals(key + "!", response.getAttributes().get(0).getKey());
      assertTrue(
          Objects.requireNonNull(response.getAttributes().get(0).getStringValues()).isEmpty());
      assertEquals(Boolean.TRUE, response.getAttributes().get(0).getBooleanValue());
    }
  }

  /**
   * GET /documents/{documentId}/attributes when running POST /documents/{documentId}/attributes on
   * same attribute.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentUploadAttribute03() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      for (String attribute : List.of(key)) {
        addAttribute(siteId, attribute, null, null);
      }

      String documentId = addDocumentAttribute(siteId, key, "555", null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute(key, Arrays.asList("abc", "xyz", "123")));

      try {
        // when
        this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"document attribute 'security' already exists\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes and /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "strings", "nums")) {
        addAttribute(siteId, a, null, null);
      }

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      SetDocumentAttributeRequest req = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().stringValue("123"));

      // when
      SetResponse response =
          this.documentAttributesApi.setDocumentAttributeValue(documentId, "security", req, siteId);

      // then
      assertEquals("Updated attribute 'security' on document '" + documentId + "'",
          response.getMessage());

      DocumentAttribute a = this.documentAttributesApi
          .getDocumentAttribute(documentId, "security", siteId).getAttribute();
      assert a != null;
      assertEquals("security", a.getKey());
      assertEquals("123", a.getStringValue());

      // when
      this.documentAttributesApi.setDocumentAttributeValue(documentId, "strings", req, siteId);

      // then
      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assert a != null;
      assertEquals("strings", a.getKey());
      assertEquals("123", a.getStringValue());
    }
  }

  /**
   * PUT /documents/{documentId}/attributes with OPA attribute attached to document.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, "security", null, AttributeType.OPA);
      addAttribute(siteId, "strings", null, null);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
          .addAttributesItem(createStringAttribute("strings", "123"));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributes(documentId, sreq, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"Cannot remove attribute 'security' type OPA\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      this.documentAttributesApi.setDocumentAttributes(documentId, sreq, siteId);

      // then
      assertEquals("123", getDocumentAttribute(siteId, documentId, "strings").getStringValue());

      try {
        getDocumentAttribute(siteId, documentId, "security");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with OPA.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttributeValue01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      SetDocumentAttributeRequest sreq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().stringValue("123"));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributeValue(documentId, "security", sreq, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute "
                + "'security' is an access attribute, can only be changed by Admin\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      this.documentAttributesApi.setDocumentAttributeValue(documentId, "security", sreq, siteId);

      // then
      assertEquals("123", getDocumentAttribute(siteId, documentId, "security").getStringValue());
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   */
  @Test
  public void testSetDocumentAttribute01() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();

      // when
      try {
        SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest();
        this.documentAttributesApi.setDocumentAttributes(documentId, sreq, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /documents, existing attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUploadDocumentAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, key, AttributeDataType.STRING, null);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(createStringAttribute(key, "confidental"));

      // add document
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();
      assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

      // when - update document with Attribute Type = STANDARD
      this.documentsApi.updateDocument(documentId,
          new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")),
          siteId, null);

      // then
      assertEquals("public", getDocumentAttribute(siteId, documentId, key).getStringValue());
    }
  }

  /**
   * PATCH /documents, set Attribute Type OPA and not allow changing by anyone but admin.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUploadDocumentAttribute02() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, key, AttributeDataType.STRING, AttributeType.OPA);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(createStringAttribute(key, "confidental"));

      // add document
      String documentId =
          this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();
      assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

      // when - update document with Attribute Type = OPA
      try {
        this.documentsApi.updateDocument(documentId,
            new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")),
            siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());
      }
    }
  }
}
