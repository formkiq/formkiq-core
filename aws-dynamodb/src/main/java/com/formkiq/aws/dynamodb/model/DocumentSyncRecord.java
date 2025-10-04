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

import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.AttributeValueHelper.addDateIfNotNull;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.addEnumIfNotNull;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.addNumberIfNotEmpty;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.addStringIfNotEmpty;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toDateValue;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toEnumValue;
import static com.formkiq.aws.dynamodb.AttributeValueHelper.toStringValue;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.DateUtil.getInIso8601Format;

/**
 * Document Sync Record.
 */
@Reflectable
public class DocumentSyncRecord implements DynamodbRecord<DocumentSyncRecord> {
  /** Syncs SK. */
  public static final String SK_SYNCS = "syncs#";
  /** Document Id. */
  private String documentId;
  /** Record Sync date. */
  private Date syncDate;
  /** Record inserted date. */
  private Date insertedDate;
  /** {@link DocumentSyncServiceType}. */
  private DocumentSyncServiceType service;
  /** {@link DocumentSyncStatus}. */
  private DocumentSyncStatus status;
  /** {@link DocumentSyncType}. */
  private DocumentSyncType type;
  /** Create by UserId. */
  private String userId;
  /** Sync Message. */
  private String message;
  /** Time To Live. */
  private Long timeToLive;
  /** SK. */
  private String sk;

  /**
   * constructor.
   */
  public DocumentSyncRecord() {}

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {
    Map<String, AttributeValue> map = getDataAttributes();

    map.put(PK, fromS(pk(siteId)));
    map.put(SK, fromS(sk()));

    if (DocumentSyncStatus.FAILED.equals(status)) {
      map.put(GSI1_PK, fromS(pkGsi1(siteId)));
      map.put(GSI1_SK, fromS(skGsi1()));
    }

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    Map<String, AttributeValue> map = new HashMap<>();

    addStringIfNotEmpty(map, "documentId", this.documentId);
    addEnumIfNotNull(map, "service", this.service);
    addDateIfNotNull(map, "syncDate", this.syncDate);
    addDateIfNotNull(map, "inserteddate", this.insertedDate);
    addStringIfNotEmpty(map, "userId", this.userId);
    addEnumIfNotNull(map, "status", this.status);
    addEnumIfNotNull(map, "type", this.type);
    addNumberIfNotEmpty(map, "TimeToLive", this.timeToLive);
    addStringIfNotEmpty(map, "message", this.message);

    return map;
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
  public DocumentSyncRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {
    DocumentSyncRecord record = null;

    if (!attrs.isEmpty()) {
      record = new DocumentSyncRecord().setDocumentId(toStringValue(attrs, "documentId"))
          .setService(toEnumValue(attrs, DocumentSyncServiceType.class, "service"))
          .setStatus(toEnumValue(attrs, DocumentSyncStatus.class, "status"))
          .setType(toEnumValue(attrs, DocumentSyncType.class, "type"))
          .setUserId(toStringValue(attrs, "userId")).setMessage(toStringValue(attrs, "message"))
          .setSk(toStringValue(attrs, SK));

      if (attrs.containsKey("TimeToLive")) {
        record = record.setTimeToLive(Long.valueOf(attrs.get("TimeToLive").n()));
      }

      record = record.setSyncDate(toDateValue(attrs, "syncDate"));
      record = record.setInsertedDate(toDateValue(attrs, "inserteddate"));
    }

    return record;
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
   * Get Document Sync Message.
   * 
   * @return String
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Get {@link DocumentSyncServiceType}.
   * 
   * @return {@link DocumentSyncServiceType}
   */
  public DocumentSyncServiceType getService() {
    return this.service;
  }

  /**
   * Get Sync Status.
   * 
   * @return DocumentSyncStatus
   */
  public DocumentSyncStatus getStatus() {
    return this.status;
  }

  /**
   * Get Sync Date.
   * 
   * @return Date
   */
  public Date getSyncDate() {
    return this.syncDate;
  }

  /**
   * Get Time To Live.
   * 
   * @return Long
   */
  public Long getTimeToLive() {
    return this.timeToLive;
  }

  /**
   * Get {@link DocumentSyncType}.
   * 
   * @return {@link DocumentSyncType}
   */
  public DocumentSyncType getType() {
    return type;
  }

  /**
   * Get Create By user id.
   * 
   * @return String
   */
  public String getUserId() {
    return this.userId;
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
    if (this.service == null || this.status == null) {
      throw new IllegalArgumentException("'service', 'status' is required");
    }
    return createDatabaseKey(siteId, "doc#syncs#" + this.service + "#" + this.status);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Set Sync Message.
   * 
   * @param syncMessage {@link String}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setMessage(final String syncMessage) {
    this.message = syncMessage;
    return this;
  }

  public DocumentSyncRecord setService(final DocumentSyncServiceType documetSyncServiceType) {
    this.service = documetSyncServiceType;
    return this;
  }

  /**
   * Set Sk.
   * 
   * @param skValue {@link String}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setSk(final String skValue) {
    this.sk = skValue;
    return this;
  }

  /**
   * Set {@link DocumentSyncStatus}.
   * 
   * @param documentSyncStatus {@link DocumentSyncStatus}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setStatus(final DocumentSyncStatus documentSyncStatus) {
    this.status = documentSyncStatus;
    return this;
  }

  /**
   * Set Sync Date.
   * 
   * @param date {@link Date}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setSyncDate(final Date date) {
    this.syncDate = date;
    return this;
  }

  /**
   * Set Time To Live.
   * 
   * @param ttl Long
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setTimeToLive(final Long ttl) {
    this.timeToLive = ttl;
    return this;
  }

  /**
   * Set Document Sync Type.
   * 
   * @param documentSyncType {@link DocumentSyncType}.
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setType(final DocumentSyncType documentSyncType) {
    this.type = documentSyncType;
    return this;
  }

  /**
   * Set UserId.
   * 
   * @param documentSyncUserId {@link String}
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setUserId(final String documentSyncUserId) {
    this.userId = documentSyncUserId;
    return this;
  }

  @Override
  public String sk() {
    if (sk == null) {
      sk = SK_SYNCS
          + getInIso8601Format(this.insertedDate != null ? this.insertedDate : this.syncDate);
    }
    return sk;
  }

  @Override
  public String skGsi1() {
    return SK_SYNCS + this.type + "#" + getInIso8601Format(this.insertedDate) + "#"
        + this.documentId;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
