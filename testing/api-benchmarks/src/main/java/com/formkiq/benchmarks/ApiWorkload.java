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
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.testutils.api.VersionRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentContentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentsRequestBuilder;
import com.formkiq.testutils.api.documents.SearchDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentRequestBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** API scenarios and a manifest of documents owned exclusively by this run. */
public final class ApiWorkload {

  /** Selected response metadata; response bodies and credentials are never recorded. */
  private record ResponseInfo(int status, String requestId) {
  }

  /** Nanoseconds in one millisecond. */
  private static final double NANOS_PER_MS = 1_000_000.0;
  /** Retry interval for setup readiness. */
  private static final int READINESS_POLL_MS = 100;
  /** HTTP not-found status, also valid after an ambiguous create. */
  private static final int NOT_FOUND = 404;
  /** First HTTP status whose create result can be ambiguous. */
  private static final int SERVER_ERROR = 500;
  /** Page size for list/search scenarios. */
  private static final int PAGE_SIZE = 20;

  private static void require(final boolean valid) {
    if (!valid) {
      throw new IllegalStateException("API response failed correctness validation");
    }
  }

  /** Benchmark configuration. */
  private final BenchmarkConfig config;
  /** Shared connection pool, configured before concurrent operations start. */
  private final ApiClient client;
  /** Unique attribute value and path prefix. */
  private final String runId;
  /** Deterministic text content. */
  private final String content;
  /** Persisted cleanup manifest, including potentially ambiguous create responses. */
  private final Path manifest;
  /** Every document ID allocated before submitting a create request. */
  private final Set<String> ownedIds = ConcurrentHashMap.newKeySet();
  /** Failed creates that may still commit after the client receives an error or timeout. */
  private final Set<String> ambiguousCreates = ConcurrentHashMap.newKeySet();
  /** Creates from the current cell to remove before the next measurement. */
  private final ConcurrentLinkedQueue<String> cellWrites = new ConcurrentLinkedQueue<>();

  /** One mutable fixture per exclusive worker lane. */
  private final List<String> fixtures = new ArrayList<>();

  /** Response metadata isolated per synchronous request thread. */
  private final ThreadLocal<ResponseInfo> responseInfo = new ThreadLocal<>();

  /**
   * Prepare workload state and install HTTP status/request-ID capture.
   * 
   * @param benchmarkConfig configuration
   * @param apiClient authenticated API client
   * @param benchmarkRunId unique run identifier
   * @param reportDirectory directory containing the fixture manifest
   */
  public ApiWorkload(final BenchmarkConfig benchmarkConfig, final ApiClient apiClient,
      final String benchmarkRunId, final Path reportDirectory) {
    this.config = benchmarkConfig;
    this.client = apiClient;
    this.runId = benchmarkRunId;
    this.content = "x".repeat(config.payloadBytes());
    this.manifest = reportDirectory.resolve("documents.txt");
    client.setHttpClient(client.getHttpClient().newBuilder().addInterceptor(chain -> {
      var response = chain.proceed(chain.request());
      responseInfo.set(new ResponseInfo(response.code(),
          Objects.toString(response.header("x-amzn-requestid"), "")));
      return response;
    }).build());
  }

  private synchronized String allocateDocument() throws IOException {
    String id = UUID.randomUUID().toString();
    Files.writeString(manifest, id + System.lineSeparator(), StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
    ownedIds.add(id);
    return id;
  }

  private void awaitContent(final String id) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(config.timeoutSeconds());
    while (true) {
      var response = new GetDocumentContentRequestBuilder(id).submit(client, config.siteId());
      if (!response.isError() && response.response() != null
          && content.equals(response.response().getContent())) {
        break;
      }
      if (System.nanoTime() >= deadline) {
        throw new IllegalStateException("Fixture content did not become ready");
      }
      TimeUnit.MILLISECONDS.sleep(READINESS_POLL_MS);
    }
  }

