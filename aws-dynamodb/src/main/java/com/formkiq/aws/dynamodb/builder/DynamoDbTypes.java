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
package com.formkiq.aws.dynamodb.builder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Helper methods for dealing with DynamoDb Types.
 */
public interface DynamoDbTypes {

  /** Date Pattern. */
  String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

  /**
   * Convert {@link AttributeValue} to {@link String}.
   *
   * @param attributeValue {@link AttributeValue}
   * @return String
   */
  static String toString(final AttributeValue attributeValue) {
    return attributeValue != null ? attributeValue.s() : null;
  }

  static <T> T toCustom(final String name, final Map<String, AttributeValue> attrs,
      final CustomDynamoDbAttributeBuilder builder) {
    return builder.decode(name, attrs);
  }

  /**
   * Parses the given date-time {@link String} to a {@link Date} using the pattern
   * {@code yyyy-MM-dd'T'HH:mm:ssZ}.
   *
   * @param attributeValue the date-time string to parse
   * @return the parsed Date
   */
  static Date toDate(final AttributeValue attributeValue) {
    return attributeValue != null ? toDate(attributeValue.s()) : null;
  }

  /**
   * Parses the given date-time {@link String} to a {@link Date} using the pattern
   * {@code yyyy-MM-dd'T'HH:mm:ssZ}.
   *
   * @param s the date-time string to parse
   * @return the parsed Date
   * @throws IllegalArgumentException if {@code dateString} is null
   */
  static Date toDate(final String s) {
    Date date = null;
    if (s != null) {
      DateFormat df = getDateFormat();
      try {
        date = df.parse(s);
      } catch (ParseException e) {
        // ignore
      }
    }

    return date;
  }

  private static DateFormat getDateFormat() {
    DateFormat df = new SimpleDateFormat(DATE_PATTERN);
    TimeZone tz = TimeZone.getTimeZone("UTC");
    df.setTimeZone(tz);
    return df;
  }

  /**
   * Formats {@link Date} for DynamoDb storage.
   *
   * @param value {@link Date}
   * @return String
   */
  static String fromDate(final Date value) {
    DateFormat df = getDateFormat();
    return df.format(value);
  }
}
