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
package com.formkiq.stacks.dynamodb.attributes;

import static com.formkiq.aws.dynamodb.AttributeValueHelper.addEnumIfNotNull;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.addNumberIfNotEmpty;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.addStringIfNotEmpty;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toDoubleValue;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toEnumValue;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toStringValue;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Attribute object.
 *
 */
@Reflectable
public class AttributeRecord implements DynamodbRecord<AttributeRecord> {

  /** Attribute constant. */
  public static final String ATTR = "attr#";
  /** {@link AttributeDataType}. */
  private AttributeDataType dataType;
  /** Queue Document Id. */
  private String documentId;
  /** Key of Attribute. */
  private String key;
  /** Type of Attribute. */
  private AttributeType type;
  /** Watermark Text. */
  private String watermarkText;
  /** Watermark Image DocumentId. */
  private String watermarkImageDocumentId;
  /** Watermark X Anchor. */
  private WatermarkXanchor watermarkxAnchor;
  /** Watermark Y Anchor. */
  private WatermarkYanchor watermarkyAnchor;
  /** Watermark X Offset. */
  private Double watermarkxOffset;
  /** Watermark Y Offset. */
  private Double watermarkyOffset;
  /** Watermark Y Offset. */
  private Double watermarkRotation;
  /** {@link WatermarkScale}. */
  private WatermarkScale watermarkScale;

  /**
   * constructor.
   */
  public AttributeRecord() {

  }

  /**
   * Get Watermark Rotation.
   * 
   * @return Double
   */
  public Double getWatermarkRotation() {
    return this.watermarkRotation;
  }

  /**
   * Set Watermark Rotation.
   * 
   * @param rotation Double
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkRotation(final Double rotation) {
    this.watermarkRotation = rotation;
    return this;
  }

  /**
   * Get {@link WatermarkScale}.
   * 
   * @return {@link WatermarkScale}
   */
  public WatermarkScale getWatermarkScale() {
    return this.watermarkScale;
  }

  /**
   * Set {@link WatermarkScale}.
   * 
   * @param scale {@link WatermarkScale}
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkScale(final WatermarkScale scale) {
    this.watermarkScale = scale;
    return this;
  }

  /**
   * Get Watermark X Anchor.
   * 
   * @return WatermarkXanchor
   */
  public WatermarkXanchor getWatermarkxAnchor() {
    return this.watermarkxAnchor;
  }

  /**
   * Set Watermark X Anchor.
   *
   * @param anchor {@link String}
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkxAnchor(final WatermarkXanchor anchor) {
    this.watermarkxAnchor = anchor;
    return this;
  }

  /**
   * Get Watermark Y Anchor.
   * 
   * @return String
   */
  public WatermarkYanchor getWatermarkyAnchor() {
    return this.watermarkyAnchor;
  }

  /**
   * Set Watermark Y Anchor.
   *
   * @param anchor {@link String}
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkyAnchor(final WatermarkYanchor anchor) {
    this.watermarkyAnchor = anchor;
    return this;
  }

  /**
   * Get Watermark X Offset.
   * 
   * @return float
   */
  public Double getWatermarkxOffset() {
    return this.watermarkxOffset;
  }

  /**
   * Set Watermark X Offset.
   *
   * @param offset float
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkxOffset(final Double offset) {
    this.watermarkxOffset = offset;
    return this;
  }

  /**
   * Get Watermark Y Offset.
   * 
   * @return float
   */
  public Double getWatermarkyOffset() {
    return this.watermarkyOffset;
  }

  /**
   * Set Watermark Y Offset.
   *
   * @param offset float
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkyOffset(final Double offset) {
    this.watermarkyOffset = offset;
    return this;
  }

  /**
   * Get Watermark Image Document Id.
   * 
   * @return String
   */
  public String getWatermarkImageDocumentId() {
    return this.watermarkImageDocumentId;
  }

  public AttributeRecord setWatermarkImageDocumentId(final String imageDocumentId) {
    this.watermarkImageDocumentId = imageDocumentId;
    return this;
  }

  /**
   * Get Watermark Text.
   * 
   * @return String
   */
  public String getWatermarkText() {
    return this.watermarkText;
  }

  /**
   * Set Watermark Text.
   * 
   * @param text {@link String}
   * @return AttributeRecord
   */
  public AttributeRecord setWatermarkText(final String text) {
    this.watermarkText = text;
    return this;
  }

