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
package com.formkiq.stacks.dynamodb;

import java.util.Map;
import com.formkiq.graalvm.annotations.Reflectable;

/** Pagination Token for Results. */
@Reflectable
public class PaginationMapToken {

  /** {@link Object} {@link Map}. */
  @Reflectable
  private Map<String, Object> attributeMap;

  /**
   * constructor.
   *
   * @param map {@link Map}
   */
  public PaginationMapToken(final Map<String, Object> map) {
    this.attributeMap = map;
  }

  /**
   * Get Attribute Map.
   *
   * @return {@link Map}
   */
  public Map<String, Object> getAttributeMap() {
    return this.attributeMap;
  }

  @Override
  public String toString() {
    return this.attributeMap.toString();
  }
}
