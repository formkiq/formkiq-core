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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
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

    if (tz == null || tz.trim().length() == 0) {
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

  /**
   * Get {@link ZoneOffset}.
   * 
   * @param offset {@link String}
   * @return {@link ZoneOffset}
   */
  public static ZoneOffset getZoneOffset(final String offset) {
    return ZoneOffset.of(formatTz(offset));
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

    ZonedDateTime zoneDate = null;

    try {
      zoneDate = LocalDateTime.parse(date, df).atZone(zone);
    } catch (DateTimeException e) {
      LocalDate parsedDate = LocalDate.parse(date, df);
      zoneDate = parsedDate.atStartOfDay(zone);
    }

    return zoneDate;
  }

  /** private constructor. */
  private DateUtil() {}
}
