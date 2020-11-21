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

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.lambda.apigateway.ApiResponse;

/** API Upload URL Response. */
@Reflectable
public class ApiUrlResponse implements ApiResponse {

  /** {@link String} URL. */
  @Reflectable
  private String url;
  /** {@link String} DocumentId. */
  @Reflectable
  private String documentId;

  /** constructor. */
  public ApiUrlResponse() {}

  /**
   * constructor.
   *
   * @param u {@link String}
   * @param id {@link String}
   */
  public ApiUrlResponse(final String u, final String id) {
    this();
    this.url = u;
    this.documentId = id;
  }

  @Override
  public String getNext() {
    return null;
  }

  @Override
  public String getPrevious() {
    return null;
  }

  /**
   * Get Url.
   *
   * @return {@link String}
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * Set Url.
   *
   * @param u {@link String}
   */
  public void setUrl(final String u) {
    this.url = u;
  }

  /**
   * Get Document Id.
   *
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set DocumentId.
   *
   * @param id {@link String}
   */
  public void setDocumentId(final String id) {
    this.documentId = id;
  }
}
