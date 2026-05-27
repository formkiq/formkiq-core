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
package com.formkiq.dates;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Converts two Instants into a list of LocalDate values (yyyy-MM-dd) between them, inclusive.
 * Supports nulls: - If both start and end are null → returns empty list - If only one is non-null →
 * returns a list with only that LocalDate
 */
public class InstantRangeToDatesFunction
    implements BiFunction<Instant, Instant, List<InstantRange>> {

  /** Sort Order. */
  private final String sort;

  /**
   * constructor.
   * 
   * @param sortOrder {@link String}
   */
  public InstantRangeToDatesFunction(final String sortOrder) {
    this.sort = sortOrder;
  }

  private void addRange(final Instant startDate, final Instant endDate,
      final Collection<InstantRange> dates) {

    if (isSameDayUtc(startDate, endDate)) {
      dates.add(new InstantRange(startDate, endDate));
    } else {
      dates.add(new InstantRange(startDate, endOfDayUtc(startDate)));

      Instant date = startOfDayUtc(startDate.plus(1, ChronoUnit.DAYS));

      if (date.isBefore(endDate)) {

        for (Instant d = date; !d.isAfter(endDate); d = d.plus(1, ChronoUnit.DAYS)) {

          if (isSameDayUtc(d, endDate)) {
            dates.add(new InstantRange(startOfDayUtc(d), endDate));
          } else {
            dates.add(new InstantRange(startOfDayUtc(d), endOfDayUtc(d)));
          }
        }

      } else {
        dates.add(new InstantRange(startOfDayUtc(endDate), endDate));
      }
    }
  }

  @Override
  public List<InstantRange> apply(final Instant startInstant, final Instant endInstant) {

    if (startInstant == null && endInstant == null) {
      return Collections.emptyList();
    }

    Instant start;
    Instant end;

    if (startInstant != null && endInstant != null && startInstant.isAfter(endInstant)) {
      start = endInstant.truncatedTo(ChronoUnit.SECONDS);
      end = startInstant.truncatedTo(ChronoUnit.SECONDS);
    } else {
      start = startInstant != null ? startInstant.truncatedTo(ChronoUnit.SECONDS) : null;
      end = endInstant != null ? endInstant.truncatedTo(ChronoUnit.SECONDS) : null;
    }

    Set<InstantRange> dates = createInstantRanges(start, end);
    var list = new ArrayList<>(dates);

    if ("asc".equalsIgnoreCase(this.sort)) {
      Collections.sort(list);
    } else {
      Collections.reverse(list);
    }
    return list;
  }

  private Set<InstantRange> createInstantRanges(final Instant start, final Instant end) {
    Set<InstantRange> dates = new LinkedHashSet<>();

    if (start != null && end == null) {
      dates.add(new InstantRange(start, endOfDayUtc(start)));
    } else if (start == null) {
      dates.add(new InstantRange(startOfDayUtc(end), end));
    } else {
      addRange(start, end, dates);
    }
    return dates;
  }

  private Instant endOfDayUtc(final Instant instant) {
    final int hour = 23;
    final int minute = 59;
    return instant.atZone(ZoneOffset.UTC).toLocalDate().atTime(hour, minute, minute)
        .toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
  }

  private boolean isSameDayUtc(final Instant a, final Instant b) {
    return a.atZone(ZoneOffset.UTC).toLocalDate().equals(b.atZone(ZoneOffset.UTC).toLocalDate());
  }

  private Instant startOfDayUtc(final Instant instant) {
    return instant.atZone(ZoneOffset.UTC).toLocalDate().atTime(LocalTime.MIN)
        .toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
  }

}
