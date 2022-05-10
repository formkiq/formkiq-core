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
package com.formkiq.stacks.api;

import java.util.List;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.graalvm.annotations.Reflectable;

/** API Response Object. */
@Reflectable
public class ApiDocumentTagsItemResponse implements ApiResponse {

  /** Next Page Token. */
  @Reflectable
  private String next;
  /** Previous Page Token. */
  @Reflectable
  private String previous;
  /** {@link List} of {@link ApiDocumentTagItemResponse}. */
  @Reflectable
  private List<ApiDocumentTagItemResponse> tags;

  /** constructor. */
  public ApiDocumentTagsItemResponse() {}

  @Override
  public String getNext() {
    return this.next;
  }

  /**
   * Set Next Token.
   *
   * @param token {@link String}
   */
  public void setNext(final String token) {
    this.next = token;
  }

  /**
   * Get {@link ApiDocumentTagItemResponse}.
   *
   * @return {@link List} {@link ApiDocumentTagItemResponse}
   */
  public List<ApiDocumentTagItemResponse> getTags() {
    return this.tags;
  }

  /**
   * Set {@link ApiDocumentTagItemResponse}.
   *
   * @param list {@link List} {@link Object}
   */
  public void setTags(final List<ApiDocumentTagItemResponse> list) {
    this.tags = list;
  }

  @Override
  public String getPrevious() {
    return this.previous;
  }

  /**
   * Set Previous Token.
   *
   * @param prev {@link String}
   */
  public void setPrevious(final String prev) {
    this.previous = prev;
  }
}
