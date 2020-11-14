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

/**
 * {@link Preset} with Tags.
 *
 */
@Reflectable
public class PresetTag {

  /** Preset Key. */
  @Reflectable
  private String key;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Content Type. */
  @Reflectable
  private Date insertedDate;

  /**
   * constructor.
   */
  public PresetTag() {

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
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
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
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   */
  public void setInsertedDate(final Date date) {
    this.insertedDate = date != null ? (Date) date.clone() : null;
  }

  /**
   * Set Key.
   * 
   * @param s {@link String}
   */
  public void setKey(final String s) {
    this.key = s;
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
