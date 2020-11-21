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

/** {@link RuntimeException} for Invalid DynamoDB conditions. */
public class InvalidConditionsException extends RuntimeException {

  /** serialVersionUID. */
  private static final long serialVersionUID = -8473837091233747713L;

  /**
   * constructor.
   *
   * @param message {@link String}
   */
  public InvalidConditionsException(final String message) {
    super(message);
  }
}
