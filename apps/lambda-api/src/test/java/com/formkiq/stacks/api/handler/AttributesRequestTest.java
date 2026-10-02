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
import com.formkiq.testutils.api.attributes.DeleteAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateAttribute;
import com.formkiq.client.model.UpdateAttributeRequest;
import com.formkiq.client.model.Watermark;
import com.formkiq.client.model.WatermarkPosition;
import com.formkiq.client.model.WatermarkPositionXAnchor;
import com.formkiq.client.model.WatermarkPositionYAnchor;
import com.formkiq.client.model.WatermarkScale;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributesRequestBuilder;
import com.formkiq.testutils.api.attributes.UpdateAttributeRequestBuilder;
import com.formkiq.testutils.api.schemas.SetSitesSchemaRequestBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for attribute definitions. */
public class AttributesRequestTest extends AbstractAttributesRequestTest {

  /**
   * POST /attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes01(final String siteId) throws ApiException {
    // given

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

    attribute =
        new GetAttributeRequestBuilder(key).submitOk(this.client, siteId).response().getAttribute();
    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals(AttributeType.STANDARD, attribute.getType());
    assertEquals(AttributeDataType.STRING, attribute.getDataType());

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"key\"," + "\"error\":\"attribute '" + key
            + "' already exists\"}]}");
  }

  /**
   * POST /attributes missing key.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes02(final String siteId) {
    // given

    setBearerToken(siteId);
    AddAttributeRequest req = new AddAttributeRequest();

    // when

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"message\":\"invalid request body\"}");

    // given
    req = new AddAttributeRequest().attribute(new AddAttribute());

    // when

    // when
    var errorResponse2 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"key\",\"error\":\"'key' is required\"}]}");
  }

  /**
   * POST /attributes reserved key.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes03(final String siteId) {
    // given

    setBearerToken(siteId);
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key("publication"));

    // when

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"key\","
            + "\"error\":\"'publication' is a reserved attribute name\"}]}");
  }

  /**
   * POST /attributes watermark.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes04(final String siteId) throws ApiException {
    // given
    final String text = "sample text";
    final String attributeKey = "wm1";

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

  /**
   * POST /attributes watermark missing watermark text.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes05(final String siteId) {
    // given

    setBearerToken(siteId);
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key("wm2").dataType(AttributeDataType.WATERMARK));

    // when

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"watermark\",\"error\":\"'watermark.text' "
            + "or 'watermark.imageDocumentId' is required\"}]}");
  }

  /**
   * POST /attributes watermark on wrong DataType.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes06(final String siteId) throws ApiException {
    // given
    final String attributeKey = "wm2";

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

  /**
   * POST /attributes watermark image.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes07(final String siteId) throws ApiException {
    // given
    final String attributeKey = "wm1";

    setBearerToken(siteId);
    Watermark watermark = new Watermark().imageDocumentId(ID.uuid());
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute()
        .key(attributeKey).dataType(AttributeDataType.WATERMARK).watermark(watermark));

    // when

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"watermark.imageDocumentId\","
            + "\"error\":\"watermark.imageDocumentId' does not exist\"}]}");

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

  /**
   * POST /attributes add watermark text, then try and change to invalid documentid.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes08(final String siteId) throws ApiException {
    // given
    final String attributeKey = "wm1";

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

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"key\",\"error\":\"attribute 'wm1' already exists\"},"
            + "{\"key\":\"watermark.imageDocumentId\","
            + "\"error\":\"watermark.imageDocumentId' does not exist\"}]}");
    r = new GetAttributeRequestBuilder(attributeKey).submitOk(this.client, siteId).response();
    assertNotNull(r);
    List<Attribute> list = notNull(new GetAttributesRequestBuilder().next(null)
        .submitOk(this.client, siteId).response().getAttributes());
    assertEquals(1, list.size());
    assertEquals("wm1", list.getFirst().getKey());
  }

  /**
   * POST /attributes with DATE data type.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributesDate01(final String siteId) throws ApiException {
    // given

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

  /**
   * POST /attributes watermark with font size.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributesFontSize(final String siteId) throws ApiException {
    // given
    final String text = "sample text";
    final String attributeKey = "wm1";

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

  /**
   * POST /attributes with validationRegex.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributesValidationRegex(final String siteId) throws ApiException {
    // given
    final String validationRegex = "INV-\\d+";

    setBearerToken(siteId);
    String key = "invoice_" + ID.uuid();

    // when
    AddResponse response = addAttribute(siteId, key);

    // then
    assertEquals("Attribute '" + key + "' created", response.getMessage());

    var resp = new GetAttributeRequestBuilder(key).submitOk(client, siteId).response();
    var attribute = resp.getAttribute();
    assertNotNull(attribute);

    assertEquals(key, attribute.getKey());
    assertEquals(validationRegex, attribute.getValidationRegex());

    var attributes = notNull(
        new GetAttributesRequestBuilder().submitOk(this.client, siteId).response().getAttributes());
    var o = attributes.stream().filter(a -> key.equals(a.getKey())).findFirst();
    assertTrue(o.isPresent());
    assertEquals(validationRegex, o.get().getValidationRegex());
  }

  /**
   * DELETE /attributes.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteAttributes01(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    // when
    DeleteResponse response =
        new DeleteAttributeRequestBuilder(key).submitOk(this.client, siteId).response();

    // then
    assertEquals("Attribute 'security' deleted", response.getMessage());
  }

  /**
   * DELETE /attributes missing attribute.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteAttributes02(final String siteId) {
    // given
    final String key = "security";

    setBearerToken(siteId);

    // when

    // when
    var errorResponse1 = new DeleteAttributeRequestBuilder(key).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"key\",\"error\":\"attribute 'key' not found\"}]}");
  }

  /**
   * DELETE /attributes in use.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteAttributes03(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId);
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "public"));

    new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId);

    // when

    // when
    var errorResponse1 = new DeleteAttributeRequestBuilder(key).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute 'security' is in use, cannot be deleted\"}]}");
  }

  /**
   * PUT /sites/{siteId}/schema/document with required attribute and then attempt to delete
   * attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("schemaSites")
  public void testDeleteAttributes04(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String key = "category";
    AddAttributeRequest areq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    new AddAttributeRequestBuilder().request(areq).submitOk(this.client, siteId);

    SetSitesSchemaRequest req =
        new SetSitesSchemaRequest().name("joe").attributes(new SetSchemaAttributes()
            .addRequiredItem(new AddAttributeSchemaRequired().attributeKey(key)));

    new SetSitesSchemaRequestBuilder().withSetSitesSchemaRequest(req).submitOk(this.client, siteId);

    // when

    // when
    var errorResponse1 = new DeleteAttributeRequestBuilder(key).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"category\","
            + "\"error\":\"attribute 'category' is used in a Schema / "
            + "Classification, cannot be deleted\"}]}");
  }

  /**
   * POST /sites/{siteId}/classifications with required attribute and then attempt to delete
   * attribute.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("schemaSites")
  public void testDeleteAttributes05(final String siteId) throws ApiException {
    // given

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

    // when
    var errorResponse1 = new DeleteAttributeRequestBuilder(key).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"category\","
            + "\"error\":\"attribute 'category' is used in a Schema / Classification, "
            + "cannot be deleted\"}]}");
  }

  /**
   * GET /attributes/{key} preserves JSON data type and attribute type.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetAttributeJson(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);

    String key = "invoiceDetails";
    String permissionSiteId = siteId != null ? siteId : DEFAULT_SITE_ID;
    setBearerToken(new String[] {permissionSiteId, permissionSiteId + "_govern"});
    new AddAttributeRequestBuilder().keyAsJson(key).submitOk(this.client, siteId);
    setBearerToken(siteId);

    // when
    Attribute attribute =
        new GetAttributeRequestBuilder(key).submitOk(this.client, siteId).response().getAttribute();

    // then
    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals(AttributeDataType.JSON, attribute.getDataType());
  }

  /**
   * GET /attributes lists JSON definitions alongside scalar definitions.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testGetAttributesJson(final String siteId) throws ApiException {
    // given

    String jsonKey = "invoiceDetails";
    String stringKey = "invoiceStatus";
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsJson(jsonKey).submitOk(this.client, siteId);
    new AddAttributeRequestBuilder().keyAsString(stringKey).submitOk(this.client, siteId);

    // when
    List<Attribute> attributes =
        new GetAttributesRequestBuilder().submitOk(this.client, siteId).response().getAttributes();

    // then
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    Attribute jsonAttribute =
        attributes.stream().filter(a -> jsonKey.equals(a.getKey())).findFirst().orElseThrow();
    assertEquals(AttributeDataType.JSON, jsonAttribute.getDataType());
    assertEquals(AttributeType.STANDARD, jsonAttribute.getType());
    Attribute stringAttribute =
        attributes.stream().filter(a -> stringKey.equals(a.getKey())).findFirst().orElseThrow();
    assertEquals(AttributeDataType.STRING, stringAttribute.getDataType());
    assertEquals(AttributeType.STANDARD, stringAttribute.getType());
  }

  /**
   * PATCH /attributes/{key} invalid.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUpdateAttributes03(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    String key = "abc_" + ID.uuid();

    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    UpdateAttributeRequest updateReq = new UpdateAttributeRequest();

    // when
    var errorResponse1 =
        new UpdateAttributeRequestBuilder(key).request(updateReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"message\":\"invalid request body\"}");

    updateReq = new UpdateAttributeRequest().attribute(new UpdateAttribute().type(null));

    // when
    var errorResponse2 =
        new UpdateAttributeRequestBuilder(key).request(updateReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"error\":\"Attribute Type, Validation Regex or "
            + "Watermark is required\"}]}");
  }

  /**
   * PATCH /attributes/{key} missing.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUpdateAttributes04(final String siteId) {
    // given
    setBearerToken(siteId);
    String key = "abc_" + ID.uuid();

    // when
    UpdateAttributeRequest updateReq =
        new UpdateAttributeRequest().attribute(new UpdateAttribute().type(AttributeType.STANDARD));

    // then

    // when
    var errorResponse1 =
        new UpdateAttributeRequestBuilder(key).request(updateReq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"error\":\"Attribute not found\"}]}");
  }

  /**
   * PATCH /attributes/{key} watermark.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUpdateAttributes05(final String siteId) throws ApiException {
    // given

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
