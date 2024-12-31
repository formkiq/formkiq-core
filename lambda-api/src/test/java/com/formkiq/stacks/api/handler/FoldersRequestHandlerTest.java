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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import org.junit.jupiter.api.Test;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddFolderRequest;
import com.formkiq.client.model.AddFolderResponse;
import com.formkiq.client.model.DeleteFolderResponse;
import com.formkiq.client.model.GetFoldersResponse;
import com.formkiq.client.model.SearchResultDocument;

/**
 * 
 * Test Handlers for: GET/POST /folders, DELETE /folders/{indexKey}.
 *
 */
public class FoldersRequestHandlerTest extends AbstractApiClientRequestTest {

  /**
   * Test getting folders.
   * 
   * @throws Exception Exception
   */
  @Test
  void testGetFolders01() throws Exception {
    // given
    final String content = "some content";

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago/sample1.txt", "Chicago/sample2.txt",
          "NewYork/sample1.txt")) {
        addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
            "text/plain", null);
      }

      // when
      GetFoldersResponse response =
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(2, documents.size());
      assertEquals("Chicago", documents.get(0).getPath());
      assertEquals("NewYork", documents.get(1).getPath());

      // given
      String indexKey0 = documents.get(0).getIndexKey();
      String indexKey1 = documents.get(1).getIndexKey();

      // when
      final GetFoldersResponse response0 =
          this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, null, null);
      final GetFoldersResponse response1 =
          this.foldersApi.getFolderDocuments(siteId, indexKey1, null, null, null, null);

      // then
      documents = response0.getDocuments();
      assertNotNull(documents);
      assertEquals(2, documents.size());
      assertEquals("Chicago/sample1.txt", documents.get(0).getPath());
      assertEquals("Chicago/sample2.txt", documents.get(1).getPath());

      documents = response1.getDocuments();
      assertNotNull(documents);
      assertEquals(1, documents.size());
      assertEquals("NewYork/sample1.txt", documents.get(0).getPath());
    }
  }

  /**
   * Test getting folders no accesss.
   * 
   * @throws Exception Exception
   */
  @Test
  void testGetFolders02() throws Exception {
    // given
    final String content = "some content";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago/sample1.txt", "Chicago/sample2.txt",
          "NewYork/sample1.txt")) {
        addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
            "text/plain", null);
      }

      // when
      setBearerToken(ID.uuid());

      // then
      assertDocumentForbidden(siteId);
    }
  }

  /**
   * Test getting pagination.
   * 
   * @throws Exception Exception
   */
  @Test
  void testGetFolders03() throws Exception {
    // given
    final int count = 15;
    final String content = "some content";

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      for (String path : List.of("Chicago")) {

        for (int i = 0; i < count; i++) {
          addDocument(this.client, siteId, path + "/sample_" + i,
              content.getBytes(StandardCharsets.UTF_8), "text/plain", null);
        }
      }

      // when
      GetFoldersResponse response =
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(1, documents.size());
      assertEquals("Chicago", documents.get(0).getPath());

      String indexKey0 = documents.get(0).getIndexKey();

      response = this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, null, null);
      documents = response.getDocuments();

      final int expected = 10;
      assertNotNull(documents);
      assertEquals(expected, documents.size());
      assertEquals("Chicago/sample_0", documents.get(0).getPath());
      assertEquals("Chicago/sample_1", documents.get(1).getPath());

      response = this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, null,
          response.getNext());
      documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(expected / 2, documents.size());
      assertEquals("Chicago/sample_5", documents.get(0).getPath());
      assertEquals("Chicago/sample_6", documents.get(1).getPath());
    }
  }

  /**
   * Test getting folders using path parameter.
   *
   * @throws Exception Exception
   */
  @Test
  void testGetFolders04() throws Exception {
    // given
    final String content = "some content";

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago/sample1.txt", "Chicago/sample2.txt",
          "NewYork/sample1.txt")) {
        addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
            "text/plain", null);
      }

      for (String path : List.of("/Chicago", "/Chicago/", "Chicago")) {
        // when
        GetFoldersResponse response =
            this.foldersApi.getFolderDocuments(siteId, null, path, null, null, null);

        // then
        List<SearchResultDocument> documents = response.getDocuments();
        assertNotNull(documents);
        assertEquals(2, documents.size());
        assertEquals("Chicago/sample1.txt", documents.get(0).getPath());
        assertEquals("Chicago/sample2.txt", documents.get(1).getPath());
      }
    }
  }

  /**
   * Test getting invalid folders using path parameter.
   *
   * @throws Exception Exception
   */
  @Test
  void testGetFolders05() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String path = "Chicago";

      // when
      GetFoldersResponse response =
          this.foldersApi.getFolderDocuments(siteId, null, path, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(0, documents.size());
    }
  }

  /**
   * Test adding documents on multiple threads.
   *
   * @throws Exception Exception
   */
  @Test
  void testGetFolders06() throws Exception {
    // given
    final int threadPool = 10;
    final int numberOfThreads = 20;
    final String content = "some content";
    ExecutorService executorService = Executors.newFixedThreadPool(threadPool);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    String siteId = ID.uuid();
    setBearerToken(siteId);

    // when
    for (int i = 0; i < numberOfThreads; i++) {
      final int ii = i;
      executorService.submit(() -> {
        try {
          try {
            String path = "Chicago/sample" + ii + ".txt";
            addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
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

    List<SearchResultDocument> docs = notNull(
        foldersApi.getFolderDocuments(siteId, null, null, "200", null, null).getDocuments());
    assertEquals(1, docs.size());
    assertEquals(docs.get(0).getFolder(), TRUE);
    assertEquals("Chicago", docs.get(0).getPath());

    docs = notNull(
        foldersApi.getFolderDocuments(siteId, null, "Chicago", "200", null, null).getDocuments());

    assertEquals(numberOfThreads, docs.size());
  }

  /**
   * Test add /delete folders.
   * 
   * @throws Exception Exception
   */
  @Test
  void testAddFolders01() throws Exception {
    // given
    final String path = "Chicago/Southside";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      AddFolderResponse response =
          this.foldersApi.addFolder(new AddFolderRequest().path(path), siteId, null);

      // then
      assertEquals("created folder", response.getMessage());
      final String indexKey = response.getIndexKey();
      assertNotNull(indexKey);

      // given
      // when
      GetFoldersResponse folders =
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null, null);

      // then
      assertNotNull(folders.getDocuments());
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Chicago", folders.getDocuments().get(0).getPath());

      // given
      String folderIndexKey = folders.getDocuments().get(0).getIndexKey();

      // when
      folders = this.foldersApi.getFolderDocuments(siteId, folderIndexKey, null, null, null, null);

      // then
      assertNotNull(folders.getDocuments());
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Southside", folders.getDocuments().get(0).getPath());
      assertEquals(indexKey, folders.getDocuments().get(0).getIndexKey());

      // when
      DeleteFolderResponse deleteResponse = this.foldersApi.deleteFolder(indexKey, siteId, null);

      // then
      assertEquals("deleted folder", deleteResponse.getMessage());
      folders = this.foldersApi.getFolderDocuments(siteId, folderIndexKey, null, null, null, null);
      assertNotNull(folders.getDocuments());
      assertTrue(folders.getDocuments().isEmpty());
    }
  }

  /**
   * Test /delete folders that does not exist.
   *
   */
  @Test
  void testDeletedFolders01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String indexKey = UUID.randomUUID() + "#" + UUID.randomUUID();

      // when
      try {
        this.foldersApi.deleteFolder(indexKey, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"directory not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Test /delete existing folders.
   *
   */
  @Test
  void testDeletedFolders02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddFolderRequest req = new AddFolderRequest()
          .path("e0647979-13f3-4c46-9f29-4b3984ef6bca/Order_Document_1123.pdf");

      // when
      AddFolderResponse addFolderResponse = this.foldersApi.addFolder(req, siteId, null);

      // then
      List<SearchResultDocument> docs0 = getSearchResultDocuments(siteId, null);
      assertEquals(1, docs0.size());
      assertEquals("e0647979-13f3-4c46-9f29-4b3984ef6bca", docs0.get(0).getPath());

      String indexKey = addFolderResponse.getIndexKey();
      List<SearchResultDocument> docs1 =
          getSearchResultDocuments(siteId, docs0.get(0).getIndexKey());
      assertEquals(1, docs1.size());
      assertEquals("Order_Document_1123.pdf", docs1.get(0).getPath());
      assertEquals(indexKey, docs1.get(0).getIndexKey());

      // when
      DeleteFolderResponse response = this.foldersApi.deleteFolder(indexKey, siteId, null);

      // then
      assertEquals("deleted folder", response.getMessage());
      docs0 = getSearchResultDocuments(siteId, null);
      assertEquals(1, docs0.size());
      docs1 = getSearchResultDocuments(siteId, docs0.get(0).getIndexKey());
      assertEquals(0, docs1.size());
    }
  }

  /**
   * Test /delete existing folders with file inside.
   *
   */
  @Test
  void testDeletedFolders03()
      throws ApiException, IOException, URISyntaxException, InterruptedException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String content = "some content";
      String folder = "e0647979-13f3-4c46-9f29-4b3984ef6bca/Order_Document_1123.pdf";

      AddFolderRequest req = new AddFolderRequest().path(folder);

      // when
      final AddFolderResponse addFolderResponse = this.foldersApi.addFolder(req, siteId, null);
      addDocument(this.client, siteId, folder + "/test.pdf",
          content.getBytes(StandardCharsets.UTF_8), "text/plain", null);

      // then
      List<SearchResultDocument> docs0 = getSearchResultDocuments(siteId, null);
      assertEquals(1, docs0.size());
      assertEquals("e0647979-13f3-4c46-9f29-4b3984ef6bca", docs0.get(0).getPath());

      String indexKey = addFolderResponse.getIndexKey();
      List<SearchResultDocument> docs1 =
          getSearchResultDocuments(siteId, docs0.get(0).getIndexKey());
      assertEquals(1, docs1.size());
      assertEquals("Order_Document_1123.pdf", docs1.get(0).getPath());
      assertEquals(indexKey, docs1.get(0).getIndexKey());

      // when
      try {
        this.foldersApi.deleteFolder(indexKey, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"folder is not empty\"}", e.getResponseBody());
      }
    }
  }

  private List<SearchResultDocument> getSearchResultDocuments(final String siteId,
      final String indexKey) throws ApiException {
    return notNull(this.foldersApi.getFolderDocuments(siteId, indexKey, null, null, null, null)
        .getDocuments());
  }

  /**
   * Test add folders - missing path.
   *
   */
  @Test
  void testAddFolders02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      try {
        this.foldersApi.addFolder(new AddFolderRequest(), siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"missing 'path' parameters\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Assert Document Forbidden.
   * 
   * @param siteId {@link String}
   */
  private void assertDocumentForbidden(final String siteId) {
    try {
      this.foldersApi.getFolderDocuments(siteId, null, null, null, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }
}
