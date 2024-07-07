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
package com.formkiq.aws.dynamodb.model;

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
 * Mapping object.
 *
 */
@Reflectable
public class MappingRecord implements DynamodbRecord<MappingRecord> {

  /** Prefix PK. */
  public static final String PREFIX_PK = "mappings#";
  /** Gsi1 SK Prefix. */
  public static final String PREFIX_SK_GSI1 = "mapping#";
  /** SK Prefix. */
  public static final String PREFIX_SK = "site#document#";
  /** Mapping Attributes {@link String}. */
  private String attributes;
  /** Description of Mapping. */
  private String description;
  /** Mapping Id. */
  private String documentId;
  /** Name of Mapping. */
  private String name;

  /**
   * constructor.
   */
  public MappingRecord() {

  }

  /**
   * Get Attributes.
   * 
   * @return {@link String}
   */
  public String getAttributes() {
    return this.attributes;
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
    Map<String, AttributeValue> map = new HashMap<>(Map.of("documentId", fromS(this.documentId),
        "name", fromS(this.name), "attributes", fromS(this.attributes)));

    if (!isEmpty(this.description)) {
      map.put("description", fromS(this.description));
    }

    return map;
  }

  /**
   * Get Description.
   * 
   * @return {@link String}
   */
  public String getDescription() {
    return this.description;
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
  public MappingRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    MappingRecord record = null;

    if (!attrs.isEmpty()) {
      record = new MappingRecord().setDocumentId(ss(attrs, "documentId")).setName(ss(attrs, "name"))
          .setDescription(ss(attrs, "description")).setAttributes(ss(attrs, "attributes"));
    }

    return record;
  }

  /**
   * Get Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return this.name;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, PREFIX_PK + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    return createDatabaseKey(siteId, PREFIX_PK);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  /**
   * Set Attributes.
   * 
   * @param mappingAttributes {@link String}
   * @return {@link MappingRecord}
   */
  public MappingRecord setAttributes(final String mappingAttributes) {
    this.attributes = mappingAttributes;
    return this;
  }

  /**
   * Set Description.
   * 
   * @param mappingDescription {@link String}
   * @return {@link MappingRecord}
   */
  public MappingRecord setDescription(final String mappingDescription) {
    this.description = mappingDescription;
    return this;
  }

  /**
   * Set Document Id.
   * 
   * @param mappingId {@link String}
   * @return {@link MappingRecord}
   */
  public MappingRecord setDocumentId(final String mappingId) {
    this.documentId = mappingId;
    return this;
  }

  /**
   * Set Name.
   * 
   * @param schemaName {@link String}
   * @return {@link MappingRecord}
   */
  public MappingRecord setName(final String schemaName) {
    this.name = schemaName;
    return this;
  }

  @Override
  public String sk() {
    return "mapping";
  }

  @Override
  public String skGsi1() {
    if (this.name == null) {
      throw new IllegalArgumentException("'name' is required");
    }
    return PREFIX_SK_GSI1 + this.name;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
