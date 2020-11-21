/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

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

/** Date Helper class. */
public final class DateUtil {

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
