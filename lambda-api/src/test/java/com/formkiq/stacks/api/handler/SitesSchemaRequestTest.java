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
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SearchRangeDataType;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.createNumberAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringAttribute;
import static com.formkiq.testutils.aws.FkqAttributeService.createStringsAttribute;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /sites/{siteId}/schema/document. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class SitesSchemaRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /documents Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocument01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq =
          new AddDocumentRequest().content("adasd").addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("category")));

      // when
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "strings", "category", null);
    }
  }

  private void addAttribute(final String siteId, final String key,
      final AttributeDataType dataType) {
    try {
      AddAttributeRequest req =
          new AddAttributeRequest().attribute(new AddAttribute().key(key).dataType(dataType));
      this.attributesApi.addAttribute(req, siteId);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * POST /documents Add attributes. Missing attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocument02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);


      // when
      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      try {
        this.documentsApi.addDocument(areq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocument03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req0 = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req0);

      req0 = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addOptionalItem(createOptional("strings")));
      this.schemasApi.setSitesSchema(siteId, req0);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");

      // when
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(0, attributes.size());
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttributesRequest attrReq =
          new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("category")));

      // when
      AddResponse response =
          this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "strings", "category", null);
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Add attributes missing required.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "other", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttributesRequest attrReq =
          new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("other").stringValue("category")));

      // when
      try {
        this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes Add attributes - test composite keys generated.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String attribute : List.of("strings", "category", "documentType", "other")) {
        addAttribute(siteId, attribute, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addCompositeKeysItem(createCompositeKey("strings", "category"))
              .addCompositeKeysItem(createCompositeKey("strings", "documentType")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      // when
      AddDocumentAttributesRequest attrReq = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(List.of("category", "1234"))))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("documentType").stringValue("invoice")))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValue("doc")));
      this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 5;
      int i = 0;
      assertEquals(expected, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "category", "doc", null);
      assertDocumentAttributeEquals(attributes.get(i++), "documentType", "invoice", null);
      assertDocumentAttributeEquals(attributes.get(i++), "strings", null, "1234,category");
      assertDocumentAttributeEquals(attributes.get(i++), "strings::category", null,
          "1234::doc,category::doc");
      assertDocumentAttributeEquals(attributes.get(i), "strings::documentType", null,
          "1234::invoice,category::invoice");

      // given
      AddDocumentAttributesRequest attrReq1 = new AddDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute("other", List.of("thing123")));

      // when
      AddResponse addResponse =
          this.documentAttributesApi.addDocumentAttributes(documentId, attrReq1, siteId, null);

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", addResponse.getMessage());
    }
  }

  /**
   * POST /documents/{documentId}/attributes Add attributes - existing attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String attribute : List.of("strings")) {
        addAttribute(siteId, attribute, null);
      }

      AddDocumentRequest areq =
          new AddDocumentRequest().content("adasd").addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("category")));

      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      // when
      AddDocumentAttributesRequest attrReq =
          new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("1234")));

      try {
        this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\","
                + "\"error\":\"document attribute 'strings' already exists\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/{documentId}/attributes. Add attributes with NULL required attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);
      addAttribute(siteId, "strings", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes().required(null));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttributesRequest attrReq =
          new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("category")));

      // when
      AddResponse response =
          this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "strings", "category", null);
    }
  }

  /**
   * POST /documents/upload with site schema required attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key = "category";
      addAttribute(siteId, key, null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired(key)));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq =
          new AddDocumentUploadRequest().path("sample.txt").contentType("text/plain");

      // when
      try {
        this.documentsApi.addDocumentUpload(ureq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"category\","
            + "\"error\":\"missing required attribute 'category'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/upload with site schema required attribute and default value.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(createRequired("category").defaultValue("person"))
              .addRequiredItem(createRequired("flag").defaultValue("true"))
              .addRequiredItem(createRequired("keyonly"))
              .addRequiredItem(createRequired("num").defaultValues(Arrays.asList("123", "233")))
              .addRequiredItem(
                  createRequired("strings").defaultValues(Arrays.asList("abc", "qwe"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq =
          new AddDocumentUploadRequest().path("sample.txt").contentType("text/plain");

      // when
      GetDocumentUrlResponse response0 =
          this.documentsApi.addDocumentUpload(ureq, siteId, null, null, null);

      AddDocumentRequest areq = new AddDocumentRequest().path("sample.txt").content("test sample")
          .contentType("text/plain");

      AddDocumentResponse response1 = this.documentsApi.addDocument(areq, siteId, null);

      // then
      for (String documentId : Arrays.asList(response0.getDocumentId(),
          response1.getDocumentId())) {

        List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
            .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

        int i = 0;
        final int expected = 5;
        assertEquals(expected, attributes.size());
        assertDocumentAttributeEquals(attributes.get(i), "category", "person", null);
        // assertEquals("category", attributes.get(i).getKey());
        assertEquals(AttributeValueType.STRING, attributes.get(i++).getValueType());
        // assertEquals("person", attributes.get(i++).getStringValue());

        assertEquals("flag", attributes.get(i).getKey());
        assertEquals(AttributeValueType.BOOLEAN, attributes.get(i).getValueType());
        assertEquals(Boolean.TRUE, attributes.get(i++).getBooleanValue());

        assertEquals("keyonly", attributes.get(i).getKey());
        assertEquals(AttributeValueType.KEY_ONLY, attributes.get(i).getValueType());
        assertNull(attributes.get(i++).getStringValue());

        assertEquals("num", attributes.get(i).getKey());
        assertEquals(AttributeValueType.NUMBER, attributes.get(i).getValueType());
        assertEquals("123,233", String.join(",", notNull(attributes.get(i++).getNumberValues())
            .stream().map(n -> formatDouble(n.doubleValue())).toList()));

        assertDocumentAttributeEquals(attributes.get(i), "strings", null, "abc,qwe");
        // assertEquals("strings", attributes.get(i).getKey());
        assertEquals(AttributeValueType.STRING, attributes.get(i).getValueType());
        // assertEquals("abc,qwe", String.join(",", notNull(attributes.get(i).getStringValues())));
      }
    }
  }

  private AttributeSchemaRequired createRequired(final String attributeKey) {
    return new AttributeSchemaRequired().attributeKey(attributeKey);
  }

  /**
   * POST /documents/upload with site schema required attribute and no default value.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings"))
              .addRequiredItem(createRequired("flag")).addRequiredItem(createRequired("keyonly"))
              .addRequiredItem(createRequired("num")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq = new AddDocumentUploadRequest().path("sample.txt")
          .contentType("text/plain")
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222"))))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE)))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard().key("num")
              .numberValues(Arrays.asList(new BigDecimal("111.11"), new BigDecimal("222.22")))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq, siteId, null, null, null);

      // then
      String documentId = response.getDocumentId();
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      int i = 0;
      final int expected = 4;
      assertEquals(expected, attributes.size());

      assertEquals("flag", attributes.get(i).getKey());
      assertEquals(Boolean.TRUE, attributes.get(i++).getBooleanValue());

      assertEquals("keyonly", attributes.get(i).getKey());
      assertNull(attributes.get(i++).getStringValue());

      assertEquals("num", attributes.get(i).getKey());
      assertEquals("111.11,222.22", String.join(",", notNull(attributes.get(i++).getNumberValues())
          .stream().map(n -> formatDouble(n.doubleValue())).toList()));

      assertDocumentAttributeEquals(attributes.get(i), "strings", null, "111,222");
    }
  }

  /**
   * POST /documents/upload with site schema required attribute and no default value and allowed
   * values.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(
                  createRequired("strings").addAllowedValuesItem("111").addAllowedValuesItem("222"))
              .addRequiredItem(createRequired("flag").addAllowedValuesItem("true"))
              .addRequiredItem(createRequired("keyonly")).addRequiredItem(createRequired("num")
                  .addAllowedValuesItem("111.11").addAllowedValuesItem("222.22")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222"))))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.TRUE)))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard().key("num")
              .numberValues(Arrays.asList(new BigDecimal("111.11"), new BigDecimal("222.22")))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      String documentId = response.getDocumentId();
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      final int expected = 4;
      assertEquals(expected, attributes.size());

      // given
      AddDocumentUploadRequest ureq1 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(createStringsAttribute("strings", List.of("333")))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("flag").booleanValue(Boolean.FALSE)))
          .addAttributesItem(createNumberAttribute("num", new BigDecimal("111.12")));

      // when
      try {
        this.documentsApi.addDocumentUpload(ureq1, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\",\"error\":\"invalid attribute value 'strings', "
                + "only allowed values are 111,222\"},{\"key\":\"flag\","
                + "\"error\":\"invalid attribute value 'flag', only allowed values are true\"},"
                + "{\"key\":\"num\",\"error\":\"invalid attribute value 'num', only allowed "
                + "values are 111.11,222.22\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/upload with site schema optional attributes with valid/invalid attributes
   * values.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addOptionalItem(createOptional("strings").allowedValues(List.of("123")))
              .addOptionalItem(createOptional("category").allowedValues(List.of("person"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValues(List.of("123"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // given
      AddDocumentUploadRequest ureq1 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValues(List.of("333"))));

      // when
      try {
        this.documentsApi.addDocumentUpload(ureq1, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\","
                + "\"error\":\"invalid attribute value 'strings', only allowed values are 123\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/upload with site schema. Add attributes not in required / optional /
   * allowAdditionalAttributes = true. values.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addOptionalItem(createOptional("strings")).allowAdditionalAttributes(Boolean.TRUE));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("123"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());
    }
  }

  /**
   * POST /documents/upload with site schema. Add attributes missing key.
   *
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema07() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired())
              .addOptionalItem(new AttributeSchemaOptional())
              .allowAdditionalAttributes(Boolean.FALSE));

      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        assertEquals(
            "{\"errors\":[{\"error\":\"required attribute missing attributeKey'\"},"
                + "{\"error\":\"optional attribute missing attributeKey'\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /documents/upload with site schema. Add attributes not in required / optional /
   * allowAdditionalAttributes = false. values.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema08() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addOptionalItem(createOptional("strings")).allowAdditionalAttributes(Boolean.FALSE));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("123"))));

      // when
      try {
        this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"category\",\"error\":\"attribute 'category' "
            + "is not listed as a required or optional attribute\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue}. schema required
   * attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributeAndValue03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").addStringValuesItem("1")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttributeAndValue(documentId, "strings", "1",
            siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\",\"error\":\"'strings' is a required attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}. invalid attributeKey.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributes01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttribute(documentId, "strings", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"message\":\"attribute 'strings' not found on document '" + documentId + "'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}. valid attributeKey.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributes02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").addStringValuesItem("111")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response =
          this.documentAttributesApi.deleteDocumentAttribute(documentId, "strings", siteId);

      // then
      assertEquals("attribute 'strings' removed from document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(0, attributes.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}. schema required attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributes03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").addStringValuesItem("1")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttribute(documentId, "strings", siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\",\"error\":\"'strings' is a required attribute\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue}. invalid
   * attributeKey.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributeValue01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      try {
        this.documentAttributesApi.deleteDocumentAttributeAndValue(documentId, "category", "value",
            siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"message\":\"attribute 'category' not found on document ' " + documentId + "'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue}. valid attributeKey.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributeValue02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").addStringValuesItem("111")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "strings", "111", siteId);

      // then
      assertEquals(
          "attribute value '111' removed from attribute 'strings', document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(0, attributes.size());
    }
  }

  /**
   * DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue}. schema optional
   * attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteDocumentAttributeValue04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addOptionalItem(createOptional("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").addStringValuesItem("111")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "strings", "111", siteId);

      // then
      assertEquals(
          "attribute value '111' removed from attribute 'strings', document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(0, attributes.size());
    }
  }

  /**
   * GET /sites/{siteId}/schema/document, not set.
   *
   */
  @Test
  public void testGetSitesSchema01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      // when
      try {
        this.schemasApi.getSitesSchema(siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"message\":\"Sites Schema not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} without schema, no attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutDocumentAttributesKey01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String attributeKey = "strings";

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("123"));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributeValue(documentId, attributeKey, setReq,
            siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\",\"error\":\"attribute 'strings' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} without schema.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutDocumentAttributesKey02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String attributeKey = "strings";
      addAttribute(siteId, attributeKey, null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("123"));

      // when
      this.documentAttributesApi.setDocumentAttributeValue(documentId, attributeKey, setReq,
          siteId);

      // then
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), attributeKey, "123", null);
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with: - Site Schema - Required attribute
   * - add different attribute.
   *
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutDocumentAttributesKey03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValue("person")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("123"));

      // when
      SetResponse response = this.documentAttributesApi.setDocumentAttributeValue(documentId,
          "category", setReq, siteId);

      // then
      assertEquals("Updated attribute 'category' on document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "category", "123", null);
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with: - Site Schema - Required attribute
   * - invalid value.
   *
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutDocumentAttributesKey04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("person")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired()
              .attributeKey("strings").addAllowedValuesItem("1234").addAllowedValuesItem("4444")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("123"));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributeValue(documentId, "strings", setReq, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"strings\",\"error\":\"invalid attribute value 'strings', "
                + "only allowed values are 1234,4444\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes/{attributeKey} with: - Site Schema - Required attribute
   * - allowed value.
   *
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testPutDocumentAttributesKey05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("person")));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired()
              .attributeKey("strings").addAllowedValuesItem("1234").addAllowedValuesItem("4444")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("4444"));

      // when
      SetResponse response = this.documentAttributesApi.setDocumentAttributeValue(documentId,
          "strings", setReq, siteId);

      // then
      assertEquals("Updated attribute 'strings' on document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "strings", "4444", null);
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload EQ.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category"))
              .addRequiredItem(createRequired("strings"))
              .addCompositeKeysItem(createCompositeKey("strings", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());

      // invalid search
      DocumentSearchRequest sreq1 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("333"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      DocumentSearchResponse response1 =
          this.searchApi.documentSearch(sreq1, siteId, null, null, null);
      assertEquals(0, notNull(response1.getDocuments()).size());

      // wrong attribute order
      DocumentSearchRequest sreq2 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person"))
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222")));

      DocumentSearchResponse response2 =
          this.searchApi.documentSearch(sreq2, siteId, null, null, null);
      assertEquals(1, notNull(response2.getDocuments()).size());
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload BEGINS WITH.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category"))
              .addRequiredItem(createRequired("strings"))
              .addCompositeKeysItem(createCompositeKey("strings", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").beginsWith("p")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());

      // invalid search
      DocumentSearchRequest sreq1 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").beginsWith("a")));

      DocumentSearchResponse response1 =
          this.searchApi.documentSearch(sreq1, siteId, null, null, null);
      assertEquals(0, notNull(response1.getDocuments()).size());

      // begingsWith as first element
      DocumentSearchRequest sreq2 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").beginsWith("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("p")));

      try {
        this.searchApi.documentSearch(sreq2, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"beginsWith\""
                + ",\"error\":\"'beginsWith' can only be used on last attribute in list\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload String RANGE.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category")).addRequiredItem(createRequired("date"))
              .addCompositeKeysItem(createCompositeKey("category", "date")));
      this.schemasApi.setSitesSchema(siteId, req);

      for (String value : Arrays.asList("2024-01-03", "2024-01-04", "2024-01-05", "2024-01-10",
          "2024-02-03")) {
        createRangeAttributeString(siteId, value);
      }

      // when - valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person"))
          .addAttributesItem(new DocumentSearchAttribute().key("date")
              .range(new DocumentSearchRange().start("2024-01-04").end("2024-01-10"))));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);

      // then
      final int expected = 3;
      int i = 0;
      List<SearchResultDocument> documents = notNull(response0.getDocuments());
      assertEquals(expected, documents.size());

      assertEquals("category::date",
          requireNonNull(documents.get(i).getMatchedAttribute()).getKey());
      assertEquals("person::2024-01-04",
          requireNonNull(documents.get(i++).getMatchedAttribute()).getStringValue());

      assertEquals("category::date",
          requireNonNull(documents.get(i).getMatchedAttribute()).getKey());
      assertEquals("person::2024-01-05",
          requireNonNull(documents.get(i++).getMatchedAttribute()).getStringValue());

      assertEquals("category::date",
          requireNonNull(documents.get(i).getMatchedAttribute()).getKey());
      assertEquals("person::2024-01-10",
          requireNonNull(documents.get(i).getMatchedAttribute()).getStringValue());

      // when - invalid search
      DocumentSearchRequest sreq1 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category")
              .range(new DocumentSearchRange().start("2024-02-04").end("2024-03-05"))));

      try {
        this.searchApi.documentSearch(sreq1, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"no composite key"
            + " found for attributes 'strings,category'\"}]}", e.getResponseBody());
      }
    }
  }

  private void createRangeAttributeString(final String siteId, final String value)
      throws ApiException {
    AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
        .addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("category").stringValue("person")))
        .addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("date").stringValue(value)));

    this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);
  }

  /**
   * Search document by strict composite key using POST /documents/upload String wrong order RANGE.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category")).addRequiredItem(createRequired("date"))
              .addCompositeKeysItem(createCompositeKey("category", "date")));
      this.schemasApi.setSitesSchema(siteId, req);

      for (String value : Arrays.asList("2024-01-03", "2024-01-04", "2024-01-05", "2024-01-10",
          "2024-02-03")) {
        createRangeAttributeString(siteId, value);
      }

      // range as first element
      DocumentSearchRequest sreq2 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("date")
              .range(new DocumentSearchRange().start("2024-01-0").end("2024-01-0")))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("p")));

      // when
      try {
        this.searchApi.documentSearch(sreq2, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"range\""
                + ",\"error\":\"'range' can only be used on last attribute in list\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload number RANGE.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category")).addRequiredItem(createRequired("date"))
              .addCompositeKeysItem(createCompositeKey("category", "date")));
      this.schemasApi.setSitesSchema(siteId, req);

      for (String value : Arrays.asList("100.12", "150.14", "200", "300", "1000")) {
        createRangeAttributeNumber(siteId, value);
      }

      // when - valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person"))
          .addAttributesItem(new DocumentSearchAttribute().key("date").range(
              new DocumentSearchRange().start("1").end("275").type(SearchRangeDataType.NUMBER))));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);

      // then
      final int expected = 3;
      int i = 0;
      assertEquals(expected, notNull(response0.getDocuments()).size());

      assertEquals("category::date",
          requireNonNull(response0.getDocuments().get(i).getMatchedAttribute()).getKey());
      assertEquals("person::000000000000100.1200",
          requireNonNull(response0.getDocuments().get(i++).getMatchedAttribute()).getStringValue());

      assertEquals("category::date",
          requireNonNull(response0.getDocuments().get(i).getMatchedAttribute()).getKey());
      assertEquals("person::000000000000150.1400",
          requireNonNull(response0.getDocuments().get(i++).getMatchedAttribute()).getStringValue());

      assertEquals("category::date",
          requireNonNull(response0.getDocuments().get(i).getMatchedAttribute()).getKey());
      assertEquals("person::000000000000200.0000",
          requireNonNull(response0.getDocuments().get(i).getMatchedAttribute()).getStringValue());
    }
  }

  private void createRangeAttributeNumber(final String siteId, final String value)
      throws ApiException {
    AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
        .addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("category").stringValue("person")))
        .addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key("date").numberValue(new BigDecimal(value))));

    this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);
  }

  /**
   * Search document by strict composite key using POST /documents/upload number EQOR.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category"))
              .addRequiredItem(createRequired("strings"))
              .addCompositeKeysItem(createCompositeKey("strings", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222", "333"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(
              new DocumentSearchAttribute().key("strings").eqOr(Arrays.asList("111", "222")))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      try {
        this.searchApi.documentSearch(sreq0, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"range\",\"error\":\"'eqOr' "
            + "is not supported with composite keys\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload EQ with optional tag.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey07() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addOptionalItem(createOptional("category"))
              .addRequiredItem(createRequired("strings"))
              .addCompositeKeysItem(createCompositeKey("strings", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("strings").stringValues(Arrays.asList("111", "222"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // valid search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());
    }
  }

  /**
   * Search document by old composite key.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey08() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);
      addAttribute(siteId, "other", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category"))
              .addRequiredItem(createRequired("strings"))
              .addCompositeKeysItem(createCompositeKey("strings", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(createRequired("category")).addRequiredItem(createRequired("other"))
              .addCompositeKeysItem(createCompositeKey("other", "category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("other").stringValues(Arrays.asList("111", "222"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // old composite key search
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      try {
        this.searchApi.documentSearch(sreq0, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"error\":\"no composite key found "
            + "for attributes 'strings,category'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Search document by composite key not defined in optional/required attributes.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey09() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key0 = "strings_" + siteId;
      String key1 = "category_" + siteId;
      addAttribute(siteId, key0, null);
      addAttribute(siteId, key1, null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addCompositeKeysItem(createCompositeKey(key0, key1)));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(key1).stringValues(List.of("person"))))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard().key(key0)
              .stringValues(Arrays.asList("111", "222"))));

      // when
      String documentId0 =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(
          new DocumentSearch().addAttributesItem(new DocumentSearchAttribute().key(key0).eq("222"))
              .addAttributesItem(new DocumentSearchAttribute().key(key1).eq("person")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());
      assertEquals(documentId0, response0.getDocuments().get(0).getDocumentId());
    }
  }

  /**
   * Search document by composite key by defined in optional/required attributes.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey10() throws ApiException {
    // given
    List<String> attributeKeys =
        Arrays.asList("key1", "key2", "customerId", "customerUUID", "transactionId");
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      attributeKeys.forEach(a -> addAttribute(siteId, a, null));

      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .addCompositeKeysItem(createCompositeKey("key1", "key2"))
          .addCompositeKeysItem(createCompositeKey("customerId", "customerUUID"))
          .addCompositeKeysItem(createCompositeKey("customerId", "transactionId"))
          .addCompositeKeysItem(createCompositeKey("customerUUID", "transactionId"))
          .addCompositeKeysItem(createCompositeKey("customerId", "customerUUID", "transactionId"))
          .addOptionalItem(createOptional("key1")).addOptionalItem(createOptional("key2"))
          .addOptionalItem(createOptional("customerId"))
          .addOptionalItem(createOptional("customerUUID"))
          .addOptionalItem(createOptional("transactionId")).allowAdditionalAttributes(false);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(schemaAttributes);
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("customerId").stringValue("3697c70f9cfe44c9a56185ce224e35b6")))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("customerUUID").stringValue("403cd39862564e36b490738c3c312b38")));

      // when
      String documentId0 =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("customerId")
              .eq("3697c70f9cfe44c9a56185ce224e35b6"))
          .addAttributesItem(new DocumentSearchAttribute().key("customerUUID")
              .eq("403cd39862564e36b490738c3c312b38")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());
      assertEquals(documentId0, response0.getDocuments().get(0).getDocumentId());
    }
  }

  /**
   * Search document by attribute with partial matching composite key.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey11() throws ApiException {
    // given
    List<String> attributeKeys =
        Arrays.asList("key1", "key2", "customerId", "customerUUID", "transactionId");
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      attributeKeys.forEach(a -> addAttribute(siteId, a, null));

      SchemaAttributes schemaAttributes = new SchemaAttributes()
          .addCompositeKeysItem(createCompositeKey("customerId", "customerUUID"))
          .addCompositeKeysItem(createCompositeKey("customerId", "customerUUID", "transactionId"));

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(schemaAttributes);
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("customerId").stringValue("3697c70f9cfe44c9a56185ce224e35b6")))
          .addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeStandard()
              .key("customerUUID").stringValue("403cd39862564e36b490738c3c312b38")));

      // when
      String documentId0 =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // then
      DocumentSearchRequest sreq0 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("customerId")
              .eq("3697c70f9cfe44c9a56185ce224e35b6"))
          .addAttributesItem(new DocumentSearchAttribute().key("customerUUID")
              .eq("403cd39862564e36b490738c3c312b38")));

      DocumentSearchResponse response0 =
          this.searchApi.documentSearch(sreq0, siteId, null, null, null);
      assertEquals(1, notNull(response0.getDocuments()).size());
      assertEquals(documentId0, response0.getDocuments().get(0).getDocumentId());
    }
  }

  private AttributeSchemaOptional createOptional(final String attributeKey) {
    return new AttributeSchemaOptional().attributeKey(attributeKey);
  }

  /**
   * Create Composite Key from {@link String}.
   *
   * @param attributeKeys {@link String}
   * @return AttributeSchemaCompositeKey
   */
  private AttributeSchemaCompositeKey createCompositeKey(final String... attributeKeys) {
    return new AttributeSchemaCompositeKey().attributeKeys(List.of(attributeKeys));
  }

  /**
   * PUT /documents/{documentId}/attributes. Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributesRequest attrReq =
          new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("category")));

      // when
      SetResponse response =
          this.documentAttributesApi.setDocumentAttributes(documentId, attrReq, siteId);

      // then
      assertEquals("set attributes on documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(1, attributes.size());
      assertDocumentAttributeEquals(attributes.get(0), "strings", "category", null);
    }
  }

  /**
   * PUT /documents/{documentId}/attributes. Add attributes missing required.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "other", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributesRequest attrReq =
          new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("other").stringValue("category")));

      // when
      try {
        this.documentAttributesApi.setDocumentAttributes(documentId, attrReq, siteId);
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * PUT /documents/{documentId}/attributes Add attributes - test composite keys generated.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String attribute : List.of("strings", "category", "documentType")) {
        addAttribute(siteId, attribute, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addCompositeKeysItem(createCompositeKey("strings", "category"))
              .addCompositeKeysItem(createCompositeKey("strings", "documentType")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd")
          .addAttributesItem(createStringsAttribute("strings", List.of("category", "1234")))
          .addAttributesItem(createStringAttribute("documentType", "invoice"));
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      int i = 0;
      final int expected3 = 3;
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(expected3, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "documentType", "invoice", null);
      assertDocumentAttributeEquals(attributes.get(i++), "strings", null, "1234,category");
      assertDocumentAttributeEquals(attributes.get(i), "strings::documentType", null,
          "1234::invoice,category::invoice");

      // when
      SetDocumentAttributesRequest attrReq = new SetDocumentAttributesRequest()
          .addAttributesItem(createStringsAttribute("strings", List.of("category", "1234")))
          .addAttributesItem(createStringAttribute("documentType", "invoice"))
          .addAttributesItem(createStringAttribute("category", "doc"));
      this.documentAttributesApi.setDocumentAttributes(documentId, attrReq, siteId);

      // then
      attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 5;
      i = 0;
      assertEquals(expected, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "category", "doc", null);
      assertDocumentAttributeEquals(attributes.get(i++), "documentType", "invoice", null);
      assertDocumentAttributeEquals(attributes.get(i++), "strings", null, "1234,category");
      assertDocumentAttributeEquals(attributes.get(i++), "strings::category", null,
          "1234::doc,category::doc");
      assertDocumentAttributeEquals(attributes.get(i), "strings::documentType", null,
          "1234::invoice,category::invoice");
    }
  }

  /**
   * PUT /documents/{documentId}/attributes Add attributes - test composite keys generated /
   * deleted.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String attribute : List.of("strings", "category", "documentType")) {
        addAttribute(siteId, attribute, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addCompositeKeysItem(createCompositeKey("strings", "category"))
              .addCompositeKeysItem(createCompositeKey("strings", "documentType"))
              .addCompositeKeysItem(createCompositeKey("category", "documentType")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd")
          .addAttributesItem(createStringsAttribute("strings", List.of("category", "1234")))
          .addAttributesItem(createStringAttribute("documentType", "invoice"));
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      int i = 0;
      final int expected3 = 3;
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(expected3, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "documentType", "invoice", null);
      assertDocumentAttributeEquals(attributes.get(i++), "strings", null, "1234,category");
      assertDocumentAttributeEquals(attributes.get(i), "strings::documentType", null,
          "1234::invoice,category::invoice");

      // when
      SetDocumentAttributesRequest attrReq = new SetDocumentAttributesRequest()
          .addAttributesItem(createStringAttribute("documentType", "invoice"))
          .addAttributesItem(createStringAttribute("category", "doc"));
      this.documentAttributesApi.setDocumentAttributes(documentId, attrReq, siteId);

      // then
      attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

      final int expected = 3;
      i = 0;
      assertEquals(expected, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "category", "doc", null);
      assertDocumentAttributeEquals(attributes.get(i++), "category::documentType", "doc::invoice",
          null);
      assertDocumentAttributeEquals(attributes.get(i), "documentType", "invoice", null);
    }
  }

  /**
   * PUT /documents/{documentId}/attributes Add attributes - test composite keys generated with
   * required atrtibutes with default value.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String attribute : List.of("category", "documentType")) {
        addAttribute(siteId, attribute, null);
      }

      AttributeSchemaRequired r0 = new AttributeSchemaRequired().attributeKey("documentType")
          .defaultValues(List.of("invoice", "doc"));

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(r0)
              .addCompositeKeysItem(createCompositeKey("category", "documentType")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd")
          .addAttributesItem(createStringsAttribute("category", List.of("other")));
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      int i = 0;
      final int expected = 3;
      List<DocumentAttribute> attributes = notNull(this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(expected, attributes.size());
      assertDocumentAttributeEquals(attributes.get(i++), "category", "other", null);
      assertDocumentAttributeEquals(attributes.get(i++), "category::documentType", null,
          "other::doc,other::invoice");
      assertDocumentAttributeEquals(attributes.get(i), "documentType", null, "doc,invoice");
    }
  }

  private void assertDocumentAttributeEquals(final DocumentAttribute da, final String attributeKey,
      final String stringValue, final String stringValues) {
    assertEquals(attributeKey, da.getKey());

    if (stringValue != null) {
      assertEquals(stringValue, da.getStringValue());
    }

    if (stringValues != null) {
      assertEquals(stringValues, String.join(",", notNull(da.getStringValues())));
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with required attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key = "category";
      addAttribute(siteId, key, null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired(key)));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      GetSitesSchemaResponse schema = this.schemasApi.getSitesSchema(siteId);
      assertEquals("joe", schema.getName());
      assertNotNull(schema.getAttributes());
      assertEquals(key, notNull(schema.getAttributes().getRequired()).get(0).getAttributeKey());
      assertTrue(notNull(schema.getAttributes().getRequired().get(0).getAllowedValues()).isEmpty());

      req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addRequiredItem(createRequired(key).addAllowedValuesItem("123")));

      // when
      response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      schema = this.schemasApi.getSitesSchema(siteId);
      assertEquals("joe", schema.getName());
      assertNotNull(schema.getAttributes());
      assertEquals(key, notNull(schema.getAttributes().getRequired()).get(0).getAttributeKey());
      assertEquals("123", String.join(",",
          notNull(schema.getAttributes().getRequired().get(0).getAllowedValues())));
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document. Empty.
   *
   */
  @Test
  public void testSetSitesSchema02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest();

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"},"
            + "{\"key\":\"schema\",\"error\":\"'schema' is required\"}]}", e.getResponseBody());
      }

      // given
      req.attributes(new SchemaAttributes());

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document invalid attribute.
   *
   */
  @Test
  public void testSetSitesSchema03() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      String key = "category";

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired(key)));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"category\",\"error\":\"attribute 'category' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with overlapping required/optional attribute.
   *
   */
  @Test
  public void testSetSitesSchema04() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("category"))
              .addOptionalItem(createOptional("category")).addOptionalItem(createOptional("test")));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"category\","
                + "\"error\":\"attribute 'category' is in both required & optional lists\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with required/optional attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(createRequired("category")).addOptionalItem(createOptional("test")));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      GetSitesSchemaResponse schema = this.schemasApi.getSitesSchema(siteId);
      assertEquals("joe", schema.getName());
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with missing composite attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.TRUE)
              .addCompositeKeysItem(createCompositeKey("category", "test", "other"))
              .addRequiredItem(createRequired("category")).addOptionalItem(createOptional("test")));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);
      // then
      assertEquals("Sites Schema set", response.getMessage());

      // given
      assertNotNull(req.getAttributes());
      req.getAttributes().setAllowAdditionalAttributes(Boolean.FALSE);

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"other\","
                + "\"error\":\"attribute 'other' not listed in required/optional attributes\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document key only shouldn't allow allowed value or default values.
   *
   */
  @Test
  public void testSetSitesSchema07() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired()
              .attributeKey("keyonly").addAllowedValuesItem("123").addDefaultValuesItem("222")));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(
            "{\"errors\":[{\"key\":\"keyonly\","
                + "\"error\":\"attribute 'keyonly' does not allow allowed values\"},"
                + "{\"key\":\"keyonly\","
                + "\"error\":\"attribute 'keyonly' does not allow default values\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with single composite attribute.
   *
   */
  @Test
  public void testSetSitesSchema08() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.TRUE)
              .addCompositeKeysItem(createCompositeKey("category")));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"compositeKeys\","
                + "\"error\":\"compositeKeys must have more than 1 value\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document with default value not in allowed values.
   *
   */
  @Test
  public void testSetSitesSchema09() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired()
              .attributeKey("category").defaultValue("1").allowedValues(List.of("222"))));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"defaultValue\","
                + "\"error\":\"defaultValue must be part of allowed values\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document duplicate composite keys.
   *
   */
  @Test
  public void testSetSitesSchema10() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "id", null);

      AttributeSchemaCompositeKey k0 = createCompositeKey("category", "id");
      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.TRUE)
              .addCompositeKeysItem(k0).addCompositeKeysItem(k0));

      // when
      try {
        this.schemasApi.setSitesSchema(siteId, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"compositeKeys\",\"error\":\"duplicate compositeKey\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /documents after site schema is applied and without attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("asd.txt");

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /documents after site schema is applied and with invalid attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().path("asd.txt").addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValue("test")));

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
      } catch (ApiException e) {
        // then
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * PATCH /documents after site schema is applied and with valid attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().path("asd.txt").addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("strings").stringValue("test")));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("asd.txt", document.getPath());
    }
  }

  /**
   * PATCH /documents/:documentId with all attributes that where used at creation except the
   * required one.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "user", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("category")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttribute categoryAttribute = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("category").stringValue("test"));
      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(categoryAttribute);
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().path("asd.txt").addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("user").stringValue("1234")));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("asd.txt", document.getPath());
    }
  }

  /**
   * PATCH /documents after site schema is applied and with invalid attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().addRequiredItem(createRequired("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("asd.txt");

      // when
      try {
        this.documentsApi.updateDocument(documentId, updateReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"strings\","
            + "\"error\":\"missing required attribute 'strings'\"}]}", e.getResponseBody());
      }
    }
  }
}
