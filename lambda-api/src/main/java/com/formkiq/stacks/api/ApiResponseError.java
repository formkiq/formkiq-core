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

/** Error Response. */
@Reflectable
public class ApiResponseError implements ApiResponse {

  /** Error Message. */
  @Reflectable
  private String message;

  /**
   * constructor.
   *
   * @param s {@link String}
   */
  public ApiResponseError(final String s) {
    this.message = s;
  }

  /**
   * Get {@link ApiResponse} Message.
   *
   * @return {@link String}
   */
  public String getMessage() {
    return this.message;
  }

  @Override
  public String getNext() {
    return null;
  }

  @Override
  public String getPrevious() {
    return null;
  }
}
