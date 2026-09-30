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
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityAttribute;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntitiesResponse;
import com.formkiq.client.model.GetEntityResponse;
import com.formkiq.client.model.SetEntityRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.UpdateEntityRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.entity.AddEntityTypeRequestBuilder;
import com.formkiq.testutils.api.entity.DeleteEntityTypeRequestBuilder;
import com.formkiq.testutils.api.entity.GetEntitiesRequestBuilder;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentAttributesRequestBuilder;
import com.formkiq.testutils.api.entity.AddEntityRequestBuilder;
import com.formkiq.testutils.api.entity.DeleteEntityAttributeRequestBuilder;
import com.formkiq.testutils.api.entity.DeleteEntityRequestBuilder;
import com.formkiq.testutils.api.entity.GetEntityRequestBuilder;
import com.formkiq.testutils.api.entity.UpdateEntityRequestBuilder;
import com.formkiq.testutils.api.schemas.SetSchemaDocumentRequestBuilder;
import com.formkiq.testutils.api.entity.SetEntityRequestBuilder;
import com.formkiq.urls.HttpStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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

  private void addAttribute(final String siteId, final String attributeKey,
      final AttributeDataType dataType) throws ApiException {
    new AddAttributeRequestBuilder()
        .request(new AddAttributeRequest()
            .attribute(new AddAttribute().key(attributeKey).dataType(dataType)))
        .submitOk(this.client, siteId);
  }

  private String addEntityType(final String siteId) throws ApiException {
    String entityTypeId = new AddEntityTypeRequestBuilder()
        .request(new AddEntityTypeRequest()
            .entityType(new AddEntityType().name("Company").namespace(EntityTypeNamespace.CUSTOM)))
        .submitOk(this.client, siteId).response().getEntityTypeId();
    assertNotNull(entityTypeId);
    return entityTypeId;
  }

  private void assertEntity(final Entity entity, final String name, final String keys,
      final String stringValues) {
    assertEquals(name, entity.getName());
    assertEquals(keys, notNull(entity.getAttributes()).stream().map(EntityAttribute::getKey)
        .collect(Collectors.joining(",")));
    assertEquals(stringValues, notNull(entity.getAttributes()).stream()
        .map(EntityAttribute::getStringValue).collect(Collectors.joining(",")));
  }

  private void assertEntityAttributeBoolean(final EntityAttribute attribute) {
    assertEquals("b", attribute.getKey());
    assertEquals(AttributeValueType.BOOLEAN, attribute.getValueType());
    assertEquals(Boolean.TRUE, attribute.getBooleanValue());
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

  private void assertEntityAttributeString(final EntityAttribute attribute, final String key,
      final String value) {
    assertEquals(key, attribute.getKey());
    assertEquals(AttributeValueType.STRING, attribute.getValueType());
    assertEquals(value, attribute.getStringValue());
  }

  private void assertEntityAttributeStrings(final EntityAttribute attribute) {
    assertEquals("ss", attribute.getKey());
    assertEquals(AttributeValueType.STRING, attribute.getValueType());
    assertEquals("1,2", String.join(",", notNull(attribute.getStringValues())));
  }

  private void assertEntityEquals(final Entity entity, final String name) {
    assertEquals(name, entity.getName());
    assertNotNull(entity.getEntityTypeId());
    assertNotNull(entity.getInsertedDate());
    assertNotNull(DynamoDbTypes.toDate(entity.getInsertedDate()));
  }

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
        AddEntityResponse addEntityResponse = new AddEntityRequestBuilder(entityTypeId, "custom")
            .request(req).submitOk(this.client, siteId).response();

        // then
        assertNotNull(addEntityResponse.getEntityId());

        GetEntityResponse entity =
            new GetEntityRequestBuilder(entityTypeId, "CUSTOM", addEntityResponse.getEntityId())
                .submitOk(this.client, siteId).response();
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
        new AddEntityRequestBuilder(entityTypeId, null).request(req).submitOk(this.client, siteId);
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
        new AddEntityRequestBuilder(entityTypeId, null).request(req).submitOk(this.client, siteId);
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
        new AddEntityRequestBuilder(entityTypeId, "CUSTOM").request(req).submitOk(this.client,
            siteId);
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
        new AddEntityRequestBuilder(entityTypeId, "CUSTOM").request(req).submitOk(this.client,
            siteId);
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
   * Post /entities/{entityTypeId}. Entity Attribute Data Type.
   *
   */
  @Test
  public void testAddEntity06() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});
      addAttribute(siteId, "myentity", AttributeDataType.ENTITY);

      String entityTypeId = addEntityType(siteId);

      // when
      var resp = new AddEntityRequestBuilder(entityTypeId, "custom").name("test")
          .addAttribute("myentity", "123").submit(client, siteId);

      // then
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"stringValue\","
              + "\"error\":\"invalid 'stringValue' for Entity\"}]}",
          resp.exception().getResponseBody());

      // given
      var stringValue = "asd#123";

      // when
      resp = new AddEntityRequestBuilder(entityTypeId, "custom").name("test")
          .addAttribute("myentity", stringValue).submit(client, siteId);

      // then
      assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"entityTypeId\",\"error\":\"EntityTypeId does not exist\"},"
              + "{\"key\":\"entityId\",\"error\":\"EntityId does not exist\"}]}",
          resp.exception().getResponseBody());
    }
  }

  /**
   * Post /entities/{entityTypeId}. Link two entities with Entity Attribute Data Type.
   *
   */
  @Test
  public void testAddEntity07() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});
      addAttribute(siteId, "myentity", AttributeDataType.ENTITY);

      String entityTypeId = addEntityType(siteId);

      // when
      var resp0 = new AddEntityRequestBuilder(entityTypeId, "custom").name("test0")
          .submit(client, siteId).throwIfError();

      // then
      assertNotNull(resp0.response());
      String entityId = resp0.response().getEntityId();

      // when
      var resp1 = new AddEntityRequestBuilder(entityTypeId, "custom").name("test1")
          .addAttribute("myentity", entityTypeId + "#" + entityId).submit(client, siteId)
          .throwIfError();

      // then
      assertNotNull(resp1.response());
      assertNotNull(resp1.response().getEntityId());
    }
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
        new AddEntityRequestBuilder("Checkout", "preset").request(req).submitOk(this.client,
            siteId);
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
      String entityId = new AddEntityRequestBuilder(entityTypeId, null).request(req)
          .submitOk(this.client, siteId).response().getEntityId();
      assertNotNull(entityId);

      // when
      try {
        new DeleteEntityTypeRequestBuilder(entityTypeId).submitOk(this.client, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"entityId\","
            + "\"error\":\"Entities attached to Entity type\"}]}", e.getResponseBody());
      }

      // when
      DeleteResponse deleteResponse = new DeleteEntityRequestBuilder(entityTypeId, entityId)
          .submitOk(this.client, siteId).response();

      // then
      assertEquals("Entity deleted", deleteResponse.getMessage());

      // when
      deleteResponse =
          new DeleteEntityTypeRequestBuilder(entityTypeId).submitOk(this.client, siteId).response();

      // then
      assertEquals("EntityType deleted", deleteResponse.getMessage());
    }
  }

  /**
   * DELETE /entities/{entityTypeId}/{entityId}. Invalid entityId.
   *
   */
  @Test
  public void testDeleteEntity02() {
    // given
    String siteId = ID.uuid();
    setBearerToken(new String[] {siteId});

    String entityTypeId = ID.uuid();
    String entityId = ID.uuid();

    // when
    try {
      new DeleteEntityRequestBuilder(entityTypeId, entityId).submitOk(this.client, siteId);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"entity '" + entityId + "' not found\"}", e.getResponseBody());
    }
  }

  /**
   * DELETE /entities/{entityTypeId}/{entityId} when used as a schema default entity.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testDeleteEntity03() throws ApiException {
    // given
    String siteId = ID.uuid();
    setBearerToken(new String[] {siteId});

    addAttribute(siteId, "company", AttributeDataType.ENTITY);

    var entityTypeId = addEntityType(siteId);
    var entityId =
        new AddEntityRequestBuilder(entityTypeId).name("myentity").getEntityId(client, siteId);

    new SetSchemaDocumentRequestBuilder("joe")
        .addRequiredEntityAttribute("company", entityTypeId, entityId).submitOk(client, siteId);

    // when
    var document = new AddDocumentRequestBuilder().content().getDocument(client, siteId);

    // then
    var documentAttributes =
        new GetDocumentAttributesRequestBuilder(document).getAttributes(client, siteId);

    assertEquals(1, documentAttributes.size());
    DocumentAttribute attribute = documentAttributes.getFirst();
    assertEquals("company", attribute.getKey());
    assertEquals(AttributeValueType.ENTITY, attribute.getValueType());
    assertEquals(entityTypeId + "#" + entityId, attribute.getStringValue());
    assertNotNull(attribute.getEntity());
    assertEquals("myentity", attribute.getEntity().getName());

    // when
    var resp = new DeleteEntityRequestBuilder(entityTypeId, entityId).submitError(client, siteId);

    // then
    assertEquals(HttpStatus.BAD_REQUEST, resp.exception().getCode());
    assertEquals(
        "{\"errors\":[{\"key\":\"entityId\",\"error\":\"entity '" + entityId
            + "' is used in a Schema / Classification, cannot be deleted\"}]}",
        resp.exception().getResponseBody());
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
        new AddEntityRequestBuilder(entityTypeId, null).request(req).submitOk(this.client, siteId);
      }

      String limit = "2";

      // when
      GetEntitiesResponse response = new GetEntitiesRequestBuilder(entityTypeId, null).next(null)
          .limit(limit).submitOk(this.client, siteId).response();

      // then
      assertNotNull(response.getNext());

      List<Entity> entityTypes = notNull(response.getEntities());
      assertEquals(2, entityTypes.size());
      assertEntityEquals(entityTypes.get(0), "myentity_0");
      assertEntityEquals(entityTypes.get(1), "myentity_1");

      // when
      response = new GetEntitiesRequestBuilder(entityTypeId, null).next(response.getNext())
          .limit(limit).submitOk(this.client, siteId).response();

      // then
      entityTypes = notNull(response.getEntities());
      assertEquals(2, entityTypes.size());
      assertEntityEquals(entityTypes.get(0), "myentity_2");
      assertEntityEquals(entityTypes.get(1), "myentity_3");

      // when
      response = new GetEntitiesRequestBuilder("Company", "CUSTOM").next(null).limit(limit)
          .submitOk(this.client, siteId).response();

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
      new GetEntityRequestBuilder(id, null, id).submitOk(this.client, null);
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
      String entityId = new AddEntityRequestBuilder(entityTypeId, null).request(req)
          .submitOk(this.client, siteId).response().getEntityId();
      assertNotNull(entityId);

      for (String entityType : List.of(entityTypeId, "Company")) {
        // when
        GetEntityResponse response = new GetEntityRequestBuilder(entityType, "CUSTOM", entityId)
            .submitOk(this.client, siteId).response();

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

    String entityId = new AddEntityRequestBuilder(entityTypeId, null).request(req)
        .submitOk(this.client, siteId).response().getEntityId();
    assertNotNull(entityId);

    // when
    GetEntityResponse response = new GetEntityRequestBuilder("Company", "CUSTOM", entityId)
        .submitOk(this.client, siteId).response();

    // then
    assertNotNull(response.getEntity());
    assertEntityEquals(response.getEntity(), "myentity");

    List<EntityAttribute> attributes0 = notNull(response.getEntity().getAttributes());

    List<Entity> entities = notNull(new GetEntitiesRequestBuilder(entityTypeId, null).next(null)
        .limit(null == null ? 100 : Integer.parseInt(null)).submitOk(this.client, siteId).response()
        .getEntities());
    assertFalse(entities.isEmpty());
    List<EntityAttribute> attributes1 = notNull(entities.getFirst().getAttributes());

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
   * PUT /entities/{entityTypeId}/{entityId} createIfMissing.
   *
   * @throws ApiException an error has occurred
   */
  @Test
  public void testSetEntity01() throws ApiException {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});

    String entityTypeId = addEntityType(siteId);
    String entityId = ID.uuid();
    SetEntityRequest setReq = new SetEntityRequest().entity(new AddEntity().name("setentity"));

    // when
    try {
      new SetEntityRequestBuilder().withEntityTypeId(entityTypeId).withEntityId(entityId)
          .withSetEntityRequest(setReq).withNamespace(null).withCreateIfMissing(false)
          .submitOk(this.client, siteId);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"Entity '" + entityId + "' not found\"}", e.getResponseBody());
    }

    // when
    SetResponse setResponse = new SetEntityRequestBuilder().withEntityTypeId(entityTypeId)
        .withEntityId(entityId).withSetEntityRequest(setReq).withNamespace(null)
        .withCreateIfMissing(true).submitOk(this.client, siteId).response();

    // then
    assertEquals("Entity set", setResponse.getMessage());

    GetEntityResponse response = new GetEntityRequestBuilder(entityTypeId, null, entityId)
        .submitOk(this.client, siteId).response();
    assertNotNull(response.getEntity());
    assertEntityEquals(response.getEntity(), "setentity");
    assertEquals(entityId, response.getEntity().getEntityId());
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

    String entityId = new AddEntityRequestBuilder(entityTypeId, null).request(req)
        .submitOk(this.client, siteId).response().getEntityId();
    assertNotNull(entityId);

    // when
    GetEntityResponse response = new GetEntityRequestBuilder(entityTypeId, null, entityId)
        .submitOk(this.client, siteId).response();

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
      UpdateResponse updateResponse = new UpdateEntityRequestBuilder(entityTypeId, null, entityId)
          .request(updateReq).submitOk(this.client, siteId).response();

      // then
      assertEquals("Entity updated", updateResponse.getMessage());
      response = new GetEntityRequestBuilder(entityTypeId, null, entityId)
          .submitOk(this.client, siteId).response();
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
      new UpdateEntityRequestBuilder(entityTypeId, null, entityId).request(updateReq)
          .submitOk(this.client, siteId);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"Entity '" + entityId + "' not found\"}", e.getResponseBody());
    }
  }

  /**
   * PATCH /entities/{entityTypeId}/{entityId} name with existing attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testUpdateEntity03() throws Exception {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});
    addAttribute(siteId, "s", AttributeDataType.STRING);

    String entityTypeId = addEntityType(siteId);

    // when
    var addEntity = new AddEntityRequestBuilder(entityTypeId).name("mine").addAttribute("s", "555")
        .submit(client, siteId).throwIfError();

    // then
    String entityId = addEntity.response().getEntityId();
    assertNotNull(entityId);

    // when
    var resp = new UpdateEntityRequestBuilder(entityTypeId, entityId).name("newname")
        .submit(client, siteId).throwIfError();

    // then
    assertEquals("Entity updated", resp.response().getMessage());

    var get = new GetEntityRequestBuilder(entityTypeId, null, entityId).submit(client, siteId)
        .throwIfError().response();
    var entity = get.getEntity();
    assertNotNull(entity);
    assertEntity(entity, "newname", "s", "555");
  }

  /**
   * PATCH /entities/{entityTypeId}/{entityId} name with add/update/delete attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testUpdateEntity04() throws Exception {
    // given
    String siteId = ID.uuid();

    setBearerToken(new String[] {siteId});
    addAttribute(siteId, "s", AttributeDataType.STRING);
    addAttribute(siteId, "ss", AttributeDataType.STRING);

    String entityTypeId = addEntityType(siteId);

    // when
    var addEntity = new AddEntityRequestBuilder(entityTypeId).name("mine").addAttribute("s", "555")
        .submit(client, siteId).throwIfError();

    // then
    String entityId = addEntity.response().getEntityId();
    assertNotNull(entityId);

    // when
    var resp = new UpdateEntityRequestBuilder(entityTypeId, entityId).addAttribute("ss", "111")
        .addAttribute("s", "222").submit(client, siteId).throwIfError();

    // then
    assertEquals("Entity updated", resp.response().getMessage());

    var get = new GetEntityRequestBuilder(entityTypeId, null, entityId).submit(client, siteId)
        .throwIfError().response();
    var entity = get.getEntity();
    assertNotNull(entity);
    assertEntity(entity, "mine", "s,ss", "222,111");

    // when
    new DeleteEntityAttributeRequestBuilder(entityTypeId, entityId, "ss").submit(client, siteId)
        .throwIfError();

    // then
    assertEquals("Entity updated", resp.response().getMessage());

    get = new GetEntityRequestBuilder(entityTypeId, null, entityId).submit(client, siteId)
        .throwIfError().response();
    entity = get.getEntity();
    assertNotNull(entity);
    assertEntity(entity, "mine", "s", "222");
  }

}
