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

import java.time.Duration;
import java.util.function.LongSupplier;

/** Monotonic deadline for one multi-attribute fallback search. */
final class SearchTimeBudget {

  /** Search time reserved before returning through API Gateway. */
  private static final Duration MAX_DURATION = Duration.ofSeconds(25);

  /** Monotonic clock. */
  private final LongSupplier clock;

  /** Start time in nanoseconds. */
  private final long started;

  /**
   * Start a budget using an injectable monotonic clock.
   *
   * @param nanoTime monotonic time source
   */
  SearchTimeBudget(final LongSupplier nanoTime) {
    this.clock = nanoTime;
    this.started = nanoTime.getAsLong();
  }

  /**
   * Reject new work once the elapsed-time budget is exhausted.
   */
  void check() {
    long nanos = MAX_DURATION.toNanos() - (this.clock.getAsLong() - this.started);
    if (nanos <= 0) {
      throw new SearchTimeBudgetExceededException();
    }
  }
}
