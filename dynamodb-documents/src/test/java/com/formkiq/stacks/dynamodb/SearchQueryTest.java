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
