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
import com.formkiq.client.model.AddAttributeSchemaOptional;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.AddClassificationRequest;
import com.formkiq.client.model.AddLocaleRequest;
import com.formkiq.client.model.AddLocaleResourceClassificationItem;
import com.formkiq.client.model.AddLocaleResourceInterfaceItem;
import com.formkiq.client.model.AddLocaleResourceItemRequest;
import com.formkiq.client.model.AddLocaleResourceItemResponse;
import com.formkiq.client.model.AddLocaleResourceSchemaItem;
import com.formkiq.client.model.AddResourceItem;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.AttributeSchemaRequired;
import com.formkiq.client.model.Classification;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetAttributeAllowedValuesResponse;
import com.formkiq.client.model.GetLocaleResourceItemResponse;
import com.formkiq.client.model.GetSitesSchemaResponse;
import com.formkiq.client.model.Locale;
import com.formkiq.client.model.LocaleResourceType;
import com.formkiq.client.model.ResourceItem;
import com.formkiq.client.model.SetLocaleResourceItemRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /sites/{siteId}/locales/{locale}. */
public class SitesLocaleResourceItemRequestHandlerTest extends AbstractApiClientRequestTest {

  private static void assertResourceInterface(final ResourceItem item, final String interfaceKey,
      final String localizedValue, final String itemKey) {
    assertEquals(LocaleResourceType.INTERFACE, item.getItemType());
    assertEquals(interfaceKey, item.getInterfaceKey());
    assertEquals(localizedValue, item.getLocalizedValue());
    assertEquals(itemKey, item.getItemKey());
  }

  private static void assertResourceSchema(final ResourceItem item,
      final LocaleResourceType resourceType, final String attributeKey, final String allowedValue,
      final String itemKey) {
    assertEquals(resourceType, item.getItemType());
    assertEquals(attributeKey, item.getAttributeKey());
    assertEquals(allowedValue, item.getAllowedValue());
    assertEquals(itemKey, item.getItemKey());
  }

