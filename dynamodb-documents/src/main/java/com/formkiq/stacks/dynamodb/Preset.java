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
 * Preset.
 *
 */
@Reflectable
public class Preset {

  /** Id. */
  @Reflectable
  private String id;
  /** Name. */
  @Reflectable
  private String name;
  /** Type. */
  @Reflectable
  private String type;
  /** User Id. */
  @Reflectable
  private String userId;
  /** Content Type. */
  @Reflectable
  private Date insertedDate;

  /**
   * constructor.
   */
  public Preset() {}

  /**
   * Get Id.
   * 
   * @return {@link String}
   */
  public String getId() {
    return this.id;
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
   * Get Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get Type.
   * 
   * @return {@link String}
   */
  public String getType() {
    return this.type;
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
   * Set Id.
   * 
   * @param presetId {@link String}
   */
  public void setId(final String presetId) {
    this.id = presetId;
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
   * Set Name.
   * 
   * @param s {@link String}
   */
  public void setName(final String s) {
    this.name = s;
  }

  /**
   * Set Type.
   * 
   * @param presetType {@link String}
   */
  public void setType(final String presetType) {
    this.type = presetType;
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
