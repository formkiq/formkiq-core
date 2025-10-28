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
package com.formkiq.dates;

import java.time.Instant;

/**
 * A value object representing a time range between two Instants. The range is typically interpreted
 * as: start <= time < end.
 */
public record InstantRange(Instant start, Instant end) implements Comparable<InstantRange> {
  @Override
  public int compareTo(final InstantRange other) {
    int result;

    // null other means this is greater
    if (other == null) {
      return 1;
    }

    // 1) compare start
    result = compareNullableInstant(this.start, other.start);

    // 2) compare end only if start is equal
    if (result == 0) {
      result = compareNullableInstant(this.end, other.end);
    }

    return result;
  }

  /**
   * Compares two Instants where null is considered the earliest (for start) and the smallest (for
   * ordering). Example: null < any Instant
   */
  private static int compareNullableInstant(final Instant a, final Instant b) {
    int result;
    if (a == null && b == null) {
      result = 0;
    } else if (a == null) {
      result = -1;
    } else if (b == null) {
      result = 1;
    } else {
      result = a.compareTo(b);
    }
    return result;
  }
}
