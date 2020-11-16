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
package com.formkiq.lambda.apigateway;

import com.formkiq.graalvm.annotations.Reflectable;

/** Custom Message {@link ApiResponse}. */
@Reflectable
public class ApiMessageResponse implements ApiResponse {

  /** {@link String}. */
  @Reflectable
  private String message;

  /**
   * constructor.
   *
   */
  public ApiMessageResponse() {}

  /**
   * constructor.
   *
   * @param msg {@link String}
   */
  public ApiMessageResponse(final String msg) {
    this.message = msg;
  }

  /**
   * Get Api Response Message.
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

  /**
   * Set Api Response Message.
   * 
   * @param msg {@link String}
   */
  public void setMessage(final String msg) {
    this.message = msg;
  }
}
