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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.SetDocumentRestoreResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/** Unit Tests for request /documents/{documentId}. */
public class DocumentsIdRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  /**
   * PUT /documents/{documentId}/restore request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleSetDocumentRestore01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);
        AdvancedDocumentSearchApi sapi = new AdvancedDocumentSearchApi(client);

        AddDocumentUploadRequest req = new AddDocumentUploadRequest().path("test.txt");
        GetDocumentUrlResponse response = api.addDocumentUpload(req, siteId, null, null, null);

        String documentId = response.getDocumentId();

        // when
        api.deleteDocument(documentId, siteId, Boolean.TRUE);

        // then
        List<Document> softDeletedDocuments = api
            .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, "100").getDocuments();
        assertFalse(softDeletedDocuments.isEmpty());

        try {
          api.getDocument(documentId, siteId, null);
          fail();
        } catch (ApiException e) {
          assertEquals(SC_NOT_FOUND.getStatusCode(), e.getCode());
        }

        try {
          while (true) {
            sapi.getDocumentFulltext(documentId, siteId, null);
            TimeUnit.SECONDS.sleep(1);
          }
        } catch (ApiException e) {
          assertEquals(SC_NOT_FOUND.getStatusCode(), e.getCode());
        }

        // when
        SetDocumentRestoreResponse restore = api.setDocumentRestore(documentId, siteId);

        // then
        assertEquals("document restored", restore.getMessage());
        assertNotNull(api.getDocument(documentId, siteId, null));
      }
    }
  }

  /**
   * GET /documents/{documentId}/url deepLinkPath request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testHandleGetDocumentUrl01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);

        String deepLink = "https://www.google.com/sample.pdf";
        AddDocumentRequest req =
            new AddDocumentRequest().deepLinkPath(deepLink).contentType("application/pdf");
        String documentId = api.addDocument(req, siteId, null).getDocumentId();

        // when
        GetDocumentResponse document = waitForDocument(client, siteId, documentId);

        // then
        assertEquals(deepLink, document.getDeepLinkPath());
        assertTrue(document.getPath().contains("sample"));
        assertEquals("application/pdf", document.getContentType());
      }
    }
  }
}
