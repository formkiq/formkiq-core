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
import com.formkiq.lambda.apigateway.ApiResponse;

/** /sites {@link ApiResponse}. */
@Reflectable
public class Site {

  /** Site Id. */
  @Reflectable
  private String siteId;
  /** Upload Email. */
  @Reflectable
  private String uploadEmail;

  /**
   * Get Site Id.
   * 
   * @return {@link String}
   */
  public String getSiteId() {
    return this.siteId;
  }

  /**
   * Set Site Id.
   * 
   * @param site {@link String}
   */
  public void setSiteId(final String site) {
    this.siteId = site;
  }

  /**
   * Get Upload Email.
   * 
   * @return {@link String}
   */
  public String getUploadEmail() {
    return this.uploadEmail;
  }

  /**
   * Set Upload Email.
   * 
   * @param email {@link String}
   */
  public void setUploadEmail(final String email) {
    this.uploadEmail = email;
  }
}
