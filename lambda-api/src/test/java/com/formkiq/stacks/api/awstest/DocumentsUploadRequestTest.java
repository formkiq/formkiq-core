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

import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddLargeDocument;
import com.formkiq.stacks.client.models.DocumentContent;
import com.formkiq.stacks.client.models.DocumentTag;
import com.formkiq.stacks.client.requests.AddLargeDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentContentRequest;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;

/**
 * GET, OPTIONS /documents/upload tests.
 *
 */
public class DocumentsUploadRequestTest extends AbstractApiTest {

  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** 200 No Content. */
  private static final int STATUS_NO_CONTENT = 204;
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** Random Site ID. */
  private static final String SITEID0 = UUID.randomUUID().toString();
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;
  /** Sleep Timeout. */
  private static final int SLEEP = 1000;

  /**
   * After Class.
   */
  @AfterClass
  public static void afterClass() {
    getConfigService().delete(SITEID0);
    getConfigService().delete(SITEID1);
  }

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Assert Document Conttent.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @param content {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void assertDocumentContent(final FormKiqClientV1 client, final String documentId,
      final String content) throws IOException, InterruptedException {
    DocumentContent documentContent =
        client.getDocumentContent(new GetDocumentContentRequest().documentId(documentId));
    while (!content.equals(documentContent.content())) {
      Thread.sleep(SLEEP);
      documentContent =
          client.getDocumentContent(new GetDocumentContentRequest().documentId(documentId));
    }

    assertEquals(content, documentContent.content());
  }

  /**
   * before.
   */
  @Before
  public void before() {
    getConfigService().delete(SITEID0);
    getConfigService().delete(SITEID1);
  }

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String content = "<html><body>test content</body></html>";
      GetDocumentUploadRequest request =
          new GetDocumentUploadRequest().contentLength(content.length());

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      Map<String, Object> map = toMap(response);
      assertNotNull(map.get("url"));
      assertNotNull(map.get("documentId"));

      // given
      final String documentId = map.get("documentId").toString();
      String url = map.get("url").toString();

      // when
      response = this.http.send(HttpRequest.newBuilder(new URI(url))
          .header("Content-Type", MimeType.MIME_HTML.getContentType())
          .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());

      // then
      assertEquals(STATUS_OK, response.statusCode());

      assertDocumentContent(client, documentId, content);
    }
  }

  /**
   * Get Request Upload Document Url, Content Length missing.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    // given
    getConfigService().save(SITEID0, new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "5")));

    for (FormKiqClientV1 client : getFormKiqClients()) {

      GetDocumentUploadRequest request = new GetDocumentUploadRequest().siteId(SITEID0);

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertRequestCorsHeaders(response.headers());
      assertEquals("{\"message\":\"'contentLength' is required\"}", response.body());
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet03() throws Exception {
    // given
    final int contentLength = 100;
    getConfigService().save(SITEID0, new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "5")));

    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID0).contentLength(contentLength);

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertRequestCorsHeaders(response.headers());
      assertEquals("{\"message\":\"'contentLength' cannot exceed 5 bytes\"}", response.body());
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet04() throws Exception {
    // given
    final int contentLength = 5;
    getConfigService().save(SITEID0,
        new DynamicObject(Map.of(MAX_DOCUMENT_SIZE_BYTES, "" + contentLength)));

    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID0).contentLength(contentLength);

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Get Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    getConfigService().save(SITEID1, new DynamicObject(Map.of(MAX_DOCUMENTS, "1")));

    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID1).contentLength(1);

    HttpResponse<String> response =
        getFormKiqClients().get(0).getDocumentUploadAsHttpResponse(request);
    assertEquals(STATUS_OK, response.statusCode());

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertEquals("{\"message\":\"Max Number of Documents reached\"}", response.body());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      // when
      HttpResponse<String> response = client.optionsDocumentUpload();
      // then
      assertEquals(STATUS_NO_CONTENT, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * POST Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      DocumentTag tag0 = new DocumentTag().key("test").value("this");
      AddLargeDocumentRequest request =
          new AddLargeDocumentRequest().document(new AddLargeDocument().tags(Arrays.asList(tag0)));

      // when
      HttpResponse<String> response = client.addLargeDocumentAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      Map<String, Object> map = toMap(response);
      assertNotNull(map.get("url"));
      assertNotNull(map.get("documentId"));

      // given
      String content = "<html><body>test content</body></html>";
      final String documentId = map.get("documentId").toString();
      String url = map.get("url").toString();

      // when
      response = this.http.send(HttpRequest.newBuilder(new URI(url))
          .header("Content-Type", MimeType.MIME_HTML.getContentType())
          .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertDocumentContent(client, documentId, content);
    }
  }
}
