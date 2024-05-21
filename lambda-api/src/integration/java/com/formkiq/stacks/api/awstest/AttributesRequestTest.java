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
package com.formkiq.stacks.api.awstest;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeResponse;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration Test for URLS:
 * <p>
 * GET /attributes
 * </p>
 * <p>
 * POST /attributes
 * </p>
 * <p>
 * GET /attributes/{attributeKey}
 * </p>
 * <p>
 * DELETE /attributes/{attributeKey}
 * </p>
 */
public class AttributesRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;
  /** Site Id. */
  private static final String SITE_ID = UUID.randomUUID().toString();

  /**
   * Add Attribute missing key.
   * 
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void addAttributesRequest01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      List<ApiClient> apiClients = getApiClients(siteId);

      for (ApiClient apiClient : apiClients) {
        AttributesApi attributesApi = new AttributesApi(apiClient);

        // when
        try {
          attributesApi.addAttribute(new AddAttributeRequest().attribute(new AddAttribute()),
              siteId);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals("{\"errors\":[{\"key\":\"key\",\"error\":\"'key' is required\"}]}",
              e.getResponseBody());
        }
      }
    }
  }

  /**
   * Add String Attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void addAttributesRequest02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      List<ApiClient> apiClients = getApiClients(siteId);

      for (ApiClient apiClient : apiClients) {
        AttributesApi attributesApi = new AttributesApi(apiClient);

        String key = "category_" + UUID.randomUUID();
        AddAttribute attribute = new AddAttribute().key(key);

        // when
        AddAttributeResponse response =
            attributesApi.addAttribute(new AddAttributeRequest().attribute(attribute), siteId);

        // then
        assertEquals("Attribute '" + key + "' created", response.getMessage());

        Attribute attribute1 = attributesApi.getAttribute(key, siteId).getAttribute();

        assert attribute1 != null;
        assertEquals(key, attribute1.getKey());
        assertEquals(AttributeType.STANDARD, attribute1.getType());
        assertEquals(AttributeDataType.STRING, attribute1.getDataType());
        assertEquals(Boolean.FALSE, attribute1.getInUse());

        List<Attribute> attributes =
            attributesApi.getAttributes(siteId, null, "100").getAttributes();
        assertFalse(notNull(attributes).isEmpty());

        Attribute attribute2 = Objects.requireNonNull(attributes).get(0);
        assertNotNull(attribute2.getKey());
        assertNotNull(attribute2.getType());
        assertNotNull(attribute2.getDataType());
        assertNotNull(attribute2.getInUse());
      }
    }
  }

  /**
   * Add Number/OPA Attribute and test DELETE /attribute/{attributeKey}.
   *
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void addAttributesRequest03() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      List<ApiClient> apiClients = getApiClients(siteId);

      for (ApiClient apiClient : apiClients) {
        AttributesApi attributesApi = new AttributesApi(apiClient);

        String key = "category_" + UUID.randomUUID();
        AddAttribute attribute =
            new AddAttribute().key(key).type(AttributeType.OPA).dataType(AttributeDataType.NUMBER);

        // when
        AddAttributeResponse response =
            attributesApi.addAttribute(new AddAttributeRequest().attribute(attribute), siteId);

        // then
        assertEquals("Attribute '" + key + "' created", response.getMessage());

        Attribute attribute1 = attributesApi.getAttribute(key, siteId).getAttribute();
        assertNotNull(attribute1);
        assertEquals(key, attribute1.getKey());
        assertEquals(AttributeType.OPA, attribute1.getType());
        assertEquals(AttributeDataType.NUMBER, attribute1.getDataType());
        assertEquals(Boolean.FALSE, attribute1.getInUse());

        // when
        DeleteResponse deleteResponse = attributesApi.deleteAttribute(key, siteId);

        // then
        assertEquals("Attribute '" + key + "' deleted", deleteResponse.getMessage());
        try {
          attributesApi.getAttribute(key, siteId).getAttribute();
        } catch (ApiException e) {
          assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
          assertEquals("{\"message\":\"Attribute " + key + " not found\"}", e.getResponseBody());
        }
      }
    }
  }
}
