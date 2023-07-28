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

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;

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
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   */
  PaginationResults<DynamicDocumentItem> findInFolder(String siteId, String indexKey,
      PaginationMapToken token, int maxresults);

  /**
   * Search for Documents.
   *
   * @param siteId Optional Grouping siteId
   * @param search {@link SearchQuery}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   */
  PaginationResults<DynamicDocumentItem> search(String siteId, SearchQuery search,
      PaginationMapToken token, int maxresults);

  /**
   * Search for Document Ids.
   *
   * @param siteId Optional Grouping siteId
   * @param criteria {@link SearchTagCriteria}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link String}
   */
  PaginationResults<String> searchForDocumentIds(String siteId, SearchTagCriteria criteria,
      PaginationMapToken token, int maxresults);
}
