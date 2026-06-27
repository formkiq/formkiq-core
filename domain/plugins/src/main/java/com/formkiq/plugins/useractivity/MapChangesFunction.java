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
package com.formkiq.plugins.useractivity;

import com.formkiq.aws.dynamodb.DbKeyPredicate;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * {@link BiFunction} to calculate the differences between 2 maps.
 */
public class MapChangesFunction
    implements BiFunction<Map<String, Object>, Map<String, Object>, Map<String, ChangeRecord>> {

  /** {@link DbKeyPredicate}. */
  private final DbKeyPredicate dbKeyPredicate = new DbKeyPredicate();

  @Override
  public Map<String, ChangeRecord> apply(final Map<String, Object> previous,
      final Map<String, Object> current) {

    Map<String, ChangeRecord> differences = new HashMap<>();

    for (Map.Entry<String, Object> entry : current.entrySet()) {
      String key = entry.getKey();
      if (!dbKeyPredicate.test(key)) {
        Object curr = entry.getValue();
        Object prev = previous.get(key);

        if (prev == null || curr == null) {
          differences.put(key, new ChangeRecord(prev, curr));
        } else if (!isEquals(prev, curr)) {
          differences.put(key, new ChangeRecord(prev, curr));
        }
      }
    }

    for (Map.Entry<String, Object> entry : previous.entrySet()) {
      String key = entry.getKey();
      if (!dbKeyPredicate.test(key) && !current.containsKey(key)) {
        differences.put(key, new ChangeRecord(entry.getValue(), null));
      }
    }

    return differences;
  }

  private boolean isEquals(final Object o1, final Object o2) {
    boolean isEqual = o1.equals(o2);

    if (!isEqual && o1 instanceof Number && o2 instanceof Number) {
      BigDecimal n1 = toBigDecimal((Number) o1);
      BigDecimal n2 = toBigDecimal((Number) o2);
      isEqual = n1.compareTo(n2) == 0;
    }

    return isEqual;
  }

  /**
   * Turn any Number into a BigDecimal “sensibly”: - if it’s already BigDecimal, return it -
   * otherwise use its string form (so 1.0D → "1.0", 2L → "2").
   * 
   * @param number {@link Number}
   * @return BigDecimal
   */
  private BigDecimal toBigDecimal(final Number number) {
    if (number instanceof BigDecimal) {
      return (BigDecimal) number;
    }
    return new BigDecimal(number.toString());
  }
}
