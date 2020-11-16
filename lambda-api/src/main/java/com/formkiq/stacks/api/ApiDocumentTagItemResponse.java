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
package com.formkiq.stacks.api;

import java.util.Date;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.lambda.apigateway.ApiResponse;

/** API Response Object. */
@Reflectable
public class ApiDocumentTagItemResponse implements ApiResponse {

  /** Document Tag Key. */
  @Reflectable
  private String key;
  /** Document String Tag Value. */
  @Reflectable
  private String value;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Document Inserted Date. */
  @Reflectable
  private Date insertedDate;
  /** Tag Type. */
  @Reflectable
  private String type;
  /** Document Id. */
  @Reflectable
  private String documentId;

  /** constructor. */
  public ApiDocumentTagItemResponse() {}

  @Override
  public String getNext() {
    return null;
  }

  @Override
  public String getPrevious() {
    return null;
  }

  /**
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Key.
   * 
   * @param tagkey {@link String}
   */
  public void setKey(final String tagkey) {
    this.key = tagkey;
  }

  /**
   * Get Value.
   * 
   * @return {@link String}
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Set Value.
   * 
   * @param s {@link String}
   */
  public void setValue(final String s) {
    this.value = s;
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
   * Set User Id.
   *
   * @param user {@link String}
   */
  public void setUserId(final String user) {
    this.userId = user;
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
   * Set Inserted Date.
   *
   * @param date {@link Date}
   */
  public void setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
  }

  /**
   * Get Tag Type.
   * 
   * @return {@link String}
   */
  public String getType() {
    return this.type;
  }

  /**
   * Set Tag Type.
   * 
   * @param tagtype {@link String}
   */
  public void setType(final String tagtype) {
    this.type = tagtype;
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
   * Set Document Id.
   * 
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }
}
