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
import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/** Search Query. */
@Reflectable
public class SearchQuery {

  /** {@link SearchAttributeCriteria}. */
  @Reflectable
  private SearchAttributeCriteria attribute;
  /** {@link List} {@link SearchAttributeCriteria}. */
  @Reflectable
  private List<SearchAttributeCriteria> attributes;
  /** Collection of Document Ids use {@link SearchTagCriteria} against. */
  @Reflectable
  private Collection<String> documentIds;
  /** {@link SearchMetaCriteria}. */
  @Reflectable
  private SearchMetaCriteria meta;
  /** {@link SearchTagCriteria}. */
  @Reflectable
  private SearchTagCriteria tag;
  /** {@link List} {@link SearchTagCriteria}. */
  @Reflectable
  private List<SearchTagCriteria> tags;
  /** {@link String}. */
  @Reflectable
  private String text;

  /** constructor. */
  public SearchQuery() {}

  /**
   * Set {@link SearchAttributeCriteria}.
   * 
   * @param searchAttribute {@link SearchAttributeCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery attribute(final SearchAttributeCriteria searchAttribute) {
    this.attribute = searchAttribute;
    return this;
  }

  /**
   * Set Attributes.
   * 
   * @param searchAttributes {@link List} {@link SearchAttributeCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery attributes(final List<SearchAttributeCriteria> searchAttributes) {
    this.attributes = searchAttributes;
    return this;
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
   * Get {@link SearchAttributeCriteria}.
   * 
   * @return {@link SearchAttributeCriteria}
   */
  public SearchAttributeCriteria getAttribute() {
    return this.attribute;
  }

  /**
   * Get {@link List} {@link SearchAttributeCriteria}.
   * 
   * @return {@link List} {@link SearchAttributeCriteria}
   */
  public List<SearchAttributeCriteria> getAttributes() {
    return this.attributes;
  }

  /**
   * Get Document Ids.
   * 
   * @return {@link Collection} {@link String}
   */
  public Collection<String> getDocumentIds() {
    return this.documentIds;
  }

  /**
   * Get {@link Map}.
   *
   * @return {@link Map}
   */
  public SearchMetaCriteria getMeta() {
    return this.meta;
  }

  /**
   * Get {@link SearchTagCriteria}.
   *
   * @return {@link SearchTagCriteria}
   */
  public SearchTagCriteria getTag() {
    return this.tag;
  }

  /**
   * Get {@link List} {@link SearchTagCriteria}.
   * 
   * @return {@link List} {@link SearchTagCriteria}
   */
  public List<SearchTagCriteria> getTags() {
    return this.tags;
  }

  /**
   * Get {@link String}.
   *
   * @return {@link String}
   */
  public String getText() {
    return this.text;
  }

  /**
   * Set {@link SearchTagCriteria}.
   *
   * @param searchMeta {@link SearchMetaCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery meta(final SearchMetaCriteria searchMeta) {
    this.meta = searchMeta;
    return this;
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
   * Set {@link List} {@link SearchTagCriteria}.
   * 
   * @param list {@link List} {@link SearchTagCriteria}
   * @return {@link SearchQuery}
   */
  public SearchQuery tags(final List<SearchTagCriteria> list) {
    this.tags = list;
    return this;
  }

  /**
   * Set {@link SearchTagCriteria}.
   *
   * @param textString {@link String}
   * @return {@link SearchQuery}
   */
  public SearchQuery text(final String textString) {
    this.text = textString;
    return this;
  }
}
