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
package com.formkiq.lambda.apigateway.exception;

/** {@link Exception} that will return a 404 error. */
public class NotFoundException extends Exception {

  /** serialVersionUID. */
  private static final long serialVersionUID = -3307625920614270509L;

  /**
   * constructor.
   *
   * @param msg {@link String}
   */
  public NotFoundException(final String msg) {
    super(msg);
  }
}
