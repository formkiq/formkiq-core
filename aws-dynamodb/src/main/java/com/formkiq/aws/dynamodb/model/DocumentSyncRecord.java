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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.DateUtil.getInIso8601Format;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Document Sync Record.
 */
@Reflectable
public class DocumentSyncRecord implements DynamodbRecord<DocumentSyncRecord> {
  /** Syncs SK. */
  private static final String SK_SYNCS = "syncs#";
  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
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

  /**
   * Get Time To Live.
   * 
   * @return Long
   */
  public Long getTimeToLive() {
    return this.timeToLive;
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
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
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
   * Get Sync Date.
   * 
   * @return Date
   */
  public Date getSyncDate() {
    return this.syncDate;
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
   * Get {@link DocumentSyncServiceType}.
   * 
   * @return {@link DocumentSyncServiceType}
   */
  public DocumentSyncServiceType getService() {
    return this.service;
  }

  public DocumentSyncRecord setService(final DocumentSyncServiceType documetSyncServiceType) {
    this.service = documetSyncServiceType;
    return this;
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
   * Get {@link DocumentSyncType}.
   * 
   * @return {@link DocumentSyncType}
   */
  public DocumentSyncType getType() {
    return type;
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
   * Get Create By user id.
   * 
   * @return String
   */
  public String getUserId() {
    return this.userId;
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

  /**
   * Get Document Sync Message.
   * 
   * @return String
   */
  public String getMessage() {
    return this.message;
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

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {
    Map<String, AttributeValue> map = getDataAttributes();

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(SK, fromS(sk()));

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    Map<String, AttributeValue> map = new HashMap<>();

    map.put("documentId", fromS(this.documentId));

    if (this.service != null) {
      map.put("service", fromS(this.service.name()));
    }

    if (this.syncDate != null) {
      map.put("syncDate", AttributeValue.fromS(this.df.format(this.syncDate)));
    }

    map.put("inserteddate", AttributeValue.fromS(this.df.format(this.insertedDate)));
    map.put("userId", fromS(this.userId));

    if (this.status != null) {
      map.put("status", fromS(this.status.name()));
    }

    if (this.type != null) {
      map.put("type", fromS(this.type.name()));
    }

    if (this.timeToLive != null) {
      map.put("TimeToLive", AttributeValue.fromN(String.valueOf(this.timeToLive)));
    }

    if (!isEmpty(message)) {
      map.put("message", fromS(this.message));
    }

    return map;
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

  @Override
  public DocumentSyncRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {
    DocumentSyncRecord record = null;

    if (!attrs.isEmpty()) {
      record = new DocumentSyncRecord().setDocumentId(ss(attrs, "documentId"))
          .setService(DocumentSyncServiceType.valueOf(ss(attrs, "service")))
          .setStatus(DocumentSyncStatus.valueOf(ss(attrs, "status")))
          .setType(DocumentSyncType.valueOf(ss(attrs, "type"))).setUserId(ss(attrs, "userId"))
          .setMessage(ss(attrs, "message")).setSk(ss(attrs, SK));

      if (attrs.containsKey("TimeToLive")) {
        record = record.setTimeToLive(Long.valueOf(attrs.get("TimeToLive").n()));
      }

      record = record.setSyncDate(toDate(attrs, "syncDate"));
      record = record.setInsertedDate(toDate(attrs, "inserteddate"));
    }

    return record;
  }

  private Date toDate(final Map<String, AttributeValue> attrs, final String key) {

    Date returnDate = null;
    String date = ss(attrs, key);

    if (!isEmpty(date)) {
      try {
        returnDate = this.df.parse(date);
      } catch (ParseException e) {
        // ignore
      }
    }

    return returnDate;
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
    if (sk == null) {
      sk = SK_SYNCS
          + getInIso8601Format(this.insertedDate != null ? this.insertedDate : this.syncDate);
    }
    return sk;
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
