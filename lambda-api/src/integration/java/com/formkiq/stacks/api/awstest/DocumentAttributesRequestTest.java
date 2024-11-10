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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.ReindexApi;
import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddReindexDocumentRequest;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.DocumentSearchMatchAttribute;
import com.formkiq.client.model.ReindexTarget;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SetSitesSchemaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.api.DocumentSearchApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddAttributeResponse;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * GET, POST /documents/{documentId}/attributes tests. GET, PUT, DELETE
 * /documents/{documentId}/attributes/{attributeKey}. DELETE
 * /documents/{documentId}/attributes/{attributeKey}/{attributeValue}
 *
 */
public class DocumentAttributesRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;

  private void addAttribute(final AttributesApi attributeApi, final String siteId, final String key)
      throws ApiException {
    AddAttributeResponse addAttrResponse = attributeApi
        .addAttribute(new AddAttributeRequest().attribute(new AddAttribute().key(key)), siteId);
    assertEquals("Attribute '" + key + "' created", addAttrResponse.getMessage());
  }

  private AddDocumentAttributesRequest addAttributeToDocument(final String key,
      final String value) {
    return new AddDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).addStringValuesItem(value)));
  }

  private String createDocument(final ApiClient apiClient, final String siteId)
      throws ApiException, InterruptedException {
    String documentId = addDocument(apiClient, siteId, null, "some content", null, null);
    waitForDocumentContent(apiClient, siteId, documentId);
    return documentId;
  }

  private void deleteDocumentAttribute(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key) throws ApiException {
    DeleteResponse deleteResponse = api.deleteDocumentAttribute(documentId, key, siteId);
    assertEquals("attribute '" + key + "' removed from document '" + documentId + "'",
        deleteResponse.getMessage());

    List<DocumentAttribute> attributes =
        notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
    assertEquals(0, attributes.size());
  }

  private void deleteDocumentAttributeValue(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key) throws ApiException {
    DeleteResponse deleteResponse =
        api.deleteDocumentAttributeAndValue(documentId, key, "987", siteId);
    assertEquals("attribute value '" + "987" + "' removed from attribute '" + key + "', document '"
        + documentId + "'", deleteResponse.getMessage());

    List<DocumentAttribute> attributes =
        notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertEquals("xyz", attributes.get(0).getStringValue());
    assertTrue(notNull(attributes.get(0).getStringValues()).isEmpty());
  }

  private void setDocumentAttributes(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key) throws ApiException {
    // given
    SetDocumentAttributesRequest setReq =
        new SetDocumentAttributesRequest().addAttributesItem(new AddDocumentAttribute(
            new AddDocumentAttributeStandard().key(key).stringValues(Arrays.asList("123", "abc"))));

    // when
    SetResponse setResponse = api.setDocumentAttributes(documentId, setReq, siteId);

    // then
    assertEquals("set attributes on documentId '" + documentId + "'", setResponse.getMessage());
    List<DocumentAttribute> attributes =
        notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertNull(attributes.get(0).getStringValue());
    assertEquals("123,abc", String.join(",", notNull(attributes.get(0).getStringValues())));
  }

  private void setDocumentAttributeValues(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key, final List<String> values) throws ApiException {

    // given
    SetDocumentAttributeRequest req = new SetDocumentAttributeRequest()
        .attribute(new AddDocumentAttributeValue().stringValues(values));

    // when
    SetResponse response = api.setDocumentAttributeValue(documentId, key, req, siteId);

    // then
    assertEquals("Updated attribute '" + key + "' on document '" + documentId + "'",
        response.getMessage());
    List<DocumentAttribute> attributes =
        notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertNull(attributes.get(0).getStringValue());
    assertEquals("987,xyz", String.join(",", notNull(attributes.get(0).getStringValues())));
  }

  /**
   * GET,POST /documents/{documentId}/attributes and GET
   * /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddDocumentAttributes01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> apiClients = getApiClients(siteId);

      final String key1 = "test_" + UUID.randomUUID();
      final String key2 = "test2_" + UUID.randomUUID();
      final String value = "val";

      AttributesApi attributeApi = new AttributesApi(apiClients.get(0));
      addAttribute(attributeApi, siteId, key1);
      addAttribute(attributeApi, siteId, key2);

      for (ApiClient apiClient : apiClients) {

        String documentId = createDocument(apiClient, siteId);

        DocumentAttributesApi api = new DocumentAttributesApi(apiClient);

        AddDocumentAttributesRequest req = addAttributeToDocument(key1, value);

        // when
        AddResponse response = api.addDocumentAttributes(documentId, req, siteId, null);

        // then
        assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

        List<DocumentAttribute> attributes =
            notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
        assertEquals(1, attributes.size());
        assertEquals(key1, attributes.get(0).getKey());
        assertEquals(value, attributes.get(0).getStringValue());

        DocumentAttribute attribute =
            api.getDocumentAttribute(documentId, key1, siteId).getAttribute();
        assertNotNull(attribute);
        assertEquals(value, attribute.getStringValue());

        setDocumentAttributes(api, siteId, documentId, key2);

        setDocumentAttributeValues(api, siteId, documentId, key2, Arrays.asList("987", "xyz"));

        deleteDocumentAttributeValue(api, siteId, documentId, key2);
        deleteDocumentAttribute(api, siteId, documentId, key2);
      }
    }
  }

  /**
   * Test add document with attributes, add a composite key to schema and then reindex.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testReindexDocument01() throws Exception {
    // given
    String siteId = ID.uuid();

    List<ApiClient> apiClients = getApiClients(siteId);

    final String key1 = "test_" + UUID.randomUUID();
    final String key2 = "test2_" + UUID.randomUUID();
    final String value1 = "val1";
    final String value2 = "val2";

    AttributesApi attributeApi = new AttributesApi(apiClients.get(0));
    addAttribute(attributeApi, siteId, key1);
    addAttribute(attributeApi, siteId, key2);

    for (ApiClient apiClient : apiClients) {

      String documentId = createDocument(apiClient, siteId);

      DocumentAttributesApi api = new DocumentAttributesApi(apiClient);

      AddDocumentAttributesRequest req = new AddDocumentAttributesRequest()
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(key1).addStringValuesItem(value1)))
          .addAttributesItem(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key(key2).addStringValuesItem(value2)));

      // when
      AddResponse response = api.addDocumentAttributes(documentId, req, siteId, null);

      // then
      assertEquals("added attributes to documentId '" + documentId + "'", response.getMessage());

      // given
      SchemasApi schemasApi = new SchemasApi(apiClient);
      SetSitesSchemaRequest sreq = new SetSitesSchemaRequest().name("test")
          .attributes(new SchemaAttributes().addCompositeKeysItem(
              new AttributeSchemaCompositeKey().attributeKeys(List.of(key1, key2))));
      schemasApi.setSitesSchema(siteId, sreq);

      ReindexApi reindexApi = new ReindexApi(apiClient);
      AddReindexDocumentRequest reindexReq =
          new AddReindexDocumentRequest().target(ReindexTarget.ATTRIBUTES);

      // when
      AddResponse addResponse = reindexApi.addReindexDocument(documentId, reindexReq, siteId);

      // then
      assertEquals("Reindex started for documentId '" + documentId + "' on target 'ATTRIBUTES'",
          addResponse.getMessage());

      final int expected = 3;
      List<DocumentAttribute> attributes =
          notNull(api.getDocumentAttributes(documentId, siteId, null, null).getAttributes());
      assertEquals(expected, attributes.size());
    }
  }

  /**
   * Reindex missing TARGET.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testReindexDocument02() throws Exception {
    // given
    String siteId = ID.uuid();

    List<ApiClient> apiClients = getApiClients(siteId);

    for (ApiClient apiClient : apiClients) {

      String documentId = ID.uuid();

      AddReindexDocumentRequest reindexReq = new AddReindexDocumentRequest();
      ReindexApi reindexApi = new ReindexApi(apiClient);

      // when
      try {
        reindexApi.addReindexDocument(documentId, reindexReq, siteId);
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
   * POST /search.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testSearchDocumentAttributes01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      List<ApiClient> apiClients = getApiClients(siteId);

      final String key = "category_" + UUID.randomUUID();
      final String value = "person";

      AttributesApi attributeApi = new AttributesApi(apiClients.get(0));
      addAttribute(attributeApi, siteId, key);

      String documentId = createDocument(apiClients.get(0), siteId);

      DocumentAttributesApi api = new DocumentAttributesApi(apiClients.get(0));
      AddDocumentAttributesRequest req = addAttributeToDocument(key, value);
      api.addDocumentAttributes(documentId, req, siteId, null);

      for (ApiClient apiClient : apiClients) {

        DocumentSearchApi searchApi = new DocumentSearchApi(apiClient);
        DocumentSearchRequest sreq = new DocumentSearchRequest().query(new DocumentSearch()
            .addAttributesItem(new DocumentSearchAttribute().key(key).eq(value)));

        // when EQ
        List<SearchResultDocument> response =
            notNull(searchApi.documentSearch(sreq, siteId, null, null, null).getDocuments());

        // then
        assertEquals(1, response.size());
        assertEquals(documentId, response.get(0).getDocumentId());
        DocumentSearchMatchAttribute matchedAttribute = response.get(0).getMatchedAttribute();
        assertNotNull(matchedAttribute);
        assertEquals(key, matchedAttribute.getKey());
        assertEquals("person", matchedAttribute.getStringValue());
      }
    }
  }
}
