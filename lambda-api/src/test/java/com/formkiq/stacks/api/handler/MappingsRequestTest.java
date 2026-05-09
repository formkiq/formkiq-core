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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.AddMappingResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetMappingsResponse;
import com.formkiq.client.model.Mapping;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeContent;
import com.formkiq.client.model.MappingAttributeDataClassification;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeMalwareScan;
import com.formkiq.client.model.MappingAttributeManual;
import com.formkiq.client.model.MappingAttributeMetadata;
import com.formkiq.client.model.MappingAttributeMetadataExtractionResult;
import com.formkiq.client.model.MappingAttributeMetadataField;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.client.model.MappingClassification;
import com.formkiq.client.model.MappingClassificationConditionMatchingType;
import com.formkiq.client.model.MappingClassificationConditionMetadataExtractionResult;
import com.formkiq.client.model.MappingClassificationConditionSourceType;
import com.formkiq.client.model.SetMappingRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.mappings.AddMappingRequestBuilder;
import com.formkiq.testutils.api.mappings.GetMappingRequestBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /mappings. */
public class MappingsRequestTest extends AbstractApiClientRequestTest {

  /** SiteId. */
  private static final String SITE_ID = ID.uuid();
  /** Classification Id. */
  private static final String CLASSIFICATION_ID = "1658e3c2-b7c9-4cf2-8325-abcdd101ec57";
  /** Classification Id 2. */
  private static final String CLASSIFICATION_ID_2 = "2658e3c2-b7c9-4cf2-8325-abcdd101ec58";

  private static void assertMapping(final GetMappingsResponse response, final String name,
      final String description) {
    assertEquals(1, notNull(response.getMappings()).size());
    assertMapping(response.getMappings().get(0), name, description);
  }

  private static void assertMapping(final Mapping mapping, final String name,
      final String description) {
    assertNotNull(mapping);
    assertEquals(name, mapping.getName());
    assertEquals(description, mapping.getDescription());
  }

  private static void assertMappingAttributeContent(final GetMappingsResponse response) {
    var mappings = notNull(response.getMappings());
    assertEquals(1, mappings.size());

    assertMappingAttributeContent(mappings.get(0));
  }

  private static void assertMappingAttributeContent(final Mapping mapping) {

    assertNotNull(mapping);

    var mappingAttributes = notNull(mapping.getAttributes());
    assertEquals(1, mappingAttributes.size());

    var attribute = mappingAttributes.get(0).getMappingAttributeContent();
    assertEquals("invoice", attribute.getAttributeKey());
    assertEquals(MappingAttributeSourceType.CONTENT, attribute.getSourceType());
    assertEquals(MappingAttributeLabelMatchingType.CONTAINS, attribute.getLabelMatchingType());
    assertEquals("invoice", String.join(",", notNull(attribute.getLabelTexts())));
  }

  private static void assertMappingManual(final Mapping mapping, final String defaultValue) {

    assertNotNull(mapping);
    var mappingAttributes = notNull(mapping.getAttributes());
    assertEquals(1, mappingAttributes.size());

    var mappingAttribute = mappingAttributes.get(0).getMappingAttributeManual();
    assertEquals("invoice", mappingAttribute.getAttributeKey());
    assertEquals(MappingAttributeSourceType.MANUAL, mappingAttribute.getSourceType());
    assertEquals(defaultValue, mappingAttribute.getDefaultValue());
    assertEquals("", String.join(",", notNull(mappingAttribute.getDefaultValues())));
  }

  private void addAttribute(final String siteId) throws ApiException {
    addAttributeString(siteId, "invoice");
  }

  private void addAttributeKeyOnly(final String siteId) throws ApiException {
    this.attributesApi.addAttribute(new AddAttributeRequest()
        .attribute(new AddAttribute().key("invoice").dataType(AttributeDataType.KEY_ONLY)), siteId);
  }

  private void addAttributeString(final String siteId, final String key) throws ApiException {
    this.attributesApi
        .addAttribute(new AddAttributeRequest().attribute(new AddAttribute().key(key)), siteId);
  }

