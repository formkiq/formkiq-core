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

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import java.util.TimeZone;

/** Date Helper class. */
public final class DateUtil {

  /** Date Format. */
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  /** Date Form yyyy-mm-dd. */
  public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";

  /**
   * Formats TZ String to start with '+' or '-'.
   * 
   * @param tz {@link String}
   * @return {@link String}
   */
  private static String formatTz(final String tz) {

    final int min = 3;
    String ret = tz;

    if (tz == null || tz.trim().isEmpty()) {
      ret = "+0";
    } else {

      String[] s = tz.split("[+-]");

      if (s.length == 2) {
        ret = tz.substring(0, 1) + (s[1].length() == min ? "0" + s[1] : s[1]);
      } else if (s.length == 1) {
        ret = "+" + (s[0].length() == min ? "0" + s[0] : s[0]);
      }
    }

    return ret;
  }

  /**
   * Returns Now in ISO 8601 format.
   * 
   * @param date {@link Date}
   * @return String
   */
  public static String getInIso8601Format(final Date date) {
    return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
  }

  /**
   * String to ISO Standard format.
   * 
   * @return {@link SimpleDateFormat}
   */
  public static SimpleDateFormat getIsoDateFormatter() {
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
    TimeZone tz = TimeZone.getTimeZone("UTC");
    df.setTimeZone(tz);
    return df;
  }

  /**
   * Returns Now in ISO 8601 format.
   *
   * @return String
   */
  public static String getNowInIso8601Format() {
    Instant nowUtc = Instant.now();
    return DateTimeFormatter.ISO_INSTANT.format(nowUtc);
  }