  /**
   * Set {@link AttributeDataType}.
   * 
   * @param attributeDataType {@link AttributeDataType}
   * @return {@link AttributeRecord}
   */
  public AttributeRecord dataType(final AttributeDataType attributeDataType) {
    this.dataType = attributeDataType;
    return this;
  }

  /**
   * Set Document Id.
   * 
   * @param document {@link String}
   * @return {@link AttributeRecord}
   */
  public AttributeRecord documentId(final String document) {
    this.documentId = document;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = new HashMap<>(getDataAttributes());

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));
    map.put(DbKeys.GSI1_PK, fromS(pkGsi1(siteId)));
    map.put(DbKeys.GSI1_SK, fromS(skGsi1()));

    String pkGsi2 = pkGsi2(siteId);
    if (!isEmpty(pkGsi2)) {
      map.put(DbKeys.GSI2_PK, fromS(pkGsi2));
      map.put(DbKeys.GSI2_SK, fromS(skGsi2()));
    }

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    Map<String, AttributeValue> attr =
        new HashMap<>(Map.of("documentId", fromS(this.documentId), "dataType",
            fromS(this.dataType.name()), "type", fromS(this.type.name()), "key", fromS(this.key)));

    addEnumIfNotNull(attr, "watermarkScale", this.watermarkScale);
    addStringIfNotEmpty(attr, "watermarkText", this.watermarkText);
    addStringIfNotEmpty(attr, "watermarkImageDocumentId", this.watermarkImageDocumentId);
    addEnumIfNotNull(attr, "watermarkxAnchor", this.watermarkxAnchor);
    addEnumIfNotNull(attr, "watermarkyAnchor", this.watermarkyAnchor);
    addNumberIfNotEmpty(attr, "watermarkxOffset", this.watermarkxOffset);
    addNumberIfNotEmpty(attr, "watermarkyOffset", this.watermarkyOffset);
    addNumberIfNotEmpty(attr, "watermarkRotation", this.watermarkRotation);

    return attr;
  }

  /**
   * Get {@link AttributeDataType}.
   * 
   * @return {@link AttributeDataType}
   */
  public AttributeDataType getDataType() {
    return this.dataType;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  @Override
  public AttributeRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    AttributeRecord record = null;

    if (!attrs.isEmpty()) {
      record = new AttributeRecord().documentId(toStringValue(attrs, "documentId"))
          .key(toStringValue(attrs, "key")).type(toEnumValue(attrs, AttributeType.class, "type"))
          .setWatermarkText(toStringValue(attrs, "watermarkText"))
          .setWatermarkImageDocumentId(toStringValue(attrs, "watermarkImageDocumentId"))
          .setWatermarkxAnchor(toEnumValue(attrs, WatermarkXanchor.class, "watermarkxAnchor"))
          .setWatermarkyAnchor(toEnumValue(attrs, WatermarkYanchor.class, "watermarkyAnchor"))
          .setWatermarkxOffset(toDoubleValue(attrs, "watermarkxOffset"))
          .setWatermarkRotation(toDoubleValue(attrs, "watermarkRotation"))
          .setWatermarkyOffset(toDoubleValue(attrs, "watermarkyOffset"))
          .setWatermarkScale(toEnumValue(attrs, WatermarkScale.class, "watermarkScale"))
          .dataType(toEnumValue(attrs, AttributeDataType.class, "dataType"));
    }

    return record;
  }

  /**
   * Get Attribute Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Attribute Type.
   * 
   * @return {@link AttributeType}
   */
  public AttributeType getType() {
    return this.type;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return {@link AttributeRecord}
   */
  public AttributeRecord key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, ATTR + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    return createDatabaseKey(siteId, ATTR);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return !isEmpty(this.watermarkText) ? createDatabaseKey(siteId, ATTR) : null;
  }

  @Override
  public String sk() {
    return "attribute";
  }

  @Override
  public String skGsi1() {
    if (this.key == null) {
      throw new IllegalArgumentException("'key' is required");
    }
    return ATTR + this.key;
  }

  @Override
  public String skGsi2() {
    return !isEmpty(this.watermarkText) ? ATTR + this.dataType : null;
  }

  /**
   * Set Attribute Type.
   * 
   * @param attributeType {@link AttributeType}
   * @return {@link AttributeRecord}
   */
  public AttributeRecord type(final AttributeType attributeType) {
    this.type = attributeType;
    return this;
  }
}
