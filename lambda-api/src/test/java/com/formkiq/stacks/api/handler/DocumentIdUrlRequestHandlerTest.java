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

import static com.formkiq.testutils.aws.TestServices.ACCESS_POINT_S3_BUCKET;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.Watermark;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.impl.bootstrap.HttpServer;

/** Unit Tests for request /documents/{documentId}/url. */
public class DocumentIdUrlRequestHandlerTest extends AbstractApiClientRequestTest {

  /** {@link HttpServer}. */
  private final HttpService http = new HttpServiceJdk11();
  /** {@link DocumentService}. */
  private DocumentService documentService;

  /**
   * Before.
   *
   */
  @BeforeEach
  public void before() {

    AwsServiceCache awsServices = getAwsServices();
    awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
    awsServices.register(DocumentService.class, new DocumentServiceExtension());

    this.documentService = awsServices.getExtension(DocumentService.class);
  }

  /**
   * /documents/{documentId}/url request.
   * 
   * Tests No Content-Type, Content-Type matches DocumentItem's Content Type and Document Format
   * exists.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent01() throws Exception {

    for (String contentType : Arrays.asList(null, "application/pdf", "text/plain")) {

      for (String siteId : Arrays.asList(null, ID.uuid())) {
        // given
        setBearerToken(siteId);

        String documentId = ID.uuid();
        String userId = "jsmith";

        if (contentType != null) {
          DocumentFormat format = new DocumentFormat();
          format.setContentType(contentType);
          format.setDocumentId(documentId);
          format.setInsertedDate(new Date());
          format.setUserId(userId);
          this.documentService.saveDocumentFormat(siteId, format);
        }

        String filename = "file_" + UUID.randomUUID() + ".pdf";
        DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
        item.setPath("/somepath/" + filename);
        if ("text/plain".equals(contentType)) {
          item.setContentType(contentType);
        }

        this.documentService.saveDocument(siteId, item, new ArrayList<>());

        // when
        GetDocumentUrlResponse resp =
            this.documentsApi.getDocumentUrl(documentId, siteId, null, null, null, null);

        // then
        assertNotNull(resp);
        assertNotNull(resp.getUrl());

        URI uri = new URI(resp.getUrl());
        assertTrue(uri.getQuery().contains("filename=\"" + filename + "\""));

        assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(resp.getUrl().contains("X-Amz-Expires=172800"));
        assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

        assertS3Url(resp, BUCKET_NAME, siteId, documentId);
      }
    }
  }

  /**
   * /documents/{documentId}/url request w/ duration.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();

      String userId = "jsmith";
      final int duration = 8;
      this.documentService.saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      // when
      GetDocumentUrlResponse resp =
          this.documentsApi.getDocumentUrl(documentId, siteId, null, duration, null, null);

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());

      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

      assertS3Url(resp, BUCKET_NAME, siteId, documentId);
    }
  }

  /**
   * /documents/{documentId}/url request, document not found.
   *
   */
  @Test
  public void testHandleGetDocumentContent03() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();

      // when
      try {
        this.documentsApi.getDocumentUrl(documentId, siteId, null, null, null, null);
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
   * /documents/{documentId}/url request deepLinkPath to another bucket.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent04() throws Exception {

    if (!getS3().exists("anotherbucket")) {
      getS3().createBucket("anotherbucket");
    }

    byte[] content = "Some data".getBytes(StandardCharsets.UTF_8);
    getS3().putObject("anotherbucket", "somefile.txt", content, "text/plain");

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();

      final int duration = 8;
      String userId = "jsmith";
      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setDeepLinkPath("s3://anotherbucket/somefile.txt");
      doc.setContentType("text/plain");
      this.documentService.saveDocument(siteId, doc, new ArrayList<>());

      // when
      GetDocumentUrlResponse resp =
          this.documentsApi.getDocumentUrl(documentId, siteId, null, duration, null, null);

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());

      assertTrue(resp.getUrl().contains("/anotherbucket/"));
      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

      if (siteId != null) {
        assertFalse(resp.getUrl().contains("/" + siteId + "/" + documentId));
      } else {
        assertFalse(resp.getUrl().contains("/" + documentId));
      }

      assertTrue(resp.getUrl().contains("somefile.txt"));
      assertEquals("Some data", getS3().getContentAsString("anotherbucket", "somefile.txt", null));
      assertEquals("Some data",
          this.http.get(resp.getUrl(), Optional.empty(), Optional.empty()).body());
    }
  }

  private S3Service getS3() {
    return getAwsServices().getExtension(S3Service.class);
  }

  /**
   * /documents/{documentId}/url request deepLinkPath to http url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent05() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = ID.uuid();

      String userId = "jsmith";
      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setDeepLinkPath("https://www.google.com/something/else.pdf");
      doc.setContentType("application/pdf");
      this.documentService.saveDocument(siteId, doc, new ArrayList<>());

      // when
      GetDocumentUrlResponse resp =
          this.documentsApi.getDocumentUrl(documentId, siteId, null, null, null, null);

      // then
      assertNotNull(resp);
      assertEquals("https://www.google.com/something/else.pdf", resp.getUrl());
    }
  }

  /**
   * /documents/{documentId}/url with watermarks.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent06() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      Watermark watermark1 = new Watermark().text("watermark1");
      this.attributesApi.addAttribute(new AddAttributeRequest().attribute(new AddAttribute()
          .key("wm1").dataType(AttributeDataType.WATERMARK).watermark(watermark1)), siteId);

      Watermark watermark2 = new Watermark().text("watermark2");
      this.attributesApi.addAttribute(new AddAttributeRequest().attribute(new AddAttribute()
          .key("wm2").dataType(AttributeDataType.WATERMARK).watermark(watermark2)), siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("test content")
          .addAttributesItem(
              new AddDocumentAttribute(new AddDocumentAttributeStandard().key("wm1")))
          .addAttributesItem(
              new AddDocumentAttribute(new AddDocumentAttributeStandard().key("wm2")));

      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      GetDocumentUrlResponse resp =
          this.documentsApi.getDocumentUrl(documentId, siteId, null, null, null, null);

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());
      assertS3Url(resp, ACCESS_POINT_S3_BUCKET, siteId, documentId);
    }
  }

  private void assertS3Url(final GetDocumentUrlResponse resp, final String bucket,
      final String siteId, final String documentId) {
    assertNotNull(resp.getUrl());
    assertTrue(resp.getUrl().contains(bucket));
    if (siteId != null) {
      assertTrue(resp.getUrl().contains("/" + siteId + "/" + documentId));
    } else {
      assertTrue(resp.getUrl().contains("/" + documentId));
    }
  }
}
