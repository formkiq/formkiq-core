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

import com.formkiq.testutils.api.schemas.AddClassificationRequestBuilder;
import com.formkiq.testutils.api.folders.GetFoldersRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.attributes.DeleteAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentTagsRequestBuilder;
import com.formkiq.testutils.api.documents.SearchDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentRequestBuilder;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeRelationship;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentRelationshipType;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SearchResultDocumentAttribute;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateAttribute;
import com.formkiq.client.model.UpdateAttributeRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.client.model.Watermark;
import com.formkiq.client.model.WatermarkPosition;
import com.formkiq.client.model.WatermarkPositionXAnchor;
import com.formkiq.client.model.WatermarkPositionYAnchor;
import com.formkiq.client.model.WatermarkScale;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributesRequestBuilder;
import com.formkiq.testutils.api.attributes.UpdateAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.schemas.SetSitesSchemaRequestBuilder;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumberAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumbersAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringsAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /attributes. */
public class AttributesRequestTest extends AbstractApiClientRequestTest {

  /** SiteId. */
  private static final String SITE_ID = ID.uuid();

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

  private AddResponse addAttribute(final String siteId, final String key) throws ApiException {
    return new AddAttributeRequestBuilder().keyAsString(key, "INV-\\d+").submit(client, siteId)
        .throwIfError().response();
  }

