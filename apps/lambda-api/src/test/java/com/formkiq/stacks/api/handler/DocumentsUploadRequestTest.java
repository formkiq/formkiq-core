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

import com.formkiq.testutils.api.documents.GetDocumentActionsRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentTagsRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentIdUploadRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentUploadRequestBuilder;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.ChecksumType;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.DocumentResourceType;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.UpdateConfigurationRequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /documents/upload, /documents/{documentId}/upload . */
public class DocumentsUploadRequestTest extends AbstractApiClientRequestTest {

  /** Ten. */
  private static final int TEN = 10;

  private HttpResponse<String> putS3Request(final GetDocumentUrlResponse response,
      final String content) throws IOException {
    HttpService http = new HttpServiceJdk11();

    HttpHeaders hds = new HttpHeaders();
    notNull(response.getHeaders()).forEach((h, v) -> hds.add(h, v.toString()));

    return http.put(response.getUrl(), Optional.of(hds), Optional.empty(), content);
  }

  /**
   * Get Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGet01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config = new UpdateConfigurationRequest().maxDocuments("1");
      new UpdateConfigurationRequestBuilder().withUpdateConfigurationRequest(config)
          .submitOk(this.client, siteId);

      setBearerToken(siteId);
      new GetDocumentUploadRequestBuilder().contentLength(1).submitOk(this.client, siteId);

      // when
      try {
        new GetDocumentUploadRequestBuilder().contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"Max Number of Documents reached\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGet02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      // when
      GetDocumentUrlResponse response = new GetDocumentUploadRequestBuilder().contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      assertNotNull(response.getUrl());

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());
    }
  }

  /**
   * GET Request Upload Document with SHA256 missing checksum.
   *
   */
  @Test
  public void testGet03() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      try {
        new GetDocumentUploadRequestBuilder().checksumType("sha256").contentLength(1)
            .submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"checksum\",\"error\":\"'checksum' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET Request Upload Document with SHA256 and checksum != body text.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGet04() throws Exception {
    // given
    final String content = "dummy data123";
    final String reqChecksum = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      GetDocumentUrlResponse response = new GetDocumentUploadRequestBuilder().checksumType("sha256")
          .checksum(reqChecksum).contentLength(1).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(SC_BAD_REQUEST.getStatusCode(), put.statusCode());
    }
  }

  /**
   * POST Request Upload Document with SHA1.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGet05() throws Exception {
    // given
    final String content = "dummy data";
    final String reqChecksum = "611ff54ef4d8389cf982da9516804906d99389b6";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      GetDocumentUrlResponse response = new GetDocumentUploadRequestBuilder().checksumType("sha1")
          .checksum(reqChecksum).contentLength(1).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
    }
  }

  /**
   * GET Request Upload Document with SHA512.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGet06Sha512() throws Exception {
    // given
    final String reqChecksum = "ead40277a5f9d21db05ede5d78063478a1795dbd4b2db4af919b7e06a733cfc6"
        + "ddd7eac7f956194a05a18ce1b8cc9e352b490b983431b83c8e78730aa8a40634";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      GetDocumentUrlResponse response = new GetDocumentUploadRequestBuilder().checksumType("sha512")
          .checksum(reqChecksum).contentLength(1).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("6tQCd6X50h2wXt5deAY0eKF5Xb1LLbSvkZt+Bqczz8bd1+rH+VYZSgWhjOG4zJ41"
          + "K0kLmDQxuDyOeHMKqKQGNA==", response.getHeaders().get("x-amz-checksum-sha512"));
      assertEquals("SHA512", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);

      AwsServiceCache awsServices = getAwsServices();
      awsServices.register(DocumentService.class, new DocumentServiceExtension());
      awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
      DocumentRecord document = awsServices.getExtension(DocumentService.class).findDocument(siteId,
          DocumentArtifact.of(documentId, null));
      assertEquals(documentId, document.documentId());
      assertEquals("SHA512", document.checksumType());
    }
  }

  /**
   * Get Request Upload Document Url, invalid documentId.
   *
   */
  @Test
  public void testGetUpload01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");
      setBearerToken(siteId);

      String documentId = ID.uuid();

      // when
      try {
        new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, null))
            .contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Get Request Upload Document Url.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGetUpload02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("akldajds");
      String documentId = new AddDocumentRequestBuilder(req).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentUrlResponse response =
          new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, null))
              .contentLength(1).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());
    }
  }

  /**
   * GET Request Upload Document with SHA256 missing checksum.
   *
   */
  @Test
  public void testGetUpload03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("akldajds");
      String documentId = new AddDocumentRequestBuilder(req).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);

      // when
      try {
        new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, null))
            .checksumType("sha256").contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"checksum\",\"error\":\"'checksum' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * GET Request Upload Document with SHA256 and checksum != body text.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGetUpload04() throws Exception {
    // given
    final String content = "dummy data123";
    final String reqChecksum = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("akldajds");
      String documentId = new AddDocumentRequestBuilder(req).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentUrlResponse response =
          new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, null))
              .checksumType("sha256").checksum(reqChecksum).contentLength(1)
              .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(SC_BAD_REQUEST.getStatusCode(), put.statusCode());
    }
  }

  /**
   * POST Request Upload Document with SHA1.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGetUpload05() throws Exception {
    // given
    final String content = "dummy data";
    final String reqChecksum = "611ff54ef4d8389cf982da9516804906d99389b6";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().content("akldajds");
      String documentId = new AddDocumentRequestBuilder(req).submitOk(this.client, siteId)
          .response().getDocumentId();
      assertNotNull(documentId);

      // when
      GetDocumentUrlResponse response =
          new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, null))
              .checksumType("sha1").checksum(reqChecksum).contentLength(1)
              .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
    }
  }

  /**
   * Get Request Upload Document Url for Artifact.
   *
   * @throws Exception Exception
   */
  @Test
  public void testGetUploadArtifact01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .throwIfError().response().getDocumentId();

      String artifactId = new AddDocumentRequestBuilder().content().documentId(documentId)
          .artifacts(true).submit(client, siteId).throwIfError().response().getArtifactId();

      // then
      assertNotNull(documentId);
      assertNotNull(artifactId);

      // when
      GetDocumentUrlResponse response =
          new GetDocumentIdUploadRequestBuilder(DocumentArtifact.of(documentId, artifactId))
              .contentLength(1).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(documentId, response.getDocumentId());
      assertTrue(response.getUrl().contains("/artifacts/"));
    }
  }

  /**
   * POST Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config = new UpdateConfigurationRequest().maxDocuments("1");
      new UpdateConfigurationRequestBuilder().withUpdateConfigurationRequest(config)
          .submitOk(this.client, siteId);

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);

      // when
      try {
        new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"Max Number of Documents reached\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost02() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      assertNotNull(response.getUrl());

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());
      assertEquals(DocumentResourceType.DOCUMENT, document.getResourceType());
    }
  }

  /**
   * POST Request Upload Document Url with tags.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost03() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest()
          .addTagsItem(new AddDocumentTag().key("category").value("person"));
      new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      assertNotNull(response.getUrl());

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      List<DocumentTag> tags =
          notNull(new GetDocumentTagsRequestBuilder(documentId).setArtifactId(null).limit(null)
              .next(null).submitOk(this.client, siteId).response().getTags());
      assertEquals(1, tags.size());
      assertEquals("category", tags.getFirst().getKey());
      assertEquals("person", tags.getFirst().getValue());
      assertEquals("userdefined", tags.getFirst().getType());
    }
  }

  /**
   * POST Request Upload Document Url with documentId.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost04() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String documentId = ID.uuid();
      AddDocumentUploadRequest req = new AddDocumentUploadRequest().documentId(documentId)
          .addTagsItem(new AddDocumentTag().key("category").value("person"));

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());

      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      GetDocumentTagsResponse tags = new GetDocumentTagsRequestBuilder(documentId)
          .setArtifactId(null).limit(null).next(null).submitOk(this.client, siteId).response();
      assertEquals(1, notNull(tags.getTags()).size());
      assertEquals("category", tags.getTags().getFirst().getKey());
      assertEquals("person", tags.getTags().getFirst().getValue());
      assertEquals("userdefined", tags.getTags().getFirst().getType());
    }
  }

  /**
   * POST Request Upload Document with SHA256.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost05() throws Exception {
    // given
    final String content = "dummy data";
    final String reqChecksum = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().checksumType(ChecksumType.SHA256).checksum(reqChecksum);

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
    }
  }

  /**
   * POST Request Upload Document with SHA256 missing checksum.
   *
   */
  @Test
  public void testPost06() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().checksumType(ChecksumType.SHA256);

      // when
      try {
        new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"checksum\",\"error\":\"'checksum' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Upload Document with SHA256 and checksum != body text.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost07() throws Exception {
    // given
    final String content = "dummy data123";
    final String reqChecksum = "797bb0abff798d7200af7685dca7901edffc52bf26500d5bd97282658ee24152";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().checksumType(ChecksumType.SHA256).checksum(reqChecksum);

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(SC_BAD_REQUEST.getStatusCode(), put.statusCode());
    }
  }

  /**
   * POST Request Upload Document with SHA1.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost08() throws Exception {
    // given
    final String content = "dummy data";
    final String reqChecksum = "611ff54ef4d8389cf982da9516804906d99389b6";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().checksumType(ChecksumType.SHA1).checksum(reqChecksum);

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      GetDocumentResponse document = new GetDocumentRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null))
          .submitOk(this.client, siteId).response();
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
    }
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost09() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req =
          new AddDocumentUploadRequest().addTagsItem(new AddDocumentTag().key("test").value("this"))
              .addActionsItem(new AddAction().type(DocumentActionType.OCR));

      // when
      GetDocumentUrlResponse response = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      String url = response.getUrl();
      assertNotNull(url);
      assertTrue(
          siteId != null ? url.contains("/testbucket/" + siteId) : url.contains("/testbucket/"));

      String documentId = response.getDocumentId();
      assertNotNull(documentId);
      List<DocumentAction> actions = notNull(new GetDocumentActionsRequestBuilder(
          com.formkiq.aws.dynamodb.documents.DocumentArtifact.of(documentId, null)).limit(null)
          .next(null).submitOk(this.client, siteId).response().getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.getFirst().getType());
      assertEquals(DocumentActionStatus.PENDING, actions.getFirst().getStatus());

      List<DocumentTag> tags =
          notNull(new GetDocumentTagsRequestBuilder(documentId).setArtifactId(null).limit(null)
              .next(null).submitOk(this.client, siteId).response().getTags());
      assertEquals(1, tags.size());
      assertEquals("test", tags.getFirst().getKey());
      assertEquals("this", tags.getFirst().getValue());
    }
  }

  /**
   * Valid POST generate upload document signed url.
   *
   */
  @Test
  public void testPost10() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest()
          .addTagsItem(new AddDocumentTag().key("CLAMAV_SCAN_TIMESTAMP").value("this"));

      // when
      try {
        new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"CLAMAV_SCAN_TIMESTAMP\","
            + "\"error\":\"unallowed tag key\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Content Length greater than allowed.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost11() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config =
          new UpdateConfigurationRequest().maxContentLengthBytes("2");
      new UpdateConfigurationRequestBuilder().withUpdateConfigurationRequest(config)
          .submitOk(this.client, siteId);

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      new AddDocumentUploadRequestBuilder(req).contentLength(1).submitOk(this.client, siteId);

      // when
      try {
        new AddDocumentUploadRequestBuilder(req).contentLength(TEN).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"'contentLength' cannot exceed 2 bytes\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST Request Content Length not set.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost12() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken("Admins");

      UpdateConfigurationRequest config =
          new UpdateConfigurationRequest().maxContentLengthBytes("2");
      new UpdateConfigurationRequestBuilder().withUpdateConfigurationRequest(config)
          .submitOk(this.client, siteId);

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();

      // when
      try {
        new AddDocumentUploadRequestBuilder(req).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"'contentLength' is required when "
            + "MaxContentLengthBytes is configured\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/upload with artifacts=true no documentId.
   */
  @Test
  public void testPostArtifacts() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      AddDocumentUploadRequest req = new AddDocumentUploadRequest().artifacts(Boolean.TRUE);

      // when
      var resp = new AddDocumentUploadRequestBuilder(req).contentLength(1)
          .submitOk(this.client, siteId).response();

      // then
      String documentId = resp.getDocumentId();
      String artifactId = resp.getArtifactId();
      assertNotNull(documentId);
      assertNotNull(artifactId);

      new GetDocumentRequestBuilder(documentId).submit(client, siteId).throwIfError();
      new GetDocumentRequestBuilder(DocumentArtifact.of(documentId, artifactId))
          .submit(client, siteId).throwIfError();
    }
  }
}
