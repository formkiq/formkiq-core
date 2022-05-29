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

import java.util.Collection;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.graalvm.annotations.Reflectable;

/** Searches for {@link DocumentItem}. */
@Reflectable
public class SearchTagCriteria {

  /** Search Tag Key. */
  @Reflectable
  private String key;
  /** Search Tag Value Equals. */
  @Reflectable
  private String eq;
  /** Search Tag Values May Equal. */
  @Reflectable
  private Collection<String> eqOr;
  /** Search Tag Value Begins With. */
  @Reflectable
  private String beginsWith;

  /** constructor. */
  public SearchTagCriteria() {}

  /**
   * constructor.
   *
   * @param tagKey {@link String}
   */
  public SearchTagCriteria(final String tagKey) {
    this.key = tagKey;
  }

  /**
   * Get Begins With.
   *
   * @return {@link String}
   */
  public String beginsWith() {
    return this.beginsWith;
  }

  /**
   * Set Begins With.
   *
   * @param s {@link String}
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria beginsWith(final String s) {
    this.beginsWith = s;
    return this;
  }

  /**
   * Get EQ {@link String}.
   *
   * @return {@link String}
   */
  public String eq() {
    return this.eq;
  }

  /**
   * Set EQ String.
   *
   * @param s {@link String}
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria eq(final String s) {
    this.eq = s;
    return this;
  }

  /**
   * Get EQ OR {@link Collection} {@link String}.
   *
   * @return {@link String}
   */
  public Collection<String> eqOr() {
    return this.eqOr;
  }

  /**
   * Set EQ Or String.
   *
   * @param s {@link Collection} {@link String}
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria eqOr(final Collection<String> s) {
    this.eqOr = s;
    return this;
  }
  
  /**
   * {@link String} has value.
   *
   * @param s {@link String}
   * @return boolean
   */
  private boolean hasValue(final String s) {
    return s != null && s.length() > 0;
  }

  /**
   * Is {@link SearchTagCriteria} valid for DynamoDB.
   *
   * @throws IllegalArgumentException IllegalArgumentException
   */
  public void isValid() {
    if (!hasValue(this.key)) {
      throw new IllegalArgumentException("'key' attribute is required.");
    }
  }

  /**
   * Get Tag Key.
   *
   * @return {@link String}
   */
  public String key() {
    return this.key;
  }

  /**
   * Set Tag Name.
   *
   * @param s {@link String}
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria key(final String s) {
    this.key = s;
    return this;
  }
}
