/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.plugins.useractivity;

import java.util.Map;

/**
 * User Activity.
 */
public interface UserActivityPlugin {

  /**
   * Add User Activity View.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param versionKey {@link String}
   */
  void addDocumentViewActivity(String siteId, String documentId, String versionKey);

  /**
   * Add Document Activity.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param record {@link Map}
   */
  void addDocumentActivity(String siteId, String documentId, Map<String, Object> record);

  /**
   * Add update Document Activity.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param record {@link Map}
   */
  void updateDocumentActivity(String siteId, String documentId, Map<String, Object> record);

  /**
   * Add delete Document Activity.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteDocumentActivity(String siteId, String documentId);


  /**
   * Add soft delete Document Activity.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteSoftDocumentActivity(String siteId, String documentId);

  /**
   * Restore Soft Delete Document Activity.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void restoreSoftDeletedDocumentActivity(String siteId, String documentId);
}
