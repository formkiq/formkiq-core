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
 * Site Schema Attribute Key object.
 *
 */
public class SchemaAttributeKeyRecord implements DynamodbRecord<SchemaAttributeKeyRecord> {

  /** Composite Key. */
  public static final String SK = "attr#";
  /** GSI1_PK. */
  public static final String GSI1_PK = "attr#";
  /** Document Id. */
  private String documentId;
  /** Attribute Key. */
  private String key;

  /**
   * constructor.
   */
  public SchemaAttributeKeyRecord() {

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
   * Set Attribute Key.
   * 
   * @param attributeKey {@link String}
   * @return SchemaAttributeKeyRecord
   */
  public SchemaAttributeKeyRecord setKey(final String attributeKey) {
    this.key = attributeKey;
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
    return Map.of("key", fromS(this.key));
  }

  @Override
  public SchemaAttributeKeyRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {
    return this;
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
    return createDatabaseKey(siteId, GSI1_PK + this.key);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    return SK + this.key;
  }

  @Override
  public String skGsi1() {
    return "true";
  }

  @Override
  public String skGsi2() {
    return null;
  }

  /**
   * Get Document Id.
   * 
   * @return String
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set DocumentId.
   * 
   * @param id {@link String}
   * @return SiteSchemaCompositeKeyRecord
   */
  public SchemaAttributeKeyRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }
}