  private void assertAddMappingValidationError(final String siteId,
      final MappingAttribute mappingAttribute, final String expectedResponseBody) {
    AddMappingRequest req = new AddMappingRequest()
        .mapping(new AddMapping().name("asd").addAttributesItem(mappingAttribute));

    try {
      this.mappingsApi.addMapping(req, siteId);
      fail();
    } catch (ApiException e) {
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals(expectedResponseBody, e.getResponseBody());
    }
  }

  private AddMapping classificationMappingRequest(
      final List<MappingClassification> classifications) {
    return new AddMapping().name("AI Document Classification").description("").attributes(List.of())
        .classifications(classifications);
  }

  private MappingAttribute contentKeyValueMappingAttribute(final List<String> labelTexts) {
    return new MappingAttribute(new MappingAttributeContent().attributeKey("invoicekv")
        .sourceType(MappingAttributeSourceType.CONTENT_KEY_VALUE)
        .labelMatchingType(MappingAttributeLabelMatchingType.EXACT).labelTexts(labelTexts));
  }

  private MappingAttribute contentMappingAttribute(final String attributeKey,
      final List<String> labelTexts) {
    return new MappingAttribute(new MappingAttributeContent().attributeKey(attributeKey)
        .sourceType(MappingAttributeSourceType.CONTENT)
        .labelMatchingType(MappingAttributeLabelMatchingType.CONTAINS).labelTexts(labelTexts));
  }

  private MappingAttribute dataClassificationMappingAttribute() {
    return new MappingAttribute(new MappingAttributeDataClassification().attributeKey("invoicedata")
        .sourceType(MappingAttributeSourceType.DATA_CLASSIFICATION));
  }

  private MappingAttribute malwareScanMappingAttribute() {
    return new MappingAttribute(new MappingAttributeMalwareScan().attributeKey("invoicemalware")
        .sourceType(MappingAttributeSourceType.MALWARE_SCAN));
  }

  private MappingAttribute manualMappingAttribute() {
    return new MappingAttribute(new MappingAttributeManual().attributeKey("invoice")
        .sourceType(MappingAttributeSourceType.MANUAL));
  }

  private MappingAttribute manualMappingAttribute(final String attributeKey) {
    return new MappingAttribute(new MappingAttributeManual().attributeKey(attributeKey)
        .sourceType(MappingAttributeSourceType.MANUAL).defaultValue("23"));
  }

  private MappingClassification mappingClassification(final String classificationId,
      final List<MappingClassificationConditionMetadataExtractionResult> conditions) {
    return new MappingClassification().classificationId(classificationId).conditions(conditions);
  }

  private MappingClassificationConditionMetadataExtractionResult mappingClassificationCondition() {
    return new MappingClassificationConditionMetadataExtractionResult()
        .sourceType(MappingClassificationConditionSourceType.METADATA_EXTRACTION_RESULT)
        .resultKey("classification").resultValue("INVOICE").llmPromptEntityName("myprompt")
        .resultMatchingType(MappingClassificationConditionMatchingType.EXACT);
  }

  private MappingAttribute metadataExtractionMappingAttribute() {
    return new MappingAttribute(
        new MappingAttributeMetadataExtractionResult().attributeKey("invoiceextraction")
            .sourceType(MappingAttributeSourceType.METADATA_EXTRACTION_RESULT)
            .llmPromptEntityName("invoice"));
  }

  private MappingAttribute metadataMappingAttribute(final String attributeKey,
      final List<String> labelTexts, final MappingAttributeMetadataField metadataField) {
    return new MappingAttribute(new MappingAttributeMetadata().attributeKey(attributeKey)
        .sourceType(MappingAttributeSourceType.METADATA)
        .labelMatchingType(MappingAttributeLabelMatchingType.EXACT).labelTexts(labelTexts)
        .metadataField(metadataField));
  }

