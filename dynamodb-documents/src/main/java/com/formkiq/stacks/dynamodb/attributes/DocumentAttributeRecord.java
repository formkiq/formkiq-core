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
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Document Attribute object.
 *
 */
@Reflectable
public class DocumentAttributeRecord implements DynamodbRecord<DocumentAttributeRecord>, DbKeys {

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
  private DocumentAttributeValueType valueType;

  /**
   * constructor.
   */
  public DocumentAttributeRecord() {

  }

  /**
   * Set Boolean Value.
   * 
   * @param value {@link Boolean}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord booleanValue(final Boolean value) {
    this.booleanValue = value;
    return this;
  }

  /**
   * Set Document Id.
   * 
   * @param document {@link String}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord documentId(final String document) {
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
  public DocumentAttributeRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    DocumentAttributeRecord record = null;

    if (!attrs.isEmpty()) {

      record =
          new DocumentAttributeRecord().documentId(ss(attrs, "documentId")).key(ss(attrs, "key"))
              .valueType(DocumentAttributeValueType.valueOf(ss(attrs, "valueType")));

      if (attrs.containsKey("stringValue")) {
        record.stringValue(ss(attrs, "stringValue"));
      }

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
   * @return {@link DocumentAttributeValueType}
   */
  public DocumentAttributeValueType getValueType() {
    return this.valueType;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set Number Value.
   * 
   * @param value {@link Double}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord numberValue(final Double value) {
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
      case STRING:
      case COMPOSITE_STRING:
        sk += this.stringValue;
        break;

      case BOOLEAN:
        sk += this.booleanValue;
        break;

      case NUMBER:
        sk += formatDouble(this.numberValue);
        break;

      case KEY_ONLY:
        break;

      default:
        throw new IllegalArgumentException("Unexpected value: " + this.valueType);
    }

    return sk;
  }

  @Override
  public String skGsi1() {

    String val = switch (this.valueType) {
      case STRING, COMPOSITE_STRING -> this.stringValue;
      case NUMBER -> formatDouble(this.numberValue);
      case BOOLEAN -> this.booleanValue.toString();
      case KEY_ONLY -> "#";
      default -> throw new IllegalArgumentException("Unexpected value: " + this.valueType);
    };

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
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord stringValue(final String value) {
    this.stringValue = value;
    return this;
  }

  /**
   * Set Attribute Type.
   * 
   * @param attributeType {@link DocumentAttributeValueType}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord valueType(final DocumentAttributeValueType attributeType) {
    this.valueType = attributeType;
    return this;
  }

  /**
   * Update Value Type.
   */
  public void updateValueType() {
    this.valueType = DocumentAttributeValueType.KEY_ONLY;

    if (!Strings.isEmpty(this.stringValue)) {
      this.valueType = DocumentAttributeValueType.STRING;
    } else if (this.numberValue != null) {
      this.valueType = DocumentAttributeValueType.NUMBER;
    } else if (this.booleanValue != null) {
      this.valueType = DocumentAttributeValueType.BOOLEAN;
    }
  }
}
