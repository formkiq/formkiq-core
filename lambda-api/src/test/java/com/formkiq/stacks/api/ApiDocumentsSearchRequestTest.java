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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.client.models.DocumentSearch;
import com.formkiq.stacks.client.models.DocumentSearchQuery;
import com.formkiq.stacks.client.models.DocumentSearchTag;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /search. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsSearchRequestTest extends AbstractRequestHandler {

  /** Match Tag element count. */
  private static final int MATCH_COUNT = 3;

  /**
   * Invalid search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        String documentId = UUID.randomUUID().toString();
        final String tagKey = "category";
        final String tagvalue = "person";
        final String username = "jsmith";
        final Date now = new Date();

        ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
        addParameter(event, "siteId", siteId);

        DocumentSearch s = new DocumentSearch().query(
            new DocumentSearchQuery().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Arrays.asList(documentId)));
        event.setBody(GsonUtil.getInstance().toJson(s));
        event.setIsBase64Encoded(Boolean.FALSE);

        if ("eqOr".equals(op)) {
          s.query().tag().eq(null).eqOr(Arrays.asList("person"));
        }

        DocumentTag item = new DocumentTag(documentId, tagKey, tagvalue, now, username);
        item.setUserId(UUID.randomUUID().toString());

        getDocumentService().saveDocument(siteId,
            new DocumentItemDynamoDb(documentId, now, username), Arrays.asList(item));

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        assertEquals(getHeaders(),
            "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
        DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

        List<DynamicObject> documents = resp.getList("documents");
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.get(0).get("documentId"));
        assertEquals(username, documents.get(0).get("userId"));
        assertNotNull(documents.get(0).get("insertedDate"));

        Map<String, Object> matchedTag = (Map<String, Object>) documents.get(0).get("matchedTag");
        assertEquals(MATCH_COUNT, matchedTag.size());
        assertEquals("USERDEFINED", matchedTag.get("type"));
        assertEquals("category", matchedTag.get("key"));
        assertEquals("person", matchedTag.get("value"));
        assertNull(resp.get("next"));
        assertNull(resp.get("previous"));
      }
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
      assertEquals(MATCH_COUNT, matchedTag.size());
      assertEquals("USERDEFINED", matchedTag.get("type"));
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

  /**
   * Valid POST search by eq/eqOr tagValue and valid/invalid DocumentId.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest07() throws Exception {
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        final String documentId = UUID.randomUUID().toString();
        final String tagKey = "category";
        final String tagvalue = "person";
        final String username = "jsmith";
        final Date now = new Date();

        ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
        addParameter(event, "siteId", siteId);
        event.setIsBase64Encoded(Boolean.FALSE);

        DocumentSearch s = new DocumentSearch().query(
            new DocumentSearchQuery().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Arrays.asList(documentId)));
        event.setBody(GsonUtil.getInstance().toJson(s));

        if ("eqOr".equals(op)) {
          s.query().tag().eq(null).eqOr(Arrays.asList("person"));
        }

        for (String v : Arrays.asList("", "!")) {
          DocumentTag item = new DocumentTag(documentId + v, tagKey, tagvalue + v, now, username);
          item.setUserId(UUID.randomUUID().toString());

          getDocumentService().saveDocument(siteId,
              new DocumentItemDynamoDb(documentId + v, now, username), Arrays.asList(item));
        }

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = fromJson(response, Map.class);
        DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

        List<DynamicObject> documents = resp.getList("documents");
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.get(0).get("documentId"));
        assertEquals(username, documents.get(0).get("userId"));
        assertNotNull(documents.get(0).get("insertedDate"));

        Map<String, Object> matchedTag = (Map<String, Object>) documents.get(0).get("matchedTag");
        assertEquals(MATCH_COUNT, matchedTag.size());
        assertEquals("category", matchedTag.get("key"));
        assertEquals("person", matchedTag.get("value"));
        assertEquals("USERDEFINED", matchedTag.get("type"));
        assertNull(resp.get("next"));
        assertNull(resp.get("previous"));

        // given - invalid document id
        s.query().documentIds(Arrays.asList("123"));
        event.setBody(GsonUtil.getInstance().toJson(s));
        // when
        response = handleRequest(event);
        // then
        m = fromJson(response, Map.class);
        resp = new DynamicObject(fromJson(m.get("body"), Map.class));
        documents = resp.getList("documents");
        assertEquals(0, documents.size());
      }
    }
  }

  /**
   * Valid POST search by eq tagValue and TOO many DocumentId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest08() throws Exception {
    final int count = 101;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest().query(new SearchQuery()
          .tag(new SearchTagCriteria().key("test")).documentsIds(new ArrayList<>()));
      List<String> ids = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        ids.add(UUID.randomUUID().toString());
      }
      q.query().documentsIds(ids);

      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":"
          + "\"{\\\"message\\\":\\\"Maximum number of DocumentIds is 100\\\"}\","
          + "\"statusCode\":400}";
      assertEquals(expected, response);
    }
  }

  /**
   * Valid POST search by eq tagValue with > 10 Document.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest09() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int count = 100;
      final String tagKey = "category";
      final String tagvalue = "person";
      final String username = "jsmith";
      List<String> documentIds = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        String documentId = UUID.randomUUID().toString();
        Date now = new Date();

        documentIds.add(documentId);

        DocumentTag item = new DocumentTag(documentId, tagKey, tagvalue, now, username);
        item.setUserId(UUID.randomUUID().toString());

        getDocumentService().saveDocument(siteId,
            new DocumentItemDynamoDb(documentId, now, username), Arrays.asList(item));
      }

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest().query(new SearchQuery()
          .tag(new SearchTagCriteria().key(tagKey).eq(tagvalue)).documentsIds(documentIds));
      event.setBody(GsonUtil.getInstance().toJson(q));

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
      assertEquals(count, documents.size());

      // given not search by documentIds should be limited to 10
      q = new QueryRequest().query(new SearchQuery()
          .tag(new SearchTagCriteria().key(tagKey).eq(tagvalue)).documentsIds(null));
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      response = handleRequest(event);

      // then
      final int ten = 10;
      m = fromJson(response, Map.class);

      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      documents = resp.getList("documents");
      assertEquals(ten, documents.size());
    }
  }

  /**
   * Test Setting multiple tags.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest10() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest()
          .query(new SearchQuery().tags(Arrays.asList(new SearchTagCriteria().key("test"))));
      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":"
          + "\"{\\\"message\\\":\\\"Feature only available in FormKiQ Enterprise\\\"}\","
          + "\"statusCode\":402}";
      assertEquals(expected, response);
    }
  }

  /**
   * Missing Tag Key.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest11() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest().query(new SearchQuery().tag(new SearchTagCriteria()));
      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":"
          + "\"{\\\"message\\\":\\\"'tag' attribute is required.\\\"}\"," + "\"statusCode\":400}";
      assertEquals(expected, response);
    }
  }
}
