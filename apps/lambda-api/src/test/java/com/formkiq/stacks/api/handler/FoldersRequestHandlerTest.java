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
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchMeta.IndexTypeEnum;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.FolderPermission;
import com.formkiq.client.model.FolderPermissionType;
import com.formkiq.client.model.SetResponse;
import com.formkiq.stacks.lambda.s3.DocumentsS3Update;
import com.formkiq.testutils.api.ApiHttpClient;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import com.formkiq.testutils.api.SetBearers;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentTagRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentContentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentUrlRequestBuilder;
import com.formkiq.testutils.api.folders.AddFolderRequestBuilder;
import com.formkiq.testutils.api.folders.GetFolderPermissionsRequestBuilder;
import com.formkiq.testutils.api.folders.GetFoldersRequestBuilder;
import com.formkiq.testutils.api.folders.SetFolderPermissionsRequestBuilder;
import com.formkiq.testutils.aws.s3.S3EventJsonBuilder;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.Test;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddFolderRequest;
import com.formkiq.client.model.AddFolderResponse;
import com.formkiq.client.model.DeleteFolderResponse;
import com.formkiq.client.model.GetFoldersResponse;
import com.formkiq.client.model.MoveFolderRequest;
import com.formkiq.client.model.MoveFolderResponse;
import com.formkiq.client.model.SearchResultDocument;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;

/**
 * 
 * Test Handlers for: GET/POST /folders, DELETE /folders/{indexKey}.
 *
 */
public class FoldersRequestHandlerTest extends AbstractApiClientRequestTest {

  private static Map<String, Object> createS3Map(final String siteId,
      final DocumentArtifact document) {
    String s3Key =
        SiteIdKeyGenerator.createS3Key(siteId, document.documentId(), document.artifactId());

    return new S3EventJsonBuilder()
        .addRecord(new S3EventJsonBuilder.RecordBuilder().withEventName("ObjectCreated:Put")
            .withS3(new S3EventJsonBuilder.S3Builder().withBucket(BUCKET_NAME).withObject(s3Key)))
        .build();
  }

  private AddFolderResponse addFolder(final String siteId, final String path) throws ApiException {
    return this.foldersApi.addFolder(new AddFolderRequest().path(path), siteId, null);
  }

  /**
   * Add Folder Null Request.
   */
  @Test
  void addFolderInvalidRequest() throws IOException, InterruptedException {
    // given
    // when
    var response =
        ApiHttpClient.send(DEFAULT_SITE_ID, server.getBasePath() + "/folders", "POST", "null");

    // then
    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
    assertEquals("{\"message\":\"invalid JSON body\"}", response.body());
  }

