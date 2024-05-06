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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.GetDocumentAttributesResponse;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
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

      GetSitesSchemaResponse schema = this.schemasApi.getSitesSchema(siteId);
      assertEquals("joe", schema.getName());
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document. Empty.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema02() throws ApiException {
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
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetSitesSchema03() throws ApiException {
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
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (String key : Arrays.asList("category", "test")) {
        addAttribute(siteId, key, null);
      }

      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("joe")
          .attributes(new SchemaAttributes()
              .addCompositeKeysItem(new AttributeSchemaCompositeKey()
                  .attributeKeys(Arrays.asList("category", "test", "other")))
              .addRequiredItem(new AttributeSchemaRequired().attributeKey("category"))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("test")));

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
   * GET /sites/{siteId}/schema/document, not set.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testGetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

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
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq, siteId, null, null, null);

      // then
      String documentId = response.getDocumentId();
      GetDocumentAttributesResponse attributes =
          this.documentAttributesApi.getDocumentAttributes(documentId, siteId, null, null);

      int i = 0;
      final int expected = 5;
      assertEquals(expected, attributes.getAttributes().size());
      assertEquals("category", attributes.getAttributes().get(i).getKey());
      assertEquals("person", attributes.getAttributes().get(i++).getStringValue());

      assertEquals("flag", attributes.getAttributes().get(i).getKey());
      assertTrue(attributes.getAttributes().get(i++).getBooleanValue().booleanValue());

      assertEquals("keyonly", attributes.getAttributes().get(i).getKey());
      assertNull(attributes.getAttributes().get(i++).getStringValue());

      assertEquals("num", attributes.getAttributes().get(i).getKey());
      assertEquals("123,233", String.join(",", attributes.getAttributes().get(i++).getNumberValues()
          .stream().map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList()));

      assertEquals("strings", attributes.getAttributes().get(i).getKey());
      assertEquals("abc,qwe",
          String.join(",", attributes.getAttributes().get(i++).getStringValues()));
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
      assertTrue(attributes.getAttributes().get(i++).getBooleanValue().booleanValue());

      assertEquals("keyonly", attributes.getAttributes().get(i).getKey());
      assertNull(attributes.getAttributes().get(i++).getStringValue());

      assertEquals("num", attributes.getAttributes().get(i).getKey());
      assertEquals("111.11,222.22",
          String.join(",", attributes.getAttributes().get(i++).getNumberValues().stream()
              .map(n -> formatDouble(Double.valueOf(n.doubleValue()))).toList()));

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
          .addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("333")))
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
                  .allowedValues(Arrays.asList("123")))
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("category")
                  .allowedValues(Arrays.asList("person"))));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("123")));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // given
      AddDocumentUploadRequest ureq1 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("strings").stringValues(Arrays.asList("333")));

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
              new AddDocumentAttribute().key("category").stringValues(Arrays.asList("123")));

      // when
      GetDocumentUrlResponse response =
          this.documentsApi.addDocumentUpload(ureq0, siteId, null, null, null);

      // then
      assertNotNull(response.getDocumentId());
    }
  }

  /**
   * POST /documents/upload with site schema. Add attributes not in required / optional /
   * allowAdditionalAttributes = false. values.
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
          .attributes(new SchemaAttributes()
              .addOptionalItem(new AttributeSchemaOptional().attributeKey("strings"))
              .allowAdditionalAttributes(Boolean.FALSE));
      this.schemasApi.setSitesSchema(siteId, req);

      AddDocumentUploadRequest ureq0 =
          new AddDocumentUploadRequest().path("sample.txt").addAttributesItem(
              new AddDocumentAttribute().key("category").stringValues(Arrays.asList("123")));

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

  // search composite keys
  // test POST /documents & PATCH /documents ??
}
