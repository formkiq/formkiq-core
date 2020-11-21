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

/**
 * Service for keeping track of number of documents.
 *
 */
public interface DocumentCountService {

  /**
   * Get Document Count.
   * 
   * @param siteId {@link String}
   * @return long
   */
  long getDocumentCount(String siteId);

  /**
   * Increment Document Count.
   * 
   * @param siteId {@link String}
   */
  void incrementDocumentCount(String siteId);

  /**
   * Remove Document Count for SiteId.
   * 
   * @param siteId {@link String}
   */
  void removeDocumentCount(String siteId);
}
