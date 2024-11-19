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
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.SearchResultDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentFoldersApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddFolderRequest;
import com.formkiq.client.model.AddFolderResponse;
import com.formkiq.client.model.DeleteFolderResponse;
import com.formkiq.client.model.GetFoldersResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * 
 * GET/POST/DELETE /folders integration tests.
 *
 */
public class FoldersRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /**
   * Test POST/DELETE /folders.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testDeleteFolders01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> clients = getApiClients(siteId);

      for (ApiClient apiClient : clients) {

        DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);
        String folder = "somefolder123";
        AddFolderRequest req = new AddFolderRequest().path(folder);

        // when
        AddFolderResponse response = foldersApi.addFolder(req, siteId, null);

        // then
        assertNotNull(response.getIndexKey());
        assertEquals("created folder", response.getMessage());

        // when
        DeleteFolderResponse deleteFolder =
            foldersApi.deleteFolder(response.getIndexKey(), siteId, null);

        // then
        assertEquals("deleted folder", deleteFolder.getMessage());
      }
    }
  }

  /**
   * Test GET /folders.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetFolders01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> clients = getApiClients(siteId);

      for (ApiClient apiClient : clients) {

        DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);

        // when
        GetFoldersResponse folderDocuments =
            foldersApi.getFolderDocuments(siteId, null, null, null, null, null);

        // then
        assertNotNull(folderDocuments.getDocuments());
      }
    }
  }

  /**
   * Test adding documents on multiple threads.
   *
   * @throws Exception Exception
   */
  @Test
  void testGetFolders02() throws Exception {
    // given
    final int threadPool = 5;
    final int numberOfThreads = 5;
    final String content = "some content";
    ExecutorService executorService = Executors.newFixedThreadPool(threadPool);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    String siteId = ID.uuid();

    List<ApiClient> clients = getApiClients(siteId);
    final String random = ID.uuid();

    ApiClient apiClient = clients.get(0);

    // when
    for (int i = 0; i < numberOfThreads; i++) {
      final int ii = i;
      executorService.submit(() -> {
        try {
          try {
            String path = "Chicago_" + random + "/sample" + ii + ".txt";
            addDocument(apiClient, siteId, path, content.getBytes(StandardCharsets.UTF_8),
                "text/plain", null);
          } catch (IOException | InterruptedException | URISyntaxException | ApiException e) {
            throw new RuntimeException(e);
          }
        } finally {
          latch.countDown();
        }
      });
    }

    // then
    latch.await();
    executorService.shutdown();

    DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);
    List<SearchResultDocument> docs = notNull(foldersApi
        .getFolderDocuments(siteId, null, "Chicago_" + random, "200", null, null).getDocuments());

    assertEquals(numberOfThreads, docs.size());
  }

  /**
   * Test POST/GET /folders.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPostFolders01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> clients = getApiClients(siteId);

      for (ApiClient apiClient : clients) {

        DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);
        String folder = ID.uuid();
        AddFolderRequest req = new AddFolderRequest().path(folder);

        DocumentsApi documentsApi = new DocumentsApi(apiClient);
        AddDocumentRequest addDoc =
            new AddDocumentRequest().content("some content").path(folder + "/test.txt");
        String documentId = documentsApi.addDocument(addDoc, siteId, null).getDocumentId();
        waitForDocumentContent(apiClient, siteId, documentId);

        // when
        AddFolderResponse response = foldersApi.addFolder(req, siteId, null);

        // then
        assertNotNull(response.getIndexKey());
        assertEquals("created folder", response.getMessage());

        GetFoldersResponse folderDocuments =
            foldersApi.getFolderDocuments(siteId, null, null, "100", null, null);
        assertFalse(folderDocuments.getDocuments().isEmpty());
        folderDocuments =
            foldersApi.getFolderDocuments(siteId, response.getIndexKey(), null, "100", null, null);
        assertEquals(1, folderDocuments.getDocuments().size());
        assertEquals(folder + "/test.txt", folderDocuments.getDocuments().get(0).getPath());
      }
    }
  }
}
