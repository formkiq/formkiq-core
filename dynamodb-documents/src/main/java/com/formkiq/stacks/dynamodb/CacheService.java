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

/**
 * Cache Service.
 *
 */
public interface CacheService {

  /**
   * Get Cache Key Expiry Date.
   * 
   * @param key {@link String}
   * @return {@link Date}
   */
  Date getExpiryDate(String key);

  /**
   * Read Value from Cache.
   * 
   * @param key {@link String}
   * @return {@link String}
   */
  String read(String key);

  /**
   * Write to Cache.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  void write(String key, String value);
}
