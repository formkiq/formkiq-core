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
package com.formkiq.stacks.api.awstest;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.ChecksumType;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * GET, OPTIONS /documents/upload tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsUploadRequestTest extends AbstractAwsIntegrationTest {

  /** {@link ConfigService}. */
  private static ConfigService configService;
  /** Random Site ID. */
  private static final String SITEID0 = UUID.randomUUID().toString();
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /**
   * After Class.
   */
  @AfterAll
  public static void afterClass() {
    configService.delete(SITEID0);
    configService.delete(SITEID1);
  }

  /**
   * BeforeAll.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws IOException, InterruptedException, URISyntaxException {
    AbstractAwsIntegrationTest.beforeClass();

    configService = new FkqConfigService(getAwsprofile(), getAwsregion(), getAppenvironment());
  }

  /** {@link HttpClient}. */
  private final HttpClient http = HttpClient.newHttpClient();

  /**
   * before.
   */
  @BeforeEach
  public void before() {
    configService.delete(SITEID0);
    configService.delete(SITEID1);
  }

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {

      DocumentsApi api = new DocumentsApi(client);
      String content = "<html><body>test content</body></html>";

      // when
      GetDocumentUrlResponse response =
          api.getDocumentUpload(null, null, null, null, content.length(), null, null);

      // then
      assertNotNull(response.getUrl());
      assertNotNull(response.getDocumentId());

      // given
      final String documentId = response.getDocumentId();
      String url = response.getUrl();

      // when
      HttpResponse<String> httpResponse = this.http.send(HttpRequest.newBuilder(new URI(url))
          .header("Content-Type", MimeType.MIME_HTML.getContentType())
          .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());

      // then
      assertEquals(STATUS_OK, httpResponse.statusCode());

      waitForDocumentContent(client, null, documentId, content);
    }
  }

  /**
   * Get Request Upload Document Url, Content Length missing.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    // given
    configService.save(SITEID0, new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "5")));

    for (ApiClient client : getApiClients(SITEID0)) {

      DocumentsApi api = new DocumentsApi(client);

      // when
      try {
        api.getDocumentUpload(null, SITEID0, null, null, null, null, null);
        fail();
      } catch (ApiException e) {
        assertEquals(STATUS_BAD_REQUEST, e.getCode());
        assertEquals("{\"message\":\"'contentLength' is required\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet03() throws Exception {
    // given
    final int contentLength = 100;
    configService.save(SITEID0, new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "5")));

    for (ApiClient client : getApiClients(SITEID0)) {

      DocumentsApi api = new DocumentsApi(client);

      // when
      try {
        api.getDocumentUpload(null, SITEID0, null, null, contentLength, null, null);
      } catch (ApiException e) {
        assertEquals(STATUS_BAD_REQUEST, e.getCode());
        assertEquals("{\"message\":\"'contentLength' cannot exceed 5 bytes\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet04() throws Exception {
    // given
    final int contentLength = 5;
    configService.save(SITEID0,
        new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "" + contentLength)));

    for (ApiClient client : getApiClients(SITEID0)) {

      DocumentsApi api = new DocumentsApi(client);

      // when
      GetDocumentUrlResponse response =
          api.getDocumentUpload(null, SITEID0, null, null, contentLength, null, null);

      // then
      assertNotNull(response.getUrl());
    }
  }

  /**
   * Get Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    configService.save(SITEID1, new DynamicObject(Map.of(MAX_DOCUMENTS, "1")));

    DocumentsApi api = new DocumentsApi(getApiClients(SITEID1).get(0));

    api.getDocumentUpload(null, SITEID1, null, null, 1, null, null);

    for (ApiClient client : getApiClients(SITEID1)) {

      api = new DocumentsApi(client);

      // when
      try {
        api.getDocumentUpload(null, SITEID1, null, null, 1, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(STATUS_BAD_REQUEST, e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost01() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {

      DocumentsApi api = new DocumentsApi(client);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest()
          .addTagsItem(new AddDocumentTag().key("test").value("this"));

      // when
      GetDocumentUrlResponse response = api.addDocumentUpload(req, null, null, null, null);

      // then
      assertNotNull(response.getDocumentId());
      assertNotNull(response.getUrl());

      // given
      String documentId = response.getDocumentId();
      String url = response.getUrl();
      String content = "<html><body>test content</body></html>";

      // when
      HttpResponse<String> httpResponse = putS3PresignedUrl(url, MimeType.MIME_HTML, content);

      // then
      assertEquals(STATUS_OK, httpResponse.statusCode());
      waitForDocumentContent(client, null, documentId, content);

      DocumentTagsApi tagApi = new DocumentTagsApi(client);
      List<DocumentTag> tags =
          notNull(tagApi.getDocumentTags(documentId, null, null, null, null, null).getTags());
      assertEquals(1, tags.size());
      assertEquals("test", tags.get(0).getKey());
      assertEquals("this", tags.get(0).getValue());
    }
  }

  private HttpResponse<String> putS3PresignedUrl(final String url, final MimeType mime,
      final String content) throws IOException, InterruptedException, URISyntaxException {
    return this.http
        .send(HttpRequest.newBuilder(new URI(url)).header("Content-Type", mime.getContentType())
            .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());
  }

  /**
   * POST Request Upload Document Url.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost02() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {

      DocumentsApi api = new DocumentsApi(client);

      String checksum = "ff7b3ee5cc8c328a8f8a1372a8bef4f09d01ecc581fdee2b65a0b76e50ae73ef";
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().checksum(checksum).checksumType(ChecksumType.SHA256);

      // when
      GetDocumentUrlResponse response = api.addDocumentUpload(req, null, null, null, null);

      // then
      assertNotNull(response.getDocumentId());
      assertNotNull(response.getUrl());

      Map<String, Object> headers = notNull(response.getHeaders());
      assertEquals(2, headers.size());
      assertEquals("/3s+5cyMMoqPihNyqL708J0B7MWB/e4rZaC3blCuc+8=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      // given
      String documentId = response.getDocumentId();
      String url = response.getUrl();
      String content = "<html><body>test content</body></html>";

      // when
      HttpRequest.Builder put = HttpRequest.newBuilder(new URI(url))
          .header("Content-Type", MimeType.MIME_HTML.getContentType())
          .method("PUT", BodyPublishers.ofString(content));

      headers.forEach((key, value) -> put.setHeader(key, (String) value));

      HttpResponse<String> httpResponse = this.http.send(put.build(), BodyHandlers.ofString());

      // then
      assertEquals(STATUS_OK, httpResponse.statusCode());
      waitForDocumentContent(client, null, documentId, content);
    }
  }

  /**
   * POST Request Upload Document Url and then patch document content type.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost03() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);

    DocumentsApi api = new DocumentsApi(client);
    AddDocumentUploadRequest req = new AddDocumentUploadRequest();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      GetDocumentUrlResponse response = api.addDocumentUpload(req, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());
      assertNotNull(response.getUrl());

      // given
      String documentId = response.getDocumentId();
      String url = response.getUrl();
      String content = "<html><body>test content</body></html>";

      // when
      HttpResponse<String> httpResponse = putS3PresignedUrl(url, MimeType.MIME_HTML, content);

      // then
      assertEquals(STATUS_OK, httpResponse.statusCode());
      waitForDocumentContent(client, siteId, documentId, content);
      waitForDocumentContentType(client, siteId, documentId, "text/html");
      assertEquals("text/html", api.getDocument(documentId, siteId, null).getContentType());

      // given
      url = api.getDocumentIdUpload(documentId, siteId, null, null, null, null, null).getUrl();
      content = "some test data";

      // when
      httpResponse = putS3PresignedUrl(url, MimeType.MIME_PLAIN_TEXT, content);

      // then
      assertEquals(STATUS_OK, httpResponse.statusCode());
      waitForDocumentContent(client, siteId, documentId, content);
      waitForDocumentContentType(client, siteId, documentId, "text/plain");
      assertEquals("text/plain", api.getDocument(documentId, siteId, null).getContentType());
    }
  }
}
