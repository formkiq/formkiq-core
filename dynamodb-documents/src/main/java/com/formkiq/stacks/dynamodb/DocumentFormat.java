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

/**
 * Document Formats.
 *
 */
public class DocumentFormat {

  /** Document Id. */
  private String documentId;
  /** Document Inserted Date. */
  private Date insertedDate;
  /** User Id. */
  private String userId;
  /** Content Type. */
  private String contentType;

  /**
   * constructor.
   */
  public DocumentFormat() {}

  /**
   * Get Content-Type.
   * 
   * @return {@link String}
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date getInsertedDate() {
    return this.insertedDate != null ? (Date) this.insertedDate.clone() : null;
  }

  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set Content-Type.
   * 
   * @param ct {@link String}
   */
  public void setContentType(final String ct) {
    this.contentType = ct;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   */
  public void setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
  }

  /**
   * Set User Id.
   * 
   * @param user {@link String}
   */
  public void setUserId(final String user) {
    this.userId = user;
  }
}