  private void addAttribute(final String siteId, final String key, final AttributeDataType dataType,
      final AttributeType type) throws ApiException {
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key).dataType(dataType).type(type));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
  }

  private String addDocument(final String siteId) throws ApiException {
    AddDocumentRequest docReq = new AddDocumentRequest().content("test");
    return new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId).response()
        .getDocumentId();
  }

  private String addDocument(final String siteId, final String key, final String stringValue,
      final BigDecimal numberValue, final Boolean booleanValue) throws ApiException {

    AddDocumentRequest docReq = new AddDocumentRequest().content("test");

    AddDocumentAttributeStandard o = new AddDocumentAttributeStandard().stringValue(stringValue)
        .booleanValue(booleanValue).numberValue(numberValue);

    if (key != null) {
      o.key(key);
    }

    docReq.addAttributesItem(new AddDocumentAttribute(o));

    return new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId).response()
        .getDocumentId();
  }

  private void addDocumentAttribute(final String siteId, final DocumentArtifact document,
      final AddDocumentAttribute attribute) throws ApiException {
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attribute);
    new AddDocumentAttributeRequestBuilder(com.formkiq.aws.dynamodb.documents.DocumentArtifact
        .of(document.documentId(), document.artifactId())).request(req)
        .submitOk(this.client, siteId);
  }

  private void addDocumentAttribute(final String siteId, final String documentId,
      final AddDocumentAttribute attribute) throws ApiException {
    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(attribute);
    new AddDocumentAttributeRequestBuilder(
        com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
        .submitOk(this.client, siteId);
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

    return new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId).response()
        .getDocumentId();
  }

  private void addRelationship(final String siteId, final String d0,
      final DocumentRelationshipType r0, final String d1) throws ApiException {

    AddDocumentAttributeRelationship o =
        new AddDocumentAttributeRelationship().documentId(d1).relationship(r0);

    AddDocumentAttributesRequest req =
        new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(o));
    new AddDocumentAttributeRequestBuilder(
        com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(d0, null)).request(req)
        .submitOk(this.client, siteId);
  }

  private Attribute assertAttributeEquals(final String siteId, final String key,
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

  private void assertEmptyTags(final String siteId, final String documentId) throws ApiException {
    List<DocumentTag> tags =
        notNull(new GetDocumentTagsRequestBuilder(documentId).setArtifactId(null).limit(null)
            .next(null).submitOk(this.client, siteId).response().getTags());
    assertEquals(0, tags.size());
  }

  private void assertInvalidSearch(final String siteId, final DocumentSearchRequest searchRequest,
      final String responseBody) {
    try {
      new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null).previous(null)
          .projection(null).submitOk(this.client, siteId);
      fail();
    } catch (ApiException e) {
      assertEquals(responseBody, e.getResponseBody());
    }
  }

  private void assertUpdateAttributeException(final String siteId, final String key,
      final UpdateAttributeRequest updateReq, final String errorMessage) {
    // when
    try {
      new UpdateAttributeRequestBuilder(key).request(updateReq).submitOk(this.client, siteId);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals(errorMessage, e.getResponseBody());
    }
  }

  private void deleteDocumentAttributeSecurity(final String siteId, final String documentId)
      throws ApiException {
    // when
    DeleteResponse response = new DeleteDocumentAttributeRequestBuilder(
        com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security")
        .submitOk(this.client, siteId).response();

    // then
    assertEquals("attribute 'security' removed from document '" + documentId + "'",
        response.getMessage());
    List<DocumentAttribute> attributes = notNull(new GetDocumentAttributesRequestBuilder(
        com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
        .next(null).submitOk(this.client, siteId).response().getAttributes());
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
    return new GetAttributeRequestBuilder("security").submitOk(this.client, siteId).response()
        .getAttribute();
  }

  private DocumentAttribute getDocumentAttribute(final String siteId,
      final DocumentArtifact document, final String key) throws ApiException {
    DocumentAttribute response = new GetDocumentAttributeRequestBuilder(document, key)
        .submit(client, siteId).throwIfError().response().getAttribute();
    assertNotNull(response);
    return response;
  }

  private DocumentAttribute getDocumentAttribute(final String siteId, final String documentId,
      final String key) throws ApiException {
    DocumentAttribute response = new GetDocumentAttributeRequestBuilder(
        com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), key)
        .submitOk(this.client, siteId).response().getAttribute();
    assertNotNull(response);
    return response;
  }

  private List<DocumentAttribute> getDocumentAttributes(final String siteId,
      final DocumentArtifact document) throws ApiException {
    return notNull(new GetDocumentAttributesRequestBuilder(document).submit(client, siteId)
        .throwIfError().response().getAttributes());
  }

  private DocumentArtifact saveArtifactDocument(final String siteId, final String documentId)
      throws ApiException {
    AddDocumentResponse resp = new AddDocumentRequestBuilder().content().documentId(documentId)
        .artifacts(true).submit(client, siteId).throwIfError().response();
    return DocumentArtifact.of(resp.getDocumentId(), resp.getArtifactId());
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
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());

      List<Attribute> attributes = notNull(new GetAttributesRequestBuilder().next(null)
          .submitOk(this.client, siteId).response().getAttributes());
      assertEquals(1, attributes.size());
      Attribute attribute = attributes.getFirst();
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertEquals(AttributeDataType.STRING, attribute.getDataType());

      attribute = new GetAttributeRequestBuilder(key).submitOk(this.client, siteId).response()
          .getAttribute();
      assertNotNull(attribute);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertEquals(AttributeDataType.STRING, attribute.getDataType());

      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
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
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid request body\"}", e.getResponseBody());
      }

      // given
      req = new AddAttributeRequest().attribute(new AddAttribute());
      // when
      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
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
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
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
   * POST /attributes watermark.
   *
   */
  @Test
  public void testAddAttributes04() throws ApiException {
    // given
    final String text = "sample text";
    final String attributeKey = "wm1";
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      WatermarkPosition pos =
          new WatermarkPosition().yAnchor(WatermarkPositionYAnchor.TOP).yOffset(new BigDecimal(1))
              .xAnchor(WatermarkPositionXAnchor.RIGHT).xOffset(new BigDecimal(2));

      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(attributeKey).dataType(AttributeDataType.WATERMARK)
              .watermark(new Watermark().scale(com.formkiq.client.model.WatermarkScale.ORIGINAL)
                  .rotation(new BigDecimal("123")).text(text).position(pos)));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + attributeKey + "' created", response.getMessage());

      GetAttributeResponse attr =
          new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
      assertNotNull(attr.getAttribute());
      assertEquals(attributeKey, attr.getAttribute().getKey());
      Watermark watermark = attr.getAttribute().getWatermark();
      assertNotNull(watermark);
      assertEquals(text, watermark.getText());
      assertEquals(WatermarkScale.ORIGINAL, watermark.getScale());
      assertNull(watermark.getFontSize());
      assertEquals("123.0", String.valueOf(watermark.getRotation()));
      WatermarkPosition position = watermark.getPosition();
      assertNotNull(position);
      assertEquals("2.0", String.valueOf(position.getxOffset()));
      assertEquals("1.0", String.valueOf(position.getyOffset()));
      assertEquals(WatermarkPositionXAnchor.RIGHT, position.getxAnchor());
      assertEquals(WatermarkPositionYAnchor.TOP, position.getyAnchor());
    }
  }

  /**
   * POST /attributes watermark missing watermark text.
   *
   */
  @Test
  public void testAddAttributes05() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key("wm2").dataType(AttributeDataType.WATERMARK));

      // when
      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"watermark\",\"error\":\"'watermark.text' "
            + "or 'watermark.imageDocumentId' is required\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /attributes watermark on wrong DataType.
   *
   */
  @Test
  public void testAddAttributes06() throws ApiException {
    // given
    final String attributeKey = "wm2";
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      AddAttributeRequest req =
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)
              .dataType(AttributeDataType.STRING).watermark(new Watermark().text("as")));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + attributeKey + "' created", response.getMessage());

      GetAttributeResponse attr =
          new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
      assertNotNull(attr.getAttribute());
      assertEquals(attributeKey, attr.getAttribute().getKey());
      assertNull(attr.getAttribute().getWatermark());
    }
  }

  /**
   * POST /attributes watermark image.
   *
   */
  @Test
  public void testAddAttributes07() throws ApiException {
    // given
    final String attributeKey = "wm1";
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      Watermark watermark = new Watermark().imageDocumentId(ID.uuid());
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute()
          .key(attributeKey).dataType(AttributeDataType.WATERMARK).watermark(watermark));

      // when
      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"watermark.imageDocumentId\","
                + "\"error\":\"watermark.imageDocumentId' does not exist\"}]}",
            e.getResponseBody());
      }

      // given
      String documentId = addDocument(siteId);
      watermark.setImageDocumentId(documentId);

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + attributeKey + "' created", response.getMessage());

      GetAttributeResponse attr =
          new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
      assertNotNull(attr.getAttribute());
      assertEquals(attributeKey, attr.getAttribute().getKey());
      watermark = attr.getAttribute().getWatermark();
      assertNotNull(watermark);
      assertNull(watermark.getText());
      assertEquals(documentId, watermark.getImageDocumentId());
    }
  }

  /**
   * POST /attributes add watermark text, then try and change to invalid documentid.
   *
   */
  @Test
  public void testAddAttributes08() throws ApiException {
    // given
    final String attributeKey = "wm1";
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      Watermark watermark = new Watermark().text("test");
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute()
          .key(attributeKey).dataType(AttributeDataType.WATERMARK).watermark(watermark));

      // when
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      // then
      GetAttributeResponse r =
          new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
      assertNotNull(r);
      assertNotNull(r.getAttribute());
      assertNotNull(r.getAttribute().getWatermark());
      assertEquals("test", r.getAttribute().getWatermark().getText());

      // given
      watermark.text(null).imageDocumentId(ID.uuid());

      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"key\",\"error\":\"attribute 'wm1' already exists\"},"
                + "{\"key\":\"watermark.imageDocumentId\","
                + "\"error\":\"watermark.imageDocumentId' does not exist\"}]}",
            e.getResponseBody());

        r = new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
        assertNotNull(r);
        List<Attribute> list = notNull(new GetAttributesRequestBuilder().next(null)
            .submitOk(this.client, siteId).response().getAttributes());
        assertEquals(1, list.size());
        assertEquals("wm1", list.getFirst().getKey());
      }
    }
  }

  /**
   * POST /attributes GOVERNANCE without ADMIN permission.
   *
   */
  @Test
  public void testAddAttributes09() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key("gov").type(AttributeType.GOVERNANCE));

      // when
      try {
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"gov\",\"error\":\"Access denied to attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /attributes GOVERNANCE with GOVERN permission.
   *
   */
  @Test
  public void testAddAttributes10() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");

      String key = "gov_" + ID.uuid();
      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(key).type(AttributeType.GOVERNANCE));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());
    }
  }

  /**
   * POST /attributes with DATE data type.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributesDate01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      String key = "dueDate_" + (siteId != null ? siteId : "default");
      setBearerToken(siteId);
      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(key).dataType(AttributeDataType.DATE));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());

      GetAttributeResponse getResponse =
          new GetAttributeRequestBuilder(key).submitOk(this.client, siteId).response();
      Attribute attribute = getResponse.getAttribute();
      assertNotNull(attribute);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertEquals(AttributeDataType.DATE, attribute.getDataType());
    }
  }

  /**
   * POST /attributes watermark with font size.
   *
   */
  @Test
  public void testAddAttributesFontSize() throws ApiException {
    // given
    final String text = "sample text";
    final String attributeKey = "wm1";
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(attributeKey).dataType(AttributeDataType.WATERMARK)
              .watermark(new Watermark().fontSize(new BigDecimal("17"))
                  .scale(com.formkiq.client.model.WatermarkScale.ORIGINAL).text(text)));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + attributeKey + "' created", response.getMessage());

      GetAttributeResponse attr =
          new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
      assertNotNull(attr.getAttribute());
      Watermark watermark = attr.getAttribute().getWatermark();
      assertNotNull(watermark);
      assertEquals("17.0", String.valueOf(watermark.getFontSize()));
    }
  }

  /**
   * POST /attributes with validationRegex.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddAttributesValidationRegex() throws ApiException {
    // given
    final String validationRegex = "INV-\\d+";

    for (String siteId : Arrays.asList(null, SITE_ID)) {
      setBearerToken(siteId);
      String key = "invoice_" + ID.uuid();

      // when
      AddResponse response = addAttribute(siteId, key);

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());

      var resp =
          new GetAttributeRequestBuilder(key).submit(client, siteId).throwIfError().response();
      var attribute = resp.getAttribute();
      assertNotNull(attribute);

      assertEquals(key, attribute.getKey());
      assertEquals(validationRegex, attribute.getValidationRegex());

      var attributes = notNull(new GetAttributesRequestBuilder().submit(client, siteId)
          .throwIfError().response().getAttributes());
      var o = attributes.stream().filter(a -> key.equals(a.getKey())).findFirst();
      assertTrue(o.isPresent());
      assertEquals(validationRegex, o.get().getValidationRegex());
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
      String documentId = addDocument(siteId, key, "confidential", null, null);

      // then
      DocumentAttribute response = getDocumentAttribute(siteId, documentId, key);
      assertEquals("confidential", response.getStringValue());
      assertEquals("joesmith", response.getUserId());

      assertEmptyTags(siteId, documentId);
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

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key, null, null);

      // when
      try {
        addDocument(siteId, null, "confidential", null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"'key' is required\"}", e.getResponseBody());
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
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      String documentId = addDocumentAttribute(siteId, null, null, null, null);
      AddDocumentAttribute attributes = new AddDocumentAttribute();
      AddDocumentAttributesRequest req =
          new AddDocumentAttributesRequest().addAttributesItem(attributes);

      // when
      try {
        new AddDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"no attributes found\"}]}", e.getResponseBody());
      }

      // given
      attributes.setActualInstance(new AddDocumentAttributeStandard());

      // when
      try {
        new AddDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"'key' is required\"}", e.getResponseBody());
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

      String documentId = ID.uuid();

      // when
      try {
        AddDocumentAttributesRequest req =
            new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute());
        new AddDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
            .submitOk(this.client, siteId);
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
        addDocument(siteId, key, null, new BigDecimal("100"), null);
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
        addDocument(siteId, key, "asd", null, null);
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
        addDocument(siteId, key, "asd", null, null);
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
        addDocument(siteId, key, "asd", null, null);
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
      String documentId = new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId)
          .response().getDocumentId();

      // then
      assertNotNull(new GetAttributeRequestBuilder(AttributeKeyReserved.RELATIONSHIPS.getKey())
          .submitOk(this.client, siteId).response());
      DocumentAttribute response0 =
          getDocumentAttribute(siteId, documentId, AttributeKeyReserved.RELATIONSHIPS.getKey());
      assertEquals("PRIMARY#" + documentId0, response0.getStringValue());
      assertEquals("joesmith", response0.getUserId());

      try {
        new GetDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId0, null),
            AttributeKeyReserved.RELATIONSHIPS.getKey()).submitOk(this.client, siteId);
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

      try {
        new GetDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId3, null),
            AttributeKeyReserved.RELATIONSHIPS.getKey()).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"attribute 'Relationships' not found on document '"
            + documentId3 + "'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents ad watermark.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute13() throws ApiException {
    // given
    final String key = "wm1";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

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
  }

  /**
   * POST /documents with Relationships with missing document.
   *
   */
  @Test
  public void testAddDocumentAttribute14() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

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
      try {
        new AddDocumentRequestBuilder(docReq).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"" + documentId1 + "\",\"error\":\"document '"
            + documentId1 + "' does not exist\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents with Boolean = FALSE.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttribute15() throws ApiException {
    // given
    final String key1 = "flag1";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId, key1, AttributeDataType.BOOLEAN, null);

      // when
      String documentId = addDocument(siteId, key1, null, null, Boolean.FALSE);

      // then
      DocumentAttribute response = getDocumentAttribute(siteId, documentId, key1);
      assertNotNull(response.getBooleanValue());
      assertFalse(response.getBooleanValue());
    }
  }

  /**
   * POST /documents/{documentId}/attributes with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testAddDocumentAttributeArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
      // given
      setBearerToken(siteId);
      addAttribute(siteId, "security", null, null);

      String documentId = addDocument(siteId);
      DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

      // when
      AddResponse response = new AddDocumentAttributeRequestBuilder(artifact)
          .addAttribute("security", "artifact").submit(client, siteId).throwIfError().response();

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
      assertEquals(1, documentAttributes.size());
      assertAttributeValues(documentAttributes.getFirst(), "security", "artifact", null, null, null,
          null);
    }
  }

  /**
   * POST /documents/{documentId}/attributes with value that fails validationRegex.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttributeThatFailsAttributeValidationRegex() throws ApiException {
    // given

    for (String siteId : Arrays.asList(null, SITE_ID)) {
      setBearerToken(siteId);
      String key = "invoice_" + ID.uuid();
      addAttribute(siteId, key);
      String documentId = addDocument(siteId);
      String value = "not-an-invoice";

      // when
      ApiException e = new AddDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null))
          .addAttribute(key, value).submit(client, siteId).exception();

      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"" + key + "\",\"error\":\"'" + key
          + "' unexpected value '" + value + "'\"}]}", e.getResponseBody());
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
            new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
                .previous(null).projection(null).submitOk(this.client, siteId).response();

        assertEquals(2, Objects.requireNonNull(response.getDocuments()).size());
        SearchResultDocument sr = response.getDocuments().getFirst();
        assertTrue(
            documentId0.equals(sr.getDocumentId()) || documentId1.equals(sr.getDocumentId()));
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
        new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId);
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

      // when patch document
      new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
          .submitOk(this.client, siteId);

      // then
      response = new SearchDocumentRequestBuilder().query(searchRequest).limit(null).next(null)
          .previous(null).projection(null).submitOk(this.client, siteId).response();
      assertEquals(1, Objects.requireNonNull(response.getDocuments()).size());
    }
  }

  /**
   * Test Add relationship when missing linking document.
   *
   */
  @Test
  void testAddRelationshipMissingDocument() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {
      setBearerToken(siteId);

      // when
      var resp = new AddDocumentRequestBuilder().content().addAttribute("Relationships")
          .submit(client, siteId);

      // then
      assertNotNull(resp.exception());
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals("{\"errors\":[{\"key\":\"\",\"error\":\"document '' does not exist\"}]}",
          resp.exception().getResponseBody());
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
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      // when
      DeleteResponse response =
          new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId).response();

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
        new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId);
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
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      AddDocumentUploadRequest docReq =
          new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "public"));

      new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId);

      // when
      try {
        new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"attribute 'security' is in use, cannot be deleted\"}]}",
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key = "category";
      AddAttributeRequest areq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      new AddAttributeRequestBuilder().request(areq).submitOk(this.client, siteId);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SetSchemaAttributes()
              .addRequiredItem(new AddAttributeSchemaRequired().attributeKey(key)));

      new SetSitesSchemaRequestBuilder().withSetSitesSchemaRequest(req).submitOk(this.client,
          siteId);

      // when
      try {
        new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"category\","
            + "\"error\":\"attribute 'category' is used in a Schema / "
            + "Classification, cannot be deleted\"}]}", e.getResponseBody());
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key = "category";
      AddAttributeRequest areq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      new AddAttributeRequestBuilder().request(areq).submitOk(this.client, siteId);

      SetSchemaAttributes attr = new SetSchemaAttributes()
          .addRequiredItem(new AddAttributeSchemaRequired().attributeKey(key));
      AddClassificationRequest req = new AddClassificationRequest()
          .classification(new AddClassification().name("test").attributes(attr));
      new AddClassificationRequestBuilder().request(req).submitOk(this.client, siteId);

      // when
      try {
        new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"category\","
            + "\"error\":\"attribute 'category' is used in a Schema / Classification, "
            + "cannot be deleted\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /attributes OPA / GOVERNANCE attribute.
   *
   */
  @Test
  public void testDeleteAttributes06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      for (AttributeType type : List.of(AttributeType.GOVERNANCE, AttributeType.OPA)) {

        setBearerToken(new String[] {siteId, "admins"});

        AddAttributeRequest req =
            new AddAttributeRequest().attribute(new AddAttribute().key("gov").type(type));
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

        setBearerToken(siteId);

        // when
        try {
          new DeleteAttributeRequestBuilder("gov").submitOk(this.client, siteId);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals("{\"errors\":[{\"key\":\"gov\",\"error\":\"Access denied to attribute\"}]}",
              e.getResponseBody());
        }

        // given
        setBearerToken(new String[] {siteId, "admins"});

        // when
        DeleteResponse deleteResponse =
            new DeleteAttributeRequestBuilder("gov").submitOk(this.client, siteId).response();

        // then
        assertEquals("Attribute 'gov' deleted", deleteResponse.getMessage());
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
      DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");

      assert a != null;
      assertEquals("security", a.getKey());
      assertEquals("confidential", a.getStringValue());

      a = getDocumentAttribute(siteId, documentId, "strings");
      assertEquals("strings", a.getKey());
      assertEquals("abc,xyz", String.join(",", Objects.requireNonNull(a.getStringValues())));

      deleteDocumentAttributeSecurity(siteId, documentId);

      // when
      DeleteResponse response0 = new DeleteDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "strings",
          "abc").submitOk(this.client, siteId).response();
      DeleteResponse response1 = new DeleteDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "nums", "100")
          .submitOk(this.client, siteId).response();

      // then
      assertEquals(
          "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
          response0.getMessage());
      assertEquals(
          "attribute value '100' removed from attribute 'nums', document '" + documentId + "'",
          response1.getMessage());

      List<DocumentAttribute> attributes = new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response().getAttributes();
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

      setBearerToken("Admins");
      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
      setBearerToken(siteId);

      // when
      try {
        new DeleteDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security")
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute 'security' is an protected attribute, "
            + "can only be changed by Goverance/Admin role\"}]}", e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      new DeleteDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security")
          .submitOk(this.client, siteId);

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
   * DELETE /documents/{documentId}/attributes/{attributeKey} with OPA and GOVERN.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttribute03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(new String[] {siteId, siteId + "_govern"});

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      // when
      new DeleteDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security")
          .submitOk(this.client, siteId);

      // then
      List<DocumentAttribute> documentAttributes = notNull(new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response().getAttributes());
      assertEquals(0, documentAttributes.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttributeArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
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
          .submit(client, siteId).throwIfError().response();

      // then
      assertEquals("attribute 'security' removed from document '" + documentId + "'",
          response.getMessage());
      assertEquals(0, getDocumentAttributes(siteId, artifact).size());
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");
      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
      setBearerToken(siteId);

      // when
      try {
        new DeleteDocumentAttributeValueRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security",
            "confidential").submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute 'security' is an protected attribute, "
            + "can only be changed by Goverance/Admin role\"}]}", e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      new DeleteDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null), "security",
          "confidential").submitOk(this.client, siteId);

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
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testDeleteDocumentAttributeValueArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
      // given
      setBearerToken(siteId);
      addAttribute(siteId, "strings", AttributeDataType.STRING, null);

      String documentId = addDocument(siteId);
      DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

      addDocumentAttribute(siteId, artifact,
          createStringsAttribute("strings", Arrays.asList("abc", "xyz")));

      // when
      DeleteResponse response = new DeleteDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, artifact.artifactId()),
          "strings", "abc").submitOk(this.client, siteId).response();

      // then
      assertEquals(
          "attribute value 'abc' removed from attribute 'strings', document '" + documentId + "'",
          response.getMessage());
      assertEquals("xyz", getDocumentAttribute(siteId, artifact, "strings").getStringValue());
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

      String documentId = ID.uuid();

      // when
      try {
        new GetDocumentAttributesRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
            .next(null).submitOk(this.client, siteId);
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
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentAttributeArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
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
      assertAttributeValues(attribute, "security", "artifact", null, null, null, null);
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
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      req = new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE)));
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      req = new AddDocumentAttributesRequest().addAttributesItem(
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key("keyonly")));
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      req = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute("strings", Arrays.asList("abc", "xyz", "123")));
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      req = new AddDocumentAttributesRequest().addAttributesItem(createNumbersAttribute("nums",
          Arrays.asList(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("123"))));
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      // when
      GetDocumentAttributesResponse response = new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response();

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
      new AddDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
          .submitOk(this.client, siteId);

      // when
      GetDocumentAttributesResponse response = new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
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
      new SetDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(sreq)
          .submitOk(this.client, siteId);

      // then
      response = new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response();
      assertEquals(expected, notNull(response.getAttributes()).size());

      assertEquals(key + "!", response.getAttributes().getFirst().getKey());
      assertTrue(
          Objects.requireNonNull(response.getAttributes().getFirst().getStringValues()).isEmpty());
      assertEquals(Boolean.TRUE, response.getAttributes().getFirst().getBooleanValue());
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
        new AddDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(req)
            .submitOk(this.client, siteId);
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
   * GET /documents/{documentId}/attributes with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testGetDocumentUploadAttributeArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
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
      assertAttributeValues(attributes.getFirst(), "security", "artifact", null, null, null, null);
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
      SetResponse response = new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .setKey("security").request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Updated attribute 'security' on document '" + documentId + "'",
          response.getMessage());

      DocumentAttribute a = getDocumentAttribute(siteId, documentId, "security");
      assert a != null;
      assertEquals("security", a.getKey());
      assertEquals("123", a.getStringValue());

      // when
      new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .setKey("strings").request(req).submitOk(this.client, siteId);

      // then
      a = getDocumentAttribute(siteId, documentId, "strings");
      assertNotNull(a);
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");
      addAttribute(siteId, "security", null, AttributeType.OPA);

      addAttribute(siteId, "strings", null, null);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
      setBearerToken(siteId);

      AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
          .addAttributesItem(createStringAttribute("strings", "123"));

      // when
      try {
        new SetDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(sreq)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      new SetDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(sreq)
          .submitOk(this.client, siteId);

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
   * PUT /documents/{documentId}/attributes with OPA attribute attached to document and govern.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(new String[] {siteId, siteId + "_govern"});

      addAttribute(siteId, "security", null, AttributeType.OPA);
      addAttribute(siteId, "strings", null, null);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
      addDocumentAttribute(siteId, documentId, strings);

      SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
          .addAttributesItem(createStringAttribute("strings", "123"));

      // when
      new SetDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(sreq)
          .submitOk(this.client, siteId);

      // then
      List<DocumentAttribute> documentAttributes = notNull(new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response().getAttributes());
      assertEquals(1, documentAttributes.size());
      assertEquals("strings", documentAttributes.getFirst().getKey());
      assertEquals("123", documentAttributes.getFirst().getStringValue());
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} check only replaces the attributeKey.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

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
      SetResponse response = new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).setKey("c0")
          .request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Updated attribute 'c0' on document '" + documentId + "'",
          response.getMessage());

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
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with the same values.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttribute05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

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
      SetResponse response = new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).setKey("c0")
          .request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Updated attribute 'c0' on document '" + documentId + "'",
          response.getMessage());

      DocumentAttribute c0 = getDocumentAttribute(siteId, documentId, "c0");
      assertNotNull(c0);
      assertEquals("c0", c0.getKey());
      assertEquals("111,222", String.join(",", notNull(c0.getStringValues())));

      // given
      req = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("111"));

      // when
      new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).setKey("c0")
          .request(req).submitOk(this.client, siteId);

      // then
      c0 = getDocumentAttribute(siteId, documentId, "c0");
      assertNotNull(c0);
      assertEquals("c0", c0.getKey());
      assertEquals("111", c0.getStringValue());
    }
  }

  /**
   * PUT /documents/{documentId}/attributes with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttributeArtifact01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
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
          .addAttribute("strings", "123").submit(client, siteId).throwIfError().response();

      // then
      assertEquals("set attributes on documentId '" + documentId + "'", response.getMessage());
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
      assertEquals(1, documentAttributes.size());
      assertAttributeValues(documentAttributes.getFirst(), "strings", "123", null, null, null,
          null);
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with artifactId.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPutDocumentAttributeArtifactValue01() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {
      // given
      setBearerToken(siteId);
      addAttribute(siteId, "c0", null, null);

      String documentId = addDocument(siteId);
      DocumentArtifact artifact = saveArtifactDocument(siteId, documentId);

      addDocumentAttribute(siteId, artifact, new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("c0").stringValue("111")));

      // when
      SetResponse response = new SetDocumentAttributeValueRequestBuilder(artifact).setKey("c0")
          .stringValue("123").submit(client, siteId).throwIfError().response();

      // then
      assertEquals("Updated attribute 'c0' on document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, artifact);
      assertEquals(1, documentAttributes.size());
      assertAttributeValues(documentAttributes.getFirst(), "c0", "123", null, null, null, null);
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
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");
      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      setBearerToken(siteId);
      SetDocumentAttributeRequest sreq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().stringValue("123"));

      // when
      try {
        new SetDocumentAttributeValueRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
            .setKey("security").request(sreq).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}",
            e.getResponseBody());
      }

      // given
      setBearerToken("Admins");

      // when
      new SetDocumentAttributeValueRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .setKey("security").request(sreq).submitOk(this.client, siteId);

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

      String documentId = ID.uuid();

      // when
      try {
        SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest();
        new SetDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(sreq)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /attributes/{key} update to governance and back.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributes01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken("Admins");
      String key = "abc_" + ID.uuid();
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));

      // when
      AddResponse response =
          new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' created", response.getMessage());

      // given
      UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.GOVERNANCE));

      // when
      UpdateResponse updateResponse = new UpdateAttributeRequestBuilder(key).request(updateReq)
          .submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' updated", updateResponse.getMessage());
      assertAttributeEquals(siteId, key, AttributeType.GOVERNANCE, null);

      // given
      updateReq = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.STANDARD));

      // when
      updateResponse = new UpdateAttributeRequestBuilder(key).request(updateReq)
          .submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute '" + key + "' updated", updateResponse.getMessage());
      assertAttributeEquals(siteId, key, AttributeType.STANDARD, null);
    }
  }

  /**
   * PATCH /attributes/{key} update to governance and back not admin.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributes02() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken("Admins");

      String key0 = "abc_" + ID.uuid();
      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(key0).type(AttributeType.STANDARD));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      String key1 = "abc_" + ID.uuid();
      req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(key1).type(AttributeType.GOVERNANCE));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      setBearerToken(siteId);
      UpdateAttributeRequest updateReq0 = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.GOVERNANCE));
      UpdateAttributeRequest updateReq1 = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.STANDARD));

      assertUpdateAttributeException(siteId, key0, updateReq0,
          "{\"errors\":[{\"error\":\"Access denied to attribute\"}]}");
      assertUpdateAttributeException(siteId, key1, updateReq1,
          "{\"errors\":[{\"error\":\"Access denied to attribute\"}]}");
    }
  }

  /**
   * PATCH /attributes/{key} invalid.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributes03() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      String key = "abc_" + ID.uuid();

      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      UpdateAttributeRequest updateReq = new UpdateAttributeRequest();
      assertUpdateAttributeException(siteId, key, updateReq,
          "{\"message\":\"invalid request body\"}");

      updateReq = new UpdateAttributeRequest().attribute(new UpdateAttribute().type(null));
      assertUpdateAttributeException(siteId, key, updateReq,
          "{\"errors\":[{\"error\":\"Attribute Type, Validation Regex or "
              + "Watermark is required\"}]}");
    }
  }

  /**
   * PATCH /attributes/{key} missing.
   *
   */
  @Test
  public void testUpdateAttributes04() {
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      // given
      setBearerToken(siteId);
      String key = "abc_" + ID.uuid();

      // when
      UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.STANDARD));

      // then
      assertUpdateAttributeException(siteId, key, updateReq,
          "{\"errors\":[{\"error\":\"Attribute not found\"}]}");
    }
  }

  /**
   * PATCH /attributes/{key} watermark.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributes05() throws ApiException {
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      String key = "wm_" + ID.uuid();

      WatermarkPosition position = new WatermarkPosition().xAnchor(WatermarkPositionXAnchor.RIGHT)
          .yAnchor(WatermarkPositionYAnchor.BOTTOM).xOffset(new BigDecimal("1"))
          .yOffset(new BigDecimal("2"));
      Watermark watermark0 = new Watermark().text("mytext").position(position)
          .scale(WatermarkScale.ORIGINAL).rotation(new BigDecimal("45"));
      AddAttributeRequest req = new AddAttributeRequest().attribute(
          new AddAttribute().key(key).dataType(AttributeDataType.WATERMARK).watermark(watermark0));

      // when
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      // then
      assertAttributeEquals(siteId, key, AttributeType.STANDARD, "mytext");

      // given
      position = new WatermarkPosition().xAnchor(WatermarkPositionXAnchor.LEFT)
          .xOffset(new BigDecimal("222")).yOffset(new BigDecimal("111"));

      Watermark watermark1 = new Watermark().text("mytext2").position(position);
      UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(null).watermark(watermark1));

      // when
      new UpdateAttributeRequestBuilder(key).request(updateReq).submitOk(this.client, siteId);

      // then
      Attribute attribute = assertAttributeEquals(siteId, key, AttributeType.STANDARD, "mytext2");
      assertNotNull(attribute.getWatermark());
      assertNotNull(attribute.getWatermark().getPosition());
      WatermarkPosition pos = attribute.getWatermark().getPosition();
      assertEquals(WatermarkPositionXAnchor.LEFT, pos.getxAnchor());
      assertEquals("222.0", Objects.requireNonNull(pos.getxOffset()).toString());
      assertEquals("111.0", Objects.requireNonNull(pos.getyOffset()).toString());
      assertNull(pos.getyAnchor());
    }
  }

  /**
   * PATCH /attributes/{key} validationRegex.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributesValidationRegex() throws ApiException {
    // given
    final String validationRegex = "PO-\\d+";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {
      setBearerToken(siteId);
      String key = "purchaseOrder_" + ID.uuid();
      AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      setBearerToken(siteId + "_govern");

      // when
      updateAttribute(siteId, key);

      // then
      var resp =
          new GetAttributeRequestBuilder(key).submit(client, siteId).throwIfError().response();
      var attribute = resp.getAttribute();
      assertNotNull(attribute);
      assertEquals(key, attribute.getKey());
      assertEquals(validationRegex, attribute.getValidationRegex());
    }
  }

  /**
   * PATCH /attributes/{key} with blank validationRegex removes validationRegex.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateAttributesValidationRegexBlank() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {
      setBearerToken(siteId);
      String key = "purchaseOrder_" + ID.uuid();
      AddAttributeRequest req = new AddAttributeRequest()
          .attribute(new AddAttribute().key(key).validationRegex("PO-\\d+"));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      setBearerToken(siteId + "_govern");
      UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
          .attribute(new UpdateAttribute().type(AttributeType.STANDARD).validationRegex(""));

      // when
      new UpdateAttributeRequestBuilder(key).request(updateReq).submitOk(this.client, siteId);

      // then
      var resp =
          new GetAttributeRequestBuilder(key).submit(client, siteId).throwIfError().response();
      var attribute = resp.getAttribute();
      assertNotNull(attribute);
      assertEquals(key, attribute.getKey());
      assertEquals(AttributeType.STANDARD, attribute.getType());
      assertNull(attribute.getValidationRegex());
    }
  }

  /**
   * PATCH /documents/{documentId} with OPA.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUpdateDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
      setBearerToken(siteId);

      List<SearchResultDocument> documents =
          notNull(new GetFoldersRequestBuilder().indexKey(null).path(null).limit(null).next(null)
              .submitOk(this.client, siteId).response().getDocuments());
      assertEquals(1, documents.size());

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("somepath.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("security").stringValue("other")));

      // when
      try {
        new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}",
            e.getResponseBody());

        documents = notNull(new GetFoldersRequestBuilder().indexKey(null).path(null).limit(null)
            .next(null).submitOk(this.client, siteId).response().getDocuments());
        assertEquals(1, documents.size());
      }
    }
  }

  /**
   * PATCH /documents/{documentId} with OPA and Govern permission.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testUpdateDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      // setBearerToken(siteId);
      setBearerToken(new String[] {siteId, siteId + "_govern"});

      addAttribute(siteId, "security", null, AttributeType.OPA);

      String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

      List<SearchResultDocument> documents =
          notNull(new GetFoldersRequestBuilder().indexKey(null).path(null).limit(null).next(null)
              .submitOk(this.client, siteId).response().getDocuments());
      assertEquals(1, documents.size());

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("somepath.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("security").stringValue("other")));

      // when
      new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
          .submitOk(this.client, siteId);

      // then
      List<DocumentAttribute> documentAttributes = notNull(new GetDocumentAttributesRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response().getAttributes());
      assertEquals(1, documentAttributes.size());
      assertEquals("security", documentAttributes.getFirst().getKey());
      assertEquals("other", documentAttributes.getFirst().getStringValue());
    }
  }

  /**
   * PATCH /documents, POST /documents/{documentId}/attributes, PUT
   * /documents/{documentId}/attributes, existing attribute .
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
      String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);
      assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

      // when - update document with Attribute Type = STANDARD
      new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null),
          new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")))
          .submitOk(this.client, siteId);

      // then
      assertEquals("public", getDocumentAttribute(siteId, documentId, key).getStringValue());

      // given - POST /documents/{documentId}/attributes
      AddDocumentAttributesRequest addReq = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringAttribute(key, "another"));

      // when
      try {
        new AddDocumentAttributeRequestBuilder(
            com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
            .request(addReq).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"security\","
                + "\"error\":\"document attribute 'security' already exists\"}]}",
            e.getResponseBody());
      }

      // give - PUT /documents/{documentId}/attributes
      SetDocumentAttributesRequest setReq =
          new SetDocumentAttributesRequest().addAttributesItem(createStringAttribute(key, "third"));

      // when
      new SetDocumentAttributeRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).request(setReq)
          .submitOk(this.client, siteId);

      // then
      assertEquals("third", getDocumentAttribute(siteId, documentId, key).getStringValue());
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, SITE_ID)) {

      setBearerToken(siteId + "_govern");
      addAttribute(siteId, key, AttributeDataType.STRING, AttributeType.OPA);

      AddDocumentUploadRequest docReq = new AddDocumentUploadRequest()
          .addAttributesItem(createStringAttribute(key, "confidental"));

      // add document
      String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);
      assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

      setBearerToken(siteId);

      // when - update document with Attribute Type = OPA
      try {
        new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null),
            new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")))
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());
      }
    }
  }

  private void updateAttribute(final String siteId, final String key) throws ApiException {
    new UpdateAttributeRequestBuilder(key).setValidationRegex("PO-\\d+").submit(client, siteId)
        .throwIfError();
  }
}
