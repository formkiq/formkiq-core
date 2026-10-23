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
package com.formkiq.stacks.api.handler.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for deletion mode conversion. */
public class DocumentDeleteTypeTest {

  /**
   * Deletion modes accept any case and surrounding whitespace.
   *
   * @param value query parameter value
   * @param expected expected deletion mode
   */
  @ParameterizedTest
  @CsvSource({"soft_delete, SOFT_DELETE", "Soft_Delete, SOFT_DELETE", "hard_delete, HARD_DELETE",
      "Hard_Delete, HARD_DELETE", "content_only, CONTENT_ONLY", "Content_Only, CONTENT_ONLY",
      "'  content_only  ', CONTENT_ONLY"})
  void testFromString(final String value, final DocumentDeleteType expected) {
    // given
    String input = value;

    // when
    DocumentDeleteType result = DocumentDeleteType.fromString(input);

    // then
    assertEquals(expected, result);
  }
}
