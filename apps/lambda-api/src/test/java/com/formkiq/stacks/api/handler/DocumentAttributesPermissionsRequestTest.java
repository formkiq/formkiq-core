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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.NumberingSequenceReset;
import com.formkiq.client.model.SetNumberingSequenceRequest;
import com.formkiq.testutils.api.SetBearerPermissionMap;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.documents.GenerateDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.systemmanagement.SetNumberingSequenceRequestBuilder;

/** Tests scoped, parent and invalid permissions for each Document Attributes handler method. */
class DocumentAttributesPermissionsRequestTest extends AbstractApiClientRequestTest {

  private void addAttribute(final String siteId, final DocumentArtifact document, final String key)
      throws ApiException {
    new AddDocumentAttributeRequestBuilder(document).addAttribute(key, "original")
        .submitOk(this.client, siteId);
  }

  private DocumentArtifact createDocument(final String siteId, final String key)
      throws ApiException {
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsString(key).submitOk(this.client, siteId);
    return new AddDocumentRequestBuilder().content("permission test").getDocument(this.client,
        siteId);
  }

  private void setPermission(final String siteId, final ApiPermission permission) {
    new SetBearerPermissionMap().apply(this.client, List.of(siteId),
        Map.of(siteId, List.of(permission.name())), Map.of());
  }

