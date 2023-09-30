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
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddFolderRequest;
import com.formkiq.client.model.AddFolderResponse;
import com.formkiq.client.model.DeleteFolderResponse;
import com.formkiq.client.model.GetFoldersResponse;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/**
 * 
 * Test Handlers for: GET/POST /folders, DELETE /folders/{indexKey}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class FoldersRequestHandlerTest extends AbstractApiClientRequestTest {

  /** Forbidden (403). */
  private static final int STATUS_FORBIDDEN = 403;

  /**
   * Test getting folders.
   * 
   * @throws Exception Exception
   */
  @Test
  void testGetFolders01() throws Exception {
    // given
    final String content = "some content";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago/sample1.txt", "Chicago/sample2.txt",
          "NewYork/sample1.txt")) {
        addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
            "text/plain", null);
      }

      // when
      GetFoldersResponse response =
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(2, documents.size());
      assertEquals("Chicago", documents.get(0).getPath());
      assertEquals("NewYork", documents.get(1).getPath());

      // given
      String indexKey0 = documents.get(0).getIndexKey();
      String indexKey1 = documents.get(1).getIndexKey();

      // when
      final GetFoldersResponse response0 =
          this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, null);
      final GetFoldersResponse response1 =
          this.foldersApi.getFolderDocuments(siteId, indexKey1, null, null, null);

      // then
      documents = response0.getDocuments();
      assertEquals(2, documents.size());
      assertEquals("Chicago/sample1.txt", documents.get(0).getPath());
      assertEquals("Chicago/sample2.txt", documents.get(1).getPath());

      documents = response1.getDocuments();
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago/sample1.txt", "Chicago/sample2.txt",
          "NewYork/sample1.txt")) {
        addDocument(this.client, siteId, path, content.getBytes(StandardCharsets.UTF_8),
            "text/plain", null);
      }

      // when
      setBearerToken(UUID.randomUUID().toString());

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

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String path : Arrays.asList("Chicago")) {

        for (int i = 0; i < count; i++) {
          addDocument(this.client, siteId, path + "/sample_" + i,
              content.getBytes(StandardCharsets.UTF_8), "text/plain", null);
        }
      }

      // when
      GetFoldersResponse response =
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(1, documents.size());
      assertEquals("Chicago", documents.get(0).getPath());

      String indexKey0 = documents.get(0).getIndexKey();

      response = this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, null);
      documents = response.getDocuments();

      final int expected = 10;
      assertEquals(expected, documents.size());
      assertEquals("Chicago/sample_0", documents.get(0).getPath());
      assertEquals("Chicago/sample_1", documents.get(1).getPath());

      response =
          this.foldersApi.getFolderDocuments(siteId, indexKey0, null, null, response.getNext());
      documents = response.getDocuments();
      assertEquals(expected / 2, documents.size());
      assertEquals("Chicago/sample_5", documents.get(0).getPath());
      assertEquals("Chicago/sample_6", documents.get(1).getPath());
    }
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

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
          this.foldersApi.getFolderDocuments(siteId, null, null, null, null);

      // then
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Chicago", folders.getDocuments().get(0).getPath());

      // given
      String folderIndexKey = folders.getDocuments().get(0).getIndexKey();

      // when
      folders = this.foldersApi.getFolderDocuments(siteId, folderIndexKey, null, null, null);

      // then
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Southside", folders.getDocuments().get(0).getPath());
      assertEquals(indexKey, folders.getDocuments().get(0).getIndexKey());

      // when
      DeleteFolderResponse deleteResponse = this.foldersApi.deleteFolder(indexKey, siteId, null);

      // then
      assertEquals("deleted folder", deleteResponse.getMessage());
      folders = this.foldersApi.getFolderDocuments(siteId, folderIndexKey, null, null, null);
      assertTrue(folders.getDocuments().isEmpty());
    }
  }

  /**
   * Test /delete folders that does not exist.
   * 
   * @throws Exception Exception
   */
  @Test
  void testDeletedFolders01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String indexKey = UUID.randomUUID().toString() + "#" + UUID.randomUUID().toString();

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
   * Test add folders - missing path.
   * 
   * @throws Exception Exception
   */
  @Test
  void testAddFolders02() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

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
      this.foldersApi.getFolderDocuments(siteId, null, null, null, null);
      fail();
    } catch (ApiException e) {
      assertEquals(STATUS_FORBIDDEN, e.getCode());
    }
  }
}
