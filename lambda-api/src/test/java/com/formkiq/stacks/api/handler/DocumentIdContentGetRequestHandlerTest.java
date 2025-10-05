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

import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.GetDocumentContentResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.testutils.aws.TestServices;

/** Unit Tests for request /documents/{documentId}/content. */
public class DocumentIdContentGetRequestHandlerTest extends AbstractApiClientRequestTest {

  /**
   * /documents/{documentId}/content request, with S3 file missing.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetDocumentS3FileMissing() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      String documentId =
          this.documentsApi.addDocumentUpload(req, siteId, null, null, null).getDocumentId();
      assertNotNull(documentId);

      // when
      try {
        this.documentsApi.getDocumentContent(documentId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent01() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("test");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentContentResponse response =
          this.documentsApi.getDocumentContent(documentId, siteId, null, null);

      // then
      assertNull(response.getContent());
      String url = response.getContentUrl();
      assertNotNull(url);

      assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(url.contains("X-Amz-Expires="));
      assertTrue(url.contains(AWS_REGION.toString()));
      assertEquals("application/octet-stream", response.getContentType());

      if (siteId != null) {
        assertTrue(url.startsWith(TestServices.getEndpointOverride(Service.S3).toString()
            + "/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(url.startsWith(
            TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/" + documentId));
      }
    }
  }

  /**
   * /documents/{documentId}/content request.
   *
   * Document not found.
   *
   */
  @Test
  public void testHandleGetDocumentContent02() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = ID.uuid();

      // when
      try {
        this.documentsApi.getDocumentContent(documentId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * /documents/{documentId}/content request.
   *
   * Tests Text Content is returned (content-type plain/text).
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent03() throws Exception {
    testReturnContent("text/plain");
  }

  /**
   * /documents/{documentId}/content request.
   *
   * Tests Text Content is returned (content-type application/x-www-form-urlencoded).
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent04() throws Exception {
    testReturnContent("application/x-www-form-urlencoded");
  }

  /**
   * /documents/{documentId}/content text/plain missing s3 file.
   *
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent05() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("test").contentType("text/plain");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      assertNotNull(documentId);

      S3Service s3 = getAwsServices().getExtension(S3Service.class);
      s3.deleteObject(BUCKET_NAME, SiteIdKeyGenerator.createS3Key(siteId, documentId), null);

      // when
      try {
        this.documentsApi.getDocumentContent(documentId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * /documents/{documentId}/content wrong text/plain content-type.
   *
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent06() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/ocr/sample.pdf"));
      String content = Base64.getEncoder().encodeToString(bytes);
      AddDocumentRequest req = new AddDocumentRequest().content(content).isBase64(Boolean.TRUE)
          .contentType("text/plain");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentContentResponse response =
          this.documentsApi.getDocumentContent(documentId, siteId, null, null);

      // then
      assertNull(response.getContent());
      assertNotNull(response.getContentUrl());
      assertEquals("text/plain", response.getContentType());
    }
  }

  /**
   * /documents/{documentId}/content TOO large content.
   *
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent07() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final int sixMb = 6 * 1024 * 1024; // 6 MB in bytes
      String content = "a".repeat(sixMb);
      AddDocumentRequest req = new AddDocumentRequest().content(content).contentType("text/plain");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      assertNotNull(documentId);

      // when
      try {
        this.documentsApi.getDocumentContent(documentId, siteId, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Response exceeds allowed size\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Test Content is returned.
   *
   * @param contentType {@link String}
   * @throws Exception Exception
   */
  private void testReturnContent(final String contentType) throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String content = "this is a test";
      AddDocumentRequest req = new AddDocumentRequest().content(content).contentType(contentType);
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentContentResponse response =
          this.documentsApi.getDocumentContent(documentId, siteId, null, null);

      // then
      assertEquals(content, response.getContent());
      assertEquals(contentType, response.getContentType());
      assertEquals(Boolean.FALSE, response.getIsBase64());
    }
  }
}
