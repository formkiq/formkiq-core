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

import com.formkiq.graalvm.annotations.Reflectable;

/** Searches for {@link DocumentItem}. */
@Reflectable
public class SearchTagCriteria {

  /** Search Tag Key. */
  @Reflectable
  private String key;
  /** Search Tag Value Equals. */
  @Reflectable
  private String eq;
  /** Search Tag Value Begins With. */
  @Reflectable
  private String beginsWith;

  /** constructor. */
  public SearchTagCriteria() {}

  /**
   * constructor.
   *
   * @param tagKey {@link String}
   */
  public SearchTagCriteria(final String tagKey) {
    this.key = tagKey;
  }

  /**
   * Get Tag Key.
   *
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set Tag Name.
   *
   * @param s {@link String}
   */
  public void setKey(final String s) {
    this.key = s;
  }

  /**
   * Get EQ {@link String}.
   *
   * @return {@link String}
   */
  public String getEq() {
    return this.eq;
  }

  /**
   * Set EQ String.
   *
   * @param s {@link String}
   */
  public void setEq(final String s) {
    this.eq = s;
  }

  /**
   * Is {@link SearchTagCriteria} valid for DynamoDB.
   *
   * @throws InvalidConditionsException InvalidConditionsException
   */
  public void isValid() {
    if (!hasValue(this.key)) {
      throw new InvalidConditionsException("'key' attribute is required.");
    }
  }

  /**
   * {@link String} has value.
   *
   * @param s {@link String}
   * @return boolean
   */
  private boolean hasValue(final String s) {
    return s != null && s.length() > 0;
  }

  /**
   * Get Begins With.
   *
   * @return {@link String}
   */
  public String getBeginsWith() {
    return this.beginsWith;
  }

  /**
   * Set Begins With.
   *
   * @param s {@link String}
   */
  public void setBeginsWith(final String s) {
    this.beginsWith = s;
  }
}
