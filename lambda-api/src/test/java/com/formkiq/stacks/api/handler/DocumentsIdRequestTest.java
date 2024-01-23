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

import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.GsonUtil;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAccessAttribute;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.SetDocumentRestoreResponse;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentsIdRequestTest extends AbstractApiClientRequestTest {

  /**
   * PUT /documents/{documentId}/restore request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSetDocumentRestore01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().path("test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);

      String documentId = response.getDocumentId();

      // when
      this.documentsApi.deleteDocument(documentId, siteId, Boolean.TRUE);

      // then
      List<Document> softDeletedDocuments = this.documentsApi
          .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, null).getDocuments();
      assertEquals(1, softDeletedDocuments.size());
      assertEquals("test.txt", softDeletedDocuments.get(0).getPath());

      List<Document> documents = this.documentsApi
          .getDocuments(siteId, null, null, null, null, null, null, null).getDocuments();
      assertEquals(0, documents.size());

      // when
      SetDocumentRestoreResponse restore = this.documentsApi.setDocumentRestore(documentId, siteId);

      // then
      assertEquals("document restored", restore.getMessage());
      softDeletedDocuments = this.documentsApi
          .getDocuments(siteId, null, Boolean.TRUE, null, null, null, null, null).getDocuments();
      assertEquals(0, softDeletedDocuments.size());
      documents = this.documentsApi.getDocuments(siteId, null, null, null, null, null, null, null)
          .getDocuments();
      assertEquals(1, documents.size());
      assertEquals("test.txt", documents.get(0).getPath());
    }
  }

  /**
   * PUT /documents/{documentId}/restore request, document not found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSetDocumentRestore02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String documentId = UUID.randomUUID().toString();

      // when
      try {
        this.documentsApi.setDocumentRestore(documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        final int code = 404;
        assertEquals(code, e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET /documents/{documentId} request, deeplink.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().deepLinkPath("test.txt");
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null);
      String documentId = response.getDocumentId();

      // when
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);

      // then
      assertEquals("test.txt", document.getDeepLinkPath());
    }
  }

  /**
   * POST /documents/{documentId} request, deeplink.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String deepLinkPath = "http://google.com/test/sample.pdf";

      AddDocumentRequest req =
          new AddDocumentRequest().deepLinkPath(deepLinkPath).contentType("application/pdf");

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();

      S3Service s3 = getAwsServices().getExtension(S3Service.class);
      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId) + ".fkb64";

      String content = s3.getContentAsString(STAGE_BUCKET_NAME, s3Key, null);

      GetDocumentResponse document =
          GsonUtil.getInstance().fromJson(content, GetDocumentResponse.class);

      // then
      assertNull(document.getPath());
      assertEquals("http://google.com/test/sample.pdf", document.getDeepLinkPath());
      assertEquals("application/pdf", document.getContentType());
    }
  }

  /**
   * POST /documents/{documentId} request, access attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleAddDocument02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("SKADJASKDSA")
          .contentType("text/plain").addAccessAttributesItem(
              new AddAccessAttribute().key("department").stringValue("marketing"));

      // when
      try {
        this.documentsApi.addDocument(req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"accessAttributes\","
            + "\"error\":\"Access attributes are only supported with "
            + "the 'open policy access' module\"}]}", e.getResponseBody());
      }
    }
  }
}
