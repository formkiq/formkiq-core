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

import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchResponseFields;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationException;

/**
 * 
 * Document Search Service.
 *
 */
public interface DocumentSearchService {

  /**
   * Search for Documents in Folder.
   * 
   * @param siteId {@link String}
   * @param indexKey {@link String}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link DynamicDocumentItem}
   */
  Pagination<DynamicDocumentItem> findInFolder(String siteId, String indexKey, String nextToken,
      int maxresults);

  /**
   * Search for Documents.
   *
   * @param siteId Optional Grouping siteId
   * @param search {@link SearchQuery}
   * @param searchResponseFields {@link SearchResponseFields}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link DynamicDocumentItem}
   * @throws ValidationException ValidationException
   */
  Pagination<DynamicDocumentItem> search(String siteId, SearchQuery search,
      SearchResponseFields searchResponseFields, String nextToken, int maxresults)
      throws ValidationException;

  /**
   * Search for Document Ids.
   *
   * @param siteId Optional Grouping siteId
   * @param criteria {@link SearchTagCriteria}
   * @param nextToken {@link String}
   * @param maxresults int
   * @return {@link Pagination} {@link String}
   */
  Pagination<String> searchForDocumentIds(String siteId, SearchTagCriteria criteria,
      String nextToken, int maxresults);
}
