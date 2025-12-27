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

/**
 * {@link SearchQuery} builder.
 */
public final class SearchQueryBuilder {

  /** {@link SearchAttributeCriteria}. */
  private SearchAttributeCriteria attribute;
  /** {@link List} {@link SearchAttributeCriteria}. */
  private List<SearchAttributeCriteria> attributes;
  /** {@link String}. */
  private Collection<String> documentIds;
  /** {@link SearchMetaCriteria}. */
  private SearchMetaCriteria meta;
  /** {@link SearchTagCriteria}. */
  private SearchTagCriteria tag;
  /** {@link List} {@link SearchTagCriteria}. */
  private List<SearchTagCriteria> tags;
  /** Text. */
  private String text;

  /**
   * constructor.
   */
  public SearchQueryBuilder() {}

  public SearchQueryBuilder attribute(final SearchAttributeCriteria searchAttribute) {
    this.attribute = searchAttribute;
    return this;
  }

  public SearchQueryBuilder attributes(final List<SearchAttributeCriteria> searchAttributes) {
    this.attributes = searchAttributes;
    return this;
  }

  public SearchQuery build() {
    return new SearchQuery(attribute, attributes, documentIds, meta, tag, tags, text, null, null);
  }

  public SearchQueryBuilder documentIds(final Collection<String> searchDocumentIds) {
    this.documentIds = searchDocumentIds;
    return this;
  }

  public SearchQueryBuilder meta(final SearchMetaCriteria searchMeta) {
    this.meta = searchMeta;
    return this;
  }

  public SearchQueryBuilder tag(final SearchTagCriteria searchTag) {
    this.tag = searchTag;
    return this;
  }

  public SearchQueryBuilder tags(final List<SearchTagCriteria> searchTags) {
    this.tags = searchTags;
    return this;
  }

  // ---- build ----

  public SearchQueryBuilder text(final String searchText) {
    this.text = searchText;
    return this;
  }
}
