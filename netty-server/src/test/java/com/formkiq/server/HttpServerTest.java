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
package com.formkiq.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Unit Test for {@link HttpServer}.
 */
public class HttpServerTest {

  /** Base Url. */
  private static final String BASE_URL = "http://localhost:8080";
  /** {@link Thread}. */
  private static Thread serverThread;

  /**
   * BeforeAll.
   */
  @BeforeAll
  public static void beforeAll() {
    serverThread = new Thread(() -> {
      try {
        HttpServer.main(new String[0]);
      } catch (Exception e) {
        // stopped
      }
    });
    serverThread.start();
  }

  /**
   * After All.
   */
  @AfterAll
  public static void tearDown() {
    serverThread.interrupt();
  }

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /**
   * Test non existing endpoints.
   * 
   * @throws Exception Exception
   */
  @Test
  void testInvalidEndpoint() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(new URI(BASE_URL + "/hello")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), response.statusCode());
    assertEquals("{\"message\":\"/hello not found\"}", response.body());
  }

  /**
   * Test /version.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testVersions() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(new URI(BASE_URL + "/version")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    Map<String, Object> results = this.gson.fromJson(response.body(), Map.class);
    assertEquals("1.13", results.get("version"));
    assertEquals("core", results.get("type"));
    assertEquals("fulltext,ocr",
        ((List<String>) results.get("modules")).stream().sorted().collect(Collectors.joining(",")));
  }

  /**
   * Test /sites.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testSites() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(new URI(BASE_URL + "/sites")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    Map<String, Object> results = this.gson.fromJson(response.body(), Map.class);
    assertEquals("admin", results.get("username"));

    List<Map<String, Object>> sites = (List<Map<String, Object>>) results.get("sites");
    assertEquals(1, sites.size());
    assertEquals("[DELETE, READ, WRITE]", sites.get(0).get("permissions").toString());
    assertEquals("default", sites.get(0).get("siteId"));
    assertEquals("READ_WRITE", sites.get(0).get("permission"));
  }
}
