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
package com.formkiq.stacks.api;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for request GET / POST / DELETE /documents. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ApiDocumentsRequestTest extends AbstractRequestHandler {

  /**
   * POST /documents request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent postDocumentsRequest(final String siteId, final String group,
      final String body) {
    return new ApiGatewayRequestEventBuilder().method("post").resource("/documents")
        .path("/documents").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
  }

  /**
   * DELETE /documents request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      getS3().putObject(BUCKET_NAME, s3Key, "testdata".getBytes(StandardCharsets.UTF_8), null);

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));

      assertFalse(getS3().getObjectMetadata(BUCKET_NAME, s3Key, null).isObjectExists());
      assertNull(getDocumentService().findDocument(siteId, documentId));
    }
  }

  /**
   * DELETE /documents request that S3 file doesn't exist.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");

      getDocumentService().saveDocument(siteId, item, null);
      assertNotNull(getDocumentService().findDocument(siteId, documentId));

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid02.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertNull(getDocumentService().findDocument(siteId, documentId));

      Map<String, String> m = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * DELETE /documents request. Document doesn't exist.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String documentId = ID.uuid();

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
    }
  }

  /**
   * Test user with no roles with siteid.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments08() throws Exception {
    // given
    String siteId = ID.uuid();

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, "");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    Map<String, Object> resp = fromJson((String) m.get("body"), Map.class);
    assertEquals("fkq access denied to siteId (" + siteId + ")", resp.get("message"));
  }

  /**
   * Test user with User role with siteid.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments09() throws Exception {
    // given
    String siteId = ID.uuid();

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    Map<String, Object> resp = fromJson((String) m.get("body"), Map.class);
    assertEquals("fkq access denied to siteId (" + siteId + ")", resp.get("message"));
  }

  /**
   * Test user with 'Finance' role with no siteid.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments10() throws Exception {
    // given

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", null);
    setCognitoGroup(event, "Finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

    List<DynamicObject> documents = resp.getList("documents");
    assertEquals(0, documents.size());
  }

  /**
   * Test user with 'Finance' role with no siteid and belongs to multiple groups.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments11() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", DEFAULT_SITE_ID);
    setCognitoGroup(event, "Finance Bleh");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    assertEquals("{\"message\":\"fkq access denied to siteId (default)\"}", m.get("body"));
  }

  /**
   * Test user with 'Finance' role with siteid.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments12() throws Exception {
    // given
    String siteId = "Finance";

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

    List<DynamicObject> documents = resp.getList("documents");
    assertEquals(0, documents.size());
  }

  /**
   * Test IAM user API Gateway access.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments13() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents04.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(0, documents.size());
    }
  }

  /**
   * Get /documents request for documents created in previous days.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments14() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final int year = 2020;
      Date date =
          Date.from(LocalDate.of(year, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

      final long contentLength = 1000L;
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);
      item.setContentLength(contentLength);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertTrue(documents.get(0).getString("insertedDate").startsWith("" + year));
      assertTrue(documents.get(0).getString("lastModifiedDate").startsWith("" + year));
    }
  }

  /**
   * Get /documents request with "actionStatus".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments15() throws Exception {

    ActionsService actions = getAwsServices().getExtension(ActionsService.class);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date date = new Date();
      final long contentLength = 1000L;
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);
      item.setContentLength(contentLength);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "actionStatus", "pending");

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());
      actions.saveAction(siteId,
          new Action().index("0").type(ActionType.OCR).documentId(documentId).userId("joe"));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).get("documentId"));
      assertNotNull(documents.get(0).get("insertedDate"));
      assertNotNull(documents.get(0).get("lastModifiedDate"));
      assertNotNull(documents.get(0).get("userId"));
    }
  }

  /**
   * Get /documents request with invalid "actionStatus".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocuments16() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date date = new Date();
      final long contentLength = 1000L;
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);
      item.setContentLength(contentLength);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "actionStatus", "nothing");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"invalid actionStatus 'nothing'\"}", m.get("body"));
    }
  }

  /**
   * Options /documents request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleOptionsDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date date = new Date();
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = ID.uuid();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);

      ApiGatewayRequestEvent event = toRequestEvent("/request-options-documents.json");
      addParameter(event, "siteId", siteId);
      setCognitoGroup(event, siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 2;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
    }
  }

  /**
   * POST /documents request non Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments01() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body =
          "{\"isBase64\":true,\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
              + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
              + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body));

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));
      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(o.getString("documentId"));
      assertNull(o.getString("next"));
      assertNull(o.getString("previous"));

      String documentId = o.getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

      DocumentItem item = getDocumentService().findDocument(siteId, documentId);
      assertEquals("joesmith", item.getUserId());

      String content = getS3().getContentAsString(BUCKET_NAME, key, null);
      assertEquals("this is a test", content);
    }
  }

  /**
   * POST /documents request Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("201.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));

      DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

      assertNotNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));

      String key = siteId != null ? siteId + "/" + resp.get("documentId")
          : resp.get("documentId").toString();

      assertNotNull(getDocumentService().findDocument(siteId, resp.getString("documentId")));
      assertNotNull(UUID.fromString(resp.getString("documentId")));

      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());
      String content = getS3().getContentAsString(BUCKET_NAME, key, null);
      assertEquals("dGhpcyBpcyBhIHRlc3Q=", content);
    }
  }

  /**
   * POST /documents request NO body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid03.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"request body is required\"}", String.valueOf(m.get("body")));
    }
  }

  /**
   * POST /documents request Invalid JSON body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments04() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid04.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

      assertNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * POST /documents request Invalid JSON body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments05() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid05.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

      assertNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * POST /documents request Invalid body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments06() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid06.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      assertCorsHeaders((Map<String, Object>) m.get("headers"));
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"invalid JSON body\"}", String.valueOf(m.get("body")));
    }
  }

  /**
   * POST /documents without Group but has siteId set.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments07() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid07.json");
    addParameter(event, "siteId", "demo");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals("{\"message\":\"fkq access denied to siteId (demo)\"}",
        String.valueOf(m.get("body")));
  }

  /**
   * POST /documents with default & default_read role.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments08() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents01.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
    DynamicObject resp = new DynamicObject(fromJson((String) m.get("body"), Map.class));

    String documentId = resp.get("documentId").toString();
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    assertNotNull(UUID.fromString(documentId));
    assertNotNull(getDocumentService().findDocument(null, documentId));

    assertTrue(getS3().getObjectMetadata(BUCKET_NAME, documentId, null).isObjectExists());
  }

  /**
   * POST /documents with default_read role.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments09() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents02.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertCorsHeaders((Map<String, Object>) m.get("headers"));
  }

  /**
   * POST /documents with sub documents.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments10() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      // when
      DynamicObject obj =
          handleRequest("/request-post-documents03.json", siteId, username, "Admins");

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNotNull(body.getString("uploadUrl"));

      List<DynamicObject> documents = body.getList("documents");
      final int count = 3;
      assertEquals(count, documents.size());

      int i = 0;
      assertNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i++).getString("documentId"));
      assertNotNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i++).getString("documentId"));
      assertNotNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i).getString("documentId"));

      String documentId = documents.get(0).getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());
    }
  }

  /**
   * POST /documents with sub documents.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments11() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      // when
      DynamicObject obj =
          handleRequest("/request-post-documents04.json", siteId, username, "Admins");

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNotNull(body.getString("uploadUrl"));

      List<DynamicObject> documents = body.getList("documents");
      assertEquals(1, documents.size());

      assertNull(documents.get(0).getString("uploadUrl"));
      assertNotNull(documents.get(0).getString("documentId"));

      String key = SiteIdKeyGenerator.createS3Key(siteId, documents.get(0).getString("documentId"));
      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());

      assertNotNull(getDocumentService().findDocument(siteId, body.getString("documentId")));
      assertNotNull(
          getDocumentService().findDocument(siteId, documents.get(0).getString("documentId")));
    }
  }

  /**
   * POST /documents gutenburg content-type with IAM Role.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments12() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given

      // when
      DynamicObject obj = handleRequest("/request-post-documents05.json", siteId, null, null);

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNull(body.getString("uploadUrl"));

      assertEquals("[]", body.getList("documents").toString());

      String documentId = body.getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      assertNotNull(getDocumentService().findDocument(siteId, documentId));

      assertEquals("application/octet-stream",
          getS3().getObjectMetadata(BUCKET_NAME, key, null).getContentType());
    }
  }

  /**
   * POST /documents to create index "folder".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments15() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body = "{\"path\":\"something/bleh/\"}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body);

      // when
      DynamicObject obj = handleRequestDynamic(event);

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNull(o.getString("documentId"));
      assertEquals("folder created", o.getString("message"));

      DocumentSearchService search = getAwsServices().getExtension(DocumentSearchService.class);

      SearchQuery q =
          new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "", null, null, null)).build();
      Pagination<DynamicDocumentItem> results = search.search(siteId, q, null, null, 2);
      assertEquals(1, results.getResults().size());
      assertEquals("something", results.getResults().get(0).get("path"));

      q = new SearchQueryBuilder().meta(new SearchMetaCriteria(null, "something", null, null, null))
          .build();
      results = search.search(siteId, q, null, null, 2);
      assertEquals(1, results.getResults().size());
      assertEquals("bleh", results.getResults().get(0).get("path"));

      // given
      // when
      String response = handleRequest(event);
      obj = new DynamicObject(fromJson(response, Map.class));

      // then
      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"folder\",\"error\":\"already exists\"}]}",
          obj.getString("body"));
    }
  }

  /**
   * POST /documents too many metadata.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments16() throws Exception {
    // given
    final int count = 30;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
          + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";
      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body);

      // when

      Map<String, Object> data = fromJson(event.getBody(), Map.class);
      List<Map<String, Object>> metadata = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        metadata.add(Map.of("key", "ad_" + i, "value", "some"));
      }
      data.put("metadata", metadata);
      event.setBody(GsonUtil.getInstance().toJson(data));

      // when
      String response = handleRequest(event);

      // then
      DynamicObject obj = new DynamicObject(fromJson(response, Map.class));
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=metadata, error=maximum number is 25}]}", o.toString());
    }
  }

  /**
   * POST /documents too large meta data.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments17() throws Exception {
    // given
    final int count = 1001;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      String filled = StringUtils.repeat("*", count);

      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
          + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body);

      Map<String, Object> data = fromJson(event.getBody(), Map.class);
      List<Map<String, Object>> metadata = new ArrayList<>();
      metadata.add(Map.of("key", "ad1", "value", filled));
      metadata.add(Map.of("key", "ad2", "values", List.of(filled)));

      data.put("metadata", metadata);
      event.setBody(GsonUtil.getInstance().toJson(data));

      // when
      String response = handleRequest(event);

      // then
      DynamicObject obj = new DynamicObject(fromJson(response, Map.class));
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=ad1, error=value cannot exceed 1000}, "
          + "{key=ad2, error=value cannot exceed 1000}]}", o.toString());
    }
  }

  /**
   * POST /documents invalid tag.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments18() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"CLAMAV_SCAN_STATUS\",\"value\":\"john\"}]}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body);

      // when
      DynamicObject obj = handleRequestDynamic(event);

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=CLAMAV_SCAN_STATUS, error=unallowed tag key}]}", o.toString());
    }
  }

  /**
   * POST /documents request webhook invalid action.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments19() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"webhook\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"parameters.url\","
          + "\"error\":\"action 'url' parameter is required\"}]}", obj.getString("body"));
    }
  }

  /**
   * POST /documents request webhook valid action.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments20() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"webhook\",\"parameters\":{\"url\":\"http://localhost\"}}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body));

      // then
      assertEquals("201.0", obj.getString("statusCode"));
    }
  }

  /**
   * POST /documents request webhook invalid type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments21() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"something\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"type\",\"error\":\"action 'type' is required\"}]}",
          obj.getString("body"));
    }
  }

  /**
   * POST /documents request missing type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments22() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type2\":\"something\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : DEFAULT_SITE_ID, body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"type\",\"error\":\"action 'type' is required\"}]}",
          obj.getString("body"));
    }
  }
}
