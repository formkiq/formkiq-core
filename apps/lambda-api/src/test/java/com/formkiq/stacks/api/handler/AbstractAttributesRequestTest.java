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

import com.formkiq.testutils.api.documents.GetDocumentTagsRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeRelationship;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentRelationshipType;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.UpdateAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import com.formkiq.testutils.api.ApiHttpResponse;

/** Shared setup and assertions for attribute request tests. */
public abstract class AbstractAttributesRequestTest extends AbstractApiClientRequestTest {
  /** Site used to exercise explicitly scoped requests. */
  protected static final String SITE_ID = ID.uuid();

  /** Data Type Map. */
  protected static final Map<String, AttributeDataType> DATA_TYPES =
      Map.of("other", AttributeDataType.NUMBER, "flag", AttributeDataType.BOOLEAN, "keyonly",
          AttributeDataType.KEY_ONLY, "nums", AttributeDataType.NUMBER);

  protected static void assertApiError(final ApiHttpResponse<?> response, final int statusCode) {
    assertTrue(response.isError(), "Expected an API error response");
    assertNotNull(response.exception());
    assertEquals(statusCode, response.exception().getCode());
  }

  protected static void assertApiError(final ApiHttpResponse<?> response, final int statusCode,
      final String responseBody) {
    assertApiError(response, statusCode);
    assertEquals(responseBody, response.exception().getResponseBody());
  }

  protected static Stream<String> explicitSites() {
    return Stream.of(DEFAULT_SITE_ID, SITE_ID);
  }

  protected static Map<String, Object> replacementInvoiceDetails() {
    return Map.of("approved", false, "customer", Map.of("name", "Beta"));
  }

  protected static Stream<String> schemaSites() {
    return Stream.of(DEFAULT_SITE_ID, ID.uuid());
  }

  protected static Stream<String> sites() {
    return Stream.of(null, SITE_ID);
  }

  protected AddResponse addAttribute(final String siteId, final String key) throws ApiException {
    return new AddAttributeRequestBuilder().keyAsString(key, "INV-\\d+")
        .submitOk(this.client, siteId).response();
  }

