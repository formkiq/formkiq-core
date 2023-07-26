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
public class MatchDocumentTag {
  /** Tag Key. */
  @Reflectable
  private String key;
  /** Tag Value. */
  @Reflectable
  private String value;

  /**
   * constructor.
   */
  public MatchDocumentTag() {}

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
   * Tag key.
   * 
   * @return key
   **/
  public String getKey() {
    return this.key;
  }

  /**
   * Set Value.
   * 
   * @param tagValue {@link String}
   * @return {@link MatchDocumentTag}
   */
  public MatchDocumentTag value(final String tagValue) {
    this.value = tagValue;
    return this;
  }

  /**
   * Tag value.
   * 
   * @return value
   **/
  public String getValue() {
    return this.value;
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
        && Objects.equals(this.value, matchDocumentTag.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.key, this.value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MatchDocumentTag {\n");
    sb.append("    key: ").append(toIndentedString(this.key)).append("\n");
    sb.append("    value: ").append(toIndentedString(this.value)).append("\n");
    sb.append("}");
    return sb.toString();
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
}
