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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginExtension;
import com.formkiq.stacks.client.models.AddLargeDocument;
import com.formkiq.stacks.client.models.DocumentActionType;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

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

    @SuppressWarnings("unchecked")
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
   * @param readonly boolean
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String documentId,
      final boolean readonly) {

    String resource = documentId != null ? "/documents/{documentId}/upload" : "/documents/upload";
    String path = documentId != null ? "/documents/" + documentId + "/upload" : "/documents/upload";
    String group = siteId != null ? siteId : "default";

    if (readonly) {
      group += "_read";
    }

    Map<String, String> pathMap =
        documentId != null ? Map.of("documentId", documentId) : Collections.emptyMap();
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("get")
        .resource(resource).path(path).group(group).user("joesmith").pathParameters(pathMap)
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
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
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("post")
        .resource("/documents/upload").path("/documents/upload").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
    return event;
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload01() throws Exception {
    // given
    for (String path : Arrays.asList(null, "/bleh/test.txt")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
          assertTrue(getLogger().containsString("generated presign url: "
              + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/" + siteId));
        } else {
          assertFalse(resp.getUrl().contains("/testbucket/default"));
          assertTrue(getLogger().containsString("generated presign url: "
              + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
        }

        assertTrue(getLogger().containsString("saving document: "));

        if (path != null) {
          assertTrue(getLogger().containsString(" on path /bleh/test.txt"));
        } else {
          assertTrue(getLogger().containsString(" on path " + null));
        }
      }
    }
  }

  /**
   * Valid PUT generate upload document signed url for new document.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      String documentId = UUID.randomUUID().toString();
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

      assertTrue(getLogger().containsString("generated presign url: "
          + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
      assertTrue(getLogger().containsString("for document " + resp.getDocumentId()));

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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
  @SuppressWarnings("unchecked")
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

    assertTrue(getLogger().containsString("generated presign url: "
        + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
    assertTrue(getLogger().containsString("saving document: "));
    assertTrue(getLogger().containsString(" on path " + null));
  }

  /**
   * Content-Length > Max Content Length.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload06() throws Exception {

    String maxContentLengthBytes = "2783034";
    this.configService.save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload07() throws Exception {

    String maxContentLengthBytes = "2783034";
    this.configService.save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

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
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentsUpload01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String path = "/bleh/test.txt";

      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("test").value("this");
      com.formkiq.stacks.client.models.AddDocumentAction action0 =
          new com.formkiq.stacks.client.models.AddDocumentAction().type(DocumentActionType.OCR);
      AddLargeDocument document = new AddLargeDocument().path(path).tags(Arrays.asList(tag0))
          .actions(Arrays.asList(action0));

      ApiGatewayRequestEvent event = postDocumentsUploadRequest(siteId,
          siteId != null ? siteId : "default", GsonUtil.getInstance().toJson(document));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      ApiUrlResponse resp = expectResponse(response);
      assertFalse(resp.getUrl().contains("content-length"));

      if (siteId != null) {
        assertTrue(resp.getUrl().contains("/testbucket/" + siteId));
        assertTrue(getLogger().containsString("generated presign url: "
            + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/" + siteId));
      } else {
        assertFalse(resp.getUrl().contains("/testbucket/default"));
        assertTrue(getLogger().containsString("generated presign url: "
            + TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
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
      assertEquals("this", tags.get(i++).getValue());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * fails TagSchema required tags.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentsUpload02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      final String tagSchemaId = UUID.randomUUID().toString();
      getAwsServices().register(DocumentTagSchemaPlugin.class,
          new DocumentTagSchemaPluginExtension(new DocumentTagSchemaReturnErrors()));

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      event.setHttpMethod("POST");
      addParameter(event, "siteId", siteId);
      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("test").value("this");
      AddLargeDocument document =
          new AddLargeDocument().tagSchemaId(tagSchemaId).tags(Arrays.asList(tag0));
      event.setBody(GsonUtil.getInstance().toJson(document));
      event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"errors\":[{\"error\":\"test error\",\"key\":\"type\"}]}", m.get("body"));

      assertNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentsUpload03() throws Exception {
    // given
    getAwsServices().register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(new DocumentTagSchemaReturnNewTags()));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String tagSchemaId = UUID.randomUUID().toString();
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      event.setHttpMethod("POST");
      addParameter(event, "siteId", siteId);
      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("test").value("this");
      AddLargeDocument document =
          new AddLargeDocument().tagSchemaId(tagSchemaId).tags(Arrays.asList(tag0));
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
      final int expectedCount = 2;
      List<DocumentTag> tags =
          getDocumentService().findDocumentTags(siteId, documentId, null, LIMIT).getResults();
      assertEquals(expectedCount, tags.size());
      assertEquals("test", tags.get(i).getKey());
      assertEquals("this", tags.get(i++).getValue());
      assertEquals("testtag", tags.get(i).getKey());
      assertEquals("testvalue", tags.get(i++).getValue());

      assertNotNull(getDocumentService().findMostDocumentDate());
    }
  }

  /**
   * Valid readonly user generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentsUpload04() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (String documentId : Arrays.asList(null, UUID.randomUUID().toString())) {

        ApiGatewayRequestEvent event = getRequest(siteId, documentId, true);

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
        assertEquals("403.0", String.valueOf(m.get("statusCode")));

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
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentsUpload05() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      com.formkiq.stacks.client.models.DocumentTag tag0 =
          new com.formkiq.stacks.client.models.DocumentTag().key("CLAMAV_SCAN_TIMESTAMP")
              .value("this");
      AddLargeDocument document = new AddLargeDocument().tags(Arrays.asList(tag0));

      ApiGatewayRequestEvent event = postDocumentsUploadRequest(siteId,
          siteId != null ? siteId : "default", GsonUtil.getInstance().toJson(document));

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
