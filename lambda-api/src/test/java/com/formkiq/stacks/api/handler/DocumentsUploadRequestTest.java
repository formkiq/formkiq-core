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

import com.formkiq.aws.dynamodb.ID;
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
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.UpdateConfigurationRequest;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
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
      this.systemApi.updateConfiguration(siteId, config);

      setBearerToken(siteId);
      this.documentsApi.getDocumentUpload(null, siteId, null, null, 1, null, null);

      // when
      try {
        this.documentsApi.getDocumentUpload(null, siteId, null, null, 1, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
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
      GetDocumentUrlResponse response =
          this.documentsApi.getDocumentUpload(null, siteId, null, null, 1, null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
        this.documentsApi.getDocumentUpload(null, siteId, "sha256", null, 1, null, null);
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
      GetDocumentUrlResponse response =
          this.documentsApi.getDocumentUpload(null, siteId, "sha256", reqChecksum, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
      GetDocumentUrlResponse response =
          this.documentsApi.getDocumentUpload(null, siteId, "sha1", reqChecksum, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
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
        this.documentsApi.getDocumentIdUpload(documentId, siteId, null, null, 1, null, null);
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
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.getDocumentIdUpload(documentId, siteId, null, null, 1, null, null);

      // then
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      try {
        this.documentsApi.getDocumentIdUpload(documentId, siteId, "sha256", null, 1, null, null);
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
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      GetDocumentUrlResponse response = this.documentsApi.getDocumentIdUpload(documentId, siteId,
          "sha256", reqChecksum, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      GetDocumentUrlResponse response = this.documentsApi.getDocumentIdUpload(documentId, siteId,
          "sha1", reqChecksum, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());

      HttpResponse<String> put = putS3Request(response, content);
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), put.statusCode());
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
      this.systemApi.updateConfiguration(siteId, config);

      setBearerToken(siteId);
      AddDocumentUploadRequest req = new AddDocumentUploadRequest();
      this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // when
      try {
        this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
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
      this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());
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
      this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      String documentId = response.getDocumentId();
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());

      List<DocumentTag> tags = notNull(
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags());
      assertEquals(1, tags.size());
      assertEquals("category", tags.get(0).getKey());
      assertEquals("person", tags.get(0).getValue());
      assertEquals("userdefined", tags.get(0).getType());
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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      assertNotNull(response.getUrl());

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(documentId, document.getDocumentId());

      GetDocumentTagsResponse tags =
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null);
      assertEquals(1, notNull(tags.getTags()).size());
      assertEquals("category", tags.getTags().get(0).getKey());
      assertEquals("person", tags.getTags().get(0).getValue());
      assertEquals("userdefined", tags.getTags().get(0).getType());
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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
        this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);
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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("eXuwq/95jXIAr3aF3KeQHt/8Ur8mUA1b2XKCZY7iQVI=",
          response.getHeaders().get("x-amz-checksum-sha256"));
      assertEquals("SHA256", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      assertNotNull(response.getUrl());
      assertEquals(2, notNull(response.getHeaders()).size());
      assertEquals("YR/1TvTYOJz5gtqVFoBJBtmTibY=",
          response.getHeaders().get("x-amz-checksum-sha1"));
      assertEquals("SHA1", response.getHeaders().get("x-amz-sdk-checksum-algorithm"));

      String documentId = response.getDocumentId();
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);

      // then
      String url = response.getUrl();
      assertNotNull(url);
      assertTrue(
          siteId != null ? url.contains("/testbucket/" + siteId) : url.contains("/testbucket/"));

      String documentId = response.getDocumentId();
      List<DocumentAction> actions = notNull(this.documentActionsApi
          .getDocumentActions(documentId, siteId, null, null, null).getActions());
      assertEquals(1, actions.size());
      assertEquals(DocumentActionType.OCR, actions.get(0).getType());
      assertEquals(DocumentActionStatus.PENDING, actions.get(0).getStatus());

      List<DocumentTag> tags = notNull(
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags());
      assertEquals(1, tags.size());
      assertEquals("test", tags.get(0).getKey());
      assertEquals("this", tags.get(0).getValue());
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
        this.documentsApi.addDocumentUpload(req, siteId, 1, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"CLAMAV_SCAN_TIMESTAMP\","
            + "\"error\":\"unallowed tag key\"}]}", e.getResponseBody());
      }
    }
  }

  private HttpResponse<String> putS3Request(final GetDocumentUrlResponse response,
      final String content) throws IOException {
    HttpService http = new HttpServiceJdk11();

    HttpHeaders hds = new HttpHeaders();
    notNull(response.getHeaders()).forEach((h, v) -> hds.add(h, v.toString()));

    return http.put(response.getUrl(), Optional.of(hds), Optional.empty(), content);
  }
}
