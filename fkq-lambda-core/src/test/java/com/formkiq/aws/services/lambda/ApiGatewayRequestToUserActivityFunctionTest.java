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
package com.formkiq.aws.services.lambda;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.dynamodb.useractivities.UserActivityStatus;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit Test for {@link ApiGatewayRequestToUserActivityFunction}.
 */
public class ApiGatewayRequestToUserActivityFunctionTest {

  /** {@link Gson}. */
  private final Gson gson = new Gson();
  /** {@link ApiGatewayRequestToUserActivityFunction}. */
  private final ApiGatewayRequestToUserActivityFunction function =
      new ApiGatewayRequestToUserActivityFunction();

  @BeforeEach
  public void setup() {
    UserActivityContext.clear();
  }

  /**
   * GET /documents/{documentId}/url and /documents/{documentId}/content.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testGetDocumentById() throws IOException {
    // given
    UserActivityContext.set(ActivityResourceType.DOCUMENT, UserActivityType.VIEW, Map.of(),
        Map.of());
    Collection<String> files = List.of("get-documentid-url.json", "get-documentid-content.json");
    for (String file : files) {

      ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/" + file);

      for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

        ApiAuthorization auth = createAuthorization(siteId);

        // when
        UserActivity activity = function.apply(auth, request, null).iterator().next().build(siteId);

        // then
        assertNotNull(activity);
        assertEquals("documents", activity.resource());
        assertEquals("HTTP", activity.source());
        assertEquals(UserActivityType.VIEW, activity.type());
        assertNull(activity.entityId());
        assertNull(activity.entityNamespace());
        assertNull(activity.entityTypeId());
        assertNull(activity.body());
        assertEquals("03c0737e-2bc8-40f8-8291-797b364d4310", activity.documentId());

        assertEquals("1.73.5.111", activity.sourceIpAddress());
        assertEquals(UserActivityStatus.COMPLETE, activity.status());
        assertNull(activity.message());

        final long expectedTime = 1750944882199L;
        assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
        assertEquals(request.getPathParameters().get("documentId"), activity.documentId());
      }
    }
  }

  /**
   * DELETE /documents/{documentId}.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testDeleteDocumentById() throws IOException {
    // given
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/delete-documentid.json");
    UserActivityContext.set(ActivityResourceType.DOCUMENT, UserActivityType.DELETE, Map.of(),
        Map.of());

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("documents", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.DELETE, activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertEquals("test.pdf", activity.documentId());
      assertNotNull(activity.body());

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1546105259536L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
      assertEquals(request.getPathParameters().get("documentId"), activity.documentId());
    }
  }

  /**
   * GET /documents.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testGetDocuments() throws IOException {
    // given
    UserActivityContext.set(ActivityResourceType.DOCUMENT, UserActivityType.VIEW, Map.of(),
        Map.of());
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/get-documents.json");

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("documents", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.VIEW, activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertNull(activity.documentId());
      // assertNull(activity.s3Key());
      assertNull(activity.body());

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1750944882199L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
    }
  }

  /**
   * Null {@link ApiGatewayRequestEvent}.
   */
  @Test
  public void testNull() {
    // given
    // when
    UserActivity activity = function.apply(null, null, null).iterator().next().build(null);

    // then
    assertNotNull(activity);
    assertNull(activity.resource());
    assertEquals("HTTP", activity.source());
    assertNull(activity.type());
    assertNull(activity.entityId());
    assertNull(activity.entityNamespace());
    assertNull(activity.entityTypeId());
    assertNull(activity.documentId());
    assertNull(activity.sourceIpAddress());
    assertEquals(UserActivityStatus.COMPLETE, activity.status());
    assertNull(activity.message());
    assertNotNull(activity.insertedDate());
  }

  /**
   * GET /entityTypes.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testGetEntityTypes() throws IOException {
    // given
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/get-entityTypes.json");
    UserActivityContext.set(ActivityResourceType.ENTITY_TYPE, UserActivityType.VIEW, Map.of(),
        Map.of());

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("entityTypes", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.VIEW, activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertNull(activity.documentId());
      // assertNull(activity.s3Key());
      assertNull(activity.body());

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751589734043L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
    }
  }

  private ApiAuthorization createAuthorization(final String siteId) {
    return new ApiAuthorization().siteId(siteId);
  }

  /**
   * POST /entityTypes.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testAddEntityTypes() throws IOException {
    // given
    UserActivityContext.set(ActivityResourceType.ENTITY_TYPE, UserActivityType.CREATE, Map.of(),
        Map.of());
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/add-entityTypes.json");
    Map<String, Object> body = Map.of("entityTypeId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity =
          function.apply(auth, request, response).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("entityTypes", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.CREATE, activity.type());
      assertEquals("""
          {
            "entityType": {
              "namespace": "CUSTOM",
              "name": "customer"
            }
          }""", activity.body());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertEquals(body.get("entityTypeId"), activity.entityTypeId());
      assertNull(activity.documentId());

      assertEquals("2.7.1.11", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751589856468L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
    }
  }

  /**
   * POST /entities/{entityType}.
   *
   * @throws IOException IOException
   */
  @Test
  public void testAddEntity() throws IOException {
    // given
    UserActivityContext.set(ActivityResourceType.ENTITY, UserActivityType.CREATE, Map.of(),
        Map.of());
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/add-entity.json");
    Map<String, Object> body = Map.of("entityId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity =
          function.apply(auth, request, response).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("entities", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.CREATE, activity.type());
      assertEquals(body.get("entityId"), activity.entityId());
      assertEquals("CUSTOM", activity.entityNamespace());
      assertEquals("Customer", activity.entityTypeId());
      assertNull(activity.documentId());
      assertEquals("""
          {
            "entity": {
              "name": "AcmeInc"
            }
          }""", activity.body());

      assertEquals("2.7.1.3", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751636384960L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
    }
  }

  /**
   * GET /entities/{entityType}.
   *
   * @throws IOException IOException
   */
  @Test
  public void testGetEntitiesByType() throws IOException {
    // given
    UserActivityContext.set(ActivityResourceType.ENTITY, UserActivityType.VIEW, Map.of(), Map.of());
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/get-entity.json");
    Map<String, Object> body = Map.of("entityTypeId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity =
          function.apply(auth, request, response).iterator().next().build(siteId);

      // then
      assertNotNull(activity);
      assertEquals("entities", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals(UserActivityType.VIEW, activity.type());
      assertNull(activity.body());
      assertNull(activity.entityId());
      assertEquals("CUSTOM", activity.entityNamespace());
      assertEquals("Customer", activity.entityTypeId());

      assertNull(activity.documentId());
      // assertNull(activity.s3Key());

      assertEquals("2.7.2.11", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751636079140L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate().toInstant());
    }
  }

  private ApiGatewayRequestEvent loadFile(final String file) throws IOException {
    String json = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
    return gson.fromJson(json, ApiGatewayRequestEvent.class);
  }
}
