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
package com.formkiq.testutils.aws;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * 
 * JUnit 5 Extension for TypeSense.
 *
 */
public class TypesenseExtension implements BeforeAllCallback {

  /** Root-scoped Typesense container shared by all test classes in one test worker. */
  private static final class SharedTypesenseResource implements AutoCloseable {

    /** HTTP timeout. */
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    /** HTTP success response lower bound. */
    private static final int HTTP_OK = 200;
    /** HTTP success response upper bound. */
    private static final int HTTP_MULTIPLE_CHOICES = 300;
    /** {@link GenericContainer}. */
    private final GenericContainer<?> container;
    /** {@link HttpClient}. */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @SuppressWarnings("resource")
    private SharedTypesenseResource() {
      final Integer exposedPort = Integer.valueOf(DEFAULT_PORT);
      this.container = new GenericContainer<>(image)
          .withEnv(Map.of("TYPESENSE_DATA_DIR", "/tmp", "TYPESENSE_API_KEY", API_KEY))
          .withExposedPorts(exposedPort).waitingFor(Wait.forHttp("/health"));
      this.container.start();
    }

    private synchronized void clearCollections() throws IOException {
      HttpResponse<String> response = sendRequest("/collections", "GET");
      if (response.statusCode() != HTTP_OK) {
        throw new IOException("Unable to list Typesense collections: " + response.body());
      }

      List<String> names = new ArrayList<>();
      for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonArray()) {
        names.add(element.getAsJsonObject().get("name").getAsString());
      }

      for (String name : names) {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        response = sendRequest("/collections/" + encodedName, "DELETE");
        if (response.statusCode() < HTTP_OK || response.statusCode() >= HTTP_MULTIPLE_CHOICES) {
          throw new IOException(
              "Unable to delete Typesense collection '" + name + "': " + response.body());
        }
      }
    }

    @Override
    public void close() {
      this.container.close();
    }

    private Integer getFirstMappedPort() {
      return this.container.getFirstMappedPort();
    }

    private HttpResponse<String> sendRequest(final String path, final String method)
        throws IOException {
      URI uri = URI.create("http://localhost:" + getFirstMappedPort() + path);
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(uri).timeout(HTTP_TIMEOUT).header("X-TYPESENSE-API-KEY", API_KEY);
      HttpRequest request =
          "DELETE".equals(method) ? builder.DELETE().build() : builder.GET().build();

      try {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while calling Typesense", e);
      }
    }
  }

  /** Test API Key. */
  public static final String API_KEY = "Hu1234s212AdxdZ";
  /** Default DynamoDB Port. */
  public static final int DEFAULT_PORT = 8108;
  /** Type Sense Image. */
  private static DockerImageName image = DockerImageName.parse("typesense/typesense:0.25.1");
  /** Extension Store Key. */
  private static final String STORE_KEY = "typesense";
  /** Extension Store Namespace. */
  private static final ExtensionContext.Namespace STORE_NAMESPACE =
      ExtensionContext.Namespace.create(TypesenseExtension.class);

  /** Mapped Port. */
  private Integer mappedPort = null;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    SharedTypesenseResource resource =
        context.getRoot().getStore(STORE_NAMESPACE).getOrComputeIfAbsent(STORE_KEY,
            key -> new SharedTypesenseResource(), SharedTypesenseResource.class);
    resource.clearCollections();
    this.mappedPort = resource.getFirstMappedPort();
  }

  /**
   * Get First Mapped Port.
   * 
   * @return Integer
   */
  public Integer getFirstMappedPort() {
    return this.mappedPort;
  }
}
