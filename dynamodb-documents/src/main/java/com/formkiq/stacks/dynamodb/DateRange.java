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

import java.util.Date;
import java.util.Objects;

/** Holder for Date Range. */
public class DateRange {

  /** Start Date. */
  private Date startDate;
  /** End Date. */
  private Date endDate;

  /**
   * constructor.
   *
   * @param date {@link Date}
   */
  public DateRange(final Date date) {
    this.startDate = (Date) date.clone();
    this.endDate = this.startDate;
  }

  /**
   * constructor.
   *
   * @param start {@link Date}
   * @param end {@link Date}
   */
  public DateRange(final Date start, final Date end) {
    this.startDate = (Date) start.clone();
    this.endDate = (Date) end.clone();
  }

  /**
   * Get Start Date.
   *
   * @return {@link Date}
   */
  public Date getStartDate() {
    return (Date) this.startDate.clone();
  }

  /**
   * Get End Date.
   *
   * @return {@link Date}
   */
  public Date getEndDate() {
    return (Date) this.endDate.clone();
  }

  @Override
  public String toString() {
    return this.endDate != null ? "startdate=" + this.startDate + ",enddate=" + this.endDate
        : "startdate=" + this.startDate;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof DateRange)) {
      return false;
    }

    DateRange dr = (DateRange) o;
    return Objects.equals(this.startDate, dr.getStartDate())
        && Objects.equals(this.endDate, dr.getEndDate());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startDate, this.endDate);
  }
}
