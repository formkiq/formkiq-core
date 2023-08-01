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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/folder/move. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class IndicesFolderMoveRequestTest extends AbstractRequestHandler {

  /**
   * POST /indices/folder/move request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePost01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("x/z/test.pdf");
      getDocumentService().saveDocument(siteId, item, null);

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-move.json");
      event.setPathParameters(Map.of("indexType", "folder"));
      addParameter(event, "siteId", siteId);
      Map<String, String> body = Map.of("source", item.getPath(), "target", "a/b/c/");
      event.setBody(GsonUtil.getInstance().toJson(body));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"Folder moved\"}", m.get("body"));

      assertEquals("a/b/c/test.pdf",
          getDocumentService().findDocument(siteId, documentId).getPath());
    }
  }

  /**
   * POST /indices/folder/move missing body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePost02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-move.json");
      event.setPathParameters(Map.of("indexType", "folder"));
      addParameter(event, "siteId", siteId);
      event.setBody("");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"errors\":[{\"error\":\"invalid body\"}]}", m.get("body"));
    }
  }

  /**
   * POST /indices/folder/move missing 'source'/'target'.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePost03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-move.json");
      event.setPathParameters(Map.of("indexType", "folder"));
      addParameter(event, "siteId", siteId);
      event.setBody("{}");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"errors\":[{\"key\":\"source\",\"error\":\"attribute is required\"},"
          + "{\"key\":\"target\",\"error\":\"attribute is required\"}]}", m.get("body"));
    }
  }

  /**
   * POST /indices/folder/move - source does not exist.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePost04() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String path = UUID.randomUUID().toString() + ".txt";

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-move.json");
      event.setPathParameters(Map.of("indexType", "folder"));
      addParameter(event, "siteId", siteId);
      Map<String, String> body = Map.of("source", path, "target", "a/b/c/");
      event.setBody(GsonUtil.getInstance().toJson(body));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"folder '" + path + "' does not exist\"}", m.get("body"));
    }
  }

  /**
   * POST /indices/folder/move to root request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePost05() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      for (String target : Arrays.asList("/")) {

        String documentId = UUID.randomUUID().toString();
        DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
        item.setPath("x/z/test.pdf");
        getDocumentService().saveDocument(siteId, item, null);

        ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-move.json");
        event.setPathParameters(Map.of("indexType", "folder"));
        addParameter(event, "siteId", siteId);
        Map<String, String> body = Map.of("source", item.getPath(), "target", target);
        event.setBody(GsonUtil.getInstance().toJson(body));

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        assertEquals(getHeaders(),
            "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
        assertEquals("{\"message\":\"Folder moved\"}", m.get("body"));

        assertEquals("test.pdf", getDocumentService().findDocument(siteId, documentId).getPath());
      }
    }
  }
}
