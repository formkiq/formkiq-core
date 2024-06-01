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

import com.formkiq.client.api.MappingsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddActionParameters;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.AddMappingResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.Mapping;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqAttributeService.addAttribute;
import static com.formkiq.testutils.aws.FkqDocumentService.addDocument;
import static com.formkiq.testutils.aws.FkqDocumentService.getDocumentAttribute;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActionsComplete;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration Tests for.
 * <p>
 * GET /mappings
 * </p>
 * <p>
 * POST /mappings
 * </p>
 * <p>
 * GET /mappings/{mappingId}
 * </p>
 * <p>
 * PUT /mappings/{mappingId}
 * </p>
 * <p>
 * DELETE /mappings/{mappingId}
 * </p>
 */
public class MappingRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30;

  /**
   * Add Attribute empty request.
   *
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void addMappingRequest01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      List<ApiClient> apiClients = getApiClients(siteId);

      for (ApiClient apiClient : apiClients) {

        MappingsApi api = new MappingsApi(apiClient);

        // when
        try {
          api.addMapping(new AddMappingRequest(), siteId);
          fail();
        } catch (ApiException e) {
          // then
          assertEquals("{\"message\":\"invalid request body\"}", e.getResponseBody());
        }
      }
    }
  }

  /**
   * Add Attribute.
   *
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void addMappingRequest02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      List<ApiClient> apiClients = getApiClients(siteId);
      ApiClient client = apiClients.get(0);
      String attributeKey = "invoice_" + UUID.randomUUID();

      addAttribute(client, siteId, attributeKey, AttributeDataType.STRING, null);
      MappingsApi api = new MappingsApi(client);

      AddMapping addMapping = new AddMapping().name("test")
          .addAttributesItem(new MappingAttribute().attributeKey(attributeKey)
              .sourceType(MappingAttributeSourceType.CONTENT)
              .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
              .labelTexts(List.of("invoice")));

      // when
      AddMappingResponse addMappingResponse =
          api.addMapping(new AddMappingRequest().mapping(addMapping), siteId);

      // then
      final String mappingId = addMappingResponse.getMappingId();

      List<Mapping> mappings = notNull(api.getMappings(siteId, null, null).getMappings());
      assertFalse(mappings.isEmpty());
      assertNotNull(mappings.get(0).getName());
      assertNotNull(mappings.get(0).getDescription());

      List<MappingAttribute> attributes = notNull(mappings.get(0).getAttributes());
      assertNotNull(attributes.get(0).getAttributeKey());

      Mapping mapping = api.getMapping(mappingId, siteId).getMapping();
      assertNotNull(mapping);
      assertEquals("test", mapping.getName());
      assertEquals("", mapping.getDescription());

      attributes = notNull(mapping.getAttributes());
      assertEquals(1, attributes.size());
      assertEquals(attributeKey, attributes.get(0).getAttributeKey());

      api.deleteMapping(mappingId, siteId);

      try {
        api.getMapping(mappingId, siteId).getMapping();
      } catch (ApiException e) {
        assertEquals("{\"message\":\"Mapping '" + mappingId + "' not found\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Test IDP action.
   *
   * @throws ApiException ApiException
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  void addIdpAction01() throws ApiException, InterruptedException {
    // given
    final String attributeKey = "document_invoice_" + UUID.randomUUID();

    List<ApiClient> apiClients = getApiClients(null);
    ApiClient client = apiClients.get(0);

    addAttribute(client, null, attributeKey, AttributeDataType.STRING, null);
    MappingsApi api = new MappingsApi(client);

    AddMapping addMapping = new AddMapping().name("Document Invoice")
        .addAttributesItem(new MappingAttribute().attributeKey(attributeKey)
            .sourceType(MappingAttributeSourceType.CONTENT)
            .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS)
            .validationRegex("INV-\\d+").labelTexts(List.of("invoice", "invoice no")));

    AddMappingResponse addMappingResponse =
        api.addMapping(new AddMappingRequest().mapping(addMapping), null);

    final String mappingId = addMappingResponse.getMappingId();

    String content = """
        From:
        DEMO - Sliced Invoices
        Order Number 12345
        Invoice Number INV-3337
        123 Somewhere Street Your City AZ 12345 admin@slicedinvoices.com""";

    // when
    AddAction action = new AddAction().type(DocumentActionType.IDP)
        .parameters(new AddActionParameters().mappingId(mappingId));
    String documentId =
        addDocument(client, null, "document_invoice.txt", content, "text/plain", List.of(action));

    // then
    waitForActionsComplete(client, null, documentId);

    assertEquals("INV-3337",
        getDocumentAttribute(client, null, documentId, attributeKey).getStringValue());
  }
}
