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
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddLocaleRequest;
import com.formkiq.client.model.AddLocaleResourceInterfaceItem;
import com.formkiq.client.model.AddLocaleResourceItemRequest;
import com.formkiq.client.model.AddLocaleResourceItemResponse;
import com.formkiq.client.model.AddLocaleResourceSchemaItem;
import com.formkiq.client.model.AddResourceItem;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.LocaleResourceType;
import com.formkiq.client.model.ResourceItem;
import com.formkiq.client.model.SetLocaleResourceItemRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  void testGetSitesSchema01() throws ApiException {
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
   * PUT /sites/{siteId}/schema/document.
   *
   */
  @Test
  void testPutSitesSchema01() throws ApiException {
    // given
    String siteId = ID.uuid();
    final String locale = "en";

    List<ApiClient> apiClients = getApiClients(siteId);
    for (ApiClient apiClient : apiClients) {

      String key = "category_" + UUID.randomUUID();

      AttributesApi attributesApi = new AttributesApi(apiClient);
      AddAttributeRequest addReq = new AddAttributeRequest().attribute(new AddAttribute().key(key));
      attributesApi.addAttribute(addReq, siteId);

      SchemasApi schemasApi = new SchemasApi(apiClient);
      AddAttributeSchemaRequired item = new AddAttributeSchemaRequired().attributeKey(key)
          .allowedValues(List.of("111", "222", "333"));
      SetSitesSchemaRequest req = new SetSitesSchemaRequest().name("test")
          .attributes(new SetSchemaAttributes().addRequiredItem(item));

      // when
      SetResponse setResponse = schemasApi.setSitesSchema(siteId, req);

      // then
      assertEquals("Sites Schema set", setResponse.getMessage());

      // given
      SystemManagementApi api = new SystemManagementApi(apiClient);

      api.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceSchemaItem aitem = new AddLocaleResourceSchemaItem().localizedValue("777")
          .allowedValue("222").itemType(LocaleResourceType.SCHEMA).attributeKey(key);
      AddLocaleResourceItemRequest addResourceReq =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(aitem));

      // when
      api.addLocaleResourceItem(siteId, locale, addResourceReq);

      // then
      GetSitesSchemaResponse sitesSchema = schemasApi.getSitesSchema(siteId, locale);
      assertNotNull(sitesSchema);
      assertNotNull(sitesSchema.getAttributes());
      List<AttributeSchemaRequired> required = notNull(sitesSchema.getAttributes().getRequired());
      assertEquals(1, required.size());
      assertEquals(1, notNull(required.get(0).getLocalizedAllowedValues()).size());
      assertEquals("777", notNull(required.get(0).getLocalizedAllowedValues()).get("222"));

      sitesSchema = schemasApi.getSitesSchema(siteId, null);
      assertNotNull(sitesSchema);
      assertNotNull(sitesSchema.getAttributes());
      required = notNull(sitesSchema.getAttributes().getRequired());
      assertEquals(1, required.size());
      assertEquals(0, notNull(required.get(0).getLocalizedAllowedValues()).size());
    }
  }

  /**
   * Add AddLocaleResourceInterfaceItem.
   *
   * POST /sites/{siteId}/locales/{locale}/resourceItems GET
   * /sites/{siteId}/locales/{locale}/resourceItems DELETE
   * /sites/{siteId}/locales/{locale}/resourceItems/{itemKey} GET
   * /sites/{siteId}/locales/{locale}/resourceItems/{itemKey}
   * 
   * @throws ApiException ApiException
   */
  @Test
  void addResourceItem01() throws ApiException {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      List<ApiClient> apiClients = getApiClients(siteId);
      for (ApiClient apiClient : apiClients) {

        String key = ID.uuid();
        SystemManagementApi api = new SystemManagementApi(apiClient);
        api.addLocale(siteId, new AddLocaleRequest().locale(locale));

        AddLocaleResourceInterfaceItem item = new AddLocaleResourceInterfaceItem()
            .itemType(LocaleResourceType.INTERFACE).interfaceKey(key).localizedValue("123");
        AddLocaleResourceItemRequest addReq =
            new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

        // when
        AddLocaleResourceItemResponse response = api.addLocaleResourceItem(siteId, locale, addReq);

        // then
        assertEquals("INTERFACE#" + key, response.getItemKey());
        assertFalse(getResourceItems(api, siteId, "en").isEmpty());
        assertTrue(getResourceItems(api, siteId, "fr").isEmpty());

        assertInterfaceResourceItem(getResourceItem(api, siteId, "en", response.getItemKey()), key,
            "123");

        // given
        item.setLocalizedValue("555");
        SetLocaleResourceItemRequest setReq =
            new SetLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

        // when
        api.setLocaleResourceItem(siteId, "en", response.getItemKey(), setReq);

        // then
        assertInterfaceResourceItem(getResourceItem(api, siteId, "en", response.getItemKey()), key,
            "555");

        // when
        api.deleteLocaleResourceItem(siteId, "en", response.getItemKey());

        // then
        try {
          getResourceItem(api, siteId, "en", response.getItemKey());
          fail();
        } catch (ApiException e) {
          assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
          assertEquals("{\"message\":\"ItemKey '" + response.getItemKey() + "' not found\"}",
              e.getResponseBody());
        }
      }
    }
  }

  private void assertInterfaceResourceItem(final ResourceItem item, final String interfaceKey,
      final String value) {
    assertNotNull(item.getItemKey());
    assertEquals(interfaceKey, item.getInterfaceKey());
    assertEquals(value, item.getLocalizedValue());
  }

  private static ResourceItem getResourceItem(final SystemManagementApi api, final String siteId,
      final String locale, final String itemKey) throws ApiException {
    return api.getLocaleResourceItem(siteId, locale, itemKey).getResourceItem();
  }

  private static List<ResourceItem> getResourceItems(final SystemManagementApi api,
      final String siteId, final String locale) throws ApiException {
    return notNull(api.getLocaleResourceItems(siteId, locale, null, null).getResourceItems());
  }
}
