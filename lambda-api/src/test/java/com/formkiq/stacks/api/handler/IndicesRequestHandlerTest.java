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
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.SearchResultDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DeleteIndicesResponse;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/{type}/{key}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class IndicesRequestHandlerTest extends AbstractApiClientRequestTest {

  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** {@link DocumentSearchService}. */
  private static DocumentSearchService dss;

  /**
   * BeforeAll.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder db = DynamoDbTestServices.getDynamoDbConnection();
    documentService = new DocumentServiceImpl(db, DOCUMENTS_TABLE, DOCUMENT_SYNCS_TABLE,
        new DocumentVersionServiceNoVersioning());
    dss = new DocumentSearchServiceImpl(db, documentService, DOCUMENTS_TABLE);
  }

  /**
   * DELETE /indices/{type}/{key} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete01() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);
      documentService.deleteDocument(siteId, item.getDocumentId(), false);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results =
          dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      // when
      DeleteIndicesResponse response = this.indexApi.deleteIndex(indexKey, "folder", siteId);

      // then
      assertEquals("Folder deleted", response.getMessage());

      results = dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * DELETE /indices/{type}/{key} request, folder not empty.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete02() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results =
          dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Folder not empty\"}", e.getResponseBody());
      }

      results = dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
    }
  }

  /**
   * DELETE /indices/{type}/{key} request, invalid key.
   *
   */
  @Test
  public void testHandleDelete03() {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String indexKey = "12345";

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid indexKey '12345'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /indices/{type}/{key} request, TAGS type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete04() throws Exception {

    String indexType = "tags";
    SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().indexType(indexType));

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      DocumentItem item = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      String tagKey = "category";
      String tagValue = "person";
      DocumentTag tag = new DocumentTag(item.getDocumentId(), tagKey, tagValue, new Date(), "joe");
      documentService.saveDocument(siteId, item, List.of(tag));

      PaginationResults<DynamicDocumentItem> results =
          dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());

      String indexKey = "category";

      // when
      DeleteIndicesResponse deleteIndex = this.indexApi.deleteIndex(indexKey, indexType, siteId);

      // then
      assertEquals("Folder deleted", deleteIndex.getMessage());

      results = dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }


  /**
   * DELETE /indices/{type}/{key} request, invalid type.
   *
   */
  @Test
  public void testHandleDelete05() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String indexKey = "12345";

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "asd", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid indexKey '12345'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /indices/{type}/{key} with invalid siteId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete06() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);
      documentService.deleteDocument(siteId, item.getDocumentId(), false);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results =
          dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", "finance");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      }

      results = dss.search(siteId, q, null, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
    }
  }

  /**
   * DELETE /indices/{type}/{key} with folder.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete07() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String path = "test/4000007025   text .pdf/";

      AddDocumentRequest req = new AddDocumentRequest().content("test").path(path);

      try {
        this.documentsApi.addDocument(req, siteId, null);
      } catch (IllegalArgumentException e) {
        // bug in API AddDocumentResponse doesn't support messages for created folders
        // safe to ignore
      }

      DocumentSearchMeta meta = new DocumentSearchMeta().folder("");
      DocumentSearchRequest sreq =
          new DocumentSearchRequest().query(new DocumentSearch().meta(meta));

      // when
      List<SearchResultDocument> docs =
          notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());

      // then
      assertEquals(1, docs.size());
      assertNotNull(docs.get(0).getIndexKey());
      assertEquals("test", docs.get(0).getPath());
      assertEquals(Boolean.TRUE, docs.get(0).getFolder());
      assertNotNull(docs.get(0).getDocumentId());

      // given
      meta.folder("test");

      // when
      docs = notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());

      // then
      assertEquals(1, docs.size());
      assertEquals(Boolean.TRUE, docs.get(0).getFolder());
      assertEquals("4000007025   text .pdf", docs.get(0).getPath());
      assertNotNull(docs.get(0).getIndexKey());

      // when
      DeleteIndicesResponse response =
          this.indexApi.deleteIndex(docs.get(0).getIndexKey(), "folder", siteId);

      // then
      assertEquals("Folder deleted", response.getMessage());
      docs = notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());
      assertEquals(0, docs.size());
    }
  }

  /**
   * DELETE /indices/{type}/{key} with invalid key.
   *
   */
  @Test
  public void testHandleDelete08() {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String indexKey = "7b893efa-8ab4-4745-b72a-f23c99fdf917#4000007025   text .pdf";

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"invalid indexKey '" + indexKey + "'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /indices/{type}/{key} with file.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete09() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String path = "test/4000007025   text .pdf";

      AddDocumentRequest req = new AddDocumentRequest().content("test").path(path).content("data");

      this.documentsApi.addDocument(req, siteId, null);

      DocumentSearchMeta meta = new DocumentSearchMeta().folder("");
      DocumentSearchRequest sreq =
          new DocumentSearchRequest().query(new DocumentSearch().meta(meta));

      // when
      List<SearchResultDocument> docs =
          notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());

      // then
      assertEquals(1, docs.size());
      assertNotNull(docs.get(0).getIndexKey());
      assertEquals("test", docs.get(0).getPath());
      assertEquals(Boolean.TRUE, docs.get(0).getFolder());
      assertNotNull(docs.get(0).getDocumentId());

      // given
      meta.folder("test");

      // when
      docs = notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());

      // then
      assertEquals(1, docs.size());
      assertNull(docs.get(0).getFolder());
      assertEquals(path, docs.get(0).getPath());
      assertNull(docs.get(0).getIndexKey());

      // when
      DeleteIndicesResponse response = this.indexApi.deleteIndex(path, "folder", siteId);

      // then
      assertEquals("File deleted", response.getMessage());
      docs = notNull(this.searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());
      assertEquals(0, docs.size());
    }
  }
}
