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

import java.util.List;
import java.util.Objects;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Data to update.
 */
@Reflectable
public class UpdateMatchingDocumentTagsRequestUpdate {
  /** {@link List} {@link AddDocumentTag}. */
  @Reflectable
  private List<AddDocumentTag> tags;

  /**
   * constructor.
   */
  public UpdateMatchingDocumentTagsRequestUpdate() {}

  /**
   * Set {@link AddDocumentTag} {@link List}.
   * 
   * @param tagList {@link AddDocumentTag} {@link List}
   * @return {@link UpdateMatchingDocumentTagsRequestUpdate}
   */
  public UpdateMatchingDocumentTagsRequestUpdate tags(final List<AddDocumentTag> tagList) {
    this.tags = tagList;
    return this;
  }

  /**
   * List of document tags.
   * 
   * @return tags {@link List} {@link AddDocumentTag}
   **/
  public List<AddDocumentTag> getTags() {
    return this.tags;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateMatchingDocumentTagsRequestUpdate updateMatchingDocumentTagsRequestUpdate =
        (UpdateMatchingDocumentTagsRequestUpdate) o;
    return Objects.equals(this.tags, updateMatchingDocumentTagsRequestUpdate.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateMatchingDocumentTagsRequestUpdate {\n");
    sb.append("    tags: ").append(toIndentedString(this.tags)).append("\n");
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
