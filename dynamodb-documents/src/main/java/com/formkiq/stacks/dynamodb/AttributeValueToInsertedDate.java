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
public class AttributeValueToInsertedDate implements Function<Map<String, AttributeValue>, Date> {

  /** Date Format. */
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;

  /**
   * constructor.
   */
  public AttributeValueToInsertedDate() {
    this.df = new SimpleDateFormat(DATE_FORMAT);

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.df.setTimeZone(tz);
  }

  @Override
  public Date apply(final Map<String, AttributeValue> map) {

    Date date;
    if (map.containsKey("inserteddate")) {
      String dateString = map.get("inserteddate").s();

      try {
        date = this.df.parse(dateString);
      } catch (ParseException e) {
        date = null;
      }

    } else {
      date = null;
    }

    return date;
  }
}
