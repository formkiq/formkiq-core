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
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/{type}/{key}. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class IndicesRequestHandlerTest extends AbstractRequestHandler {

  /**
   * POST /indices/{type}/{key} request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDelete01() throws Exception {

    DocumentSearchService dss = getAwsServices().getExtension(DocumentSearchService.class);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      getDocumentService().saveDocument(siteId, item, null);
      getDocumentService().deleteDocument(siteId, item.getDocumentId());

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-indices.json");
      addParameter(event, "siteId", siteId);
      event.setPathParameters(Map.of("indexType", "folder", "indexKey", indexKey));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"Folder deleted\"}", m.get("body"));

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * POST /indices/{type}/{key} request, folder not empty.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDelete02() throws Exception {

    DocumentSearchService dss = getAwsServices().getExtension(DocumentSearchService.class);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      getDocumentService().saveDocument(siteId, item, null);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder("x"));
      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem folder = results.getResults().get(0);
      String indexKey = folder.get("indexKey").toString();

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-indices.json");
      addParameter(event, "siteId", siteId);
      event.setPathParameters(Map.of("indexType", "folder", "indexKey", indexKey));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"Folder not empty\"}", m.get("body"));

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
    }
  }

  /**
   * POST /indices/{type}/{key} request, invalid key.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDelete03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String indexKey = "12345";

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-indices.json");
      addParameter(event, "siteId", siteId);
      event.setPathParameters(Map.of("indexType", "folder", "indexKey", indexKey));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"invalid indexKey\"}", m.get("body"));
    }
  }

  /**
   * POST /indices/{type}/{key} request, TAGS type.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDelete04() throws Exception {

    DocumentService ds = getAwsServices().getExtension(DocumentService.class);
    DocumentSearchService dss = getAwsServices().getExtension(DocumentSearchService.class);

    String indexType = "tags";
    SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().indexType(indexType));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), new Date(), "joe");
      String tagKey = "category";
      String tagValue = "person";
      DocumentTag tag = new DocumentTag(item.getDocumentId(), tagKey, tagValue, new Date(), "joe");
      ds.saveDocument(siteId, item, Arrays.asList(tag));

      PaginationResults<DynamicDocumentItem> results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());

      String indexKey = "category";

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-indices.json");
      addParameter(event, "siteId", siteId);
      event.setPathParameters(Map.of("indexType", indexType, "indexKey", indexKey));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"Folder deleted\"}", m.get("body"));

      results = dss.search(siteId, q, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }


  /**
   * POST /indices/{type}/{key} request, invalid type.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDelete05() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String indexKey = "12345";

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-indices.json");
      addParameter(event, "siteId", siteId);
      event.setPathParameters(Map.of("indexType", "asd", "indexKey", indexKey));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"invalid 'indexType' parameter\"}", m.get("body"));
    }
  }
}
