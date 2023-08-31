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

import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Unit Test for {@link HttpServer}.
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(MinioExtension.class)
@ExtendWith(NettyExtension.class)
public class HttpServerTest {

  /** Test Time. */
  private static final int TEST_TIME = 10;
  /** Http Server Port. */
  private static final int BASE_HTTP_SERVER_PORT = 8080;
  /** Base Url. */
  private static final String BASE_URL = "http://localhost:" + BASE_HTTP_SERVER_PORT;

  /** {@link ApiClient}. */
  private ApiClient apiClient = new ApiClient().setReadTimeout(0).setBasePath(BASE_URL);

  /** {@link DocumentsApi}. */
  private DocumentsApi documentsApi = new DocumentsApi(this.apiClient);

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /**
   * Test add documents.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIME)
  @Disabled
  void testAddDocument01() throws Exception {
    // given
    String siteId = null;
    String content = UUID.randomUUID().toString();
    AddDocumentRequest req = new AddDocumentRequest().content(content).contentType("text/plain");

    // when
    AddDocumentResponse addDocument = this.documentsApi.addDocument(req, siteId, null);

    // then
    String documentId = addDocument.getDocumentId();
    assertNotNull(documentId);
    waitForDocumentContent(this.apiClient, siteId, documentId);
  }

  /**
   * Test non existing endpoints.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIME)
  void testInvalidEndpoint() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(new URI(BASE_URL + "/hello")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), response.statusCode());
    assertEquals("{\"message\":\"/hello not found\"}", response.body());
  }

  /**
   * Test /sites.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIME)
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

  /**
   * Test /version.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIME)
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
}