  /**
   * String to ISO Standard format.
   * 
   * @return {@link SimpleDateFormat}
   */
  public static SimpleDateFormat getYyyyMmDdFormatter() {
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_YYYY_MM_DD);
    TimeZone tz = TimeZone.getTimeZone("UTC");
    df.setTimeZone(tz);
    return df;
  }

  private static boolean hasZoneMatch(final ZoneId zone, final String value, final String regex) {
    return zone == null && value.matches(regex);
  }

  /**
   * Convert {@link String} Date format and Tz to {@link Date}.
   * 
   * @param date {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws DateTimeException DateTimeException
   * @throws ZoneRulesException ZoneRulesException
   */
  public static Date toDateFromString(final String date, final String tz)
      throws DateTimeException, ZoneRulesException {
    return Date.from(toDateTimeFromString(date, tz).toInstant());
  }

  /**
   * Parses a date or date-time string into a {@link java.util.Date} instance.
   *
   * <p>
   * This method accepts multiple ISO-8601 compatible formats and attempts parsing in the following
   * order:
   * </p>
   *
   * <ol>
   * <li><strong>Offset date-time</strong> (e.g., {@code 2025-11-29T14:20:00Z},
   * {@code 2025-11-29T14:20:00+02:00}) via {@link java.time.OffsetDateTime#parse}</li>
   * <li><strong>Local date-time</strong> without timezone (e.g., {@code 2025-11-29T14:20:00}) which
   * is interpreted as UTC</li>
   * <li><strong>Date only</strong> (e.g., {@code 2025-11-29}), interpreted as the start of day
   * UTC</li>
   * </ol>
   *
   * <p>
   * If a format fails to parse, the method transparently falls back to the next format until a
   * valid {@link Date} is produced. A {@link java.time.format.DateTimeParseException} will only be
   * thrown if the input cannot be parsed by any of the allowed formats.
   * </p>
   *
   * @param s the input date or date-time string (must be in an ISO-8601 compatible format)
   * @param zoneId {@link ZoneId}
   * @return a {@link Date} representing the parsed instant in time
   * @throws java.time.format.DateTimeParseException if the string is not a valid date or date-time
   */
  public static Date toDateFromString(final String s, final ZoneId zoneId) {
    Date result;

    try {
      // First try: full date-time with timezone (ISO 8601)
      result = Date.from(OffsetDateTime.parse(s).toInstant());
    } catch (DateTimeParseException e1) {
      try {
        // Second try: local date-time (assume UTC)
        LocalDateTime dt = LocalDateTime.parse(s);
        result = Date.from(dt.atZone(zoneId).toInstant());
      } catch (DateTimeParseException e2) {
        // Final fallback: date only
        LocalDate date = LocalDate.parse(s);
        result = Date.from(date.atStartOfDay(zoneId).toInstant());
      }
    }

    return result;
  }

  /**
   * Convert {@link String} Date format and Tz to {@link ZonedDateTime}.
   * 
   * @param date {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws DateTimeException DateTimeException
   * @throws ZoneRulesException ZoneRulesException
   */
  public static ZonedDateTime toDateTimeFromString(final String date, final String tz)
      throws DateTimeException, ZoneRulesException {

    DateTimeFormatter df = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE)
        .parseDefaulting(ChronoField.NANO_OF_SECOND, 0).optionalStart().appendLiteral('T')
        .append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter();

    String tzString = formatTz(tz);
    ZoneId zone = ZoneId.of(tzString);

    ZonedDateTime zoneDate;

    try {
      zoneDate = LocalDateTime.parse(date, df).atZone(zone);
    } catch (DateTimeException e) {
      LocalDate parsedDate = LocalDate.parse(date, df);
      zoneDate = parsedDate.atStartOfDay(zone);
    }

    return zoneDate;
  }

  /**
   * Convert {@link String} to {@link ZoneId}.
   * 
   * @param tz {@link String}
   * @return {@link ZoneId}
   */
  public static ZoneId toZoneId(final String tz) {

    String value = tz.trim();
    ZoneId zone = null;

    // Case 1: UTC aliases
    if ("Z".equalsIgnoreCase(value) || "UTC".equalsIgnoreCase(value)) {
      zone = ZoneOffset.UTC;
    } else {
      // Case 2: Try named ZoneId (America/..., Europe/..., GMT+2, etc.)
      try {
        zone = ZoneId.of(value);
      } catch (Exception ignored) {
        // continue to offset patterns
      }

      zone = toZoneIdFromOffset(zone, value);
    }

    if (zone == null) {
      throw new IllegalArgumentException("Invalid timezone or offset: " + tz);
    }

    return zone;
  }

  private static ZoneId toZoneIdFromOffset(final ZoneId zoneId, final String value) {

    ZoneId zone = zoneId;
    final int offsetLen = 3;

    if (hasZoneMatch(zone, value, "^[+-]\\d{2}:\\d{2}$")) {
      // Case 3: Offset with colon (-05:00, +02:30)
      zone = ZoneOffset.of(value);
    }

    if (hasZoneMatch(zone, value, "^[+-]\\d{4}$")) {
      // Case 4: Offset without colon with sign (-0500, +0930)
      String formatted = value.substring(0, offsetLen) + ":" + value.substring(offsetLen);
      zone = ZoneOffset.of(formatted);
    }

    if (hasZoneMatch(zone, value, "^\\d{4}$")) {
      // Case 5: Bare 4-digit offset (0500 → +05:00)
      String signed = "+" + value;
      String formatted = signed.substring(0, offsetLen) + ":" + signed.substring(offsetLen);
      zone = ZoneOffset.of(formatted);
    }

    if (hasZoneMatch(zone, value, "^[+-]\\d{2}$")) {
      // Case 6: Offset hours only with sign (-05, +03)
      zone = ZoneOffset.of(value + ":00");
    }

    if (hasZoneMatch(zone, value, "^\\d{2}$")) {
      // Case 7: Bare 2-digit hours (05 → +05:00)
      zone = ZoneOffset.of("+" + value + ":00");
    }

    return zone;
  }

  /** private constructor. */
  private DateUtil() {}
}
