/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;

/** Unit Tests for uploading /documents. */
public class ApiDocumentsUploadRequestTest extends AbstractRequestHandler {

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    removeSsmParameter("/formkiq/unittest/siteid/default/MaxContentLengthBytes");
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
        newOutstream();
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
          assertTrue(getLogger()
              .containsString("generated presign url: http://localhost:4566/testbucket/" + siteId));
        } else {
          assertTrue(getLogger()
              .containsString("generated presign url: http://localhost:4566/testbucket/"));
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
      newOutstream();
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
      newOutstream();
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

      assertTrue(
          getLogger().containsString("generated presign url: http://localhost:4566/testbucket/"));
      assertTrue(getLogger().containsString("for document " + resp.getDocumentId()));

      assertEquals(documentId, resp.getDocumentId());
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
      newOutstream();
      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      ApiUrlResponse resp = expectResponse(response);
      DocumentItem item = getDocumentService().findDocument(siteId, resp.getDocumentId());
      assertEquals(
          "AROAZB6IP7U6SDBIQTEUX:formkiq-docstack-unittest-api-ApiGatewayInvokeRole-IKJY8XKB0IUK",
          item.getUserId());
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

    assertTrue(
        getLogger().containsString("generated presign url: http://localhost:4566/testbucket/"));
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
    putSsmParameter("/formkiq/" + getAppenvironment() + "/siteid/default/MaxContentLengthBytes",
        maxContentLengthBytes);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      newOutstream();

      if (siteId != null) {
        putSsmParameter(
            "/formkiq/" + getAppenvironment() + "/siteid/" + siteId + "/MaxContentLengthBytes",
            maxContentLengthBytes);
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
    putSsmParameter("/formkiq/" + getAppenvironment() + "/siteid/default/MaxContentLengthBytes",
        maxContentLengthBytes);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      newOutstream();

      if (siteId != null) {
        putSsmParameter(
            "/formkiq/" + getAppenvironment() + "/siteid/" + siteId + "/MaxContentLengthBytes",
            maxContentLengthBytes);
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
}
