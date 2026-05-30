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
package com.formkiq.testutils.api.activities;

import com.formkiq.client.model.UserActivityChanges;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Assert {@link UserActivityChanges}.
 */
public class AssertUserActivityChanges implements
    BiFunction<Map<String, UserActivityChanges>, Map<String, Map<String, Object>>, Void> {

  @Override
  public Void apply(final Map<String, UserActivityChanges> changes,
      final Map<String, Map<String, Object>> expected) {
    assertEquals(changes.size(), expected.size());
    changes.forEach((key, value) -> {
      if (!expected.containsKey(key)) {
        fail("cannot find key: " + key);
      }

      if (!expected.get(key).isEmpty()) {

        Object nv = value.getNewValue();
        if (nv instanceof Collection c) {
          nv = String.join(",", c);
        }
        assertEquals(nv, expected.get(key).get("newValue"));

        Object ov = value.getOldValue();
        if (ov instanceof Collection c) {
          ov = String.join(",", c);
        }
        assertEquals(ov, expected.get(key).get("oldValue"));
      }
    });

    return null;
  }
}
