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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Unit Tests for {@link SearchQuery}. */
public class SearchQueryTest {

  /** {@link SearchQuery}. */
  private SearchQuery sq = new SearchQuery();
  /** {@link SearchTagCriteria}. */
  private SearchTagCriteria st = new SearchTagCriteria();

  /** {@link SearchQuery} missing tag. */
  @Test
  public void testIsValid01() {
    // given
    // when
    try {
      this.sq.isValid();
    } catch (Exception e) {
      // then
      assertEquals("'tag' attribute is required.", e.getMessage());
    }
  }

  /** {@link SearchQuery} Tag Name is missing. */
  @Test
  public void testIsValid02() {
    // given
    this.sq.setTag(this.st);

    // when
    try {
      this.sq.isValid();
    } catch (Exception e) {
      // then
      assertEquals("'key' attribute is required.", e.getMessage());
    }
  }

  /** {@link SearchQuery} is valid EQ. */
  @Test
  public void testIsValid03() {
    // given
    this.st.setKey("category");
    this.sq.setTag(this.st);
    this.st.setEq("1");

    // when
    this.sq.isValid();

    // then
  }
}
