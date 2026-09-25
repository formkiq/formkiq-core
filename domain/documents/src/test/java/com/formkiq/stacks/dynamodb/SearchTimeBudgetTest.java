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

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests elapsed-time checks without waiting for wall-clock time. */
class SearchTimeBudgetTest {

  /** New work is allowed before the boundary and prevented at the boundary. */
  @Test
  void testBudgetBoundary() {
    // given
    AtomicLong clock = new AtomicLong();
    SearchTimeBudget budget = new SearchTimeBudget(clock::get);

    // when
    clock.set(Duration.ofSeconds(25).toNanos() - 1);

    // then
    assertDoesNotThrow(budget::check);

    // when
    clock.incrementAndGet();

    // then
    assertThrows(SearchTimeBudgetExceededException.class, budget::check);
  }

  /** Each invocation starts with its own elapsed-time budget. */
  @Test
  void testIndependentBudgets() {
    // given
    AtomicLong clock = new AtomicLong();
    SearchTimeBudget first = new SearchTimeBudget(clock::get);
    clock.set(Duration.ofSeconds(20).toNanos());
    SearchTimeBudget second = new SearchTimeBudget(clock::get);

    // when
    clock.set(Duration.ofSeconds(25).toNanos());

    // then
    assertThrows(SearchTimeBudgetExceededException.class, first::check);
    assertDoesNotThrow(second::check);
  }
}
