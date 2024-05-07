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
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Site Schemas object.
 *
 */
public class SitesSchemaRecord implements DynamodbRecord<SitesSchemaRecord> {

  /** SK Prefix. */
  public static final String PREFIX_SK = "site#document#";
  /** Name of Schema. */
  private String name;
  /** Schema {@link String}. */
  private String schema;
  /** Schema Version. */
  private Integer version;

  /**
   * constructor.
   */
  public SitesSchemaRecord() {

  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = new HashMap<>(getDataAttributes());

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    return Map.of("name", fromS(this.name), "schema", fromS(this.schema), "version",
        AttributeValue.fromN("" + this.version));
  }

  @Override
  public SitesSchemaRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    SitesSchemaRecord record = null;

    if (!attrs.isEmpty()) {
      record = new SitesSchemaRecord().name(ss(attrs, "name")).schema(ss(attrs, "schema"))
          .version(Integer.valueOf(toInt(attrs, "version")));
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

  /**
   * Get Schema.
   * 
   * @return {@link String}
   */
  public String getSchema() {
    return this.schema;
  }

  /**
   * Set Name.
   * 
   * @param schemaName {@link String}
   * @return {@link SitesSchemaRecord}
   */
  public SitesSchemaRecord name(final String schemaName) {
    this.name = schemaName;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    return createDatabaseKey(siteId, "schemas#");
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
   * Set Schema.
   * 
   * @param siteSchema {@link String}
   * @return {@link SitesSchemaRecord}
   */
  public SitesSchemaRecord schema(final String siteSchema) {
    this.schema = siteSchema;
    return this;
  }

  @Override
  public String sk() {
    if (this.version == null) {
      throw new IllegalArgumentException("'version' is required");
    }
    return PREFIX_SK + "v"
        + Objects.formatInt(this.version.intValue(), Objects.INT_ZERO_PAD_FORMAT);
  }

  @Override
  public String skGsi1() {
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
  }

  /**
   * Get Version.
   * 
   * @return {@link Integer}
   */
  public Integer getVersion() {
    return this.version;
  }

  /**
   * Set Version.
   * 
   * @param schemaVersion {@link Integer}
   * @return {@link SitesSchemaRecord}
   */
  public SitesSchemaRecord version(final Integer schemaVersion) {
    this.version = schemaVersion;
    return this;
  }
}
