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
import com.formkiq.client.invoker.ApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the real request builders against a local API fixture, without AWS credentials. */
@Timeout(30)
class BenchmarkRunTest {

  /** Local FormKiQ-shaped API with observable document ownership. */
  private static final class Instance implements AutoCloseable {
    /** Response for injected create status. */
    private record Created(int status, String body) {
    }

    /** Server bound only to loopback. */
    private final HttpServer server;
    /** In-memory documents, including an unrelated document that must survive. */
    private final Map<String, JsonObject> documents = new ConcurrentHashMap<>();
    /** Attribute definitions, including an unrelated definition that must survive. */
    private final Map<String, JsonObject> attributes = new ConcurrentHashMap<>();
    /** Create endpoint status used to inject failures. */
    private final AtomicInteger createStatus = new AtomicInteger(201);
    /** Get metadata endpoint status for measured failure injection. */
    private final AtomicInteger getStatus = new AtomicInteger(200);
    /** Delete status for testing incomplete cleanup. */
    private final AtomicInteger deleteStatus = new AtomicInteger(200);
    /** Last Authorization header observed on the wire. */
    private final AtomicReference<String> authorization = new AtomicReference<>();

    /** Whether a failed create still commits a document. */
    private boolean ambiguousCreate;

    Instance() throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      documents.put("unrelated", new JsonObject());
      attributes.put("unrelated", new JsonObject());
      server.createContext("/", this::handle);
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.start();
    }

    private Created attribute(final HttpExchange exchange) throws IOException {
      if ("POST".equals(exchange.getRequestMethod())) {
        var request = new Gson().fromJson(
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        JsonObject definition = request.getAsJsonObject("attribute");
        attributes.put(definition.get("key").getAsString(), definition);
        return new Created(201, "{}");
      }
      String path = exchange.getRequestURI().getPath();
      String key = path.substring(path.lastIndexOf('/') + 1);
      int status = deleteStatus.get();
      if ("DELETE".equals(exchange.getRequestMethod())) {
        if (status == 200) {
          status = attributes.remove(key) == null ? 404 : 200;
        }
        return new Created(status, "{}");
      }
      return new Created(attributes.containsKey(key) ? 200 : 404,
          new Gson().toJson(Map.of("attribute", attributes.getOrDefault(key, new JsonObject()))));
    }

    @Override
    public void close() {
      server.stop(0);
      ((java.util.concurrent.ExecutorService) server.getExecutor()).close();
    }

    private Created create(final HttpExchange exchange) throws IOException {
      var request = new Gson().fromJson(
          new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
          JsonObject.class);
      String id = request.get("documentId").getAsString();
      for (var value : request.getAsJsonArray("attributes")) {
        String key = value.getAsJsonObject().get("key").getAsString();
        if (!attributes.containsKey(key)) {
          return new Created(400, new Gson().toJson(Map.of("message", "Attribute not found")));
        }
      }
      int status = createStatus.get();
      if (status < 300 || ambiguousCreate) {
        documents.put(id, request);
      }
      return new Created(status, "{\"documentId\":\"" + id + "\"}");
    }

    private int delete(final String id) {
      int status = deleteStatus.get();
      if (status == 200) {
        status = documents.remove(id) == null ? 404 : 200;
      }
      return status;
    }

    String endpoint() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void handle(final HttpExchange exchange) throws IOException {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      String path = exchange.getRequestURI().getPath();
      String method = exchange.getRequestMethod();
      String id = path.substring(path.lastIndexOf('/') + 1);
      String body;
      int status = 200;
      if (path.startsWith("/attributes")) {
        var result = attribute(exchange);
        status = result.status();
        body = result.body();
      } else if ("/version".equals(path)) {
        body = "{\"version\":\"test-version\"}";
      } else if ("/documents".equals(path) && "POST".equals(method)) {
        var created = create(exchange);
        status = created.status();
        body = created.body();
      } else if ("/documents".equals(path) || "/search".equals(path)) {
        body = new Gson().toJson(Map.of("documents",
            documents.keySet().stream().filter(documentId -> !"unrelated".equals(documentId))
                .map(documentId -> Map.of("documentId", documentId)).toList()));
      } else if (path.endsWith("/content")) {
        String documentId = path.split("/")[2];
        body = "{\"content\":" + documents.get(documentId).get("content") + "}";
      } else if ("DELETE".equals(method)) {
        status = delete(id);
        body = "{\"message\":\"deleted\"}";
      } else if ("PATCH".equals(method)) {
        body = "{\"documentId\":\"" + id + "\"}";
      } else {
        status = getStatus.get();
        body = "{\"documentId\":\"" + id + "\"}";
      }
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.getResponseHeaders().set("x-amzn-requestid", "fixture-request");
      exchange.sendResponseHeaders(status, bytes.length);
      try (var output = exchange.getResponseBody()) {
        output.write(bytes);
      }
      exchange.close();
    }
  }

  /** Temporary output root. */
  @TempDir
  private Path directory;

  private ApiClient client(final BenchmarkConfig config) {
    return BenchmarkClient.create(config, new Properties(),
        Map.of("FORMKIQ_BENCHMARK_TOKEN", "never-persist-this-token"));
  }

