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
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.Document;
import com.formkiq.stacks.client.models.Documents;
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
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      SearchDocumentsRequest req = new SearchDocumentsRequest().tagKey("untagged");
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
    for (FormKiqClientV1 client : getFormKiqClients()) {
      addDocumentWithoutFile(client);
      SearchDocumentsRequest req = new SearchDocumentsRequest().tagKey("untagged");
      // when
      Documents documents = client.search(req);

      // then
      assertFalse(documents.documents().isEmpty());

      Document doc = documents.documents().get(0);
      assertNotNull(doc.documentId());
      assertNotNull(doc.insertedDate());
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
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String next = "3aa3a255-6a67-4d05-8e67-f7a22b827433";
      SearchDocumentsRequest req = new SearchDocumentsRequest().tagKey("untagged").next(next);

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
}
