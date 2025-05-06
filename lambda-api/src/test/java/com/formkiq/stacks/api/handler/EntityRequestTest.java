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
import com.formkiq.aws.dynamodb.eventsourcing.DynamoDbTypes;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddEntity;
import com.formkiq.client.model.AddEntityRequest;
import com.formkiq.client.model.AddEntityResponse;
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntitiesResponse;
import com.formkiq.client.model.GetEntityResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /entityTypes. */
public class EntityRequestTest extends AbstractApiClientRequestTest {

  /**
   * Post /entities.
   *
   */
  @Test
  public void testAddEntity01() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId =
          this.entityApi.addEntityType(
              new AddEntityTypeRequest().entityType(
                  new AddEntityType().name("company").namespace(EntityTypeNamespace.CUSTOM)),
              siteId).getEntityTypeId();
      assertNotNull(entityTypeId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("Acme Inc"));

      // when
      AddEntityResponse addEntityResponse = this.entityApi.addEntity(entityTypeId, req, siteId);

      // then
      assertNotNull(addEntityResponse.getEntityId());

      GetEntityResponse entity =
          this.entityApi.getEntity(entityTypeId, addEntityResponse.getEntityId(), siteId, null);
      assertNotNull(entity.getEntity());
      assertEntityEquals(entity.getEntity(), "Acme Inc");
    }
  }

  /**
   * Post /entities. Invalid Request.
   *
   */
  @Test
  public void testAddEntity02() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(new String[] {siteId});

      String entityTypeId =
          this.entityApi.addEntityType(
              new AddEntityTypeRequest().entityType(
                  new AddEntityType().name("company").namespace(EntityTypeNamespace.CUSTOM)),
              siteId).getEntityTypeId();
      assertNotNull(entityTypeId);

      AddEntityRequest req = new AddEntityRequest();

      // when
      try {
        this.entityApi.addEntity(entityTypeId, req, siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Missing 'entity'\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Post /entities. Invalid EntityTypeId.
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
        this.entityApi.addEntity(entityTypeId, req, siteId);
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
   * Get entities/{entityTypeId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetEntity01() throws Exception {
    // given
    final int count = 10;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      String entityTypeId =
          this.entityApi.addEntityType(
              new AddEntityTypeRequest().entityType(
                  new AddEntityType().name("company").namespace(EntityTypeNamespace.CUSTOM)),
              siteId).getEntityTypeId();
      assertNotNull(entityTypeId);

      for (int i = 0; i < count; i++) {
        AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity_" + i));
        this.entityApi.addEntity(entityTypeId, req, siteId);
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
      response = this.entityApi.getEntities("company", siteId, "CUSTOM", null, limit);

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
  public void testGetEntityTypes02() {
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

      String entityTypeId =
          this.entityApi.addEntityType(
              new AddEntityTypeRequest().entityType(
                  new AddEntityType().name("company").namespace(EntityTypeNamespace.CUSTOM)),
              siteId).getEntityTypeId();
      assertNotNull(entityTypeId);

      AddEntityRequest req = new AddEntityRequest().entity(new AddEntity().name("myentity"));
      String entityId = this.entityApi.addEntity(entityTypeId, req, siteId).getEntityId();
      assertNotNull(entityId);

      for (String entityType : List.of(entityTypeId, "company")) {
        // when
        GetEntityResponse response =
            this.entityApi.getEntity(entityType, entityId, siteId, "CUSTOM");

        // then
        assertNotNull(response.getEntity());
        assertEntityEquals(response.getEntity(), "myentity");
      }
    }
  }

  private void assertEntityEquals(final Entity entity, final String name) {
    assertEquals(name, entity.getName());
    assertNotNull(entity.getEntityTypeId());
    assertNotNull(entity.getInsertedDate());
    assertNotNull(DynamoDbTypes.toDate(entity.getInsertedDate()));
  }
}
