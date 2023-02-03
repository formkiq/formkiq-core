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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import software.amazon.awssdk.services.sqs.model.Message;

/** Unit Tests for request /documents/{documentId}/actions. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsActionsRequestTest extends AbstractRequestHandler {

  /** {@link ActionsService}. */
  private ActionsService service;

  @Override
  @BeforeEach
  public void before() throws Exception {
    super.before();
    this.service = getAwsServices().getExtension(ActionsService.class);
  }

  /**
   * Get /documents/{documentId}/actions request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentActions01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      this.service.saveActions(siteId, documentId, Arrays.asList(new Action()
          .status(ActionStatus.COMPLETE).parameters(Map.of("test", "this")).type(ActionType.OCR)));

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-actions01.json");
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

      Map<String, Object> actionsMap = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
      List<Map<String, Object>> list = (List<Map<String, Object>>) actionsMap.get("actions");
      assertEquals(1, list.size());
      assertEquals("ocr", list.get(0).get("type"));
      assertEquals("complete", list.get(0).get("status"));
      assertEquals("{test=this}", list.get(0).get("parameters").toString());
    }
  }

  /**
   * POST /documents/{documentId}/actions request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentActions01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      this.service.saveActions(siteId, documentId,
          Arrays.asList(new Action().status(ActionStatus.COMPLETE)
              .parameters(Map.of("test", "this")).type(ActionType.FULLTEXT)));

      Map<String, Object> body = Map.of("actions",
          Arrays.asList(Map.of("type", "ocr", "parameters", Map.of("ocrParseTypes", "text")),
              Map.of("type", "webhook", "parameters", Map.of("url", "https://localhost"))));
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-actions01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody(GsonUtil.getInstance().toJson(body));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"Actions saved\"}", m.get("body"));

      int i = 0;
      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(mapsize, actions.size());
      assertEquals(ActionType.FULLTEXT, actions.get(i).type());
      assertEquals(ActionStatus.COMPLETE, actions.get(i++).status());

      assertEquals(ActionType.OCR, actions.get(i).type());
      assertEquals(ActionStatus.PENDING, actions.get(i).status());
      assertEquals("{ocrParseTypes=text}", actions.get(i++).parameters().toString());

      assertEquals(ActionType.WEBHOOK, actions.get(i).type());
      assertEquals(ActionStatus.PENDING, actions.get(i).status());
      assertEquals("{url=https://localhost}", actions.get(i++).parameters().toString());

      List<Message> sqsMessages = getSqsMessages();
      assertEquals(1, sqsMessages.size());

      Map<String, String> map =
          GsonUtil.getInstance().fromJson(sqsMessages.get(0).body(), Map.class);

      map = GsonUtil.getInstance().fromJson(map.get("Message"), Map.class);
      assertNotNull(map.get("siteId"));
      assertEquals(documentId, map.get("documentId"));
      assertEquals("actions", map.get("type"));
    }
  }

  /**
   * POST /documents/{documentId}/actions missing 'type'.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentActions02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      Map<String, Object> body =
          Map.of("actions", Arrays.asList(Map.of("parameters", Map.of("ocrParseTypes", "text"))));
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-actions01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody(GsonUtil.getInstance().toJson(body));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      assertEquals("{\"message\":\"missing/invalid 'type' in body\"}", m.get("body"));

      List<Action> actions = this.service.getActions(siteId, documentId);
      assertEquals(0, actions.size());
    }
  }
}
