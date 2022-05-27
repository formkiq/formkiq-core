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

import java.util.Date;
import java.util.List;
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
  /** Document Tag Values. */
  @Reflectable
  private List<String> values;
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
   * constructor.
   *
   * @param docid {@link String}
   * @param tagKey {@link String}
   * @param tagValues {@link List} {@link String}
   * @param date {@link Date}
   * @param user {@link String}
   * @param tagType {@link DocumentTagType}
   */
  public DocumentTag(final String docid, final String tagKey, final List<String> tagValues,
      final Date date, final String user, final DocumentTagType tagType) {
    this();
    setDocumentId(docid);
    setKey(tagKey);
    setValues(tagValues);
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
   * @return {@link DocumentTag}
   */
  public DocumentTag setDocumentId(final String id) {
    this.documentId = id;
    return this;
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
   * @return {@link DocumentTag}
   */
  public DocumentTag setKey(final String tagkey) {
    this.key = tagkey;
    return this;
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
   * @return {@link DocumentTag}
   */
  public DocumentTag setValue(final String s) {
    this.value = s;
    return this;
  }
  
  /**
   * Get Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getValues() {
    return this.values;
  }

  /**
   * Set Values.
   * 
   * @param list {@link List} {@link String}
   * @return {@link DocumentTag}
   */
  public DocumentTag setValues(final List<String> list) {
    this.values = list;
    return this;
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
   * @return {@link DocumentTag} 
   */
  public DocumentTag setUserId(final String user) {
    this.userId = user;
    return this;
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
   * @return {@link DocumentTag}
   */
  public DocumentTag setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
    return this;
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
   * @return {@link DocumentTag}
   */
  public DocumentTag setType(final DocumentTagType tagType) {
    this.type = tagType;
    return this;
  }
}
