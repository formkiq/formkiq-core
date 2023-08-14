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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for {@link CoreRequestHandler} class. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiRequestHandlerTest extends AbstractRequestHandler {

  /**
   * Get Document Request, Document not found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetRequest01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid01.json");
      addParameter(event, "siteId", siteId);

      String expected =
          "{" + getHeaders() + ",\"body\":\"{\\\"message\\\":\\\"Document 142 not found.\\\"}\","
              + "\"statusCode\":404}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * Get Document Request, Document found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetRequest02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(documentId, date, userId);
      DocumentMetadata md = new DocumentMetadata("category", "person");
      item.setMetadata(Arrays.asList(md));
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      assertEquals(documentId, resp.getString("documentId"));
      assertEquals(userId, resp.getString("userId"));
      assertNotNull(resp.get("insertedDate"));
      assertNotNull(resp.get("lastModifiedDate"));

      List<DynamicObject> metadata = resp.getList("metadata");
      assertEquals(1, metadata.size());
      assertEquals("{value=person, key=category}", metadata.get(0).toString());

      assertEquals(resp.get("insertedDate"), resp.get("lastModifiedDate"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * Invalid Request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetRequest03() throws Exception {
    // given
    String input = "";
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final InputStream instream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    final String expected = "{" + getHeaders() + ","
        + "\"body\":\"{\\\"message\\\":\\\"Invalid Request\\\"}\",\"statusCode\":404}";

    // when
    getHandler().handleRequest(instream, outstream, getMockContext());

    // then
    assertEquals(expected, new String(outstream.toByteArray(), "UTF-8"));
    assertTrue(getLogger().containsString("response: " + expected));
  }

  /**
   * unknown resource.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetRequest04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-invalid-resource.json");
      addParameter(event, "siteId", siteId);

      String expected = "{" + getHeaders()
          + ",\"body\":\"{\\\"message\\\":\\\"/unknown not found\\\"}\"," + "\"statusCode\":404}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * Get Document Request, Document found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetRequest05() throws Exception {
    // given
    Date date = new Date();
    String documentId = "1a1d1938-451e-4e20-bf95-e0e7a749505a";
    String userId = "jsmith";

    DocumentItem item = new DocumentItemDynamoDb(documentId, date, userId);
    getDocumentService().saveDocument(null, item, new ArrayList<>());

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid02.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertEquals(documentId, resp.get("documentId"));
    assertEquals(userId, resp.get("userId"));
    assertNotNull(resp.get("insertedDate"));
    assertEquals(DEFAULT_SITE_ID, resp.get("siteId"));
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));
  }

  /**
   * Get Document Request, Document with sub docs found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetRequest06() throws Exception {
    // given
    Date date = new Date();
    String documentId0 = "1a1d1938-451e-4e20-bf95-e0e7a749505a";
    String documentId1 = UUID.randomUUID().toString();
    String userId = "jsmith";

    DocumentItem item = new DocumentItemDynamoDb(documentId0, date, userId);
    DocumentItem citem = new DocumentItemDynamoDb(documentId1, date, userId);

    DynamicDocumentItem doc = new DocumentItemToDynamicDocumentItem().apply(item);
    doc.put("documents", Arrays.asList(new DocumentItemToDynamicDocumentItem().apply(citem)));

    getDocumentService().saveDocumentItemWithTag(null, doc);

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid02.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertEquals(documentId0, resp.get("documentId"));
    assertEquals(userId, resp.get("userId"));
    assertNotNull(resp.get("insertedDate"));
    assertEquals(DEFAULT_SITE_ID, resp.get("siteId"));
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    List<Map<String, Object>> children = (List<Map<String, Object>>) resp.get("documents");
    assertEquals(1, children.size());

    assertEquals(documentId1, children.get(0).get("documentId"));
    assertEquals(userId, children.get(0).get("userId"));
    assertNotNull(children.get(0).get("belongsToDocumentId"));
    assertNotNull(children.get(0).get("insertedDate"));
    assertNull(children.get(0).get("siteId"));
  }

  /**
   * /version request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testVersion01() throws Exception {
    // given
    setEnvironment("MODULE_ocr", "true");
    setEnvironment("MODULE_typesense", "true");
    setEnvironment("MODULE_otherone", "false");

    ApiGatewayRequestEvent event = toRequestEvent("/request-version.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    Map<String, Object> resp = fromJson(m.get("body"), Map.class);
    assertEquals("1.1", resp.get("version"));
    assertEquals("core", resp.get("type"));
    assertEquals("[ocr, typesense]", resp.get("modules").toString());
  }

  /**
   * /version request belong to multiple groups.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testVersion02() throws Exception {
    // given
    setEnvironment("MODULE_ocr", "true");
    setEnvironment("MODULE_typesense", "true");
    setEnvironment("MODULE_otherone", "false");

    ApiGatewayRequestEvent event = toRequestEvent("/request-version.json");
    setCognitoGroup(event, "finance", "other");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    Map<String, Object> resp = fromJson(m.get("body"), Map.class);
    assertEquals("1.1", resp.get("version"));
    assertEquals("core", resp.get("type"));
    assertEquals("[ocr, typesense]", resp.get("modules").toString());
  }
}
