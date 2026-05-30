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
package com.formkiq.module.http;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A utility class for executing operations with retry logic and exponential backoff.
 */
public class RetryExecutor {

  /** Default initial delay for backoff in seconds. */
  private static final long INITIAL_DELAY_SECONDS = 1L;

  /**
   * Executes the given operation with retry logic and exponential backoff.
   *
   * @param <R> the return type of the operation
   * @param operation the function to execute; may throw an exception
   * @param retries the maximum number of retries before failing
   * @param retryExceptionCondition a predicate that determines whether the exception should be
   *        retried
   * @return the result of the successful operation
   * @throws RuntimeException if the operation fails after all retries
   */
  public static <R> R executeWithRetry(final Function<Void, R> operation, final int retries,
      final Predicate<Exception> retryExceptionCondition) {
    int attempt = 0;
    long waitTime = INITIAL_DELAY_SECONDS;

    while (true) {
      try {
        return operation.apply(null);
      } catch (Exception e) {

        if (retryExceptionCondition.test(e)) {
          attempt++;
          if (attempt > retries) {
            throw new RuntimeException(String.format("Operation failed after %d retries", retries),
                e);
          }

          try {
            TimeUnit.SECONDS.sleep(waitTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ie);
          }

          waitTime *= 2;
        } else {
          return null;
        }
      }
    }
  }
}
