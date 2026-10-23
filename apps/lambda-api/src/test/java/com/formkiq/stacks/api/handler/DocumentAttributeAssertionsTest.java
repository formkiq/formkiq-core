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
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertNumberAttribute;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertCreatedBetween;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertModifiedAfter;
import static com.formkiq.stacks.api.handler.DocumentAttributeAssertions.assertSameTimestamps;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression tests for attribute response assertions. */
class DocumentAttributeAssertionsTest {
  private static Stream<DocumentAttribute> changedTimestamps() {
    return Stream.of(new DocumentAttribute("2026-10-02T00:00:02+0000", "2026-10-02T00:00:01+0000"),
        new DocumentAttribute("2026-10-02T00:00:00+0000", "2026-10-02T00:00:02+0000"));
  }

  private static Stream<DocumentAttribute> invalidCreationTimestamps() {
    return Stream.of(new DocumentAttribute("2026-10-01T23:59:59+0000", "2026-10-01T23:59:59+0000"),
        new DocumentAttribute("2026-10-02T00:00:02+0000", "2026-10-02T00:00:02+0000"),
        new DocumentAttribute("2026-10-02T00:00:00+0000", "2026-10-02T00:00:01+0000"));
  }

  @Test
  void testIncorrectNumberFails() {
    // given
    DocumentAttribute attribute = new DocumentAttribute("2026-10-02T00:00:00Z", null).key("total")
        .userId("joesmith").numberValue(BigDecimal.TEN);

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertThrows(AssertionError.class, assertion);
  }

  @ParameterizedTest
  @MethodSource("invalidCreationTimestamps")
  void testInvalidCreationTimestampsFail(final DocumentAttribute attribute) {
    // given
    Instant started = Instant.parse("2026-10-02T00:00:00Z");
    Instant finished = started.plusSeconds(1);

    // when
    Executable assertion = () -> assertCreatedBetween(attribute, started, finished);

    // then
    assertThrows(AssertionError.class, assertion);
  }

  @Test
  void testMatchingNumberPasses() {
    // given
    DocumentAttribute attribute = new DocumentAttribute("2026-10-02T00:00:00Z", null).key("total")
        .userId("joesmith").numberValue(new BigDecimal("100.00"));

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertDoesNotThrow(assertion);
  }

  @Test
  void testMissingExpectedNumberFails() {
    // given
    DocumentAttribute attribute =
        new DocumentAttribute("2026-10-02T00:00:00Z", null).key("total").userId("joesmith");

    // when
    Executable assertion = () -> assertNumberAttribute(attribute, "total", "100");

    // then
    assertThrows(AssertionError.class, assertion);
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-10-02T00:00:00+0000", "2026-10-01T23:59:59+0000"})
  void testModificationMustAdvance(final String lastModifiedDate) {
    // given
    String insertedDate = "2026-10-02T00:00:00+0000";
    DocumentAttribute before = new DocumentAttribute(insertedDate, insertedDate);
    DocumentAttribute after = new DocumentAttribute(insertedDate, lastModifiedDate);

    // when
    Executable assertion = () -> assertModifiedAfter(before, after);

    // then
    assertThrows(AssertionError.class, assertion);
  }

  @ParameterizedTest
  @MethodSource("changedTimestamps")
  void testTimestampChangesFail(final DocumentAttribute actual) {
    // given
    DocumentAttribute expected =
        new DocumentAttribute("2026-10-02T00:00:00+0000", "2026-10-02T00:00:01+0000");

    // when
    Executable assertion = () -> assertSameTimestamps(expected, actual);

    // then
    assertThrows(AssertionError.class, assertion);
  }
}
