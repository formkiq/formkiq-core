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
 * Add/Update of multiple document tag(s) based on document(s) that have the matching tag.
 */
@Reflectable
public class UpdateMatchingDocumentTagsRequest {

  /** {@link UpdateMatchingDocumentTagsRequestMatch}. */
  @Reflectable
  private UpdateMatchingDocumentTagsRequestMatch match;
  /** {@link UpdateMatchingDocumentTagsRequestUpdate}. */
  @Reflectable
  private UpdateMatchingDocumentTagsRequestUpdate update;

  /**
   * constructor.
   */
  public UpdateMatchingDocumentTagsRequest() {}

  /**
   * Set Match.
   * 
   * @param tagsMatch {@link UpdateMatchingDocumentTagsRequestMatch}
   * @return {@link UpdateMatchingDocumentTagsRequest}
   */
  public UpdateMatchingDocumentTagsRequest match(
      final UpdateMatchingDocumentTagsRequestMatch tagsMatch) {
    this.match = tagsMatch;
    return this;
  }

  /**
   * Get match.
   * 
   * @return match {@link UpdateMatchingDocumentTagsRequestMatch}
   **/
  public UpdateMatchingDocumentTagsRequestMatch getMatch() {
    return this.match;
  }

  /**
   * Set {@link UpdateMatchingDocumentTagsRequestUpdate}.
   * 
   * @param updateRequest {@link UpdateMatchingDocumentTagsRequestUpdate}
   * @return {@link UpdateMatchingDocumentTagsRequest}
   */
  public UpdateMatchingDocumentTagsRequest update(
      final UpdateMatchingDocumentTagsRequestUpdate updateRequest) {
    this.update = updateRequest;
    return this;
  }

  /**
   * Get update.
   * 
   * @return update
   **/
  public UpdateMatchingDocumentTagsRequestUpdate getUpdate() {
    return this.update;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateMatchingDocumentTagsRequest updateMatchingDocumentTagsRequest =
        (UpdateMatchingDocumentTagsRequest) o;
    return Objects.equals(this.match, updateMatchingDocumentTagsRequest.match)
        && Objects.equals(this.update, updateMatchingDocumentTagsRequest.update);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.match, this.update);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateMatchingDocumentTagsRequest {\n");
    sb.append("    match: ").append(toIndentedString(this.match)).append("\n");
    sb.append("    update: ").append(toIndentedString(this.update)).append("\n");
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

