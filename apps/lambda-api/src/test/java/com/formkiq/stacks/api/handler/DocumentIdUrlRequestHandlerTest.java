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


import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.base64.StringToBase64Decoder;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
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
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentUrlRequestBuilder;
import com.formkiq.urls.UrlParser;
import com.formkiq.urls.UrlParts;
import org.junit.jupiter.api.Test;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.impl.bootstrap.HttpServer;

/** Unit Tests for request /documents/{documentId}/url. */
public class DocumentIdUrlRequestHandlerTest extends AbstractApiClientRequestTest {

  /** {@link HttpServer}. */
  private final HttpService http = new HttpServiceJdk11();

  private String addDocumentWithWatermarks(final String siteId) throws ApiException {
    Watermark watermark1 = new Watermark().text("watermark1");
    this.attributesApi.addAttribute(new AddAttributeRequest().attribute(
        new AddAttribute().key("wm1").dataType(AttributeDataType.WATERMARK).watermark(watermark1)),
        siteId);

    Watermark watermark2 = new Watermark().text("watermark2");
    this.attributesApi.addAttribute(new AddAttributeRequest().attribute(
        new AddAttribute().key("wm2").dataType(AttributeDataType.WATERMARK).watermark(watermark2)),
        siteId);

    AddDocumentRequest req = new AddDocumentRequest().content("test content")
        .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard().key("wm1")))
        .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard().key("wm2")));

    return new AddDocumentRequestBuilder(req).submitOk(this.documentsApi.getApiClient(), siteId)
        .response().getDocumentId();
  }

  private void addS3File(final String siteId, final String documentId, final String contentType) {
    getS3().putObject(BUCKET_NAME, createS3Key(siteId, documentId, null),
        "ASD".getBytes(StandardCharsets.UTF_8), contentType);
  }

  private void assertS3Url(final GetDocumentUrlResponse resp, final String siteId,
      final String documentId) {
    assertS3Url(resp.getUrl(), siteId, documentId);
  }

  private void assertS3Url(final GetDocumentUrlResponse resp, final String siteId,
      final String documentId, final String artifactId) {
    assertS3Url(resp.getUrl(), siteId, documentId, artifactId);
  }

  private void assertS3Url(final String url, final String siteId, final String documentId) {
    assertNotNull(url);
    assertTrue(url.contains(BUCKET_NAME));
    if (siteId != null) {
      assertTrue(url.contains("/" + siteId + "/" + documentId));
    } else {
      assertTrue(url.contains("/" + documentId));
    }
  }

  private void assertS3Url(final String url, final String siteId, final String documentId,
      final String artifactId) {
    assertNotNull(url);
    assertTrue(url.contains(BUCKET_NAME));
    if (siteId != null) {
      assertTrue(url.contains("/" + siteId + "/" + documentId + "/artifacts/" + artifactId));
    } else {
      assertTrue(url.contains("/" + documentId + "/artifacts/" + artifactId));
    }
  }

  private void assertS3Url(final UrlParts parts, final String siteId, final String documentId) {
    String base64 = parts.queryParameters().get("url").getFirst();
    String s = new StringToBase64Decoder().apply(base64);
    assertS3Url(s, siteId, documentId);

    assertNotNull(parts.queryParameters().get("X-Amz-Signature").getFirst());
    assertNotNull(parts.queryParameters().get("X-Amz-Algorithm").getFirst());
    assertNotNull(parts.queryParameters().get("X-Amz-Date").getFirst());
    assertNotNull(parts.queryParameters().get("X-Amz-SignedHeaders").getFirst());
  }

  private void createBucket(final String bucket) {
    if (!getS3().exists(bucket)) {
      getS3().createBucket(bucket);
    }
  }

  private S3Service getS3() {
    return getAwsServices().getExtension(S3Service.class);
  }

  /**
   * /documents/{documentId}/url request missing s3 file.
   *
   */
  @Test
  public void testGetDocumentMissingS3File() throws ApiException {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      var document = new AddDocumentRequestBuilder().content().getDocument(client, siteId);
      getS3().deleteObject(BUCKET_NAME, SiteIdKeyGenerator.createS3Key(siteId, document), null);

      // when
      var response = new GetDocumentUrlRequestBuilder(document)
          .submitError(this.documentsApi.getApiClient(), siteId);

      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), response.exception().getCode());
      assertEquals("{\"message\":\"Document " + document.documentId() + " not found.\"}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Script-bearing SVG must be downloaded even when inline delivery is requested.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetSvgDocumentUrlForcesAttachmentWhenInlineRequested() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    String filename = "script-bearing.svg";
    String contentType = "image/svg+xml";
    String content = "<svg xmlns=\"http://www.w3.org/2000/svg\">"
        + "<script>document.documentElement.setAttribute('data-script-executed', 'true');</script>"
        + "</svg>";

    DocumentArtifact document = new AddDocumentRequestBuilder().path(filename)
        .contentType(contentType).content(content).getDocument(client, siteId);

    // when
    GetDocumentUrlResponse response = new GetDocumentUrlRequestBuilder(document).setInline(true)
        .submitOk(client, siteId).response();
    assertS3Url(response, siteId, document.documentId());
    HttpResponse<String> download =
        this.http.get(response.getUrl(), Optional.empty(), Optional.empty());

    // then: verify delivery headers, not browser execution or access to Console privileges
    assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), download.statusCode());
    assertEquals(content, download.body());
    assertEquals(contentType, download.headers().firstValue("Content-Type").orElseThrow());
    String expectedDisposition = "attachment; filename*=UTF-8''" + filename;
    UrlParts parts = new UrlParser().apply(response.getUrl());
    assertAll(
        () -> assertEquals(expectedDisposition,
            parts.queryParameters().get("response-content-disposition").getFirst(),
            "The signed URL must force attachment delivery for SVG"),
        () -> assertEquals(expectedDisposition,
            download.headers().firstValue("Content-Disposition").orElseThrow(),
            "The SVG response must be an attachment even when inline=true"));
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

        // String documentId = ID.uuid();
        String userId = "jsmith";

        if (contentType != null) {
          DocumentFormat format = new DocumentFormat();
          format.setContentType(contentType);
          // format.setDocumentId(documentId);
          format.setInsertedDate(new Date());
          format.setUserId(userId);
          // this.documentService.saveDocumentFormat(siteId, format);
        }

        String filename = "file " + UUID.randomUUID() + ".pdf";

        var document = new AddDocumentRequestBuilder().path("/somepath/" + filename).content()
            .contentType("text/plain".equals(contentType) ? contentType : null)
            .getDocument(client, siteId);
        addS3File(siteId, document.documentId(), contentType);

        // when
        GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document)
            .submitOk(this.documentsApi.getApiClient(), siteId).response();

        // then
        assertNotNull(resp);
        assertNotNull(resp.getUrl());

        URI uri = new URI(resp.getUrl());
        assertTrue(uri.getQuery().contains("filename*=UTF-8''" + filename));

        assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(resp.getUrl().contains("X-Amz-Expires=172800"));
        assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

        assertS3Url(resp, siteId, document.documentId());
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

      final int duration = 8;
      var document = new AddDocumentRequestBuilder().content().getDocument(client, siteId);

      // when
      GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document).duration(duration)
          .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());

      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

      assertS3Url(resp, siteId, document.documentId());
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
        new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
            .submitOk(this.documentsApi.getApiClient(), siteId);
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

    createBucket("anotherbucket");

    byte[] content = "Some data".getBytes(StandardCharsets.UTF_8);
    getS3().putObject("anotherbucket", "somefile.txt", content, "text/plain");

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final int duration = 8;
      var document = new AddDocumentRequestBuilder().deepLink("s3://anotherbucket/somefile.txt")
          .getDocument(client, siteId);

      // when
      GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document).duration(duration)
          .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());

      assertTrue(resp.getUrl().contains("/anotherbucket/"));
      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));

      if (siteId != null) {
        assertFalse(resp.getUrl().contains("/" + siteId + "/" + document.documentId()));
      } else {
        assertFalse(resp.getUrl().contains("/" + document.documentId()));
      }

      assertTrue(resp.getUrl().contains("somefile.txt"));
      assertTrue(resp.getUrl().contains("text%2Fplain"));
      assertEquals("Some data", getS3().getContentAsString("anotherbucket", "somefile.txt", null));
      assertEquals("Some data",
          this.http.get(resp.getUrl(), Optional.empty(), Optional.empty()).body());
    }
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

      var document = new AddDocumentRequestBuilder()
          .deepLink("https://www.google.com/something/else.pdf").getDocument(client, siteId);

      // when
      GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document)
          .submitOk(this.documentsApi.getApiClient(), siteId).response();

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
    // given
    String watermarkFunctionUrl =
        "https://3u7ocozmmheiodztsnuy72zswa0mzlsu.lambda-url.us-east-2.on.aws/";
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      server.getEnvironmentMap().put("WATERMARK_FUNCTION_URL", watermarkFunctionUrl);

      String documentId = addDocumentWithWatermarks(siteId);
      getS3().putObject(BUCKET_NAME, createS3Key(siteId, documentId, null),
          "ASD".getBytes(StandardCharsets.UTF_8), null);

      // when
      GetDocumentUrlResponse resp =
          new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
              .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());
      assertTrue(resp.getUrl().startsWith(watermarkFunctionUrl));
      UrlParts parts = new UrlParser().apply(resp.getUrl());
      assertS3Url(parts, siteId, documentId);
    }

    // given
    String siteId = ID.uuid();
    setBearerToken(new String[] {siteId, "admins"});

    String documentId = addDocumentWithWatermarks(siteId);
    getS3().putObject(BUCKET_NAME, createS3Key(siteId, documentId, null),
        "ASD".getBytes(StandardCharsets.UTF_8), null);

    // when
    GetDocumentUrlResponse resp =
        new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
            .submitOk(this.documentsApi.getApiClient(), siteId).response();

    // then
    assertNotNull(resp);
    assertNotNull(resp.getUrl());
    assertTrue(resp.getUrl().startsWith(watermarkFunctionUrl));
    UrlParts parts = new UrlParser().apply(resp.getUrl());
    assertS3Url(parts, siteId, documentId);
  }

  /**
   * /documents/{documentId}/url with watermarks and missing environment variable.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent07() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      server.getEnvironmentMap().remove("WATERMARK_FUNCTION_URL");

      String documentId = addDocumentWithWatermarks(siteId);

      // when
      GetDocumentUrlResponse resp =
          new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
              .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());
      assertS3Url(resp, siteId, documentId);
    }

    // given
    String siteId = ID.uuid();
    setBearerToken(new String[] {siteId, "admins"});

    String documentId = addDocumentWithWatermarks(siteId);

    // when
    GetDocumentUrlResponse resp =
        new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
            .submitOk(this.documentsApi.getApiClient(), siteId).response();

    // then
    assertNotNull(resp);
    assertNotNull(resp.getUrl());
    assertS3Url(resp, siteId, documentId);
  }

  /**
   * /documents/{documentId}/url with watermarks and ByPassWatermark as Admin/govern.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent08() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = addDocumentWithWatermarks(siteId);

      for (String[] groups : Arrays.asList(new String[] {siteId, "admins"},
          new String[] {siteId, siteId + "_govern"})) {
        setBearerToken(groups);

        // when
        GetDocumentUrlResponse resp =
            new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
                .bypassWatermark(Boolean.TRUE).submitOk(this.documentsApi.getApiClient(), siteId)
                .response();

        // then
        assertNotNull(resp);
        assertNotNull(resp.getUrl());
        assertS3Url(resp, DEFAULT_SITE_ID.equals(siteId) ? null : siteId, documentId);
      }
    }
  }

  /**
   * /documents/{documentId}/url with watermarks and ByPassWatermark without permissions.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent09() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = addDocumentWithWatermarks(siteId);

      // when
      try {
        new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, null))
            .bypassWatermark(Boolean.TRUE).submitOk(this.documentsApi.getApiClient(), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"user requires 'admin' or 'govern' permission\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * /documents/{documentId}/url request deep link to another s3 bucket.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent10() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      String bucketName = "somebucket";
      createBucket(bucketName);
      String filename = UUID.randomUUID() + " .pdf";
      var document = new AddDocumentRequestBuilder().deepLink("s3://" + bucketName + "/" + filename)
          .getDocument(client, siteId);
      getS3().putObject(bucketName, filename, "ASD".getBytes(StandardCharsets.UTF_8), null);

      // when
      GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document)
          .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());

      URI uri = new URI(resp.getUrl());
      assertTrue(uri.getQuery().contains("filename*=UTF-8''" + filename));
      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=172800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));
      assertTrue(resp.getUrl().contains(bucketName));
      assertTrue(resp.getUrl().contains(filename.replace(" ", "%20")));
    }
  }

  /**
   * /documents/{documentId}/url request deep link https resource.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent11() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      var document = new AddDocumentRequestBuilder().deepLink("https://www.google.com")
          .getDocument(client, siteId);

      // when
      GetDocumentUrlResponse resp = new GetDocumentUrlRequestBuilder(document)
          .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());
      assertEquals("https://www.google.com", resp.getUrl());
    }
  }

  /**
   * /documents/{documentId}/url request for artifact.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContentArtifact01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();
      assertNotNull(documentId);

      String artifactId = new AddDocumentRequestBuilder().content().documentId(documentId)
          .artifacts(true).submit(client, siteId).throwIfError().response().getArtifactId();

      // when
      GetDocumentUrlResponse resp =
          new GetDocumentUrlRequestBuilder(DocumentArtifact.of(documentId, artifactId))
              .submitOk(this.documentsApi.getApiClient(), siteId).response();

      // then
      assertNotNull(resp);
      assertNotNull(resp.getUrl());
      assertTrue(resp.getUrl().contains("/artifacts/"));
      assertS3Url(resp, siteId, documentId, artifactId);
    }
  }

  /**
   * Get /documents/{documentId}/url rejects maxUses without short format.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testHandleGetDocumentMaxUsesWithoutShortFormat() throws ApiException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      var createdDocument =
          new AddDocumentRequestBuilder().content().submitOk(client, siteId).response();
      var document = DocumentArtifact.of(createdDocument.getDocumentId(), null);

      // when
      var response =
          new GetDocumentUrlRequestBuilder(document).maxUses(1).submitError(client, siteId);

      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
          response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"maxUses\",\"error\":\"maxUses requires format=short\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Get /documents/{documentId}/url request using short format.
   */
  @Test
  public void testHandleGetDocumentShortFormat01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      setBearerToken(siteId);

      var document = new AddDocumentRequestBuilder().content().getDocument(client, siteId);

      // when
      try {
        new GetDocumentUrlRequestBuilder(document).setFormat("short")
            .submitOk(this.documentsApi.getApiClient(), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"format\"," + "\"error\":\"format=short is not supported\"}]}",
            e.getResponseBody());
      }
    }
  }
}
