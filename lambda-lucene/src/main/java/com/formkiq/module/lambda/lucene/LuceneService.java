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
package com.formkiq.module.lambda.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * 
 * Lucene Service.
 *
 */
public interface LuceneService {

  /**
   * Add Document to Lucene Index.
   * 
   * @param siteId {@link String}
   * @param document {@link Document}
   * @throws IOException IOException
   */
  void addDocument(String siteId, Document document) throws IOException;

  /**
   * Add Document to Lucene Index.
   * 
   * @param siteId {@link String}
   * @param documents {@link Collection} {@link Document}
   * @throws IOException IOException
   */
  void addDocuments(String siteId, Collection<Document> documents) throws IOException;

  /**
   * Delete Documents.
   * 
   * @param siteId {@link String}
   * @param term {@link Term}
   * @throws IOException IOException
   */
  void deleteDocuments(String siteId, Term term) throws IOException;

  /**
   * Find by Tag.
   * 
   * @param siteId {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @return {@link List} {@link Document}
   * @throws IOException IOException
   */
  List<Document> findByTag(String siteId, String tagKey, String tagValue) throws IOException;

  /**
   * Find By Terms.
   * 
   * @param siteId {@link String}
   * @param key {@link String}
   * @param value {@link String}
   * @return {@link List} {@link Document}
   * @throws IOException IOException
   */
  List<Document> findByTerms(String siteId, String key, String value) throws IOException;

  /**
   * Find Document.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link Document}
   * @throws IOException IOException
   */
  Document findDocument(String siteId, String documentId) throws IOException;

  /**
   * Full text search.
   * 
   * @param siteId {@link String}
   * @param text {@link String}
   * @param maxResults int
   * @return {@link List} {@link Document}
   * @throws IOException IOException
   * @throws ParseException ParseException
   */
  List<Document> searchFulltext(String siteId, String text, int maxResults)
      throws IOException, ParseException;

  /**
   * Update Document.
   * 
   * @param siteId {@link String}
   * @param term {@link Term}
   * @param document {@link Document}
   * @throws IOException IOException
   */
  void updateDocument(String siteId, Term term, Document document) throws IOException;
}
