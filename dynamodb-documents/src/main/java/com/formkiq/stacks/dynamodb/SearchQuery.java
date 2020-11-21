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

import com.formkiq.graalvm.annotations.Reflectable;

/** Search Query. */
@Reflectable
public class SearchQuery {

  /** {@link SearchTagCriteria}. */
  @Reflectable
  private SearchTagCriteria tag;

  /** constructor. */
  public SearchQuery() {}

  /**
   * Get {@link SearchTagCriteria}.
   *
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria getTag() {
    return this.tag;
  }

  /**
   * Set {@link SearchTagCriteria}.
   *
   * @param searchtag {@link SearchTagCriteria}
   */
  public void setTag(final SearchTagCriteria searchtag) {
    this.tag = searchtag;
  }

  /** Is {@link SearchQuery} object valid. */
  public void isValid() {
    if (this.tag == null) {
      throw new InvalidConditionsException("'tag' attribute is required.");
    }

    isSearchTagValid();
  }

  /** Is {@link SearchTagCriteria} valid. */
  private void isSearchTagValid() {
    this.tag.isValid();
  }
}
