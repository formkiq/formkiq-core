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
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.Document;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentRequestBuilder;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.testutils.api.documents.GetDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentsRequestBuilder;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /documents/{documentId}/purge. */
public class DocumentsIdPurgeRequestTest extends AbstractApiClientRequestTest {

  private GetDocumentUrlResponse addDocument(final String siteId) throws ApiException {
    return new GetDocumentUploadRequestBuilder().path("test.txt").submit(client, siteId)
        .throwIfError().response();
  }

  private boolean exists(final String siteId, final String documentId, final String artifactId) {
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId, artifactId);
    S3Service s3Service = getAwsServices().getExtension(S3Service.class);
    return s3Service.exists(BUCKET_NAME, s3Key);
  }

  private List<Document> getDocuments(final String siteId) throws ApiException {
    return notNull(new GetDocumentsRequestBuilder().submit(client, siteId).throwIfError().response()
        .getDocuments());
    // return notNull(this.documentsApi
    // .getDocuments(siteId, null, null, null, null, null, null, null, null, null).getDocuments());
  }

  private List<S3Object> getS3Files(final String siteId, final String documentId) {
    String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId, null);
    S3Service s3Service = getAwsServices().getExtension(S3Service.class);
    return notNull(s3Service.listObjects(BUCKET_NAME, s3Key).contents());
  }

  private HttpResponse<String> putS3Request(final GetDocumentUrlResponse response)
      throws IOException {
    HttpService http = new HttpServiceJdk11();

    HttpHeaders hds = new HttpHeaders();
    notNull(response.getHeaders()).forEach((h, v) -> hds.add(h, v.toString()));
    return http.put(response.getUrl(), Optional.of(hds), Optional.empty(), "test content");
  }

  /**
   * DELETE /documents/{documentId} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete01() throws Exception {

    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (String token : Arrays.asList("Admins", siteId + "_govern")) {
        setBearerToken(token);

        GetDocumentUrlResponse response = addDocument(siteId);
        String documentId = response.getDocumentId();
        assertNotNull(documentId);

        HttpResponse<String> put = putS3Request(response);
        assertEquals(SC_OK.getStatusCode(), put.statusCode());

        List<S3Object> s3Files = getS3Files(siteId, documentId);
        assertEquals(1, s3Files.size());

        // when
        DeleteResponse deleteResponse = this.documentsApi.purgeDocument(documentId, siteId, null);

        // then
        assertEquals("Deleted document '" + documentId + "' permanently",
            deleteResponse.getMessage());
        List<Document> documents = getDocuments(siteId);
        assertEquals(0, documents.size());

        s3Files = getS3Files(siteId, documentId);
        assertEquals(0, s3Files.size());
      }
    }
  }

  /**
   * DELETE /documents/{documentId} request NOT admin.
   *
   */
  @Test
  public void testDocumentDelete02() {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String documentId = ID.uuid();

      // when
      try {
        this.documentsApi.purgeDocument(documentId, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals(
            "{\"message\":\"fkq access denied " + "(groups: " + siteId + " (DELETE,READ,WRITE))\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId} request when Metadata, S3 is missing.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete03() throws Exception {

    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (String token : Arrays.asList("Admins", siteId + "_govern")) {
        setBearerToken(token);

        String documentId = addDocument(siteId).getDocumentId();
        assertNotNull(documentId);

        List<S3Object> s3Files = getS3Files(siteId, documentId);
        assertEquals(0, s3Files.size());

        // when
        DeleteResponse deleteResponse = this.documentsApi.purgeDocument(documentId, siteId, null);

        // then
        assertEquals("Deleted document '" + documentId + "' permanently",
            deleteResponse.getMessage());
        List<Document> documents = getDocuments(siteId);
        assertEquals(0, documents.size());

        s3Files = getS3Files(siteId, documentId);
        assertEquals(0, s3Files.size());
      }
    }
  }

  /**
   * DELETE /documents/{documentId} request with artifactId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDocumentDelete04() throws Exception {

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      // given
      setBearerToken(siteId);
      String path0 = ID.ulid() + ".txt";
      String path1 = ID.ulid() + ".txt";

      // when
      var document = new AddDocumentRequestBuilder().content().path(path0).submit(client, siteId)
          .throwIfError().response();
      var documentId = document.getDocumentId();

      var artifact = new AddDocumentRequestBuilder().documentId(documentId).content().path(path1)
          .artifacts(true).submit(client, siteId).throwIfError().response();

      // then
      String artifactId = artifact.getArtifactId();

      assertNotNull(documentId);
      assertNotNull(artifactId);
      assertTrue(exists(siteId, documentId, null));
      assertTrue(exists(siteId, documentId, artifactId));

      // given
      setBearerToken(siteId + "_govern");

      // when
      DeleteResponse deleteResponse =
          this.documentsApi.purgeDocument(documentId, siteId, artifactId);

      // then
      assertEquals(
          "Deleted artifact '" + artifactId + "' from document '" + documentId + "' permanently",
          deleteResponse.getMessage());
      assertNotNull(new GetDocumentRequestBuilder(documentId).submit(client, siteId).throwIfError()
          .response());
      assertNotNull(new GetDocumentRequestBuilder(DocumentArtifact.of(documentId, artifactId))
          .submit(client, siteId).exception());
      assertTrue(exists(siteId, documentId, null));
      assertFalse(exists(siteId, documentId, artifactId));
    }
  }
}
