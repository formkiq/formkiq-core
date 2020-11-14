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

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/** API Response Object. */
@Reflectable
public class ApiMapResponse implements ApiResponse {

  /** Document Id. */
  @Reflectable
  private Map<String, Object> map;

  /** constructor. */
  public ApiMapResponse() {}

  /**
   * constructor.
   * 
   * @param m {@link Map}
   */
  public ApiMapResponse(final Map<String, Object> m) {
    this.map = m;
  }

  @Override
  public String getNext() {
    return (String) this.map.get("next");
  }

  @Override
  public String getPrevious() {
    return (String) this.map.get("previous");
  }

  @Override
  public String toString() {
    return this.map.toString();
  }

  /**
   * Get {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, Object> getMap() {
    return this.map;
  }

  /**
   * Set {@link Map}.
   * 
   * @param m {@link Map}
   */
  public void setMap(final Map<String, Object> m) {
    this.map = m;
  }
}
