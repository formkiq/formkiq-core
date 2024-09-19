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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbVersionRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;

/**
 * 
 * Document Attribute object.
 *
 */
@Reflectable
public class DocumentAttributeRecord
    implements DynamodbVersionRecord<DocumentAttributeRecord>, DbKeys {

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Boolean value. */
  private Boolean booleanValue;
  /** Attribute Document Id. */
  private String documentId;
  /** Key of Attribute. */
  private String key;
  /** Number value. */
  private Double numberValue;
  /** String valueAttribute. */
  private String stringValue;
  /** Type of Attribute. */
  private DocumentAttributeValueType valueType;
  /** Inserted Date. */
  private Date insertedDate;
  /** Attribute Document Id. */
  private String userId;

  /**
   * constructor.
   */
  public DocumentAttributeRecord() {}

  /**
   * Get User Id.
   * 
   * @return String
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set User Id.
   * 
   * @param user {@link String}
   * @return DocumentAttributeRecord
   */
  public DocumentAttributeRecord setUserId(final String user) {
    this.userId = user;
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

  @Override
  public Map<String, AttributeValue> getDataAttributes() {

    Map<String, AttributeValue> map = new HashMap<>();
    map.put("key", fromS(this.key));
    map.put("valueType", fromS(this.valueType.name()));
    map.put("documentId", fromS(this.documentId));
    map.put("userId", fromS(this.userId));

    if (this.booleanValue != null) {
      map.put("booleanValue", AttributeValue.fromBool(this.booleanValue));
    }

    if (this.numberValue != null) {
      map.put("numberValue", AttributeValue.fromN(formatDouble(this.numberValue)));
    }

    if (this.stringValue != null) {
      map.put("stringValue", fromS(this.stringValue));
    }

    if (this.insertedDate != null) {
      map.put("inserteddate", AttributeValue.fromS(df.format(this.insertedDate)));
    }

    return map;
  }

  @Override
  public DocumentAttributeRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    DocumentAttributeRecord record = null;

    if (!attrs.isEmpty()) {

      record = new DocumentAttributeRecord().setUserId(ss(attrs, "userId"))
          .setDocumentId(ss(attrs, "documentId")).setKey(ss(attrs, "key"))
          .setValueType(DocumentAttributeValueType.valueOf(ss(attrs, "valueType")));

      if (attrs.containsKey("stringValue")) {
        record.setStringValue(ss(attrs, "stringValue"));
      }

      if (attrs.containsKey("booleanValue")) {
        record.setBooleanValue(bb(attrs, "booleanValue"));
      }

      if (attrs.containsKey("numberValue")) {
        record.setNumberValue(nn(attrs, "numberValue"));
      }

      if (attrs.containsKey("inserteddate")) {
        try {
          record = record.setInsertedDate(df.parse(ss(attrs, "inserteddate")));
        } catch (ParseException e) {
          // ignore
        }
      }

    }

    return record;
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

    sk = getSkValue(sk);

    return truncateSk(sk);
  }

  private String getSkValue(final String sk) {
    String val = sk;
    switch (this.valueType) {
      case STRING, COMPOSITE_STRING, CLASSIFICATION, RELATIONSHIPS -> val += this.stringValue;
      case BOOLEAN -> val += this.booleanValue;
      case NUMBER -> val += formatDouble(this.numberValue);
      case KEY_ONLY, PUBLICATION -> {
      }
      default -> throw new IllegalArgumentException("Unexpected value: " + this.valueType);
    }
    return val;
  }

  @Override
  public String skGsi1() {

    return switch (this.valueType) {
      case STRING, COMPOSITE_STRING, RELATIONSHIPS -> truncateSk(this.stringValue);
      case NUMBER -> formatDouble(this.numberValue);
      case BOOLEAN -> this.booleanValue.toString();
      case CLASSIFICATION, PUBLICATION -> truncateSk(this.stringValue);
      case KEY_ONLY -> "#";
    };

  }

  @Override
  public String skGsi2() {
    return null;
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
   * Set Attribute Type.
   *
   * @param attributeType {@link DocumentAttributeValueType}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setValueType(final DocumentAttributeValueType attributeType) {
    this.valueType = attributeType;
    return this;
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
   * Set Key.
   *
   * @param attributeKey {@link String}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setKey(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Get Document Id.
   *
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   *
   * @param document {@link String}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setDocumentId(final String document) {
    this.documentId = document;
    return this;
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
   * Set String value.
   *
   * @param value {@link String}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setStringValue(final String value) {
    this.stringValue = value;
    return this;
  }

  /**
   * Get Boolean Value.
   *
   * @return {@link Boolean}
   */
  public Boolean getBooleanValue() {
    return this.booleanValue;
  }

  /**
   * Set Boolean Value.
   *
   * @param value {@link Boolean}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setBooleanValue(final Boolean value) {
    this.booleanValue = value;
    return this;
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
   * Set Number Value.
   *
   * @param value {@link Double}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setNumberValue(final Double value) {
    this.numberValue = value;
    return this;
  }

  /**
   * Update Value Type.
   *
   * @return DocumentAttributeRecord
   */
  public DocumentAttributeRecord updateValueType() {
    this.valueType = DocumentAttributeValueType.KEY_ONLY;

    if (!Strings.isEmpty(this.stringValue)) {
      this.valueType = DocumentAttributeValueType.STRING;
    } else if (this.numberValue != null) {
      this.valueType = DocumentAttributeValueType.NUMBER;
    } else if (this.booleanValue != null) {
      this.valueType = DocumentAttributeValueType.BOOLEAN;
    }

    return this;
  }

  @Override
  public String skVersion() {
    String sk = ATTR + this.key + "#" + this.df.format(getInsertedDate()) + "#";
    return getSkValue(sk);
  }

  /**
   * Get Inserted Date.
   *
   * @return {@link Date}
   */
  public Date getInsertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   *
   * @param date {@link Date}
   * @return {@link DocumentAttributeRecord}
   */
  public DocumentAttributeRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }
}
