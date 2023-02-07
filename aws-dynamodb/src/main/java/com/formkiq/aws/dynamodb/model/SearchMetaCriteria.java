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

/** Searches for {@link DocumentItem} Meta Data. */
@Reflectable
public class SearchMetaCriteria {

  /** Search Tag Value Equals. */
  @Reflectable
  private String eq;
  /**
   * Folder Key.
   * 
   * @deprecated replaced with indexType = 'folder'
   */
  @Deprecated
  @Reflectable
  private String folder;
  /** Begins With Filter. */
  @Reflectable
  private String indexFilterBeginsWith;
  /** Index Key. */
  @Reflectable
  private String indexType;
  /** Document Path. */
  @Reflectable
  private String path;

  /** constructor. */
  public SearchMetaCriteria() {}

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
   * @return {@link SearchMetaCriteria}
   */
  public SearchMetaCriteria eq(final String s) {
    this.eq = s;
    return this;
  }

  /**
   * Get Folder.
   *
   * @return {@link String}
   */
  public String folder() {
    return this.folder;
  }

  /**
   * Set Folder.
   *
   * @param s {@link String}
   * @return {@link SearchMetaCriteria}
   */
  public SearchMetaCriteria folder(final String s) {
    this.folder = s;
    return this;
  }

  /**
   * Get Index 'Begins With' filter.
   * 
   * @return {@link String}
   */
  public String indexFilterBeginsWith() {
    return this.indexFilterBeginsWith;
  }

  /**
   * Set Index 'Begins With' filter.
   * 
   * @param beginsWith {@link String}
   * @return {@link SearchMetaCriteria}
   */
  public SearchMetaCriteria indexFilterBeginsWith(final String beginsWith) {
    this.indexFilterBeginsWith = beginsWith;
    return this;
  }

  /**
   * Get Index Type.
   * 
   * @return {@link String}
   */
  public String indexType() {
    return this.indexType;
  }

  /**
   * Set Index Type.
   * 
   * @param index {@link String}
   * @return {@link SearchMetaCriteria}
   */
  public SearchMetaCriteria indexType(final String index) {
    this.indexType = index;
    return this;
  }

  /**
   * Get Path.
   *
   * @return {@link String}
   */
  public String path() {
    return this.path;
  }

  /**
   * Set Path.
   *
   * @param s {@link String}
   * @return {@link SearchMetaCriteria}
   */
  public SearchMetaCriteria path(final String s) {
    this.path = s;
    return this;
  }
}
