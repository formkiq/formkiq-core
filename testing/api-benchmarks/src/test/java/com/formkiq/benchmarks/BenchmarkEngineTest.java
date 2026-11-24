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
package com.formkiq.benchmarks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Verifies overlap and overload accounting with bounded synchronization. */
@Timeout(10)
class BenchmarkEngineTest {

  @Test
  void allowsRequestsToOverlapWithoutExceedingLanes() throws Exception {
    // given
    var overlapped = new CountDownLatch(2);
    var engine = new BenchmarkEngine();

    // when
    var result = engine.run(2, 10, 2, (lane, sequence) -> {
      overlapped.countDown();
      try {
        if (!overlapped.await(3, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Requests did not overlap");
        }
      } catch (InterruptedException exception) {
        throw new IllegalStateException(exception);
      }
      return new Sample(sequence, "success", 200, 1, 0, "");
    }, sample -> {
    });

    // then
    assertEquals(2, result.successes());
    assertEquals(0, result.dropped());
    assertEquals(0, overlapped.getCount());
  }

  @Test
  void keepsErrorsInPercentilesAndExcludesUnsentRequests() {
    // given
    var cell = new CellResult("VERSION", 10, 1, 1,
        List.of(new Sample(0, "success", 200, 10, 0, ""),
            new Sample(1, "http_500", 500, 100, 0, ""),
            new Sample(2, "dropped_concurrency", 0, 0, 0, "")));

    // when
    var summary = cell.summary();

    // then
    assertEquals(100.0, summary.get("p95Ms"));
    assertEquals(10.0, summary.get("successP95Ms"));
    assertEquals(1L, summary.get("errors"));
    assertEquals(1L, summary.get("dropped"));
    assertEquals(1.0, summary.get("successfulRps"));
    assertEquals(99.0, CellResult.percentile(
        IntStream.rangeClosed(1, 100).mapToObj(value -> (double) value).toList(), 0.99));
  }

  @Test
  void reportsDroppedRequestsWhileOneLaneIsOccupied() throws Exception {
    // given
    var entered = new CountDownLatch(1);
    var dropped = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var engine = new BenchmarkEngine();
    try (var executor = Executors.newSingleThreadExecutor()) {
      // when
      var future = executor.submit(() -> engine.run(3, 10, 1, (lane, sequence) -> {
        entered.countDown();
        try {
          if (!release.await(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Request was not released");
          }
        } catch (InterruptedException exception) {
          throw new IllegalStateException(exception);
        }
        return new Sample(sequence, "success", 200, 1, 0, "");
      }, sample -> {
        if (!sample.attempted()) {
          dropped.countDown();
        }
      }));
      final boolean enteredRequest;
      final boolean droppedRequest;
      try {
        enteredRequest = entered.await(3, TimeUnit.SECONDS);
        droppedRequest = dropped.await(3, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
      var result = future.get(3, TimeUnit.SECONDS);

      // then
      assertTrue(enteredRequest);
      assertTrue(droppedRequest);
      assertEquals(3, result.samples().size());
      assertTrue(result.dropped() >= 1);
      assertFalse(result.samples().stream().allMatch(Sample::successful));
    }
  }
}
