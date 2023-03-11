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
import static com.formkiq.testutils.aws.TestServices.getSqsWebsocketQueueUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginExtension;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/** Unit Tests for request /documents/{documentId}/tags. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsTagsRequestTest extends AbstractRequestHandler {

  /** Test Timeout. */
  private static final long TEST_TIMEOUT = 10000L;

  /**
   * DELETE /documents/{documentId}/tags/{tagKey} request with Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      DocumentTag tag0 = new DocumentTag(documentId, tagKey, tagKey, now, userId);
      tag0.setInsertedDate(new Date());

      DocumentTag tag1 = new DocumentTag(documentId, tagKey + "2", tagKey, now, userId);
      tag1.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag0, tag1), null);
      assertEquals(2, getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS)
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
      assertEquals(1, tags.getResults().size());
      assertEquals("category2", tags.getResults().get(0).getKey());

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
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      DocumentTag tag = new DocumentTag(documentId, tagKey, null, now, userId);
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
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
   * DELETE /documents/{documentId}/tags/{tagKey} request with Tag Values.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      DocumentTag tag = new DocumentTag(documentId, tagKey, null, now, userId);
      tag.setValues(Arrays.asList("abc", "xyz"));
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
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
   * DELETE /documents/{documentId}/tags/{tagKey} request with Validation Errors.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagDocument04() throws Exception {
    AwsServiceCache.register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(new DocumentTagSchemaReturnErrors()));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      DocumentTag tag = new DocumentTag(documentId, tagKey, tagKey, now, userId);
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
      assertEquals(1, getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS)
          .getResults().size());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"errors\":[{\"error\":\"test error\",\"key\":\"type\"}]}", m.get("body"));
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} request with Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagValue01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String tagValue = "person";
      final String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tag-value01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      setPathParameter(event, "tagKey", tagKey);
      setPathParameter(event, "tagValue", tagValue);

      DocumentTag tag = new DocumentTag(documentId, tagKey, tagValue, now, userId);
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
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
      assertEquals("Removed Tag from document '" + documentId + "'.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} request with Tag Values.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagValue02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tag-value01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      setPathParameter(event, "tagKey", tagKey);
      setPathParameter(event, "tagValue", "xyz");

      DocumentTag tag = new DocumentTag(documentId, tagKey, null, now, userId);
      tag.setValues(Arrays.asList("abc", "xyz"));
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
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
      assertEquals("Removed Tag from document '" + documentId + "'.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("abc", tags.getResults().get(0).getValue());
      assertNull(tags.getResults().get(0).getValues());
    }
  }

  /**
   * DELETE /documents/{documentId}/tags/{tagKey}/{tagValue} wrong Tag Value.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteTagValue03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Date now = new Date();
      final String documentId = UUID.randomUUID().toString();
      final String tagKey = "category";
      final String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(documentId, now, "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-delete-documents-documentid-tag-value01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      setPathParameter(event, "tagKey", tagKey);
      setPathParameter(event, "tagValue", "xyz123");

      DocumentTag tag = new DocumentTag(documentId, tagKey, null, now, userId);
      tag.setValues(Arrays.asList("abc", "xyz"));
      tag.setInsertedDate(new Date());

      getDocumentService().addTags(siteId, documentId, Arrays.asList(tag), null);
      assertEquals(1, getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS)
          .getResults().size());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiMessageResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiMessageResponse.class);
      assertEquals("Tag/Value combination not found.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertNull(tags.getResults().get(0).getValue());
      assertEquals("[abc, xyz]", tags.getResults().get(0).getValues().toString());
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
      // given
      String userId = "jsmith";
      Date date = new Date();
      String documentId = UUID.randomUUID().toString();
      String tagname = "category";
      String tagvalue = UUID.randomUUID().toString();

      DocumentTag item = new DocumentTag(documentId, tagname, tagvalue, date, userId);

      getDocumentService().addTags(siteId, documentId, Arrays.asList(item), null);

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
      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      DocumentTag item0 = new DocumentTag(documentId, "category0", "person", now, userId);
      DocumentTag item1 = new DocumentTag(documentId, "category1", "thing", now, userId);

      getDocumentService().addTags(siteId, documentId, Arrays.asList(item0, item1), null);

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

      assertNull(resp.getTags().get(0).getDocumentId());
      assertNotNull(resp.getTags().get(0).getInsertedDate());
      assertEquals("category0", resp.getTags().get(0).getKey());
      assertEquals("userdefined", resp.getTags().get(0).getType());
      assertEquals("jsmith", resp.getTags().get(0).getUserId());
      assertEquals("person", resp.getTags().get(0).getValue());
    }
  }

  /**
   * GET /documents/{documentId}/tags values.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentTags03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();

      DocumentTag item0 = new DocumentTag(documentId, "category0", null, now, userId);
      item0.setValues(Arrays.asList("abc", "xyz"));
      DocumentTag item1 = new DocumentTag(documentId, "category1", null, now, userId);
      item1.setValues(Arrays.asList("bbb", "ccc"));

      getDocumentService().addTags(siteId, documentId, Arrays.asList(item0, item1), null);

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

      assertNull(resp.getTags().get(0).getDocumentId());
      assertNotNull(resp.getTags().get(0).getInsertedDate());
      assertEquals("category0", resp.getTags().get(0).getKey());
      assertEquals("userdefined", resp.getTags().get(0).getType());
      assertEquals("jsmith", resp.getTags().get(0).getUserId());
      assertNull(resp.getTags().get(0).getValue());
      assertEquals("[abc, xyz]", resp.getTags().get(0).getValues().toString());
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
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final String expected = "{" + getHeaders()
        + ",\"body\":\"{\\\"message\\\":\\\"Tag category not found.\\\"}\",\"statusCode\":404}";

    final InputStream in = toStream("/request-get-documents-documentid-tags01.json");

    // when
    getHandler().handleRequest(in, outstream, getMockContext());

    // then
    assertEquals(expected, new String(outstream.toByteArray(), "UTF-8"));
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
      assertEquals("", resp.getValue());
    }
  }

  /**
   * GET /documents/{documentId}/tags/{tagKey} request. Tag and values found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetTags04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), "jsmith");
      DocumentTag tag = new DocumentTag(documentId, "category", null, new Date(), "jsmith");
      tag.setValues(Arrays.asList("abc", "xyz"));
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
      assertEquals("[abc, xyz]", resp.getValues().toString());
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
      // given
      final String documentId = UUID.randomUUID().toString();
      final String tagname = "category";
      final String tagvalue = "job";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());
      assertEquals("testadminuser@formkiq.com", tags.getResults().get(0).getUserId());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag non Base 64 and add 'webnotify=true'
   * parameter
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
  public void testHandlePostDocumentTags03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final long sleep = 500L;
      final String documentId = "test" + UUID.randomUUID().toString() + ".pdf";
      final String tagname = "category";
      final String tagvalue = "job";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags02.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "webnotify", "true");
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

      SqsService sqsService = getAwsServices().getExtension(SqsService.class);
      ReceiveMessageResponse msgs = sqsService.receiveMessages(getSqsWebsocketQueueUrl(null));
      while (msgs.messages().isEmpty()) {
        msgs = sqsService.receiveMessages(getSqsWebsocketQueueUrl(null));
        Thread.sleep(sleep);
      }

      assertEquals(1, msgs.messages().size());
      if (siteId != null) {
        assertEquals(
            "{\"siteId\":\"" + siteId + "\",\"documentId\":\"" + documentId
                + "\",\"message\":\"{\\\"key\\\": \\\"category\\\",\\\"value\\\": \\\"job\\\"}\"}",
            msgs.messages().get(0).body());
      } else {
        assertEquals(
            "{\"documentId\":\"" + documentId
                + "\",\"message\":\"{\\\"key\\\": \\\"category\\\",\\\"value\\\": \\\"job\\\"}\"}",
            msgs.messages().get(0).body());
      }
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
      // given
      final String documentId = "test" + UUID.randomUUID().toString() + ".pdf";
      final String tagname = "category";
      final String tagvalue = "";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

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
   * POST /documents/{documentId}/tags request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      final String tagname = "category";
      final String tagvalue = "somevalue";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

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
   * POST /documents/{documentId}/tags testing "untagged" gets removed.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = "test" + UUID.randomUUID().toString() + ".pdf";

      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(
          Map.of("documentId", documentId, "userId", username, "insertedDate", new Date()));
      getDocumentService().saveDocumentItemWithTag(siteId, doc);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("untagged", tags.getResults().get(0).getKey());

      final String tagname = "category";
      final String tagvalue = "";

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

      tags = getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertEquals(tagvalue, tags.getResults().get(0).getValue());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags "values" request. Add Tag Base 64
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags07() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();
      final String tagname = "category";
      final String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tag 'category'.\\\"}\",\"statusCode\":201}";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("eyJrZXkiOiAiY2F0ZWdvcnkiLCJ2YWx1ZXMiOiBbImpvYiIsIndob2tub3dzIl19");

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals(tagname, tags.getResults().get(0).getKey());
      assertNull(tags.getResults().get(0).getValue());
      assertEquals("[job, whoknows]", tags.getResults().get(0).getValues().toString());
      assertEquals("testadminuser@formkiq.com", tags.getResults().get(0).getUserId());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags multiple "tags" request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags08() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();
      final String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"Created Tags.\\\"}\",\"statusCode\":201}";

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("ewogICJ0YWdzIjogWwogICAgewogICAgICAia2V5IjogIm1pbmUiCiAg"
          + "ICB9LAogICAgewogICAgICAia2V5IjogInBsYXllcklkIiwKICAgICAgInZhbHVlI"
          + "jogIjEiCiAgICB9LAogICAgewogICAgICAia2V5IjogImNhc2VJZCIsCiAgICAgICJ"
          + "2YWx1ZXMiOiBbCiAgICAgICAgIjEyMyIsCiAgICAgICAgIjk5OSIKICAgICAgXQogIC"
          + "AgfQogIF0KfQ==");

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      final int count = 3;
      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(count, tags.getResults().size());
      assertEquals("caseId", tags.getResults().get(0).getKey());
      assertNull(tags.getResults().get(0).getValue());
      assertEquals("[123, 999]", tags.getResults().get(0).getValues().toString());
      assertEquals("testadminuser@formkiq.com", tags.getResults().get(0).getUserId());

      assertEquals("mine", tags.getResults().get(1).getKey());
      assertEquals("", tags.getResults().get(1).getValue());
      assertNull(tags.getResults().get(1).getValues());
      assertEquals("testadminuser@formkiq.com", tags.getResults().get(1).getUserId());

      assertEquals("playerId", tags.getResults().get(2).getKey());
      assertEquals("1", tags.getResults().get(2).getValue());
      assertNull(tags.getResults().get(2).getValues());
      assertEquals("testadminuser@formkiq.com", tags.getResults().get(2).getUserId());

      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags invalid "tags" request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags09() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();
      final String expected = "{" + getHeaders()
          + ",\"body\":\"{\\\"message\\\":\\\"invalid json body\\\"}\"" + ",\"statusCode\":400}";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("ewogICJ0YWdzIjogWwogICAgewogICAgICAia2V5MSI6ICJtaW5lIgogICAgfQogIF0KfQ==");

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);

      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(0, tags.getResults().size());
      assertTrue(getLogger().containsString("response: " + expected));
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Tag Base 64
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags10() throws Exception {
    AwsServiceCache.register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(new DocumentTagSchemaReturnErrors()));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"errors\\\":[{\\\"error\\\":\\\"test error\\\",\\\"key\\\":\\\"type\\\"}]}\","
          + "\"statusCode\":400}";
      assertEquals(expected, response);
    }
  }

  /**
   * POST /documents/{documentId}/tags with Document Missing.
   * 
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags11() throws Exception {
    AwsServiceCache.register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(new DocumentTagSchemaReturnErrors()));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\"" + "{\\\"message\\\":\\\"Document "
          + documentId + " not found.\\\"}\"," + "\"statusCode\":404}";
      assertEquals(expected, response);
    }
  }

  /**
   * POST /documents/{documentId}/tags with duplicate keys.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentTags12() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody("{\"tags\":[{\"key\": \"author\", \"value\": \"William Shakespeare\"},"
          + "{\"key\": \"author\", \"value\": \"Kevin Bacon\"}]}");

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> map = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("400.0", ((Double) map.get("statusCode")).toString());
      assertEquals(
          "{\"message\":\"Tag key can only be included once in body; "
              + "please use 'values' to assign multiple tag values to that key\"}",
          map.get("body").toString());
    }
  }

  /**
   * POST /documents/{documentId}/tags tags request. Add Restricted Tag Name
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentTags13() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-tags01.json");
      event.setIsBase64Encoded(Boolean.FALSE);
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("{\"key\": \"CLAMAV_SCAN_STATUS\",\"value\": \"asdkjasd\"}");

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"errors\\\":[{\\\"key\\\":\\\"CLAMAV_SCAN_STATUS\\\","
          + "\\\"error\\\":\\\"unallowed tag key\\\"}]}\",\"statusCode\":400}";
      assertEquals(expected, response);
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} VALUE request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' on document '" + documentId + "'.\\\"}\""
          + ",\"statusCode\":200}";

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
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final String expected = "{" + getHeaders() + "," + "\"body\":\""
        + "{\\\"message\\\":\\\"Document 143 not found.\\\"}\"" + ",\"statusCode\":404}";

    final InputStream in = toStream("/request-put-documents-documentid-tags01.json");

    // when
    getHandler().handleRequest(in, outstream, getMockContext());

    // then
    assertEquals(expected, new String(outstream.toByteArray(), "UTF-8"));
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
      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";
      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' on document '" + documentId + "'.\\\"}\""
          + ",\"statusCode\":200}";

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
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    final String expected = "{" + getHeaders() + ","
        + "\"body\":\"{\\\"message\\\":\\\"request body is invalid\\\"}\"" + ",\"statusCode\":400}";

    final InputStream in = toStream("/request-put-documents-documentid-tags03.json");

    // when
    getHandler().handleRequest(in, outstream, getMockContext());

    // then
    assertEquals(expected, new String(outstream.toByteArray(), "UTF-8"));
    in.close();
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} change VALUE to VALUES request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' on document '" + documentId + "'.\\\"}\""
          + ",\"statusCode\":200}";

      DocumentTag tag = new DocumentTag(null, "category", "nope", new Date(), userId);
      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), Arrays.asList(tag));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-put-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody("ewogICJ2YWx1ZXMiOiBbImFiYyIsICJ4eXoiXQp9");

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("category", tags.getResults().get(0).getKey());
      assertNull(tags.getResults().get(0).getValue());
      assertEquals("[abc, xyz]", tags.getResults().get(0).getValues().toString());
      assertEquals("8a73dfef-26d3-43d8-87aa-b3ec358e43ba@formkiq.com",
          tags.getResults().get(0).getUserId());
    }
  }

  /**
   * PUT /documents/{documentId}/tags/{tagKey} change VALUES to VALUE request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePutTags06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      final String expected = "{" + getHeaders() + "," + "\"body\":\""
          + "{\\\"message\\\":\\\"Updated tag 'category' on document '" + documentId + "'.\\\"}\""
          + ",\"statusCode\":200}";

      DocumentTag tag = new DocumentTag(null, "category", null, new Date(), userId);
      tag.setValues(Arrays.asList("abc", "xyz"));
      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), Arrays.asList(tag));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-put-documents-documentid-tags01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertEquals(expected, response);
      PaginationResults<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("category", tags.getResults().get(0).getKey());
      assertEquals("active", tags.getResults().get(0).getValue());
      assertNull(tags.getResults().get(0).getValues());
      assertEquals("8a73dfef-26d3-43d8-87aa-b3ec358e43ba@formkiq.com",
          tags.getResults().get(0).getUserId());
    }
  }
}
