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
package com.formkiq.stacks.dynamodb.schemas;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * 
 * Site Schemas Attribute Allowed Value object.
 *
 */
public class SchemaAttributeAllowedValueRecord
    implements DynamodbRecord<SchemaAttributeAllowedValueRecord> {

  /** SK Prefix. */
  public static final String GSI_SK = "val#";
  /** Composite Key. */
  public static final String SK = "attr#";
  /** Attribute Key {@link String}. */
  private String key;
  /** Attribute Key Value {@link String}. */
  private String value;
  /** Document Id. */
  private String documentId;

  /**
   * constructor.
   */
  public SchemaAttributeAllowedValueRecord() {

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
    return Map.of("key", fromS(this.key), "value", fromS(this.value));
  }

  /**
   * Get Document Id.
   * 
   * @return String
   */
  public String getDocumentId() {
    return this.documentId;
  }

  @Override
  public SchemaAttributeAllowedValueRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    if (!attrs.isEmpty()) {
      this.key = attrs.get("key").s();
      this.value = attrs.get("value").s();
    }

    return this;
  }

  /**
   * Get Attribute Key.
   * 
   * @return String
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Attribute Value.
   * 
   * @return String
   */
  public String getValue() {
    return this.value;
  }

  @Override
  public String pk(final String siteId) {

    if (this.documentId != null) {
      return createDatabaseKey(siteId, "schemas#" + this.documentId);
    } else {
      return createDatabaseKey(siteId, "schemas");
    }
  }

  @Override
  public String pkGsi1(final String siteId) {
    return createDatabaseKey(siteId, "attr#" + this.key + "#allowedvalue");
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  /**
   * Set DocumentId.
   * 
   * @param id {@link String}
   * @return SiteSchemaCompositeKeyRecord
   */
  public SchemaAttributeAllowedValueRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Set Attribute Key.
   * 
   * @param attributeKey {@link String}
   * @return SchemaAttributeAllowedValueRecord
   */
  public SchemaAttributeAllowedValueRecord setKey(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set Attribute Value.
   * 
   * @param attributeValue {@link String}
   * @return SchemaAttributeAllowedValueRecord
   */
  public SchemaAttributeAllowedValueRecord setValue(final String attributeValue) {
    this.value = attributeValue;
    return this;
  }

  @Override
  public String sk() {
    if (this.key == null || this.value == null) {
      throw new IllegalArgumentException("'key', 'value' is required");
    }
    return SK + this.key + "#allowedvalue#" + this.value;
  }

  @Override
  public String skGsi1() {
    if (this.value == null) {
      throw new IllegalArgumentException("'value' is required");
    }
    return GSI_SK + value;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
