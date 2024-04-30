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

import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeResponse;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.client.model.GetAttributesResponse;
import com.formkiq.client.model.SearchResponseAttributeField;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /attributes. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class AttributesRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributes01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));

      // when
      AddAttributeResponse response = this.attributesApi.addAttribute(req, siteId);

      // then
      assertEquals("Attribute 'security' created", response.getMessage());

      GetAttributesResponse attributes = this.attributesApi.getAttributes(siteId, null, null);
      assertEquals(1, attributes.getAttributes().size());
      Attribute attribute = attributes.getAttributes().get(0);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());

      GetAttributeResponse attr = this.attributesApi.getAttribute(key, siteId);
      attribute = attr.getAttribute();
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
    }
  }

  /**
   * POST /attributes missing key.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributes02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
   * POST /documents/upload, POST /search attributes 'eq' stringValue.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute01() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      // when
      String documentId0 = addDocumentAttribute(siteId, key, "confidential", null, null);
      String documentId1 = addDocumentAttribute(siteId, key, "confidential", null, null);

      // then
      DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
      DocumentSearch query = new DocumentSearch().attribute(attribute);
      DocumentSearchRequest searchRequest = new DocumentSearchRequest().query(query);

      for (String val : Arrays.asList(null, "confidential")) {
        attribute.eq(val);
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

        assertEquals(2, response.getDocuments().size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertTrue(
            documentId0.equals(sr.getDocumentId()) || documentId1.equals(sr.getDocumentId()));
        assertEquals("security", sr.getMatchedAttribute().getKey());
        assertEquals("confidential", sr.getMatchedAttribute().getStringValue());
      }

      query.addDocumentIdsItem(documentId1);
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, response.getDocuments().size());
      assertEquals(documentId1, response.getDocuments().get(0).getDocumentId());

      attribute.eq("confidential2");
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

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

        assertEquals(1, response.getDocuments().size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", sr.getMatchedAttribute().getKey());
        assertEquals(Boolean.TRUE, sr.getMatchedAttribute().getBooleanValue());
      }

      attribute.eq(Boolean.FALSE.toString());
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, response.getDocuments().size());
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String numberValue : Arrays.asList("100", "50.02")) {

        final String key = "security" + UUID.randomUUID();

        AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
        this.attributesApi.addAttribute(req, siteId);

        // AddDocumentUploadRequest docReq = new AddDocumentUploadRequest().addAttributesItem(
        // new AddDocumentAttribute().key(key).numberValue(new BigDecimal(numberValue)));

        // when
        String documentId =
            addDocumentAttribute(siteId, key, null, null, new BigDecimal(numberValue));
        // String documentId =
        // this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();

        // then
        DocumentSearchAttribute attribute = new DocumentSearchAttribute().key(key);
        DocumentSearchRequest searchRequest =
            new DocumentSearchRequest().query(new DocumentSearch().attribute(attribute));

        for (BigDecimal val : Arrays.asList(null, new BigDecimal(numberValue))) {
          attribute.eq(val != null ? val.toString() : null);
          DocumentSearchResponse response =
              this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

          assertEquals(1, response.getDocuments().size());
          SearchResultDocument sr = response.getDocuments().get(0);
          assertEquals(documentId, sr.getDocumentId());
          assertEquals(key, sr.getMatchedAttribute().getKey());
          assertEquals(numberValue, formatDouble(
              Double.valueOf(sr.getMatchedAttribute().getNumberValue().doubleValue())));
        }

        attribute.eq("101");
        DocumentSearchResponse response =
            this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
        assertEquals(0, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute().key(key)
              .stringValues(Arrays.asList("confidential1", "confidential2")));

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

        assertEquals(1, response.getDocuments().size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", sr.getMatchedAttribute().getKey());

        if (val != null) {
          assertEquals(val, sr.getMatchedAttribute().getStringValue());
        } else {
          assertEquals("confidential1", sr.getMatchedAttribute().getStringValue());
        }
      }

      attribute.eq("confidential3");
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute().key(key)
              .numberValues(Arrays.asList(new BigDecimal("100"), new BigDecimal("200"))));

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

        assertEquals(1, response.getDocuments().size());
        SearchResultDocument sr = response.getDocuments().get(0);
        assertEquals(documentId, sr.getDocumentId());
        assertEquals("security", sr.getMatchedAttribute().getKey());

        if (val != null) {
          assertEquals(formatDouble(Double.valueOf(val.doubleValue())), formatDouble(
              Double.valueOf(sr.getMatchedAttribute().getNumberValue().doubleValue())));
        } else {
          assertEquals("100", formatDouble(
              Double.valueOf(sr.getMatchedAttribute().getNumberValue().doubleValue())));
        }
      }

      attribute.eq("confidential3");
      DocumentSearchResponse response =
          this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

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
      assertEquals(expected, response.getDocuments().size());

      // range with start / end
      attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(2, response.getDocuments().size());
      assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
      assertEquals(doc1, response.getDocuments().get(1).getDocumentId());

      // range with documents ids
      attribute.range(new DocumentSearchRange().start("2024-01-01").end("2024-01-02"));
      query.addDocumentIdsItem(doc1).addDocumentIdsItem(doc3);
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, response.getDocuments().size());
      assertEquals(doc1, response.getDocuments().get(0).getDocumentId());
      query.setDocumentIds(null);

      // range with start
      for (List<String> documentIds : Arrays.asList(null,
          Arrays.asList(UUID.randomUUID().toString()))) {
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

  private void assertInvalidSearch(final String siteId, final DocumentSearchRequest searchRequest,
      final String responseBody) {
    try {
      this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals(responseBody, e.getResponseBody());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

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
      assertEquals(expected, response.getDocuments().size());

      // beginsWith
      attribute.beginsWith("2024-01");
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(2, response.getDocuments().size());
      SearchResultDocument doc = response.getDocuments().get(0);
      assertEquals(documentId0, doc.getDocumentId());
      assertEquals(documentId1, response.getDocuments().get(1).getDocumentId());

      // correct documentids
      query.addDocumentIdsItem(documentId1);
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(1, response.getDocuments().size());
      doc = response.getDocuments().get(0);
      assertEquals(key, doc.getMatchedAttribute().getKey());
      assertEquals("2024-01-02", doc.getMatchedAttribute().getStringValue());

      // incorrect document id
      query.setDocumentIds(Arrays.asList(documentId2));
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);
      assertEquals(0, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String attribute : Arrays.asList(key, "anotherkey")) {
        AddAttributeRequest req =
            new AddAttributeRequest().attribute(new AddAttribute().key(attribute));
        this.attributesApi.addAttribute(req, siteId);
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
      assertEquals(2, response.getDocuments().size());
      assertEquals(doc0, response.getDocuments().get(0).getDocumentId());
      assertEquals(doc2, response.getDocuments().get(1).getDocumentId());

      // given
      searchRequest.getQuery().addDocumentIdsItem(doc2).addDocumentIdsItem(doc3);

      // when
      response = this.searchApi.documentSearch(searchRequest, siteId, null, null, null);

      // then
      assertEquals(1, response.getDocuments().size());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String attribute : Arrays.asList(key, "playerId", "category")) {
        AddAttributeRequest req =
            new AddAttributeRequest().attribute(new AddAttribute().key(attribute));
        this.attributesApi.addAttribute(req, siteId);
      }

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(new AddDocumentAttribute().key(key).stringValue("confidential"))
          .addAttributesItem(new AddDocumentAttribute().key("playerId").stringValue("1234"))
          .addAttributesItem(new AddDocumentAttribute().key("category")
              .stringValues(Arrays.asList("person", "house")));

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
      final int expected = 3;
      List<SearchResponseAttributeField> responseAttributes =
          response.getDocuments().get(0).getResponseAttributes();

      assertEquals(expected, responseAttributes.size());
      assertEquals("security", responseAttributes.get(0).getKey());
      assertEquals("confidential", responseAttributes.get(0).getStringValue());
      assertEquals("category", responseAttributes.get(1).getKey());
      assertEquals("house", responseAttributes.get(1).getStringValue());
      assertEquals("category", responseAttributes.get(2).getKey());
      assertEquals("person", responseAttributes.get(2).getStringValue());
    }
  }

  /**
   * POST /documents/upload, with invalid attributes.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentUploadAttribute10() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(new AddDocumentAttribute().key(key).stringValue("confidential"));

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

  private String addDocumentAttribute(final String siteId, final String key,
      final String stringValue, final Boolean booleanValue, final BigDecimal numberValue)
      throws ApiException {

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute().key(key)
            .stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue));

    return this.documentsApi.addDocumentUpload(docReq, siteId, null, null, null).getDocumentId();
  }

  private String addDocument(final String siteId, final String key, final String stringValue,
      final Boolean booleanValue, final BigDecimal numberValue) throws ApiException {

    AddDocumentRequest docReq =
        new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute()
            .key(key).stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue));

    return this.documentsApi.addDocument(docReq, siteId, null).getDocumentId();
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      // when
      String documentId = addDocument(siteId, key, "confidential", null, null);

      // then
      S3Service s3 = getAwsServices().getExtension(S3Service.class);

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId) + ".fkb64";
      String content = s3.getContentAsString(STAGE_BUCKET_NAME, s3Key, null);

      String expected =
          "{\"metadata\":[],\"newCompositeTags\":false,\"accessAttributes\":[],\"documents\":[],"
              + "\"attributes\":[{\"key\":\"security\",\"stringValue\":\"confidential\","
              + "\"stringValues\":[],\"numberValues\":[]}],\"documentId\":\"" + documentId
              + "\",\"actions\":[],\"contentType\":\"application/octet-stream\","
              + "\"userId\":\"joesmith\",\"content\":\"test\",\"tags\":[]}";
      assertEquals(expected, content);
    }
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      this.attributesApi.addAttribute(req, siteId);

      // when
      try {
        addDocument(siteId, null, "confidential", null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"'key' is missing from attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents. Invalid attribute key.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute03() throws ApiException {
    // given
    final String key = "security";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      // when
      try {
        addDocument(siteId, key, "confidential", null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}",
            e.getResponseBody());
      }
    }
  }
}
