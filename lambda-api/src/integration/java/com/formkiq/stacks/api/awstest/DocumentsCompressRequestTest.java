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

import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.DocumentsCompressRequest;
import com.formkiq.client.model.DocumentsCompressResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.module.http.HttpResponseStatus;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.testutils.FileGenerator;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * 
 * POST /documents/compress integration tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsCompressRequestTest extends AbstractAwsIntegrationTest {

  /** 1024 Constant. */
  private static final int MB = 1024;
  /** Number of bytes in 1 MB. */
  private static final int MB_1 = 1000000;
  /** Http Status 404. */
  private static final int STATUS_NOT_FOUND = 404;

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  /** {@link FileGenerator}. */
  private FileGenerator fileGenerator = new FileGenerator();
  /** {@link HttpService}. */
  private HttpService http = new HttpServiceJdk11();
  /** {@link HttpClient}. */
  private HttpClient httpClient = HttpClient.newHttpClient();

  private HttpResponse<InputStream> fetchDownloadUrl(final String downloadUrl)
      throws IOException, InterruptedException {
    int status = STATUS_NOT_FOUND;
    HttpResponse<InputStream> httpResponse = null;
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();

    while (status == STATUS_NOT_FOUND) {

      httpResponse = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
      status = httpResponse.statusCode();

      if (status == STATUS_NOT_FOUND) {
        TimeUnit.SECONDS.sleep(1);
      }
    }
    return httpResponse;
  }

  /**
   * Test POST /documents/compress small files.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPostDocumentsCompresss01() throws Exception {
    // given
    final int fileCount = 10;
    final String content = "some data";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      List<ApiClient> clients = getApiClients(siteId);
      DocumentsApi documentsApi = new DocumentsApi(clients.get(0));

      Map<String, String> documentIds = new HashMap<>();
      for (int i = 0; i < fileCount; i++) {
        String path = UUID.randomUUID() + ".txt";
        AddDocumentRequest req =
            new AddDocumentRequest().content(content).contentType("text/plain").path(path);
        String documentId = documentsApi.addDocument(req, siteId, null).getDocumentId();
        documentIds.put(path, documentId);
      }

      for (Map.Entry<String, String> e : documentIds.entrySet()) {
        waitForDocumentContent(clients.get(0), siteId, e.getValue());
      }

      for (ApiClient apiClient : clients) {

        documentsApi = new DocumentsApi(apiClient);

        // when
        DocumentsCompressRequest compressReq =
            new DocumentsCompressRequest().documentIds(new ArrayList<>(documentIds.values()));
        DocumentsCompressResponse response = documentsApi.compressDocuments(compressReq, siteId);

        // then
        String downloadUrl = response.getDownloadUrl();

        HttpResponse<InputStream> httpResponse = fetchDownloadUrl(downloadUrl);
        assertNotNull(httpResponse);

        try (ZipInputStream stream = new ZipInputStream(httpResponse.body())) {

          for (ZipEntry entry = stream.getNextEntry(); entry != null; entry =
              stream.getNextEntry()) {
            String name = entry.getName();
            assertTrue(documentIds.containsKey(name));
            assertEquals(content, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
          }
        }
      }
    }
  }

  /**
   * Test POST /documents/compress 2 large files.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPostDocumentsCompresss02() throws Exception {
    // given
    File file1 = this.fileGenerator.generateZipFile(MB * MB);
    final int numberOfMb = 5;
    File file2 = this.fileGenerator.generateZipFile(numberOfMb * MB * MB);

    String siteId = null;
    ApiClient apiClient = getApiClients(null).get(0);

    DocumentsApi documentsApi = new DocumentsApi(apiClient);
    GetDocumentUrlResponse upload1 = documentsApi.getDocumentUpload(null, siteId, null, null, null);
    GetDocumentUrlResponse upload2 = documentsApi.getDocumentUpload(null, siteId, null, null, null);

    // when
    HttpResponse<String> response1 =
        this.http.put(upload1.getUrl(), Optional.empty(), Optional.empty(), file1.toPath());

    HttpResponse<String> response2 =
        this.http.put(upload2.getUrl(), Optional.empty(), Optional.empty(), file2.toPath());

    // then
    assertTrue(HttpResponseStatus.is2XX(response1));
    assertTrue(HttpResponseStatus.is2XX(response2));

    // when
    DocumentsCompressRequest compressReq = new DocumentsCompressRequest()
        .documentIds(Arrays.asList(upload1.getDocumentId(), upload2.getDocumentId()));
    DocumentsCompressResponse response = documentsApi.compressDocuments(compressReq, siteId);

    // then
    String downloadUrl = response.getDownloadUrl();

    HttpResponse<InputStream> httpResponse = fetchDownloadUrl(downloadUrl);
    assertNotNull(httpResponse);

    try (ZipInputStream stream = new ZipInputStream(httpResponse.body())) {

      int count = 0;
      for (ZipEntry entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {
        String name = entry.getName();
        assertTrue(name.equals(upload1.getDocumentId()) || name.equals(upload2.getDocumentId()));
        assertTrue(stream.readAllBytes().length > MB_1);
        count++;
      }

      assertEquals(2, count);
    }
  }
}
