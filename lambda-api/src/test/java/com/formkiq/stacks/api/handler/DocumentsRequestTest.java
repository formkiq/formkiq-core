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
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddChildDocument;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
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

      assertEquals(1, response.getDocuments().size());
      assertNull(response.getDocuments().get(0).getUploadUrl());

      String documentId = response.getDocumentId();
      String childDocumentId = response.getDocuments().get(0).getDocumentId();

      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals(1, document.getDocuments().size());
      assertEquals(childDocumentId, document.getDocuments().get(0).getDocumentId());
      assertEquals("application/json", document.getDocuments().get(0).getContentType());
      assertEquals(documentId, document.getDocuments().get(0).getBelongsToDocumentId());

      GetDocumentTagsResponse tags =
          this.tagsApi.getDocumentTags(documentId, siteId, null, null, null, null);

      assertEquals(1, tags.getTags().size());
      assertEquals("formName", tags.getTags().get(0).getKey());
      assertEquals("Job Application Form", tags.getTags().get(0).getValue());

      tags = this.tagsApi.getDocumentTags(childDocumentId, siteId, null, null, null, null);
      assertEquals(0, tags.getTags().size());

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
      assertEquals(2, document.getMetadata().size());
      assertEquals("playerId", document.getMetadata().get(0).getKey());
      assertEquals("11,22", String.join(",", document.getMetadata().get(0).getValues()));
      assertEquals("person", document.getMetadata().get(1).getKey());
      assertEquals("category", document.getMetadata().get(1).getValue());

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
          .addAttributesItem(new AddDocumentAttribute().key(attributeKey0).stringValue("person"))
          .addAttributesItem(new AddDocumentAttribute().key(attributeKey1).stringValue("privacy"));

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

      AddDocumentRequest req = new AddDocumentRequest().content(content0)
          .addAttributesItem(new AddDocumentAttribute().key(attributeKey).stringValue("123"));

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
}
