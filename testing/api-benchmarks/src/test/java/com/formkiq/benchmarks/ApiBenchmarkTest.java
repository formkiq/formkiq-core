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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Explicitly enabled benchmark against one deployed FormKiQ instance. */
@EnabledIfSystemProperty(named = "benchmarkUrl", matches = ".+")
class ApiBenchmarkTest {

  @Test
  @Timeout(value = 2, unit = TimeUnit.HOURS)
  void benchmarkInstance() throws Exception {
    // given
    var config = BenchmarkConfig.from(System.getProperties());
    var run = new BenchmarkRun(config);
    var client = BenchmarkClient.create(config, System.getProperties(), System.getenv());

    // when
    run.execute(client);
    Logger.getLogger(ApiBenchmarkTest.class.getName())
        .info(() -> "Benchmark report: " + run.directory().resolve("report.html"));

    // then
    assertTrue(run.successful(),
        () -> "Benchmark failed; inspect " + run.directory().resolve("report.html"));
  }
}
