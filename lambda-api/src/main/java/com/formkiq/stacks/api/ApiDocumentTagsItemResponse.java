/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.lambda.apigateway.ApiResponse;

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
