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

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentSearchApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.DocumentSearchTag;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * OPTIONS, POST /search tests.
 *
 */
public class DocumentsSearchRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test /search.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch01() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {
      addDocument(client, siteId, null, new byte[] {}, null, null);
      DocumentSearchApi api = new DocumentSearchApi(client);

      DocumentSearchRequest req = new DocumentSearchRequest()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("untagged")));

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertFalse(results.getDocuments().isEmpty());

      SearchResultDocument doc = results.getDocuments().get(0);
      assertNotNull(doc.getDocumentId());
      assertNotNull(doc.getInsertedDate());
      assertEquals(doc.getInsertedDate(), doc.getLastModifiedDate());
      assertNotNull(doc.getUserId());
    }
  }

  /**
   * Test /search for specific DocumentIds.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch02() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);
      DocumentSearchRequest req = new DocumentSearchRequest().query(new DocumentSearch()
          .tag(new DocumentSearchTag().key("untagged")).documentIds(Arrays.asList(documentId)));

      DocumentSearchApi api = new DocumentSearchApi(client);

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(1, results.getDocuments().size());

      SearchResultDocument doc = results.getDocuments().get(0);
      assertEquals(documentId, doc.getDocumentId());
      assertNotNull(doc.getInsertedDate());
      assertEquals(doc.getInsertedDate(), doc.getLastModifiedDate());
      assertNotNull(doc.getUserId());
    }
  }

  /**
   * Test /search for invalid specific DocumentIds.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch03() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      addDocument(client, siteId, null, new byte[] {}, null, null);

      DocumentSearchRequest req = new DocumentSearchRequest()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("untagged"))
              .documentIds(Arrays.asList(UUID.randomUUID().toString())));

      DocumentSearchApi api = new DocumentSearchApi(client);

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(0, results.getDocuments().size());
    }
  }

  /**
   * Test /search for specific DocumentIds & eq.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch04() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      DocumentTagsApi tagApi = new DocumentTagsApi(client);

      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);
      AddDocumentTagsRequest addTagReq = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("test").value("somevalue"));

      tagApi.addDocumentTags(documentId, addTagReq, siteId, null);

      DocumentSearchRequest req =
          new DocumentSearchRequest().responseFields(new SearchResponseFields().addTagsItem("test"))
              .query(new DocumentSearch().tag(new DocumentSearchTag().key("test").eq("somevalue"))
                  .documentIds(Arrays.asList(documentId)));

      DocumentSearchApi api = new DocumentSearchApi(client);

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(1, results.getDocuments().size());
      Map<String, Object> tags = results.getDocuments().get(0).getTags();
      assertEquals(1, tags.size());
      assertEquals("somevalue", tags.get("test"));

      SearchResultDocument doc = results.getDocuments().get(0);
      assertEquals(documentId, doc.getDocumentId());
      assertNotNull(doc.getInsertedDate());
      assertNotNull(doc.getUserId());

      // given
      req = new DocumentSearchRequest().responseFields(new SearchResponseFields())
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("test").eq("somevalue2"))
              .documentIds(Arrays.asList(documentId)));

      // when
      results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(0, results.getDocuments().size());
    }
  }

  /**
   * Test /search for specific DocumentIds & eqOr.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch05() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      String documentId = addDocument(client, siteId, null, new byte[] {}, null, null);

      DocumentTagsApi tagApi = new DocumentTagsApi(client);
      AddDocumentTagsRequest addTagReq = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key("test").value("somevalue"));

      tagApi.addDocumentTags(documentId, addTagReq, siteId, null);

      DocumentSearchApi api = new DocumentSearchApi(client);

      DocumentSearchRequest req =
          new DocumentSearchRequest().query(new DocumentSearch().addDocumentIdsItem(documentId)
              .tag(new DocumentSearchTag().key("test").eqOr(Arrays.asList("somevalue"))));

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(1, results.getDocuments().size());

      SearchResultDocument doc = results.getDocuments().get(0);
      assertEquals(documentId, doc.getDocumentId());
      assertNotNull(doc.getInsertedDate());
      assertNotNull(doc.getUserId());

      // given
      req = new DocumentSearchRequest().query(new DocumentSearch().addDocumentIdsItem(documentId)
          .tag(new DocumentSearchTag().key("test").eqOr(Arrays.asList("somevalue2"))));

      // when
      results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(0, results.getDocuments().size());
    }
  }

  /**
   * Test /search for eq & eqOr.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDocumentsSearch06() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {

      byte[] content = "sample content".getBytes(StandardCharsets.UTF_8);
      DocumentSearchApi api = new DocumentSearchApi(client);
      DocumentTagsApi tagApi = new DocumentTagsApi(client);

      String tagKey = UUID.randomUUID().toString();
      String documentId = addDocument(client, siteId, "data.txt", content, "text/plain", null);

      AddDocumentTagsRequest addReq = new AddDocumentTagsRequest()
          .addTagsItem(new AddDocumentTag().key(tagKey).value("somevalue"));
      tagApi.addDocumentTags(documentId, addReq, siteId, null);

      DocumentSearchRequest req = new DocumentSearchRequest().query(
          new DocumentSearch().tag(new DocumentSearchTag().key(tagKey).addEqOrItem("somevalue")));

      // when
      DocumentSearchResponse results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(1, results.getDocuments().size());

      SearchResultDocument doc = results.getDocuments().get(0);
      assertEquals(documentId, doc.getDocumentId());
      assertNotNull(doc.getInsertedDate());
      assertNotNull(doc.getUserId());

      // given
      req = new DocumentSearchRequest().query(
          new DocumentSearch().tag(new DocumentSearchTag().key(tagKey).addEqOrItem("somevalue2")));

      // when
      results = api.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(0, results.getDocuments().size());
    }
  }

  /**
   * Test /search.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT * 2)
  public void testDocumentsSearch07() throws Exception {
    // given
    String siteId = null;
    String path = "some/thing/else/intelligent Documents.pdf";
    String text = "intelligent Documents";
    final String limit = "100";
    for (ApiClient client : getApiClients(null)) {

      DocumentSearchApi api = new DocumentSearchApi(client);

      String documentId = addDocument(client, siteId, path, new byte[] {}, null, null);

      DocumentSearchRequest req =
          new DocumentSearchRequest().query(new DocumentSearch().text(text));

      DocumentSearchResponse results = null;
      Optional<SearchResultDocument> o = Optional.empty();

      // when
      while (o.isEmpty()) {
        results = api.documentSearch(req, siteId, limit, null, null);
        o = results.getDocuments().stream().filter(d -> documentId.equals(d.getDocumentId()))
            .findAny();

        TimeUnit.SECONDS.sleep(1);
      }

      // then
      assertNotNull(results);
      assertFalse(results.getDocuments().isEmpty());
      assertTrue(results.getDocuments().get(0).getPath()
          .contains("some/thing/else/intelligent Documents"));

      // given
      DocumentsApi docApi = new DocumentsApi(client);

      // when
      docApi.deleteDocument(documentId, siteId, Boolean.FALSE);

      // then
      while (o.isPresent()) {
        results = api.documentSearch(req, siteId, limit, null, null);
        o = results.getDocuments().stream().filter(d -> documentId.equals(d.getDocumentId()))
            .findAny();
        TimeUnit.SECONDS.sleep(1);
      }

      assertFalse(o.isPresent());
    }
  }
}
