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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.api.handler.DocumentIdRequestHandler.FORMKIQ_DOC_EXT;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.aws.services.lambda.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request PATCH /documents/{documentId}. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsPatchRequestTest extends AbstractRequestHandler {

  /**
   * PATCH /documents/{documentId} request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent patchDocumentsRequest(final String siteId, final String documentId,
      final String group, final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("PATCH")
        .resource("/documents/{documentId}").path("/documents/" + documentId).group(group)
        .user("joesmith").pathParameters(Map.of("documentId", documentId))
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
    return event;
  }

  /**
   * PATCH /documents/{documentId} request Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();
      String body = "{\"contentType\":\"application/pdf\",\"path\":\"/documents/test2.txt\"}";

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);

      assertTrue(
          getLogger().containsString("setting userId: joesmith " + "contentType: application/pdf"));
    }
  }

  /**
   * PATCH /documents/{documentId} request Base64 body. Document does not exist.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocuments02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String body = "{\"contentType\":\"application/pdf\",\"path\":\"/documents/test2.txt\"}";
      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // when
      String response = handleRequest(event);

      // then
      assert404Response(response);
    }
  }

  /**
   * PATCH /documents/{documentId} request non Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";
      final long contentLength = 1000;

      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setPath("test.txt");
      item.setContentLength(Long.valueOf(contentLength));
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);

      DocumentItem doc = getDocumentService().findDocument(siteId, documentId);
      assertEquals("test.txt", doc.getPath());
      assertEquals("jsmith", doc.getUserId());
      assertEquals(doc.getLastModifiedDate(), doc.getInsertedDate());

      String key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);
      String s = getS3().getContentAsString(STAGE_BUCKET_NAME, key, null);
      Map<String, Object> map = fromJson(s, Map.class);
      assertEquals(documentId, map.get("documentId"));
      assertEquals("/documents/test2.txt", map.get("path"));
      assertNull(map.get("contentLength"));
    }
  }

  /**
   * PATCH /documents/{documentId} request In Readonly.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      Map<String, String> map = new HashMap<>(getMap());
      map.put("readonly", "true");
      createApiRequestHandler(map);

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid03.json");
      addParameter(event, "siteId", siteId);
      setCognitoGroup(event, "default_read");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      assertEquals("403.0", String.valueOf(m.get("statusCode")));

      assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}", m.get("body"));

      assertTrue(getLogger()
          .containsString("response: {\"headers\":{\"Access-Control-Allow-Origin\":\"*\","
              + "\"Access-Control-Allow-Methods\":\"*\","
              + "\"Access-Control-Allow-Headers\":\"Content-Type,X-Amz-Date,Authorization,"
              + "X-Api-Key\",\"Content-Type\":\"application/json\"},"
              + "\"body\":\"{\\\"message\\\":\\\""
              + "fkq access denied (groups: default (READ))\\\"}\"," + "\"statusCode\":403}"));
    }
  }

  /**
   * PATCH /documents/{documentId} with TAG(s) only.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      String body = "{\"tags\":[{\"key\":\"author\",\"value\":\"Bacon\"}]}";
      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // ApiGatewayRequestEvent event =
      // toRequestEvent("/request-patch-documents-documentid01.json");
      // addParameter(event, "siteId", siteId);
      // setPathParameter(event, "documentId", documentId);
      event.setBody("{\"tags\":[{\"key\":\"author\",\"value\":\"Bacon\"}]}");
      // event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);

      S3Service s3 = getS3();
      String s3key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);

      String json = s3.getContentAsString(STAGE_BUCKET_NAME, s3key, null);
      Map<String, Object> map = fromJson(json, Map.class);
      assertEquals(documentId, map.get("documentId"));

      List<Map<String, String>> tags = (List<Map<String, String>>) map.get("tags");
      assertEquals(1, tags.size());

      assertEquals("USERDEFINED", tags.get(0).get("type"));
      assertEquals("author", tags.get(0).get("key"));
      assertEquals("Bacon", tags.get(0).get("value"));
    }
  }

  /**
   * PATCH /documents/{documentId} Metadata.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setMetadata(Arrays.asList(new DocumentMetadata("person", "something"),
          new DocumentMetadata("playerId", "something")));
      getDocumentService().saveDocument(siteId, doc, null);

      String body = "{\"metadata\":[{\"key\":\"person\",\"value\":\"category\"},"
          + "{\"key\":\"playerId\",\"values\":[\"111\",\"222\"]}]}";
      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);

      String s3key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);

      String json = getS3().getContentAsString(STAGE_BUCKET_NAME, s3key, null);
      Map<String, Object> map = fromJson(json, Map.class);

      List<Map<String, Object>> metadata = (List<Map<String, Object>>) map.get("metadata");
      assertEquals(2, metadata.size());
      assertEquals("person", metadata.get(0).get("key"));
      assertEquals("category", metadata.get(0).get("value"));
      assertEquals("playerId", metadata.get(1).get("key"));
      assertEquals("[111, 222]", metadata.get(1).get("values").toString());
    }
  }

  /**
   * PATCH /documents/{documentId} Metadata too many metadata.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments07() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int count = 30;
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setMetadata(Arrays.asList(new DocumentMetadata("person", "something"),
          new DocumentMetadata("playerId", "something")));
      getDocumentService().saveDocument(siteId, doc, null);

      Map<String, Object> data = new HashMap<>();
      List<Map<String, Object>> metadata = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        metadata.add(Map.of("key", "ad_" + i, "value", "some"));
      }
      data.put("metadata", metadata);

      String body = GsonUtil.getInstance().toJson(data);
      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"errors\":[{\"key\":\"metadata\",\"error\":\"maximum number is 25\"}]}",
          String.valueOf(m.get("body")));
    }
  }

  /**
   * PATCH /documents/{documentId} with invalid TAG.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePatchDocuments08() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      String body = "{\"tags\":[{\"key\":\"CLAMAV_SCAN_TIMESTAMP\",\"value\":\"Bacon\"}]}";
      ApiGatewayRequestEvent event =
          patchDocumentsRequest(siteId, documentId, siteId != null ? siteId : "default", body);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(
          "{\"errors\":[{\"key\":\"CLAMAV_SCAN_TIMESTAMP\",\"error\":\"unallowed tag key\"}]}",
          String.valueOf(m.get("body")));
    }
  }

  /**
   * Asserts 200 response.
   *
   * @param siteId {@link String}
   * @param response {@link String}.
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void assert200Response(final String siteId, final String response) throws IOException {

    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertNotNull(resp.get("documentId"));

    if (siteId != null) {
      assertEquals(siteId, resp.get("siteId"));
    }

    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    String key = siteId != null ? siteId + "/" + resp.get("documentId") + ".fkb64"
        : resp.get("documentId") + ".fkb64";

    assertTrue(
        getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));
    assertNotNull(UUID.fromString(resp.getString("documentId")));
  }

  /**
   * Asserts 404 response.
   *
   * @param response {@link String}.
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void assert404Response(final String response) throws IOException {

    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("404.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    ApiResponseError resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiResponseError.class);

    assertNotNull(resp.getMessage());
    assertNull(resp.getNext());
    assertNull(resp.getPrevious());
  }
}