  private BenchmarkConfig configuration(final Instance instance) {
    var properties = new Properties();
    properties.setProperty("benchmarkUrl", instance.endpoint());
    properties.setProperty("benchmarkReportDir", directory.toString());
    properties.setProperty("benchmarkArchitecture", "arm64");
    properties.setProperty("benchmarkLabel", "<test instance>");
    properties.setProperty("benchmarkRequests", "2");
    properties.setProperty("benchmarkWarmup", "1");
    properties.setProperty("benchmarkRepetitions", "1");
    properties.setProperty("benchmarkConcurrency", "2");
    properties.setProperty("benchmarkFixtures", "2");
    properties.setProperty("benchmarkRates", "10");
    return BenchmarkConfig.from(properties);
  }

  @Test
  void doesNotAssumeAmbiguousCreateIsFinishedWhenDeletionReturnsNotFound() throws Exception {
    // given
    try (var instance = new Instance()) {
      instance.createStatus.set(500);
      var config = configuration(instance);
      var run = new BenchmarkRun(config);

      // when
      run.execute(client(config));
      var report = new Gson().fromJson(Files.readString(run.directory().resolve("results.json")),
          JsonObject.class);

      // then
      assertFalse(run.successful());
      assertEquals(1, report.getAsJsonArray("remainingDocuments").size());
      assertEquals(1, instance.documents.size());
    }
  }

  @Test
  void listsUnresolvedDocumentsWhenCleanupFails() throws Exception {
    // given
    try (var instance = new Instance()) {
      instance.deleteStatus.set(403);
      var config = configuration(instance);
      var run = new BenchmarkRun(config);

      // when
      run.execute(client(config));
      var report = new Gson().fromJson(Files.readString(run.directory().resolve("results.json")),
          JsonObject.class);

      // then
      assertFalse(run.successful());
      assertEquals(1, report.getAsJsonArray("remainingAttributes").size());
      assertTrue(report.getAsJsonArray("remainingDocuments").size() > 0);
      assertTrue(Files.exists(run.directory().resolve("report.html")));
      assertTrue(instance.documents.containsKey("unrelated"));
    }
  }

  @Test
  void preservesReportAndCleansUpAmbiguousCreateAfterSetupFails() throws Exception {
    // given
    try (var instance = new Instance()) {
      instance.createStatus.set(500);
      instance.ambiguousCreate = true;
      var config = configuration(instance);
      var run = new BenchmarkRun(config);

      // when
      run.execute(client(config));
      final String json = Files.readString(run.directory().resolve("results.json"));

      // then
      assertFalse(run.successful());
      assertTrue(json.contains("\"failure\": \"setup: ApiException\""));
      assertTrue(Files.exists(run.directory().resolve("report.html")));
      assertEquals(1, Files.readAllLines(run.directory().resolve("documents.txt")).size());
      assertEquals(1, instance.documents.size());
      assertTrue(instance.documents.containsKey("unrelated"));
    }
  }

  @Test
  void retainsHttpErrorsAndWarmupFailuresInReport() throws Exception {
    // given
    try (var instance = new Instance()) {
      instance.getStatus.set(429);
      var config = configuration(instance);
      var run = new BenchmarkRun(config);

      // when
      run.execute(client(config));
      var cell = run.cells().stream().filter(result -> "GET_DOCUMENT".equals(result.scenario()))
          .findFirst().orElseThrow();
      final String json = Files.readString(run.directory().resolve("results.json"));

      // then
      assertFalse(run.successful());
      assertEquals(2L, cell.summary().get("errors"));
      assertTrue(cell.samples().stream().allMatch(sample -> sample.httpStatus() == 429));
      assertTrue(json.contains("GET_DOCUMENT:http_429"));
      assertEquals(1, instance.documents.size());
    }
  }

  @Test
  void runsScenariosWritesReportsAndCleansUpOnlyOwnedDocuments() throws Exception {
    // given
    try (var instance = new Instance()) {
      var config = configuration(instance);
      var run = new BenchmarkRun(config);

      // when
      run.execute(client(config));
      final String json = Files.readString(run.directory().resolve("results.json"));
      final String html = Files.readString(run.directory().resolve("report.html"));
      final String csv = Files.readString(run.directory().resolve("samples.csv"));

      // then
      assertTrue(run.successful(), json);
      assertEquals(7, run.cells().size());
      assertEquals(1, instance.attributes.size());
      assertTrue(instance.attributes.containsKey("unrelated"));
      assertEquals(1, instance.documents.size());
      assertTrue(instance.documents.containsKey("unrelated"));
      assertTrue(html.contains("&lt;test instance&gt;"));
      assertFalse(html.contains("never-persist-this-token"));
      assertFalse(json.contains("never-persist-this-token"));
      assertTrue(csv.contains("fixture-request"));
      assertTrue(json.contains("\"formkiqVersion\": \"test-version\""));
      assertEquals(15, csv.lines().count());
    }
  }

  @Test
  void sendsApiKeyInFormkiqAuthorizationHeader() throws Exception {
    // given
    try (var instance = new Instance()) {
      var config = configuration(instance);
      var run = new BenchmarkRun(config);
      var apiClient = BenchmarkClient.create(config, new Properties(),
          Map.of("FORMKIQ_BENCHMARK_API_KEY", "test-api-key"));

      // when
      run.execute(apiClient);
      final String json = Files.readString(run.directory().resolve("results.json"));

      // then
      assertTrue(run.successful(), json);
      assertEquals("test-api-key", instance.authorization.get());
      assertFalse(json.contains("test-api-key"));
    }
  }
}
