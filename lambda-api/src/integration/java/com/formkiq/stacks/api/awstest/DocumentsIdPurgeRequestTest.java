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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /documents/{documentId}/purge. */
public class DocumentsIdPurgeRequestTest extends AbstractAwsIntegrationTest {

  /**
   * DELETE /documents/{documentId}/purge request when document is missing s3 file.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPurgeDocument01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiClient client = getApiClients(siteId).get(1);

      String path = ID.uuid() + ".txt";
      DocumentsApi api = new DocumentsApi(client);

      GetDocumentUrlResponse response =
          api.getDocumentUpload(path, siteId, null, null, null, null, null);
      String documentId = response.getDocumentId();
      assertNotNull(documentId);

      // when
      DeleteResponse deleteResponse = api.purgeDocument(documentId, siteId);

      // then
      assertEquals("'" + documentId + "' object deleted all versions", deleteResponse.getMessage());
    }
  }

  /**
   * DELETE /documents/{documentId}/purge document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPurgeDocument02() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiClient client = getApiClients(siteId).get(1);
      DocumentsApi api = new DocumentsApi(client);

      byte[] content = "mycontent".getBytes(StandardCharsets.UTF_8);
      String documentId = addDocument(client, siteId, null, content, "text/plain", null);

      // when
      DeleteResponse deleteResponse = api.purgeDocument(documentId, siteId);

      // then
      assertEquals("'" + documentId + "' object deleted all versions", deleteResponse.getMessage());
    }
  }

  /**
   * DELETE /documents/{documentId}/purge document NOT admin.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPurgeDocument03() throws Exception {

    for (String siteId : Arrays.asList("default", ID.uuid())) {
      // given
      ApiClient client = getApiClients(siteId).get(2);
      DocumentsApi api = new DocumentsApi(client);

      byte[] content = "mycontent".getBytes(StandardCharsets.UTF_8);
      String documentId = addDocument(client, siteId, null, content, "text/plain", null);

      // when
      try {
        api.purgeDocument(documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals(
            "{\"message\":\"fkq access denied (groups: " + siteId + " (DELETE,READ,WRITE))\"}",
            e.getResponseBody());
      }
    }
  }
}
