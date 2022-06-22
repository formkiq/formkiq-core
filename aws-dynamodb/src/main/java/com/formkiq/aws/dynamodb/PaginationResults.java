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
package com.formkiq.aws.dynamodb;

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
