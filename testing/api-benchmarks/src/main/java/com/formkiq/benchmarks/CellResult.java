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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Measurements for one scenario, offered request rate and repetition. */
public record CellResult(String scenario, int rate, int repetition, double elapsedSeconds,
    List<Sample> samples) {

  /** Median percentile. */
  private static final double MEDIAN = 0.50;
  /** 95th percentile. */
  private static final double P95 = 0.95;
  /** 99th percentile. */
  private static final double P99 = 0.99;

  /**
   * Calculate nearest-rank percentiles from attempted requests, including errors.
   * 
   * @param values latency values
   * @param percentile percentile from zero to one
   * @return percentile, or null for no observations
   */
  public static Double percentile(final List<Double> values, final double percentile) {
    if (values.isEmpty()) {
      return null;
    }
    List<Double> sorted = values.stream().sorted().toList();
    int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
    return sorted.get(index);
  }

  /**
   * Count requests with valid successful responses.
   * 
   * @return successful requests
   */
  public long successes() {
    return samples.stream().filter(Sample::successful).count();
  }

  /**
   * Count requests not sent by the generator.
   * 
   * @return dropped scheduled operations
   */
  public long dropped() {
    return samples.stream().filter(sample -> !sample.attempted()).count();
  }

  /**
   * Summarize measured traffic, preserving errors and generator drops.
   * 
   * @return aggregate statistics
   */
  public Map<String, Object> summary() {
    List<Double> latencies =
        samples.stream().filter(Sample::attempted).map(Sample::latencyMs).toList();
    List<Double> successLatencies =
        samples.stream().filter(Sample::successful).map(Sample::latencyMs).toList();
    Map<String, Object> values = new TreeMap<>();
    values.put("scenario", scenario);
    values.put("offeredRps", rate);
    values.put("repetition", repetition);
    values.put("elapsedSeconds", elapsedSeconds);
    values.put("scheduled", samples.size());
    values.put("attempted", latencies.size());
    values.put("successes", successes());
    values.put("errors", samples.size() - dropped() - successes());
    values.put("dropped", dropped());
    values.put("successfulRps", successes() / elapsedSeconds);
    values.put("p50Ms", percentile(latencies, MEDIAN));
    values.put("p95Ms", percentile(latencies, P95));
    values.put("p99Ms", percentile(latencies, P99));
    values.put("successP95Ms", percentile(successLatencies, P95));
    values.put("schedulingDelayP95Ms",
        percentile(samples.stream().map(Sample::schedulingDelayMs).toList(), P95));
    Map<String, Long> outcomes = new TreeMap<>();
    samples.forEach(sample -> outcomes.merge(sample.outcome(), 1L, Long::sum));
    values.put("outcomes", outcomes);
    return values;
  }
}
