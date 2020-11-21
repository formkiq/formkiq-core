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
