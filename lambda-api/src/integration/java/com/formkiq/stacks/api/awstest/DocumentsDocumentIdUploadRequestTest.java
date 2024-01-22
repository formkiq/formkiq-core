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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * GET, OPTIONS /documents/{documentId}/upload tests.
 *
 */
public class DocumentsDocumentIdUploadRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(siteId)) {
      String documentId = UUID.randomUUID().toString();
      DocumentsApi api = new DocumentsApi(client);

      // when
      try {
        api.getDocumentIdUpload(documentId, siteId, Integer.valueOf(1), null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      }
    }
  }

  /**
   * Get Request Document Found.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      String siteId = null;
      String documentId = addDocument(client, siteId, "test.txt", new byte[] {}, null, null);
      DocumentsApi api = new DocumentsApi(client);

      // when
      GetDocumentUrlResponse response =
          api.getDocumentIdUpload(documentId, siteId, Integer.valueOf(1), null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(documentId, response.getDocumentId());
    }
  }
}
