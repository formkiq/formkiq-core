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

import java.util.ArrayList;
import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.lambda.apigateway.ApiResponse;

/** /sites {@link ApiResponse}. */
@Reflectable
public class ApiSitesResponse implements ApiResponse {

  /** {@link String}. */
  @Reflectable
  private List<Site> sites;

  /**
   * constructor.
   */
  public ApiSitesResponse() {
    this.sites = new ArrayList<>();
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
   * Get Sites.
   * 
   * @return {@link List} {@link Site}
   */
  public List<Site> getSites() {
    return this.sites;
  }

  /**
   * Set Sites.
   * 
   * @param list {@link List} {@link Site}
   */
  public void setSites(final List<Site> list) {
    this.sites = list;
  }
}
