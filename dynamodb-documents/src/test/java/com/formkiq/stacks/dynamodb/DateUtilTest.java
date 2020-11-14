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

import static org.junit.Assert.assertEquals;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for {@link DateUtil}.
 *
 */
public class DateUtilTest {

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * before.
   */
  @Before
  public void before() {
    this.df.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Test yyyy-MM-dd in different Timezones.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testToDateFromString01() throws Exception {
    Date date = null;

    for (String tz : Arrays.asList("-0500", "-05", "-05:00", "-5", "-500")) {
      date = DateUtil.toDateFromString("2020-01-28", tz);
      assertEquals("2020-01-28T05:00:00", this.df.format(date));
    }

    for (String tz : Arrays.asList("+0500", "0500", "05:00", "500")) {
      date = DateUtil.toDateFromString("2020-01-28", tz);
      assertEquals("2020-01-27T19:00:00", this.df.format(date));
    }

    for (String tz : Arrays.asList("-1100", "-11", "-11:00", "-1100", "-11")) {
      date = DateUtil.toDateFromString("2020-01-28", tz);
      assertEquals("2020-01-28T11:00:00", this.df.format(date));
    }

    for (String tz : Arrays.asList("+1100", "+11", "+11:00", "+1100")) {
      date = DateUtil.toDateFromString("2020-01-28", tz);
      assertEquals("2020-01-27T13:00:00", this.df.format(date));
    }

    date = DateUtil.toDateFromString("2020-01-28", "0500");
    assertEquals("2020-01-27T19:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "-0700");
    assertEquals("2020-01-28T07:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "0");
    assertEquals("2020-01-28T00:00:00", this.df.format(date));
  }

  /**
   * Test yyyy-MM-dd in different Timezones.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testToDateFromString02() throws Exception {
    Date date = DateUtil.toDateFromString("2020-01-28", "-0500");
    assertEquals("2020-01-28T05:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "+0500");
    assertEquals("2020-01-27T19:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "0500");
    assertEquals("2020-01-27T19:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "-0700");
    assertEquals("2020-01-28T07:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "0");
    assertEquals("2020-01-28T00:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", "");
    assertEquals("2020-01-28T00:00:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28", null);
    assertEquals("2020-01-28T00:00:00", this.df.format(date));
  }

  /**
   * Test Format yyyy-MM-ddThh:mm:ss.
   */
  @Test
  public void testToDateFromString03() {
    Date date = DateUtil.toDateFromString("2020-01-30T17:59:00", "");
    assertEquals("2020-01-30T17:59:00", this.df.format(date));

    date = DateUtil.toDateFromString("2020-01-28T17:31:45", "+0500");
    assertEquals("2020-01-28T12:31:45", this.df.format(date));
  }
}
