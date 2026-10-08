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
package com.formkiq.stacks.api.handler;

import com.formkiq.client.model.DocumentAttribute;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumberAttribute;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression tests for numeric attribute assertions. */
class DocumentAttributeAssertionsTest {
  @Test
  void testIncorrectNumberFails() {
    // given
    DocumentAttribute attribute = new DocumentAttribute().key("total").userId("joesmith")
        .insertedDate("2026-10-02T00:00:00Z").numberValue(BigDecimal.TEN);

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertThrows(AssertionError.class, assertion);
  }

  @Test
  void testMatchingNumberPasses() {
    // given
    DocumentAttribute attribute = new DocumentAttribute().key("total").userId("joesmith")
        .insertedDate("2026-10-02T00:00:00Z").numberValue(new BigDecimal("100.00"));

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertDoesNotThrow(assertion);
  }

  @Test
  void testMissingExpectedNumberFails() {
    // given
    DocumentAttribute attribute = new DocumentAttribute().key("total").userId("joesmith")
        .insertedDate("2026-10-02T00:00:00Z");

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertThrows(AssertionError.class, assertion);
  }
}