  /**
   * POST /mappings.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddContentMappings() throws ApiException {
    // given
    final String key0 = "invoice";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId);

      AddMapping mapping = new AddMapping().name("test")
          .addAttributesItem(contentMappingAttribute(key0, List.of("invoice")));

      AddMappingRequest req = new AddMappingRequest().mapping(mapping);

      // when
      AddMappingResponse addResponse = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(addResponse.getMappingId());

      GetMappingsResponse response = this.mappingsApi.getMappings(siteId, null, null);
      assertMapping(response, "test", "");
      assertMappingAttributeContent(response);

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
      assertMapping(response, "another", "test desc");
      assertMappingAttributeContent(response);

      Mapping m = this.mappingsApi.getMapping(addResponse.getMappingId(), siteId).getMapping();
      assertMapping(m, "another", "test desc");

      assertMappingAttributeContent(m);
    }
  }

  /**
   * POST /mappings SourceTypes.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddMappingsAllSourceTypes() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      List<String> attributeKeys = List.of("invoicecontent", "invoicekv", "invoicemetadata",
          "invoicemanual", "invoicedata", "invoiceextraction", "invoicemalware");
      for (String attributeKey : attributeKeys) {
        addAttributeString(siteId, attributeKey);
      }

      AddMapping mapping = new AddMapping().name("source types")
          .addAttributesItem(contentMappingAttribute("invoicecontent", List.of("invoice")))
          .addAttributesItem(contentKeyValueMappingAttribute(List.of("invoice")))
          .addAttributesItem(metadataMappingAttribute("invoicemetadata", List.of("application/pdf"),
              MappingAttributeMetadataField.CONTENT_TYPE))
          .addAttributesItem(manualMappingAttribute("invoicemanual"))
          .addAttributesItem(dataClassificationMappingAttribute())
          .addAttributesItem(metadataExtractionMappingAttribute())
          .addAttributesItem(malwareScanMappingAttribute());

      AddMappingRequest req = new AddMappingRequest().mapping(mapping);

      // when
      AddMappingResponse addResponse = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(addResponse.getMappingId());

      // when
      Mapping responseMapping =
          this.mappingsApi.getMapping(addResponse.getMappingId(), siteId).getMapping();

      // then
      assertNotNull(responseMapping);
      List<MappingAttribute> attributes = notNull(responseMapping.getAttributes());
      assertEquals(7, attributes.size());
      assertEquals(MappingAttributeSourceType.CONTENT,
          attributes.get(0).getMappingAttributeContent().getSourceType());
      assertEquals(MappingAttributeSourceType.CONTENT_KEY_VALUE,
          attributes.get(1).getMappingAttributeContent().getSourceType());
      assertEquals(MappingAttributeSourceType.METADATA,
          attributes.get(2).getMappingAttributeMetadata().getSourceType());
      assertEquals(MappingAttributeSourceType.MANUAL,
          attributes.get(3).getMappingAttributeManual().getSourceType());
      assertEquals(MappingAttributeSourceType.DATA_CLASSIFICATION,
          attributes.get(4).getMappingAttributeDataClassification().getSourceType());
      assertEquals(MappingAttributeSourceType.METADATA_EXTRACTION_RESULT,
          attributes.get(5).getMappingAttributeMetadataExtractionResult().getSourceType());
      assertEquals(MappingAttributeSourceType.MALWARE_SCAN,
          attributes.get(6).getMappingAttributeMalwareScan().getSourceType());
    }
  }

  /**
   * POST /mappings with attributes and classifications.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddMappingsAttributesAndClassificationsFails() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {
      setBearerToken(siteId);
      addAttribute(siteId);

      AddMapping mapping = new AddMapping().name("AI Document Classification")
          .addAttributesItem(contentMappingAttribute("invoice", List.of("invoice")))
          .classifications(List.of(mappingClassification(CLASSIFICATION_ID, List.of())));

      // when
      try {
        this.mappingsApi.addMapping(new AddMappingRequest().mapping(mapping), siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"attributes\","
                + "\"error\":\"'attributes' and 'classifications' cannot both be set\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * POST /mappings with classifications.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddMappingsClassifications() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      AddMapping addMapping = classificationMappingRequest(List
          .of(mappingClassification(CLASSIFICATION_ID, List.of(mappingClassificationCondition()))));

      // when
      var mappingId = new AddMappingRequestBuilder().addMapping(addMapping).submit(client, siteId)
          .throwIfError().response().getMappingId();

      // then
      assertNotNull(mappingId);

      // when
      var mapping = new GetMappingRequestBuilder(mappingId).submit(client, siteId).throwIfError()
          .response().getMapping();

      // then
      assertNotNull(mapping);
      assertEquals("AI Document Classification", mapping.getName());
      assertEquals("", mapping.getDescription());

      List<MappingAttribute> attributes = notNull(mapping.getAttributes());
      assertEquals(0, attributes.size());

      List<MappingClassification> classifications = notNull(mapping.getClassifications());
      assertEquals(1, classifications.size());

      MappingClassification classification = classifications.get(0);
      assertEquals(CLASSIFICATION_ID, classification.getClassificationId());

      List<MappingClassificationConditionMetadataExtractionResult> conditions =
          notNull(classification.getConditions());
      assertEquals(1, conditions.size());
      assertEquals(MappingClassificationConditionSourceType.METADATA_EXTRACTION_RESULT,
          conditions.get(0).getSourceType());
      assertEquals("classification", conditions.get(0).getResultKey());
      assertEquals("INVOICE", conditions.get(0).getResultValue());
      assertEquals(MappingClassificationConditionMatchingType.EXACT,
          conditions.get(0).getResultMatchingType());
    }
  }

  /**
   * POST /mappings empty body.
   *
   */
  @Test
  public void testAddMappingsEmptyBody() {
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
  public void testAddMappingsEmptyMapping() {
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
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"},"
            + "{\"key\":\"attributes\",\"error\":\"'attributes' or 'classifications' is "
            + "required\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /mappings MANUAL, KEY_ONLY attribute.
   *
   */
  @Test
  public void testAddMappingsKeyOnlyAttribute() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttributeKeyOnly(siteId);

      AddMappingRequest req = new AddMappingRequest().mapping(
          new AddMapping().name("asd").addAttributesItem(manualMappingAttribute("invoice")));

      // when
      try {
        this.mappingsApi.addMapping(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"attribute[0].defaultValue\","
            + "\"error\":\"'defaultValue' or 'defaultValues' cannot be used with "
            + "KEY_ONLY attribute\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * POST /mappings SourceType MANUAL.
   *
   */
  @Test
  public void testAddMappingsManual() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId);

      AddMappingRequest req = new AddMappingRequest().mapping(
          new AddMapping().name("asd").addAttributesItem(manualMappingAttribute("invoice")));

      // when
      AddMappingResponse response = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(response.getMappingId());

      Mapping m = this.mappingsApi.getMapping(response.getMappingId(), siteId).getMapping();
      assertMapping(m, "asd", "");
      assertMappingManual(m, "23");
    }
  }

  /**
   * POST /mappings MANUAL, KEY_ONLY attribute.
   *
   */
  @Test
  public void testAddMappingsManualKeyOnly() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttributeKeyOnly(siteId);

      AddMappingRequest req = new AddMappingRequest()
          .mapping(new AddMapping().name("asd").addAttributesItem(manualMappingAttribute()));

      // when
      AddMappingResponse response = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(response.getMappingId());

      Mapping m = this.mappingsApi.getMapping(response.getMappingId(), siteId).getMapping();
      assertMapping(m, "asd", "");
      assertMappingManual(m, null);
    }
  }

  /**
   * POST /mappings with multiple classifications missing conditions.
   *
   */
  @Test
  public void testAddMappingsMultipleClassificationsConditionsFails() {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      var addMapping =
          classificationMappingRequest(List.of(mappingClassification(CLASSIFICATION_ID, List.of()),
              mappingClassification(CLASSIFICATION_ID_2, List.of())));

      // when
      var response = new AddMappingRequestBuilder().addMapping(addMapping).submit(client, siteId);

      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(),
          response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"classifications\","
              + "\"error\":\"only one classification can omit conditions\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * POST /mappings with one default classification and one conditional classification.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddMappingsMultipleClassificationsOneWithoutConditions() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      var addMapping = classificationMappingRequest(List.of(
          mappingClassification(CLASSIFICATION_ID, List.of()),
          mappingClassification(CLASSIFICATION_ID_2, List.of(mappingClassificationCondition()))));

      // when
      var response = new AddMappingRequestBuilder().addMapping(addMapping).submit(client, siteId)
          .throwIfError().response();

      // then
      assertNotNull(response.getMappingId());
    }
  }

  /**
   * POST /mappings SourceType validation.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testAddMappingsValidationFails() throws ApiException {
    // given
    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);
      addAttribute(siteId);

      assertAddMappingValidationError(siteId,
          new MappingAttribute(new MappingAttributeContent().attributeKey("invoice")
              .sourceType(MappingAttributeSourceType.CONTENT)),
          "{\"errors\":[{\"key\":\"attribute[0].labelMatchingType\","
              + "\"error\":\"'labelMatchingType' is required\"},"
              + "{\"key\":\"attribute[0].labelTexts\","
              + "\"error\":\"'labelTexts' is required\"}]}");

      assertAddMappingValidationError(siteId,
          new MappingAttribute(new MappingAttributeContent().attributeKey("invoice")
              .sourceType(MappingAttributeSourceType.CONTENT_KEY_VALUE)),
          "{\"errors\":[{\"key\":\"attribute[0].labelMatchingType\","
              + "\"error\":\"'labelMatchingType' is required\"},"
              + "{\"key\":\"attribute[0].labelTexts\","
              + "\"error\":\"'labelTexts' is required\"}]}");

      assertAddMappingValidationError(siteId,
          new MappingAttribute(new MappingAttributeMetadata().attributeKey("invoice")
              .sourceType(MappingAttributeSourceType.METADATA)
              .labelMatchingType(MappingAttributeLabelMatchingType.EXACT)
              .labelTexts(List.of("invoice"))),
          "{\"errors\":[{\"key\":\"attribute[0].metadataField\","
              + "\"error\":\"'metadataField' is required\"}]}");

      assertAddMappingValidationError(siteId, manualMappingAttribute(),
          "{\"errors\":[{\"key\":\"attribute[0].defaultValue\","
              + "\"error\":\"'defaultValue' or 'defaultValues' is required\"}]}");

      assertAddMappingValidationError(siteId,
          new MappingAttribute(new MappingAttributeDataClassification()
              .sourceType(MappingAttributeSourceType.DATA_CLASSIFICATION)),
          "{\"errors\":[{\"key\":\"attribute[0].attributeKey\","
              + "\"error\":\"'attributeKey' is required\"}]}");

      assertAddMappingValidationError(siteId,
          new MappingAttribute(
              new MappingAttributeMetadataExtractionResult().attributeKey("invoice")
                  .sourceType(MappingAttributeSourceType.METADATA_EXTRACTION_RESULT)),
          "{\"errors\":[{\"key\":\"attribute[0].llmPromptEntityName\","
              + "\"error\":\"'llmPromptEntityName' is required\"}]}");

      assertAddMappingValidationError(siteId,
          new MappingAttribute(new MappingAttributeMalwareScan()
              .sourceType(MappingAttributeSourceType.MALWARE_SCAN)),
          "{\"errors\":[{\"key\":\"attribute[0].attributeKey\","
              + "\"error\":\"'attributeKey' is required\"}]}");
    }
  }

  /**
   * DELETE /mappings/{mappingId}.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteMappings() throws ApiException {
    // given
    final String key0 = "invoice";

    for (String siteId : Arrays.asList(null, SITE_ID)) {

      setBearerToken(siteId);

      addAttribute(siteId);

      AddMapping mapping = new AddMapping().name("test")
          .addAttributesItem(contentMappingAttribute(key0, List.of("invoice")));

      AddMappingRequest req = new AddMappingRequest().mapping(mapping);

      // when
      AddMappingResponse addResponse = this.mappingsApi.addMapping(req, siteId);

      // then
      assertNotNull(addResponse.getMappingId());
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
}
