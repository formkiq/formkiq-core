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

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.stacks.client.models.AddLargeDocument;
import com.formkiq.stacks.client.models.DocumentActionType;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for uploading /documents/uploads. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ApiDocumentsUploadRequestTest extends AbstractRequestHandler {

  /** Results Limit. */
  private static final int LIMIT = 10;
  /** {@link ConfigService}. */
  private ConfigService configService = null;

  @Override
  @BeforeEach
  public void before() throws Exception {
    super.before();
    this.configService = getAwsServices().getExtension(ConfigService.class);
    this.configService.delete(null);
  }

  /**
   * Verify Response.
   *
   * @param response {@link String}
   * @return {@link ApiUrlResponse}
   */
  private ApiUrlResponse expectResponse(final String response) {

    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiUrlResponse resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiUrlResponse.class);

    assertNull(resp.getNext());
    assertNull(resp.getPrevious());
    return resp;
  }

  /**
   * Get /documents/upload request.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String documentId) {

    String resource = documentId != null ? "/documents/{documentId}/upload" : "/documents/upload";
    String path = documentId != null ? "/documents/" + documentId + "/upload" : "/documents/upload";
    String group = siteId != null ? siteId : DEFAULT_SITE_ID;

    group += "_read";

    Map<String, String> pathMap =
        documentId != null ? Map.of("documentId", documentId) : Collections.emptyMap();
    return new ApiGatewayRequestEventBuilder().method("get").resource(resource).path(path)
        .group(group).user("joesmith").pathParameters(pathMap)
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
  }

  /**
   * POST /documents/upload request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent postDocumentsUploadRequest(final String siteId, final String group,
      final String body) {
    return new ApiGatewayRequestEventBuilder().method("post").resource("/documents/upload")
        .path("/documents/upload").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload01() throws Exception {
    // given
    for (String path : Arrays.asList(null, "/bleh/test.txt")) {
      for (String siteId : Arrays.asList(null, ID.uuid())) {
        ApiGatewayRequestEvent event =
            toRequestEvent("/request-get-documents-upload-documentid.json");
        addParameter(event, "siteId", siteId);
        addParameter(event, "path", path);

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        ApiUrlResponse resp = expectResponse(response);
        assertFalse(resp.getUrl().contains("content-length"));

        if (siteId != null) {
          assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
        } else {
          assertFalse(resp.getUrl().contains("/testbucket/default"));
        }
      }
    }
  }

  /**
   * Valid PUT generate upload document signed url for new document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload02() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * Valid PUT generate upload document signed url for existing document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload03() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date date = new Date();
      String documentId = ID.uuid();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, "jsmith");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      String response = handleRequest(event);

      // then
      ApiUrlResponse resp = expectResponse(response);

      if (siteId != null) {
        assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
      } else {
        assertFalse(resp.getUrl().contains("/testbucket/default"));
      }

      assertEquals(documentId, resp.getDocumentId());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid PUT generate upload document signed url for NEW document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload04() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      ApiUrlResponse resp = expectResponse(response);

      if (siteId != null) {
        assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
      } else {
        assertFalse(resp.getUrl().contains("/testbucket/default"));
      }

      DocumentItem item = getDocumentService().findDocument(siteId, resp.getDocumentId());
      assertEquals(
          "AROAZB6IP7U6SDBIQTEUX:formkiq-docstack-unittest-api-ApiGatewayInvokeRole-IKJY8XKB0IUK",
          item.getUserId());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid POST generate upload document signed url with contentLength parameter.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload05() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-upload-documentid.json");
    addParameter(event, "contentLength", "1000");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    ApiUrlResponse resp = expectResponse(response);
    assertTrue(resp.getUrl().contains("content-length"));
  }

  /**
   * Content-Length > Max Content Length.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload06() throws Exception {

    String maxContentLengthBytes = "2783034";
    this.configService.save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      if (siteId != null) {
        this.configService.save(siteId, new DynamicObject(
            Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));
      }

      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "contentLength", "2783035");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"'contentLength' cannot exceed 2783034 bytes\"}", m.get("body"));
    }
  }

  /**
   * Content-Length is required.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload07() throws Exception {

    String maxContentLengthBytes = "2783034";
    this.configService.save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      if (siteId != null) {
        this.configService.save(siteId, new DynamicObject(
            Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));
      }

      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"'contentLength' is required\"}", m.get("body"));
    }
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentsUpload01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String path = "/bleh/test.txt";

      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("test").value("this");
      com.formkiq.stacks.client.models.AddDocumentAction action0 =
          new com.formkiq.stacks.client.models.AddDocumentAction().type(DocumentActionType.OCR);
      AddLargeDocument document = new AddLargeDocument().path(path)
          .tags(Collections.singletonList(tag0)).actions(Collections.singletonList(action0));

      ApiGatewayRequestEvent event = postDocumentsUploadRequest(siteId,
          siteId != null ? siteId : DEFAULT_SITE_ID, GsonUtil.getInstance().toJson(document));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      ApiUrlResponse resp = expectResponse(response);
      assertFalse(resp.getUrl().contains("content-length"));

      if (siteId != null) {
        assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
      } else {
        assertFalse(resp.getUrl().contains("/testbucket/default"));
      }

      String documentId = resp.getDocumentId();
      assertNotNull(document);

      ActionsService actionsService = getAwsServices().getExtension(ActionsService.class);
      List<Action> actions = actionsService.getActions(siteId, documentId);
      assertEquals(1, actions.size());
      assertEquals(ActionType.OCR, actions.get(0).type());
      assertEquals(ActionStatus.PENDING, actions.get(0).status());

      int i = 0;
      final int expectedCount = 1;
      List<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, LIMIT).getResults();
      assertEquals(expectedCount, tags.size());
      assertEquals("test", tags.get(i).getKey());
      assertEquals("this", tags.get(i).getValue());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentsUpload03() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String tagSchemaId = ID.uuid();
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      event.setHttpMethod("POST");
      addParameter(event, "siteId", siteId);
      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("test").value("this");
      AddLargeDocument document =
          new AddLargeDocument().tagSchemaId(tagSchemaId).tags(Collections.singletonList(tag0));
      event.setBody(GsonUtil.getInstance().toJson(document));
      event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      ApiUrlResponse resp = expectResponse(response);

      if (siteId != null) {
        assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
      } else {
        assertFalse(resp.getUrl().contains("/testbucket/default"));
      }

      String documentId = resp.getDocumentId();
      assertNotNull(document);

      int i = 0;
      final int expectedCount = 1;
      List<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, LIMIT).getResults();
      assertEquals(expectedCount, tags.size());
      assertEquals("test", tags.get(i).getKey());
      assertEquals("this", tags.get(i).getValue());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid readonly user generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentsUpload04() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (String documentId : Arrays.asList(null, ID.uuid())) {

        ApiGatewayRequestEvent event = getRequest(siteId, documentId);

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
        assertEquals("401.0", String.valueOf(m.get("statusCode")));

        String body = String.valueOf(m.get("body"));
        assertTrue(body.contains("\"message\":\"fkq access denied (groups:"));
      }
    }
  }

  /**
   * Valid POST /documents/upload, invalid tag.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocumentsUpload05() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("CLAMAV_SCAN_TIMESTAMP")
              .value("this");
      AddLargeDocument document = new AddLargeDocument().tags(Collections.singletonList(tag0));

      ApiGatewayRequestEvent event = postDocumentsUploadRequest(siteId,
          siteId != null ? siteId : DEFAULT_SITE_ID, GsonUtil.getInstance().toJson(document));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(
          "{\"errors\":[{\"key\":\"CLAMAV_SCAN_TIMESTAMP\",\"error\":\"unallowed tag key\"}]}",
          String.valueOf(m.get("body")));
    }
  }
}
