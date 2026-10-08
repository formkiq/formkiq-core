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

import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumbersAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertStringsAttribute;

import com.formkiq.testutils.api.documents.DeleteDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeRelationship;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentRelationshipType;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.Watermark;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeValueRequestBuilder;
import com.formkiq.urls.HttpStatus;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumberAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumbersAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringsAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertStringAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertJsonAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.findAttribute;

/** Tests for document attribute creation, retrieval, replacement, and deletion. */
public class DocumentAttributesRequestTest extends AbstractAttributesRequestTest {

  /**
   * POST /documents.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute01(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, null, null);

    // when
    String documentId = addDocument(siteId, key, "confidential", null, null);

    // then
    DocumentAttribute response = getDocumentAttribute(siteId, documentId, key);
    assertEquals("confidential", response.getStringValue());
    assertEquals("joesmith", response.getUserId());

    assertEmptyTags(siteId, documentId);
  }

  /**
   * POST /documents. Missing attribute key.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute02(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, null, null);

    // when

    // when
    var errorResponse1 =
        documentRequest(null, "confidential", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"message\":\"'key' is required\"}");
  }

  /**
   * POST /documents. Invalid attribute key.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute03(final String siteId) {
    // given
    final String key = "security";

    setBearerToken(siteId);

    // when

    // when
    var errorResponse1 =
        documentRequest(key, "confidential", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}");
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid attribute key.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute04(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String documentId = addDocumentAttribute(siteId, null, null, null, null);
    AddDocumentAttribute attributes = new AddDocumentAttribute();
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attributes);

    // when

    // when
    var errorResponse1 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"error\":\"no attributes found\"}]}");

    // given
    attributes.setActualInstance(new AddDocumentAttributeStandard());

    // when

    // when
    var errorResponse2 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"message\":\"'key' is required\"}");
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute05(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

    // when

    // when
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute());
    var errorResponse1 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"Document " + documentId + " not found.\"}");
  }

  /**
   * Add numeric value to string attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute06(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, AttributeDataType.STRING, null);

    // when

    // when
    var errorResponse1 =
        documentRequest(key, null, new BigDecimal("100"), null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support string value\"}]}");
  }

  /**
   * Add string value to number attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute07(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, AttributeDataType.NUMBER, null);

    // when

    // when
    var errorResponse1 = documentRequest(key, "asd", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support number value\"}]}");
  }

  /**
   * Add string value to boolean attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute08(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, AttributeDataType.BOOLEAN, null);

    // when

    // when
    var errorResponse1 = documentRequest(key, "asd", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute only support boolean value\"}]}");
  }

  /**
   * Add string value to keys only attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute09(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, AttributeDataType.KEY_ONLY, null);

    // when

    // when
    var errorResponse1 = documentRequest(key, "asd", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute does not support a value\"}]}");
  }

  /**
   * POST /documents with Relationships bi directional.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute10(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String documentId0 = addDocument(siteId);

    AddDocumentAttributeRelationship o = new AddDocumentAttributeRelationship()
        .documentId(documentId0).relationship(DocumentRelationshipType.PRIMARY)
        .inverseRelationship(DocumentRelationshipType.APPENDIX);
    AddDocumentRequest docReq =
        new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute(o));

    // when
    String documentId = new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();

    // then
    assertNotNull(new GetAttributeRequestBuilder(AttributeKeyReserved.RELATIONSHIPS.getKey())
        .submitOk(this.client, siteId).response());

    DocumentAttribute response0 =
        getDocumentAttribute(siteId, documentId, AttributeKeyReserved.RELATIONSHIPS.getKey());
    assertEquals("PRIMARY#" + documentId0, response0.getStringValue());
    assertEquals("joesmith", response0.getUserId());

    DocumentAttribute response1 =
        getDocumentAttribute(siteId, documentId0, AttributeKeyReserved.RELATIONSHIPS.getKey());
    assertEquals("APPENDIX#" + documentId, response1.getStringValue());
    assertEquals("joesmith", response1.getUserId());
  }

  /**
   * POST /documents with Relationships uni-directional.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute11(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String documentId0 = addDocument(siteId);

    AddDocumentAttributeRelationship o = new AddDocumentAttributeRelationship()
        .documentId(documentId0).relationship(DocumentRelationshipType.PRIMARY);
    AddDocumentRequest docReq =
        new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute(o));

    // when
    String documentId = new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();

    // then
    assertNotNull(new GetAttributeRequestBuilder(AttributeKeyReserved.RELATIONSHIPS.getKey())
        .submitOk(this.client, siteId).response());
    DocumentAttribute response0 =
        getDocumentAttribute(siteId, documentId, AttributeKeyReserved.RELATIONSHIPS.getKey());
    assertEquals("PRIMARY#" + documentId0, response0.getStringValue());
    assertEquals("joesmith", response0.getUserId());

    // when
    var errorResponse1 =
        new GetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId0, null),
            AttributeKeyReserved.RELATIONSHIPS.getKey()).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"attribute 'Relationships' not found on document '" + documentId0 + "'\"}");
  }

  /**
   * POST /documents multiple parent with attachments.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute12(final String siteId) throws ApiException {
    // given

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
    assertNotNull(new GetAttributeRequestBuilder(AttributeKeyReserved.RELATIONSHIPS.getKey())
        .submitOk(this.client, siteId).response());

    DocumentAttribute response =
        getDocumentAttribute(siteId, documentId0, AttributeKeyReserved.RELATIONSHIPS.getKey());
    List<String> stringValues = notNull(response.getStringValues());
    assertTrue(stringValues.contains("APPENDIX#" + documentId1));
    assertTrue(stringValues.contains("APPENDIX#" + documentId2));

    response =
        getDocumentAttribute(siteId, documentId1, AttributeKeyReserved.RELATIONSHIPS.getKey());
    assertEquals("PRIMARY#" + documentId0, response.getStringValue());

    response =
        getDocumentAttribute(siteId, documentId2, AttributeKeyReserved.RELATIONSHIPS.getKey());
    assertEquals("PRIMARY#" + documentId0, response.getStringValue());

    // when
    var errorResponse1 =
        new GetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId3, null),
            AttributeKeyReserved.RELATIONSHIPS.getKey()).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"attribute 'Relationships' not found on document '" + documentId3 + "'\"}");
  }

  /**
   * POST /documents ad watermark.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute13(final String siteId) throws ApiException {
    // given
    final String key = "wm1";

    setBearerToken(siteId);
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key)
        .dataType(AttributeDataType.WATERMARK).watermark(new Watermark().text("saldjsadkj")));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    // when
    String documentId = addDocument(siteId, key, null, null, null);

    // then
    DocumentAttribute response = getDocumentAttribute(siteId, documentId, key);
    assertNull(response.getStringValue());
    assertEquals(AttributeValueType.WATERMARK, response.getValueType());
    assertEquals("joesmith", response.getUserId());

    assertEmptyTags(siteId, documentId);
  }

  /**
   * POST /documents with Relationships with missing document.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute14(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    String documentId0 = addDocument(siteId);
    String documentId1 = ID.uuid();

    AddDocumentAttributeRelationship o0 = new AddDocumentAttributeRelationship()
        .documentId(documentId0).relationship(DocumentRelationshipType.PRIMARY);
    AddDocumentAttributeRelationship o1 = new AddDocumentAttributeRelationship()
        .documentId(documentId1).relationship(DocumentRelationshipType.PRIMARY);

    AddDocumentRequest docReq =
        new AddDocumentRequest().content("test").addAttributesItem(new AddDocumentAttribute(o0))
            .addAttributesItem(new AddDocumentAttribute(o1));

    // when

    // when
    var errorResponse1 = new AddDocumentRequestBuilder(docReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + documentId1 + "\",\"error\":\"document '" + documentId1
            + "' does not exist\"}]}");
  }

  /**
   * POST /documents with Boolean = FALSE.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttribute15(final String siteId) throws ApiException {
    // given
    final String key1 = "flag1";

    setBearerToken(siteId);
    addAttribute(siteId, key1, AttributeDataType.BOOLEAN, null);

    // when
    String documentId = addDocument(siteId, key1, null, null, Boolean.FALSE);

    // then
    DocumentAttribute response = getDocumentAttribute(siteId, documentId, key1);
    assertNotNull(response.getBooleanValue());
    assertFalse(response.getBooleanValue());
  }

  /**
   * POST /documents/{documentId}/attributes with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    // when
    AddResponse response = new AddDocumentAttributeRequestBuilder(artifact)
        .addAttribute("security", "artifact").submitOk(client, siteId).response();

    // then
    assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

    List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
    assertEquals(1, documentAttributes.size());
    assertStringAttribute(documentAttributes.getFirst(), "security", "artifact");
  }

  /**
   * POST /documents/{documentId}/attributes preserves a JSON object and its nested values.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJson(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> jsonValue = Map.of("total", 1250.50, "approved", true, "customer",
        Map.of("name", "Acme"), "lineItems", List.of(Map.of("sku", "HOSTING", "quantity", 2.0)),
        "tags", List.of("paid", "priority"));
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);

    // when
    AddResponse response = new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute(key, jsonValue).submitOk(this.client, siteId).response();

    // then
    assertEquals("added attributes to documentId '" + document.documentId() + "'",
        response.getMessage());

    // when
    DocumentAttribute attribute = new GetDocumentAttributeRequestBuilder(document, key)
        .submitOk(this.client, siteId).response().getAttribute();

    // then
    assertJsonAttribute(attribute, key, jsonValue);
  }

  /**
   * POST /documents/{documentId}/attributes accepts an empty JSON object.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJsonEmptyObject(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> jsonValue = Map.of();
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);

    // when
    AddResponse response = new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute(key, jsonValue).submitOk(this.client, siteId).response();

    // then
    assertEquals("added attributes to documentId '" + document.documentId() + "'",
        response.getMessage());

    // when
    DocumentAttribute attribute = new GetDocumentAttributeRequestBuilder(document, key)
        .submitOk(this.client, siteId).response().getAttribute();
    List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
        .submitOk(this.client, siteId).response().getAttributes();

    // then
    assertJsonAttribute(attribute, key, jsonValue);
    assertJsonAttribute(findAttribute(attributes, key), key, jsonValue);
  }

  /**
   * POST /documents/{documentId}/attributes rejects JSON for numeric definitions.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJsonForNumberDataType(final String siteId)
      throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceTotal";
    new AddAttributeRequestBuilder().keyAsNumber(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);

    // when
    var response = new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute(key, Map.of("total", 1250.50)).submit(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + key
            + "\",\"error\":\"attribute only support number value\"}]}");
  }

  /**
   * JSON attribute round trips omit nested null fields and retain other fields.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJsonOmitsNestedNull(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> customer = new HashMap<>();
    customer.put("name", "Acme");
    customer.put("reference", null);
    Map<String, Object> jsonValue = Map.of("customer", customer);
    Map<String, Object> expectedValue = Map.of("customer", Map.of("name", "Acme"));
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);

    // when
    AddResponse response = new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute(key, jsonValue).submitOk(this.client, siteId).response();

    // then
    assertEquals("added attributes to documentId '" + document.documentId() + "'",
        response.getMessage());

    // when
    DocumentAttribute attribute = new GetDocumentAttributeRequestBuilder(document, key)
        .submitOk(this.client, siteId).response().getAttribute();
    List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
        .submitOk(this.client, siteId).response().getAttributes();

    // then
    assertJsonAttribute(attribute, key, expectedValue);
    assertJsonAttribute(findAttribute(attributes, key), key, expectedValue);
  }

  /**
   * POST /documents/{documentId}/attributes requires a JSON value for JSON definitions.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJsonRequiresJsonValue(final String siteId)
      throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);

    // when
    var response = new AddDocumentAttributeRequestBuilder(document).addAttribute(key, "approved")
        .submit(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + key + "\",\"error\":\"attribute only support json value\"}]}");
  }

  /**
   * POST /documents/{documentId}/attributes rejects JSON combined with a string value.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeJsonWithStringValue(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);
    AddDocumentAttributeStandard attribute = new AddDocumentAttributeStandard().key(key)
        .jsonValue(Map.of("approved", true)).stringValue("approved");
    AddDocumentAttributesRequest request =
        new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(attribute));

    // when
    var response = new AddDocumentAttributeRequestBuilder(document).request(request)
        .submit(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + key
            + "\",\"error\":\"jsonValue cannot be combined with other value fields\"}]}");
  }

  /**
   * POST /documents/{documentId}/attributes with value that fails validationRegex.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddDocumentAttributeThatFailsAttributeValidationRegex(final String siteId)
      throws ApiException {
    // given

    setBearerToken(siteId);
    String key = "invoice_" + ID.uuid();
    addAttribute(siteId, key);
    String documentId = addDocument(siteId);
    String value = "not-an-invoice";

    // when
    var response = new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null))
        .addAttribute(key, value).submit(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + key + "\",\"error\":\"'" + key + "' unexpected value '" + value
            + "'\"}]}");
  }

  /**
   * Test Add relationship when missing linking document.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  void testAddRelationshipMissingDocument(final String siteId) {
    // given

    setBearerToken(siteId);

    // when
    var resp = new AddDocumentRequestBuilder().content().addAttribute("Relationships")
        .submit(client, siteId);

    // then
    assertApiError(resp, HttpStatus.BAD_REQUEST,
        "{\"errors\":[{\"key\":\"\",\"error\":\"document '' does not exist\"}]}");
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttribute01(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    for (String a : Arrays.asList("security", "strings", "nums")) {
      addAttribute(siteId, a, DATA_TYPES.get(a), null);
    }

    // when
    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
    addDocumentAttribute(siteId, documentId, strings);

    AddDocumentAttribute numberValues = createNumbersAttribute("nums",
        Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123")));
    addDocumentAttribute(siteId, documentId, numberValues);

    // then
    DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");
    assertNotNull(a);
    assertEquals("security", a.getKey());
    assertEquals("confidential", a.getStringValue());

    a = getDocumentAttribute(siteId, documentId, "strings");
    assertEquals("strings", a.getKey());
    assertEquals("abc,xyz", String.join(",", Objects.requireNonNull(a.getStringValues())));

    // when
    DeleteResponse deleteResponse =
        new DeleteDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
            .submitOk(this.client, siteId).response();

    // then
    assertEquals("attribute 'security' removed from document '" + documentId + "'",
        deleteResponse.getMessage());
    List<DocumentAttribute> remainingAttributes =
        notNull(new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .limit(null).next(null).submitOk(this.client, siteId).response().getAttributes());
    assertEquals(2, remainingAttributes.size());
    assertNumbersAttribute(remainingAttributes.get(0), "nums", List.of("100", "123", "200"));
    assertStringsAttribute(remainingAttributes.get(1), "strings", List.of("abc", "xyz"));

    // when
    DeleteResponse response0 =
        new DeleteDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null),
            "strings", "abc").submitOk(this.client, siteId).response();
    DeleteResponse response1 =
        new DeleteDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null),
            "nums", "100").submitOk(this.client, siteId).response();

    // then
    assertEquals(
        "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
        response0.getMessage());
    assertEquals(
        "attribute value '100' removed from attribute 'nums', document '" + documentId + "'",
        response1.getMessage());

    List<DocumentAttribute> attributes =
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null)).limit(null)
            .next(null).submitOk(this.client, siteId).response().getAttributes();
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertEquals("nums", attributes.get(0).getKey());
    assertEquals("123,200",
        String.join(",", Objects.requireNonNull(attributes.get(0).getNumberValues()).stream()
            .map(n -> formatDouble(n.doubleValue())).toList()));
    assertEquals("strings", attributes.get(1).getKey());
    assertTrue(Objects.requireNonNull(attributes.get(1).getStringValues()).isEmpty());
    assertEquals("xyz", attributes.get(1).getStringValue());
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttributeArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("security").stringValue("artifact")));
    assertEquals(1, getDocumentAttributes(siteId, artifact).size());

    // when
    DeleteResponse response = new DeleteDocumentAttributeRequestBuilder(artifact, "security")
        .submitOk(client, siteId).response();

    // then
    assertEquals("attribute 'security' removed from document '" + documentId + "'",
        response.getMessage());
    assertEquals(0, getDocumentAttributes(siteId, artifact).size());
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} removes a complete JSON attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttributeJson(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    new AddAttributeRequestBuilder().keyAsString("status").submitOk(this.client, siteId);
    DocumentArtifact document = new AddDocumentRequestBuilder().content("test")
        .addAttribute("status", "approved").getDocument(this.client, siteId);
    new AddDocumentAttributeRequestBuilder(document)
        .addJsonAttribute(key, Map.of("approved", true, "customer", Map.of("name", "Acme")))
        .submitOk(this.client, siteId);

    // when
    DeleteResponse response = new DeleteDocumentAttributeRequestBuilder(document, key)
        .submitOk(this.client, siteId).response();

    // then
    assertEquals("attribute '" + key + "' removed from document '" + document.documentId() + "'",
        response.getMessage());

    // when
    var deletedAttribute =
        new GetDocumentAttributeRequestBuilder(document, key).submit(this.client, siteId);
    final List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
        .submitOk(this.client, siteId).response().getAttributes();

    // then
    assertApiError(deletedAttribute, ApiResponseStatus.SC_NOT_FOUND.getStatusCode());
    assertNotNull(attributes);
    assertEquals(1, attributes.size());
    assertEquals("status", attributes.getFirst().getKey());
    assertEquals("approved", attributes.getFirst().getStringValue());
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttributeValueArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "strings", AttributeDataType.STRING, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact,
        createStringsAttribute("strings", Arrays.asList("abc", "xyz")));

    // when
    DeleteResponse response = new DeleteDocumentAttributeValueRequestBuilder(
        DocumentArtifact.of(documentId, artifact.artifactId()), "strings", "abc")
        .submitOk(this.client, siteId).response();

    // then
    assertEquals(
        "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
        response.getMessage());
    assertEquals("xyz", getDocumentAttribute(siteId, artifact, "strings").getStringValue());
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} rejects JSON.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttributeValueJson(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> jsonValue = Map.of("approved", true, "customer", Map.of("name", "Acme"));
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    DocumentArtifact document =
        new AddDocumentRequestBuilder().content("test").getDocument(this.client, siteId);
    new AddDocumentAttributeRequestBuilder(document).addJsonAttribute(key, jsonValue)
        .submitOk(this.client, siteId);

    for (String value : List.of("Acme", "missing",
        "{\"approved\":true,\"customer\":{\"name\":\"Acme\"}}")) {

      // when
      var response = new DeleteDocumentAttributeValueRequestBuilder(document, key, value)
          .submit(this.client, siteId);

      // then
      assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
          "{\"errors\":[{\"key\":\"" + key
              + "\",\"error\":\"JSON attributes must be deleted by attribute key.\"}]}");

      // when
      DocumentAttribute attribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertJsonAttribute(attribute, key, jsonValue);
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentAttribute01(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

    // when

    // when
    var errorResponse1 =
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null)).limit(null)
            .next(null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"Document " + documentId + " not found.\"}");
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey}.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentAttribute02(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    for (String a : Arrays.asList("security", "other", "flag", "keyonly", "strings", "nums")) {
      addAttribute(siteId, a, DATA_TYPES.get(a), null);
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
    DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");

    assertEquals("security", a.getKey());
    assertEquals("confidential", a.getStringValue());

    a = getDocumentAttribute(siteId, documentId, "other");
    assertEquals("other", Objects.requireNonNull(a).getKey());
    assertEquals("100", formatDouble(Objects.requireNonNull(a.getNumberValue()).doubleValue()));

    a = getDocumentAttribute(siteId, documentId, "flag");
    assertEquals("flag", Objects.requireNonNull(a).getKey());
    assertEquals(Boolean.TRUE, a.getBooleanValue());

    a = getDocumentAttribute(siteId, documentId, "keyonly");
    assertEquals("keyonly", Objects.requireNonNull(a).getKey());
    assertNull(a.getBooleanValue());
    assertNull(a.getStringValue());
    assertNull(a.getNumberValue());

    a = getDocumentAttribute(siteId, documentId, "nums");
    assertEquals("nums", Objects.requireNonNull(a).getKey());
    assertEquals("100,123,200", String.join(",", Objects.requireNonNull(a.getNumberValues())
        .stream().map(n -> formatDouble(n.doubleValue())).toList()));

    a = getDocumentAttribute(siteId, documentId, "strings");
    assertEquals("strings", Objects.requireNonNull(a).getKey());
    assertEquals("123,abc,xyz", String.join(",", Objects.requireNonNull(a.getStringValues())));
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetDocumentAttributeArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("security").stringValue("artifact")));

    // when
    DocumentAttribute attribute = getDocumentAttribute(siteId, artifact, "security");

    // then
    assertStringAttribute(attribute, "security", "artifact");
  }

  /**
   * PUT /documents/{documentId}/attributes and /documents/{documentId}/attributes/{attributeKey}.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttribute01(final String siteId) throws ApiException {
    // given

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
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("security").request(req).submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute 'security' on document '" + documentId + "'",
        response.getMessage());

    DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");
    assertNotNull(a);
    assertEquals("security", a.getKey());
    assertEquals("123", a.getStringValue());

    // when
    new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
        .setKey("strings").request(req).submitOk(this.client, siteId);

    // then
    a = getDocumentAttribute(siteId, documentId, "strings");
    assertNotNull(a);
    assertEquals("strings", a.getKey());
    assertEquals("123", a.getStringValue());
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} check only replaces the attributeKey.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttribute04(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    for (String a : Arrays.asList("c0", "c1")) {
      addAttribute(siteId, a, null, null);
    }

    AddDocumentUploadRequest docReq = new AddDocumentUploadRequest();
    AddDocumentAttribute attr0 =
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key("c0").stringValue("111"));
    docReq.addAttributesItem(attr0);
    AddDocumentAttribute attr1 =
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key("c1").stringValue("222"));
    docReq.addAttributesItem(attr1);

    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();
    assertNotNull(documentId);

    SetDocumentAttributeRequest req = new SetDocumentAttributeRequest()
        .attribute(new AddDocumentAttributeValue().stringValue("123"));

    // when
    SetResponse response =
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("c0").request(req).submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute 'c0' on document '" + documentId + "'", response.getMessage());

    DocumentAttribute c0 = getDocumentAttribute(siteId, documentId, "c0");
    assertNotNull(c0);
    assertEquals("c0", c0.getKey());
    assertTrue(notNull(c0.getStringValues()).isEmpty());
    assertEquals("123", c0.getStringValue());

    DocumentAttribute c1 = getDocumentAttribute(siteId, documentId, "c1");
    assertNotNull(c1);
    assertEquals("c1", c1.getKey());
    assertEquals("222", c1.getStringValue());
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with the same values.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttribute05(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    addAttribute(siteId, "c0", null, null);

    AddDocumentUploadRequest docReq = new AddDocumentUploadRequest();
    AddDocumentAttribute attr0 = new AddDocumentAttribute(new AddDocumentAttributeStandard()
        .key("c0").addStringValuesItem("111").addStringValuesItem("222"));
    docReq.addAttributesItem(attr0);

    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();
    assertNotNull(documentId);

    SetDocumentAttributeRequest req = new SetDocumentAttributeRequest().attribute(
        new AddDocumentAttributeValue().addStringValuesItem("111").addStringValuesItem("222"));

    // when
    SetResponse response =
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("c0").request(req).submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute 'c0' on document '" + documentId + "'", response.getMessage());

    DocumentAttribute c0 = getDocumentAttribute(siteId, documentId, "c0");
    assertNotNull(c0);
    assertEquals("c0", c0.getKey());
    assertEquals("111,222", String.join(",", notNull(c0.getStringValues())));

    // given
    req = new SetDocumentAttributeRequest()
        .attribute(new AddDocumentAttributeValue().addStringValuesItem("111"));

    // when
    new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null)).setKey("c0")
        .request(req).submitOk(this.client, siteId);

    // then
    c0 = getDocumentAttribute(siteId, documentId, "c0");
    assertNotNull(c0);
    assertEquals("c0", c0.getKey());
    assertEquals("111", c0.getStringValue());
  }

  /**
   * PUT /documents/{documentId}/attributes with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributeArtifact01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);
    addAttribute(siteId, "strings", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("security").stringValue("artifact")));

    // when
    SetResponse response = new SetDocumentAttributeRequestBuilder(artifact)
        .addAttribute("strings", "123").submitOk(client, siteId).response();

    // then
    assertEquals("set attributes on documentId '" + documentId + "'", response.getMessage());
    List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
    assertEquals(1, documentAttributes.size());
    assertStringAttribute(documentAttributes.getFirst(), "strings", "123");
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributeArtifactValue01(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "c0", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact,
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key("c0").stringValue("111")));

    // when
    SetResponse response = new SetDocumentAttributeValueRequestBuilder(artifact).setKey("c0")
        .stringValue("123").submitOk(client, siteId).response();

    // then
    assertEquals("Updated attribute 'c0' on document '" + documentId + "'", response.getMessage());
    List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
    assertEquals(1, documentAttributes.size());
    assertStringAttribute(documentAttributes.getFirst(), "c0", "123");
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} replaces JSON and preserves other keys.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributeJson(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> replacementValue = replacementInvoiceDetails();
    DocumentArtifact document = createInvoiceDocument(siteId);

    // when
    SetResponse response = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
        .jsonValue(replacementValue).submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute '" + key + "' on document '" + document.documentId() + "'",
        response.getMessage());

    // when
    List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
        .submitOk(this.client, siteId).response().getAttributes();

    // then
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertJsonAttribute(findAttribute(attributes, key), key, replacementValue);
    assertStringAttribute(findAttribute(attributes, "status"), "status", "paid");
  }

  /**
   * Repeated PUT writes leave one current JSON object and preserve unrelated attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributeJsonRepeatedWrites(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> replacementValue = replacementInvoiceDetails();
    DocumentArtifact document = createInvoiceDocument(siteId);

    for (int write = 0; write < 2; write++) {
      // when
      SetResponse response = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
          .jsonValue(replacementValue).submitOk(this.client, siteId).response();

      // then
      assertEquals("Updated attribute '" + key + "' on document '" + document.documentId() + "'",
          response.getMessage());

      // when
      DocumentAttribute attribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();
      List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
          .submitOk(this.client, siteId).response().getAttributes();

      // then
      assertJsonAttribute(attribute, key, replacementValue);
      assertNotNull(attributes);
      assertEquals(2, attributes.size());
      assertEquals(1L, attributes.stream().filter(a -> key.equals(a.getKey())).count());
      assertJsonAttribute(findAttribute(attributes, key), key, replacementValue);
      assertStringAttribute(findAttribute(attributes, "status"), "status", "paid");
    }
  }

  /**
   * PUT /documents/{documentId}/attributes replaces JSON while retaining resubmitted attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributesJson(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> replacementValue = replacementInvoiceDetails();
    DocumentArtifact document = createInvoiceDocument(siteId);

    // when
    SetResponse response =
        new SetDocumentAttributeRequestBuilder(document).addJsonAttribute(key, replacementValue)
            .addAttribute("status", "paid").submitOk(this.client, siteId).response();

    // then
    assertEquals("set attributes on documentId '" + document.documentId() + "'",
        response.getMessage());

    // when
    List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(document)
        .submitOk(this.client, siteId).response().getAttributes();

    // then
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertJsonAttribute(findAttribute(attributes, key), key, replacementValue);
    assertStringAttribute(findAttribute(attributes, "status"), "status", "paid");
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testSetDocumentAttribute01(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

    // when

    // when
    SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest();
    var errorResponse1 =
        new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(sreq)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"Document " + documentId + " not found.\"}");
  }

}
