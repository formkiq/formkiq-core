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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Document;
import com.formkiq.stacks.client.models.DocumentSearchQuery;
import com.formkiq.stacks.client.models.DocumentSearchResponseFields;
import com.formkiq.stacks.client.models.DocumentSearchTag;
import com.formkiq.stacks.client.models.Documents;
import com.formkiq.stacks.client.requests.AddDocumentTagRequest;
import com.formkiq.stacks.client.requests.DeleteDocumentRequest;
import com.formkiq.stacks.client.requests.SearchDocumentsRequest;

/**
 * OPTIONS, POST /search tests.
 *
 */
public class DocumentsSearchRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Test Raw /search.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsRawSearch01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      SearchDocumentsRequest req = new SearchDocumentsRequest()
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("untagged")));
      // when
      HttpResponse<String> response = client.searchAsHttpResponse(req);
      assertEquals("200", String.valueOf(response.statusCode()));
      assertRequestCorsHeaders(response.headers());
      assertTrue(response.body().contains("\"documents\":["));

      HttpResponse<String> options = client.optionsSearch();
      assertPreflightedCorsHeaders(options.headers());
    }
  }

  /**
   * Test /search.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch01() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      addDocumentWithoutFile(client, null, null);
      SearchDocumentsRequest req = new SearchDocumentsRequest()
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("untagged")));
      // when
      Documents documents = client.search(req);

      // then
      assertFalse(documents.documents().isEmpty());

      Document doc = documents.documents().get(0);
      assertNotNull(doc.documentId());
      assertNotNull(doc.insertedDate());
      assertEquals(doc.insertedDate(), doc.lastModifiedDate());
      assertNotNull(doc.userId());
    }
  }

  /**
   * Test /search paging .
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch02() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      String next = "3aa3a255-6a67-4d05-8e67-f7a22b827433";
      SearchDocumentsRequest req = new SearchDocumentsRequest()
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("untagged"))).next(next);

      // when
      HttpResponse<String> response = client.searchAsHttpResponse(req);

      // then
      assertEquals("200", String.valueOf(response.statusCode()));
      assertRequestCorsHeaders(response.headers());
      assertTrue(response.body().contains("\"documents\":["));

      HttpResponse<String> options = client.optionsSearch();
      assertPreflightedCorsHeaders(options.headers());
    }
  }

  /**
   * Test /search for specific DocumentIds.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch03() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      String documentId = addDocumentWithoutFile(client, null, null);
      SearchDocumentsRequest req = new SearchDocumentsRequest().query(new DocumentSearchQuery()
          .tag(new DocumentSearchTag().key("untagged")).documentIds(Arrays.asList(documentId)));
      // when
      Documents documents = client.search(req);

      // then
      assertEquals(1, documents.documents().size());

      Document doc = documents.documents().get(0);
      assertEquals(documentId, doc.documentId());
      assertNotNull(doc.insertedDate());
      assertEquals(doc.insertedDate(), doc.lastModifiedDate());
      assertNotNull(doc.userId());
    }
  }

  /**
   * Test /search for invalid specific DocumentIds.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch04() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      addDocumentWithoutFile(client, null, null);
      SearchDocumentsRequest req = new SearchDocumentsRequest()
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("untagged"))
              .documentIds(Arrays.asList(UUID.randomUUID().toString())));

      // when
      Documents documents = client.search(req);

      // then
      assertEquals(0, documents.documents().size());
    }
  }

  /**
   * Test /search for specific DocumentIds & eq.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch05() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTagRequest tagRequest =
          new AddDocumentTagRequest().documentId(documentId).tagKey("test").tagValue("somevalue");
      client.addDocumentTag(tagRequest);
      SearchDocumentsRequest req = new SearchDocumentsRequest()
          .responseFields(new DocumentSearchResponseFields().tags(Arrays.asList("test")))
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("test").eq("somevalue"))
              .documentIds(Arrays.asList(documentId)));

      // when
      Documents documents = client.search(req);
      HttpResponse<String> body = client.searchAsHttpResponse(req);

      // then
      assertTrue(body.body().contains("\"tags\":{\"test\":\"somevalue\"}"));
      assertEquals(1, documents.documents().size());

      Document doc = documents.documents().get(0);
      assertEquals(documentId, doc.documentId());
      assertNotNull(doc.insertedDate());
      assertNotNull(doc.userId());

      // given
      req = new SearchDocumentsRequest()
          .query(new DocumentSearchQuery().tag(new DocumentSearchTag().key("test").eq("somevalue2"))
              .documentIds(Arrays.asList(documentId)));

      // when
      documents = client.search(req);

      // then
      assertEquals(0, documents.documents().size());
    }
  }

  /**
   * Test /search for specific DocumentIds & eqOr.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch06() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTagRequest tagRequest =
          new AddDocumentTagRequest().documentId(documentId).tagKey("test").tagValue("somevalue");
      client.addDocumentTag(tagRequest);
      SearchDocumentsRequest req = new SearchDocumentsRequest().query(new DocumentSearchQuery()
          .tag(new DocumentSearchTag().key("test").eqOr(Arrays.asList("somevalue")))
          .documentIds(Arrays.asList(documentId)));

      // when
      Documents documents = client.search(req);

      // then
      assertEquals(1, documents.documents().size());

      Document doc = documents.documents().get(0);
      assertEquals(documentId, doc.documentId());
      assertNotNull(doc.insertedDate());
      assertNotNull(doc.userId());

      // given
      req = new SearchDocumentsRequest().query(new DocumentSearchQuery()
          .tag(new DocumentSearchTag().key("test").eqOr(Arrays.asList("somevalue2")))
          .documentIds(Arrays.asList(documentId)));

      // when
      documents = client.search(req);

      // then
      assertEquals(0, documents.documents().size());
    }
  }

  /**
   * Test /search for eq & eqOr.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch07() throws Exception {
    // given
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      String tagKey = UUID.randomUUID().toString();
      String documentId = addDocumentWithoutFile(client, null, null);
      AddDocumentTagRequest tagRequest =
          new AddDocumentTagRequest().documentId(documentId).tagKey(tagKey).tagValue("somevalue");
      client.addDocumentTag(tagRequest);
      SearchDocumentsRequest req = new SearchDocumentsRequest().query(new DocumentSearchQuery()
          .tag(new DocumentSearchTag().key(tagKey).eqOr(Arrays.asList("somevalue"))));

      // when
      Documents documents = client.search(req);

      // then
      assertEquals(1, documents.documents().size());

      Document doc = documents.documents().get(0);
      assertEquals(documentId, doc.documentId());
      assertNotNull(doc.insertedDate());
      assertNotNull(doc.userId());

      // given
      req = new SearchDocumentsRequest().query(new DocumentSearchQuery()
          .tag(new DocumentSearchTag().key(tagKey).eqOr(Arrays.asList("somevalue2"))));

      // when
      documents = client.search(req);

      // then
      assertEquals(0, documents.documents().size());
    }
  }

  /**
   * Test /search.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDocumentsSearch08() throws Exception {
    // given
    String siteId = null;
    String path = "some/thing/else/My Documents.pdf";
    String text = "My Documents";
    final int limit = 100;
    for (FormKiqClientV1 client : getFormKiqClients(null)) {

      String documentId = addDocumentWithoutFile(client, siteId, path);
      SearchDocumentsRequest req = new SearchDocumentsRequest().siteId(siteId)
          .query(new DocumentSearchQuery().text(text)).limit(limit);

      Documents response = null;
      Optional<Document> o = Optional.empty();

      // when
      while (o.isEmpty()) {
        response = client.search(req);
        o = response.documents().stream().filter(d -> documentId.equals(d.documentId())).findAny();
        TimeUnit.SECONDS.sleep(1);
      }

      // then
      assertNotNull(response);
      assertFalse(response.documents().isEmpty());
      assertTrue(response.documents().get(0).path().startsWith("some/thing/else/My Documents"));

      // given
      DeleteDocumentRequest delReq =
          new DeleteDocumentRequest().siteId(siteId).documentId(documentId);

      // when
      boolean deleteResponse = client.deleteDocument(delReq);

      // then
      assertTrue(deleteResponse);

      while (o.isPresent()) {
        response = client.search(req);
        o = response.documents().stream().filter(d -> documentId.equals(d.documentId())).findAny();
        TimeUnit.SECONDS.sleep(1);
      }

      assertFalse(o.isPresent());
    }
  }
}
