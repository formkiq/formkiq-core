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
package com.formkiq.stacks.api;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.module.http.HttpResponseStatus.STATUS_FORBIDDEN;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.client.api.CustomIndexApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.Configuration;
import com.formkiq.client.model.DeleteIndicesResponse;
import com.formkiq.stacks.api.handler.FormKiQResponseCallback;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.FormKiqApiExtension;
import com.formkiq.testutils.aws.JwtTokenEncoder;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/{type}/{key}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class IndicesRequestHandlerTest {

  /** {@link FormKiQResponseCallback}. */
  private static final FormKiQResponseCallback CALLBACK = new FormKiQResponseCallback();
  /** FormKiQ Server. */
  @RegisterExtension
  static FormKiqApiExtension server = new FormKiqApiExtension(CALLBACK);
  /** {@link ApiClient}. */
  private ApiClient client =
      Configuration.getDefaultApiClient().setReadTimeout(0).setBasePath(server.getBasePath());
  /** {@link CustomIndexApi}. */
  private CustomIndexApi indexApi = new CustomIndexApi(this.client);
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
    documentService =
        new DocumentServiceImpl(db, DOCUMENTS_TABLE, new DocumentVersionServiceNoVersioning());
    dss = new DocumentSearchServiceImpl(db, documentService, DOCUMENTS_TABLE, null);
  }

  /**
   * Set BearerToken.
   * 
   * @param siteId {@link String}
   */
  private void setBearerToken(final String siteId) {
    String jwt = JwtTokenEncoder.encodeCognito(new String[] {siteId != null ? siteId : "default"},
        "joesmith");
    this.client.addDefaultHeader("Authorization", jwt);
  }

  /**
   * DELETE /indices/{type}/{key} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete01() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);
      documentService.deleteDocument(siteId, item.getDocumentId(), false);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      // when
      DeleteIndicesResponse response = this.indexApi.deleteIndex(indexKey, "folder", siteId);

      // then
      assertEquals("Folder deleted", response.getMessage());

      results = dss.search(siteId, q, null, MAX_RESULTS);
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
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

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
    }
  }

  /**
   * DELETE /indices/{type}/{key} request, invalid key.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete03() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String indexKey = "12345";

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid indexKey\"}", e.getResponseBody());
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), new Date(), "joe");
      String tagKey = "category";
      String tagValue = "person";
      DocumentTag tag = new DocumentTag(item.getDocumentId(), tagKey, tagValue, new Date(), "joe");
      documentService.saveDocument(siteId, item, Arrays.asList(tag));

      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());

      String indexKey = "category";

      // when
      DeleteIndicesResponse deleteIndex = this.indexApi.deleteIndex(indexKey, indexType, siteId);

      // then
      assertEquals("Folder deleted", deleteIndex.getMessage());

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }


  /**
   * DELETE /indices/{type}/{key} request, invalid type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDelete05() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String indexKey = "12345";

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "asd", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"invalid 'indexType' parameter\"}", e.getResponseBody());
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

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      documentService.saveDocument(siteId, item, null);
      documentService.deleteDocument(siteId, item.getDocumentId(), false);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      // when
      try {
        this.indexApi.deleteIndex(indexKey, "folder", "finance");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(STATUS_FORBIDDEN, e.getCode());
      }

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
    }
  }
}
