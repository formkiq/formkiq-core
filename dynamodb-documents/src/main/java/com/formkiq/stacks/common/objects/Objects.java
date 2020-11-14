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
package com.formkiq.stacks.common.objects;

import java.util.Collections;
import java.util.List;

/**
 * 
 * Objects Helper.
 *
 */
public class Objects {

  /**
   * Returns a {@link List} that is guarantee not to be null.
   * 
   * @param <T> Type
   * @param list {@link List}
   * @return {@link List}
   */
  public static <T> List<T> notNull(final List<T> list) {
    return list != null ? list : Collections.emptyList();
  }
}
