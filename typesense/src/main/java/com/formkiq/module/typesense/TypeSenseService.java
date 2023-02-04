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
package com.formkiq.module.typesense;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 
 * Type Sense Service.
 *
 */
public interface TypeSenseService {

  /**
   * Add Collection.
   * 
   * @param siteId {@link String}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  HttpResponse<String> addCollection(String siteId) throws IOException;

  /**
   * Add Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  HttpResponse<String> addDocument(String siteId, String documentId, Map<String, Object> data)
      throws IOException;

  /**
   * Delete Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  HttpResponse<String> deleteDocument(String siteId, String documentId) throws IOException;

  /**
   * Get Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  HttpResponse<String> getDocument(String siteId, String documentId) throws IOException;

  /**
   * Full text search.
   * 
   * @param siteId {@link String}
   * @param text {@link String}
   * @param maxResults int
   * @return {@link List} {@link String}
   * @throws IOException IOException
   */
  List<String> searchFulltext(String siteId, String text, int maxResults) throws IOException;

  /**
   * Add Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param data {@link Map}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  HttpResponse<String> updateDocument(String siteId, String documentId, Map<String, Object> data)
      throws IOException;
}