  /**
   * POST /documents/{documentId}/attributes/{attributeKey}/generate accepts its scoped and parent
   * grants and rejects document-only writes.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributeGenerateRequestHandlerPostPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      SetNumberingSequenceRequest sequence =
          new SetNumberingSequenceRequest().pattern("PERMISSION-{YEAR}-{SEQUENCE}").startAt(1L)
              .padding(2).reset(NumberingSequenceReset.YEARLY).timezone("UTC");
      new SetNumberingSequenceRequestBuilder().withAttributeKey(key)
          .withSetNumberingSequenceRequest(sequence).submitOk(this.client, siteId);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResult =
          new GenerateDocumentAttributeValueRequestBuilder().withDocumentId(document.documentId())
              .withAttributeKey(key).submitOk(this.client, siteId).response();

      // then
      assertEquals(1L, scopedResult.getSequence());

      // given
      setBearerToken(siteId);
      DocumentArtifact parentDocument =
          new AddDocumentRequestBuilder().content("parent grant").getDocument(this.client, siteId);
      setPermission(siteId, ApiPermission.WRITE);

      // when
      var parentResult = new GenerateDocumentAttributeValueRequestBuilder()
          .withDocumentId(parentDocument.documentId()).withAttributeKey(key)
          .submitOk(this.client, siteId).response();

      // then
      assertEquals(2L, parentResult.getSequence());

      // given
      setBearerToken(siteId);
      DocumentArtifact deniedDocument =
          new AddDocumentRequestBuilder().content("invalid grant").getDocument(this.client, siteId);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENTS);

      // when
      var deniedResponse = new GenerateDocumentAttributeValueRequestBuilder()
          .withDocumentId(deniedDocument.documentId()).withAttributeKey(key)
          .submitError(this.client, siteId);
      setBearerToken(siteId);
      List<DocumentAttribute> unchangedAttributes =
          new GetDocumentAttributesRequestBuilder(deniedDocument).getAttributes(this.client,
              siteId);

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals(0, unchangedAttributes.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} accepts its scoped and parent grants
   * and rejects document-only deletions.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributeRequestHandlerDeletePermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResponse =
          new DeleteDocumentAttributeRequestBuilder(document, key).submitOk(this.client, siteId);

      // then
      assertNotNull(scopedResponse.response());

      // given
      setBearerToken(siteId);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE);

      // when
      var parentResponse =
          new DeleteDocumentAttributeRequestBuilder(document, key).submitOk(this.client, siteId);

      // then
      assertNotNull(parentResponse.response());

      // given
      setBearerToken(siteId);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE_DOCUMENTS);

      // when
      var deniedResponse =
          new DeleteDocumentAttributeRequestBuilder(document, key).submitError(this.client, siteId);
      setBearerToken(siteId);
      DocumentAttribute unchangedAttribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals("original", unchangedAttribute.getStringValue());
    }
  }

  /**
   * GET /documents/{documentId}/attributes/{attributeKey} accepts its scoped and parent grants and
   * rejects document-only access.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributeRequestHandlerGetPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.READ_DOCUMENT_ATTRIBUTES);

      // when
      DocumentAttribute scopedResult = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals("original", scopedResult.getStringValue());

      // given
      setPermission(siteId, ApiPermission.READ);

      // when
      DocumentAttribute parentResult = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals("original", parentResult.getStringValue());

      // given
      setPermission(siteId, ApiPermission.READ_DOCUMENTS);

      // when
      var deniedResponse =
          new GetDocumentAttributeRequestBuilder(document, key).submitError(this.client, siteId);

      // then
      assertEquals(401, deniedResponse.exception().getCode());
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} accepts its scoped and parent grants and
   * rejects document-only writes.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributeRequestHandlerPutPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResponse = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
          .stringValue("updated").submitOk(this.client, siteId);

      // then
      assertNotNull(scopedResponse.response());

      // given
      setPermission(siteId, ApiPermission.WRITE);

      // when
      var parentResponse = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
          .stringValue("updated").submitOk(this.client, siteId);

      // then
      assertNotNull(parentResponse.response());

      // given
      setPermission(siteId, ApiPermission.WRITE_DOCUMENTS);

      // when
      var deniedResponse = new SetDocumentAttributeValueRequestBuilder(document).setKey(key)
          .stringValue("forbidden").submitError(this.client, siteId);
      setBearerToken(siteId);
      DocumentAttribute unchangedAttribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals("updated", unchangedAttribute.getStringValue());
    }
  }

  /**
   * GET /documents/{documentId}/attributes accepts its scoped and parent grants and rejects
   * document-only access.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributesRequestHandlerGetPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.READ_DOCUMENT_ATTRIBUTES);

      // when
      List<DocumentAttribute> scopedResult = new GetDocumentAttributesRequestBuilder(document)
          .submitOk(this.client, siteId).response().getAttributes();

      // then
      assertEquals(1, scopedResult.size());
      assertEquals("original", scopedResult.getFirst().getStringValue());

      // given
      setPermission(siteId, ApiPermission.READ);

      // when
      List<DocumentAttribute> parentResult = new GetDocumentAttributesRequestBuilder(document)
          .submitOk(this.client, siteId).response().getAttributes();

      // then
      assertEquals(1, parentResult.size());
      assertEquals("original", parentResult.getFirst().getStringValue());

      // given
      setPermission(siteId, ApiPermission.READ_DOCUMENTS);

      // when
      var deniedResponse =
          new GetDocumentAttributesRequestBuilder(document).submitError(this.client, siteId);

      // then
      assertEquals(401, deniedResponse.exception().getCode());
    }
  }

  /**
   * POST /documents/{documentId}/attributes accepts its scoped and parent grants and rejects
   * document-only writes.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributesRequestHandlerPostPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResponse = new AddDocumentAttributeRequestBuilder(document)
          .addAttribute(key, "updated").submitOk(this.client, siteId);

      // then
      assertNotNull(scopedResponse.response());

      // given
      setBearerToken(siteId);
      DocumentArtifact parentDocument =
          new AddDocumentRequestBuilder().content("parent grant").getDocument(this.client, siteId);
      setPermission(siteId, ApiPermission.WRITE);

      // when
      var parentResponse = new AddDocumentAttributeRequestBuilder(parentDocument)
          .addAttribute(key, "updated").submitOk(this.client, siteId);

      // then
      assertNotNull(parentResponse.response());

      // given
      setBearerToken(siteId);
      DocumentArtifact deniedDocument =
          new AddDocumentRequestBuilder().content("invalid grant").getDocument(this.client, siteId);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENTS);

      // when
      var deniedResponse = new AddDocumentAttributeRequestBuilder(deniedDocument)
          .addAttribute(key, "forbidden").submitError(this.client, siteId);
      setBearerToken(siteId);
      List<DocumentAttribute> unchangedAttributes =
          new GetDocumentAttributesRequestBuilder(deniedDocument).submitOk(this.client, siteId)
              .response().getAttributes();

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals(0, unchangedAttributes.size());
    }
  }

  /**
   * PUT /documents/{documentId}/attributes accepts its scoped and parent grants and rejects
   * document-only writes.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributesRequestHandlerPutPermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResponse = new SetDocumentAttributeRequestBuilder(document)
          .addAttribute(key, "updated").submitOk(this.client, siteId);

      // then
      assertNotNull(scopedResponse.response());

      // given
      setPermission(siteId, ApiPermission.WRITE);

      // when
      var parentResponse = new SetDocumentAttributeRequestBuilder(document)
          .addAttribute(key, "updated").submitOk(this.client, siteId);

      // then
      assertNotNull(parentResponse.response());

      // given
      setPermission(siteId, ApiPermission.WRITE_DOCUMENTS);

      // when
      var deniedResponse = new SetDocumentAttributeRequestBuilder(document)
          .addAttribute(key, "forbidden").submitError(this.client, siteId);
      setBearerToken(siteId);
      DocumentAttribute unchangedAttribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals("updated", unchangedAttribute.getStringValue());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} accepts its scoped
   * and parent grants and rejects document-only deletions.
   *
   * @throws ApiException API error
   */
  @Test
  void testDocumentAttributesValueRequestHandlerDeletePermissions() throws ApiException {
    for (String siteId : List.of(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String key = "permissionValue" + ID.uuid().replace("-", "");
      DocumentArtifact document = createDocument(siteId, key);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE_DOCUMENT_ATTRIBUTES);

      // when
      var scopedResponse = new DeleteDocumentAttributeValueRequestBuilder(document, key, "original")
          .submitOk(this.client, siteId);

      // then
      assertNotNull(scopedResponse.response());

      // given
      setBearerToken(siteId);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE);

      // when
      var parentResponse = new DeleteDocumentAttributeValueRequestBuilder(document, key, "original")
          .submitOk(this.client, siteId);

      // then
      assertNotNull(parentResponse.response());

      // given
      setBearerToken(siteId);
      addAttribute(siteId, document, key);
      setPermission(siteId, ApiPermission.DELETE_DOCUMENTS);

      // when
      var deniedResponse = new DeleteDocumentAttributeValueRequestBuilder(document, key, "original")
          .submitError(this.client, siteId);
      setBearerToken(siteId);
      DocumentAttribute unchangedAttribute = new GetDocumentAttributeRequestBuilder(document, key)
          .submitOk(this.client, siteId).response().getAttribute();

      // then
      assertEquals(401, deniedResponse.exception().getCode());
      assertEquals("original", unchangedAttribute.getStringValue());
    }
  }
}
