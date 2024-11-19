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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.TypesenseExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContentLength;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentFulltext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit Test for {@link HttpServer}.
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(MinioExtension.class)
public class HttpServerTest {

  /** {@link TypesenseExtension}. */
  @RegisterExtension
  @Order(1)
  static TypesenseExtension typesenseExtension = new TypesenseExtension();
  /** {@link NettyExtension}. */
  @RegisterExtension
  @Order(2)
  static NettyExtension nettyExtension = new NettyExtension(typesenseExtension);

  /** Http Server Port. */
  private static final int BASE_HTTP_SERVER_PORT = 8080;
  /** Base Url. */
  private static final String BASE_URL = "http://localhost:" + BASE_HTTP_SERVER_PORT;
  /** Test Time. */
  private static final int TEST_TIME = 30;

  /** {@link ApiClient}. */
  private final ApiClient apiClient = new ApiClient().setReadTimeout(0).setBasePath(BASE_URL)
      .addDefaultHeader("Authorization", NettyExtension.API_KEY);

  /** {@link DocumentsApi}. */
  private final DocumentsApi documentsApi = new DocumentsApi(this.apiClient);

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  private void assertCorsHeaders(final HttpResponse<String> response) {
    assertEquals("Content-Type,X-Amz-Date,Authorization,X-Api-Key",
        response.headers().firstValue("access-control-allow-headers").get());
    assertEquals("*", response.headers().firstValue("access-control-allow-methods").get());
    assertEquals("*", response.headers().firstValue("access-control-allow-origin").get());
  }

  /**
   * Test add documents.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testAddDocument01() throws Exception {
    // given
    String content = ID.uuid();
    String path = "sometest123.txt";
    AddDocumentRequest req =
        new AddDocumentRequest().path(path).content(content).contentType("text/plain");

    // when
    AddDocumentResponse addDocument = this.documentsApi.addDocument(req, null, null);

    // then
    String documentId = addDocument.getDocumentId();
    assertNotNull(documentId);
    waitForDocumentContent(this.apiClient, null, documentId);

    assertEquals(content,
        this.documentsApi.getDocumentContent(documentId, null, null, null).getContent());

    GetDocumentResponse response = waitForDocumentContentLength(this.apiClient, null, documentId);
    assertEquals(content.length(), response.getContentLength().intValue());

    GetDocumentFulltextResponse fulltext =
        waitForDocumentFulltext(this.apiClient, null, documentId);
    assertEquals(path, fulltext.getPath());
  }

  /**
   * Test get documents by date.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testGetDocument01() throws Exception {
    // given
    // when
    GetDocumentsResponse documents =
        this.documentsApi.getDocuments(null, null, null, "2020-05-20", null, null, null, null);

    // then
    assertEquals(0, documents.getDocuments().size());
  }

  /**
   * Test non existing endpoints.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testInvalidEndpoint() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().header("Authorization", NettyExtension.API_KEY)
        .uri(new URI(BASE_URL + "/hello")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.NOT_FOUND.code(), response.statusCode());
    assertEquals("{\"message\":\"/hello not found\"}", response.body());
  }

  /**
   * Test /login failed.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testLoginFailed() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    Map<String, Object> body = Map.of("username", "jsmith", "password", "12345");
    byte[] content = this.gson.toJson(body).getBytes(StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofByteArray(content))
        .uri(new URI(BASE_URL + "/login")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), response.statusCode());
    assertEquals(
        "{\"code\":\"NotAuthorizedException\",\"message\":\"Incorrect username or password.\"}",
        response.body());
  }

  /**
   * Test /login ok.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(value = TEST_TIME)
  void testLoginOk() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    Map<String, Object> body = Map.of("username", NettyExtension.ADMIN_USERNAME, "password",
        NettyExtension.ADMIN_PASSWORD);
    byte[] content = this.gson.toJson(body).getBytes(StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofByteArray(content))
        .uri(new URI(BASE_URL + "/login")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    Map<String, Object> results = this.gson.fromJson(response.body(), Map.class);

    results = (Map<String, Object>) results.get("AuthenticationResult");

    assertEquals(NettyExtension.API_KEY, results.get("AccessToken"));
    assertEquals(NettyExtension.API_KEY, results.get("IdToken"));
    assertEquals("", results.get("RefreshToken"));
    assertCorsHeaders(response);
  }

  /**
   * Test /sites.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(value = TEST_TIME)
  void testSites() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().header("Authorization", NettyExtension.API_KEY)
        .uri(new URI(BASE_URL + "/sites")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    assertCorsHeaders(response);
    Map<String, Object> results = this.gson.fromJson(response.body(), Map.class);
    assertEquals("admin", results.get("username"));

    List<Map<String, Object>> sites = (List<Map<String, Object>>) results.get("sites");
    assertEquals(1, sites.size());
    assertEquals("[DELETE, READ, WRITE]", sites.get(0).get("permissions").toString());
    assertEquals(DEFAULT_SITE_ID, sites.get(0).get("siteId"));
    assertEquals("READ_WRITE", sites.get(0).get("permission"));
  }

  /**
   * Test unauthorized endpoints.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testUnAuthorizedEndpoint() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(new URI(BASE_URL + "/hello")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), response.statusCode());
    assertEquals("request not supported", response.body());
  }

  /**
   * Test /version.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(value = TEST_TIME)
  void testVersions() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().header("Authorization", NettyExtension.API_KEY)
        .uri(new URI(BASE_URL + "/version")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    assertCorsHeaders(response);
    Map<String, Object> results = this.gson.fromJson(response.body(), Map.class);
    assertEquals("1.13", results.get("version"));
    assertEquals("core", results.get("type"));
    assertEquals("typesense",
        ((List<String>) results.get("modules")).stream().sorted().collect(Collectors.joining(",")));
  }

  /**
   * Test Options.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIME)
  void testOptions() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder().method("OPTIONS", BodyPublishers.ofByteArray(new byte[] {}))
            .uri(new URI(BASE_URL + "/version")).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
    assertEquals("*", response.headers().firstValue("access-control-allow-headers").get());
    assertEquals("*", response.headers().firstValue("access-control-allow-methods").get());
    assertEquals("*", response.headers().firstValue("access-control-allow-origin").get());
  }
}
