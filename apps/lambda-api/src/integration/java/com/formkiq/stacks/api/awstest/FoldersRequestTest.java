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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.base64.StringToBase64Encoder;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.FolderPermission;
import com.formkiq.client.model.FolderPermissionType;
import com.formkiq.client.model.GetFolderPermissionsResponse;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.testutils.api.folders.GetFolderPermissionsRequestBuilder;
import com.formkiq.testutils.api.folders.GetFoldersRequestBuilder;
import com.formkiq.testutils.api.folders.SetFolderPermissionsRequestBuilder;
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
import com.formkiq.client.model.MoveFolderRequest;
import com.formkiq.client.model.MoveFolderResponse;
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

        // when
        ApiHttpResponse<GetFoldersResponse> submit =
            new GetFoldersRequestBuilder().submit(apiClient, siteId);

        // then
        assertNotNull(submit.response().getDocuments());
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
    try (ExecutorService executorService = Executors.newFixedThreadPool(threadPool)) {
      CountDownLatch latch = new CountDownLatch(numberOfThreads);

      String siteId = ID.uuid();

      List<ApiClient> clients = getApiClients(siteId);
      final String random = ID.uuid();

      ApiClient apiClient = clients.getFirst();

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
  }

  /**
   * Test POST /folders/{indexKey}/moves.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testMoveFolder01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.ulid())) {

      ApiClient apiClient = getApiClients(siteId).getFirst();

      final String sourceFolder = "/movetest/source-" + ID.uuid();
      final String targetFolder = "/movetest/target-" + ID.uuid();
      final String sourceFilePath = sourceFolder + "/root.txt";
      final String sourceChildFilePath = sourceFolder + "/child/nested.txt";
      final String targetFilePath = targetFolder + "/root.txt";
      final String targetChildFilePath = targetFolder + "/child/nested.txt";

      // when
      var sourceDocument = new AddDocumentRequestBuilder().content().path(sourceFilePath)
          .getDocument(apiClient, siteId);
      var childDocument = new AddDocumentRequestBuilder().content().path(sourceChildFilePath)
          .getDocument(apiClient, siteId);

      // then
      waitForDocumentContent(apiClient, siteId, sourceDocument.documentId());
      waitForDocumentContent(apiClient, siteId, childDocument.documentId());

      // when
      var folderDocs =
          new GetFoldersRequestBuilder().path(sourceFolder).getFolderDocuments(apiClient, siteId);

      // then
      assertEquals(1, folderDocs.size());

      // given
      var indexKey = folderDocs.getFirst().getIndexKey();
      assertNotNull(indexKey);

      DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);

      // when
      MoveFolderResponse response =
          foldersApi.moveFolder(indexKey, new MoveFolderRequest().path(targetFolder), siteId);

      // then
      assertEquals("folder move request created", response.getMessage());
      waitForFolderMove(apiClient, siteId, sourceDocument, targetFilePath, childDocument,
          targetChildFilePath, targetFolder);
    }
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

        ApiHttpResponse<GetFoldersResponse> submit =
            new GetFoldersRequestBuilder().limit("100").submit(apiClient, siteId);
        assertFalse(notNull(submit.response().getDocuments()).isEmpty());

        submit = new GetFoldersRequestBuilder().indexKey(response.getIndexKey()).limit("100")
            .submit(apiClient, siteId);

        List<SearchResultDocument> docs = notNull(submit.response().getDocuments());
        assertEquals(1, docs.size());
        assertEquals(folder + "/test.txt", docs.getFirst().getPath());
      }
    }
  }

  /**
   * Test POST /folders/permissions.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPutFolderWithPermissions() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> clients = getApiClients(siteId);

      String folder = "mydoc_" + ID.uuid();
      String path = folder + "/doc.txt";
      ApiHttpResponse<AddDocumentResponse> addDoc =
          new AddDocumentRequestBuilder().content().path(path).submit(clients.getFirst(), siteId);
      assertNull(addDoc.exception());

      String indexKey = new StringToBase64Encoder().apply("#" + folder);

      SetFolderPermissionsRequestBuilder req = new SetFolderPermissionsRequestBuilder().path(folder)
          .addRole("myrole", FolderPermissionType.WRITE);

      for (ApiClient apiClient : List.of(clients.get(0), clients.get(1))) {

        // when
        ApiHttpResponse<SetResponse> setPerm = req.submit(apiClient, siteId);

        // then
        assertEquals("Folder permissions set", setPerm.response().getMessage());
        ApiHttpResponse<GetFolderPermissionsResponse> getPerms =
            new GetFolderPermissionsRequestBuilder().indexKey(indexKey).submit(apiClient, siteId);
        assertNull(getPerms.exception());
        List<FolderPermission> roles = notNull(getPerms.response().getRoles());
        assertEquals(1, roles.size());
        assertEquals("myrole", roles.getFirst().getRoleName());
        assertEquals("WRITE", String.join(",",
            notNull(roles.getFirst().getPermissions()).stream().map(Enum::name).toList()));
      }

      // when
      ApiHttpResponse<SetResponse> setPerm = req.submit(clients.get(2), siteId);

      // then
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), setPerm.exception().getCode());
    }
  }

  private void waitForFolderMove(final ApiClient apiClient, final String siteId,
      final DocumentArtifact sourceDocument, final String targetFilePath,
      final DocumentArtifact childDocument, final String targetChildFilePath,
      final String targetFolder) throws Exception {

    new GetDocumentRequestBuilder(sourceDocument).submitUntil(apiClient, siteId,
        resp -> !resp.isError() && targetFilePath.equals(resp.response().getPath()));
    new GetDocumentRequestBuilder(childDocument).submitUntil(apiClient, siteId,
        resp -> !resp.isError() && targetChildFilePath.equals(resp.response().getPath()));

    DocumentFoldersApi foldersApi = new DocumentFoldersApi(apiClient);
    List<SearchResultDocument> targetFolderDocuments = notNull(foldersApi
        .getFolderDocuments(siteId, null, targetFolder, "100", null, null).getDocuments());
    List<SearchResultDocument> targetChildFolderDocuments = notNull(
        foldersApi.getFolderDocuments(siteId, null, targetFolder + "/child", "100", null, null)
            .getDocuments());

    assertEquals(1,
        targetFolderDocuments.stream().filter(d -> targetFilePath.equals(d.getPath())).count());
    assertEquals(1, targetChildFolderDocuments.stream()
        .filter(d -> targetChildFilePath.equals(d.getPath())).count());
  }
}
