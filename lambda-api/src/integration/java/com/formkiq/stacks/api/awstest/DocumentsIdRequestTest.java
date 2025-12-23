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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.uploadDocumentContent;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.ChecksumType;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.testutils.api.opensearch.OpenSearchIndexPurgeRequestBuilder;
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
  private static final int TEST_TIMEOUT = 120;

  /**
   * Save new File and update content.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testDocumentUpdate01() throws Exception {
    // given
    byte[] content = "ajlsdkjsald".getBytes(StandardCharsets.UTF_8);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      for (ApiClient client : getApiClients(siteId)) {
        String documentId =
            addDocument(client, siteId, "askljdkalsd.txt", content, "text/plain", null, null);

        DocumentsApi api = new DocumentsApi(client);
        UpdateDocumentRequest updateReq = new UpdateDocumentRequest();

        // when
        AddDocumentResponse response = api.updateDocument(documentId, updateReq, siteId, null);

        // then
        String newContent = "new content";
        assertNotNull(response.getUploadUrl());
        uploadDocumentContent(response.getUploadUrl(), newContent.getBytes(StandardCharsets.UTF_8),
            "text/plain", Map.of());
        waitForDocumentContent(client, siteId, documentId, newContent);
      }
    }
  }

  /**
   * Save new File and update content using SHA256.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testDocumentUpdate02() throws Exception {
    // given
    String contentType = "text/plain";
    String content0Hash = "1d4c375d631fdb1072f2299458b3882561fd86c31ec13a8aebe642e7196b01c9";
    byte[] content = "ajlsdkjsald".getBytes(StandardCharsets.UTF_8);
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);
        AddDocumentRequest req = new AddDocumentRequest()
            .content(Base64.getEncoder().encodeToString(content)).contentType(contentType)
            .isBase64(Boolean.TRUE).checksum(content0Hash).checksumType(ChecksumType.SHA256);
        AddDocumentResponse response = api.addDocument(req, siteId, null);
        String documentId = response.getDocumentId();

        String newContentHash = "fe32608c9ef5b6cf7e3f946480253ff76f24f4ec0678f3d0f07f9844cbff9601";
        UpdateDocumentRequest updateReq =
            new UpdateDocumentRequest().checksum(newContentHash).checksumType(ChecksumType.SHA256);

        // when
        response = api.updateDocument(documentId, updateReq, siteId, null);

        // then
        assertNotNull(response.getUploadUrl());
        assertEquals(2, notNull(response.getHeaders()).size());
        String newContent = "new content";

        uploadDocumentContent(response.getUploadUrl(), newContent.getBytes(StandardCharsets.UTF_8),
            contentType, response.getHeaders());
        waitForDocumentContent(client, siteId, documentId, newContent);

      }
    }
  }

  /**
   * GET /documents/{documentId}/url deepLinkPath request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testHandleGetDocumentUrl01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
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
        assertTrue(Objects.requireNonNull(document.getPath()).contains("sample"));
        assertEquals("application/pdf", document.getContentType());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/restore request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testHandleSetDocumentRestore01() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);
    new OpenSearchIndexPurgeRequestBuilder().submit(client, null);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      client = getApiClients(siteId).get(0);

      DocumentsApi api = new DocumentsApi(client);
      AdvancedDocumentSearchApi sapi = new AdvancedDocumentSearchApi(client);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().path("test.txt");
      GetDocumentUrlResponse response = api.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      api.deleteDocument(documentId, siteId, Boolean.TRUE);

      // then
      List<Document> softDeletedDocuments = notNull(
          api.getDocuments(siteId, null, null, Boolean.TRUE, null, null, null, null, null, "100")
              .getDocuments());
      assertFalse(softDeletedDocuments.isEmpty());

      try {
        api.getDocument(documentId, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(SC_NOT_FOUND.getStatusCode(), e.getCode());
      }

      while (true) {
        try {
          sapi.getDocumentFulltext(documentId, siteId, null);
          TimeUnit.SECONDS.sleep(1);
        } catch (ApiException e) {
          TimeUnit.SECONDS.sleep(1);
          if (e.getCode() == SC_NOT_FOUND.getStatusCode()) {
            break;
          }
        }
      }

      // when
      SetDocumentRestoreResponse restore = api.setDocumentRestore(documentId, siteId);

      // then
      assertEquals("document restored", restore.getMessage());
      assertNotNull(api.getDocument(documentId, siteId, null));
    }
  }
}
