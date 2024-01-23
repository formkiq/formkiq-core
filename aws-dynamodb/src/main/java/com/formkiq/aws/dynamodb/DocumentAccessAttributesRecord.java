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
package com.formkiq.aws.dynamodb;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Document Access Attributes Record.
 */
public class DocumentAccessAttributesRecord
    implements DynamodbRecord<DocumentAccessAttributesRecord>, DbKeys {

  /** Boolean Access Attributes. */
  private static final String PREFIX_BOOLEAN = "aab#";
  /** Number Access Attributes. */
  private static final String PREFIX_NUMBER = "aan#";
  /** String Access Attributes. */
  private static final String PREFIX_STRING = "aas#";
  /** Access Attributes. */
  private Map<String, Object> attributes;
  /** DocumentId. */
  private String documentId;

  /**
   * constructor.
   */
  public DocumentAccessAttributesRecord() {
    this.attributes = new HashMap<>();
  }

  /**
   * Add Boolean Value.
   * 
   * @param key {@link String}
   * @param booleanValue {@link Boolean}
   * @return {@link DocumentAccessAttributesRecord}
   */
  public DocumentAccessAttributesRecord addBooleanValue(final String key,
      final Boolean booleanValue) {
    this.attributes.put(key, booleanValue);
    return this;
  }

  /**
   * Add Number Value.
   * 
   * @param key {@link String}
   * @param numberValue {@link Double}
   * @return {@link DocumentAccessAttributesRecord}
   */
  public DocumentAccessAttributesRecord addNumberValue(final String key, final Double numberValue) {
    this.attributes.put(key, numberValue);
    return this;
  }

  /**
   * Add {@link String} Value.
   * 
   * @param key {@link String}
   * @param stringValue {@link String}
   * @return {@link DocumentAccessAttributesRecord}
   */
  public DocumentAccessAttributesRecord addStringValue(final String key, final String stringValue) {
    this.attributes.put(key, stringValue);
    return this;
  }

  /**
   * Set Access Attributes.
   * 
   * @param accessAttributes {@link Map}
   * @return {@link DocumentAccessAttributesRecord}
   */
  public DocumentAccessAttributesRecord attributes(final Map<String, Object> accessAttributes) {
    this.attributes = accessAttributes;
    return this;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return {@link DocumentAccessAttributesRecord}
   */
  public DocumentAccessAttributesRecord documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get Access Attributes.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> attrs = new HashMap<>(Map.of(DbKeys.PK, fromS(pk(siteId)),
        DbKeys.SK, fromS(sk()), "documentId", fromS(this.documentId)));

    for (Map.Entry<String, Object> e : this.attributes.entrySet()) {

      String key = e.getKey();

      if (e.getValue() instanceof Double) {
        attrs.put(PREFIX_NUMBER + key, AttributeValue.fromN(((Double) e.getValue()).toString()));
      } else if (e.getValue() instanceof Boolean) {
        attrs.put(PREFIX_BOOLEAN + key, AttributeValue.fromBool((Boolean) e.getValue()));
      } else {
        attrs.put(PREFIX_STRING + key, AttributeValue.fromS((String) e.getValue()));
      }
    }

    return attrs;
  }

  /**
   * Get DocumentId.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  @Override
  public DocumentAccessAttributesRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    DocumentAccessAttributesRecord record =
        new DocumentAccessAttributesRecord().documentId(ss(attrs, "documentId"));

    for (Map.Entry<String, AttributeValue> e : attrs.entrySet()) {
      String key = e.getKey();

      if (key.startsWith(PREFIX_BOOLEAN)) {
        record.addBooleanValue(key.substring(PREFIX_BOOLEAN.length()), e.getValue().bool());
      } else if (key.startsWith(PREFIX_NUMBER)) {
        record.addNumberValue(key.substring(PREFIX_NUMBER.length()),
            Double.valueOf(e.getValue().n()));
      } else if (key.startsWith(PREFIX_STRING)) {
        record.addStringValue(key.substring(PREFIX_STRING.length()), e.getValue().s());
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
    return null;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  /**
   * Remove Attribute Key.
   * 
   * @param key {@link String}
   */
  public void removeAttribute(final String key) {
    this.attributes.remove(key);
  }

  @Override
  public String sk() {
    return "accessAttributes";
  }

  @Override
  public String skGsi1() {
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
