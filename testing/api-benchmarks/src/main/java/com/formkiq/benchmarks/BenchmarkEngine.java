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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/** Fixed arrival rate scheduler with bounded concurrency and no request backlog. */
public final class BenchmarkEngine {

  /** Operation executed in one exclusive worker lane. */
  @FunctionalInterface
  public interface Operation {
    /**
     * Perform one API request and validate its response.
     * 
     * @param lane exclusive lane number
     * @param sequence scheduled request number
     * @return measured response
     */
    Sample execute(int lane, int sequence);
  }

  /** Nanoseconds in one millisecond. */
  private static final double NANOS_PER_MS = 1_000_000.0;

  private static void await(final long deadline) throws InterruptedException {
    long remaining;
    while ((remaining = deadline - System.nanoTime()) > 0) {
      LockSupport.parkNanos(remaining);
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Benchmark interrupted");
      }
    }
  }

  /**
   * Run one cell without silently reducing load when requests become slow.
   * 
   * @param count number of scheduled requests
   * @param rate requests per second
   * @param concurrency maximum in-flight requests
   * @param operation API operation
   * @param observer receives completed and dropped samples
   * @return samples and elapsed time
   * @throws Exception if interrupted or a worker fails unexpectedly
   */
  public CellResult run(final int count, final int rate, final int concurrency,
      final Operation operation, final Consumer<Sample> observer) throws Exception {
    var lanes = new ArrayBlockingQueue<Integer>(concurrency);
    for (int lane = 0; lane < concurrency; lane++) {
      lanes.add(lane);
    }
    var samples = new ConcurrentLinkedQueue<Sample>();
    List<Future<?>> futures = new ArrayList<>();
    long started;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      started = System.nanoTime();
      for (int sequence = 0; sequence < count; sequence++) {
        long scheduled = started + TimeUnit.SECONDS.toNanos(1) * sequence / rate;
        await(scheduled);
        double delay = (System.nanoTime() - scheduled) / NANOS_PER_MS;
        Integer lane = lanes.poll();
        final int requestSequence = sequence;
        if (lane == null || delay > TimeUnit.SECONDS.toMillis(1) / (double) rate) {
          if (lane != null) {
            lanes.add(lane);
          }
          var sample = new Sample(sequence,
              lane == null ? "dropped_concurrency" : "dropped_scheduler", 0, 0, delay, "");
          samples.add(sample);
          observer.accept(sample);
        } else {
          futures.add(executor.submit(() -> {
            try {
              double actualDelay = (System.nanoTime() - scheduled) / NANOS_PER_MS;
              Sample result = operation.execute(lane, requestSequence);
              var sample = new Sample(result.sequence(), result.outcome(), result.httpStatus(),
                  result.latencyMs(), actualDelay, result.requestId());
              samples.add(sample);
              observer.accept(sample);
            } finally {
              lanes.add(lane);
            }
          }));
        }
      }
      for (Future<?> future : futures) {
        future.get();
      }
      await(started + TimeUnit.SECONDS.toNanos(1) * count / rate);
    }
    double elapsed = (System.nanoTime() - started) / (double) TimeUnit.SECONDS.toNanos(1);
    return new CellResult("", rate, 0, elapsed,
        samples.stream().sorted(Comparator.comparingInt(Sample::sequence)).toList());
  }
}
