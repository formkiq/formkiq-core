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

  /**
   * constructor.
   */
  public AttributeRecord() {

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

    if (!isEmpty(this.watermarkText)) {
      attr.put("watermarkText", fromS(this.watermarkText));
    }

    if (!isEmpty(this.watermarkImageDocumentId)) {
      attr.put("watermarkImageDocumentId", fromS(this.watermarkImageDocumentId));
    }

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
      record = new AttributeRecord().documentId(ss(attrs, "documentId")).key(ss(attrs, "key"))
          .type(AttributeType.valueOf(ss(attrs, "type")))
          .setWatermarkText(ss(attrs, "watermarkText"))
          .setWatermarkImageDocumentId(ss(attrs, "watermarkImageDocumentId"))
          .dataType(AttributeDataType.valueOf(ss(attrs, "dataType")));
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
