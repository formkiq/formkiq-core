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
package com.formkiq.aws.dynamodb;

import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Helper class for {@link software.amazon.awssdk.services.dynamodb.model.AttributeValue}.
 */
public class AttributeValueHelper {

  /**
   * Add {@link Date} to {@link Map} if value is not empty.
   *
   * @param map {@link Map}
   * @param key {@link String}
   * @param value {@link Date}
   */
  public static void addDateIfNotNull(final Map<String, AttributeValue> map, final String key,
      final Date value) {
    if (value != null) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();
      map.put(key, AttributeValue.fromS(df.format(value)));
    }
  }

  /**
   * Add {@link String} to {@link Map} if value is not empty.
   *
   * @param map {@link Map}
   * @param key {@link String}
   * @param value {@link Enum}
   */
  public static void addEnumIfNotNull(final Map<String, AttributeValue> map, final String key,
      final Enum<?> value) {
    if (value != null) {
      map.put(key, fromS(value.name()));
    }
  }

  /**
   * Add {@link String} to {@link Map} if value is not empty.
   * 
   * @param map {@link Map}
   * @param key {@link String}
   * @param value {@link Double}
   */
  public static void addNumberIfNotEmpty(final Map<String, AttributeValue> map, final String key,
      final Double value) {
    if (value != null) {
      map.put(key, fromN("" + value));
    }
  }

  /**
   * Add {@link String} to {@link Map} if value is not empty.
   *
   * @param map {@link Map}
   * @param key {@link String}
   * @param value {@link Long}
   */
  public static void addNumberIfNotEmpty(final Map<String, AttributeValue> map, final String key,
      final Long value) {
    if (value != null) {
      map.put(key, AttributeValue.fromN(String.valueOf(value)));
    }
  }

  /**
   * Add {@link String} to {@link Map} if value is not empty.
   * 
   * @param map {@link Map}
   * @param key {@link String}
   * @param value {@link String}
   */
  public static void addStringIfNotEmpty(final Map<String, AttributeValue> map, final String key,
      final String value) {
    if (!isEmpty(value)) {
      map.put(key, fromS(value));
    }
  }

  /**
   * Convert {@link AttributeValue} to {@link Date}.
   * 
   * @param attrs {@link Map}
   * @param key {@link String}
   * @return Date
   */
  public static Date toDateValue(final Map<String, AttributeValue> attrs, final String key) {

    Date returnDate = null;
    String date = toStringValue(attrs, key);

    if (!isEmpty(date)) {
      try {
        SimpleDateFormat df = DateUtil.getIsoDateFormatter();
        returnDate = df.parse(date);
      } catch (ParseException e) {
        // ignore
      }
    }

    return returnDate;
  }

  /**
   * Convert {@link AttributeValue} to {@link String} or null.
   *
   * @param attrs {@link Map}
   * @param key {@link String}
   * @return Double
   */
  public static Double toDoubleValue(final Map<String, AttributeValue> attrs, final String key) {
    AttributeValue av = attrs.get(key);
    return av != null ? Double.parseDouble(av.n()) : null;
  }

  /**
   * Convert {@link AttributeValue} to {@link String} or null.
   *
   * @param <E> type of {@link Enum}
   * @param attrs {@link Map}
   * @param enumClass {@link Enum}
   * @param key {@link String}
   * @return Enum
   */
  public static <E extends Enum<E>> E toEnumValue(final Map<String, AttributeValue> attrs,
      final Class<E> enumClass, final String key) {
    Enum<E> val = null;
    String sval = toStringValue(attrs, key);
    if (!isEmpty(sval)) {
      val = Enum.valueOf(enumClass, sval);
    }
    return (E) val;
  }

  /**
   * Convert {@link AttributeValue} to {@link String} or null.
   * 
   * @param attrs {@link Map}
   * @param key {@link String}
   * @return String
   */
  public static String toStringValue(final Map<String, AttributeValue> attrs, final String key) {
    AttributeValue av = attrs.get(key);
    return av != null ? av.s() : null;
  }
}
