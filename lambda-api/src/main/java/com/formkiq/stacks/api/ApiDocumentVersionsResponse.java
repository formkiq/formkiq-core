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

/** API Response Object. */
@Reflectable
public class ApiDocumentVersionsResponse implements ApiResponse {

  /** Next Results Token. */
  @Reflectable
  private String next;
  /** Document String Tag Value. */
  @Reflectable
  private List<ApiDocumentVersion> versions;

  /** constructor. */
  public ApiDocumentVersionsResponse() {}

  @Override
  public String getNext() {
    return this.next;
  }

  @Override
  public String getPrevious() {
    return null;
  }

  /**
   * Get Document Versions.
   * 
   * @return {@link List} {@link ApiDocumentVersion}
   */
  public List<ApiDocumentVersion> getVersions() {
    return this.versions;
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
   * Set Document Versions.
   * 
   * @param list {@link List} {@link ApiDocumentVersion}
   */
  public void setVersions(final List<ApiDocumentVersion> list) {
    this.versions = list;
  }
}
