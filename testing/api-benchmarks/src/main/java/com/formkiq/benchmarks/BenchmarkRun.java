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

import com.formkiq.client.invoker.ApiClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Runs an instance benchmark and preserves a report even when setup or cleanup fails. */
public final class BenchmarkRun {

  /** Unique run identifier, safe as a directory name. */
  private final String runId = Instant.now().toString().replace(":", "-") + "-"
      + UUID.randomUUID().toString().substring(0, 8);
  /** Benchmark configuration. */
  private final BenchmarkConfig config;
  /** Independently measured cells. */
  private final List<CellResult> cells = new ArrayList<>();
  /** Warm-up errors; not included in latency statistics. */
  private final ConcurrentLinkedQueue<String> warmupFailures = new ConcurrentLinkedQueue<>();
  /** Documents requiring manual cleanup. */
  private List<String> remainingDocuments = List.of();
  /** Attribute definitions requiring manual cleanup. */
  private List<String> remainingAttributes = List.of();
  /** Sanitized fatal failure; never includes API bodies or credential material. */
  private String failure = "";
  /** FormKiQ version returned by the target. */
  private String version = "unknown";
  /** Current phase for sanitized failure diagnostics. */
  private String stage = "setup";

  /**
   * Create a run and its output directory.
   * 
   * @param benchmarkConfig configuration
   * @throws IOException if the output directory cannot be created
   */
  public BenchmarkRun(final BenchmarkConfig benchmarkConfig) throws IOException {
    this.config = benchmarkConfig;
    Files.createDirectories(directory());
    Files.createFile(directory().resolve("documents.txt"));
    Files.createFile(directory().resolve("attributes.txt"));
  }

  /**
   * Obtain the measured cells.
   * 
   * @return immutable cells
   */
  public List<CellResult> cells() {
    return List.copyOf(cells);
  }

  /**
   * Find the report output directory.
   * 
   * @return unique directory for this run
   */
  public Path directory() {
    return config.reportRoot().resolve(runId);
  }

  /**
   * Build the machine-readable report, excluding raw request samples.
   * 
   * @return sanitized report document
   */
  public Map<String, Object> document() {
    return Map.of("schemaVersion", 1, "runId", runId, "configuration", config.metadata(),
        "formkiqVersion", version, "successful", successful(), "failure", failure, "warmupFailures",
        List.copyOf(warmupFailures), "remainingDocuments", remainingDocuments, "results",
        cells.stream().map(CellResult::summary).toList(), "remainingAttributes",
        remainingAttributes);
  }

  /**
   * Run all scenarios and always write results after cleanup.
   * 
   * @param client authenticated, reusable client
   * @throws IOException if report files cannot be written
   */
  public void execute(final ApiClient client) throws IOException {
    var workload = new ApiWorkload(config, client, runId, directory());
    try {
      version = workload.prepare();
      for (int repetition = 1; repetition <= config.repetitions(); repetition++) {
        for (Scenario scenario : config.scenarios()) {
          for (int rate : config.rates()) {
            measure(workload, scenario, rate, repetition);
          }
        }
      }
    } catch (Exception exception) {
      failure = stage + ": " + exception.getClass().getSimpleName();
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    } finally {
      boolean interrupted = Thread.interrupted();
      try {
        remainingDocuments = workload.cleanup();
        remainingAttributes = workload.remainingAttributes();
        new BenchmarkReport().write(this);
      } finally {
        client.getHttpClient().connectionPool().evictAll();
        client.getHttpClient().dispatcher().executorService().shutdown();
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private void measure(final ApiWorkload workload, final Scenario scenario, final int rate,
      final int repetition) throws Exception {
    var engine = new BenchmarkEngine();
    var operation =
        (BenchmarkEngine.Operation) (lane, sequence) -> workload.execute(scenario, lane, sequence);
    if (config.warmup() > 0) {
      stage = "warmup " + scenario + " @ " + rate + " RPS";
      engine.run(config.warmup(), rate, config.concurrency(), operation, sample -> {
        if (!sample.successful()) {
          warmupFailures.add(
              scenario + ":" + sample.outcome() + " rate=" + rate + " repetition=" + repetition);
        }
      });
      restoreFixtures(workload);
    }
    stage = "measurement " + scenario + " @ " + rate + " RPS";
    CellResult result =
        engine.run(config.requests(), rate, config.concurrency(), operation, sample -> {
        });
    cells.add(new CellResult(scenario.name(), rate, repetition, result.elapsedSeconds(),
        result.samples()));
    restoreFixtures(workload);
  }

  private void restoreFixtures(final ApiWorkload workload) {
    stage = "restore fixtures";
    if (!workload.cleanupWrites().isEmpty()) {
      throw new IllegalStateException("Unable to restore fixture dataset");
    }
  }

  /**
   * Determine whether setup, warm-up, measured traffic and cleanup all succeeded.
   * 
   * @return whether this is a completely successful run
   */
  public boolean successful() {
    boolean complete =
        failure.isEmpty() && warmupFailures.isEmpty() && remainingDocuments.isEmpty();
    complete = complete && remainingAttributes.isEmpty();
    return complete && !cells.isEmpty()
        && cells.stream().allMatch(cell -> cell.successes() == cell.samples().size());
  }
}
