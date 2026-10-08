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

import com.formkiq.testutils.api.documents.SearchDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentRequestBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SearchResultDocumentAttribute;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeRequestBuilder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertStringAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertStringsAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumberAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumbersAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertBooleanAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertKeyOnlyAttribute;

/** Tests for attributes in document upload and update flows. */
public class DocumentUploadAttributesRequestTest extends AbstractAttributesRequestTest {

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute01(final String siteId) throws ApiException {
    // given
    final String key = "security";

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
          new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
              .previous(null).projection(null).submitOk(this.client, siteId).response();

      assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
      SearchResultDocument sr = response.getDocuments().getFirst();
      assertTrue(documentId0.equals(sr.getDocumentId()) || documentId1.equals(sr.getDocumentId()));
      assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
      assertEquals("confidential", sr.getMatchedAttribute().getStringValue());
    }

    query.addDocumentIdsItem(documentId1);
    DocumentSearchResponse response =
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    assertEquals(documentId1, response.getDocuments().getFirst().getDocumentId());

    searchAttribute.eq("confidential2");
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());

    Attribute attribute = getAttribute(siteId);
    assertEquals("security", attribute.getKey());
    assertEquals(AttributeDataType.STRING, attribute.getDataType());
    assertEquals(AttributeType.STANDARD, attribute.getType());
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' booleanValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute02(final String siteId) throws ApiException {
    // given
    final String key = "security";

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
          new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
              .previous(null).projection(null).submitOk(this.client, siteId).response();

      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      SearchResultDocument sr = response.getDocuments().getFirst();
      assertEquals(documentId, sr.getDocumentId());
      assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
      assertEquals(Boolean.TRUE, sr.getMatchedAttribute().getBooleanValue());
    }

    attribute.eq(Boolean.FALSE.toString());
    DocumentSearchResponse response =
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' numberValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute03(final String siteId) throws ApiException {
    // given

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
            new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
                .previous(null).projection(null).submitOk(this.client, siteId).response();

        assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().getFirst();
        assertEquals(documentId, sr.getDocumentId());
        assertEquals(key, Objects.requireNonNull(sr.getMatchedAttribute()).getKey());
        assertEquals(numberValue, formatDouble(
            Objects.requireNonNull(sr.getMatchedAttribute().getNumberValue()).doubleValue()));
      }

      attribute.eq("101");
      DocumentSearchResponse response =
          new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
              .previous(null).projection(null).submitOk(this.client, siteId).response();
      assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValues.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute04(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, null, null);

    AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
        .stringValues(Arrays.asList("confidential1", "confidential2"));
    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute(o));

    // when
    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();

    // then
    DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
    DocumentSearchRequest searchRequest =
        new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

    for (String val : Arrays.asList(null, "confidential1", "confidential2")) {
      attribute.eq(val);
      DocumentSearchResponse response =
          new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
              .previous(null).projection(null).submitOk(this.client, siteId).response();

      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      SearchResultDocument sr = response.getDocuments().getFirst();
      assertEquals(documentId, sr.getDocumentId());
      assertEquals("security", Objects.requireNonNull(sr.getMatchedAttribute()).getKey());

      assertEquals(Objects.requireNonNullElse(val, "confidential1"),
          sr.getMatchedAttribute().getStringValue());
    }

    attribute.eq("confidential3");
    DocumentSearchResponse response =
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' numberValues.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute05(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, AttributeDataType.NUMBER, null);

    AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
        .numberValues(Arrays.asList(new BigDecimal("100"), new BigDecimal("200")));

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute(o));

    // when
    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();

    // then
    DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
    DocumentSearchRequest searchRequest =
        new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

    for (BigDecimal val : Arrays.asList(null, new BigDecimal("100"), new BigDecimal("200"))) {
      attribute.eq(val != null ? val.toString() : null);
      DocumentSearchResponse response =
          new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
              .previous(null).projection(null).submitOk(this.client, siteId).response();

      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
      SearchResultDocument sr = response.getDocuments().getFirst();
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
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
  }

  /**
   * POST /documents/upload, POST /search attributes 'range' stringValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute06(final String siteId) throws ApiException {
    // given
    final String key = "date";

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
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();

    final int expected = 4;
    assertEquals(expected, Objects.requireNonNull(response.getDocuments()).size());

    // range with start / end
    attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
    assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
    assertEquals(doc1, response.getDocuments().get(1).getDocumentId());

    // range with documents ids
    attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
    query.addDocumentIdsItem(doc1).addDocumentIdsItem(doc3);
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    assertEquals(doc1, response.getDocuments().getFirst().getDocumentId());
    query.setDocumentIds(null);

    // range with start
    for (List<String> documentIds : Arrays.asList(null, Collections.singletonList(ID.uuid()))) {
      query.setDocumentIds(documentIds);
      attribute.range(new DocumentSearchRange().start("2024-01-03"));

      // when
      var errorResponse1 = new SearchDocumentRequestBuilder().query(searchRequest).limit(null)
          .next(null).previous(null).projection(null).submit(this.client, siteId);

      // then
      assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
          "{\"errors\":[{\"key\":\"end\",\"error\":\"'end' is required\"}]}");
    }

    // range with end only
    attribute.range(new DocumentSearchRange().end("2024-01-03"));

    // when
    var errorResponse2 = new SearchDocumentRequestBuilder().query(searchRequest).limit(null)
        .next(null).previous(null).projection(null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"start\",\"error\":\"'start' is required\"}]}");
  }

  /**
   * POST /documents/upload, POST /search attributes 'beginswith' stringValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute07(final String siteId) throws ApiException {
    // given
    final String key = "date";

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
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();

    final int expected = 3;
    assertEquals(expected, Objects.requireNonNull(response.getDocuments()).size());

    // beginsWith
    attribute.beginsWith("2024-01");
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
    SearchResultDocument doc = response.getDocuments().get(0);
    assertEquals(documentId0, doc.getDocumentId());
    assertEquals(documentId1, response.getDocuments().get(1).getDocumentId());

    // correct documentids
    query.addDocumentIdsItem(documentId1);
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    doc = response.getDocuments().getFirst();
    assertEquals(key, Objects.requireNonNull(doc.getMatchedAttribute()).getKey());
    assertEquals("2024-01-02", doc.getMatchedAttribute().getStringValue());

    // incorrect document id
    query.setDocumentIds(Collections.singletonList(documentId2));
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());
  }

  /**
   * POST /documents/upload, POST /search attributes 'eqOr' stringValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute08(final String siteId) throws ApiException {
    // given
    final String key = "security";

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
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();

    // then
    assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
    assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
    assertEquals(doc2, response.getDocuments().get(1).getDocumentId());

    // given
    searchRequest.getQuery().addDocumentIdsItem(doc2).addDocumentIdsItem(doc3);

    // when
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();

    // then
    assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    assertEquals(doc2, response.getDocuments().getFirst().getDocumentId());
  }

  /**
   * POST /documents/upload, POST /search attributes 'eq' stringValue with response fields.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute09(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);

    for (String attribute : Arrays.asList(key, "playerId", "category")) {
      addAttribute(siteId, attribute, null, null);
    }

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "confidential"))
            .addAttributesItem(createStringAttribute("playerId", "1234")).addAttributesItem(
                createStringsAttribute("category", Arrays.asList("person", "house")));

    new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId);

    DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key).eq("confidential");
    DocumentSearch query = new DocumentSearch().attribute(attribute);
    DocumentSearchRequest searchRequest =
        new DocumentSearchRequest().query(query).responseFields(new SearchResponseFields()
            .addAttributesItem(key).addAttributesItem("other").addAttributesItem("category"));

    // when
    DocumentSearchResponse response =
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();

    // then
    final int expected = 2;
    Map<String, SearchResultDocumentAttribute> attributes =
        notNull(Objects.requireNonNull(response.getDocuments()).getFirst().getAttributes());

    assertEquals(expected, attributes.size());
    assertEquals("confidential",
        String.join(",", notNull(attributes.get("security").getStringValues())));
    assertEquals("house,person",
        String.join(",", notNull(attributes.get("category").getStringValues())));
  }

  /**
   * POST /documents/upload, with invalid attributes.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute10(final String siteId) {
    // given
    final String key = "security";

    setBearerToken(siteId);

    AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
        .addAttributesItem(createStringAttribute(key, "confidential"));

    // when

    // when
    var errorResponse1 = new AddDocumentUploadRequestBuilder(docReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}");
  }

  /**
   * POST /documents, than PATCH /documents/{documentId}, POST /search attributes 'eq' stringValue.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentUploadAttribute11(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);

    for (String attribute : Arrays.asList(key, "playerId", "category")) {
      addAttribute(siteId, attribute, null, null);
    }

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "public"));

    // when
    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();

    // then
    assertNotNull(documentId);
    DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key).eq("confidential");
    DocumentSearch query = new DocumentSearch().attribute(attribute);
    DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);

    DocumentSearchResponse response =
        new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
            .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(0, Objects.requireNonNull(response.getDocuments()).size());

    // given
    UpdateDocumentRequest updateReq =
        new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "confidential"));

    // when
    new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
        .submitOk(this.client, siteId);

    // then
    response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
        .previous(null).projection(null).submitOk(this.client, siteId).response();
    assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
  }

  /**
   * GET /documents/{documentId}/attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentUploadAttribute01(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    for (String attribute : Arrays.asList(key, "other", "flag", "keyonly", "strings", "nums")) {
      addAttribute(siteId, attribute, DATA_TYPES.get(attribute), null);
    }

    String documentId = addDocumentAttribute(siteId, key, "confidential", null, null);

    AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
        .addAttributesItem(createNumberAttribute("other", new BigDecimal("100")));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    req = new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE)));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    req = new AddDocumentAttributesRequest().addAttributesItem(
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key("keyonly")));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    req = new AddDocumentAttributesRequest()
        .addAttributesItem(createStringsAttribute("strings", Arrays.asList("abc", "xyz", "123")));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    req = new AddDocumentAttributesRequest().addAttributesItem(createNumbersAttribute("nums",
        Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123"))));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    // when
    GetDocumentAttributesResponse response =
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null)).limit(null)
            .next(null).submitOk(this.client, siteId).response();

    // then
    final int expected = 6;
    assertEquals(expected, Objects.requireNonNull(response.getAttributes()).size());

    int i = 0;
    assertBooleanAttribute(response.getAttributes().get(i++), "flag", Boolean.TRUE);
    assertKeyOnlyAttribute(response.getAttributes().get(i++), "keyonly");
    assertNumbersAttribute(response.getAttributes().get(i++), "nums", List.of("100", "123", "200"));
    assertNumberAttribute(response.getAttributes().get(i++), "other", "100");
    assertStringAttribute(response.getAttributes().get(i++), "security", "confidential");
    assertStringsAttribute(response.getAttributes().get(i), "strings",
        List.of("123", "abc", "xyz"));
  }

  /**
   * GET /documents/{documentId}/attributes after PUT /documents/{documentId}/attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentUploadAttribute02(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);

    addAttribute(siteId, key, AttributeDataType.STRING, null);
    addAttribute(siteId, key + "!", AttributeDataType.BOOLEAN, null);

    String documentId = addDocumentAttribute(siteId, null, null, null, null);

    AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
        .addAttributesItem(createStringsAttribute(key, Arrays.asList("abc", "xyz", "123")));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);

    // when
    GetDocumentAttributesResponse response =
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null)).limit(null)
            .next(null).submitOk(this.client, siteId).response();

    // then
    final int expected = 1;
    assertEquals(expected, Objects.requireNonNull(response.getAttributes()).size());

    assertEquals(key, response.getAttributes().getFirst().getKey());
    assertEquals("123,abc,xyz",
        String.join(",", notNull(response.getAttributes().getFirst().getStringValues())));

    // given
    SetDocumentAttributesRequest sreq =
        new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key(key + "!").booleanValue(Boolean.TRUE)));

    // when
    new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(sreq)
        .submitOk(this.client, siteId);

    // then
    response = new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
        .limit(null).next(null).submitOk(this.client, siteId).response();
    assertEquals(expected, notNull(response.getAttributes()).size());

    assertEquals(key + "!", response.getAttributes().getFirst().getKey());
    assertTrue(
        Objects.requireNonNull(response.getAttributes().getFirst().getStringValues()).isEmpty());
    assertEquals(Boolean.TRUE, response.getAttributes().getFirst().getBooleanValue());
  }

  /**
   * GET /documents/{documentId}/attributes when running POST /documents/{documentId}/attributes on
   * same attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentUploadAttribute03(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    for (String attribute : List.of(key)) {
      addAttribute(siteId, attribute, null, null);
    }

    String documentId = addDocumentAttribute(siteId, key, "555", null, null);

    AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
        .addAttributesItem(createStringsAttribute(key, Arrays.asList("abc", "xyz", "123")));

    // when
    var errorResponse1 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"document attribute 'security' already exists\"}]}");
  }

  /**
   * GET /documents/{documentId}/attributes with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentUploadAttributeArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("security").stringValue("artifact")));

    // when
    List<DocumentAttribute> attributes = getDocumentAttributes(siteId, artifact);

    // then
    assertEquals(1, attributes.size());
    assertStringAttribute(attributes.getFirst(), "security", "artifact");
  }

  /**
   * PATCH /documents, POST /documents/{documentId}/attributes, PUT
   * /documents/{documentId}/attributes, existing attribute .
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUploadDocumentAttribute01(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);

    addAttribute(siteId, key, AttributeDataType.STRING, null);

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "confidental"));

    // add document
    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();
    assertNotNull(documentId);
    assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

    // when
    new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null),
        new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")))
        .submitOk(this.client, siteId);

    // then
    assertEquals("public", getDocumentAttribute(siteId, documentId, key).getStringValue());

    // given
    AddDocumentAttributesRequest addReq =
        new AddDocumentAttributesRequest().addAttributesItem(createStringAttribute(key, "another"));

    // when

    // when
    var errorResponse1 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null))
            .request(addReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"document attribute 'security' already exists\"}]}");

    // give - PUT /documents/{documentId}/attributes
    SetDocumentAttributesRequest setReq =
        new SetDocumentAttributesRequest().addAttributesItem(createStringAttribute(key, "third"));

    // when
    new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(setReq)
        .submitOk(this.client, siteId);

    // then
    assertEquals("third", getDocumentAttribute(siteId, documentId, key).getStringValue());
  }

}
