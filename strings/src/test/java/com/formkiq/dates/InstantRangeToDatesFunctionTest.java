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
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test for {@link InstantRangeToDatesFunction}.
 */
public class InstantRangeToDatesFunctionTest {

  /** {@link InstantRangeToDatesFunction}. */
  private final InstantRangeToDatesFunction fn = new InstantRangeToDatesFunction("");

  @Test
  @DisplayName("Both null returns empty list")
  void bothNullReturnsEmpty() {
    List<InstantRange> dates = fn.apply(null, null);
    assertTrue(dates.isEmpty());
  }

  @Test
  @DisplayName("End before start")
  void endBeforeStartThrows() {
    // given
    Instant start = Instant.parse("2025-10-24T00:00:00Z");
    Instant end = Instant.parse("2025-10-23T23:59:59Z");

    // when
    List<InstantRange> dates = fn.apply(start, end);

    // then
    assertEquals(2, dates.size());
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(0).end()));

    assertEquals("2025-10-23T23:59:59Z", format(dates.get(1).start()));
    assertEquals("2025-10-23T23:59:59Z", format(dates.get(1).end()));

    // given
    start = Instant.parse("2025-10-24T00:00:01Z");

    // when
    dates = fn.apply(start, end);

    // then
    assertEquals(2, dates.size());

    assertEquals("2025-10-24T00:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-24T00:00:01Z", format(dates.get(0).end()));

    assertEquals("2025-10-23T23:59:59Z", format(dates.get(1).start()));
    assertEquals("2025-10-23T23:59:59Z", format(dates.get(1).end()));
  }

  @Test
  @DisplayName("End before partial time")
  void endPartialTime() {
    Instant start = Instant.parse("2025-10-24T00:00:00Z");
    Instant end = Instant.parse("2025-10-26T20:57:11.998536Z");

    List<InstantRange> dates = fn.apply(start, end);

    assertEquals(3, dates.size());
    assertEquals("2025-10-26T00:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-26T20:57:11Z", format(dates.get(0).end()));
    assertEquals("2025-10-25T00:00:00Z", format(dates.get(1).start()));
    assertEquals("2025-10-25T23:59:59Z", format(dates.get(1).end()));
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(2).start()));
    assertEquals("2025-10-24T23:59:59Z", format(dates.get(2).end()));
  }

  private String format(final Instant instant) {
    return DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  @Test
  @DisplayName("Inclusive multi-day range returns all dates in reverse order (newest → oldest)")
  void multiDayDescendingOrder() {
    Instant start = Instant.parse("2025-10-20T10:15:30Z");
    Instant end = Instant.parse("2025-10-24T00:00:00Z");

    // when
    List<InstantRange> dates = fn.apply(start, end);

    // then
    assertEquals(5, dates.size());

    int i = 0;
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(i).start()));
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(i++).end()));

    assertEquals("2025-10-23T00:00:00Z", format(dates.get(i).start()));
    assertEquals("2025-10-23T23:59:59Z", format(dates.get(i++).end()));

    assertEquals("2025-10-22T00:00:00Z", format(dates.get(i).start()));
    assertEquals("2025-10-22T23:59:59Z", format(dates.get(i++).end()));

    assertEquals("2025-10-21T00:00:00Z", format(dates.get(i).start()));
    assertEquals("2025-10-21T23:59:59Z", format(dates.get(i++).end()));

    assertEquals("2025-10-20T10:15:30Z", format(dates.get(i).start()));
    assertEquals("2025-10-20T23:59:59Z", format(dates.get(i++).end()));
  }

  @Test
  @DisplayName("Only end provided returns that date (single element, reverse order implied)")
  void onlyEndProvided() {
    Instant end = Instant.parse("2025-10-25T05:00:00Z");

    List<InstantRange> dates = fn.apply(null, end);

    assertEquals(1, dates.size());
    assertEquals("2025-10-25T00:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-25T05:00:00Z", format(dates.get(0).end()));
  }

  @Test
  @DisplayName("Only start provided returns that date (single element, reverse order implied)")
  void onlyStartProvided() {
    Instant start = Instant.parse("2025-10-24T10:00:00Z");

    List<InstantRange> dates = fn.apply(start, null);

    assertEquals(1, dates.size());
    assertEquals("2025-10-24T10:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-24T23:59:59Z", format(dates.get(0).end()));
  }

  @Test
  @DisplayName("Same-day instants yield a single LocalDate (unique) in reverse order semantics")
  void sameDayUnique() {
    Instant a = Instant.parse("2025-10-24T00:00:00Z");
    Instant b = Instant.parse("2025-10-24T23:59:59Z");

    List<InstantRange> dates = fn.apply(a, b);

    assertEquals(1, dates.size());
    assertEquals("2025-10-24T00:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-24T23:59:59Z", format(dates.get(0).end()));
  }

  @Test
  @DisplayName("Uniqueness: different instants on same UTC day produce only one LocalDate")
  void uniquenessSameDay() {
    Instant t1 = Instant.parse("2025-10-24T01:00:00Z");
    Instant t2 = Instant.parse("2025-10-24T22:59:59Z");

    List<InstantRange> dates = fn.apply(t1, t2);

    assertEquals(1, dates.size());
    assertEquals("2025-10-24T01:00:00Z", format(dates.get(0).start()));
    assertEquals("2025-10-24T22:59:59Z", format(dates.get(0).end()));
  }
}
