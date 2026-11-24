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

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** Standalone HTML summary, structured JSON aggregates and raw CSV samples. */
public final class BenchmarkReport {

  private static String escape(final Object value) {
    return value == null ? "—"
        : value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
  }

  private void configuration(final StringBuilder body, final Map<String, Object> document) {
    body.append("<section><h2>Configuration</h2><dl><dt>FormKiQ version</dt><dd>")
        .append(escape(document.get("formkiqVersion"))).append("</dd>");
    Object configuration = document.get("configuration");
    if (configuration instanceof Map<?, ?> values) {
      values.forEach((name, value) -> body.append("<dt>").append(escape(name)).append("</dt><dd>")
          .append(escape(value)).append("</dd>"));
    }
    body.append("</dl></section>");
  }

  private String html(final BenchmarkRun run) {
    Map<String, Object> document = run.document();
    var body = new StringBuilder("""
        <!doctype html><html lang="en"><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>FormKiQ API benchmark</title><style>
        body{font:15px system-ui,sans-serif;color:#182c3c;background:#f3f6f8;margin:0}
        main{max-width:1400px;margin:auto;padding:32px}h1{font-size:32px;margin:8px 0}
        h2{font-size:20px;margin-top:28px}p{line-height:1.6}.muted{color:#50667a}
        section{background:white;padding:24px;border-radius:12px;margin:20px 0;overflow:auto}
        table{border-collapse:collapse;width:100%;font-size:13px}th,td{text-align:right;
        padding:12px 9px;border-bottom:1px solid #dce4ea;white-space:nowrap}
        th:first-child,td:first-child{text-align:left}th{background:#edf3f7}
        dt{font-weight:600}dd{margin:4px 0 14px;overflow-wrap:anywhere}
        .pass{color:#087751}.fail{color:#ae3024}code{overflow-wrap:anywhere}
        a{color:#176a9b}.bar{height:6px;background:#238aaf;border-radius:3px;margin-top:6px}
        </style><main><p class="muted">FORMKIQ / AWS API BENCHMARK</p>
        <h1>Instance performance report</h1>
        """);
    body.append("<p class=\"").append(run.successful() ? "pass" : "fail").append("\"><b>")
        .append(run.successful() ? "PASSED" : "FAILED / INCOMPLETE").append("</b> · ")
        .append(escape(document.get("runId"))).append("</p>");
    body.append("<p><a href=\"results.json\">JSON results</a> · ")
        .append("<a href=\"samples.csv\">Raw request samples</a> · ")
        .append("<a href=\"documents.txt\">Document manifest</a></p>");
    configuration(body, document);
    measurements(body, run.cells());
    body.append("<section><h2>Failures and cleanup</h2><pre>")
        .append(escape("Setup/execution failure: " + document.get("failure")
            + "\nWarm-up failures: " + document.get("warmupFailures")
            + "\nDocuments requiring cleanup: " + document.get("remainingDocuments")
            + "\nAttributes requiring cleanup: " + document.get("remainingAttributes")))
        .append("</pre></section>");
    body.append(notes());
    return body.toString();
  }

  private void measurements(final StringBuilder body, final List<CellResult> cells) {
    body.append("""
        <section><h2>Measured API operations</h2><table><thead><tr><th>Scenario</th>
        <th>Target RPS</th><th>Repeat</th><th>Sent</th><th>Success</th><th>Errors</th>
        <th>Drops</th><th>Success RPS</th><th>p50 ms</th><th>p95 ms</th><th>p99 ms</th>
        <th>Success p95 ms</th><th>Schedule p95 ms</th></tr></thead><tbody>
        """);
    for (CellResult cell : cells) {
      Map<String, Object> summary = cell.summary();
      body.append("<tr><td>").append(escape(cell.scenario())).append("</td>");
      for (String key : List.of("offeredRps", "repetition", "attempted", "successes", "errors",
          "dropped", "successfulRps", "p50Ms", "p95Ms", "p99Ms", "successP95Ms",
          "schedulingDelayP95Ms")) {
        Object value = summary.get(key);
        body.append("<td>")
            .append(value instanceof Double number ? String.format(Locale.ROOT, "%.2f", number)
                : escape(value))
            .append("</td>");
      }
      body.append("</tr>");
    }
    body.append("</tbody></table></section>");
  }

  private String notes() {
    return """
        <section><h2>How to read these results</h2>
        <p>Requests use a fixed arrival rate and bounded concurrency. Drops are
        scheduled requests
        the generator could not send; they count against success. Throughput
        includes the final
        response drain. Latency measures a synchronous SDK operation and response
        validation,
        including serialization and deserialization, excluding fixture setup, warm-
        up and cleanup.
        p50/p95/p99 use all attempted operations, including failures; success p95
        excludes errors.
        Warm-up failures are reported separately and fail the run.</p>
        <p>Each row is one repetition. Compare matching scenario, rate, payload,
        concurrency,
        revision and deployment settings across saved architecture runs.
        Architecture is a supplied
        label. The report does not verify AWS deployment equivalence, identify cold
        starts, collect
        Lambda billed duration or estimate AWS cost. Background processing
        completion is only
        checked during fixture setup; measured writes report API acceptance
        latency.</p>
        <p>Tail percentiles from fewer than 10,000 attempted requests per cell are
        exploratory.
        Keep repeated runs separate when assessing variance. These measurements
        include network
        and generator overhead. A run with drops cannot demonstrate the requested
        sustainable
        throughput. Cleanup removes only documents and attributes allocated by this
        run; failed or ambiguous
        creates remain listed for cleanup if deletion
        fails.</p></section></main></html>
        """;
  }

  /**
   * Write all report artifacts, including failures and cleanup information.
   * 
   * @param run completed or failed run
   * @throws IOException if output cannot be persisted
   */
  public void write(final BenchmarkRun run) throws IOException {
    Map<String, Object> document = run.document();
    Files.writeString(run.directory().resolve("results.json"),
        new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(document));
    writeSamples(run);
    Files.writeString(run.directory().resolve("report.html"), html(run));
  }

  private void writeSamples(final BenchmarkRun run) throws IOException {
    try (var writer = Files.newBufferedWriter(run.directory().resolve("samples.csv"));
        var printer = new CSVPrinter(writer,
            CSVFormat.DEFAULT.builder().setHeader("scenario", "offeredRps", "repetition",
                "sequence", "outcome", "httpStatus", "latencyMs", "schedulingDelayMs", "requestId")
                .get())) {
      for (CellResult cell : run.cells()) {
        for (Sample sample : cell.samples()) {
          printer.printRecord(cell.scenario(), cell.rate(), cell.repetition(), sample.sequence(),
              sample.outcome(), sample.httpStatus() == 0 ? "" : sample.httpStatus(),
              sample.attempted() ? sample.latencyMs() : "", sample.schedulingDelayMs(),
              sample.requestId());
        }
      }
    }
  }
}
