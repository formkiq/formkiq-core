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
  /** Time to Live. */
  @Reflectable
  private String timeToLive;

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
  public String getBelongsToDocumentId() {
    return this.belongsToDocumentId;
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
  public List<DocumentItem> getDocuments() {
    return this.documents;
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
  public String getTimeToLive() {
    return this.timeToLive;
  }

  @Override
  public String getUserId() {
    return this.userId;
  }

  @Override
  public void setBelongsToDocumentId(final String id) {
    this.belongsToDocumentId = id;
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
  public void setDocuments(final List<DocumentItem> objects) {
    this.documents = objects;
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
  public void setTimeToLive(final String ttl) {
    this.timeToLive = ttl;
  }

  @Override
  public void setUserId(final String username) {
    this.userId = username;
  }

  @Override
  public String toString() {
    return "documentId=" + this.documentId + ",inserteddate=" + this.insertedDate;
  }
}
