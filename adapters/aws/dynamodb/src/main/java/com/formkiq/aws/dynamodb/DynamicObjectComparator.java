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
package com.formkiq.aws.dynamodb;

import java.io.Serializable;
import java.util.Comparator;

/**
 * {@link Comparator} for {@link DynamicObject}.
 */
public class DynamicObjectComparator implements Comparator<DynamicObject>, Serializable {

  /** Serial Version UID. */
  private static final long serialVersionUID = 7148397012749271136L;

  /** {@link String}. */
  private String key;

  /**
   * constructor.
   * 
   * @param objectKey {@link String}
   */
  public DynamicObjectComparator(final String objectKey) {
    this.key = objectKey;
  }

  @Override
  public int compare(final DynamicObject o1, final DynamicObject o2) {
    return o1.getString(this.key).compareTo(o2.getString(this.key));
  }
}