  /**
   * Post /sites/{siteId}/locales.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddLocale01() throws Exception {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      // when
      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      // then
      List<Locale> locales = notNull(this.systemApi.getLocales(siteId, null, null).getLocales());
      assertEquals(1, locales.size());
      assertEquals("en", locales.get(0).getLocale());

      // when
      DeleteResponse deleteResponse = this.systemApi.deleteLocale(siteId, locale);

      // then
      assertEquals("deleted locale 'en'", deleteResponse.getMessage());
      locales = notNull(this.systemApi.getLocales(siteId, null, null).getLocales());
      assertEquals(0, locales.size());
    }
  }

  /**
   * DELETE /sites/{siteId}/locales.
   *
   */
  @Test
  public void testDeleteLocale01() {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      // when
      try {
        this.systemApi.deleteLocale(siteId, locale);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"locale\"," + "\"error\":\"Locale 'en' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddLocaleResourceItem01() throws Exception {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceInterfaceItem item = new AddLocaleResourceInterfaceItem()
          .interfaceKey("mykey").itemType(LocaleResourceType.INTERFACE).localizedValue("bbb");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      AddLocaleResourceItemResponse response =
          this.systemApi.addLocaleResourceItem(siteId, locale, req);

      // then
      assertEquals("INTERFACE##mykey", response.getItemKey());

      List<ResourceItem> items = notNull(
          this.systemApi.getLocaleResourceItems(siteId, locale, null, null).getResourceItems());
      assertEquals(1, items.size());
      assertResourceInterface(items.get(0), "mykey", "bbb", "INTERFACE##mykey");

      GetLocaleResourceItemResponse lri =
          this.systemApi.getLocaleResourceItem(siteId, locale, response.getItemKey());
      assertNotNull(lri);
      assertNotNull(lri.getResourceItem());
      assertResourceInterface(lri.getResourceItem(), "mykey", "bbb", "INTERFACE##mykey");

      // when
      try {
        this.systemApi.deleteLocale(siteId, locale);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"locale\","
                + "\"error\":\"Locale Item Resources found for Locale 'en'\"}]}",
            e.getResponseBody());
      }

      // when
      this.systemApi.deleteLocaleResourceItem(siteId, locale, response.getItemKey());

      // then
      try {
        this.systemApi.getLocaleResourceItem(siteId, locale, response.getItemKey());
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"ItemKey 'INTERFACE##mykey' not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems.
   *
   */
  @Test
  public void testAddLocaleResourceItem02() throws ApiException {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceInterfaceItem item =
          new AddLocaleResourceInterfaceItem().itemType(LocaleResourceType.INTERFACE);
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"localizedValue\","
                + "\"error\":\"'localizedValue' is required\"},"
                + "{\"key\":\"interfaceKey\",\"error\":\"'interfaceKey' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems with attribute missing.
   *
   */
  @Test
  public void testAddLocaleResourceItem03() throws ApiException {
    // given
    String locale = "en";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceSchemaItem item =
          new AddLocaleResourceSchemaItem().itemType(LocaleResourceType.SCHEMA).attributeKey("abc")
              .allowedValue("111").localizedValue("222");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"abc\",\"error\":\"missing attribute 'abc'\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems with attribute invalid allowed value.
   *
   */
  @Test
  public void testAddLocaleResourceItem04() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "abc";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);

      AddLocaleResourceSchemaItem item =
          new AddLocaleResourceSchemaItem().itemType(LocaleResourceType.SCHEMA).attributeKey("abc")
              .allowedValue("111").localizedValue("22");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"abc\","
            + "\"error\":\"AttributeKey 'abc' is not used in schema\"}]}", e.getResponseBody());
      }

      // given
      setSiteSchema(siteId, "abc", "222");

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"abc\","
            + "\"error\":\"invalid allowed value '111' on key 'abc'\"}]}", e.getResponseBody());
      }
    }
  }

  private void addAttribute(final String siteId, final String attributeKey) throws ApiException {
    AddAttributeRequest attrReq =
        new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey));
    attributesApi.addAttribute(attrReq, siteId);
  }

  private void setSiteSchema(final String siteId, final String attributeKey,
      final String allowedValue) throws ApiException {
    setSiteSchema(siteId, attributeKey, List.of(allowedValue), true);
  }

  private void setSiteSchema(final String siteId, final String attributeKey,
      final List<String> allowedValues, final boolean required) throws ApiException {

    SetSchemaAttributes schemaAttributes = new SetSchemaAttributes();

    if (required) {
      schemaAttributes.addRequiredItem(
          new AddAttributeSchemaRequired().attributeKey(attributeKey).allowedValues(allowedValues));
    } else {
      schemaAttributes.addOptionalItem(
          new AddAttributeSchemaOptional().attributeKey(attributeKey).allowedValues(allowedValues));
    }

    SetSitesSchemaRequest siteSchema =
        new SetSitesSchemaRequest().name("test").attributes(schemaAttributes);
    this.schemasApi.setSitesSchema(siteId, siteSchema);
  }

  private String setClassification(final String siteId, final String attributeKey,
      final String allowedValue) throws ApiException {
    return setClassification(siteId, attributeKey, List.of(allowedValue), true);
  }

  private String setClassification(final String siteId, final String attributeKey,
      final List<String> allowedValues, final boolean required) throws ApiException {

    SetSchemaAttributes schemaAttributes = new SetSchemaAttributes();

    if (required) {
      schemaAttributes.addRequiredItem(
          new AddAttributeSchemaRequired().attributeKey(attributeKey).allowedValues(allowedValues));
    } else {
      schemaAttributes.addOptionalItem(
          new AddAttributeSchemaOptional().attributeKey(attributeKey).allowedValues(allowedValues));
    }

    AddClassificationRequest classification = new AddClassificationRequest()
        .classification(new AddClassification().name("myClass").attributes(schemaAttributes));
    return this.schemasApi.addClassification(siteId, classification).getClassificationId();
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems with Schema attribute.
   *
   */
  @Test
  public void testAddLocaleResourceItem05() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myAbc";
    String allowedValue = "129380";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      setSiteSchema(siteId, attributeKey, allowedValue);

      // when
      AddLocaleResourceItemResponse response =
          addLocaleSchemaResourceItem(siteId, attributeKey, allowedValue, "localval", locale);

      // then
      assertEquals("SCHEMA#myAbc##129380", response.getItemKey());
      GetLocaleResourceItemResponse i =
          this.systemApi.getLocaleResourceItem(siteId, locale, response.getItemKey());
      assertNotNull(i.getResourceItem());
      assertResourceSchema(i.getResourceItem(), LocaleResourceType.SCHEMA, attributeKey,
          allowedValue, "SCHEMA#myAbc##129380");
    }
  }

  private AddLocaleResourceItemResponse addLocaleSchemaResourceItem(final String siteId,
      final String attributeKey, final String allowedValue, final String localizedValue,
      final String locale) throws ApiException {

    AddLocaleResourceSchemaItem item =
        new AddLocaleResourceSchemaItem().itemType(LocaleResourceType.SCHEMA)
            .attributeKey(attributeKey).allowedValue(allowedValue).localizedValue(localizedValue);
    AddLocaleResourceItemRequest req =
        new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

    return this.systemApi.addLocaleResourceItem(siteId, locale, req);
  }

  private void addLocaleClassificationResourceItem(final String siteId,
      final String classificationId, final String attributeKey, final String locale)
      throws ApiException {

    AddLocaleResourceClassificationItem item = new AddLocaleResourceClassificationItem()
        .itemType(LocaleResourceType.CLASSIFICATION).attributeKey(attributeKey).allowedValue("222")
        .localizedValue("localVal").classificationId(classificationId);
    AddLocaleResourceItemRequest req =
        new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

    this.systemApi.addLocaleResourceItem(siteId, locale, req);
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems with Classification attribute.
   *
   */
  @Test
  public void testAddLocaleResourceItem06() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myAbcd";
    String allowedValue = "129380";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      String classificationId = setClassification(siteId, attributeKey, allowedValue);

      AddLocaleResourceClassificationItem item = new AddLocaleResourceClassificationItem()
          .itemType(LocaleResourceType.CLASSIFICATION).classificationId(classificationId)
          .attributeKey(attributeKey).allowedValue(allowedValue).localizedValue("123");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      AddLocaleResourceItemResponse response =
          this.systemApi.addLocaleResourceItem(siteId, locale, req);

      // then
      assertEquals("CLASSIFICATION##" + classificationId + "##myAbcd##129380",
          response.getItemKey());
      GetLocaleResourceItemResponse i =
          this.systemApi.getLocaleResourceItem(siteId, locale, response.getItemKey());
      assertNotNull(i.getResourceItem());
      assertResourceSchema(i.getResourceItem(), LocaleResourceType.CLASSIFICATION, attributeKey,
          allowedValue, "CLASSIFICATION##" + classificationId + "##myAbcd##129380");
      assertEquals(classificationId, i.getResourceItem().getClassificationId());
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems with Classification attribute, but matching
   * attribute is on the schema.
   *
   */
  @Test
  public void testAddLocaleResourceItem07() throws ApiException {
    // given
    String locale = "en";
    String attributeKey0 = "myAbcd";
    String allowedValue0 = "129380";
    String attributeKey1 = "other";
    String allowedValue1 = "111111";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey0);
      addAttribute(siteId, attributeKey1);

      setSiteSchema(siteId, attributeKey0, allowedValue0);
      String classificationId = setClassification(siteId, attributeKey1, allowedValue1);

      AddLocaleResourceClassificationItem item = new AddLocaleResourceClassificationItem()
          .itemType(LocaleResourceType.CLASSIFICATION).classificationId(classificationId)
          .attributeKey(attributeKey0).allowedValue(allowedValue0).localizedValue("123");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"myAbcd\","
                + "\"error\":\"AttributeKey 'myAbcd' is not used in classification\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /sites/{siteId}/locales/{locale}/resourceItems missing locale.
   *
   */
  @Test
  public void testAddLocaleResourceItem08() {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      AddLocaleResourceInterfaceItem item = new AddLocaleResourceInterfaceItem()
          .interfaceKey("mykey").itemType(LocaleResourceType.INTERFACE).localizedValue("bbb");
      AddLocaleResourceItemRequest req =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.addLocaleResourceItem(siteId, locale, req);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"locale\"," + "\"error\":\"invalid locale 'en'\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/locales/{locale}/resourceItems, not exists.
   *
   */
  @Test
  public void testPutLocale01() throws ApiException {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceInterfaceItem item = new AddLocaleResourceInterfaceItem()
          .interfaceKey("mykey").itemType(LocaleResourceType.INTERFACE).localizedValue("bbb");
      SetLocaleResourceItemRequest req =
          new SetLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      // when
      try {
        this.systemApi.setLocaleResourceItem(siteId, locale, "asdad", req);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"itemKey 'asdad' not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * PUT /sites/{siteId}/locales/{locale}/resourceItems.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPutLocale02() throws Exception {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      AddLocaleResourceInterfaceItem item = new AddLocaleResourceInterfaceItem()
          .interfaceKey("mykey2").itemType(LocaleResourceType.INTERFACE).localizedValue("bbb");
      AddLocaleResourceItemRequest addReq =
          new AddLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));

      AddLocaleResourceItemResponse response =
          this.systemApi.addLocaleResourceItem(siteId, locale, addReq);

      SetLocaleResourceItemRequest setReq =
          new SetLocaleResourceItemRequest().resourceItem(new AddResourceItem(item));
      item.setLocalizedValue("ccc");

      // when
      SetResponse setResponse =
          this.systemApi.setLocaleResourceItem(siteId, locale, response.getItemKey(), setReq);

      // then
      assertEquals("set item 'INTERFACE##mykey2' successfully", setResponse.getMessage());
      GetLocaleResourceItemResponse lri =
          this.systemApi.getLocaleResourceItem(siteId, locale, response.getItemKey());
      assertNotNull(lri);
      assertNotNull(lri.getResourceItem());
      assertResourceInterface(lri.getResourceItem(), "mykey2", "ccc", "INTERFACE##mykey2");
    }
  }

  /**
   * ItemKey not found.
   */
  @Test
  void testDeleteLocaleResourceItem01() throws ApiException {
    // given
    String locale = "en";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      // when
      try {
        this.systemApi.deleteLocaleResourceItem(siteId, locale, "abc");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"ItemKey 'abc' not found\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Get Schema required attribute with locale.
   * 
   * @throws ApiException ApiException
   */
  @Test
  void testGetSchema01() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myattr";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      setSiteSchema(siteId, attributeKey, List.of("1111", "222", "333"), true);
      addLocaleSchemaResourceItem(siteId, attributeKey, "222", "localVal", locale);

      // when
      final GetSitesSchemaResponse sitesSchema0 = this.schemasApi.getSitesSchema(siteId, locale);
      final GetSitesSchemaResponse sitesSchema1 = this.schemasApi.getSitesSchema(siteId, null);

      // then
      assertNotNull(sitesSchema0.getAttributes());
      List<AttributeSchemaRequired> required = notNull(sitesSchema0.getAttributes().getRequired());
      assertEquals(1, required.size());
      AttributeSchemaRequired attr0 = required.get(0);
      assertEquals(attributeKey, attr0.getAttributeKey());
      assertEquals(1, notNull(attr0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", attr0.getLocalizedAllowedValues().get("222"));

      assertNotNull(sitesSchema1.getAttributes());
      AttributeSchemaRequired attr1 = notNull(sitesSchema1.getAttributes().getRequired()).get(0);
      assertEquals(0, notNull(attr1.getLocalizedAllowedValues()).size());

      // when
      GetAttributeAllowedValuesResponse resp0 =
          this.schemasApi.getSitesSchemaAttributeAllowedValues(siteId, attributeKey, locale);
      GetAttributeAllowedValuesResponse resp1 =
          this.schemasApi.getSitesSchemaAttributeAllowedValues(siteId, attributeKey, null);

      // then
      assertEquals(1, notNull(resp0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", resp0.getLocalizedAllowedValues().get("222"));
      assertEquals(0, notNull(resp1.getLocalizedAllowedValues()).size());
    }
  }

  /**
   * Get Schema optional attribute with locale.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testGetSchema02() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myattr";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      setSiteSchema(siteId, attributeKey, List.of("1111", "222", "333"), false);
      addLocaleSchemaResourceItem(siteId, attributeKey, "222", "localVal", locale);

      // when
      final GetSitesSchemaResponse sitesSchema0 = this.schemasApi.getSitesSchema(siteId, locale);
      final GetSitesSchemaResponse sitesSchema1 = this.schemasApi.getSitesSchema(siteId, null);

      // then
      assertNotNull(sitesSchema0.getAttributes());
      List<AttributeSchemaOptional> optional = notNull(sitesSchema0.getAttributes().getOptional());
      assertEquals(1, optional.size());
      AttributeSchemaOptional attr0 = optional.get(0);
      assertEquals(attributeKey, attr0.getAttributeKey());
      assertEquals(1, notNull(attr0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", attr0.getLocalizedAllowedValues().get("222"));

      assertNotNull(sitesSchema1.getAttributes());
      AttributeSchemaOptional attr1 = notNull(sitesSchema1.getAttributes().getOptional()).get(0);
      assertEquals(0, notNull(attr1.getLocalizedAllowedValues()).size());

      // when
      GetAttributeAllowedValuesResponse resp0 =
          this.schemasApi.getSitesSchemaAttributeAllowedValues(siteId, attributeKey, locale);
      GetAttributeAllowedValuesResponse resp1 =
          this.schemasApi.getSitesSchemaAttributeAllowedValues(siteId, attributeKey, null);

      // then
      assertEquals(1, notNull(resp0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", resp0.getLocalizedAllowedValues().get("222"));
      assertEquals(0, notNull(resp1.getLocalizedAllowedValues()).size());
    }
  }

  /**
   * Get Classification required attribute with locale.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testGetClassification01() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myattr";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      String classificationId =
          setClassification(siteId, attributeKey, List.of("1111", "222", "333"), true);
      addLocaleClassificationResourceItem(siteId, classificationId, attributeKey, locale);

      // when
      final Classification c0 =
          this.schemasApi.getClassification(siteId, classificationId, locale).getClassification();
      final Classification c1 =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      // then
      assertNotNull(c0);
      assertNotNull(c0.getAttributes());

      List<AttributeSchemaRequired> required = notNull(c0.getAttributes().getRequired());
      assertEquals(1, required.size());
      AttributeSchemaRequired attr0 = required.get(0);
      assertEquals(attributeKey, attr0.getAttributeKey());
      assertEquals(1, notNull(attr0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", attr0.getLocalizedAllowedValues().get("222"));

      assertNotNull(c1);
      assertNotNull(c1.getAttributes());
      AttributeSchemaRequired attr1 = notNull(c1.getAttributes().getRequired()).get(0);
      assertEquals(0, notNull(attr1.getLocalizedAllowedValues()).size());

      // when
      GetAttributeAllowedValuesResponse resp0 = this.schemasApi
          .getClassificationAttributeAllowedValues(siteId, classificationId, attributeKey, locale);
      GetAttributeAllowedValuesResponse resp1 = this.schemasApi
          .getClassificationAttributeAllowedValues(siteId, classificationId, attributeKey, null);

      // then
      assertEquals(1, notNull(resp0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", resp0.getLocalizedAllowedValues().get("222"));
      assertEquals(0, notNull(resp1.getLocalizedAllowedValues()).size());
    }
  }

  /**
   * Get Classification optional attribute with locale.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testGetClassification02() throws ApiException {
    // given
    String locale = "en";
    String attributeKey = "myattr";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(siteId);

      this.systemApi.addLocale(siteId, new AddLocaleRequest().locale(locale));

      addAttribute(siteId, attributeKey);
      String classificationId =
          setClassification(siteId, attributeKey, List.of("1111", "222", "333"), false);
      addLocaleClassificationResourceItem(siteId, classificationId, attributeKey, locale);

      // when
      final Classification c0 =
          this.schemasApi.getClassification(siteId, classificationId, locale).getClassification();
      final Classification c1 =
          this.schemasApi.getClassification(siteId, classificationId, null).getClassification();

      // then
      assertNotNull(c0);
      assertNotNull(c0.getAttributes());

      List<AttributeSchemaOptional> optional = notNull(c0.getAttributes().getOptional());
      assertEquals(1, optional.size());
      AttributeSchemaOptional attr0 = optional.get(0);
      assertEquals(attributeKey, attr0.getAttributeKey());
      assertEquals(1, notNull(attr0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", attr0.getLocalizedAllowedValues().get("222"));

      assertNotNull(c1);
      assertNotNull(c1.getAttributes());
      AttributeSchemaOptional attr1 = notNull(c1.getAttributes().getOptional()).get(0);
      assertEquals(0, notNull(attr1.getLocalizedAllowedValues()).size());

      // when
      GetAttributeAllowedValuesResponse resp0 = this.schemasApi
          .getClassificationAttributeAllowedValues(siteId, classificationId, attributeKey, locale);
      GetAttributeAllowedValuesResponse resp1 = this.schemasApi
          .getClassificationAttributeAllowedValues(siteId, classificationId, attributeKey, null);

      // then
      assertEquals(1, notNull(resp0.getLocalizedAllowedValues()).size());
      assertEquals("localVal", resp0.getLocalizedAllowedValues().get("222"));
      assertEquals(0, notNull(resp1.getLocalizedAllowedValues()).size());
    }
  }
}
