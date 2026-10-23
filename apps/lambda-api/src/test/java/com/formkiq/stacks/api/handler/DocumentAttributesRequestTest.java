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
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertCreatedBetween;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertInsertionAdvanced;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertModifiedAfter;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertModifiedBetween;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertSameTimestamps;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertBooleanAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertKeyOnlyAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumberAttribute;
import static com.formkiq.testutils.TestWait.untilNextSecond;

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
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentRelationshipType;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertStringAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertJsonAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.findAttribute;

/** Tests for document attribute creation, retrieval, replacement, and deletion. */
public class DocumentAttributesRequestTest extends AbstractAttributesRequestTest {

  private static Stream<Arguments> unsupportedAttributeValues() {
    return sites().flatMap(siteId -> Stream.of(
        Arguments.of(siteId, AttributeDataType.STRING, null, BigDecimal.valueOf(100),
            "attribute only support string value"),
        Arguments.of(siteId, AttributeDataType.NUMBER, "asd", null,
            "attribute only support number value"),
        Arguments.of(siteId, AttributeDataType.BOOLEAN, "asd", null,
            "attribute only support boolean value"),
        Arguments.of(siteId, AttributeDataType.KEY_ONLY, "asd", null,
            "attribute does not support a value")));
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid attribute key.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributeRejectsEmptyAndMissingKeys(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String documentId = addDocumentAttribute(siteId, null, null, null, null);
    AddDocumentAttribute attributes = new AddDocumentAttribute();
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attributes);

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
    var errorResponse2 =
        new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(req)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"message\":\"'key' is required\"}");
  }

  /**
   * POST /documents/{documentId}/attributes with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributeToArtifact(final String siteId) throws ApiException {
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
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributeToMissingDocument(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

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
    DocumentAttribute attribute =
        new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);

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
    var response = new AddDocumentAttributeRequestBuilder(document).addJsonAttribute(key, jsonValue)
        .submitError(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + key + "\",\"error\":\"attribute only support json value\"}]}");
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
    DocumentAttribute attribute =
        new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);
    List<DocumentAttribute> attributes =
        new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId);

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
   * POST /documents multiple parent with attachments.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddMultipleDocumentRelationships(final String siteId) throws ApiException {
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
   * A single value starts with matching timestamps; adding a value advances modification time.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   * @throws InterruptedException if interrupted while waiting for a distinct timestamp
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateAndAppendAttributeValuesWithTimestamps(final String siteId)
      throws ApiException, InterruptedException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, null, null);

    // when
    final Instant creationStarted = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    String documentId = addDocument(siteId, key, "confidential", null, null);
    final Instant creationFinished = Instant.now();
    DocumentAttribute response =
        new GetDocumentAttributeRequestBuilder(documentId, key).getAttribute(this.client, siteId);

    // then
    assertNotNull(response);
    assertEquals("confidential", response.getStringValue());
    assertEquals("joesmith", response.getUserId());
    assertCreatedBetween(response, creationStarted, creationFinished);

    assertEmptyTags(siteId, documentId);

    // given
    untilNextSecond();

    // when
    new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null)).setKey(key)
        .stringValues(List.of("confidential", "classified")).submitOk(this.client, siteId);
    DocumentAttribute updated =
        new GetDocumentAttributeRequestBuilder(documentId, key).getAttribute(this.client, siteId);
    final DocumentAttribute listed = findAttribute(
        new GetDocumentAttributesRequestBuilder(documentId).getAttributes(this.client, siteId),
        key);

    // then
    assertStringsAttribute(updated, key, List.of("classified", "confidential"));
    assertEquals(response.getInsertedDate(), updated.getInsertedDate());
    assertModifiedAfter(response, updated);
    assertSameTimestamps(updated, listed);
  }

  /**
   * POST /documents. Missing attribute key.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateDocumentRejectsMissingAttributeKey(final String siteId)
      throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    addAttribute(siteId, key, null, null);

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
  public void testCreateDocumentRejectsUnknownAttributeKey(final String siteId) {
    // given
    final String key = "security";

    setBearerToken(siteId);

    // when
    var errorResponse1 =
        documentRequest(key, "confidential", null, null).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\",\"error\":\"attribute 'security' not found\"}]}");
  }

  /**
   * POST /documents with Relationships bi directional.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateDocumentWithBidirectionalRelationship(final String siteId)
      throws ApiException {
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
   * POST /documents with Boolean = FALSE.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateDocumentWithFalseBooleanAttribute(final String siteId) throws ApiException {
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
   * POST /documents with Relationships uni-directional.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateDocumentWithUnidirectionalRelationship(final String siteId)
      throws ApiException {
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
   * POST /documents ad watermark.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testCreateDocumentWithWatermarkAttribute(final String siteId) throws ApiException {
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
  public void testCreateRelationshipRejectsMissingDocument(final String siteId)
      throws ApiException {
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
    var errorResponse1 = new AddDocumentRequestBuilder(docReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"" + documentId1 + "\",\"error\":\"document '" + documentId1
            + "' does not exist\"}]}");
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteAttributeFromArtifact(final String siteId) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", null, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key("security").stringValue("artifact")));
    final int initialAttributeCount = getDocumentAttributes(siteId, artifact).size();

    // when
    DeleteResponse response = new DeleteDocumentAttributeRequestBuilder(artifact, "security")
        .submitOk(client, siteId).response();

    // then
    assertEquals(1, initialAttributeCount);
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
    final List<DocumentAttribute> attributes =
        new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId);

    // then
    assertApiError(deletedAttribute, ApiResponseStatus.SC_NOT_FOUND.getStatusCode());
    assertNotNull(attributes);
    assertEquals(1, attributes.size());
    assertEquals("status", attributes.getFirst().getKey());
    assertEquals("approved", attributes.getFirst().getStringValue());
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
      DocumentAttribute attribute =
          new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);

      // then
      assertJsonAttribute(attribute, key, jsonValue);
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttributesAndValues(final String siteId) throws ApiException {
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
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .getAttributes(this.client, siteId);
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
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .getAttributes(this.client, siteId);
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
   * Deleting the oldest artifact attribute value selects the earliest remaining insertion date.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   * @throws InterruptedException if interrupted while waiting for distinct timestamps
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteOldestArtifactValueUpdatesInsertionDate(final String siteId)
      throws ApiException, InterruptedException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "strings", AttributeDataType.STRING, null);

    String documentId = addDocument(siteId);
    DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

    new AddDocumentAttributeRequestBuilder(artifact).addAttribute("strings", "abc")
        .submitOk(this.client, siteId);
    untilNextSecond();
    new SetDocumentAttributeValueRequestBuilder(artifact).setKey("strings")
        .stringValues(List.of("abc", "xyz")).submitOk(this.client, siteId);
    final DocumentAttribute middle = new GetDocumentAttributeRequestBuilder(artifact, "strings")
        .getAttribute(this.client, siteId);
    untilNextSecond();
    new SetDocumentAttributeValueRequestBuilder(artifact).setKey("strings")
        .stringValues(List.of("abc", "xyz", "mno")).submitOk(this.client, siteId);
    final DocumentAttribute beforeDeletion =
        new GetDocumentAttributeRequestBuilder(artifact, "strings").getAttribute(this.client,
            siteId);

    // when
    DeleteResponse response = new DeleteDocumentAttributeValueRequestBuilder(
        DocumentArtifact.of(documentId, artifact.artifactId()), "strings", "abc")
        .submitOk(this.client, siteId).response();
    DocumentAttribute remaining = new GetDocumentAttributeRequestBuilder(artifact, "strings")
        .getAttribute(this.client, siteId);
    final DocumentAttribute listed = findAttribute(
        new GetDocumentAttributesRequestBuilder(artifact).getAttributes(this.client, siteId),
        "strings");

    // then
    assertEquals(
        "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
        response.getMessage());
    assertStringsAttribute(remaining, "strings", List.of("mno", "xyz"));
    assertEquals(middle.getLastModifiedDate(), remaining.getInsertedDate());
    assertInsertionAdvanced(beforeDeletion, remaining);
    assertEquals(beforeDeletion.getLastModifiedDate(), remaining.getLastModifiedDate());
    assertSameTimestamps(remaining, listed);
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetAttributeFromArtifact(final String siteId) throws ApiException {
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
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetAttributesForMissingDocument(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

    // when
    var errorResponse1 =
        new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .submit(this.client, siteId);

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
  public void testGetAttributesWithDifferentValueTypes(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    for (String a : Arrays.asList("security", "other", "flag", "keyonly", "strings", "nums")) {
      addAttribute(siteId, a, DATA_TYPES.get(a), null);
    }

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

    // when
    final DocumentAttribute security = getDocumentAttribute(siteId, documentId, "security");
    final DocumentAttribute other = getDocumentAttribute(siteId, documentId, "other");
    final DocumentAttribute flag = getDocumentAttribute(siteId, documentId, "flag");
    final DocumentAttribute keyOnly = getDocumentAttribute(siteId, documentId, "keyonly");
    final DocumentAttribute nums = getDocumentAttribute(siteId, documentId, "nums");
    final DocumentAttribute stringValues = getDocumentAttribute(siteId, documentId, "strings");

    // then
    assertStringAttribute(security, "security", "confidential");
    assertNumberAttribute(other, "other", "100");
    assertBooleanAttribute(flag, "flag", true);
    assertKeyOnlyAttribute(keyOnly, "keyonly");
    assertNumbersAttribute(nums, "nums", List.of("100", "123", "200"));
    assertStringsAttribute(stringValues, "strings", List.of("123", "abc", "xyz"));
  }

  /**
   * Replacing JSON preserves its insertion date and other keys, and advances modification time.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   * @throws InterruptedException if interrupted while waiting for a distinct timestamp
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testPutDocumentAttributeJson(final String siteId)
      throws ApiException, InterruptedException {
    // given
    setBearerToken(siteId);
    String key = "invoiceDetails";
    Map<String, Object> replacementValue = replacementInvoiceDetails();
    DocumentArtifact document = createInvoiceDocument(siteId);
    final DocumentAttribute original =
        new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);
    untilNextSecond();

    // when
    SetResponse response = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
        .jsonValue(replacementValue).submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute '" + key + "' on document '" + document.documentId() + "'",
        response.getMessage());

    // when
    DocumentAttribute updated =
        new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);
    List<DocumentAttribute> attributes =
        new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId);

    // then
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertJsonAttribute(updated, key, replacementValue);
    assertEquals(original.getInsertedDate(), updated.getInsertedDate());
    assertModifiedAfter(original, updated);

    DocumentAttribute listed = findAttribute(attributes, key);
    assertJsonAttribute(listed, key, replacementValue);
    assertSameTimestamps(updated, listed);
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
      DocumentAttribute attribute =
          new GetDocumentAttributeRequestBuilder(document, key).getAttribute(this.client, siteId);
      List<DocumentAttribute> attributes =
          new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId);

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
    List<DocumentAttribute> attributes =
        new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId);

    // then
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertJsonAttribute(findAttribute(attributes, key), key, replacementValue);
    assertStringAttribute(findAttribute(attributes, "status"), "status", "paid");
  }

  /**
   * Reject attribute values that do not match their declared data type.
   *
   * @param siteId site identifier; null selects the default site
   * @param dataType declared attribute data type
   * @param stringValue submitted string value
   * @param numberValue submitted numeric value
   * @param expectedError expected validation error
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}, dataType={1}]")
  @MethodSource("unsupportedAttributeValues")
  public void testRejectAttributeValueWithUnsupportedDataType(final String siteId,
      final AttributeDataType dataType, final String stringValue, final BigDecimal numberValue,
      final String expectedError) throws ApiException {
    // given
    setBearerToken(siteId);
    addAttribute(siteId, "security", dataType, null);

    // when
    var response =
        documentRequest("security", stringValue, numberValue, null).submit(this.client, siteId);

    // then
    assertApiError(response, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\",\"error\":\"" + expectedError + "\"}]}");
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testReplaceArtifactAttributeValue(final String siteId) throws ApiException {
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
   * PUT /documents/{documentId}/attributes with artifactId.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testReplaceArtifactAttributes(final String siteId) throws ApiException {
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
   * PUT /documents/{documentId}/attributes/{attributeKey} check only replaces the attributeKey.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testReplaceAttributePreservesOtherKeys(final String siteId) throws ApiException {
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


    // when
    SetResponse response =
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("c0").stringValue("123").submitOk(this.client, siteId).response();

    // then
    assertNotNull(documentId);
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
   * PUT /documents/{documentId}/attributes and /documents/{documentId}/attributes/{attributeKey}.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testReplaceSingleAndMultipleAttributeValues(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    for (String a : Arrays.asList("security", "strings", "nums")) {
      addAttribute(siteId, a, null, null);
    }

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
    addDocumentAttribute(siteId, documentId, strings);


    // when
    SetResponse response =
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("security").stringValue("123").submitOk(this.client, siteId).response();

    // then
    assertEquals("Updated attribute 'security' on document '" + documentId + "'",
        response.getMessage());

    DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");
    assertNotNull(a);
    assertEquals("security", a.getKey());
    assertEquals("123", a.getStringValue());

    // when
    new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
        .setKey("strings").stringValue("123").submitOk(this.client, siteId);

    // then
    a = getDocumentAttribute(siteId, documentId, "strings");
    assertNotNull(a);
    assertEquals("strings", a.getKey());
    assertEquals("123", a.getStringValue());
  }

  /**
   * Replacement summaries preserve retained dates and exclude removed values.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   * @throws InterruptedException if interrupted while waiting for distinct timestamps
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testReplaceValuesPreservesTimestampSummaries(final String siteId)
      throws ApiException, InterruptedException {
    // given

    setBearerToken(siteId);
    addAttribute(siteId, "c0", null, null);

    String documentId = addDocumentAttribute(siteId, "c0", "111", null, null);
    final DocumentArtifact document = DocumentArtifact.of(documentId, null);

    untilNextSecond();
    new SetDocumentAttributeValueRequestBuilder(document).setKey("c0")
        .stringValues(List.of("111", "222")).submitOk(this.client, siteId);
    final DocumentAttribute original =
        new GetDocumentAttributeRequestBuilder(document, "c0").getAttribute(this.client, siteId);
    untilNextSecond();

    // when
    SetResponse response = new SetDocumentAttributeValueRequestBuilder(document).setKey("c0")
        .stringValues(List.of("111", "222")).submitOk(this.client, siteId).response();
    DocumentAttribute unchanged =
        new GetDocumentAttributeRequestBuilder(document, "c0").getAttribute(this.client, siteId);

    // then
    assertEquals("Updated attribute 'c0' on document '" + documentId + "'", response.getMessage());
    assertStringsAttribute(unchanged, "c0", List.of("111", "222"));
    assertNotNull(original.getLastModifiedDate());
    assertNotEquals(original.getInsertedDate(), original.getLastModifiedDate());
    assertSameTimestamps(original, unchanged);

    // given
    untilNextSecond();

    // when
    final Instant replacementStarted = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    new SetDocumentAttributeValueRequestBuilder(document).setKey("c0")
        .stringValues(List.of("222", "333")).submitOk(this.client, siteId);
    final Instant replacementFinished = Instant.now();
    DocumentAttribute replaced =
        new GetDocumentAttributeRequestBuilder(document, "c0").getAttribute(this.client, siteId);
    final DocumentAttribute listed = findAttribute(
        new GetDocumentAttributesRequestBuilder(document).getAttributes(this.client, siteId), "c0");

    // then
    assertStringsAttribute(replaced, "c0", List.of("222", "333"));
    assertEquals(original.getLastModifiedDate(), replaced.getInsertedDate());
    assertModifiedBetween(replaced, replacementStarted, replacementFinished);
    assertSameTimestamps(replaced, listed);

    // when
    new SetDocumentAttributeValueRequestBuilder(document).setKey("c0").stringValue("222")
        .submitOk(this.client, siteId);
    DocumentAttribute remaining =
        new GetDocumentAttributeRequestBuilder(document, "c0").getAttribute(this.client, siteId);

    // then
    assertStringAttribute(remaining, "c0", "222");
    assertEquals(original.getLastModifiedDate(), remaining.getInsertedDate());
    assertEquals(original.getLastModifiedDate(), remaining.getLastModifiedDate());
  }

  /**
   * POST /documents/{documentId}/attributes. Invalid documentId.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testSetAttributesForMissingDocument(final String siteId) {
    // given

    setBearerToken(siteId);

    String documentId = ID.uuid();

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
