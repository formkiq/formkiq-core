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
package com.formkiq.stacks.dynamodb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Date}.
 *
 */
public class AttributeValueToDate implements Function<Map<String, AttributeValue>, Date> {

  /** Date Format. */
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;
  /** Map Key. */
  private String key = null;

  /**
   * constructor.
   * 
   * @param dateField {@link String}
   */
  public AttributeValueToDate(final String dateField) {
    this.df = new SimpleDateFormat(DATE_FORMAT);

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.df.setTimeZone(tz);
    this.key = dateField;
  }

  @Override
  public Date apply(final Map<String, AttributeValue> map) {

    Date date = null;
    if (map.containsKey(this.key)) {

      String dateString = map.get(this.key).s();

      if (dateString != null) {
        try {
          date = this.df.parse(dateString);
        } catch (ParseException e) {
          // ignore
        }
      }
    }

    return date;
  }
}
