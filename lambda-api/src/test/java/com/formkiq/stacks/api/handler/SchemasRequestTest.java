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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites/{siteId}/schema/document. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class SchemasRequestTest extends AbstractApiClientRequestTest {

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
}
