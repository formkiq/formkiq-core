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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.AddMappingResponse;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetMappingsResponse;
import com.formkiq.client.model.Mapping;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.client.model.SetMappingRequest;
import com.formkiq.client.model.SetResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /mappings. */
public class MappingsRequestTest extends AbstractApiClientRequestTest {

  /** SiteId. */
  private static final String SITE_ID = UUID.randomUUID().toString();

  /**
   * POST /mappings empty body.
   *
   */
  @Test
  public void testAddMappings01() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddMappingRequest req = new AddMappingRequest();

      // when
      try {
        this.mappingsApi.addMapping(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"invalid request body\"}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /mappings empty body.
   *
   */
  @Test
  public void testAddMappings02() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddMappingRequest req = new AddMappingRequest().mapping(new AddMapping());

      // when
      try {
        this.mappingsApi.addMapping(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"},"
                + "{\"key\":\"attributes\",\"error\":\"'attributes' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /mappings.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddMappings03() throws ApiException {
    // given
    final String key0 = "invoice";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, key0);

      AddMapping mapping = new AddMapping().name("test")
          .addAttributesItem(new MappingAttribute().attributeKey(key0)
              .sourceType(MappingAttributeSourceType.CONTENT)
              .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
              .labelTexts(List.of("invoice")));

      AddMappingRequest req = new AddMappingRequest().mapping(mapping);

      // when
      AddMappingResponse addResponse = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(addResponse.getMappingId());

      GetMappingsResponse response = this.mappingsApi.getMappings(siteId, null, null);
      assertEquals(1, notNull(response.getMappings()).size());
      assertEquals("test", response.getMappings().get(0).getName());
      assertEquals("", response.getMappings().get(0).getDescription());
      validateAddMappingsAttributes03(response.getMappings().get(0));

      // given
      mapping.setName("another");
      mapping.setDescription("test desc");
      SetMappingRequest setReq = new SetMappingRequest().mapping(mapping);

      // when
      SetResponse setResponse =
          this.mappingsApi.setMapping(addResponse.getMappingId(), setReq, siteId);

      // then
      assertEquals("Mapping set", setResponse.getMessage());

      response = this.mappingsApi.getMappings(siteId, null, null);
      assertEquals(1, notNull(response.getMappings()).size());
      assertEquals("another", response.getMappings().get(0).getName());
      assertEquals("test desc", response.getMappings().get(0).getDescription());
      validateAddMappingsAttributes03(response.getMappings().get(0));

      Mapping m = this.mappingsApi.getMapping(addResponse.getMappingId(), siteId).getMapping();
      assertNotNull(m);
      assertEquals("another", m.getName());
      assertEquals("test desc", m.getDescription());
      validateAddMappingsAttributes03(m);
    }
  }

  /**
   * DELETE /mappings/{mappingId}.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteMappings01() throws ApiException {
    // given
    final String key0 = "invoice";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId, key0);

      AddMapping mapping = new AddMapping().name("test")
          .addAttributesItem(new MappingAttribute().attributeKey(key0)
              .sourceType(MappingAttributeSourceType.CONTENT)
              .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
              .labelTexts(List.of("invoice")));

      AddMappingRequest req = new AddMappingRequest().mapping(mapping);
      AddMappingResponse addResponse = this.mappingsApi.addMapping(req, siteId);
      assertNotNull(this.mappingsApi.getMapping(addResponse.getMappingId(), siteId));

      // when
      DeleteResponse deleteResponse =
          this.mappingsApi.deleteMapping(addResponse.getMappingId(), siteId);

      // then
      assertEquals("Mapping '" + addResponse.getMappingId() + "' deleted",
          deleteResponse.getMessage());

      try {
        this.mappingsApi.getMapping(addResponse.getMappingId(), siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Mapping '" + addResponse.getMappingId() + "' not found\"}",
            e.getResponseBody());
      }

      try {
        this.mappingsApi.deleteMapping(addResponse.getMappingId(), siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Mapping '" + addResponse.getMappingId() + "' not found\"}",
            e.getResponseBody());
      }
    }
  }

  private static void validateAddMappingsAttributes03(final Mapping mapping) {
    assertEquals(1, notNull(mapping.getAttributes()).size());
    assertEquals("invoice", notNull(mapping.getAttributes()).get(0).getAttributeKey());
    assertEquals(MappingAttributeSourceType.CONTENT,
        notNull(mapping.getAttributes()).get(0).getSourceType());
    assertEquals(MappingAttributeLabelMatchingType.CONTAINS,
        notNull(mapping.getAttributes()).get(0).getLabelMatchingType());
    assertEquals("invoice",
        String.join(",", notNull(notNull(mapping.getAttributes()).get(0).getLabelTexts())));
  }

  private void addAttribute(final String siteId, final String key) throws ApiException {
    attributesApi.addAttribute(new AddAttributeRequest().attribute(new AddAttribute().key(key)),
        siteId);
  }
}
