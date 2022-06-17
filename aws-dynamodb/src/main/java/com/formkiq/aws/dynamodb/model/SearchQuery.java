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

import java.util.Collection;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/** Search Query. */
@Reflectable
public class SearchQuery {

  /** {@link SearchTagCriteria}. */
  @Reflectable
  private SearchTagCriteria tag;
  /** {@link List} {@link SearchTagCriteria}. */
  @Reflectable
  private List<SearchTagCriteria> tags;
  /** Collection of Document Ids use {@link SearchTagCriteria} against. */
  @Reflectable
  private Collection<String> documentIds;

  /** constructor. */
  public SearchQuery() {}

  /**
   * Get Document Ids.
   * 
   * @return {@link Collection} {@link String}
   */
  public Collection<String> documentIds() {
    return this.documentIds;
  }

  /**
   * Set Document Ids.
   * 
   * @param ids {@link Collection} {@link String}
   * @return {@link SearchQuery}
   */
  public SearchQuery documentsIds(final Collection<String> ids) {
    this.documentIds = ids;
    return this;
  }

  /**
   * Get {@link SearchTagCriteria}.
   *
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria tag() {
    return this.tag;
  }

  /**
   * Set {@link SearchTagCriteria}.
   *
   * @param searchtag {@link SearchTagCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery tag(final SearchTagCriteria searchtag) {
    this.tag = searchtag;
    return this;
  }

  /**
   * Get {@link List} {@link SearchTagCriteria}.
   * 
   * @return {@link List} {@link SearchTagCriteria}
   */
  public List<SearchTagCriteria> tags() {
    return this.tags;
  }

  /**
   * Set {@link List} {@link SearchTagCriteria}.
   * 
   * @param list {@link List} {@link SearchTagCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery tags(final List<SearchTagCriteria> list) {
    this.tags = list;
    return this;
  }
}
