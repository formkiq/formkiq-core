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
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContentLength;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.GetDocumentResponse;

@ExtendWith(DockerComposeExtension.class)
class IntegrationTest {

  /** FormKiQ Port. */
  private static final int BASE_HTTP_SERVER_PORT = 8080;
  /** Base Url. */
  private static final String BASE_URL = "http://localhost:" + BASE_HTTP_SERVER_PORT;
  /** Test Timeout. */
  private static final int TEST_TIMEOUT = 30;
  /** {@link ApiClient}. */
  private ApiClient apiClient = new ApiClient().setReadTimeout(0).setBasePath(BASE_URL);
  /** {@link DocumentsApi}. */
  private DocumentsApi documentsApi = new DocumentsApi(this.apiClient);

  /**
   * Set BearerToken.
   * 
   * @param token {@link String}
   */
  public void setBearerToken(final String token) {
    this.apiClient.addDefaultHeader("Authorization", token);
  }

  /**
   * Test add documents.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  void testAddDocument01() throws Exception {
    // given
    String siteId = null;
    String content = UUID.randomUUID().toString();
    AddDocumentRequest req = new AddDocumentRequest().content(content).contentType("text/plain");

    setBearerToken("changeme");

    // when
    AddDocumentResponse addDocument = this.documentsApi.addDocument(req, siteId, null);

    // then
    String documentId = addDocument.getDocumentId();
    assertNotNull(documentId);
    waitForDocumentContent(this.apiClient, siteId, documentId);

    assertEquals(content,
        this.documentsApi.getDocumentContent(documentId, null, siteId, null).getContent());

    GetDocumentResponse response = waitForDocumentContentLength(this.apiClient, siteId, documentId);
    assertEquals(content.length(), response.getContentLength().intValue());
  }

}
