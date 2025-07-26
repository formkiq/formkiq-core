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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import org.junit.jupiter.api.Test;

/**
 * Unit Test for {@link MapChangesFunction}.
 */
public class MapChangesFunctionTest {

  /** {@link MapChangesFunction}. */
  private final MapChangesFunction function = new MapChangesFunction();

  @Test
  void testNoChanges() {
    // given
    Map<String, Object> previous = Map.of("key1", "value1", "key2", 2);
    Map<String, Object> current = Map.of("key1", "value1", "key2", 2);

    // when
    Map<String, ChangeRecord> result = function.apply(previous, current);

    // then
    assertTrue(result.isEmpty(), "Expected no differences");
  }

  @Test
  void testAddedKey() {
    // given
    Map<String, Object> previous = Map.of("key1", "value1");
    Map<String, Object> current = Map.of("key1", "value1", "key2", 2);

    // when
    Map<String, ChangeRecord> result = function.apply(previous, current);

    // then
    assertEquals(1, result.size());
    assertEquals(2, result.get("key2").newValue());
    assertNull(result.get("key2").oldValue());
  }

  @Test
  void testChangedValue() {
    // given
    Map<String, Object> previous = Map.of("key1", "value1");
    Map<String, Object> current = Map.of("key1", "value2");

    // when
    Map<String, ChangeRecord> result = function.apply(previous, current);

    // then
    assertEquals(1, result.size());
    assertEquals("value1", result.get("key1").oldValue());
    assertEquals("value2", result.get("key1").newValue());
  }

  @Test
  void testMultipleChanges() {
    // given
    Map<String, Object> previous = Map.of("a", 1, "b", "2", "c", "3");
    Map<String, Object> current = Map.of("a", 1, "b", "5", "d", "9");

    // when
    Map<String, ChangeRecord> result = function.apply(previous, current);

    // then
    final int expected = 3;
    assertEquals(expected, result.size());
    assertEquals("2", result.get("b").oldValue());
    assertEquals("5", result.get("b").newValue());
    assertEquals("3", result.get("c").oldValue());
    assertNull(result.get("c").newValue());
    assertNull(result.get("d").oldValue());
    assertEquals("9", result.get("d").newValue());
  }

  @Test
  void testNullHandling() {
    // given
    Map<String, Object> previous = new HashMap<>();
    previous.put("a", null);
    Map<String, Object> current = Map.of("a", "non-null");

    // when
    Map<String, ChangeRecord> result = function.apply(previous, current);

    // then
    assertEquals(1, result.size());
    assertEquals("non-null", result.get("a").newValue());
    assertNull(result.get("a").oldValue());
  }
}
