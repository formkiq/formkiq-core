/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018-2022 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.aws.services.lambda;

/**
 * 
 * Validation Errors.
 *
 */
public interface ValidationError {

  /**
   * Get Error.
   * 
   * @return {@link String}
   */
  String error();


  /**
   * Get Error Key.
   * 
   * @return {@link String}
   */
  String key();
}
