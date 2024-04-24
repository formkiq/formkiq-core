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
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Attribute Search object.
 *
 */
@Reflectable
public class AttributeSearchRecord implements DynamodbRecord<AttributeSearchRecord>, DbKeys {

  /** Boolean value. */
  @Reflectable
  private Boolean booleanValue;
  /** Queue Document Id. */
  @Reflectable
  private String documentId;
  /** Key of Attribute. */
  @Reflectable
  private String key;
  /** Number value. */
  @Reflectable
  private Double numberValue;
  /** String valueAttribute. */
  @Reflectable
  private String stringValue;
  /** Type of Attribute. */
  @Reflectable
  private AttributeSearchValueType valueType;

  /**
   * constructor.
   */
  public AttributeSearchRecord() {

  }

  /**
   * Set Boolean Value.
   * 
   * @param value {@link Boolean}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord booleanValue(final Boolean value) {
    this.booleanValue = value;
    return this;
  }

  /**
   * Set Document Id.
   * 
   * @param document {@link String}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord documentId(final String document) {
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

    return map;
  }

  /**
   * Get Boolean Value.
   * 
   * @return {@link Boolean}
   */
  public Boolean getBooleanValue() {
    return this.booleanValue;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {

    Map<String, AttributeValue> map = new HashMap<>();
    map.put("key", fromS(this.key));
    map.put("valueType", fromS(this.valueType.name()));
    map.put("documentId", fromS(this.documentId));

    if (this.booleanValue != null) {
      map.put("booleanValue", AttributeValue.fromBool(this.booleanValue));
    }

    if (this.numberValue != null) {
      map.put("numberValue", AttributeValue.fromN(formatDouble(this.numberValue)));
    }

    if (this.stringValue != null) {
      map.put("stringValue", fromS(this.stringValue));
    }

    return map;
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
  public AttributeSearchRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    AttributeSearchRecord record = null;

    if (!attrs.isEmpty()) {

      record = new AttributeSearchRecord().documentId(ss(attrs, "documentId")).key(ss(attrs, "key"))
          .valueType(AttributeSearchValueType.valueOf(ss(attrs, "valueType")));

      if (attrs.containsKey("booleanValue")) {
        record.booleanValue(bb(attrs, "booleanValue"));
      }

      if (attrs.containsKey("numberValue")) {
        record.numberValue(nn(attrs, "numberValue"));
      }
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
   * Get Number Value.
   * 
   * @return {@link Double}
   */
  public Double getNumberValue() {
    return this.numberValue;
  }

  /**
   * Get String value.
   * 
   * @return {@link String}
   */
  public String getStringValue() {
    return this.stringValue;
  }

  /**
   * Get Attribute Value Type.
   * 
   * @return {@link AttributeSearchValueType}
   */
  public AttributeSearchValueType getValueType() {
    return this.valueType;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set Number Value.
   * 
   * @param value {@link Double}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord numberValue(final Double value) {
    this.numberValue = value;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, PREFIX_DOCS + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    if (this.key == null) {
      throw new IllegalArgumentException("'key' is required");
    }
    return createDatabaseKey(siteId, PREFIX_DOCS + ATTR + this.key);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {

    if (this.key == null) {
      throw new IllegalArgumentException("'key' is required");
    }

    String sk = ATTR + this.key + "#";

    switch (this.valueType) {
      case STRING: {
        sk += this.stringValue;
        break;
      }
      case BOOLEAN: {
        sk += this.booleanValue;
        break;
      }
      case NUMBER: {
        sk += formatDouble(this.numberValue);
        break;
      }
      default:
        throw new IllegalArgumentException("Unexpected value: " + this.valueType);
    }

    return sk;
  }

  @Override
  public String skGsi1() {

    String val = null;
    switch (this.valueType) {
      case STRING: {
        val = this.stringValue;
        break;
      }
      case NUMBER: {
        val = formatDouble(this.numberValue);
        break;
      }
      case BOOLEAN: {
        val = this.booleanValue.toString();
        break;
      }
      default:
        throw new IllegalArgumentException("Unexpected value: " + this.valueType);
    }

    return val;
  }

  @Override
  public String skGsi2() {
    return null;
  }

  /**
   * Set String value.
   * 
   * @param value {@link String}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord stringValue(final String value) {
    this.stringValue = value;
    return this;
  }

  /**
   * Set Attribute Type.
   * 
   * @param attributeType {@link AttributeSearchValueType}
   * @return {@link AttributeSearchRecord}
   */
  public AttributeSearchRecord valueType(final AttributeSearchValueType attributeType) {
    this.valueType = attributeType;
    return this;
  }
}
