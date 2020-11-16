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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import software.amazon.awssdk.services.s3.S3Client;

/** Unit Tests for request /documents/{documentId}/content. */
public class DocumentIdContentGetRequestHandlerTest extends AbstractRequestHandler {

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> body = fromJson(m.get("body").toString(), Map.class);
      String url = body.get("contentUrl").toString();

      assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(url.contains("X-Amz-Expires="));
      assertTrue(url.contains(getAwsRegion().toString()));
      assertEquals("application/octet-stream", body.get("contentType"));

      if (siteId != null) {
        assertTrue(url.startsWith("http://localhost:4566/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(url.startsWith("http://localhost:4566/testbucket/" + documentId));
      }
    }
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Document not found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests Text Content is returned.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent03() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      newOutstream();

      // given
      String documentId = UUID.randomUUID().toString();

      String content = "this is a test";
      String s3key = createS3Key(siteId, documentId);
      try (S3Client s3 = getS3().buildClient()) {
        getS3().putObject(s3, getBucketName(), s3key, content.getBytes(StandardCharsets.UTF_8),
            null);
      }

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String userId = "jsmith";
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setContentType("text/plain");
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> body = fromJson(m.get("body").toString(), Map.class);
      assertEquals(content, body.get("content"));
      assertEquals("text/plain", body.get("contentType"));
      assertEquals("false", body.get("isBase64").toString());
    }
  }
}
