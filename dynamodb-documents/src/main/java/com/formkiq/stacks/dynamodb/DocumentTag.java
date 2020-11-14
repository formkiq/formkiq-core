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
import com.formkiq.graalvm.annotations.Reflectable;

/** Document Tag. */
@Reflectable
public class DocumentTag {

  /** Document Tag Key. */
  @Reflectable
  private String key;
  /** Document Id. */
  @Reflectable
  private String documentId;
  /** Document String Tag Value. */
  @Reflectable
  private String value;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Document Inserted Date. */
  @Reflectable
  private Date insertedDate;
  /** {@link DocumentTagType}. */
  @Reflectable
  private DocumentTagType type;

  /** constructor. */
  public DocumentTag() {}

  /**
   * constructor.
   *
   * @param docid {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @param date {@link Date}
   * @param user {@link String}
   */
  public DocumentTag(final String docid, final String tagKey, final String tagValue,
      final Date date, final String user) {
    this(docid, tagKey, tagValue, date, user, DocumentTagType.USERDEFINED);
  }

  /**
   * constructor.
   *
   * @param docid {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @param date {@link Date}
   * @param user {@link String}
   * @param tagType {@link DocumentTagType}
   */
  public DocumentTag(final String docid, final String tagKey, final String tagValue,
      final Date date, final String user, final DocumentTagType tagType) {
    this();
    setDocumentId(docid);
    setKey(tagKey);
    setValue(tagValue);
    setInsertedDate(date);
    setUserId(user);
    setType(tagType);
  }

  /**
   * get Document Id.
   *
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set Document ID.
   *
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  /**
   * Get Document Tag Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Tag Key.
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
   * Get {@link DocumentTagType}.
   * 
   * @return {@link DocumentTagType}
   */
  public DocumentTagType getType() {
    return this.type;
  }

  /**
   * Set Document Type.
   * 
   * @param tagType {@link DocumentTagType}
   */
  public void setType(final DocumentTagType tagType) {
    this.type = tagType;
  }
}
