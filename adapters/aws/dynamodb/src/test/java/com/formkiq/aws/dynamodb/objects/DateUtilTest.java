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
package com.formkiq.aws.dynamodb.objects;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit Tests for {@link DateUtil}.
 */
public class DateUtilTest {
  @Test
  void testCaseInsensitiveUtc() {
    ZoneId zone = DateUtil.toZoneId("utc");
    assertEquals(ZoneOffset.UTC, zone);
  }

  @Test
  void testDateOnlyUtc() {
    String input = "2025-11-30";
    ZoneId tz = ZoneOffset.UTC;

    Instant expected = LocalDate.parse(input).atStartOfDay(tz).toInstant();
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  @Test
  void testDateOnly_MidnightBoundary() {
    String input = "2025-12-31";
    ZoneId tz = ZoneId.of("UTC");

    Instant expected = Instant.parse("2025-12-31T00:00:00Z");

    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  @Test
  void testDateOnly_Winnipeg() {
    String input = "2025-06-01"; // DST is active
    ZoneId tz = ZoneId.of("America/Winnipeg");

    Instant expected = LocalDate.parse(input).atStartOfDay(tz).toInstant();
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  // ------------------------------
  // Offset DateTime Tests
  // ------------------------------

  @Test
  void testDateOnly_WinnipegWinter() {
    String input = "2025-01-01"; // no DST
    ZoneId tz = ZoneId.of("America/Winnipeg");

    Instant expected = LocalDate.parse(input).atStartOfDay(tz).toInstant();
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  @Test
  void testGmtStyleZone() {
    ZoneId zone = DateUtil.toZoneId("GMT+2");
    assertEquals(ZoneId.of("GMT+2"), zone);
  }

  // ------------------------------
  // Local DateTime Tests (no zone)
  // ------------------------------

  @Test
  void testInvalidOffsetThrowsException() {
    assertThrows(DateTimeException.class, () -> DateUtil.toZoneId("+9999")); // invalid
  }

  @Test
  void testInvalidStringThrows() {
    assertThrows(DateTimeParseException.class,
        () -> DateUtil.toDateFromString("not-a-date", ZoneId.of("UTC")));
  }

  // ------------------------------
  // Date Only Tests
  // ------------------------------

  @Test
  void testInvalidThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> DateUtil.toZoneId("NotAZone"));
  }

  @Test
  void testLocalDateTimeUtc() {
    String input = "2025-11-30T10:20:30";
    ZoneId tz = ZoneOffset.UTC;

    Instant expected = Instant.parse("2025-11-30T10:20:30Z");
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  @Test
  void testLocalDateTime_AssumeProvidedZone() {
    String input = "2025-11-30T10:20:30";
    ZoneId tz = ZoneId.of("America/Winnipeg");

    Instant expected = LocalDateTime.parse(input).atZone(tz).toInstant();
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  // ------------------------------
  // DST Boundary Test
  // ------------------------------

  @Test
  void testLocalDateTime_DstTransition() {
    // 2:30 AM might not exist on DST jump day depending on zone
    String input = "2025-03-09T02:30:00";
    ZoneId tz = ZoneId.of("America/Winnipeg");

    // Ensure method does not throw but resolves using the zone rules
    assertDoesNotThrow(() -> DateUtil.toDateFromString(input, tz));
  }

  // ------------------------------
  // Error Handling Tests
  // ------------------------------

  @Test
  void testNamedZoneAmerica() {
    ZoneId zone = DateUtil.toZoneId("America/Winnipeg");
    assertEquals(ZoneId.of("America/Winnipeg"), zone);
  }

  @Test
  void testNamedZoneEurope() {
    ZoneId zone = DateUtil.toZoneId("Europe/Paris");
    assertEquals(ZoneId.of("Europe/Paris"), zone);
  }

  @Test
  void testNullStringThrows() {
    assertThrows(NullPointerException.class,
        () -> DateUtil.toDateFromString(null, ZoneId.of("UTC")));
  }

  // ------------------------------
  // Midnight edge case
  // ------------------------------

  @Test
  void testNullThrowsException() {
    assertThrows(NullPointerException.class, () -> DateUtil.toZoneId(null));
  }

  @Test
  void testNullTimezoneThrows() {
    assertThrows(NullPointerException.class,
        () -> DateUtil.toDateFromString("2025-11-30", (ZoneId) null));
  }

  @Test
  void testOffsetDateTime_WithOffset() {
    String input = "2025-11-30T15:45:30+02:00";
    ZoneId tz = ZoneId.of("UTC"); // should be ignored

    Instant expected = Instant.parse("2025-11-30T13:45:30Z"); // minus 2h offset
    assertEquals(expected, DateUtil.toDateFromString(input, tz).toInstant());
  }

  @Test
  void testOffsetDateTime_Z() {
    String input = "2025-11-30T15:45:30Z";
    ZoneId tz = ZoneId.of("America/Winnipeg"); // should be ignored

    Date result = DateUtil.toDateFromString(input, tz);

    assertEquals(Instant.parse(input), result.toInstant());
  }

  @Test
  void testOffsetHoursOnlyNegative() {
    ZoneId zone = DateUtil.toZoneId("-05");
    assertEquals(ZoneOffset.of("-05:00"), zone);
  }

  @Test
  void testOffsetHoursOnlyPositive() {
    ZoneId zone = DateUtil.toZoneId("+02");
    assertEquals(ZoneOffset.of("+02:00"), zone);
  }

  @Test
  void testOffsetWithColon() {
    ZoneId zone = DateUtil.toZoneId("-04:30");
    assertEquals(ZoneOffset.of("-04:30"), zone);
  }

  @Test
  void testOffsetWithoutColon() {
    ZoneId zone = DateUtil.toZoneId("+0930");
    assertEquals(ZoneOffset.of("+09:30"), zone);
  }

  @Test
  void testToDateFromString_dateOnly() {
    // given
    String input = "2025-11-30";

    // when
    Date result = DateUtil.toDateFromString(input, ZoneOffset.UTC);

    // then - start of day UTC
    Instant expected = LocalDate.of(2025, 11, 30).atStartOfDay(ZoneOffset.UTC).toInstant();
    assertEquals(expected, result.toInstant());
  }

  @Test
  void testToDateFromString_invalidFormatThrows() {
    // given
    String input = "not-a-date";

    // then
    assertThrows(DateTimeParseException.class,
        () -> DateUtil.toDateFromString(input, ZoneOffset.UTC));
  }

  @Test
  void testToDateFromString_localDateTimeAssumedUtc() {
    // given
    String input = "2025-11-30T15:45:30";

    // when
    Date result = DateUtil.toDateFromString(input, ZoneOffset.UTC);

    // then - interpreted as UTC
    Instant expected = LocalDateTime.of(2025, 11, 30, 15, 45, 30).toInstant(ZoneOffset.UTC);
    assertEquals(expected, result.toInstant());
  }

  @Test
  void testToDateFromString_offsetDateTime() {
    // given
    String input = "2025-11-30T15:45:30Z";

    // when
    Date result = DateUtil.toDateFromString(input, ZoneOffset.UTC);

    // then
    Instant expected = Instant.parse("2025-11-30T15:45:30Z");
    assertEquals(expected, result.toInstant());
  }

  @Test
  void testUtcAliasUtc() {
    ZoneId zone = DateUtil.toZoneId("UTC");
    assertEquals(ZoneOffset.UTC, zone);
  }

  @Test
  void testUtcAlias_Z() {
    ZoneId zone = DateUtil.toZoneId("Z");
    assertEquals(ZoneOffset.UTC, zone);
  }
}
