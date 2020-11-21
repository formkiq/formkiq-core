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
package com.formkiq.stacks.api;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.InvalidConditionsException;
import com.formkiq.stacks.dynamodb.SearchQuery;

/** Query Request. */
@Reflectable
public class QueryRequest {

  /** {@link SearchQuery}. */
  @Reflectable
  private SearchQuery query;

  /** constructor. */
  public QueryRequest() {}

  /**
   * Get {@link SearchQuery}.
   *
   * @return {@link SearchQuery}
   */
  public SearchQuery getQuery() {
    return this.query;
  }

  /**
   * Set Query.
   *
   * @param q {@link SearchQuery}
   */
  public void setQuery(final SearchQuery q) {
    this.query = q;
  }

  /** Is {@link QueryRequest} object valid. */
  public void isValid() {
    if (this.query == null) {
      throw new InvalidConditionsException("'query' field is required.");
    }

    this.query.isValid();
  }
}
