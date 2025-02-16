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
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddReindexDocumentRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.ReindexTarget;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /reindex/documents/{documentId}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ReindexDocumentsRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /reindex/documents/{documentId} missing target.
   *
   */
  @Test
  public void testAddReindexDocumentsAttributes01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      String documentId = ID.uuid();
      AddReindexDocumentRequest req = new AddReindexDocumentRequest();

      // when
      try {
        reindexApi.addReindexDocument(documentId, req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"target\",\"error\":\"'target' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /reindex/documents/{documentId} based on siteschema added after.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddReindexDocumentsAttributes02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "invoice");
      addAttribute(siteId, "date");

      String documentId = addDocument(siteId,
          List.of(createAttribute("invoice", "INV0001"), createAttribute("date", "20240101")));

      setSiteSchema(siteId, createSiteSchema(
          new List[] {List.of("invoice", "date"), List.of("invoice", "date", "abc")}));

      AddReindexDocumentRequest req =
          new AddReindexDocumentRequest().target(ReindexTarget.ATTRIBUTES);

      // when
      AddResponse addResponse = reindexApi.addReindexDocument(documentId, req, siteId);

      // then
      assertEquals("Reindex started for documentId '" + documentId + "' on target 'ATTRIBUTES'",
          addResponse.getMessage());
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, documentId);

      final int expected = 3;
      assertEquals(expected, documentAttributes.size());

      int i = 0;
      assertDocumentAttributes(documentAttributes.get(i++), "date", "20240101");
      assertDocumentAttributes(documentAttributes.get(i++), "invoice", "INV0001");
      assertDocumentAttributes(documentAttributes.get(i), "invoice::date", "INV0001::20240101");
    }
  }

  /**
   * POST /reindex/documents/{documentId} based on compositeKeys changing.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddReindexDocumentsAttributes03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");
      addAttribute(siteId, "invoice");
      addAttribute(siteId, "date");

      setSiteSchema(siteId, createSiteSchema(new List[] {List.of("invoice", "date")}));

      // when
      String documentId = addDocument(siteId, List.of(createAttribute("invoice", "INV0001"),
          createAttribute("date", "20240101"), createAttribute("documentType", "invoice")));

      // then
      final int expected = 4;
      int i = 0;
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(expected, documentAttributes.size());

      assertDocumentAttributes(documentAttributes.get(i++), "date", "20240101");
      assertDocumentAttributes(documentAttributes.get(i++), "documentType", "invoice");
      assertDocumentAttributes(documentAttributes.get(i++), "invoice", "INV0001");
      assertDocumentAttributes(documentAttributes.get(i), "invoice::date", "INV0001::20240101");

      // given
      setSiteSchema(siteId,
          createSiteSchema(new List[] {List.of("invoice", "date", "documentType")}));

      AddReindexDocumentRequest req =
          new AddReindexDocumentRequest().target(ReindexTarget.ATTRIBUTES);

      // when
      AddResponse addResponse = reindexApi.addReindexDocument(documentId, req, siteId);

      // then
      assertEquals("Reindex started for documentId '" + documentId + "' on target 'ATTRIBUTES'",
          addResponse.getMessage());
      documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(expected, documentAttributes.size());

      i = 0;
      assertDocumentAttributes(documentAttributes.get(i++), "date", "20240101");
      assertDocumentAttributes(documentAttributes.get(i++), "documentType", "invoice");
      assertDocumentAttributes(documentAttributes.get(i++), "invoice", "INV0001");
      assertDocumentAttributes(documentAttributes.get(i), "invoice::date::documentType",
          "INV0001::20240101::invoice");
    }
  }

  /**
   * POST /reindex/documents/{documentId} new schema required field with default value added.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddReindexDocumentsAttributes04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      // when
      String documentId = addDocument(siteId, Collections.emptyList());

      // then
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(0, documentAttributes.size());

      // given
      AddAttributeSchemaRequired required =
          new AddAttributeSchemaRequired().attributeKey("documentType").defaultValue("123");
      SetSchemaAttributes sa = new SetSchemaAttributes().addRequiredItem(required);
      setSiteSchema(siteId, sa);

      AddReindexDocumentRequest req =
          new AddReindexDocumentRequest().target(ReindexTarget.ATTRIBUTES);

      // when
      AddResponse addResponse = reindexApi.addReindexDocument(documentId, req, siteId);

      // then
      assertEquals("Reindex started for documentId '" + documentId + "' on target 'ATTRIBUTES'",
          addResponse.getMessage());
      documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(1, documentAttributes.size());

      assertDocumentAttributes(documentAttributes.get(0), "documentType", "123");
    }
  }

  /**
   * POST /reindex/documents/{documentId} new schema required field with default value added but
   * existing attribute already set.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddReindexDocumentsAttributes05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "documentType");

      // when
      String documentId = addDocument(siteId, List.of(createAttribute("documentType", "other")));

      // then
      List<DocumentAttribute> documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(1, documentAttributes.size());
      assertDocumentAttributes(documentAttributes.get(0), "documentType", "other");

      // given
      AddAttributeSchemaRequired required =
          new AddAttributeSchemaRequired().attributeKey("documentType").defaultValue("123");
      SetSchemaAttributes sa = new SetSchemaAttributes().addRequiredItem(required);
      setSiteSchema(siteId, sa);

      AddReindexDocumentRequest req =
          new AddReindexDocumentRequest().target(ReindexTarget.ATTRIBUTES);

      // when
      AddResponse addResponse = reindexApi.addReindexDocument(documentId, req, siteId);

      // then
      assertEquals("Reindex started for documentId '" + documentId + "' on target 'ATTRIBUTES'",
          addResponse.getMessage());
      documentAttributes = getDocumentAttributes(siteId, documentId);
      assertEquals(1, documentAttributes.size());

      assertDocumentAttributes(documentAttributes.get(0), "documentType", "other");
    }
  }

  private SetSchemaAttributes createSiteSchema(final List<String>[] compositeKeys) {
    SetSchemaAttributes sa = new SetSchemaAttributes();

    for (List<String> compositeKey : compositeKeys) {
      sa.addCompositeKeysItem(new AttributeSchemaCompositeKey().attributeKeys(compositeKey));
    }

    return sa;
  }

  private List<DocumentAttribute> getDocumentAttributes(final String siteId,
      final String documentId) throws ApiException {
    return notNull(this.documentAttributesApi.getDocumentAttributes(documentId, siteId, "100", null)
        .getAttributes());
  }

  private void addAttribute(final String siteId, final String key) throws ApiException {
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(key).dataType(null));
    this.attributesApi.addAttribute(req, siteId);
  }

  private void setSiteSchema(final String siteId, final SetSchemaAttributes attr)
      throws ApiException {
    SetSitesSchemaRequest setSiteSchema = new SetSitesSchemaRequest().name("test").attributes(attr);
    this.schemasApi.setSitesSchema(siteId, setSiteSchema);
  }

  private void assertDocumentAttributes(final DocumentAttribute da,
      final String expectedAttributeKey, final String expectedStringValue) {
    assertEquals(expectedAttributeKey, da.getKey());
    assertEquals(expectedStringValue, da.getStringValue());
    assertNotNull(da.getInsertedDate());
    assertNotNull(da.getUserId());
  }

  private String addDocument(final String siteId, final List<AddDocumentAttribute> attributes)
      throws ApiException {
    AddDocumentRequest areq = new AddDocumentRequest().content("adasd").attributes(attributes);
    return this.documentsApi.addDocument(areq, siteId, null).getDocumentId();
  }

  private AddDocumentAttribute createAttribute(final String attributeKey,
      final String stringValue) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(attributeKey).stringValue(stringValue));
  }
}
