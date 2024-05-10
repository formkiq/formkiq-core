/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request POST /documents. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsRequestTest extends AbstractApiClientRequestTest {

  /**
   * Save new File.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPost01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      
      AddDocumentRequest req =
          new AddDocumentRequest().content("dummy data").contentType("application/pdf");

      // when
      AddDocumentResponse responseNoSiteId = this.documentsApi.addDocument(req, null, null);
      AddDocumentResponse responseSiteId = this.documentsApi.addDocument(req, siteId, null);

      // then
      assertNotNull(responseNoSiteId.getDocumentId());
      assertNotNull(responseSiteId.getDocumentId());
      assertEquals(siteId, responseSiteId.getSiteId());
    }
  }
}