  private void awaitSearch() throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(config.timeoutSeconds());
    while (!execute(Scenario.SEARCH, 0, 0).successful()) {
      if (System.nanoTime() >= deadline) {
        throw new IllegalStateException("Fixture search did not become ready");
      }
      TimeUnit.MILLISECONDS.sleep(READINESS_POLL_MS);
    }
  }

  /**
   * Remove only documents whose IDs were allocated by this run.
   * 
   * @return IDs requiring manual cleanup
   */
  public List<String> cleanup() {
    return deleteDocuments(List.copyOf(ownedIds));
  }

  /**
   * Restore the original fixture population between warm-up and measurement cells.
   * 
   * @return IDs that could not be removed
   */
  public List<String> cleanupWrites() {
    List<String> ids = new ArrayList<>();
    String id;
    while ((id = cellWrites.poll()) != null) {
      ids.add(id);
    }
    return deleteDocuments(ids);
  }

  private void create(final String id) throws ApiException {
    try {
      var response = new AddDocumentRequestBuilder().documentId(id)
          .path("api-benchmark-" + runId + "-" + id + ".txt").content(content)
          .contentType("text/plain").addAttribute("benchmarkRunId", runId)
          .submitOk(client, config.siteId()).response();
      require(response != null && id.equals(response.getDocumentId()));
    } catch (ApiException exception) {
      if (exception.getCode() <= 0 || exception.getCode() >= SERVER_ERROR) {
        ambiguousCreates.add(id);
      }
      throw exception;
    }
  }

  private List<String> deleteDocuments(final Collection<String> ids) {
    List<String> remaining = new ArrayList<>();
    for (String id : ids) {
      try {
        var result =
            new DeleteDocumentRequestBuilder(id).softDelete(false).submit(client, config.siteId());
        boolean missing = result.isError() && result.exception().getCode() == NOT_FOUND;
        boolean unresolved = missing && ambiguousCreates.contains(id);
        if ((result.isError() && !missing) || unresolved) {
          remaining.add(id);
        } else {
          ownedIds.remove(id);
        }
      } catch (RuntimeException exception) {
        remaining.add(id);
      }
    }
    return remaining;
  }

  /**
   * Perform and time one request; never retry failed measured requests.
   * 
   * @param scenario operation
   * @param lane exclusive fixture lane
   * @param sequence scheduled operation number
   * @return latency, outcome and HTTP metadata
   */
  public Sample execute(final Scenario scenario, final int lane, final int sequence) {
    String id;
    try {
      id = scenario == Scenario.CREATE_DOCUMENT ? allocateDocument()
          : fixtureId(scenario, lane, sequence);
      if (scenario == Scenario.CREATE_DOCUMENT) {
        cellWrites.add(id);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to persist cleanup manifest", exception);
    }
    responseInfo.remove();
    long started = System.nanoTime();
    String outcome = "success";
    try {
      invoke(scenario, id, sequence);
    } catch (ApiException exception) {
      outcome = exception.getCode() > 0 ? "http_" + exception.getCode() : "transport_error";
    } catch (RuntimeException exception) {
      outcome = "validation_or_client_error";
    }
    double elapsed = (System.nanoTime() - started) / NANOS_PER_MS;
    ResponseInfo info = responseInfo.get();
    responseInfo.remove();
    return new Sample(sequence, outcome, info == null ? 0 : info.status(), elapsed, 0,
        info == null ? "" : info.requestId());
  }

  private String fixtureId(final Scenario scenario, final int lane, final int sequence) {
    return fixtures.isEmpty() ? ""
        : fixtures.get(scenario == Scenario.UPDATE_DOCUMENT ? lane : sequence % fixtures.size());
  }

  private void invoke(final Scenario scenario, final String id, final int sequence)
      throws ApiException {
    String site = config.siteId();
    switch (scenario) {
      case VERSION -> {
        var response = new VersionRequestBuilder().submitOk(client, site).response();
        require(response != null && response.getVersion() != null);
      }
      case LIST_DOCUMENTS -> {
        var response =
            new GetDocumentsRequestBuilder().limit(PAGE_SIZE).submitOk(client, site).response();
        require(response != null && response.getDocuments() != null);
      }
      case GET_DOCUMENT -> {
        var response = new GetDocumentRequestBuilder(id).submitOk(client, site).response();
        require(response != null && id.equals(response.getDocumentId()));
      }
      case GET_CONTENT -> {
        var response = new GetDocumentContentRequestBuilder(id).submitOk(client, site).response();
        require(response != null && content.equals(response.getContent()));
      }
      case SEARCH -> {
        var response = new SearchDocumentRequestBuilder()
            .attribute(new DocumentSearchAttribute().key("benchmarkRunId").eq(runId))
            .limit(String.valueOf(PAGE_SIZE)).submitOk(client, site).response();
        require(response != null && response.getDocuments() != null
            && !response.getDocuments().isEmpty());
        require(response.getDocuments().stream()
            .allMatch(document -> ownedIds.contains(document.getDocumentId())));
      }
      case CREATE_DOCUMENT -> create(id);
      case UPDATE_DOCUMENT -> {
        var response = new UpdateDocumentRequestBuilder(id)
            .addMetadata("benchmarkSequence", String.valueOf(sequence)).submitOk(client, site)
            .response();
        require(response != null && id.equals(response.getDocumentId()));
      }
      default -> throw new IllegalArgumentException("Unsupported scenario");
    }
  }

  /**
   * Verify connectivity and create isolated fixtures outside measurement windows.
   * 
   * @return deployed FormKiQ version
   * @throws Exception if setup or readiness verification fails
   */
  public String prepare() throws Exception {
    var version = new VersionRequestBuilder().submitOk(client, config.siteId()).response();
    require(version != null && version.getVersion() != null);
    if (config.scenarios().stream().anyMatch(scenario -> scenario != Scenario.VERSION)) {
      for (int lane = 0; lane < config.fixtures(); lane++) {
        String id = allocateDocument();
        create(id);
        fixtures.add(id);
      }
      for (String id : fixtures) {
        awaitContent(id);
      }
      if (config.scenarios().contains(Scenario.SEARCH)) {
        awaitSearch();
      }
    }
    return version.getVersion();
  }
}
