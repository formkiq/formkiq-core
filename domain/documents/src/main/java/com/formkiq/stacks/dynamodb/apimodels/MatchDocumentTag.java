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
package com.formkiq.stacks.dynamodb.apimodels;

import java.util.Objects;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Match Document Tag.
 */
@Reflectable
public class MatchDocumentTag {
  /** Tag Key begins with. */
  @Reflectable
  private String beginsWith;
  /** Tag Key equals. */
  @Reflectable
  private String eq;
  /** Tag Key. */
  @Reflectable
  private String key;

  /**
   * constructor.
   */
  public MatchDocumentTag() {}

  /**
   * Set Tag Key beginsWith.
   * 
   * @param tagKeyBeginsWith {@link String}
   * @return {@link MatchDocumentTag}
   */
  public MatchDocumentTag beginsWith(final String tagKeyBeginsWith) {
    this.beginsWith = tagKeyBeginsWith;
    return this;
  }

  /**
   * Set Tag Key equals.
   * 
   * @param tagKeyEq {@link String}
   * @return {@link MatchDocumentTag}
   */
  public MatchDocumentTag eq(final String tagKeyEq) {
    this.eq = tagKeyEq;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MatchDocumentTag matchDocumentTag = (MatchDocumentTag) o;
    return Objects.equals(this.key, matchDocumentTag.key)
        && Objects.equals(this.beginsWith, matchDocumentTag.beginsWith)
        && Objects.equals(this.eq, matchDocumentTag.eq);
  }

  /**
   * Searches for strings that begin with.
   * 
   * @return beginsWith
   **/
  public String getBeginsWith() {
    return this.beginsWith;
  }

  /**
   * Searches for strings that eq.
   * 
   * @return eq
   **/
  public String getEq() {
    return this.eq;
  }

  /**
   * Tag key.
   * 
   * @return key
   **/
  public String getKey() {
    return this.key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.key, this.beginsWith, this.eq);
  }

  /**
   * Set Key {@link String}.
   * 
   * @param tagKey {@link String}
   * @return {@link MatchDocumentTag}
   */
  public MatchDocumentTag key(final String tagKey) {
    this.key = tagKey;
    return this;
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   * 
   * @param o {@link Object}
   * @return {@link String}
   */
  private String toIndentedString(final Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MatchDocumentTag {\n");
    sb.append("    key: ").append(toIndentedString(this.key)).append("\n");
    sb.append("    beginsWith: ").append(toIndentedString(this.beginsWith)).append("\n");
    sb.append("    eq: ").append(toIndentedString(this.eq)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
