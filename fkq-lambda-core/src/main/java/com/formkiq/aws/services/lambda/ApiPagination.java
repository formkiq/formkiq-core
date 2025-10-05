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
package com.formkiq.aws.services.lambda;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Class for holding results paginating information.
 *
 */
@Reflectable
public class ApiPagination {

  /** String Next Token. */
  @Reflectable
  private String next;
  /** Has Next Token. */
  @Reflectable
  private boolean hasNext;
  /** Previous Token. */
  @Reflectable
  private String previous;
  /** Query Limit. */
  @Reflectable
  private int limit;
  /** {@link PaginationMapToken}. */
  @Reflectable
  private PaginationMapToken startkey;

  /**
   * constructor.
   */
  public ApiPagination() {
    this.next = ID.uuid();
  }

  /**
   * Get Query Limit.
   * 
   * @return int
   */
  public int getLimit() {
    return this.limit;
  }

  /**
   * Get Next Token.
   * 
   * @return {@link String}
   */
  public String getNext() {
    return this.next;
  }

  /**
   * Get Previous Token.
   * 
   * @return {@link String}
   */
  public String getPrevious() {
    return this.previous;
  }

  /**
   * Get {@link PaginationMapToken}.
   * 
   * @return {@link PaginationMapToken}
   */
  public PaginationMapToken getStartkey() {
    return this.startkey;
  }

  /**
   * Has Next.
   * 
   * @return boolean
   */
  public boolean hasNext() {
    return this.hasNext;
  }

  /**
   * Has Previous Token.
   * 
   * @return boolean
   */
  public boolean hasPrevious() {
    return this.previous != null;
  }

  /**
   * Set whether Has Next.
   * 
   * @param nextpage boolean
   */
  public void setHasNext(final boolean nextpage) {
    this.hasNext = nextpage;
  }

  /**
   * Set Query Limit.
   * 
   * @param querylimit int
   */
  public void setLimit(final int querylimit) {
    this.limit = querylimit;
  }

  /**
   * Set Previous Token.
   * 
   * @param token {@link String}
   */
  public void setPrevious(final String token) {
    this.previous = token;
  }

  /**
   * Set {@link PaginationMapToken}.
   * 
   * @param token {@link PaginationMapToken}
   */
  public void setStartkey(final PaginationMapToken token) {
    this.startkey = token;
  }
}
