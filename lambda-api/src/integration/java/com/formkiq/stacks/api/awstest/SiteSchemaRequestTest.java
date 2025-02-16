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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integation tests for.
 * <p>
 * GET /sites/{siteId}/schema/document
 * </p>
 * <p>
 * PUT /sites/{siteId}/schema/document
 * </p>
 */
public class SiteSchemaRequestTest extends AbstractAwsIntegrationTest {

  /**
   * GET /sites/{siteId}/schema/document, not set.
   *
   */
  @Test
  public void testGetSitesSchema01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      List<ApiClient> apiClients = getApiClients(siteId);
      for (ApiClient apiClient : apiClients) {

        SchemasApi schemasApi = new SchemasApi(apiClient);

        // when
        try {
          schemasApi.getSitesSchema(siteId, null);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals("{\"message\":\"Sites Schema not found\"}", e.getResponseBody());
        }
      }
    }
  }

  /**
   * PUT /sites/{siteId}/schema/document, not set.
   *
   */
  @Test
  public void testPutSitesSchema01() throws ApiException {
    // given
    String siteId = ID.uuid();

    List<ApiClient> apiClients = getApiClients(siteId);
    for (ApiClient apiClient : apiClients) {

      String key = "category_" + UUID.randomUUID();

      AttributesApi attributesApi = new AttributesApi(apiClient);
      AddAttributeRequest addReq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      attributesApi.addAttribute(addReq, siteId);

      SchemasApi schemasApi = new SchemasApi(apiClient);
      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("test").attributes(
          new SchemaAttributes().addRequiredItem(new AttributeSchemaRequired().attributeKey(key)));

      // when
      SetResponse setResponse = schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", setResponse.getMessage());
    }
  }
}
