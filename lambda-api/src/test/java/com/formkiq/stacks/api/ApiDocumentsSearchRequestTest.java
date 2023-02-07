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
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchResponseFields;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambda.typesense.DocumentMapToDocument;
import com.formkiq.module.lambda.typesense.DocumentToFulltextDocument;
import com.formkiq.module.lambda.typesense.TypesenseProcessor;
import com.formkiq.stacks.client.models.DocumentSearchQuery;
import com.formkiq.stacks.client.models.DocumentSearchTag;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TypeSenseExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/** Unit Tests for request /search. */
@ExtendWith(TypeSenseExtension.class)
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsSearchRequestTest extends AbstractRequestHandler {

  /** Match Tag element count. */
  private static final int MATCH_COUNT = 3;
  /** {@link DocumentToFulltextDocument}. */
  private DocumentToFulltextDocument fulltext = new DocumentToFulltextDocument();

  private void saveDocument(final String siteId, final String documentId, final String path)
      throws Exception {

    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    item.setPath(path);
    getDocumentService().saveDocument(siteId, item, null);

    Map<String, Object> data = Map.of("documentId", Map.of("S", item.getDocumentId()), "path",
        Map.of("S", item.getPath()));
    Map<String, Object> document = new DocumentMapToDocument().apply(data);
    document = this.fulltext.apply(document);

    TypesenseProcessor processor = new TypesenseProcessor(getMap(),
        DynamoDbTestServices.getDynamoDbConnection(), AwsBasicCredentials.create("asd", path));
    processor.addOrUpdate(siteId, item.getDocumentId(), document, item.getUserId(), false);
  }

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

        DocumentSearchQuery dsq =
            new DocumentSearchQuery().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Arrays.asList(documentId));

        Map<String, Object> s = Map.of("query", dsq);
        event.setBody(GsonUtil.getInstance().toJson(s));
        event.setIsBase64Encoded(Boolean.FALSE);

        if ("eqOr".equals(op)) {
          dsq.tag().eq(null).eqOr(Arrays.asList("person"));
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
          + "{\\\"errors\\\":[{\\\"error\\\":\\\"invalid body\\\"}]}\",\"statusCode\":400}";

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

        DocumentSearchQuery dsq =
            new DocumentSearchQuery().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Arrays.asList(documentId));
        Map<String, Object> s = Map.of("query", dsq);
        event.setBody(GsonUtil.getInstance().toJson(s));

        if ("eqOr".equals(op)) {
          dsq.tag().eq(null).eqOr(Arrays.asList("person"));
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

        // given - invalid document id
        dsq.documentIds(Arrays.asList("123"));
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
      String expected =
          "{" + getHeaders() + ",\"body\":" + "\"{\\\"errors\\\":[{\\\"key\\\":\\\"tag/key\\\","
              + "\\\"error\\\":\\\"attribute is required\\\"}]}\"," + "\"statusCode\":400}";
      assertEquals(expected, response);
    }
  }

  /**
   * /search and return responseFields.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest12() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int count = 3;
      Date now = new Date();
      final String tagKey0 = "category";
      final String tagvalue0 = "person";
      final String tagKey1 = "playerId";
      final String tagvalue1 = "111";
      final String username = "jsmith";

      for (int i = 0; i < count; i++) {
        String documentId = UUID.randomUUID().toString();

        DocumentTag item0 = new DocumentTag(documentId, tagKey0, tagvalue0, now, username);
        DocumentTag item1 = new DocumentTag(documentId, tagKey1, tagvalue1, now, username);

        getDocumentService().saveDocument(siteId,
            new DocumentItemDynamoDb(documentId, now, username), Arrays.asList(item0, item1));
      }

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest()
          .query(new SearchQuery().tag(new SearchTagCriteria().key(tagKey0).eq(tagvalue0)))
          .responseFields(new SearchResponseFields().tags(Arrays.asList(tagKey1)));
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(count, documents.size());

      documents.forEach(doc -> {
        Map<String, Object> tags = (Map<String, Object>) doc.get("tags");
        assertEquals(1, tags.size());
        assertEquals(tagvalue1, tags.get(tagKey1));
      });
    }
  }

  /**
   * /search meta 'folder' data.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest13() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String username = "joe";

      String documentId = UUID.randomUUID().toString();

      DocumentItemDynamoDb document = new DocumentItemDynamoDb(documentId, now, username);
      document.setPath("something/path.txt");
      getDocumentService().saveDocument(siteId, document, null);

      for (String folder : Arrays.asList("something", "something/", "")) {

        ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
        addParameter(event, "siteId", siteId);
        event.setIsBase64Encoded(Boolean.FALSE);
        QueryRequest q = new QueryRequest()
            .query(new SearchQuery().meta(new SearchMetaCriteria().folder(folder)));
        event.setBody(GsonUtil.getInstance().toJson(q));

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = fromJson(response, Map.class);
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        DynamicObject resp0 = new DynamicObject(fromJson(m.get("body"), Map.class));

        List<DynamicObject> documents = resp0.getList("documents");
        assertEquals(1, documents.size());
        assertNotNull(documents.get(0).get("insertedDate"));
        assertNotNull(documents.get(0).get("lastModifiedDate"));

        if (folder.length() == 0) {
          assertEquals("something", documents.get(0).get("path"));
          assertEquals("true", documents.get(0).get("folder").toString());
          assertNotNull(documents.get(0).get("documentId"));
        } else {
          assertEquals("something/path.txt", documents.get(0).get("path"));
          assertNull(documents.get(0).get("folder"));
          assertNotNull(documents.get(0).get("documentId"));
        }
      }
    }
  }

  /**
   * /search meta 'folder' data & folders only.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest14() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String username = "joe";

      for (String path : Arrays.asList("a/b/test.txt", "a/c/test.txt", "a/test.txt")) {
        String documentId = UUID.randomUUID().toString();
        DocumentItemDynamoDb document = new DocumentItemDynamoDb(documentId, now, username);
        document.setPath(path);
        getDocumentService().saveDocument(siteId, document, null);
      }

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      QueryRequest q = new QueryRequest().query(new SearchQuery()
          .meta(new SearchMetaCriteria().indexType("folder").eq("a").indexFilterBeginsWith("ff#")));
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(2, documents.size());
      assertNotNull(documents.get(0).get("insertedDate"));
      assertNotNull(documents.get(0).get("lastModifiedDate"));

      assertEquals("b", documents.get(0).get("path"));
      assertEquals("true", documents.get(0).get("folder").toString());
      assertNotNull(documents.get(0).get("documentId"));

      assertEquals("c", documents.get(1).get("path"));
      assertEquals("true", documents.get(1).get("folder").toString());
      assertNotNull(documents.get(1).get("documentId"));

      // given
      q = new QueryRequest().query(new SearchQuery().meta(
          new SearchMetaCriteria().indexType("folder").eq("a/").indexFilterBeginsWith("fi#")));
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      response = handleRequest(event);

      // then
      m = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      documents = resp.getList("documents");
      assertEquals(1, documents.size());

      assertEquals("a/test.txt", documents.get(0).get("path"));
      assertNull(documents.get(0).get("folder"));
      assertNotNull(documents.get(0).get("documentId"));
    }
  }

  /**
   * Text Fulltext search.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest15() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();
      final String text = "My Document.docx";
      final String path = "something/My Document.docx";

      saveDocument(siteId, documentId, path);

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);

      QueryRequest query = new QueryRequest().query(new SearchQuery().text(text));

      event.setBody(GsonUtil.getInstance().toJson(query));
      event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).get("documentId"));
      assertEquals(path, documents.get(0).get("path"));

      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * Text Fulltext search no data.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest16() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String text = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);

      QueryRequest query = new QueryRequest().query(new SearchQuery().text(text));

      event.setBody(GsonUtil.getInstance().toJson(query));
      event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(0, documents.size());
    }
  }

  /**
   * /search meta path.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest17() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String username = "joe";
      String path = "something/path.txt";

      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb document = new DocumentItemDynamoDb(documentId, now, username);
      document.setPath(path);
      getDocumentService().saveDocument(siteId, document, null);

      QueryRequest q =
          new QueryRequest().query(new SearchQuery().meta(new SearchMetaCriteria().path(path)));

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody(GsonUtil.getInstance().toJson(q));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m0 = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m0.get("statusCode")));
      DynamicObject resp0 = new DynamicObject(fromJson(m0.get("body"), Map.class));

      List<DynamicObject> documents = resp0.getList("documents");
      assertEquals(1, documents.size());
      assertNotNull(documents.get(0).get("insertedDate"));
      assertNotNull(documents.get(0).get("lastModifiedDate"));
      assertEquals(documentId, documents.get(0).get("documentId"));
    }
  }
}
