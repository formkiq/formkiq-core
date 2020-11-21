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
 * 
 * Document Search Service.
 *
 */
public interface DocumentSearchService {

  /**
   * Search for Documents.
   *
   * @param siteId Optional Grouping siteId
   * @param search {@link SearchTagCriteria}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   */
  PaginationResults<DynamicDocumentItem> search(String siteId, SearchTagCriteria search,
      PaginationMapToken token, int maxresults);
}
