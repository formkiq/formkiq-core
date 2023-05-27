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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentContentUrlRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentUploadRequest;

/**
 * GET, OPTIONS /documents/{documentId}/url tests.
 *
 */
public class DocumentsDocumentIdUrlRequestTest extends AbstractApiTest {

  /** 1/2 second sleep. */
  private static final int SLEEP = 500;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;
  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      String documentId = addDocumentWithoutFile(client, null, null);
      Thread.sleep(SLEEP * 2);
      verifyDocumentContent(client, documentId, "sample content", null);
    }
  }

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      String documentId = UUID.randomUUID().toString();
      OptionsDocumentUploadRequest req = new OptionsDocumentUploadRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.optionsDocumentUpload(req);
      // then
      final int status = 204;
      assertEquals(status, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Verify Document Content.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @param content {@link String}
   * @param versionId {@link String}
   * @throws Exception Exception
   */
  private void verifyDocumentContent(final FormKiqClientV1 client, final String documentId,
      final String content, final String versionId) throws Exception {
    final int status200 = 200;

    GetDocumentContentUrlRequest request =
        new GetDocumentContentUrlRequest().documentId(documentId).versionKey(versionId);

    // when
    HttpResponse<String> response = client.getDocumentContentUrlAsHttpResponse(request);

    // then
    assertEquals(status200, response.statusCode());
    assertRequestCorsHeaders(response.headers());

    Map<String, Object> map = toMap(response);
    assertNotNull(map.get("url"));
    assertEquals(documentId, map.get("documentId"));

    // given
    String url = map.get("url").toString();

    // when
    response =
        this.http.send(HttpRequest.newBuilder().uri(new URI(url)).build(), BodyHandlers.ofString());

    // then
    assertEquals(status200, response.statusCode());
    assertEquals(content, response.body());
  }
}
