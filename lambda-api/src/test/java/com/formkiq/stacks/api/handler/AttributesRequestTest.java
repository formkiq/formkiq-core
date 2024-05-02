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

import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.client.model.GetAttributesResponse;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.SearchResponseAttributeField;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import joptsimple.internal.Strings;

/** Unit Tests for request /attributes. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class AttributesRequestTest extends AbstractApiClientRequestTest {

  private void addAttribute(final String siteId, final String key) throws ApiException {
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    this.attributesApi.addAttribute(req, siteId);
  }

  private String addDocument(final String siteId, final String key, final String stringValue,
      final Boolean booleanValue, final BigDecimal numberValue) throws ApiException {

    AddDocumentRequest docReq =
        new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute()
            .key(key).stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue));

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

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(new AddDocumentAttribute().key(key)
            .stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue));

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
      addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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

  /**
   * POST /documents/{documentId}/attributes. Invalid attribute key.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String documentId = addDocumentAttribute(siteId, null, null, null, null);

      // when
      try {
        AddDocumentAttributesRequest req =
            new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute());
        this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"'key' is missing from attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
      addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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

        addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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
      addAttribute(siteId, key);

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
        addAttribute(siteId, attribute);
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
        addAttribute(siteId, attribute);
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

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
  public void testGetDocumentAttribute06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "other", "flag", "keyonly", "strings", "nums")) {
        addAttribute(siteId, a);
      }

      // when
      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute numberValue =
          new AddDocumentAttribute().key("other").numberValue(new BigDecimal("100"));
      addDocumentAttribute(siteId, documentId, numberValue);

      AddDocumentAttribute booleanValue =
          new AddDocumentAttribute().key("flag").booleanValue(Boolean.TRUE);
      addDocumentAttribute(siteId, documentId, booleanValue);

      AddDocumentAttribute keyOnly = new AddDocumentAttribute().key("keyonly");
      addDocumentAttribute(siteId, documentId, keyOnly);

      AddDocumentAttribute strings = new AddDocumentAttribute().key("strings")
          .stringValues(Arrays.asList("abc", "xyz", "123"));
      addDocumentAttribute(siteId, documentId, strings);

      AddDocumentAttribute numberValues = new AddDocumentAttribute().key("nums").numberValues(
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123")));
      addDocumentAttribute(siteId, documentId, numberValues);

      // then
      DocumentAttribute a = this.documentAttributesApi
          .getDocumentAttribute(documentId, "security", siteId).getAttribute();

      assertEquals("security", a.getKey());
      assertEquals("confidential", a.getStringValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "other", siteId)
          .getAttribute();
      assertEquals("other", a.getKey());
      assertEquals("100", formatDouble(Double.valueOf(a.getNumberValue().doubleValue())));

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "flag", siteId)
          .getAttribute();
      assertEquals("flag", a.getKey());
      assertEquals(Boolean.TRUE, a.getBooleanValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "keyonly", siteId)
          .getAttribute();
      assertEquals("keyonly", a.getKey());
      assertNull(a.getBooleanValue());
      assertNull(a.getStringValue());
      assertNull(a.getNumberValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "nums", siteId)
          .getAttribute();
      assertEquals("nums", a.getKey());
      assertEquals("100,123,200", Strings.join(a.getNumberValues().stream()
          .map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList(), ","));

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assertEquals("strings", a.getKey());
      assertEquals("123,abc,xyz", Strings.join(a.getStringValues(), ","));
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String attribute : Arrays.asList(key, "other", "flag", "keyonly", "strings", "nums")) {
        addAttribute(siteId, attribute);
      }

      String documentId = addDocumentAttribute(siteId, key, "confidential", null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest().addAttributesItem(
          new AddDocumentAttribute().key("other").numberValue(new BigDecimal("100")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("flag").booleanValue(Boolean.TRUE));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("keyonly"));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute()
          .key("strings").stringValues(Arrays.asList("abc", "xyz", "123")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      req = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("nums").numberValues(
              Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123"))));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      // when
      GetDocumentAttributesResponse response =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      // then
      final int expected = 6;
      assertEquals(expected, response.getAttributes().size());

      int i = 0;
      assertEquals("flag", response.getAttributes().get(i).getKey());
      assertEquals(Boolean.TRUE, response.getAttributes().get(i++).getBooleanValue());

      assertEquals("keyonly", response.getAttributes().get(i).getKey());
      assertNull(response.getAttributes().get(i).getStringValue());
      assertNull(response.getAttributes().get(i).getNumberValue());
      assertNull(response.getAttributes().get(i++).getBooleanValue());

      assertEquals("nums", response.getAttributes().get(i).getKey());
      assertEquals("100,123,200", Strings.join(response.getAttributes().get(i++).getNumberValues()
          .stream().map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList(), ","));

      assertEquals("other", response.getAttributes().get(i).getKey());
      assertEquals("100", formatDouble(
          Double.valueOf(response.getAttributes().get(i++).getNumberValue().doubleValue())));

      assertEquals("security", response.getAttributes().get(i).getKey());
      assertEquals("confidential", response.getAttributes().get(i++).getStringValue());

      assertEquals("strings", response.getAttributes().get(i).getKey());
      assertEquals("123,abc,xyz",
          Strings.join(response.getAttributes().get(i++).getStringValues(), ","));
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String attribute : Arrays.asList(key)) {
        addAttribute(siteId, attribute);
      }

      String documentId = addDocumentAttribute(siteId, null, null, null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest().addAttributesItem(
          new AddDocumentAttribute().key(key).stringValues(Arrays.asList("abc", "xyz", "123")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      // when
      GetDocumentAttributesResponse response =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      // then
      final int expected = 1;
      assertEquals(expected, response.getAttributes().size());

      assertEquals(key, response.getAttributes().get(0).getKey());
      assertEquals("123,abc,xyz",
          Strings.join(response.getAttributes().get(0).getStringValues(), ","));

      // given
      SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key(key).booleanValue(Boolean.TRUE));

      // when
      this.documentAttributesApi.setDocumentAttributes(documentId, sreq, siteId);

      // then
      response = this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);
      assertEquals(expected, response.getAttributes().size());

      assertEquals(key, response.getAttributes().get(0).getKey());
      assertTrue(response.getAttributes().get(0).getStringValues().isEmpty());
      assertTrue(response.getAttributes().get(0).getBooleanValue().booleanValue());
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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String attribute : Arrays.asList(key)) {
        addAttribute(siteId, attribute);
      }

      String documentId = addDocumentAttribute(siteId, key, "555", null, null);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest().addAttributesItem(
          new AddDocumentAttribute().key(key).stringValues(Arrays.asList("abc", "xyz", "123")));
      this.documentAttributesApi.addDocumentAttributes(documentId, req, siteId, null);

      // when
      GetDocumentAttributesResponse response =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      // then
      final int expected = 1;
      assertEquals(expected, response.getAttributes().size());

      assertEquals(key, response.getAttributes().get(0).getKey());
      assertEquals("123,555,abc,xyz",
          Strings.join(response.getAttributes().get(0).getStringValues(), ","));
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testSetDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
   * DELETE /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "strings", "nums")) {
        addAttribute(siteId, a);
      }

      // when
      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings =
          new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      AddDocumentAttribute numberValues = new AddDocumentAttribute().key("nums").numberValues(
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123")));
      addDocumentAttribute(siteId, documentId, numberValues);

      // then
      DocumentAttribute a = this.documentAttributesApi
          .getDocumentAttribute(documentId, "security", siteId).getAttribute();

      assertEquals("security", a.getKey());
      assertEquals("confidential", a.getStringValue());

      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assertEquals("strings", a.getKey());
      assertEquals("abc,xyz", Strings.join(a.getStringValues(), ","));

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
      assertEquals(2, attributes.size());
      assertEquals("nums", attributes.get(0).getKey());
      assertEquals("123,200", Strings.join(attributes.get(0).getNumberValues().stream()
          .map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList(), ","));
      assertEquals("strings", attributes.get(1).getKey());
      assertTrue(attributes.get(1).getStringValues().isEmpty());
      assertEquals("xyz", attributes.get(1).getStringValue());
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
    List<DocumentAttribute> attributes = this.documentAttributesApi
        .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
    assertEquals(2, attributes.size());
    assertEquals("nums", attributes.get(0).getKey());
    assertEquals("100,123,200", Strings.join(attributes.get(0).getNumberValues().stream()
        .map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList(), ","));
    assertEquals("strings", attributes.get(1).getKey());
    assertEquals("abc,xyz", Strings.join(attributes.get(1).getStringValues(), ","));
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      for (String a : Arrays.asList("security", "strings", "nums")) {
        addAttribute(siteId, a);
      }

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings =
          new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("abc", "xyz"));
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
      assertEquals("security", a.getKey());
      assertEquals("123", a.getStringValue());

      // when
      this.documentAttributesApi.setDocumentAttributeValue(documentId, "strings", req, siteId);

      // then
      a = this.documentAttributesApi.getDocumentAttribute(documentId, "strings", siteId)
          .getAttribute();
      assertEquals("strings", a.getKey());
      assertEquals("123", a.getStringValue());
    }
  }
}
