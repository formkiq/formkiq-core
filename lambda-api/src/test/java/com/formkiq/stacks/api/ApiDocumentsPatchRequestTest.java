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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
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
   * POST /documents request Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // given
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);

      assertTrue(getLogger()
          .containsString("setting userId: 8a73dfef-26d3-43d8-87aa-b3ec358e43ba@formkiq.com "
              + "contentType: application/pdf"));
    }
  }

  /**
   * POST /documents request Base64 body. Document does not exist.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocuments02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      assert404Response(response);
    }
  }

  /**
   * POST /documents request non Base64 body.
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
   * POST /documents request In Readonly.
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

      assertEquals("{\"message\":\"fkq access denied (groups: default_read)\"}", m.get("body"));

      assertTrue(getLogger()
          .containsString("response: {\"headers\":{\"Access-Control-Allow-Origin\":\"*\","
              + "\"Access-Control-Allow-Methods\":\"*\","
              + "\"Access-Control-Allow-Headers\":\"Content-Type,X-Amz-Date,Authorization,"
              + "X-Api-Key\",\"Content-Type\":\"application/json\"},"
              + "\"body\":\"{\\\"message\\\":\\\""
              + "fkq access denied (groups: default_read)\\\"}\"," + "\"statusCode\":403}"));
    }
  }

  /**
   * POST /documents with TAG(s) only.
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

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("{\"tags\":[{\"key\":\"author\",\"value\":\"Bacon\"}]}");
      event.setIsBase64Encoded(Boolean.FALSE);

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
