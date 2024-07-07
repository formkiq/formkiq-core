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

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.invoker.ApiResponse;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddChildDocument;
import com.formkiq.client.model.AddChildDocumentResponse;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.ChildDocument;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentMetadata;
import com.formkiq.client.model.DocumentTag;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

      GetDocumentResponse noSite =
          this.documentsApi.getDocument(responseNoSiteId.getDocumentId(), siteId, null);
      assertEquals("application/pdf", noSite.getContentType());
      assertNotNull(noSite.getPath());
      assertNotNull(noSite.getDocumentId());

      GetDocumentResponse site =
          this.documentsApi.getDocument(responseSiteId.getDocumentId(), siteId, null);
      assertEquals("application/pdf", site.getContentType());
      assertNotNull(site.getPath());
      assertNotNull(site.getDocumentId());
    }
  }

  /**
   * Save new File missing content.
   *
   */
  @Test
  public void testPost02() {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest();

      // when
      try {
        this.documentsApi.addDocument(req, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"either 'content', 'documents',"
            + " or 'deepLinkPath' are required\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Save new deep link.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testPost03() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().deepLinkPath("https://google.com");

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, null, null);

      // then
      String documentId = response.getDocumentId();

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("https://google.com", document.getDeepLinkPath());
    }
  }

  /**
   * Save document with subdocuments.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testPost04() throws Exception {

    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String content = "{\"firstName\": \"Jan\",\"lastName\": \"Doe\"}";

      AddDocumentRequest req = new AddDocumentRequest()
          .addTagsItem(new AddDocumentTag().key("formName").value("Job Application Form"))
          .addDocumentsItem(new AddChildDocument().content(content).contentType("application/json")
              .addTagsItem(new AddDocumentTag().key("formData")));

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      assertNotNull(response.getUploadUrl());

      List<AddChildDocumentResponse> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      assertNull(documents.get(0).getUploadUrl());

      String documentId = response.getDocumentId();
      String childDocumentId = documents.get(0).getDocumentId();

      List<ChildDocument> documents1 =
          notNull(this.documentsApi.getDocument(documentId, siteId, null).getDocuments());
      assertEquals(1, documents1.size());
      assertEquals(childDocumentId, documents1.get(0).getDocumentId());
      assertEquals("application/json", documents1.get(0).getContentType());
      assertEquals(documentId, documents1.get(0).getBelongsToDocumentId());

      List<DocumentTag> tags = notNull(
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null).getTags());

      assertEquals(1, tags.size());
      assertEquals("formName", tags.get(0).getKey());
      assertEquals("Job Application Form", tags.get(0).getValue());

      tags = notNull(
          this.tagsApi.getDocumentTags(childDocumentId, siteId, null, null, null, null).getTags());
      assertEquals(0, tags.size());

      GetDocumentResponse childDocument =
          this.documentsApi.getDocument(childDocumentId, null, null);
      assertTrue(Objects.notNull(childDocument.getDocuments()).isEmpty());
      assertEquals(documentId, childDocument.getBelongsToDocumentId());
    }
  }

  /**
   * Save Document with Content / Metadata.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost05() throws Exception {
    // given
    String content = "test data";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content)
          .addMetadataItem(new AddDocumentMetadata().key("person").value("category"))
          .addMetadataItem(
              new AddDocumentMetadata().key("playerId").values(Arrays.asList("11", "22")));

      // when
      AddDocumentResponse response = this.documentsApi.addDocument(req, siteId, null);

      // then
      String documentId = response.getDocumentId();

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertNotNull(document);
      List<DocumentMetadata> metadata = notNull(document.getMetadata());
      assertEquals(2, metadata.size());
      assertEquals("playerId", metadata.get(0).getKey());
      assertEquals("11,22", String.join(",", notNull(metadata.get(0).getValues())));
      assertEquals("person", metadata.get(1).getKey());
      assertEquals("category", metadata.get(1).getValue());

      assertEquals(content,
          this.documentsApi.getDocumentContent(documentId, siteId, null, null).getContent());
    }
  }

  /**
   * Add Document with attributes, check composite keys.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost06() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String attributeKey0 = "category";
      String attributeKey1 = "documentType";

      for (String attributeKey : Arrays.asList(attributeKey0, attributeKey1)) {
        this.attributesApi.addAttribute(
            new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);
      }

      SetSitesSchemaRequest sitesSchema = new SetSitesSchemaRequest().name("test")
          .attributes(new SchemaAttributes().addCompositeKeysItem(new AttributeSchemaCompositeKey()
              .attributeKeys(Arrays.asList(attributeKey0, attributeKey1))));
      this.schemasApi.setSitesSchema(siteId, sitesSchema);

      AddDocumentRequest req = new AddDocumentRequest().content(content0)
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey0).stringValue("person")))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey1).stringValue("privacy")));

      // when
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 3;
      assertEquals(expected, attributes.size());
      assertEquals(attributeKey0 + "#" + attributeKey1, attributes.get(0).getKey());
      assertEquals("person#privacy", attributes.get(0).getStringValue());
      assertEquals(attributeKey0, attributes.get(1).getKey());
      assertEquals("person", attributes.get(1).getStringValue());
      assertEquals(attributeKey1, attributes.get(2).getKey());
      assertEquals("privacy", attributes.get(2).getStringValue());
    }
  }

  /**
   * Add Document with invalid schema allowed value.
   */
  @Test
  public void testPost07() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String attributeKey = "test";

      this.attributesApi.addAttribute(
          new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

      SetSitesSchemaRequest sitesSchema = new SetSitesSchemaRequest().name("test")
          .attributes(new SchemaAttributes().addOptionalItem(new AttributeSchemaOptional()
              .attributeKey(attributeKey).addAllowedValuesItem("abc")));
      this.schemasApi.setSitesSchema(siteId, sitesSchema);

      AddDocumentRequest req =
          new AddDocumentRequest().content(content0).addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(attributeKey).stringValue("123")));

      // when
      try {
        this.documentsApi.addDocument(req, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"test\","
                + "\"error\":\"invalid attribute value 'test', only allowed values are abc\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Save invalid deep link.
   *
   */
  @Test
  public void testPost08() {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String deepLinkPath : Arrays.asList("askdjaskd", "s3:/ajksdjasdsjasad", "s3/asd")) {

        AddDocumentRequest req = new AddDocumentRequest().deepLinkPath(deepLinkPath);

        // when
        try {
          this.documentsApi.addDocument(req, null, null);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
          assertEquals("{\"errors\":[{\"key\":\"deepLinkPath\"," + "\"error\":\"DeepLinkPath '"
              + deepLinkPath + "' is not a valid URL\"}]}", e.getResponseBody());
        }
      }
    }
  }

  /**
   * Test Publish no published document.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testPublish01() throws ApiException {
    // given
    String content0 = "test data 1";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String path = UUID.randomUUID() + ".txt";

      AddDocumentRequest req =
          new AddDocumentRequest().path(path).content(content0).contentType("text/plain");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      // when
      try {
        documentsApi.getPublishedDocumentContentWithHttpInfo(documentId, siteId);
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
   * Test Publish document.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testPublish02() throws ApiException {
    // given
    String content0 = "test data 1";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      setBearerToken(siteId);
      String path = UUID.randomUUID() + ".txt";

      AddDocumentRequest req =
          new AddDocumentRequest().path(path).content(content0).contentType("text/plain");
      String documentId = this.documentsApi.addDocument(req, siteId, null).getDocumentId();

      AwsServiceCache awsServices = getAwsServices();
      awsServices.register(S3Service.class, new S3ServiceExtension());
      awsServices.register(DocumentService.class, new DocumentServiceExtension());
      awsServices.register(DocumentVersionService.class, new DocumentVersionServiceExtension());

      S3Service s3 = awsServices.getExtension(S3Service.class);
      S3ObjectMetadata objectMetadata = s3.getObjectMetadata(BUCKET_NAME,
          SiteIdKeyGenerator.createS3Key(siteId, documentId), null);
      String s3version = objectMetadata.getVersionId();

      DocumentService service = awsServices.getExtension(DocumentService.class);
      service.publishDocument(siteId, documentId, s3version, path, "text/plain", "joe");

      // when
      ApiResponse<Void> response =
          documentsApi.getPublishedDocumentContentWithHttpInfo(documentId, siteId);

      // then
      assertEquals(ApiResponseStatus.SC_OK.getStatusCode(), response.getStatusCode());

      assertEquals("attachment; filename=\"" + path + "\"",
          String.join(",", response.getHeaders().get("content-disposition")));
      assertEquals("text/plain", String.join(",", response.getHeaders().get("content-type")));

      // when
      documentsApi.deletePublishedDocumentContent(documentId, siteId);

      // then
      try {
        documentsApi.getPublishedDocumentContentWithHttpInfo(documentId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Document " + documentId + " not found.\"}",
            e.getResponseBody());
      }
    }
  }
}
