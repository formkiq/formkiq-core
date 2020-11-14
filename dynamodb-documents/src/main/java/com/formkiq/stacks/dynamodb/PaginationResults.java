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

import java.util.List;
import java.util.Map;

/**
 * Pagination Results for a DynamoDB Query.
 *
 * @param <T> Type of Results.
 */
public class PaginationResults<T> {

  /** {@link List}. */
  private List<T> results;
  /** Last Evaluated DynamoDB Key. */
  private PaginationMapToken token;

  /**
   * constructor.
   *
   * @param list {@link List}
   * @param pagination {@link PaginationMapToken}
   */
  public PaginationResults(final List<T> list, final PaginationMapToken pagination) {
    this.results = list;
    this.token = pagination;
  }

  /**
   * Get Results.
   *
   * @return {@link List}
   */
  public List<T> getResults() {
    return this.results;
  }

  /**
   * Get Last Evaluated Key.
   *
   * @return {@link Map}
   */
  public PaginationMapToken getToken() {
    return this.token;
  }
}
