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
package com.formkiq.stacks.dynamodb.documents;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * Document Publish Record.
 */
public class DocumentPublishRecord implements DynamodbRecord<DocumentPublishRecord> {

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Attribute Document Id. */
  private String documentId;
  /** Inserted Date. */
  private Date insertedDate;
  /** User Id. */
  private String userId;
  /** S3 Version. */
  private String s3version;
  /** Content Type. */
  private String contentType;
  /** Path. */
  private String path;

  /**
   * constructor.
   */
  public DocumentPublishRecord() {}

  /**
   * Get Path.
   * 
   * @return String
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Set Path.
   * 
   * @param documentPath {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setPath(final String documentPath) {
    this.path = documentPath;
    return this;
  }

  /**
   * Get Content Type.
   * 
   * @return String
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Set Content Type.
   * 
   * @param documentContentType {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setContentType(final String documentContentType) {
    this.contentType = documentContentType;
    return this;
  }

  /**
   * Get S3 Version.
   * 
   * @return String
   */
  public String getS3version() {
    return this.s3version;
  }

  /**
   * Set S3 Version.
   * 
   * @param version {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setS3version(final String version) {
    this.s3version = version;
    return this;
  }

  public String getDocumentId() {
    return documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Get Inserted Date.
   * 
   * @return Date
   */
  public Date getInsertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

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
   * @return DocumentPublishRecord
   */
  public DocumentPublishRecord setUserId(final String user) {
    this.userId = user;
    return this;
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

    Map<String, AttributeValue> map = new HashMap<>();
    map.put("documentId", fromS(this.documentId));
    map.put("userId", fromS(this.userId));
    map.put("s3version", fromS(this.s3version));
    map.put("contentType", fromS(this.contentType));
    map.put("path", fromS(this.path));

    if (this.insertedDate != null) {
      map.put("inserteddate", AttributeValue.fromS(df.format(this.insertedDate)));
    }

    return map;
  }

  @Override
  public DocumentPublishRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    DocumentPublishRecord record = null;

    if (!attrs.isEmpty()) {

      record = new DocumentPublishRecord().setUserId(ss(attrs, "userId"))
          .setS3version(ss(attrs, "s3version")).setContentType(ss(attrs, "contentType"))
          .setDocumentId(ss(attrs, "documentId")).setPath(ss(attrs, "path"));

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
    return null;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    return "publish";
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
