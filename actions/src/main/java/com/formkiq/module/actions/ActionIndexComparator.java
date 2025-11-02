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
package com.formkiq.module.actions;

import java.io.Serializable;
import java.util.Comparator;

/**
 * {@link Comparator} {@link Action}.
 */
public class ActionIndexComparator implements Comparator<Action>, Serializable {

  /** SerialId. */
  private static final long serialVersionUID = 6580985311008802728L;


  @Override
  public int compare(final Action o1, final Action o2) {
    final String index1 = o1.index();
    final String index2 = o2.index();

    if (index1 == null || index2 == null) {
      return handleEitherNull(index1, index2);
    }

    final boolean numeric1 = index1.matches("\\d+");
    final boolean numeric2 = index2.matches("\\d+");

    int result;

    if (numeric1 && numeric2) {
      // Keep int to match your original; switch to Long/BigInteger if needed
      result = Integer.compare(Integer.parseInt(index1), Integer.parseInt(index2));
    } else if (numeric1) {
      // Numeric comes before non-numeric (e.g., ULID)
      result = -1;
    } else if (numeric2) {
      result = 1;
    } else {
      // Both non-numeric (e.g., ULIDs) → lexicographic (case-insensitive)
      result = index1.compareToIgnoreCase(index2);
    }

    return result;
  }

  private int handleEitherNull(final String index1, final String index2) {
    if (index1 == null && index2 == null) {
      return 0;
    }
    return (index1 == null) ? -1 : 1;
  }
}
