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
package com.formkiq.stacks.dynamodb;

import java.util.Map;

/**
 * Interceptor for {@link DocumentService}.
 */
public interface DocumentServiceInterceptor {

  /**
   * Save Document Interceptor.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param current {@link Map}
   * @param previous {@link Map}
   */
  void saveDocument(String siteId, String documentId, Map<String, Object> current,
      Map<String, Object> previous);

  /**
   * Save Document Attributes Interceptor.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param current {@link Map}
   * @param previous {@link Map}
   */
  void saveDocumentAttribute(String siteId, String documentId, Map<String, Object> current,
      Map<String, Object> previous);

  /**
   * Delete Document Interceptor.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param softDelete boolean
   * @param current {@link Map}
   */
  void deleteDocument(String siteId, String documentId, boolean softDelete,
      Map<String, Object> current);

  /**
   * Delete Document Attribute Interceptor.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param softDelete boolean
   * @param current {@link Map}
   */
  void deleteDocumentAttribute(String siteId, String documentId, boolean softDelete,
      Map<String, Object> current);

  /**
   * Restore Soft Deleted Document Inteerceptor.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param current {@link Map}
   */
  void restoreSoftDeletedDocument(String siteId, String documentId, Map<String, Object> current);
}
