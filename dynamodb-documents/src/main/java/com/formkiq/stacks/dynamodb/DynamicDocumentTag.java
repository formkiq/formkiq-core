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
import com.formkiq.stacks.common.objects.DynamicObject;

/**
 * 
 * {@link DynamicObject} implementation of the {@link DocumentTag}.
 *
 */
public class DynamicDocumentTag extends DynamicObject {

  /** serialVersionUID. */
  private static final long serialVersionUID = 7802548792852503376L;

  /**
   * constructor.
   * 
   * @param map {@link Map}
   */
  public DynamicDocumentTag(final Map<String, Object> map) {
    super(map);

    if (!hasString("type")) {
      put("type", DocumentTagType.USERDEFINED.name());
    }
  }
}
