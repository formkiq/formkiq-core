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
package com.formkiq.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link Strings}.
 */
public class StringsTest {
  /**
   * Is Empty.
   */
  @Test
  void testIsEmpty() {
    assertTrue(Strings.isEmpty(null));
    assertTrue(Strings.isEmpty(""));
    assertFalse(Strings.isEmpty(" "));
    assertFalse(Strings.isEmpty("a"));
  }

  @Test
  void testSplitByChar() {
    assertEquals("a d sa add", String.join(" ", Strings.splitByChar("a:d:sa:add", ':')));
    assertEquals("adsa add", String.join(" ", Strings.splitByChar("adsa:add", ':')));
    assertEquals("adsa:add", String.join(" ", Strings.splitByChar("adsa:add", '!')));
    assertEquals("", String.join(" ", Strings.splitByChar(null, '!')));
  }

  @Test
  void isNumeric() {
    assertFalse(Strings.isNumeric(null));
    assertFalse(Strings.isNumeric(""));
    assertTrue(Strings.isNumeric("1"));
    assertTrue(Strings.isNumeric("1.234"));
  }
}
