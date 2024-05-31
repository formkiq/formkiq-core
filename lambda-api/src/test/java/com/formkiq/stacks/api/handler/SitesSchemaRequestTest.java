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

import static com.formkiq.aws.dynamodb.objects.Objects.formatDouble;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
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
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SearchRangeDataType;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites/{siteId}/schema/document. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class SitesSchemaRequestTest extends AbstractApiClientRequestTest {

  private void addAttribute(final String siteId, final String key, final AttributeDataType dataType)
      throws ApiException {
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(key).dataType(dataType));
    this.attributesApi.addAttribute(req, siteId);
  }

  private void createRangeAttributeNumber(final String siteId, final String value)
      throws ApiException {
    AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
        .addAttributesItem(new AddDocumentAttribute().key("category").stringValue("person"))
        .addAttributesItem(
            new AddDocumentAttribute().key("date").numberValue(new BigDecimal(value)));

    this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);
  }

  private void createRangeAttributeString(final String siteId, final String value)
      throws ApiException {
    AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
        .addAttributesItem(new AddDocumentAttribute().key("category").stringValue("person"))
        .addAttributesItem(new AddDocumentAttribute().key("date").stringValue(value));

    this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);
  }

  /**
   * POST /documents Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocument01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd")
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("category"));

      // when
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      // then
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals("strings", attributes.get(0).getKey());
      assertEquals("category", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
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
   * POST /documents/{documentId}/attributes. Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttributesRequest attrReq = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("category"));

      // when
      AddResponse response =
          this.documentAttributesApi.addDocumentAttributes(documentId, attrReq, siteId, null);

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals("strings", attributes.get(0).getKey());
      assertEquals("category", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "other", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentAttributesRequest attrReq = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("other").stringValue("category"));

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
   * POST /documents/upload with site schema required attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String key = "category";
      addAttribute(siteId, key, null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key)));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(
                  new AttributeSchemaRequired().attributeKey("category").defaultValue("person"))
              .addRequiredItem(
                  new AttributeSchemaRequired().attributeKey("flag").defaultValue("true"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("keyonly"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("num")
                  .defaultValues(Arrays.asList("123", "233")))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")
                  .defaultValues(Arrays.asList("abc", "qwe"))));
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

        GetDocumentAttributesResponse attributes =
            this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

        int i = 0;
        final int expected = 5;
        assertEquals(expected, attributes.getAttributes().size());
        assertEquals("category", attributes.getAttributes().get(i).getKey());
        assertEquals("person", attributes.getAttributes().get(i++).getStringValue());

        assertEquals("flag", attributes.getAttributes().get(i).getKey());
        assertTrue(attributes.getAttributes().get(i++).getBooleanValue());

        assertEquals("keyonly", attributes.getAttributes().get(i).getKey());
        assertNull(attributes.getAttributes().get(i++).getStringValue());

        assertEquals("num", attributes.getAttributes().get(i).getKey());
        assertEquals("123,233", String.join(",", attributes.getAttributes().get(i++)
            .getNumberValues().stream().map(n -> formatDouble(n.doubleValue())).toList()));

        assertEquals("strings", attributes.getAttributes().get(i).getKey());
        assertEquals("abc,qwe",
            String.join(",", attributes.getAttributes().get(i++).getStringValues()));
      }
    }
  }

  /**
   * POST /documents/upload with site schema required attribute and no default value.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema03() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("flag"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("keyonly"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("num")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq = new AddDocumentUploadRequest().path("sample.txt")
          .contentType("text/plain")
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("111", "222")))
          .addAttributesItem(new AddDocumentAttribute().key("flag").booleanValue(Boolean.TRUE))
          .addAttributesItem(new AddDocumentAttribute().key("num")
              .numberValues(Arrays.asList(new BigDecimal("111.11"), new BigDecimal("222.22"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq, siteId, null, null, null);

      // then
      String documentId = response.getDocumentId();
      GetDocumentAttributesResponse attributes =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      int i = 0;
      final int expected = 4;
      assertEquals(expected, attributes.getAttributes().size());

      assertEquals("flag", attributes.getAttributes().get(i).getKey());
      assertTrue(attributes.getAttributes().get(i++).getBooleanValue());

      assertEquals("keyonly", attributes.getAttributes().get(i).getKey());
      assertNull(attributes.getAttributes().get(i++).getStringValue());

      assertEquals("num", attributes.getAttributes().get(i).getKey());
      assertEquals("111.11,222.22", String.join(",", attributes.getAttributes().get(i++)
          .getNumberValues().stream().map(n -> formatDouble(n.doubleValue())).toList()));

      assertEquals("strings", attributes.getAttributes().get(i).getKey());
      assertEquals("111,222",
          String.join(",", attributes.getAttributes().get(i++).getStringValues()));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "flag", AttributeDataType.BOOLEAN);
      addAttribute(siteId, "keyonly", AttributeDataType.KEY_ONLY);
      addAttribute(siteId, "num", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")
                  .addAllowedValuesItem("111").addAllowedValuesItem("222"))
              .addRequiredItem(
                  new AttributeSchemaRequired().attributeKey("flag").addAllowedValuesItem("true"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("keyonly"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("num")
                  .addAllowedValuesItem("111.11").addAllowedValuesItem("222.22")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("111", "222")))
          .addAttributesItem(new AddDocumentAttribute().key("flag").booleanValue(Boolean.TRUE))
          .addAttributesItem(new AddDocumentAttribute().key("num")
              .numberValues(Arrays.asList(new BigDecimal("111.11"), new BigDecimal("222.22"))));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      String documentId = response.getDocumentId();
      GetDocumentAttributesResponse attributes =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);
      final int expected = 4;
      assertEquals(expected, attributes.getAttributes().size());

      // given
      AddDocumentUploadRequest ureq1 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValues(List.of("333")))
          .addAttributesItem(new AddDocumentAttribute().key("flag").booleanValue(Boolean.FALSE))
          .addAttributesItem(
              new AddDocumentAttribute().key("num").addNumberValuesItem(new BigDecimal("111.12")));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("strings")
                  .allowedValues(List.of("123")))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("category")
                  .allowedValues(List.of("person"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(List.of("123")));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // given
      AddDocumentUploadRequest ureq1 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(List.of("333")));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("strings"))
              .allowAdditionalAttributes(Boolean.TRUE));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("123")));

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
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddUploadDocumentWithSetSitesSchema07() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("strings"))
              .allowAdditionalAttributes(Boolean.FALSE));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("123")));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").addStringValuesItem("1"));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").addStringValuesItem("111"));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response =
          this.documentAttributesApi.deleteDocumentAttribute(documentId, "strings", siteId);

      // then
      assertEquals("attribute 'strings' removed from document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").addStringValuesItem("1"));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").addStringValuesItem("111"));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "strings", "111", siteId);

      // then
      assertEquals(
          "attribute value '111' removed from attribute 'strings', document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").addStringValuesItem("111"));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      // when
      DeleteResponse response = this.documentAttributesApi
          .deleteDocumentAttributeAndValue(documentId, "strings", "111", siteId);

      // then
      assertEquals(
          "attribute value '111' removed from attribute 'strings', document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      // when
      try {
        this.schemasApi.getSitesSchema(siteId, null);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals(attributeKey, attributes.get(0).getKey());
      assertEquals("123", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("category").stringValue("person"));
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributeRequest setReq = new SetDocumentAttributeRequest()
          .attribute(new AddDocumentAttributeValue().addStringValuesItem("123"));

      // when
      SetResponse response = this.documentAttributesApi.setDocumentAttributeValue(documentId,
          "category", setReq, siteId);

      // then
      assertEquals("Updated attribute 'category' on document '" + documentId + "'",
          response.getMessage());
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals("category", attributes.get(0).getKey());
      assertEquals("123", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("person"));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("person"));
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
      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals("strings", attributes.get(0).getKey());
      assertEquals("4444", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("strings", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("person")))
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("111", "222")));

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
      assertEquals(1, response0.getDocuments().size());

      // invalid search
      DocumentSearchRequest sreq1 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("333"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      DocumentSearchResponse response1 =
          this.searchApi.documentSearch(sreq1, siteId, null, null, null);
      assertEquals(0, response1.getDocuments().size());

      // wrong attribute order
      DocumentSearchRequest sreq2 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person"))
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222")));

      DocumentSearchResponse response2 =
          this.searchApi.documentSearch(sreq2, siteId, null, null, null);
      assertEquals(1, response2.getDocuments().size());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("strings", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("person")))
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("111", "222")));

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
      assertEquals(1, response0.getDocuments().size());

      // invalid search
      DocumentSearchRequest sreq1 = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("strings").eq("222"))
          .addAttributesItem(new DocumentSearchAttribute().key("category").beginsWith("a")));

      DocumentSearchResponse response1 =
          this.searchApi.documentSearch(sreq1, siteId, null, null, null);
      assertEquals(0, response1.getDocuments().size());

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("date"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("category", "date"))));
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
      assertEquals(expected, response0.getDocuments().size());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#2024-01-04",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#2024-01-05",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#2024-01-10",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());

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

  /**
   * Search document by strict composite key using POST /documents/upload String wrong order RANGE.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey04() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("date"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("category", "date"))));
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "date", AttributeDataType.NUMBER);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("date"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("category", "date"))));
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
      assertEquals(expected, response0.getDocuments().size());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#000000000000100.1200",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#000000000000150.1400",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());

      assertEquals("category#date", response0.getDocuments().get(i).getMatchedAttribute().getKey());
      assertEquals("person#000000000000200.0000",
          response0.getDocuments().get(i++).getMatchedAttribute().getStringValue());
    }
  }

  /**
   * Search document by strict composite key using POST /documents/upload number EQOR.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testSearchCompositeKey06() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("strings", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("person")))
          .addAttributesItem(new AddDocumentAttribute().key("strings")
              .stringValues(Arrays.asList("111", "222", "333")));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("strings", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("person")))
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("111", "222")));

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
      assertEquals(1, response0.getDocuments().size());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "category", null);
      addAttribute(siteId, "other", null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.FALSE)
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings"))
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("strings", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      req = new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
          .allowAdditionalAttributes(Boolean.FALSE)
          .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
          .addRequiredItem(new AttributeSchemaRequired().attributeKey("other"))
          .addCompositeKeysItem(
              new AttributeSchemaCompositeKey().attributeKeys(Arrays.asList("other", "category"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt")
          .addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(List.of("person")))
          .addAttributesItem(
              new AddDocumentAttribute().key("other").stringValues(Arrays.asList("111", "222")));

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
   * PUT /documents/{documentId}/attributes. Add attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetDocumentAttribute01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributesRequest attrReq = new SetDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("category"));

      // when
      SetResponse response =
          this.documentAttributesApi.setDocumentAttributes(documentId, attrReq, siteId);

      // then
      assertEquals("set attributes on documentId '" + documentId + "'", response.getMessage());

      List<DocumentAttribute> attributes = this.documentAttributesApi
          .getDocumentAttributes(documentId, siteId, null, null).getAttributes();
      assertEquals(1, attributes.size());
      assertEquals("strings", attributes.get(0).getKey());
      assertEquals("category", attributes.get(0).getStringValue());
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);
      addAttribute(siteId, "other", null);

      AddDocumentRequest areq = new AddDocumentRequest().content("adasd");
      String documentId = this.documentsApi.addDocument(areq, siteId, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      SetDocumentAttributesRequest attrReq = new SetDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute().key("other").stringValue("category"));

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
   * PUT /sites/{siteId}/schema/document with required attribute.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String key = "category";
      addAttribute(siteId, key, null);

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key)));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      GetSitesSchemaResponse schema = this.schemasApi.getSitesSchema(siteId, null);
      assertEquals("joe", schema.getName());
      assertEquals(1, schema.getVersion().intValue());
      assertEquals(key, schema.getAttributes().getRequired().get(0).getAttributeKey());
      assertTrue(schema.getAttributes().getRequired().get(0).getAllowedValues().isEmpty());

      req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes().addRequiredItem(
              new AttributeSchemaRequired().attributeKey(key).addAllowedValuesItem("123")));

      // when
      response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      schema = this.schemasApi.getSitesSchema(siteId, null);
      assertEquals("joe", schema.getName());
      assertEquals(2, schema.getVersion().intValue());
      assertEquals(key, schema.getAttributes().getRequired().get(0).getAttributeKey());
      assertEquals("123",
          String.join(",", schema.getAttributes().getRequired().get(0).getAllowedValues()));

      schema = this.schemasApi.getSitesSchema(siteId, "1");
      assertEquals("joe", schema.getName());
      assertEquals(1, schema.getVersion().intValue());
      assertEquals(key, schema.getAttributes().getRequired().get(0).getAttributeKey());
      assertTrue(schema.getAttributes().getRequired().get(0).getAllowedValues().isEmpty());
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document. Empty.
   *
   */
  @Test
  public void testSetSitesSchema02() {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
        assertEquals(
            "{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"},"
                + "{\"error\":\"either 'required' or 'optional' attributes list is required\"}]}",
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      String key = "category";

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe").attributes(
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key)));

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
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema04() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("category"))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("test")));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("test")));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", response.getMessage());

      GetSitesSchemaResponse schema = this.schemasApi.getSitesSchema(siteId, null);
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes().allowAdditionalAttributes(Boolean.TRUE)
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("category", "test", "other")))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("test")));

      // when
      SetResponse response = this.schemasApi.setSitesSchema(siteId, req);
      // then
      assertEquals("Sites Schema set", response.getMessage());

      // given
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
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema07() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
   * PATCH /documents after site schema is applied and without attributes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testUpdateDocument01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("asd.txt");

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
  public void testUpdateDocument02() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "category", null);
      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("asd.txt")
          .addAttributesItem(new AddDocumentAttribute().key("category").stringValue("test"));

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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      addAttribute(siteId, "strings", null);

      AddDocumentUploadRequest ureq0 = new AddDocumentUploadRequest().path("sample.txt");
      String documentId =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null).getDocumentId();

      SetSitesSchemaRequest req =
          new SetSitesSchemaRequest().name("joe").attributes(new SchemaAttributes()
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("strings")));
      this.schemasApi.setSitesSchema(siteId, req);

      UpdateDocumentRequest updateReq = new UpdateDocumentRequest().path("asd.txt")
          .addAttributesItem(new AddDocumentAttribute().key("strings").stringValue("test"));

      // when
      this.documentsApi.updateDocument(documentId, updateReq, siteId, null);

      // then
      GetDocumentResponse document = this.documentsApi.getDocument(documentId, siteId, null);
      assertEquals("asd.txt", document.getPath());
    }
  }
}

// DELETE /documents/{documentId}/attributes/{attributeKey}/{attributeValue} - required attribute,
// single value

// PATCH /documents after sites schema is updated?? What happens? remains tied to version it was
// created by
// reindex...???
