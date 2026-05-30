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
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * Classification Record.
 */
public class ClassificationRecord implements DynamodbRecord<ClassificationRecord> {

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Name of Schema. */
  private String name;
  /** Schema {@link String}. */
  private String schema;
  /** Document Id. */
  private String documentId;
  /** User Id. */
  private String userId;
  /** Record inserted date. */
  private Date insertedDate;

  /**
   * constructor.
   */
  public ClassificationRecord() {}

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
    return Map.of("name", fromS(this.name), "documentId", fromS(this.documentId), "schema",
        fromS(this.schema), "userId", fromS(this.userId), "inserteddate",
        AttributeValue.fromS(df.format(this.insertedDate)));
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
  public ClassificationRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    ClassificationRecord record = null;

    if (!attrs.isEmpty()) {
      record = new ClassificationRecord().setName(ss(attrs, "name"))
          .setDocumentId(ss(attrs, "documentId")).setSchema(ss(attrs, "schema"))
          .setUserId(ss(attrs, "userId"));

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

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date getInsertedDate() {
    return this.insertedDate;
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
   * Get User Id.
   * 
   * @return {@link String}
   */
  public String getUserId() {
    return this.userId;
  }

  @Override
  public String pk(final String siteId) {

    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, "schemas#" + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    return createDatabaseKey(siteId, "class#document");
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  /**
   * Set Document Id.
   * 
   * @param classificationId {@link String}
   * @return ClassificationRecord
   */
  public ClassificationRecord setDocumentId(final String classificationId) {
    this.documentId = classificationId;
    return this;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return {@link ClassificationRecord}
   */
  public ClassificationRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Set Name.
   *
   * @param classificationName {@link String}
   * @return {@link ClassificationRecord}
   */
  public ClassificationRecord setName(final String classificationName) {
    this.name = classificationName;
    return this;
  }

  /**
   * Set Schema.
   *
   * @param siteSchema {@link String}
   * @return {@link ClassificationRecord}
   */
  public ClassificationRecord setSchema(final String siteSchema) {
    this.schema = siteSchema;
    return this;
  }

  /**
   * Set User.
   * 
   * @param user {@link String}
   * @return {@link ClassificationRecord}
   */
  public ClassificationRecord setUserId(final String user) {
    this.userId = user;
    return this;
  }

  @Override
  public String sk() {
    return "class#document";
  }

  @Override
  public String skGsi1() {
    if (this.name == null) {
      throw new IllegalArgumentException("'name' is required");
    }
    return "attr#" + this.name;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
