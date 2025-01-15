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

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContentByContentType;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentContentResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * GET, OPTIONS /documents/{documentId}/url tests.
 *
 */
public class DocumentsDocumentIdUrlRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      String text = "sample content";
      byte[] content = text.getBytes(StandardCharsets.UTF_8);

      // when
      String documentId = addDocument(client, null, null, content, "text/plain", null);
      // then

      GetDocumentContentResponse response =
          waitForDocumentContentByContentType(client, null, documentId, "text/plain");

      assertEquals("text/plain", response.getContentType());
      assertEquals(text, response.getContent());
    }
  }

  /**
   * /documents/{documentId}/content TOO large content.
   *
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testHandleGetDocumentContent01() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);
    DocumentsApi api = new DocumentsApi(client);

    final int sixMb = 6 * 1024 * 1024; // 6 MB in bytes
    byte[] content = "a".repeat(sixMb).getBytes(StandardCharsets.UTF_8);
    String documentId = addDocument(client, null, null, content, "text/plain", null);
    waitForDocumentContentType(client, null, documentId, "text/plain");

    // when
    try {
      api.getDocumentContent(documentId, null, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"Response exceeds allowed size\"}", e.getResponseBody());
    }
  }

  /**
   * /documents/{documentId}/content the largest content.
   *
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testHandleGetDocumentContent02() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);
    DocumentsApi api = new DocumentsApi(client);

    final int fivehalf = (int) (5.5 * 1024 * 1024); // 6 MB in bytes
    byte[] content = "a".repeat(fivehalf).getBytes(StandardCharsets.UTF_8);
    String documentId = addDocument(client, null, null, content, "text/plain", null);
    waitForDocumentContentType(client, null, documentId, "text/plain");

    // when
    GetDocumentContentResponse response = api.getDocumentContent(documentId, null, null, null);

    // then
    assertNotNull(response.getContent());
  }
}
