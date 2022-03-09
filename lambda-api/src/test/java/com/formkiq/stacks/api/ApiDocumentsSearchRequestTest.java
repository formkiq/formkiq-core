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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** Unit Tests for request /search. */
public class ApiDocumentsSearchRequestTest extends AbstractRequestHandler {

  /**
   * Invalid search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search-invalid.json");
      addParameter(event, "siteId", siteId);

      String expected = "{" + getHeaders() + ",\"body\":"
          + "\"{\\\"message\\\":\\\"request body is required\\\"}\"," + "\"statusCode\":400}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * Valid GET search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-search.json");
      addParameter(event, "siteId", siteId);

      final String expected = "{" + getHeaders() + ","
          + "\"body\":\"{\\\"message\\\":\\\"GET for /search not found\\\"}\""
          + ",\"statusCode\":404}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * Valid POST search by eq tagValue.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String tagKey = "category";
      String tagvalue = "person";
      String username = "jsmith";
      Date now = new Date();

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);

      DocumentTag item = new DocumentTag(documentId, tagKey, tagvalue, now, username);
      item.setUserId(UUID.randomUUID().toString());

      getDocumentService().saveDocument(siteId, new DocumentItemDynamoDb(documentId, now, username),
          Arrays.asList(item));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).get("documentId"));
      assertEquals(username, documents.get(0).get("userId"));
      assertNotNull(documents.get(0).get("insertedDate"));

      Map<String, Object> matchedTag = (Map<String, Object>) documents.get(0).get("matchedTag");
      assertEquals("category", matchedTag.get("key"));
      assertEquals("person", matchedTag.get("value"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * Valid POST search by eq tagValues.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String tagKey = "category";
      String username = "jsmith";
      Date now = new Date();

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setBody("ewogICJxdWVyeSI6IHsKICAgICJ0YWciOiB7CiAgICAgICJrZXkiOiAiY2F0Z"
          + "WdvcnkiLAogICAgICAiZXEiOiAieHl6IgogICAgfQogIH0KfQ==");

      DocumentTag item = new DocumentTag(documentId, tagKey, null, now, username);
      item.setValues(Arrays.asList("abc", "xyz"));
      item.setUserId(UUID.randomUUID().toString());

      getDocumentService().saveDocument(siteId, new DocumentItemDynamoDb(documentId, now, username),
          Arrays.asList(item));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).get("documentId"));
      assertEquals(username, documents.get(0).get("userId"));
      assertNotNull(documents.get(0).get("insertedDate"));

      Map<String, Object> matchedTag = (Map<String, Object>) documents.get(0).get("matchedTag");
      assertEquals("category", matchedTag.get("key"));
      assertEquals("xyz", matchedTag.get("value"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }
  
  /**
   * InValid POST search body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();
      // given
      final String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Invalid JSON body.\\\"}\",\"statusCode\":400}";

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
    }
  }
  
  /**
   * Valid POST search no results.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(0, documents.size());
    }
  }
}
