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

import static com.formkiq.stacks.dynamodb.DocumentSyncService.MESSAGE_ADDED_METADATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.http.JsonServiceGson;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}/syncs. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentSyncRequestHandlerTest extends AbstractRequestHandler {

  /** {@link JsonServiceGson}. */
  private JsonServiceGson gson = new JsonServiceGson();

  /**
   * Get /esignature/docusign/config request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String documentId) {
    ApiGatewayRequestEvent event =
        new ApiGatewayRequestEventBuilder().method("get").resource("/documents/{documentId}/syncs")
            .path("/documents/" + documentId + "/syncs").group(siteId != null ? siteId : null)
            .user("joesmith").pathParameters(Map.of("documentId", documentId))
            .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * Get /documents/{documentId}/syncs request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentSyncs01() throws Exception {

    String userId = "joe";
    DocumentSyncService service = getAwsServices().getExtension(DocumentSyncService.class);

    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();

      service.saveSync(siteId, documentId, DocumentSyncServiceType.OPENSEARCH,
          DocumentSyncStatus.COMPLETE, DocumentSyncType.METADATA, userId, MESSAGE_ADDED_METADATA);
      TimeUnit.SECONDS.sleep(1);
      service.saveSync(siteId, documentId, DocumentSyncServiceType.TYPESENSE,
          DocumentSyncStatus.FAILED, DocumentSyncType.METADATA, userId, MESSAGE_ADDED_METADATA);

      ApiGatewayRequestEvent event = getRequest(siteId, documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      Map<String, Object> body = this.gson.fromJsonToMap(m.get("body"));
      List<Map<String, String>> list = (List<Map<String, String>>) body.get("syncs");
      assertEquals(2, list.size());

      assertEquals("TYPESENSE", list.get(0).get("service"));
      assertEquals(documentId, list.get(0).get("documentId"));
      assertEquals("FAILED", list.get(0).get("status"));
      assertEquals("METADATA", list.get(0).get("type"));
      assertNotNull(list.get(0).get("syncDate"));

      assertEquals("OPENSEARCH", list.get(1).get("service"));
      assertEquals(documentId, list.get(1).get("documentId"));
      assertEquals("COMPLETE", list.get(1).get("status"));
      assertEquals("METADATA", list.get(1).get("type"));
      assertNotNull(list.get(1).get("syncDate"));
    }
  }
}
