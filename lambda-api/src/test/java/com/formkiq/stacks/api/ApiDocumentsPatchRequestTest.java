/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;

/** Unit Tests for request PATCH /documents/{documentId}. */
public class ApiDocumentsPatchRequestTest extends AbstractRequestHandler {

  /**
   * POST /documents request Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePatchDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

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
      newOutstream();
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
  @Test
  public void testHandlePatchDocuments03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assert200Response(siteId, response);
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
      newOutstream();

      // given
      Map<String, String> map = new HashMap<>(getMap());
      map.put("readonly", "true");
      createApiRequestHandler(map);

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-documents-documentid03.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      assertEquals("403.0", String.valueOf(m.get("statusCode")));

      assertEquals("{\"message\":\"Access Denied\"}", m.get("body"));

      assertTrue(getLogger()
          .containsString("response: {\"headers\":{\"Access-Control-Allow-Origin\":\"*\","
              + "\"Access-Control-Allow-Methods\":\"*\","
              + "\"Access-Control-Allow-Headers\":\"Content-Type,X-Amz-Date,Authorization,"
              + "X-Api-Key\",\"Content-Type\":\"application/json\"},"
              + "\"body\":\"{\\\"message\\\":\\\"" + "Access Denied\\\"}\","
              + "\"statusCode\":403}"));
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
    assertEquals(siteId, resp.get("siteId"));
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    String key = siteId != null ? siteId + "/" + resp.get("documentId") + ".fkb64"
        : resp.get("documentId") + ".fkb64";

    assertTrue(
        getLogger().containsString("s3 putObject " + key + " into bucket " + getStages3bucket()));
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
