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

import com.formkiq.testutils.api.folders.GetFoldersRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeValueRequestBuilder;
import com.formkiq.testutils.api.attributes.DeleteAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.UpdateDocumentRequestBuilder;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.UpdateAttribute;
import com.formkiq.client.model.UpdateAttributeRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.attributes.UpdateAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.SetDocumentAttributeValueRequestBuilder;
import java.util.Arrays;
import java.util.List;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringsAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for protected attribute permissions. */
public class AttributePermissionsRequestTest extends AbstractAttributesRequestTest {

  /**
   * POST /attributes GOVERNANCE without ADMIN permission.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testAddAttributes09(final String siteId) {
    // given

    setBearerToken(siteId);

    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key("gov").type(AttributeType.GOVERNANCE));

    // when

    // when
    var errorResponse1 = new AddAttributeRequestBuilder().request(req).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"gov\",\"error\":\"Access denied to attribute\"}]}");
  }

  /**
   * POST /attributes GOVERNANCE with GOVERN permission.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testAddAttributes10(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId + "_govern");

    String key = "gov_" + ID.uuid();
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key).type(AttributeType.GOVERNANCE));

    // when
    AddResponse response =
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

    // then
    assertEquals("Attribute '" + key + "' created", response.getMessage());
  }

  /**
   * DELETE /attributes OPA / GOVERNANCE attribute.
   *
   * @param siteId site identifier; null selects the default site
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testDeleteAttributes06(final String siteId) throws ApiException {
    // given

    for (AttributeType type : List.of(AttributeType.GOVERNANCE, AttributeType.OPA)) {

      setBearerToken(new String[] {siteId, "admins"});

      AddAttributeRequest req =
          new AddAttributeRequest().attribute(new AddAttribute().key("gov").type(type));
      new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

      setBearerToken(siteId);

      // when

      // when
      var errorResponse1 = new DeleteAttributeRequestBuilder("gov").submit(this.client, siteId);

      // then
      assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
          "{\"errors\":[{\"key\":\"gov\",\"error\":\"Access denied to attribute\"}]}");

      // given
      setBearerToken(new String[] {siteId, "admins"});

      // when
      DeleteResponse deleteResponse =
          new DeleteAttributeRequestBuilder("gov").submitOk(this.client, siteId).response();

      // then
      assertEquals("Attribute 'gov' deleted", deleteResponse.getMessage());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with OPA.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testDeleteDocumentAttribute02(final String siteId) throws ApiException {
    // given

    setBearerToken("Admins");
    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
    setBearerToken(siteId);

    // when

    // when
    var errorResponse1 =
        new DeleteDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute 'security' is an protected attribute, "
            + "can only be changed by Goverance/Admin role\"}]}");

    // given
    setBearerToken("Admins");

    // when
    new DeleteDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
        .submitOk(this.client, siteId);

    // then

    // when
    var errorResponse2 =
        new GetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}");
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey} with OPA and GOVERN.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testDeleteDocumentAttribute03(final String siteId) throws ApiException {
    // given

    setBearerToken(new String[] {siteId, siteId + "_govern"});

    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    // when
    new DeleteDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
        .submitOk(this.client, siteId);

    // then
    List<DocumentAttribute> documentAttributes =
        notNull(new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .limit(null).next(null).submitOk(this.client, siteId).response().getAttributes());
    assertEquals(0, documentAttributes.size());
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} with OPA.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testDeleteDocumentAttributeValue01(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId + "_govern");
    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
    setBearerToken(siteId);

    // when

    // when
    var errorResponse1 =
        new DeleteDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null),
            "security", "confidential").submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute 'security' is an protected attribute, "
            + "can only be changed by Goverance/Admin role\"}]}");

    // given
    setBearerToken("Admins");

    // when
    new DeleteDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null),
        "security", "confidential").submitOk(this.client, siteId);

    // then

    // when
    var errorResponse2 =
        new GetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}");
  }

  /**
   * PUT /documents/{documentId}/attributes with OPA attribute attached to document.
   * 
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testPutDocumentAttribute02(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId + "_govern");
    addAttribute(siteId, "security", null, AttributeType.OPA);

    addAttribute(siteId, "strings", null, null);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
    setBearerToken(siteId);

    AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
    addDocumentAttribute(siteId, documentId, strings);

    SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
        .addAttributesItem(createStringAttribute("strings", "123"));

    // when

    // when
    var errorResponse1 =
        new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(sreq)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}");

    // given
    setBearerToken("Admins");

    // when
    new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(sreq)
        .submitOk(this.client, siteId);

    // then
    assertEquals("123", getDocumentAttribute(siteId, documentId, "strings").getStringValue());

    // when
    var errorResponse2 =
        new GetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null), "security")
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_NOT_FOUND.getStatusCode(),
        "{\"message\":\"attribute 'security' not found on document '" + documentId + "'\"}");
  }

  /**
   * PUT /documents/{documentId}/attributes with OPA attribute attached to document and govern.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testPutDocumentAttribute03(final String siteId) throws ApiException {
    // given

    setBearerToken(new String[] {siteId, siteId + "_govern"});

    addAttribute(siteId, "security", null, AttributeType.OPA);
    addAttribute(siteId, "strings", null, null);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    AddDocumentAttribute strings = createStringsAttribute("strings", Arrays.asList("abc", "xyz"));
    addDocumentAttribute(siteId, documentId, strings);

    SetDocumentAttributesRequest sreq = new SetDocumentAttributesRequest()
        .addAttributesItem(createStringAttribute("strings", "123"));

    // when
    new SetDocumentAttributeRequestBuilder(DocumentArtifact.of(documentId, null)).request(sreq)
        .submitOk(this.client, siteId);

    // then
    List<DocumentAttribute> documentAttributes =
        notNull(new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .limit(null).next(null).submitOk(this.client, siteId).response().getAttributes());
    assertEquals(1, documentAttributes.size());
    assertEquals("strings", documentAttributes.getFirst().getKey());
    assertEquals("123", documentAttributes.getFirst().getStringValue());
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with OPA.
   * 
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testPutDocumentAttributeValue01(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId + "_govern");
    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    setBearerToken(siteId);
    SetDocumentAttributeRequest sreq = new SetDocumentAttributeRequest()
        .attribute(new AddDocumentAttributeValue().stringValue("123"));

    // when

    // when
    var errorResponse1 =
        new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
            .setKey("security").request(sreq).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}");

    // given
    setBearerToken("Admins");

    // when
    new SetDocumentAttributeValueRequestBuilder(DocumentArtifact.of(documentId, null))
        .setKey("security").request(sreq).submitOk(this.client, siteId);

    // then
    assertEquals("123", getDocumentAttribute(siteId, documentId, "security").getStringValue());
  }

  /**
   * PATCH /attributes/{key} update to governance and back.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUpdateAttributes01(final String siteId) throws ApiException {
    // given

    setBearerToken("Admins");
    String key = "abc_" + ID.uuid();
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));

    // when
    AddResponse response =
        new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId).response();

    // then
    assertEquals("Attribute '" + key + "' created", response.getMessage());

    // given
    UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
        .attribute(new UpdateAttribute().type(AttributeType.GOVERNANCE));

    // when
    UpdateResponse updateResponse = new UpdateAttributeRequestBuilder(key).request(updateReq)
        .submitOk(this.client, siteId).response();

    // then
    assertEquals("Attribute '" + key + "' updated", updateResponse.getMessage());
    assertAttributeEquals(siteId, key, AttributeType.GOVERNANCE, null);

    // given
    updateReq =
        new UpdateAttributeRequest().attribute(new UpdateAttribute().type(AttributeType.STANDARD));

    // when
    updateResponse = new UpdateAttributeRequestBuilder(key).request(updateReq)
        .submitOk(this.client, siteId).response();

    // then
    assertEquals("Attribute '" + key + "' updated", updateResponse.getMessage());
    assertAttributeEquals(siteId, key, AttributeType.STANDARD, null);
  }

  /**
   * PATCH /attributes/{key} update to governance and back not admin.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("sites")
  public void testUpdateAttributes02(final String siteId) throws ApiException {
    // given

    setBearerToken("Admins");

    String key0 = "abc_" + ID.uuid();
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key0).type(AttributeType.STANDARD));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    String key1 = "abc_" + ID.uuid();
    req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(key1).type(AttributeType.GOVERNANCE));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    setBearerToken(siteId);
    UpdateAttributeRequest updateReq0 = new UpdateAttributeRequest()
        .attribute(new UpdateAttribute().type(AttributeType.GOVERNANCE));
    UpdateAttributeRequest updateReq1 =
        new UpdateAttributeRequest().attribute(new UpdateAttribute().type(AttributeType.STANDARD));

    // when
    var errorResponse1 =
        new UpdateAttributeRequestBuilder(key0).request(updateReq0).submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"error\":\"Access denied to attribute\"}]}");

    // when
    var errorResponse2 =
        new UpdateAttributeRequestBuilder(key1).request(updateReq1).submit(this.client, siteId);

    // then
    assertApiError(errorResponse2, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"error\":\"Access denied to attribute\"}]}");
  }

  /**
   * PATCH /attributes/{key} validationRegex.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testUpdateAttributesValidationRegex(final String siteId) throws ApiException {
    // given
    final String validationRegex = "PO-\\d+";

    setBearerToken(siteId);
    String key = "purchaseOrder_" + ID.uuid();
    AddAttributeRequest req = new AddAttributeRequest().attribute(new AddAttribute().key(key));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    setBearerToken(siteId + "_govern");

    // when
    updateAttribute(siteId, key);

    // then
    var resp = new GetAttributeRequestBuilder(key).submitOk(client, siteId).response();
    var attribute = resp.getAttribute();
    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals(validationRegex, attribute.getValidationRegex());
  }

  /**
   * PATCH /attributes/{key} with blank validationRegex removes validationRegex.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException an error has occurred
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testUpdateAttributesValidationRegexBlank(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId);
    String key = "purchaseOrder_" + ID.uuid();
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(key).validationRegex("PO-\\d+"));
    new AddAttributeRequestBuilder().request(req).submitOk(this.client, siteId);

    setBearerToken(siteId + "_govern");
    UpdateAttributeRequest updateReq = new UpdateAttributeRequest()
        .attribute(new UpdateAttribute().type(AttributeType.STANDARD).validationRegex(""));

    // when
    new UpdateAttributeRequestBuilder(key).request(updateReq).submitOk(this.client, siteId);

    // then
    var resp = new GetAttributeRequestBuilder(key).submitOk(client, siteId).response();
    var attribute = resp.getAttribute();
    assertNotNull(attribute);
    assertEquals(key, attribute.getKey());
    assertEquals(AttributeType.STANDARD, attribute.getType());
    assertNull(attribute.getValidationRegex());
  }

  /**
   * PATCH /documents/{documentId} with OPA.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testUpdateDocumentAttribute01(final String siteId) throws ApiException {
    // given

    setBearerToken(siteId + "_govern");

    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);
    setBearerToken(siteId);

    List<SearchResultDocument> documents = notNull(new GetFoldersRequestBuilder().indexKey(null)
        .path(null).limit(null).next(null).submitOk(this.client, siteId).response().getDocuments());
    assertEquals(1, documents.size());

    UpdateDocumentRequest updateReq =
        new UpdateDocumentRequest().path("somepath.txt").addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("security").stringValue("other")));

    // when

    // when
    var errorResponse1 =
        new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
            .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
        "{\"errors\":[{\"key\":\"security\","
            + "\"error\":\"attribute can only be changed by GOVERN or ADMIN role\"}]}");
    documents = notNull(new GetFoldersRequestBuilder().indexKey(null).path(null).limit(null)
        .next(null).submitOk(this.client, siteId).response().getDocuments());
    assertEquals(1, documents.size());
  }

  /**
   * PATCH /documents/{documentId} with OPA and Govern permission.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testUpdateDocumentAttribute02(final String siteId) throws ApiException {
    // given

    // setBearerToken(siteId);
    setBearerToken(new String[] {siteId, siteId + "_govern"});

    addAttribute(siteId, "security", null, AttributeType.OPA);

    String documentId = addDocumentAttribute(siteId, "security", "confidential", null, null);

    List<SearchResultDocument> documents = notNull(new GetFoldersRequestBuilder().indexKey(null)
        .path(null).limit(null).next(null).submitOk(this.client, siteId).response().getDocuments());
    assertEquals(1, documents.size());

    UpdateDocumentRequest updateReq =
        new UpdateDocumentRequest().path("somepath.txt").addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("security").stringValue("other")));

    // when
    new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null), updateReq)
        .submitOk(this.client, siteId);

    // then
    List<DocumentAttribute> documentAttributes =
        notNull(new GetDocumentAttributesRequestBuilder(DocumentArtifact.of(documentId, null))
            .limit(null).next(null).submitOk(this.client, siteId).response().getAttributes());
    assertEquals(1, documentAttributes.size());
    assertEquals("security", documentAttributes.getFirst().getKey());
    assertEquals("other", documentAttributes.getFirst().getStringValue());
  }

  /**
   * PATCH /documents, set Attribute Type OPA and not allow changing by anyone but admin.
   *
   * @param siteId site identifier; null selects the default site
   * @throws ApiException ApiException
   */
  @ParameterizedTest(name = "{displayName} [siteId={0}]")
  @MethodSource("explicitSites")
  public void testUploadDocumentAttribute02(final String siteId) throws ApiException {
    // given
    final String key = "security";

    setBearerToken(siteId + "_govern");
    addAttribute(siteId, key, AttributeDataType.STRING, AttributeType.OPA);

    AddDocumentUploadRequest docReq =
        new AddDocumentUploadRequest().addAttributesItem(createStringAttribute(key, "confidental"));

    // add document
    String documentId = new AddDocumentUploadRequestBuilder(docReq).submitOk(this.client, siteId)
        .response().getDocumentId();
    assertNotNull(documentId);
    assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());

    setBearerToken(siteId);

    // when

    // when
    var errorResponse1 = new UpdateDocumentRequestBuilder(DocumentArtifact.of(documentId, null),
        new UpdateDocumentRequest().addAttributesItem(createStringAttribute(key, "public")))
        .submit(this.client, siteId);

    // then
    assertApiError(errorResponse1, ApiResponseStatus.SC_BAD_REQUEST.getStatusCode());
    assertEquals("confidental", getDocumentAttribute(siteId, documentId, key).getStringValue());
  }

}
