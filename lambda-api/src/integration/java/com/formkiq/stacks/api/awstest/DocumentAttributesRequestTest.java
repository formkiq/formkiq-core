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

import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import joptsimple.internal.Strings;

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
    return new AddDocumentAttributesRequest()
        .addAttributesItem(new AddDocumentAttribute().key(key).addStringValuesItem(value));
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
        api.getDocumentAttributes(documentId, siteId, null, null).getAttributes();
    assertEquals(0, attributes.size());
  }

  private void deleteDocumentAttributeValue(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key, final String value) throws ApiException {
    DeleteResponse deleteResponse =
        api.deleteDocumentAttributeAndValue(documentId, key, value, siteId);
    assertEquals("attribute value '" + value + "' removed from attribute '" + key + "', document '"
        + documentId + "'", deleteResponse.getMessage());

    List<DocumentAttribute> attributes =
        api.getDocumentAttributes(documentId, siteId, null, null).getAttributes();
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertEquals("xyz", attributes.get(0).getStringValue());
    assertTrue(attributes.get(0).getStringValues().isEmpty());
  }

  private void setDocumentAttributes(final DocumentAttributesApi api, final String siteId,
      final String documentId, final String key) throws ApiException {
    // given
    SetDocumentAttributesRequest setReq = new SetDocumentAttributesRequest().addAttributesItem(
        new AddDocumentAttribute().key(key).stringValues(Arrays.asList("123", "abc")));

    // when
    SetResponse setResponse = api.setDocumentAttributes(documentId, setReq, siteId);

    // then
    assertEquals("set attributes on documentId '" + documentId + "'", setResponse.getMessage());
    List<DocumentAttribute> attributes =
        api.getDocumentAttributes(documentId, siteId, null, null).getAttributes();
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertNull(attributes.get(0).getStringValue());
    assertEquals("123,abc", Strings.join(attributes.get(0).getStringValues(), ","));
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
        api.getDocumentAttributes(documentId, siteId, null, null).getAttributes();
    assertEquals(1, attributes.size());
    assertEquals(key, attributes.get(0).getKey());
    assertNull(attributes.get(0).getStringValue());
    assertEquals("987,xyz", Strings.join(attributes.get(0).getStringValues(), ","));
  }

  /**
   * GET,POST /documents/{documentId}/attributes and GET
   * /documents/{documentId}/attributes/{attributeKey}.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddDocumentAttributes01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
            api.getDocumentAttributes(documentId, siteId, null, null).getAttributes();
        assertEquals(1, attributes.size());
        assertEquals(key1, attributes.get(0).getKey());
        assertEquals(value, attributes.get(0).getStringValue());

        assertEquals(value,
            api.getDocumentAttribute(documentId, key1, siteId).getAttribute().getStringValue());

        setDocumentAttributes(api, siteId, documentId, key2);

        setDocumentAttributeValues(api, siteId, documentId, key2, Arrays.asList("987", "xyz"));

        deleteDocumentAttributeValue(api, siteId, documentId, key2, "987");
        deleteDocumentAttribute(api, siteId, documentId, key2);
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
        DocumentSearchResponse response = searchApi.documentSearch(sreq, siteId, null, null, null);

        // then
        assertEquals(1, response.getDocuments().size());
        assertEquals(documentId, response.getDocuments().get(0).getDocumentId());
        assertEquals(key, response.getDocuments().get(0).getMatchedAttribute().getKey());
        assertEquals("person",
            response.getDocuments().get(0).getMatchedAttribute().getStringValue());
      }
    }
  }
}
