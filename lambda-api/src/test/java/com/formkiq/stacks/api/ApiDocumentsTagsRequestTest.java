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

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.PaginationResults;

/** Unit Tests for request /documents/{documentId}/tags. */
public class ApiDocumentsTagsRequestTest extends AbstractRequestHandler {

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request with Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      Date now = new Date();
      String documentId = UUID.randomUUID().toString();
      String tagKey = "category";
      String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentTag tag = new DocumentTag(documentId, tagKey, tagKey, now, userId);
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag));
      assertEquals(1, getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS)
          .getResults().size());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiMessageResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiMessageResponse.class);
      assertEquals("Removed 'category' from document '" + documentId + "'.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());

      String expected = "response: {" + getHeaders()
          + ",\"body\":\"{\\\"message\\\":\\\"Removed 'category' from document '" + documentId
          + "'.\\\"}\"," + "\"statusCode\":200}";

      assertTrue(getLogger().containsString(expected));
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request without Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      Date now = new Date();
      String documentId = UUID.randomUUID().toString();
      String tagKey = "category";
      String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentTag tag = new DocumentTag(documentId, tagKey, null, now, userId);
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag));
      assertEquals(1, getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS)
          .getResults().size());

      // when
      final String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiMessageResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiMessageResponse.class);
      assertEquals("Removed 'category' from document '" + documentId + "'.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());

      final String expected = "response: {" + getHeaders()
          + ",\"body\":\"{\\\"message\\\":\\\"Removed 'category' from document '" + documentId
          + "'.\\\"}\"," + "\"statusCode\":200}";
      assertTrue(getLogger().containsString(expected));
    }
  }

  /**
   * Get /documents/{documentId}/tags tags request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentTags01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String userId = "jsmith";
      Date date = new Date();
      String documentId = UUID.randomUUID().toString();
      String tagname = "category";
      String tagvalue = UUID.randomUUID().toString();

      DocumentTag item = new DocumentTag(documentId, tagname, tagvalue, date, userId);

      getDocumentService().addTags(siteId, documentId, Arrays.asList(item));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-tags00.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      ApiDocumentTagsItemResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentTagsItemResponse.class);

      assertEquals(1, resp.getTags().size());
      assertEquals(tagname, resp.getTags().get(0).getKey());
      assertEquals(tagvalue, resp.getTags().get(0).getValue());
      assertNull(resp.getTags().get(0).getDocumentId());
      assertEquals(userId, resp.getTags().get(0).getUserId());

      assertNull(resp.getNext());
      assertNull(resp.getPrevious());
    }
  }

  /**
   * GET /documents/{documentId}/tags request limit 1.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentTags02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      DocumentTag item0 = new DocumentTag(documentId, "category0", "person", now, userId);
      DocumentTag item1 = new DocumentTag(documentId, "category1", "thing", now, userId);

      getDocumentService().addTags(siteId, documentId, Arrays.asList(item0, item1));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-tags02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      ApiDocumentTagsItemResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentTagsItemResponse.class);

      assertEquals(1, resp.getTags().size());
      assertNotNull(resp.getNext());
      assertNull(resp.getPrevious());
    }
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag not found.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetTags01() throws Exception {
    // given
    final String expected = "{" + getHeaders()
        + ",\"body\":\"{\\\"message\\\":\\\"Tag category not found.\\\"}\",\"statusCode\":404}";

    final InputStream in = toStream("/request-get-documents-documentid-tags01.json");

    // when
    getHandler().handleRequest(in, getOutstream(), getMockContext());

    // then
    assertEquals(expected, new String(getOutstream().toByteArray(), "UTF-8"));
    in.close();
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and value found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetTags02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), "jsmith");
      DocumentTag tag = new DocumentTag(documentId, "category", "person", new Date(), "jsmith");
      getDocumentService().saveDocument(siteId, item, Arrays.asList(tag));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiDocumentTagItemResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentTagItemResponse.class);
      assertEquals("category", resp.getKey());
      assertEquals("person", resp.getValue());
    }
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and NO value found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetTags03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), "jsmith");
      DocumentTag tag = new DocumentTag(documentId, "category", null, new Date(), "jsmith");
      getDocumentService().saveDocument(siteId, item, Arrays.asList(tag));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiDocumentTagItemResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentTagItemResponse.class);
      assertEquals("category", resp.getKey());
      assertNull(resp.getValue());
    }
  }

  /**
   * POST /documents/{documentId}/tags request missing 'tagvalue' field.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags01() throws Exception {
    // given
    ApiGatewayRequestEvent event =
        toRequestEvent("/request-post-documents-documentid-tags-invalid1.json");

    String expected = "{" + getHeaders()
        + ",\"body\":\"{\\\"message\\\":\\\"invalid json body\\\"}\"" + ",\"statusCode\":400}";

    // when
    String response = handleRequest(event);

    // then
    assertEquals(expected, response);
    assertTrue(getLogger().containsString("response: " + expected));
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag Base 64
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      final String documentId = UUID.randomUUID().toString();
      final String tagname = "category";
      final String tagvalue = "job";
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag non Base 64
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      final String documentId = "test" + UUID.randomUUID().toString() + ".pdf";
      final String tagname = "category";
      final String tagvalue = "job";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);


      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag Key ONLY no value
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      final String documentId = "test" + UUID.randomUUID().toString() + ".pdf";
      final String tagname = "category";
      final String tagvalue = null;

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags03.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags/{tagKey} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      final String tagname = "category";
      final String tagvalue = "somevalue";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags04.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());
      assertEquals(
          "AROAZB6IP7U6SDBIQTEUX:formkiq-docstack-unittest-api-ApiGatewayInvokeRole-IKJY8XKB0IUK",
          tags.getResults().get(0).getUserId());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' to 'active' for document '" + documentId
          + "'.\\\"}\"" + ",\"statusCode\":200}";

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId),
          Arrays.asList(new DocumentTag(null, "category", "nope", new Date(), userId)));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-put-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} request where DocumentId is missing.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags02() throws Exception {
    // given
    final String expected = "{" + getHeaders() + "," + "\"body\":\""
        + "{\\\"message\\\":\\\"Document 143 not found.\\\"}\"" + ",\"statusCode\":404}";

    final InputStream in = toStream("/request-put-documents-documentid-tags01.json");

    // when
    getHandler().handleRequest(in, getOutstream(), getMockContext());

    // then
    assertEquals(expected, new String(getOutstream().toByteArray(), "UTF-8"));
    in.close();
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";
      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' to 'active' for document '" + documentId
          + "'.\\\"}\"" + ",\"statusCode\":200}";

      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId),
          Arrays.asList(new DocumentTag(null, "category", "nope", new Date(), userId)));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-put-documents-documentid-tags02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} request with missing body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags04() throws Exception {
    // given
    final String expected = "{" + getHeaders() + ","
        + "\"body\":\"{\\\"message\\\":\\\"request body is invalid\\\"}\"" + ",\"statusCode\":400}";

    final InputStream in = toStream("/request-put-documents-documentid-tags03.json");

    // when
    getHandler().handleRequest(in, getOutstream(), getMockContext());

    // then
    assertEquals(expected, new String(getOutstream().toByteArray(), "UTF-8"));
    in.close();
  }
}
