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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.stacks.api.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.sqs.model.Message;

/** Unit Tests for request /documents/{documentId}/formats. */
public class DocumentIdFormatsPostRequestHandlerTest extends AbstractRequestHandler {

  /** 500 Milliseconds. */
  private static final long SLEEP = 500L;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /**
   * Assert SQS Documents Format Messages.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param sqsQueueUrl {@link String}
   * @param contentType {@link String}
   * @throws InterruptedException InterruptedException
   */
  @SuppressWarnings("unchecked")
  private void assertDocumentFormatsMessage(final String siteId, final String documentId,
      final String sqsQueueUrl, final String contentType) throws InterruptedException {

    SqsService sqsService = getAwsServices().sqsService();
    List<Message> msgs = sqsService.receiveMessages(sqsQueueUrl).messages();
    while (msgs.size() != 1) {
      Thread.sleep(SLEEP);
    }
    assertEquals(1, msgs.size());

    sqsService.deleteMessage(sqsQueueUrl, msgs.get(0).receiptHandle());
    Map<String, String> map = this.gson.fromJson(msgs.get(0).body(), Map.class);
    assertEquals(documentId, map.get("documentId"));
    assertEquals("text/html", map.get("sourceContentType"));
    assertEquals(contentType, map.get("destinationContentType"));
    assertEquals("7b87c0e2-5f20-403b-bcc6-d35cb491d3b7@formkiq.com", map.get("userId"));

    if (siteId != null) {
      assertEquals(siteId, map.get("siteId"));
    } else {
      assertNull(map.get("siteId"));
    }
  }

  /**
   * /documents/{documentId}/formats request.
   * 
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentFormats01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-formats01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody(this.gson.toJson(Map.of("mime", "application/pdf")));

      String userId = "jsmith";
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setContentType("application/pdf");
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));

      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      Map<String, String> body =
          GsonUtil.getInstance().fromJson(m.get("body").toString(), Map.class);

      assertNotNull(body.get("documentId"));
      String url = body.get("url");

      assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(url.contains("X-Amz-Expires=172800"));
      assertTrue(url.contains(getAwsRegion().toString()));

      if (siteId != null) {
        assertTrue(url.startsWith("http://localhost:4566/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(url.startsWith("http://localhost:4566/testbucket/" + documentId));
      }
    }
  }

  /**
   * /documents/{documentId}/formats request.
   * 
   * Content-Type doesn't match
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentFormats02() throws Exception {

    String ssmkey = "/formkiq/" + getAppenvironment() + "/sqs/DocumentsFormatsUrl";

    try {

      putSsmParameter(ssmkey, getSqsDocumentFormatsQueueUrl());

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        newOutstream();

        // given
        String documentId = UUID.randomUUID().toString();
        final String userId = "jsmith";

        ApiGatewayRequestEvent event =
            toRequestEvent("/request-post-documents-documentid-formats01.json");
        addParameter(event, "siteId", siteId);
        setPathParameter(event, "documentId", documentId);
        event.setBody(this.gson.toJson(Map.of("mime", "application/pdf")));

        DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
        item.setContentType("text/html");
        getDocumentService().saveDocument(siteId, item, new ArrayList<>());

        // when
        String response = handleRequest(event);

        // then
        Map<String, Object> m = fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("202.0", String.valueOf(m.get("statusCode")));

        assertDocumentFormatsMessage(siteId, documentId, getSqsDocumentFormatsQueueUrl(),
            "application/pdf");
      }

    } finally {
      removeSsmParameter(ssmkey);
    }
  }

  /**
   * /documents/{documentId}/formats request.
   * 
   * Invalid Content-Type
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentFormats03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-formats01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody(this.gson.toJson(Map.of("mime", "text/plain")));

      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setContentType("application/pdf");
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * /documents/{documentId}/formats, without formats being installed.
   * 
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentFormats04() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      final String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-documents-documentid-formats01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      event.setBody(this.gson.toJson(Map.of("mime", "application/pdf")));

      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setContentType("text/html");
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("501.0", String.valueOf(m.get("statusCode")));
    }
  }
}