  protected void addAttribute(final String siteId, final String key,
      final AttributeDataType dataType, final AttributeType type) throws ApiException {
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key).dataType(dataType).type(type));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
  }

  protected String addDocument(final String siteId) throws ApiException {
    AddDocumentRequest docReq = new AddDocumentRequest().content("test");
    return new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId).response()
        .getDocumentId();
  }

  protected String addDocument(final String siteId, final String key, final String stringValue,
      final BigDecimal numberValue, final Boolean booleanValue) throws ApiException {
    return documentRequest(key, stringValue, numberValue, booleanValue)
        .getDocument(this.client, siteId).documentId();
  }

  protected void addDocumentAttribute(final String siteId, final DocumentArtifact document,
      final AddDocumentAttribute attribute) throws ApiException {
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attribute);
    new AddDocumentAttributeRequestBuilder(com.formkiq.aws.dynamodb.documents.DocumentArtifact
        .of(document.documentId(), document.artifactId())).request(req)
        .submitOk(this.client, siteId);
  }

  protected void addDocumentAttribute(final String siteId, final String documentId,
      final AddDocumentAttribute attribute) throws ApiException {
    addDocumentAttribute(siteId, DocumentArtifact.of(documentId, null), attribute);
  }

  protected String addDocumentAttribute(final String siteId, final String key,
      final String stringValue, final Boolean booleanValue, final BigDecimal numberValue)
      throws ApiException {

    AddDocumentUploadRequest docReq = new AddDocumentUploadRequest();

    if (key != null) {
      AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().key(key)
          .stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue);
      AddDocumentAttribute attr = new AddDocumentAttribute(o);
      docReq.addAttributesItem(attr);
    }

    return new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId).response()
        .getDocumentId();
  }

  protected void addRelationship(final String siteId, final String d0,
      final DocumentRelationshipType r0, final String d1) throws ApiException {

    AddDocumentAttributeRelationship o =
        new AddDocumentAttributeRelationship().documentId(d1).relationship(r0);

    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(o));
    new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(d0, null)).request(req)
        .submitOk(this.client, siteId);
  }

  protected Attribute assertAttributeEquals(final String siteId, final String key,
      final AttributeType attributeType, final String watermarkText) throws ApiException {
    GetAttributeResponse attribute =
        new GetAttributeRequestBuilder(key).submitOk(this.client, siteId).response();

    assertNotNull(attribute);
    assertNotNull(attribute.getAttribute());

    assertEquals(attributeType, attribute.getAttribute().getType());

    if (watermarkText != null) {
      assertNotNull(attribute.getAttribute().getWatermark());
      assertEquals(watermarkText, attribute.getAttribute().getWatermark().getText());
    }

    return attribute.getAttribute();
  }

  protected void assertEmptyTags(final String siteId, final String documentId) throws ApiException {
    List<DocumentTag> tags =
        notNull(new GetDocumentTagsRequestBuilder(documentId).setArtifactId(null).limit(null)
            .next(null).submitOk(this.client, siteId).response().getTags());
    assertEquals(0, tags.size());
  }

  protected DocumentArtifact createInvoiceDocument(final String siteId) throws ApiException {
    Map<String, Object> originalValue = Map.of("total", 1250.50, "approved", true, "customer",
        Map.of("name", "Acme", "reference", "old-reference"));
    new AddAttributeRequestBuilder().keyAsJson("invoiceDetails").submitOk(this.client, siteId);
    new AddAttributeRequestBuilder().keyAsString("status").submitOk(this.client, siteId);
    DocumentArtifact document = new AddDocumentRequestBuilder().content("test")
        .addAttribute("status", "paid").getDocument(this.client, siteId);
    new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute("invoiceDetails", originalValue).submitOk(this.client, siteId);
    return document;
  }

  protected AddDocumentRequestBuilder documentRequest(final String key, final String stringValue,
      final BigDecimal numberValue, final Boolean booleanValue) {
    AddDocumentAttributeStandard attribute = new AddDocumentAttributeStandard().key(key)
        .stringValue(stringValue).numberValue(numberValue).booleanValue(booleanValue);
    return new AddDocumentRequestBuilder(new AddDocumentRequest().content("test")
        .addAttributesItem(new AddDocumentAttribute(attribute)));
  }

  /**
   * Get {@link Attribute}.
   *
   * @param siteId {@link String}
   * @return {@link Attribute}
   * @throws ApiException ApiException
   */
  protected Attribute getAttribute(final String siteId) throws ApiException {
    return new GetAttributeRequestBuilder("security").submitOk(this.client, siteId).response()
        .getAttribute();
  }

  protected DocumentAttribute getDocumentAttribute(final String siteId,
      final DocumentArtifact document, final String key) throws ApiException {
    DocumentAttribute response = new GetDocumentAttributeRequestBuilder(document, key)
        .submitOk(this.client, siteId).response().getAttribute();
    assertNotNull(response);
    return response;
  }

  protected DocumentAttribute getDocumentAttribute(final String siteId, final String documentId,
      final String key) throws ApiException {
    return getDocumentAttribute(siteId, DocumentArtifact.of(documentId, null), key);
  }

  protected List<DocumentAttribute> getDocumentAttributes(final String siteId,
      final DocumentArtifact document) throws ApiException {
    return notNull(new GetDocumentAttributesRequestBuilder(document).submitOk(this.client, siteId)
        .response().getAttributes());
  }

  protected DocumentArtifact saveArtifactDocument(final String siteId, final String documentId)
      throws ApiException {
    AddDocumentResponse resp = new AddDocumentRequestBuilder().content().documentId(documentId)
        .artifacts(true).submitOk(this.client, siteId).response();
    return DocumentArtifact.of(resp.getDocumentId(), resp.getArtifactId());
  }

  protected void updateAttribute(final String siteId, final String key) throws ApiException {
    new UpdateAttributeRequestBuilder(key).setValidationRegex("PO-\\d+").submitOk(this.client,
        siteId);
  }
}
