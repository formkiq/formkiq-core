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

import com.formkiq.graalvm.annotations.Reflectable;

/** API Document Version Object. */
@Reflectable
public class ApiDocumentVersion {

  /** Document VersionId. */
  @Reflectable
  private String versionId;
  /** Last Modified Date. */
  @Reflectable
  private String lastModifiedDate;

  /** constructor. */
  public ApiDocumentVersion() {}

  /**
   * Get Last Modified Date.
   * 
   * @return {@link String}
   */
  public String getLastModifiedDate() {
    return this.lastModifiedDate;
  }

  /**
   * Get Version Id.
   * 
   * @return {@link String}
   */
  public String getVersionId() {
    return this.versionId;
  }

  /**
   * Set Last Modified Date.
   * 
   * @param date {@link String}
   */
  public void setLastModifiedDate(final String date) {
    this.lastModifiedDate = date;
  }

  /**
   * Set Version Id.
   * 
   * @param version {@link String}
   */
  public void setVersionId(final String version) {
    this.versionId = version;
  }

}
