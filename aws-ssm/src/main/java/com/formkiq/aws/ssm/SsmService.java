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
package com.formkiq.aws.ssm;

/**
 * 
 * SSM Services.
 *
 */
public interface SsmService {

  /**
   * Get Parameter Store Value.
   *
   * @param key {@link String}
   * @return {@link String}
   */
  String getParameterValue(String key);

  /**
   * Put SSM Parameter.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  void putParameter(String key, String value);

  /**
   * Remove Parameter.
   * 
   * @param key {@link String}
   */
  void removeParameter(String key);
}
