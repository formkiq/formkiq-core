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

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Non-secret, reproducible inputs for one instance benchmark. */
public record BenchmarkConfig(URI endpoint, String label, String architecture, String siteId,
    List<Scenario> scenarios, List<Integer> rates, int requests, int warmup, int repetitions,
    int concurrency, int fixtures, int payloadBytes, int timeoutSeconds, Path reportRoot,
    String revision) {

  /** Maximum number of scheduled operations per run, including warm-up. */
  private static final long MAX_OPERATIONS = 1_000_000;
  /** Default measured request count. */
  private static final int DEFAULT_REQUESTS = 100;
  /** Maximum measured request count per cell. */
  private static final int MAX_REQUESTS = 100_000;
  /** Default warm-up count. */
  private static final int DEFAULT_WARMUP = 10;
  /** Maximum warm-up count and request rate. */
  private static final int MAX_RATE = 10_000;
  /** Default repetition count. */
  private static final int DEFAULT_REPETITIONS = 3;
  /** Maximum repetition count. */
  private static final int MAX_REPETITIONS = 20;
  /** Default concurrency. */
  private static final int DEFAULT_CONCURRENCY = 16;
  /** Maximum concurrency. */
  private static final int MAX_CONCURRENCY = 256;
  /** Default fixture count. */
  private static final int DEFAULT_FIXTURES = 100;
  /** Default payload size. */
  private static final int DEFAULT_PAYLOAD_BYTES = 1024;
  /** Maximum payload size. */
  private static final int MAX_PAYLOAD_BYTES = 1_048_576;
  /** Default whole-call timeout. */
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;
  /** Maximum whole-call timeout. */
  private static final int MAX_TIMEOUT_SECONDS = 120;

  /**
   * Parse and validate the Gradle test JVM properties.
   * 
   * @param properties non-secret JVM properties
   * @return validated configuration
   */
  public static BenchmarkConfig from(final Properties properties) {
    URI endpoint = URI.create(properties.getProperty("benchmarkUrl", "").replaceAll("/+$", ""));
    validateEndpoint(endpoint);
    String architecture = properties.getProperty("benchmarkArchitecture", "unknown");
    if (!List.of("unknown", "x86_64", "arm64").contains(architecture)) {
      throw new IllegalArgumentException("benchmarkArchitecture must be unknown, x86_64 or arm64");
    }
    List<Scenario> scenarios = Arrays.stream(properties.getProperty("benchmarkScenarios",
        "version,list_documents,get_document,get_content,search,create_document,update_document")
        .split(",", -1)).map(String::trim)
        .map(value -> Scenario.valueOf(value.toUpperCase(Locale.ROOT))).distinct().toList();
    List<Integer> rates = Arrays
        .stream(properties.getProperty("benchmarkRates", "10,50").split(",", -1)).map(String::trim)
        .map(value -> bounded("benchmarkRates", value, 1, MAX_RATE)).distinct().toList();
    var config = new BenchmarkConfig(endpoint,
        properties.getProperty("benchmarkLabel", architecture), architecture,
        properties.getProperty("benchmarkSiteId", "default"), scenarios, rates,
        number(properties, "benchmarkRequests", DEFAULT_REQUESTS, 1, MAX_REQUESTS),
        number(properties, "benchmarkWarmup", DEFAULT_WARMUP, 0, MAX_RATE),
        number(properties, "benchmarkRepetitions", DEFAULT_REPETITIONS, 1, MAX_REPETITIONS),
        number(properties, "benchmarkConcurrency", DEFAULT_CONCURRENCY, 1, MAX_CONCURRENCY),
        number(properties, "benchmarkFixtures", DEFAULT_FIXTURES, 1, MAX_RATE),
        number(properties, "benchmarkPayloadBytes", DEFAULT_PAYLOAD_BYTES, 1, MAX_PAYLOAD_BYTES),
        number(properties, "benchmarkTimeoutSeconds", DEFAULT_TIMEOUT_SECONDS, 1,
            MAX_TIMEOUT_SECONDS),
        Path.of(properties.getProperty("benchmarkReportDir", "build/reports/api-benchmarks")),
        properties.getProperty("benchmarkRevision", "unspecified"));
    long operations = ((long) config.requests() + config.warmup()) * scenarios.size() * rates.size()
        * config.repetitions();
    if (operations > MAX_OPERATIONS || config.label().isBlank() || config.siteId().isBlank()) {
      throw new IllegalArgumentException(
          "Run must have a label/site and at most 1000000 operations");
    }
    if (config.fixtures() < config.concurrency()) {
      throw new IllegalArgumentException("benchmarkFixtures must be at least benchmarkConcurrency");
    }
    return config;
  }

  private static int bounded(final String name, final String value, final int minimum,
      final int maximum) {
    int parsed = Integer.parseInt(value);
    if (parsed < minimum || parsed > maximum) {
      throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
    }
    return parsed;
  }

  private static int number(final Properties properties, final String name, final int fallback,
      final int minimum, final int maximum) {
    return bounded(name, properties.getProperty(name, String.valueOf(fallback)), minimum, maximum);
  }

  private static void validateEndpoint(final URI endpoint) {
    boolean secure = "https".equals(endpoint.getScheme());
    boolean local = "http".equals(endpoint.getScheme()) && endpoint.getHost() != null
        && List.of("localhost", "127.0.0.1", "[::1]").contains(endpoint.getHost());
    boolean credentialsOrParameters = endpoint.getRawUserInfo() != null
        || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null;
    boolean invalidAddress = (!secure && !local) || endpoint.getHost() == null;
    if (invalidAddress || credentialsOrParameters) {
      throw new IllegalArgumentException("benchmarkUrl requires HTTPS (or HTTP on loopback),"
          + " without credentials, query parameters or fragments");
    }
  }

  /**
   * Produce report metadata without credentials or local filesystem paths.
   * 
   * @return non-secret settings
   */
  public Map<String, Object> metadata() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("endpoint", endpoint.toString());
    values.put("label", label);
    values.put("architecture", architecture);
    values.put("architectureSource", "user supplied; not verified against AWS");
    values.put("siteId", siteId);
    values.put("scenarios", scenarios);
    values.put("rates", rates);
    values.put("requestsPerCell", requests);
    values.put("warmupPerCell", warmup);
    values.put("repetitions", repetitions);
    values.put("concurrency", concurrency);
    values.put("fixtures", fixtures);
    values.put("payloadBytes", payloadBytes);
    values.put("timeoutSeconds", timeoutSeconds);
    values.put("revision", revision);
    values.put("javaVersion", System.getProperty("java.version"));
    values.put("generatorOs", System.getProperty("os.name"));
    values.put("generatorArchitecture", System.getProperty("os.arch"));
    return values;
  }
}
