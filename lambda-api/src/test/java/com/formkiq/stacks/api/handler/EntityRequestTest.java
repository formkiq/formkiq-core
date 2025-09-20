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

import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddEntity;
import com.formkiq.client.model.AddEntityAttribute;
import com.formkiq.client.model.AddEntityRequest;
import com.formkiq.client.model.AddEntityResponse;
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityAttribute;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetDocumentAttributeResponse;
import com.formkiq.client.model.GetEntitiesResponse;
import com.formkiq.client.model.GetEntityResponse;
import com.formkiq.client.model.UpdateEntityRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.attributes.GetAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributeRequestBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /entities. */
public class EntityRequestTest extends AbstractApiClientRequestTest {

  /**
   * Post /entities/{entityTypeId}.
   *
   */
  @Test
  public void testAddEntity01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId0 = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("Acme Inc"));

      for (String entityTypeId : List.of(entityTypeId0, "Company")) {
        // when
        AddEntityResponse addEntityResponse =
            this.entityApi.addEntity(entityTypeId, req, siteId, "custom");

        // then
        assertNotNull(addEntityResponse.getEntityId());

        GetEntityResponse entity = this.entityApi.getEntity(entityTypeId,
            addEntityResponse.getEntityId(), siteId, "CUSTOM");
        assertNotNull(entity.getEntity());
        assertEntityEquals(entity.getEntity(), "Acme Inc");
      }
    }
  }

  /**
   * Post /entities/{entityTypeId}. Invalid Request.
   *
   */
  @Test
  public void testAddEntity02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest();

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"Missing 'entity'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities/{entityTypeId}. Invalid EntityTypeId.
   *
   */
  @Test
  public void testAddEntity03() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = ID.uuid();
      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("Acme Inc"));

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId, null);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"entityTypeId\",\"error\":\"'entityTypeId' is invalid\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities/{entityTypeId}. Missing attribute.
   *
   */
  @Test
  public void testAddEntity04() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("Acme Inc")
          .addAttributesItem(new AddEntityAttribute().key("b").booleanValue(true)));

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId, "CUSTOM");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"b\",\"error\":\"attribute 'b' not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities/{entityTypeId}. Wrong attribute data type.
   *
   */
  @Test
  public void testAddEntity05() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});
      addAttribute(siteId, "b", AttributeDataType.STRING);

      String entityTypeId = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("Acme Inc")
          .addAttributesItem(new AddEntityAttribute().key("b").booleanValue(true)));

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId, "CUSTOM");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"b\"," + "\"error\":\"attribute only support string value\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities/{entityTypeId} for LLM Prompt.
   *
   */
  @Test
  public void testAddEntityLlmPrompt() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addLlmPromptEntityType(siteId);

      for (String namespace : Arrays.asList(null, "preset", "custom")) {

        String name = "MyPrompt_" + ID.uuid();
        AddEntityRequest req =
            new AddEntityRequest().entity(new AddEntity().name(name).addAttributesItem(
                new AddEntityAttribute().key("userPrompt").stringValue("This prompt")));

        // when
        AddEntityResponse response = this.entityApi.addEntity(entityTypeId, req, siteId, namespace);

        // then
        String entityId = response.getEntityId();
        assertNotNull(entityId);

        // when - try with different namespaces
        GetEntityResponse entityResponse0 =
            this.entityApi.getEntity(entityTypeId, entityId, siteId, "preset");
        GetEntityResponse entityResponse1 =
            this.entityApi.getEntity(entityTypeId, entityId, siteId, "custom");
        GetEntityResponse entityResponse2 =
            this.entityApi.getEntity(entityTypeId, entityId, siteId, null);

        for (GetEntityResponse entityResponse : List.of(entityResponse0, entityResponse1,
            entityResponse2)) {
          Entity entity = entityResponse.getEntity();
          assertNotNull(entity);
          assertEquals(name, entity.getName());

          List<EntityAttribute> attributes = notNull(entity.getAttributes());
          assertEquals(1, attributes.size());
          assertEquals("userPrompt", attributes.get(0).getKey());
          assertEquals("This prompt", attributes.get(0).getStringValue());
        }
      }
    }
  }

  /**
   * Post /entities/{entityTypeId} for Checkout.
   *
   */
  @Test
  public void testAddEntityCheckoutByEntityId() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addCheckoutEntityType(siteId);

      String name = "MyCheckout_" + ID.uuid();
      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name(name));

      // when
      AddEntityResponse response = this.entityApi.addEntity(entityTypeId, req, siteId, "preset");

      // then
      String entityId = response.getEntityId();
      assertNotNull(entityId);

      // when - try with different namespaces
      GetEntityResponse entityResponse0 =
          this.entityApi.getEntity(entityTypeId, entityId, siteId, "preset");
      GetEntityResponse entityResponse1 =
          this.entityApi.getEntity(entityTypeId, entityId, siteId, "custom");
      GetEntityResponse entityResponse2 =
          this.entityApi.getEntity(entityTypeId, entityId, siteId, null);

      for (GetEntityResponse entityResponse : List.of(entityResponse0, entityResponse1,
          entityResponse2)) {
        Entity entity = entityResponse.getEntity();
        assertNotNull(entity);
        assertEquals(name, entity.getName());

        int i = 0;
        List<EntityAttribute> attributes = notNull(entity.getAttributes());
        assertEquals(2, attributes.size());
        assertEquals("LockedBy", attributes.get(i).getKey());
        assertEquals("joesmith", attributes.get(i++).getStringValue());
        assertEquals("LockedDate", attributes.get(i).getKey());
        assertNotNull(attributes.get(i).getStringValue());

        validateCheckoutAttributes(siteId);
      }
    }
  }

  /**
   * Post /entities/{entityTypeId} for Checkout.
   *
   */
  @Test
  public void testAddEntityCheckoutByEntityName() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      final String documentId = new AddDocumentRequestBuilder().content().submit(client, siteId)
          .response().getDocumentId();

      addCheckoutEntityType(siteId);
      validateCheckoutAttributes(siteId);

      String name = "MyCheckout_" + ID.uuid();
      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name(name));

      // when
      AddEntityResponse response = this.entityApi.addEntity("Checkout", req, siteId, "preset");

      // then
      String entityId = response.getEntityId();
      assertNotNull(entityId);

      // when - try with different namespaces
      GetEntityResponse entityResponse =
          this.entityApi.getEntity("Checkout", entityId, siteId, "preset");

      // then
      Entity entity = entityResponse.getEntity();
      assertNotNull(entity);
      assertEquals(name, entity.getName());

      int i = 0;
      List<EntityAttribute> attributes = notNull(entity.getAttributes());
      assertEquals(2, attributes.size());
      assertEquals("LockedBy", attributes.get(i).getKey());
      assertEquals("joesmith", attributes.get(i++).getStringValue());
      assertEquals("LockedDate", attributes.get(i).getKey());
      assertNotNull(attributes.get(i).getStringValue());

      validateCheckoutAttributes(siteId);

      ApiHttpResponse<AddResponse> addResp =
          new AddDocumentAttributeRequestBuilder().setDocumentId(documentId)
              .addAttribute("Checkout", "Checkout", entityId, EntityTypeNamespace.PRESET)
              .submit(client, siteId);
      assertFalse(addResp.isError());

      ApiHttpResponse<GetDocumentAttributeResponse> attr =
          new GetDocumentAttributeRequestBuilder(documentId, "Checkout").submit(client, siteId);
      assertFalse(attr.isError());
      assertNotNull(Objects.requireNonNull(attr.response().getAttribute()).getStringValue());
    }
  }

  /**
   * Post /entities/{entityTypeId} for Checkout and try and override values.
   *
   */
  @Test
  public void testAddEntityCheckoutByOverrideValues() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      addCheckoutEntityType(siteId);

      String name = "MyCheckout_" + ID.uuid();
      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name(name)
          .addAttributesItem(new AddEntityAttribute().key("LockedBy").stringValue("asd")));

      // when
      try {
        this.entityApi.addEntity("Checkout", req, siteId, "preset");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"message\":\"'Checkout' entity type does not " + "support attributes in request\"}",
            e.getResponseBody());
      }
    }
  }

  private void validateCheckoutAttributes(final String siteId) {
    List<Attribute> attributes =
        notNull(new GetAttributeRequestBuilder().submit(client, siteId).response().getAttributes());
    assertEquals(2, attributes.size());
    assertEquals("LockedBy", attributes.get(0).getKey());
    assertEquals("LockedDate", attributes.get(1).getKey());
  }

  /**
   * Post /entities/{entityTypeId} for missing Checkout EntityType.
   *
   */
  @Test
  public void testAddEntityCheckoutMissing() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String name = "MyCheckout_" + ID.uuid();
      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name(name));

      // when
      try {
        this.entityApi.addEntity("Checkout", req, siteId, "preset");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"entityTypeId\","
            + "\"error\":\"EntityType 'Checkout' is not found\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities/{entityTypeId} for missing LLM Prompt.
   *
   */
  @Test
  public void testAddEntityLlmPromptMissing() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addLlmPromptEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("MyPrompt"));

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId, "preset");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"userPrompt\",\"error\":\"'userPrompt' is required\"}]}",
            e.getResponseBody());
      }

      // when - using EntityType Name.
      try {
        this.entityApi.addEntity("LlmPrompt", req, siteId, "preset");
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"userPrompt\",\"error\":\"'userPrompt' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Get /entities/{entityTypeId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetEntity01() throws Exception {
    // given
    final int count = 10;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      String entityTypeId = addEntityType(siteId);

      for (int i = 0; i < count; i++) {
        AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity_" + i));
        this.entityApi.addEntity(entityTypeId, req, siteId, null);
      }

      String limit = "2";

      // when
      GetEntitiesResponse response =
          this.entityApi.getEntities(entityTypeId, siteId, null, null, limit);

      // then
      assertNotNull(response.getNext());

      List<Entity> entityTypes = notNull(response.getEntities());
      assertEquals(2, entityTypes.size());
      assertEntityEquals(entityTypes.get(0), "myentity_0");
      assertEntityEquals(entityTypes.get(1), "myentity_1");

      // when
      response = this.entityApi.getEntities(entityTypeId, siteId, null, response.getNext(), limit);

      // then
      entityTypes = notNull(response.getEntities());
      assertEquals(2, entityTypes.size());
      assertEntityEquals(entityTypes.get(0), "myentity_2");
      assertEntityEquals(entityTypes.get(1), "myentity_3");

      // when
      response = this.entityApi.getEntities("Company", siteId, "CUSTOM", null, limit);

      // then
      entityTypes = notNull(response.getEntities());
      assertEquals(2, entityTypes.size());
    }
  }

  /**
   * GET /entities/{entityTypeId}/{entityId}. Invalid entityId.
   *
   */
  @Test
  public void testGetEntity02() {
    // given
    String id = ID.uuid();
    setBearerToken(new String[] {DEFAULT_SITE_ID});

    // when
    try {
      this.entityApi.getEntity(id, id, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"entity '" + id + "' not found\"}", e.getResponseBody());
    }
  }

  /**
   * GET /entities/{entityTypeId}/{entityId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetEntity03() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      String entityTypeId = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity"));
      String entityId = this.entityApi.addEntity(entityTypeId, req, siteId, null).getEntityId();
      assertNotNull(entityId);

      for (String entityType : List.of(entityTypeId, "Company")) {
        // when
        GetEntityResponse response =
            this.entityApi.getEntity(entityType, entityId, siteId, "CUSTOM");

        // then
        assertNotNull(response.getEntity());
        assertEntityEquals(response.getEntity(), "myentity");
      }
    }
  }

  /**
   * GET /entities/{entityTypeId}/{entityId} with attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetEntity04() throws Exception {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});
    addAttribute(siteId, "s", AttributeDataType.STRING);
    addAttribute(siteId, "ss", AttributeDataType.STRING);
    addAttribute(siteId, "n", AttributeDataType.NUMBER);
    addAttribute(siteId, "nn", AttributeDataType.NUMBER);
    addAttribute(siteId, "b", AttributeDataType.BOOLEAN);
    addAttribute(siteId, "ko", AttributeDataType.KEY_ONLY);

    String entityTypeId = addEntityType(siteId);

    AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity")
        .addAttributesItem(new AddEntityAttribute().key("b").booleanValue(true))
        .addAttributesItem(new AddEntityAttribute().key("s").stringValue("123"))
        .addAttributesItem(new AddEntityAttribute().key("ko"))
        .addAttributesItem(new AddEntityAttribute().key("ss").stringValues(List.of("1", "2")))
        .addAttributesItem(new AddEntityAttribute().key("nn")
            .numberValues(List.of(new BigDecimal("444"), new BigDecimal("555"))))
        .addAttributesItem(new AddEntityAttribute().key("n").numberValue(new BigDecimal("222"))));

    String entityId = this.entityApi.addEntity(entityTypeId, req, siteId, null).getEntityId();
    assertNotNull(entityId);

    // when
    GetEntityResponse response = this.entityApi.getEntity("Company", entityId, siteId, "CUSTOM");

    // then
    assertNotNull(response.getEntity());
    assertEntityEquals(response.getEntity(), "myentity");

    List<EntityAttribute> attributes0 = notNull(response.getEntity().getAttributes());

    List<Entity> entities =
        notNull(this.entityApi.getEntities(entityTypeId, siteId, null, null, null).getEntities());
    assertFalse(entities.isEmpty());
    List<EntityAttribute> attributes1 = notNull(entities.get(0).getAttributes());

    for (List<EntityAttribute> attributes : List.of(attributes0, attributes1)) {

      final int expected = 6;
      assertEquals(expected, attributes.size());

      int i = 0;
      assertEntityAttributeBoolean(attributes.get(i++));
      assertEntityAttributeKeyOnly(attributes.get(i++));
      assertEntityAttributeNumber(attributes.get(i++));
      assertEntityAttributeNumbers(attributes.get(i++));
      assertEntityAttributeString(attributes.get(i++), "s", "123");
      assertEntityAttributeStrings(attributes.get(i));
    }
  }

  /**
   * PATCH /entities/{entityTypeId}/{entityId} with attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testUpdateEntity01() throws Exception {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});
    addAttribute(siteId, "s", AttributeDataType.STRING);
    addAttribute(siteId, "ss", AttributeDataType.STRING);

    String entityTypeId = addEntityType(siteId);

    AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity")
        .addAttributesItem(new AddEntityAttribute().key("s").stringValue("555")));

    String entityId = this.entityApi.addEntity(entityTypeId, req, siteId, null).getEntityId();
    assertNotNull(entityId);

    // when
    GetEntityResponse response = this.entityApi.getEntity(entityTypeId, entityId, siteId, null);

    // then
    assertNotNull(response.getEntity());
    assertEntityEquals(response.getEntity(), "myentity");

    List<EntityAttribute> attributes = notNull(response.getEntity().getAttributes());
    assertEquals(1, attributes.size());
    assertEntityAttributeString(attributes.get(0), "s", "555");

    // given
    for (String entityName : Arrays.asList("myentity2", null)) {
      UpdateEntityRequest updateReq = new UpdateEntityRequest().entity(new AddEntity()
          .name(entityName).addAttributesItem(new AddEntityAttribute().key("s").stringValue("777"))
          .addAttributesItem(new AddEntityAttribute().key("ss").stringValue("678")));

      // when
      UpdateResponse updateResponse =
          this.entityApi.updateEntity(entityTypeId, entityId, updateReq, siteId, null);

      // then
      assertEquals("Entity updated", updateResponse.getMessage());
      response = this.entityApi.getEntity(entityTypeId, entityId, siteId, null);
      assertNotNull(response.getEntity());
      assertEntityEquals(response.getEntity(), "myentity2");

      attributes = notNull(response.getEntity().getAttributes());
      assertEquals(2, attributes.size());
      assertEntityAttributeString(attributes.get(0), "s", "777");
      assertEntityAttributeString(attributes.get(1), "ss", "678");
    }
  }

  /**
   * PATCH /entities/{entityTypeId}/{entityId} not found.
   *
   */
  @Test
  public void testUpdateEntity02() {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});
    String entityTypeId = ID.uuid();
    String entityId = ID.uuid();

    UpdateEntityRequest updateReq = new UpdateEntityRequest().entity(new AddEntity().name("test"));

    // when
    try {
      this.entityApi.updateEntity(entityTypeId, entityId, updateReq, siteId, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"Entity '" + entityId + "' not found\"}", e.getResponseBody());
    }
  }

  private String addEntityType(final String siteId) throws ApiException {
    String entityTypeId = this.entityApi
        .addEntityType(new AddEntityTypeRequest().entityType(
            new AddEntityType().name("Company").namespace(EntityTypeNamespace.CUSTOM)), siteId)
        .getEntityTypeId();
    assertNotNull(entityTypeId);
    return entityTypeId;
  }

  private String addLlmPromptEntityType(final String siteId) throws ApiException {
    String entityTypeId =
        this.entityApi.addEntityType(
            new AddEntityTypeRequest().entityType(
                new AddEntityType().name("LlmPrompt").namespace(EntityTypeNamespace.PRESET)),
            siteId).getEntityTypeId();
    assertNotNull(entityTypeId);

    addAttribute(siteId, "userPrompt", AttributeDataType.STRING);

    return entityTypeId;
  }

  private String addCheckoutEntityType(final String siteId) throws ApiException {
    String entityTypeId = this.entityApi
        .addEntityType(new AddEntityTypeRequest().entityType(
            new AddEntityType().name("Checkout").namespace(EntityTypeNamespace.PRESET)), siteId)
        .getEntityTypeId();
    assertNotNull(entityTypeId);

    return entityTypeId;
  }

  private void addAttribute(final String siteId, final String attributeKey,
      final AttributeDataType dataType) throws ApiException {
    this.attributesApi.addAttribute(new AddAttributeRequest()
        .attribute(new AddAttribute().key(attributeKey).dataType(dataType)), siteId);
  }

  private void assertEntityAttributeStrings(final EntityAttribute attribute) {
    assertEquals("ss", attribute.getKey());
    assertEquals(AttributeValueType.STRING, attribute.getValueType());
    assertEquals("1,2", String.join(",", notNull(attribute.getStringValues())));
  }

  private void assertEntityAttributeString(final EntityAttribute attribute, final String key,
      final String value) {
    assertEquals(key, attribute.getKey());
    assertEquals(AttributeValueType.STRING, attribute.getValueType());
    assertEquals(value, attribute.getStringValue());
  }

  private void assertEntityAttributeKeyOnly(final EntityAttribute attribute) {
    assertEquals("ko", attribute.getKey());
    assertEquals(AttributeValueType.KEY_ONLY, attribute.getValueType());
    assertNull(attribute.getBooleanValue());
    assertNull(attribute.getStringValue());
    assertTrue(notNull(attribute.getStringValues()).isEmpty());
    assertNull(attribute.getNumberValue());
    assertTrue(notNull(attribute.getNumberValues()).isEmpty());
  }

  private void assertEntityAttributeBoolean(final EntityAttribute attribute) {
    assertEquals("b", attribute.getKey());
    assertEquals(AttributeValueType.BOOLEAN, attribute.getValueType());
    assertEquals(Boolean.TRUE, attribute.getBooleanValue());
  }

  private void assertEntityAttributeNumber(final EntityAttribute attribute) {
    assertEquals("n", attribute.getKey());
    assertEquals(AttributeValueType.NUMBER, attribute.getValueType());
    assertEquals("222.0", String.valueOf(attribute.getNumberValue()));
  }

  private void assertEntityAttributeNumbers(final EntityAttribute attribute) {
    assertEquals("nn", attribute.getKey());
    assertEquals(AttributeValueType.NUMBER, attribute.getValueType());
    assertEquals("444.0,555.0", notNull(attribute.getNumberValues()).stream().map(String::valueOf)
        .collect(Collectors.joining(",")));
  }

  /**
   * DELETE /entities/{entityTypeId}/{entityId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteEntity01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId = addEntityType(siteId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity"));
      String entityId = this.entityApi.addEntity(entityTypeId, req, siteId, null).getEntityId();
      assertNotNull(entityId);

      // when
      try {
        this.entityApi.deleteEntityType(entityTypeId, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"entityId\","
            + "\"error\":\"Entities attached to Entity type\"}]}", e.getResponseBody());
      }

      // when
      DeleteResponse deleteResponse = this.entityApi.deleteEntity(entityTypeId, entityId, siteId);

      // then
      assertEquals("Entity deleted", deleteResponse.getMessage());

      // when
      deleteResponse = this.entityApi.deleteEntityType(entityTypeId, siteId);

      // then
      assertEquals("EntityType deleted", deleteResponse.getMessage());
    }
  }

  private void assertEntityEquals(final Entity entity, final String name) {
    assertEquals(name, entity.getName());
    assertNotNull(entity.getEntityTypeId());
    assertNotNull(entity.getInsertedDate());
    assertNotNull(DynamoDbTypes.toDate(entity.getInsertedDate()));
  }
}
