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
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.OptionsDocumentVersionsRequest;

/**
 * GET, OPTIONS /documents/{documentId}/versions tests.
 *
 */
public class DocumentsDocumentIdVersionsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      String documentId = UUID.randomUUID().toString();
      OptionsDocumentVersionsRequest req =
          new OptionsDocumentVersionsRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.optionsDocumentVersions(req);
      // then
      final int status = 204;
      assertEquals(status, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