  /**
   * POST /folders/{indexKey}/moves.
   *
   * @throws Exception Exception
   */
  @Test
  void addFolderMoveRequest() throws Exception {
    // given
    S3Service s3 = getAwsServices().getExtension(S3Service.class);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      s3.deleteAllFiles(STAGE_BUCKET_NAME);
      setBearerToken(siteId);

      String sourcePath = "source-" + ID.uuid();
      String targetPath = "target-" + ID.uuid();
      String indexKey = createFolder(siteId, sourcePath);

      // when
      MoveFolderResponse response =
          this.foldersApi.moveFolder(indexKey, new MoveFolderRequest().path(targetPath), siteId);

      // then
      assertEquals("folder move request created", response.getMessage());

      ListObjectsResponse listObjects = s3.listObjects(STAGE_BUCKET_NAME, "tempfiles/moves/");
      assertEquals(1, listObjects.contents().size());

      String key = listObjects.contents().getFirst().key();
      Map<String, Object> request =
          fromJson(s3.getContentAsString(STAGE_BUCKET_NAME, key, null), Map.class);
      s3.deleteObject(STAGE_BUCKET_NAME, key, null);

      assertEquals(sourcePath + "/", request.get("sourcePath"));
      assertEquals(targetPath + "/", request.get("targetPath"));
      assertEquals(siteId != null ? siteId : DEFAULT_SITE_ID, request.get("siteId"));
      assertEquals("joesmith", request.get("userId"));
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

  private void assertFolderPermission(final String siteId, final String indexKey,
      final String roleName, final String permissions) {
    List<FolderPermission> roles = getFolderPermissions(siteId, indexKey);
    assertEquals(1, roles.size());
    assertEquals(roleName, roles.getFirst().getRoleName());
    assertEquals(permissions, notNull(roles.getFirst().getPermissions()).stream()
        .map(FolderPermissionType::getValue).collect(Collectors.joining(",")));

  }

  private String createFolder(final String siteId, final String path) throws ApiException {
    var response = addFolder(siteId, path);
    assertEquals("created folder", response.getMessage());

    int pos = path.lastIndexOf('/');
    var parentFolder = pos > 0 ? path.substring(0, pos) : "";
    var filename = Strings.getFilename(path);
    var files =
        new GetFoldersRequestBuilder().path(parentFolder).limit("100").submit(client, siteId);

    List<SearchResultDocument> docs = notNull(files.response().getDocuments());
    return docs.stream().filter(d -> d.getIndexKey() != null && filename.equals(d.getPath()))
        .map(SearchResultDocument::getIndexKey).findFirst().orElse(null);
  }

  private DocumentsS3Update getDocumentsS3Update() {
    return new DocumentsS3Update(getAwsServices());
  }

  private List<FolderPermission> getFolderPermissions(final String siteId, final String indexKey) {
    var perm = new GetFolderPermissionsRequestBuilder().indexKey(indexKey).submit(client, siteId);
    return notNull(perm.response().getRoles());
  }

  private List<SearchResultDocument> getSearchResultDocuments(final String siteId,
      final String indexKey) {
    return notNull(new GetFoldersRequestBuilder().indexKey(indexKey).submit(client, siteId)
        .response().getDocuments());
  }

  private List<SearchResultDocument> searchMeta(final String siteId, final String path)
      throws ApiException {
    DocumentSearchRequest req = new DocumentSearchRequest()
        .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(path + "/")));
    return notNull(this.searchApi.documentSearch(req, siteId, null, null, null).getDocuments());
  }

  private List<SearchResultDocument> searchMetaFolderEq(final String siteId, final String path)
      throws ApiException {
    DocumentSearchRequest req = new DocumentSearchRequest().query(new DocumentSearch()
        .meta(new DocumentSearchMeta().indexType(IndexTypeEnum.FOLDER).eq(path)));
    return notNull(this.searchApi.documentSearch(req, siteId, null, null, null).getDocuments());
  }

  private void setPathPermissions(final String siteId, final String path,
      final List<FolderPermissionType> permission) throws ApiException {
    new SetFolderPermissionsRequestBuilder().path(path).addRole("aRole", permission)
        .submitOk(client, siteId);
  }

  private ApiHttpResponse<SetResponse> setPathPermissions(final String siteId,
      final String roleName, final String path, final FolderPermissionType permission) {
    return new SetFolderPermissionsRequestBuilder().path(path).addRole(roleName, permission)
        .submit(client, siteId);
  }

  private ApiHttpResponse<SetResponse> setPathReadPermissions(final String siteId,
      final String roleName, final String path) {
    return setPathPermissions(siteId, roleName, path, FolderPermissionType.READ);
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

  private <T> ApiHttpResponse<T> submit(final String role,
      final HttpRequestBuilder<?> requestBuilder, final String siteId, final boolean unauthorized) {
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
    assertEquals(
        "{\"errors\":[{\"key\":\"roleName\",\"error\":\"'roleName' is required\"},"
            + "{\"key\":\"permissions\",\"error\":\"'permissions' is required\"}]}",
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

    for (String siteId : Arrays.asList(null, ID.ulid())) {

      for (String path : List.of("somefolder23", "a/b/c")) {

        var indexKey = createFolder(siteId, path);

        // when
        var resp = setPathReadPermissions(siteId, "myrole", path);

        // then
        assertFalse(resp.isError());
        assertFolderPermission(siteId, indexKey, "myrole", "READ");

        // when
        var set = new SetFolderPermissionsRequestBuilder().path(path).addRole("john", List.of())
            .submit(client, siteId);

        // then
        assertFalse(set.isError());
        assertFolderPermission(siteId, indexKey, "john", "");
      }
    }
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
      String indexKey = folders.getFirst().getIndexKey();
      assertNotNull(indexKey);
      assertEquals("path1", folders.getFirst().getPath());

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
      assertEquals("myrole", permissions.getFirst().getRoleName());
      assertEquals("READ", notNull(permissions.getFirst().getPermissions()).stream()
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
      assertEquals("Chicago", folders.getDocuments().getFirst().getPath());

      // given
      String folderIndexKey = folders.getDocuments().getFirst().getIndexKey();

      // when
      folders =
          new GetFoldersRequestBuilder().indexKey(folderIndexKey).submit(client, siteId).response();

      // then
      assertNotNull(folders.getDocuments());
      assertEquals(1, folders.getDocuments().size());
      assertEquals("Southside", folders.getDocuments().getFirst().getPath());
      assertEquals(indexKey, folders.getDocuments().getFirst().getIndexKey());

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
      assertEquals("e0647979-13f3-4c46-9f29-4b3984ef6bca", docs0.getFirst().getPath());

      String indexKey = addFolderResponse.getIndexKey();
      assertNotNull(indexKey);

      List<SearchResultDocument> docs1 =
          getSearchResultDocuments(siteId, docs0.getFirst().getIndexKey());
      assertEquals(1, docs1.size());
      assertEquals("Order_Document_1123.pdf", docs1.getFirst().getPath());
      assertEquals(indexKey, docs1.getFirst().getIndexKey());

      // when
      DeleteFolderResponse response = this.foldersApi.deleteFolder(indexKey, siteId, null);

      // then
      assertEquals("deleted folder", response.getMessage());
      docs0 = getSearchResultDocuments(siteId, null);
      assertEquals(1, docs0.size());
      docs1 = getSearchResultDocuments(siteId, docs0.getFirst().getIndexKey());
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
      assertEquals("e0647979-13f3-4c46-9f29-4b3984ef6bca", docs0.getFirst().getPath());

      String indexKey = addFolderResponse.getIndexKey();
      assertNotNull(indexKey);

      List<SearchResultDocument> docs1 =
          getSearchResultDocuments(siteId, docs0.getFirst().getIndexKey());
      assertEquals(1, docs1.size());
      assertEquals("Order_Document_1123.pdf", docs1.getFirst().getPath());
      assertEquals(indexKey, docs1.getFirst().getIndexKey());

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

        var document = new AddDocumentRequestBuilder().path(path).contentType("text/plain")
            .content(content).getDocument(client, siteId);
        getDocumentsS3Update().handleRequest(createS3Map(siteId, document), null);
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
      assertEquals(content.length(), documents.get(0).getContentLength());
      assertEquals(content.length(), documents.get(1).getContentLength());

      documents = response1.getDocuments();
      assertNotNull(documents);
      assertEquals(1, documents.size());
      assertEquals("NewYork/sample1.txt", documents.getFirst().getPath());
      assertEquals(content.length(), documents.getFirst().getContentLength());
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
      assertEquals("Chicago", documents.getFirst().getPath());

      String indexKey0 = documents.getFirst().getIndexKey();

      response =
          new GetFoldersRequestBuilder().indexKey(indexKey0).submit(client, siteId).response();
      documents = response.getDocuments();

      final int expected = 10;
      assertNotNull(documents);
      assertEquals(expected, documents.size());
      assertEquals("Chicago/sample_0", documents.get(0).getPath());
      assertEquals("Chicago/sample_1", documents.get(1).getPath());

      response = new GetFoldersRequestBuilder().indexKey(indexKey0).next(response.getNext())
          .submit(client, siteId).throwIfError().response();
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
    try (ExecutorService executorService = Executors.newFixedThreadPool(threadPool)) {
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
      assertEquals(TRUE, docs.getFirst().getFolder());
      assertEquals("Chicago", docs.getFirst().getPath());

      docs = notNull(new GetFoldersRequestBuilder().path("Chicago").limit("200")
          .submit(client, siteId).response().getDocuments());

      assertEquals(numberOfThreads, docs.size());
    }
  }

  /**
   * Test Get Invalid Folder permissions.
   *
   */
  @Test
  void testGetInvalidFolderPermissions() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId + "_govern");

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
   * Test READ permission for GET /url and /content.
   *
   */
  @Test
  void testGetUrlContentUrlsWithPermissions() throws ApiException {
    // given
    String siteId = DEFAULT_SITE_ID;
    new SetBearers().apply(client, new String[] {"Admins", siteId});
    var path = "somefolder3";

    // when - create folder
    createFolder(siteId, path);

    // set folder permissions
    setPathPermissions(siteId, path, List.of());

    // given
    String content = "mycontent";

    // when - add doc
    var document = new AddDocumentRequestBuilder().path(path + "/test.txt").content(content)
        .getDocument(client, siteId);

    // given
    new SetBearers().apply(client, new String[] {"aRole", siteId});

    // when - get doc url / content
    var url = new GetDocumentUrlRequestBuilder(document).submit(client, siteId);

    // then
    assertTrue(url.isError());
    assertEquals(SC_UNAUTHORIZED.getStatusCode(), url.exception().getCode());
    assertEquals("{\"message\":\"fkq access denied\"}", url.exception().getResponseBody());

    var docContent = new GetDocumentContentRequestBuilder(document).submit(client, siteId);
    assertTrue(docContent.isError());
    assertEquals(SC_UNAUTHORIZED.getStatusCode(), docContent.exception().getCode());
    assertEquals("{\"message\":\"fkq access denied\"}", docContent.exception().getResponseBody());

    // given
    new SetBearers().apply(client, new String[] {siteId, "Admins"});
    assertFalse(setPathPermissions(siteId, siteId, path, FolderPermissionType.READ).isError());

    new SetBearers().apply(client, new String[] {siteId});

    // when - get doc url / content
    url = new GetDocumentUrlRequestBuilder(document).submit(client, siteId);
    docContent = new GetDocumentContentRequestBuilder(document).submit(client, siteId);

    // then
    assertFalse(url.isError());
    assertFalse(docContent.isError());
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
          getSearchResultDocuments(siteId, list.getFirst().getIndexKey());
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
        getSearchResultDocuments(siteId, root.getFirst().getIndexKey());
    assertEquals(1, withPerms.size());
    assertEquals("path3", withPerms.getFirst().getPath());

    List<SearchResultDocument> withPermsSearch = searchMeta(siteId, "path1");
    assertEquals(1, withPermsSearch.size());
    assertEquals("path3", withPermsSearch.getFirst().getPath());

    setBearerToken(new String[] {siteId, "student", "Admins"});

    // when
    assertNull(setPathReadPermissions(siteId, "student", "path1/path2").exception());

    // then
    setBearerToken(new String[] {siteId, "student"});

    withPerms = getSearchResultDocuments(siteId, root.getFirst().getIndexKey());
    assertEquals(2, withPerms.size());
    assertEquals("path2", withPerms.get(0).getPath());
    assertEquals("path3", withPerms.get(1).getPath());

    withPermsSearch = searchMeta(siteId, "path1");
    assertEquals(2, withPermsSearch.size());
    assertEquals("path2", withPermsSearch.get(0).getPath());
    assertEquals("path3", withPermsSearch.get(1).getPath());
  }

  /**
   * Test folder READ permission filters documents from listing.
   *
   */
  @Test
  void testListDocumentWithoutFolderReadPermission() throws ApiException {
    // given
    for (var siteId : Arrays.asList(DEFAULT_SITE_ID, ID.ulid())) {
      var folder = "noReadFolder-" + ID.uuid();

      new SetBearers().apply(client, new String[] {siteId, "Admins"});
      createFolder(siteId, folder);

      new SetFolderPermissionsRequestBuilder().path(folder).addRole("API_KEY", List.of())
          .addRole("FormKiQ_User", List.of()).submitOk(client, siteId);

      var document0 = new AddDocumentRequestBuilder().path(folder + "/test0.txt").content()
          .getDocument(client, siteId);
      var document1 = new AddDocumentRequestBuilder().path(folder + "/test1.txt").content()
          .getDocument(client, siteId);

      new SetBearers().apply(client, new String[] {siteId, "API_KEY", "FormKiQ_User"});

      // when
      var url = new GetDocumentUrlRequestBuilder(document0).submit(client, siteId);
      var docContent = new GetDocumentContentRequestBuilder(document1).submit(client, siteId);
      final var rootFolder =
          new GetFoldersRequestBuilder().path("").getFolderDocuments(client, siteId);
      final var folderDocuments =
          new GetFoldersRequestBuilder().path(folder).getFolderDocuments(client, siteId);
      final var searchDocuments = searchMeta(siteId, folder);

      // then
      assertTrue(url.isError());
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), url.exception().getCode());
      assertTrue(docContent.isError());
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), docContent.exception().getCode());
      assertEquals(0, rootFolder.size(), "GET root folder");
      assertEquals(0, folderDocuments.size(), "GET /folders");
      assertEquals(0, searchDocuments.size(), "POST /search");

      // given - subfolder with folder permissions
      new SetBearers().apply(client, new String[] {siteId, "Admins"});
      createFolder(siteId, "anotherfolder/" + folder);
      new SetFolderPermissionsRequestBuilder().path("anotherfolder/" + folder)
          .addRole("API_KEY", List.of()).addRole("FormKiQ_User", List.of())
          .submitOk(client, siteId);

      new AddDocumentRequestBuilder().path("anotherfolder/" + folder + "/test2.txt").content()
          .getDocument(client, siteId);

      new SetBearers().apply(client, new String[] {siteId, "API_KEY", "FormKiQ_User"});

      // when
      final var rootFolder2 =
          new GetFoldersRequestBuilder().path("").getFolderDocuments(client, siteId);
      var anotherFolder =
          new GetFoldersRequestBuilder().path("anotherfolder/").getFolderDocuments(client, siteId);

      // then
      assertEquals(1, rootFolder2.size(), "GET root folder");
      assertEquals(0, anotherFolder.size(), "GET anotherFolder/ folder");
    }
  }

  /**
   * Test folder READ permission filters POST /search meta indexType=folder with trailing slash eq.
   *
   */
  @Test
  void testSearchFolderEqTrailingSlashWithoutFolderReadPermission() throws ApiException {
    // given
    var parent = "parent-" + ID.uuid();
    var child = "hidden-" + ID.uuid();

    setBearerToken(new String[] {"Admins"});
    createFolder(DEFAULT_SITE_ID, parent + "/" + child);

    new SetFolderPermissionsRequestBuilder().path(parent + "/" + child)
        .addRole("default_read", List.of()).submitOk(client, DEFAULT_SITE_ID);

    setBearerToken(new String[] {"default_read"});

    // when
    var documents = searchMetaFolderEq(DEFAULT_SITE_ID, parent + "/");

    // then
    assertEquals(0, documents.size(), "POST /search meta.indexType=folder eq with trailing slash");
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
   * Test Set Permissions missing PermissionType.
   *
   */
  @Test
  void testSetPermissionsWithoutPermissionSet() throws ApiException {
    // given
    String siteId = DEFAULT_SITE_ID;
    new SetBearers().apply(client, new String[] {"Admins", siteId});
    var path = "somefolder3";

    // when - create folder
    createFolder(siteId, path);

    // when
    var resp = setPathPermissions(siteId, "aRole", path, null);

    // then
    assertTrue(resp.isError());
    assertEquals(SC_BAD_REQUEST.getStatusCode(), resp.exception().getCode());
    assertEquals(
        "{\"errors\":[{\"key\":\"permissions\"," + "\"error\":\"'permissions' is required\"}]}",
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
    DocumentArtifact document =
        DocumentArtifact.of(resp.response().getDocumentId(), resp.response().getArtifactId());

    // add attribute
    AddDocumentAttributeRequestBuilder addAttributeReq =
        new AddDocumentAttributeRequestBuilder(document).addAttribute("myKey", "1234");
    submit("student", addAttributeReq, siteId, true);
    submit("teacher", addAttributeReq, siteId, false);

    // add tag
    AddDocumentTagRequestBuilder addTagReq =
        new AddDocumentTagRequestBuilder(document).addTag("myTag", "111");
    submit("student", addTagReq, siteId, true);
    submit("teacher", addTagReq, siteId, false);

    // add subfolder
    AddFolderRequestBuilder addFolderReq = new AddFolderRequestBuilder().path("path10/mysubfolder");
    submit("student", addFolderReq, siteId, true);
    submit("teacher", addFolderReq, siteId, false);

    // delete document
    DeleteDocumentRequestBuilder deleteReq = new DeleteDocumentRequestBuilder(document);

    submit("student", deleteReq, siteId, true);
    submit("teacher", deleteReq, siteId, false);
  }
}
