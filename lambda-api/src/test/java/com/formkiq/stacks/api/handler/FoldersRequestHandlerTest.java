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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import static com.formkiq.client.model.FolderPermissionType.DELETE;
import static com.formkiq.client.model.FolderPermissionType.WRITE;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.FolderPermission;
import com.formkiq.client.model.FolderPermissionType;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentTagRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.folders.AddFolderRequestBuilder;
import com.formkiq.testutils.api.folders.GetFolderPermissionsRequestBuilder;
import com.formkiq.testutils.api.folders.GetFoldersRequestBuilder;
import com.formkiq.testutils.api.folders.SetFolderPermissionsRequestBuilder;
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
          new GetFoldersRequestBuilder().submit(client, siteId).response();

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
      final var response0 =
          new GetFoldersRequestBuilder().indexKey(indexKey0).submit(client, siteId).response();
      final var response1 =
          new GetFoldersRequestBuilder().indexKey(indexKey1).submit(client, siteId).response();

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
          new GetFoldersRequestBuilder().submit(client, siteId).response();

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(1, documents.size());
      assertEquals("Chicago", documents.get(0).getPath());

      String indexKey0 = documents.get(0).getIndexKey();

      response =
          new GetFoldersRequestBuilder().indexKey(indexKey0).submit(client, siteId).response();
      documents = response.getDocuments();

      final int expected = 10;
      assertNotNull(documents);
      assertEquals(expected, documents.size());
      assertEquals("Chicago/sample_0", documents.get(0).getPath());
      assertEquals("Chicago/sample_1", documents.get(1).getPath());

      response = new GetFoldersRequestBuilder().indexKey(indexKey0).next(response.getNext())
          .submit(client, siteId).response();
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
            new GetFoldersRequestBuilder().path(path).submit(client, siteId).response();

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
   */
  @Test
  void testGetFolders05() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      String path = "Chicago";

      // when
      GetFoldersResponse response =
          new GetFoldersRequestBuilder().path(path).submit(client, siteId).response();

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

    List<SearchResultDocument> docs = notNull(new GetFoldersRequestBuilder().limit("200")
        .submit(client, siteId).response().getDocuments());

    assertEquals(1, docs.size());
    assertEquals(TRUE, docs.get(0).getFolder());
    assertEquals("Chicago", docs.get(0).getPath());

    docs = notNull(new GetFoldersRequestBuilder().path("Chicago").limit("200")
        .submit(client, siteId).response().getDocuments());

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
      AddFolderResponse response = addFolder(siteId, path);

      // then
      assertEquals("created folder", response.getMessage());
      final String indexKey = response.getIndexKey();
      assertNotNull(indexKey);

      // given
      // when
      GetFoldersResponse folders = new GetFoldersRequestBuilder().submit(client, siteId).response();

      // then
      assertNotNull(folders.getDocuments());
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Chicago", folders.getDocuments().get(0).getPath());

      // given
      String folderIndexKey = folders.getDocuments().get(0).getIndexKey();

      // when
      folders =
          new GetFoldersRequestBuilder().indexKey(folderIndexKey).submit(client, siteId).response();

      // then
      assertNotNull(folders.getDocuments());
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Southside", folders.getDocuments().get(0).getPath());
      assertEquals(indexKey, folders.getDocuments().get(0).getIndexKey());

      // when
      DeleteFolderResponse deleteResponse = this.foldersApi.deleteFolder(indexKey, siteId, null);

      // then
      assertEquals("deleted folder", deleteResponse.getMessage());
      folders =
          new GetFoldersRequestBuilder().indexKey(folderIndexKey).submit(client, siteId).response();
      assertNotNull(folders.getDocuments());
      assertTrue(folders.getDocuments().isEmpty());
    }
  }

  private AddFolderResponse addFolder(final String siteId, final String path) throws ApiException {
    return this.foldersApi.addFolder(new AddFolderRequest().path(path), siteId, null);
  }

  /**
   * Test add folders with permissions with govern role.
   *
   * @throws Exception Exception
   */
  @Test
  void testAddFolderWithPermissions() throws Exception {
    // given
    final String path = "path1/path2/path3";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId + "_govern");

      // when
      AddFolderResponse response = addFolder(siteId, path);

      // then
      assertEquals("created folder", response.getMessage());

      // when
      var folders =
          notNull(new GetFoldersRequestBuilder().submit(client, siteId).response().getDocuments());

      // then
      assertEquals(1, folders.size());
      String indexKey = folders.get(0).getIndexKey();
      assertNotNull(indexKey);
      assertEquals("path1", folders.get(0).getPath());

      // when - get permissions before set
      var permissions = notNull(this.foldersApi.getFolderPermissions(indexKey, siteId).getRoles());

      // then
      assertEquals(0, permissions.size());

      // when
      var setResponse = new SetFolderPermissionsRequestBuilder().path("path1").addReadRole("myrole")
          .submit(client, siteId);

      // then
      assertEquals("Folder permissions set", setResponse.response().getMessage());

      permissions = notNull(this.foldersApi.getFolderPermissions(indexKey, siteId).getRoles());
      assertEquals(1, permissions.size());
      assertEquals("myrole", permissions.get(0).getRoleName());
      assertEquals("READ", notNull(permissions.get(0).getPermissions()).stream()
          .map(FolderPermissionType::getValue).collect(Collectors.joining(",")));

      // when - set NO roles
      setResponse = new SetFolderPermissionsRequestBuilder().path("path1").submit(client, siteId);

      // then
      assertNull(setResponse.exception());
      assertEquals("Folder permissions set", setResponse.response().getMessage());

      permissions = notNull(this.foldersApi.getFolderPermissions(indexKey, siteId).getRoles());
      assertEquals(0, permissions.size());
    }
  }

  /**
   * Test add folders permissions to invalid path.
   *
   */
  @Test
  void testAddFolderPermissionsMissingFolder() {
    // given
    setBearerToken("Admins");
    String path = ID.uuid();

    // when
    ApiHttpResponse<SetResponse> resp = setPathReadPermissions(DEFAULT_SITE_ID, "myrole", path);

    // then
    assertNotNull(resp);
    assertNull(resp.response());
    assertEquals(SC_NOT_FOUND.getStatusCode(), resp.exception().getCode());
    assertEquals("{\"message\":\"Folder '" + path + "' not found\"}",
        resp.exception().getResponseBody());
  }

  /**
   * Test add folders permissions, then remove.
   *
   */
  @Test
  void testAddFolderPermissionsThenRemove() throws ApiException {
    // given
    setBearerToken("Admins");
    var path = "somefolder23";
    var response = addFolder(null, path);
    assertEquals("created folder", response.getMessage());

    // when
    var resp = setPathReadPermissions(null, "myrole", path);

    // then
    assertFalse(resp.isError());
    var files = new GetFoldersRequestBuilder().path("").submit(client, null);
    List<SearchResultDocument> docs = notNull(files.response().getDocuments());
    assertEquals(1, docs.size());
    assertEquals("somefolder23", docs.get(0).getPath());
    String indexKey = docs.get(0).getIndexKey();

    List<FolderPermission> roles = getFolderPermissions(indexKey);
    assertEquals(1, roles.size());
    assertEquals("myrole", roles.get(0).getRoleName());
    assertEquals("READ", notNull(roles.get(0).getPermissions()).stream()
        .map(FolderPermissionType::getValue).collect(Collectors.joining(",")));

    // when
    var set = new SetFolderPermissionsRequestBuilder().path(path).addRole("john", List.of())
        .submit(client, null);

    // then
    assertFalse(set.isError());

    roles = getFolderPermissions(indexKey);
    assertEquals(1, roles.size());
    assertEquals("john", roles.get(0).getRoleName());
    assertEquals("", notNull(roles.get(0).getPermissions()).stream()
        .map(FolderPermissionType::getValue).collect(Collectors.joining(",")));
  }

  private List<FolderPermission> getFolderPermissions(final String indexKey) {
    var perm = new GetFolderPermissionsRequestBuilder().indexKey(indexKey).submit(client, null);
    return notNull(perm.response().getRoles());
  }

  /**
   * Test add folders permissions to invalid permission.
   *
   */
  @Test
  void testAddFolderPermissionsMissingPermission() throws ApiException {
    // given
    setBearerToken("Admins");
    String path = ID.uuid();

    addFolder(DEFAULT_SITE_ID, path);

    // when
    var resp = new SetFolderPermissionsRequestBuilder().path(path)
        .addRole(null, (FolderPermissionType) null).submit(client, DEFAULT_SITE_ID);

    // then
    assertNull(resp.response());
    assertEquals(SC_BAD_REQUEST.getStatusCode(), resp.exception().getCode());
    assertEquals("{\"errors\":[{\"key\":\"roleName\",\"error\":\"'roleName' is required\"}]}",
        resp.exception().getResponseBody());
  }

  /**
   * Test add permission to file and not folder.
   *
   */
  @Test
  void testSetFolderPermissionsOnDocument() {
    // given
    setBearerToken("Admins");
    String path = "mytestdir/" + ID.uuid();

    var response =
        new AddDocumentRequestBuilder().content().path(path).submit(client, DEFAULT_SITE_ID);
    assertNull(response.exception());

    // when
    var resp = new SetFolderPermissionsRequestBuilder().path(path)
        .addRole("myrole", FolderPermissionType.READ).submit(client, DEFAULT_SITE_ID);

    // then
    assertNull(resp.response());
    assertEquals(SC_NOT_FOUND.getStatusCode(), resp.exception().getCode());
    assertEquals("{\"message\":\"Folder '" + path + "' not found\"}",
        resp.exception().getResponseBody());
  }

  /**
   * Test add documents in folder with/without WRITE permission.
   *
   * @throws Exception Exception
   */
  @Test
  void testWriteDocumentWithPermissions() throws Exception {
    // given
    String siteId = "school";
    String path = setupTeacherFolder(siteId);
    new AddAttributeRequestBuilder().keyAsString("myKey").submit(client, siteId);

    AddDocumentRequestBuilder addDocumentReq = new AddDocumentRequestBuilder().content().path(path);

    // write document
    submit("student", addDocumentReq, siteId, true);

    ApiHttpResponse<AddDocumentResponse> resp = submit("teacher", addDocumentReq, siteId, false);
    String documentId = resp.response().getDocumentId();

    // add attribute
    AddDocumentAttributeRequestBuilder addAttributeReq = new AddDocumentAttributeRequestBuilder()
        .setDocumentId(documentId).addAttribute("myKey", "1234");
    submit("student", addAttributeReq, siteId, true);
    submit("teacher", addAttributeReq, siteId, false);

    // add tag
    AddDocumentTagRequestBuilder addTagReq =
        new AddDocumentTagRequestBuilder().setDocumentId(documentId).addTag("myTag", "111");
    submit("student", addTagReq, siteId, true);
    submit("teacher", addTagReq, siteId, false);

    // add subfolder
    AddFolderRequestBuilder addFolderReq = new AddFolderRequestBuilder().path("path10/mysubfolder");
    submit("student", addFolderReq, siteId, true);
    submit("teacher", addFolderReq, siteId, false);

    // delete document
    DeleteDocumentRequestBuilder deleteReq = new DeleteDocumentRequestBuilder(documentId);

    submit("student", deleteReq, siteId, true);
    submit("teacher", deleteReq, siteId, false);
  }

  private <T> ApiHttpResponse<T> submit(final String role, final HttpRequestBuilder requestBuilder,
      final String siteId, final boolean unauthorized) {
    setBearerToken(new String[] {siteId, role});
    ApiHttpResponse<?> response = requestBuilder.submit(client, siteId);
    if (unauthorized) {
      assertNotNull(response.exception());
      assertNull(response.response());
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), response.exception().getCode());
    } else {
      assertNull(response.exception());
      assertNotNull(response.response());
    }

    return (ApiHttpResponse<T>) response;
  }

  /**
   * Set up a Teacher READ/WRITE and student READ folder.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  private String setupTeacherFolder(final String siteId) throws ApiException {
    final String folder = "path10";
    final String path = folder + "/" + ID.uuid();

    setBearerToken(new String[] {siteId, "Admins"});

    // add folder / permissions
    addFolder(siteId, "path10");
    var error = new SetFolderPermissionsRequestBuilder().path("path10")
        .addRole("teacher", List.of(WRITE, DELETE)).addReadRole("student").submit(client, siteId)
        .exception();

    assertNull(error);
    return path;
  }

  /**
   * Test set folder permissions not as admin.
   *
   * @throws Exception Exception
   */
  @Test
  void testSetFolderPermissionsNonAdmin() throws Exception {
    // given
    String siteId = "mysite";
    setBearerToken(new String[] {siteId});

    // when - add folder / permissions
    addFolder(siteId, "myfolder");
    var resp = new SetFolderPermissionsRequestBuilder().path("myfolder").addRole("teacher", WRITE)
        .submit(client, siteId);

    // then
    assertEquals(SC_UNAUTHORIZED.getStatusCode(), resp.exception().getCode());
  }

  /**
   * Test set folder permissions as bad request.
   *
   * @throws Exception Exception
   */
  @Test
  void testSetFolderPermissionsBadRequest() throws Exception {
    // given
    setBearerToken(new String[] {"Admins"});

    // when - add folder / permissions
    addFolder(null, "myfolder");
    var resp = new SetFolderPermissionsRequestBuilder().submit(client, null);

    // then
    assertEquals(SC_BAD_REQUEST.getStatusCode(), resp.exception().getCode());
    assertEquals("{\"errors\":[{\"key\":\"path\",\"error\":\"'path' is required\"}]}",
        resp.exception().getResponseBody());
  }

  /**
   * Test add folders listing with permissions.
   *
   * @throws Exception Exception
   */
  @Test
  void testListDocumentWithPermissions() throws Exception {
    // given
    String siteId = "mySite";
    setBearerToken(new String[] {siteId, "student"});

    addFolder(siteId, "path1/path2");
    addFolder(siteId, "path1/path3");

    // when
    List<SearchResultDocument> root = getSearchResultDocuments(siteId, null);
    List<SearchResultDocument> rootSearch = searchMeta(siteId, "");

    // then
    for (List<SearchResultDocument> list : List.of(root, rootSearch)) {

      assertEquals(1, list.size());

      List<SearchResultDocument> path1 =
          getSearchResultDocuments(siteId, list.get(0).getIndexKey());
      assertEquals(2, path1.size());
      assertEquals("path2", path1.get(0).getPath());
      assertEquals("path3", path1.get(1).getPath());
    }

    setBearerToken(new String[] {siteId, "student", "Admins"});

    // when
    assertNull(setPathReadPermissions(siteId, "myrole", "path1/path2").exception());

    // then
    setBearerToken(new String[] {siteId, "student"});

    List<SearchResultDocument> withPerms =
        getSearchResultDocuments(siteId, root.get(0).getIndexKey());
    assertEquals(1, withPerms.size());
    assertEquals("path3", withPerms.get(0).getPath());

    List<SearchResultDocument> withPermsSearch = searchMeta(siteId, "path1");
    assertEquals(1, withPermsSearch.size());
    assertEquals("path3", withPermsSearch.get(0).getPath());

    setBearerToken(new String[] {siteId, "student", "Admins"});

    // when
    assertNull(setPathReadPermissions(siteId, "student", "path1/path2").exception());

    // then
    setBearerToken(new String[] {siteId, "student"});

    withPerms = getSearchResultDocuments(siteId, root.get(0).getIndexKey());
    assertEquals(2, withPerms.size());
    assertEquals("path2", withPerms.get(0).getPath());
    assertEquals("path3", withPerms.get(1).getPath());

    withPermsSearch = searchMeta(siteId, "path1");
    assertEquals(2, withPermsSearch.size());
    assertEquals("path2", withPermsSearch.get(0).getPath());
    assertEquals("path3", withPermsSearch.get(1).getPath());
  }

  private List<SearchResultDocument> searchMeta(final String siteId, final String path)
      throws ApiException {
    DocumentSearchRequest req = new DocumentSearchRequest()
        .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(path + "/")));
    return notNull(this.searchApi.documentSearch(req, siteId, null, null, null).getDocuments());
  }

  private ApiHttpResponse<SetResponse> setPathReadPermissions(final String siteId,
      final String roleName, final String path) {
    return new SetFolderPermissionsRequestBuilder().path(path)
        .addRole(roleName, FolderPermissionType.READ).submit(client, siteId);
  }

  /**
   * Test Get Invalid Folder permissions.
   *
   */
  @Test
  void testGetInvalidFolderPermissions() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // given
      String indexKey = ID.uuid();

      // when
      try {
        this.foldersApi.getFolderPermissions(indexKey, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"invalid indexKey '" + indexKey + "'\"}", e.getResponseBody());
      }
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
        assertEquals("{\"message\":\"invalid indexKey '" + indexKey + "'\"}", e.getResponseBody());
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
      assertNotNull(indexKey);

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
      assertNotNull(indexKey);

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
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"folder is not empty\"}", e.getResponseBody());
      }
    }
  }

  private List<SearchResultDocument> getSearchResultDocuments(final String siteId,
      final String indexKey) {
    return notNull(new GetFoldersRequestBuilder().indexKey(indexKey).submit(client, siteId)
        .response().getDocuments());
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

    ApiHttpResponse<GetFoldersResponse> submit =
        new GetFoldersRequestBuilder().submit(client, siteId);
    assertNull(submit.response());
    assertEquals(SC_UNAUTHORIZED.getStatusCode(), submit.exception().getCode());
  }
}
