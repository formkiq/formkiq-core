/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

import java.util.Date;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/** Holder class for Document(s). */
@Reflectable
public class DocumentItemDynamoDb implements DocumentItem {

  /** Document Id. */
  @Reflectable
  private String documentId;
  /** Document Inserted Date. */
  @Reflectable
  private Date insertedDate;
  /** Document Path. */
  @Reflectable
  private String path;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Content Type. */
  @Reflectable
  private String contentType;
  /** Entity tag. */
  @Reflectable
  private String checksum;
  /** {@link Long}. */
  @Reflectable
  private Long contentLength;
  /** {@link List} {@link DocumentItem}. */
  private List<DocumentItem> documents;
  /** Belongs To Document Id. */
  private String belongsToDocumentId;

  /** constructor. */
  public DocumentItemDynamoDb() {}

  /**
   * constructor.
   *
   * @param docid {@link String}
   * @param date {@link Date}
   * @param username {@link String}
   */
  public DocumentItemDynamoDb(final String docid, final Date date, final String username) {
    this();
    setDocumentId(docid);
    setInsertedDate(date);
    setUserId(username);
  }

  @Override
  public String getChecksum() {
    return this.checksum;
  }

  @Override
  public Long getContentLength() {
    return this.contentLength;
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public String getDocumentId() {
    return this.documentId;
  }

  @Override
  public Date getInsertedDate() {
    return this.insertedDate != null ? (Date) this.insertedDate.clone() : null;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public String getUserId() {
    return this.userId;
  }

  @Override
  public void setChecksum(final String etag) {
    this.checksum = etag;
  }

  @Override
  public void setContentLength(final Long cl) {
    this.contentLength = cl;
  }

  @Override
  public void setContentType(final String ct) {
    this.contentType = ct;
  }

  @Override
  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  @Override
  public void setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
  }

  @Override
  public void setPath(final String filepath) {
    this.path = filepath;
  }

  @Override
  public void setUserId(final String username) {
    this.userId = username;
  }

  @Override
  public String toString() {
    return "documentId=" + this.documentId + ",inserteddate=" + this.insertedDate;
  }

  @Override
  public String getBelongsToDocumentId() {
    return this.belongsToDocumentId;
  }

  @Override
  public List<DocumentItem> getDocuments() {
    return this.documents;
  }

  @Override
  public void setBelongsToDocumentId(final String id) {
    this.belongsToDocumentId = id;
  }

  @Override
  public void setDocuments(final List<DocumentItem> objects) {
    this.documents = objects;
  }
}
