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
package com.formkiq.testutils;

import java.time.Duration;
import java.util.function.Predicate;

/** Bounded polling helper for eventually consistent test assertions. */
public final class TestWait {

  /**
   * Supplier that may throw while the expected result is not yet available.
   *
   * @param <T> supplied type
   */
  @FunctionalInterface
  public interface CheckedSupplier<T> {

    /**
     * Get the next value.
     *
     * @return supplied value
     * @throws Exception an error has occurred
     */
    T get() throws Exception;
  }

  /** Milliseconds per second. */
  private static final long MILLISECONDS_PER_SECOND = 1000L;

  /** Default polling interval. */
  private static final Duration POLL_INTERVAL = Duration.ofMillis(25);

  /** Default timeout. */
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  /**
   * Poll until a supplied value satisfies a condition.
   *
   * @param description timeout description
   * @param supplier value supplier
   * @param complete completion condition
   * @param <T> supplied type
   * @return first matching value
   * @throws InterruptedException interrupted while waiting
   */
  public static <T> T until(final String description, final CheckedSupplier<T> supplier,
      final Predicate<T> complete) throws InterruptedException {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    Throwable lastFailure = null;

    while (true) {
      try {
        T value = supplier.get();
        if (complete.test(value)) {
          return value;
        }
      } catch (InterruptedException e) {
        throw e;
      } catch (Exception e) {
        lastFailure = e;
      }

      if (System.nanoTime() >= deadline) {
        AssertionError error = new AssertionError("Timed out waiting for " + description);
        if (lastFailure != null) {
          error.initCause(lastFailure);
        }
        throw error;
      }

      Thread.sleep(POLL_INTERVAL.toMillis());
    }
  }

  /**
   * Wait until the wall clock advances to a new millisecond.
   *
   * @throws InterruptedException interrupted while waiting
   */
  public static void untilNextMillisecond() throws InterruptedException {
    long currentMillisecond = System.currentTimeMillis();
    until("the system clock to advance to the next millisecond", System::currentTimeMillis,
        time -> time > currentMillisecond);
  }

  /**
   * Wait until the wall clock advances to a new second.
   *
   * @throws InterruptedException interrupted while waiting
   */
  public static void untilNextSecond() throws InterruptedException {
    long currentSecond = System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
    until("the system clock to advance to the next second",
        () -> System.currentTimeMillis() / MILLISECONDS_PER_SECOND,
        second -> second > currentSecond);
  }

  private TestWait() {}
}
