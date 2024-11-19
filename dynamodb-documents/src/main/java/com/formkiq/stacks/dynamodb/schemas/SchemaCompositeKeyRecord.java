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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.ID;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Site Schemas object.
 *
 */
public class SchemaCompositeKeyRecord implements DynamodbRecord<SchemaCompositeKeyRecord> {

  /** SK Prefix. */
  public static final String GSI_SK = "key#";
  /** Composite Key. */
  public static final String SK = "compositeKey#";

  /** {@link List} {@link String}. */
  private List<String> keys;
  /** Document Id. */
  private String documentId;

  /**
   * constructor.
   */
  public SchemaCompositeKeyRecord() {

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
    return Map.of("keys", AttributeValue.fromL(this.keys.stream().map(this::fromS).toList()));
  }

  @Override
  public SchemaCompositeKeyRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    if (!attrs.isEmpty()) {
      this.keys = attrs.get("keys").l().stream().map(AttributeValue::s).toList();
    }

    return this;
  }

  /**
   * Get Keys.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getKeys() {
    return this.keys;
  }

  /**
   * Set Composite Keys.
   * 
   * @param compositeKeys {@link List} {@link String}
   * @return {@link SchemaCompositeKeyRecord}
   */
  public SchemaCompositeKeyRecord keys(final List<String> compositeKeys) {
    this.keys = compositeKeys;
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
    return createDatabaseKey(siteId, "schemas#compositeKey");
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    return SK + ID.uuid();
  }

  @Override
  public String skGsi1() {
    if (this.keys == null) {
      throw new IllegalArgumentException("'keys' is required");
    }

    List<String> s = new ArrayList<>(this.keys);
    Collections.sort(s);
    return GSI_SK + String.join(DbKeys.COMPOSITE_KEY_DELIM, s) + "#";
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
  public SchemaCompositeKeyRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }
}
