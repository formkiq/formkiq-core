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
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteIdName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityStatus;
import com.google.gson.Gson;
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

  /**
   * GET /documents/{documentId}/url and /documents/{documentId}/content.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testGetDocumentById() throws IOException {
    // given
    Collection<String> files = List.of("get-documentid-url.json", "get-documentid-content.json");
    for (String file : files) {

      ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/" + file);

      for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

        ApiAuthorization auth = createAuthorization(siteId);

        // when
        UserActivity activity = function.apply(auth, request, null).build();

        // then
        assertNotNull(activity);
        assertEquals("documents", activity.resource());
        assertEquals("HTTP", activity.source());
        assertEquals("view", activity.type());
        assertNull(activity.entityId());
        assertNull(activity.entityNamespace());
        assertNull(activity.entityTypeId());
        assertNull(activity.body());
        assertEquals("03c0737e-2bc8-40f8-8291-797b364d4310", activity.documentId());

        String expectedS3 = "activities/" + getSiteIdName(siteId) + "/documents/year=" + getYear()
            + "/month=" + getMonth() + "/day=" + getDay() + "/" + activity.documentId() + "/";
        assertTrue(activity.s3Key().startsWith(expectedS3));

        assertEquals("1.73.5.111", activity.sourceIpAddress());
        assertEquals(UserActivityStatus.COMPLETE, activity.status());
        assertNull(activity.message());

        final long expectedTime = 1750944882199L;
        assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
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

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).build();

      // then
      assertNotNull(activity);
      assertEquals("documents", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("delete", activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertEquals("test.pdf", activity.documentId());
      assertNotNull(activity.body());

      String expectedS3 = "activities/" + getSiteIdName(siteId) + "/documents/year=" + getYear()
          + "/month=" + getMonth() + "/day=" + getDay() + "/" + activity.documentId() + "/";
      assertTrue(activity.s3Key().startsWith(expectedS3));

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1546105259536L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
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
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/get-documents.json");

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).build();

      // then
      assertNotNull(activity);
      assertEquals("documents", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("view", activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertNull(activity.documentId());
      assertNull(activity.s3Key());
      assertNull(activity.body());

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1750944882199L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
    }
  }

  /**
   * Null {@link ApiGatewayRequestEvent}.
   */
  @Test
  public void testNull() {
    // given
    // when
    UserActivity activity = function.apply(null, null, null).build();

    // then
    assertNotNull(activity);
    assertNull(activity.resource());
    assertEquals("HTTP", activity.source());
    assertNull(activity.type());
    assertNull(activity.entityId());
    assertNull(activity.entityNamespace());
    assertNull(activity.entityTypeId());
    assertNull(activity.documentId());
    assertNull(activity.s3Key());
    assertNull(activity.sourceIpAddress());
    assertEquals(UserActivityStatus.COMPLETE, activity.status());
    assertNull(activity.message());
    assertNull(activity.insertedDate());
  }

  private int getYear() {
    LocalDate date = LocalDate.now(ZoneOffset.UTC);
    return date.getYear();
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

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, null).build();

      // then
      assertNotNull(activity);
      assertEquals("entitytypes", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("view", activity.type());
      assertNull(activity.entityId());
      assertNull(activity.entityNamespace());
      assertNull(activity.entityTypeId());
      assertNull(activity.documentId());
      assertNull(activity.s3Key());
      assertNull(activity.body());

      assertEquals("1.73.5.111", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751589734043L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
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
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/add-entityTypes.json");
    Map<String, Object> body = Map.of("entityTypeId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, response).build();

      // then
      assertNotNull(activity);
      assertEquals("entitytypes", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("create", activity.type());
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

      String expectedS3 = "activities/" + getSiteIdName(siteId) + "/entitytypes/year=" + getYear()
          + "/month=" + getMonth() + "/day=" + getDay() + "/" + activity.entityTypeId() + "/";
      assertTrue(activity.s3Key().startsWith(expectedS3));

      assertEquals("2.7.1.11", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751589856468L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
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
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/add-entity.json");
    Map<String, Object> body = Map.of("entityId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, response).build();

      // then
      assertNotNull(activity);
      assertEquals("entities", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("create", activity.type());
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


      String expectedS3 = "activities/" + getSiteIdName(siteId) + "/entities/"
          + activity.entityTypeId() + "/year=" + getYear() + "/month=" + getMonth() + "/day="
          + getDay() + "/" + activity.entityId() + "/";
      assertTrue(activity.s3Key().startsWith(expectedS3));

      assertEquals("2.7.1.3", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751636384960L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
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
    ApiGatewayRequestEvent request = loadFile("src/test/resources/requests/get-entity.json");
    Map<String, Object> body = Map.of("entityTypeId", ID.uuid());
    ApiRequestHandlerResponse response = new ApiRequestHandlerResponse(-1, null, body);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID, ID.uuid())) {

      ApiAuthorization auth = createAuthorization(siteId);

      // when
      UserActivity activity = function.apply(auth, request, response).build();

      // then
      assertNotNull(activity);
      assertEquals("entities", activity.resource());
      assertEquals("HTTP", activity.source());
      assertEquals("view", activity.type());
      assertNull(activity.body());
      assertNull(activity.entityId());
      assertEquals("CUSTOM", activity.entityNamespace());
      assertEquals("Customer", activity.entityTypeId());

      assertNull(activity.documentId());
      assertNull(activity.s3Key());

      assertEquals("2.7.2.11", activity.sourceIpAddress());
      assertEquals(UserActivityStatus.COMPLETE, activity.status());
      assertNull(activity.message());

      final long expectedTime = 1751636079140L;
      assertEquals(Instant.ofEpochMilli(expectedTime), activity.insertedDate());
    }
  }

  private String getMonth() {
    return LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MM"));
  }

  private String getDay() {
    return LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd"));
  }

  private ApiGatewayRequestEvent loadFile(final String file) throws IOException {
    String json = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
    return gson.fromJson(json, ApiGatewayRequestEvent.class);
  }
}
