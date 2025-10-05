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
package com.formkiq.aws.dynamodb.model;

import com.formkiq.graalvm.annotations.Reflectable;

/** Searches for {@link DocumentItem}. */
@Reflectable
public class SearchTagCriteriaRange {

  /** End. */
  private String end;
  /** Start. */
  private String start;
  /** Type. */
  private String type;

  /** constructor. */
  public SearchTagCriteriaRange() {}

  /**
   * Set End Range.
   * 
   * @param rangeEnd {@link String}
   * @return {@link SearchTagCriteriaRange}
   */
  public SearchTagCriteriaRange end(final String rangeEnd) {
    this.end = rangeEnd;
    return this;
  }

  /**
   * Get End Range.
   * 
   * @return {@link String}
   */
  public String getEnd() {
    return this.end;
  }

  /**
   * Get Start Range.
   * 
   * @return {@link String}
   */
  public String getStart() {
    return this.start;
  }

  /**
   * Get Range Type.
   * 
   * @return {@link String}
   */
  public String getType() {
    return this.type;
  }

  /**
   * Set Start Range.
   * 
   * @param rangeStart {@link String}
   * @return {@link SearchTagCriteriaRange}
   */
  public SearchTagCriteriaRange start(final String rangeStart) {
    this.start = rangeStart;
    return this;
  }

  /**
   * Set Range Type.
   * 
   * @param rangeType {@link String}
   * @return {@link SearchTagCriteriaRange}
   */
  public SearchTagCriteriaRange type(final String rangeType) {
    this.type = rangeType;
    return this;
  }
}
