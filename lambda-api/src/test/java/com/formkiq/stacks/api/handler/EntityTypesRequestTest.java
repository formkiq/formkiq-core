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

import com.formkiq.aws.dynamodb.DynamoDbTypes;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.AddEntityTypeResponse;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.EntityType;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntityTypeResponse;
import com.formkiq.client.model.GetEntityTypesResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /entityTypes. */
public class EntityTypesRequestTest extends AbstractApiClientRequestTest {

  /**
   * Post /entityTypes.
   *
   */
  @Test
  public void testAddEntityTypes01() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest();

      // when
      try {
        this.entityApi.addEntityType(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Missing required parameter 'entityType'\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entityTypes missing 'name'.
   *
   */
  @Test
  public void testAddEntityTypes02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest().entityType(new AddEntityType());

      // when
      try {
        this.entityApi.addEntityType(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"name\",\"error\":\"'name' is required\"},"
                + "{\"key\":\"namespace\",\"error\":\"'namespace' is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entityTypes invalid PRESET.
   *
   */
  @Test
  public void testAddEntityTypes03() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest()
          .entityType(new AddEntityType().name("Myentity").namespace(EntityTypeNamespace.PRESET));

      // when
      try {
        this.entityApi.addEntityType(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"namespace\","
            + "\"error\":\"'namespace' unexpected value 'PRESET'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Post /entityTypes CUSTOM.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testAddEntityTypes04() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest()
          .entityType(new AddEntityType().name("Myentity").namespace(EntityTypeNamespace.CUSTOM));

      // when
      AddEntityTypeResponse response = this.entityApi.addEntityType(req, siteId);

      // then
      String entityTypeId0 = response.getEntityTypeId();
      assertNotNull(entityTypeId0);

      for (String entityTypeId : Arrays.asList(entityTypeId0, "Myentity")) {
        GetEntityTypeResponse resp =
            this.entityApi.getEntityType(entityTypeId, siteId, EntityTypeNamespace.CUSTOM.name());
        assertNotNull(resp.getEntityType());
        assertEntityTypeEquals(resp.getEntityType(), "Myentity");
      }

      // when - add same entity
      try {
        this.entityApi.addEntityType(req, siteId);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"name\",\"error\":\"'name' already exists\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Post /entityTypes with invalid name.
   *
   */
  @Test
  public void testAddEntityTypes05() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest()
          .entityType(new AddEntityType().name("da entity").namespace(EntityTypeNamespace.CUSTOM));

      // when
      try {
        this.entityApi.addEntityType(req, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"name\","
            + "\"error\":\"'name' unexpected value 'da entity'\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Get /entityTypes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetEntityTypes01() throws Exception {
    // given
    final int count = 10;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      for (int i = 0; i < count; i++) {
        AddEntityTypeRequest req = new AddEntityTypeRequest().entityType(
            new AddEntityType().name("Myentity" + i).namespace(EntityTypeNamespace.CUSTOM));
        this.entityApi.addEntityType(req, siteId);
      }

      String limit = "2";

      // when
      GetEntityTypesResponse response =
          this.entityApi.getEntityTypes(siteId, "CUSTOM", null, limit);

      // then
      assertNotNull(response.getNext());

      List<EntityType> entityTypes = notNull(response.getEntityTypes());
      assertEquals(2, entityTypes.size());
      assertEntityTypeEquals(entityTypes.get(0), "Myentity0");
      assertEntityTypeEquals(entityTypes.get(1), "Myentity1");

      // when
      response = this.entityApi.getEntityTypes(siteId, "CUSTOM", response.getNext(), limit);

      // then
      entityTypes = notNull(response.getEntityTypes());
      assertEquals(2, entityTypes.size());
      assertEntityTypeEquals(entityTypes.get(0), "Myentity2");
      assertEntityTypeEquals(entityTypes.get(1), "Myentity3");

      // invalid NEXT token
      try {
        this.entityApi.getEntityTypes(siteId, "PRESET", response.getNext(), limit);
        fail();
      } catch (ApiException e) {
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Invalid Next token\"}", e.getResponseBody());
      }

      response = this.entityApi.getEntityTypes(siteId, "PRESET", null, limit);
      assertEquals(0, notNull(response.getEntityTypes()).size());
    }
  }

  /**
   * Get /entityTypes. Invalid namespace.
   *
   */
  @Test
  public void testGetEntityTypes02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      // when
      try {
        this.entityApi.getEntityTypes(siteId, "nothing", null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"namespace\",\"error\":\"'namespace' "
                + "unexpected value 'nothing' must be one of PRESET, CUSTOM\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Get /entityTypes/{entityTypeId}. Invalid entityTypeId.
   *
   */
  @Test
  public void testGetEntityTypes03() {
    // given
    String id = "myentity";
    setBearerToken(new String[] {DEFAULT_SITE_ID});

    // when
    try {
      this.entityApi.getEntityType(id, DEFAULT_SITE_ID, EntityTypeNamespace.CUSTOM.name());
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"entityTypeId\","
          + "\"error\":\"'entityTypeId' attribute is not found\"}]}", e.getResponseBody());
    }
  }

  /**
   * Get /entityTypes/{entityTypeId} by EntityName.
   *
   */
  @Test
  public void testGetEntityTypes04() throws ApiException {
    // given
    setBearerToken(new String[] {DEFAULT_SITE_ID});

    AddEntityTypeRequest req = new AddEntityTypeRequest()
        .entityType(new AddEntityType().name("Company").namespace(EntityTypeNamespace.CUSTOM));
    this.entityApi.addEntityType(req, null);

    // when
    GetEntityTypeResponse entityType =
        this.entityApi.getEntityType("Company", DEFAULT_SITE_ID, EntityTypeNamespace.CUSTOM.name());

    // then
    assertNotNull(entityType.getEntityType());
    assertEntityTypeEquals(entityType.getEntityType(), "Company");
  }

  /**
   * Get /entityTypes/{entityTypeId} . Invalid namespace.
   *
   */
  @Test
  public void testGetEntityTypes05() {
    // given
    String id = "myentity";
    setBearerToken(new String[] {DEFAULT_SITE_ID});

    // when
    try {
      this.entityApi.getEntityType(id, DEFAULT_SITE_ID, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"namespace\",\"error\":\"'namespace' is required\"}]}",
          e.getResponseBody());
    }

    // when
    try {
      this.entityApi.getEntityType(id, DEFAULT_SITE_ID, "adsaf");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
      assertEquals("{\"errors\":[{\"key\":\"namespace\","
          + "\"error\":\"'namespace' unexpected value 'ADSAF'\"}]}", e.getResponseBody());
    }
  }

  /**
   * DELETE /entityTypes/{entityTypeId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteEntityTypes01() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});

      AddEntityTypeRequest req = new AddEntityTypeRequest()
          .entityType(new AddEntityType().name("Myentity").namespace(EntityTypeNamespace.CUSTOM));
      AddEntityTypeResponse response = this.entityApi.addEntityType(req, siteId);
      assertNotNull(response.getEntityTypeId());

      // when
      DeleteResponse deleteResponse =
          this.entityApi.deleteEntityType(response.getEntityTypeId(), siteId);

      // then
      assertEquals("EntityType deleted", deleteResponse.getMessage());
    }
  }

  /**
   * DELETE /entityTypes/{entityTypeId} not found.
   *
   */
  @Test
  public void testDeleteEntityTypes02() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      setBearerToken(new String[] {siteId});
      String id = ID.uuid();

      // when
      try {
        this.entityApi.deleteEntityType(id, siteId);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals(
            "{\"errors\":[{\"key\":\"entityTypeId\"," + "\"error\":\"EntityType not found\"}]}",
            e.getResponseBody());
      }
    }
  }

  private void assertEntityTypeEquals(final EntityType entityType, final String name) {
    assertEquals(name, entityType.getName());
    assertEquals(EntityTypeNamespace.CUSTOM, entityType.getNamespace());
    assertNotNull(entityType.getEntityTypeId());
    assertNotNull(entityType.getInsertedDate());
    assertNotNull(DynamoDbTypes.toDate(entityType.getInsertedDate()));
  }
}
