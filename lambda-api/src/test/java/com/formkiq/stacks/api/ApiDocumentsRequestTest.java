/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.api;

import static com.formkiq.stacks.api.TestServices.BUCKET_NAME;
import static com.formkiq.stacks.api.TestServices.STAGE_BUCKET_NAME;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DateUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.IoUtils;

/** Unit Tests for request GET / POST / DELETE /documents. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsRequestTest extends AbstractRequestHandler {

  /** One Second. */
  private static final long ONE_SECOND = 1000L;

  /**
   * Create MAX_RESULTS + 2 Documents test data.
   * 
   * @param prefix {@link String}
   * @param testdatacount int
   */
  private void createTestData(final String prefix, final int testdatacount) {
    String userId = "jsmith";
    final int min10 = 10;
    LocalDateTime nowLocalDate = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(min10);

    final int max = testdatacount;
    for (int i = 0; i < max; i++) {

      nowLocalDate = nowLocalDate.plus(1, ChronoUnit.MINUTES);
      Date d = Date.from(nowLocalDate.atZone(ZoneOffset.UTC).toInstant());
      getDocumentService().saveDocument(prefix, new DocumentItemDynamoDb("doc_" + i, d, userId),
          new ArrayList<>());
    }
  }

  /**
   * Expect Document exists and is Deleted.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param filename {@link String}
   * @throws IOException IOException
   */
  private void expectDeleteDocument(final ApiGatewayRequestEvent event, final String filename)
      throws IOException {
    // given
    final String expected = "{" + getHeaders() + "," + "\"body\":\"{\\\"message\\\":\\\"'"
        + filename + "' object deleted\\\"}\"" + ",\"statusCode\":200}";

    // when
    String response = handleRequest(event);

    // then
    assertTrue(getLogger().containsString("response: " + expected));
    assertEquals(expected, response);
  }

  /**
   * Expect Next Page of Data.
   * 
   * @param next {@link String}
   * @throws IOException IOException
   */
  @SuppressWarnings("unchecked")
  private void expectNextPage(final String next) throws IOException {
    // given
    try (InputStream in = toStream("/request-get-documents-next.json")) {
      String input = IoUtils.toUtf8String(in).replaceAll("\\{\\{next\\}\\}", next);
      final InputStream instream2 =
          new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      
      // when
      getHandler().handleRequest(instream2, outstream, getMockContext());

      // then
      String response = new String(outstream.toByteArray(), "UTF-8");

      Map<String, String> m = fromJson(response, Map.class);
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      assertNull(resp.get("next"));
      assertNotNull(resp.get("previous"));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(2, documents.size());

      assertEquals("doc_10", documents.get(0).getString("documentId"));
      assertNotNull(documents.get(0).getString("siteId"));

      assertEquals("doc_11", documents.get(1).getString("documentId"));
      assertNotNull(documents.get(1).getString("siteId"));
    }
  }

  /**
   * Get S3 Key.
   * 
   * @param siteId {@link String}
   * @param body {@link DynamicObject}
   * @return {@link String}
   */
  private String getKey(final String siteId, final DynamicObject body) {
    String key =
        !DEFAULT_SITE_ID.equals(siteId) ? siteId + "/" + body.getString("documentId") + ".fkb64"
            : body.getString("documentId") + ".fkb64";
    return key;
  }

  /**
   * DELETE /documents request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String filename = "test.pdf";
      try (S3Client s3 = getS3().buildClient()) {
        getS3().putObject(s3, BUCKET_NAME, filename,
            "testdata".getBytes(StandardCharsets.UTF_8), null);
      }

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId);

      expectDeleteDocument(event, filename);
    }
  }

  /**
   * DELETE /documents request.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String filename = "test.txt";

      try (S3Client s3 = getS3().buildClient()) {
        getS3().putObject(s3, BUCKET_NAME, filename,
            "testdata".getBytes(StandardCharsets.UTF_8), null);
      }

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid02.json");
      addParameter(event, "siteId", siteId);

      expectDeleteDocument(event, filename);
    }
  }

  /**
   * DELETE /documents request. Document doesn't exist.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleDeleteDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String expected = "{" + getHeaders() + ","
          + "\"body\":\"{\\\"message\\\":\\\"Document test.pdf not found.\\\"}\""
          + ",\"statusCode\":404}";

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      assertTrue(getLogger().containsString("response: " + expected));
      assertEquals(expected, response);
    }
  }

  /**
   * Get /documents request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      final long contentLength = 1000L;
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);
      item.setContentLength(Long.valueOf(contentLength));

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertNotNull(documents.get(0).get("documentId"));
      assertNotNull(documents.get(0).get("insertedDate"));
      assertNotNull(documents.get(0).get("userId"));
      assertEquals("1000.0", documents.get(0).get("contentLength").toString());
    }
  }

  /**
   * Get /documents request with next parameter.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      createTestData(siteId, DocumentService.MAX_RESULTS + 2);

      try (InputStream in = toStream("/request-get-documents-next.json")) {
        String input = IoUtils.toUtf8String(in);

        final InputStream instream =
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        // when
        getHandler().handleRequest(instream, outstream, getMockContext());

        // then
        String response = new String(outstream.toByteArray(), "UTF-8");
        Map<String, String> m = fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        assertEquals(getHeaders(),
            "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
        DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

        List<DynamicObject> documents = resp.getList("documents");
        assertEquals(DocumentService.MAX_RESULTS, documents.size());
        assertNull(resp.get("previous"));
        assertNotNull(resp.get("next"));

        expectNextPage(resp.getString("next"));
      }
    }
  }

  /**
   * Get /documents request with Limit=MAX_RESULTS.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ByteArrayOutputStream outstream = new ByteArrayOutputStream();
      createTestData(siteId, DocumentService.MAX_RESULTS);

      try (InputStream in = toStream("/request-get-documents-next.json")) {
        String input = IoUtils.toUtf8String(in);

        final InputStream instream =
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        // when
        getHandler().handleRequest(instream, outstream, getMockContext());

        // then
        String response = new String(outstream.toByteArray(), "UTF-8");
        Map<String, String> m = fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        assertEquals(getHeaders(),
            "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
        DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

        List<DynamicObject> documents = resp.getList("documents");
        assertEquals(DocumentService.MAX_RESULTS, documents.size());
        assertNull(resp.get("previous"));
        assertNull(resp.get("next"));
      }
    }
  }

  /**
   * Get /documents request with date / tz. Date after
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-tz.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(0, documents.size());
    }
  }

  /**
   * Get /documents request with date / tz.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = DateUtil.toDateFromString("2019-08-15", "0500");
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-tz.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
    }
  }

  /**
   * Get /documents request with tz. Date after
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ZoneOffset zone = ZoneOffset.of("-05:00");
      ZonedDateTime zdate = ZonedDateTime.now(zone);
      Date date = Date.from(zdate.toInstant());

      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-tz02.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());
      Thread.sleep(ONE_SECOND);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
    }
  }

  /**
   * Get /documents request for previous date.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments07() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date0 = new Date();
      final int year = 2015;
      final int dayOfMonth = 21;
      final long contentLength = 1111L;

      LocalDate localDate = LocalDate.of(year, Month.MARCH, dayOfMonth);
      Date date1 = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());

      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId0 = UUID.randomUUID().toString();
      String documentId1 = UUID.randomUUID().toString();
      DocumentItemDynamoDb item0 = new DocumentItemDynamoDb(documentId0, date0, username);
      item0.setContentLength(Long.valueOf(contentLength));

      DocumentItemDynamoDb item1 = new DocumentItemDynamoDb(documentId1, date1, username);
      item1.setContentLength(Long.valueOf(contentLength));

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-yesterday.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item0, new ArrayList<>());
      getDocumentService().saveDocument(siteId, item1, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      final int mapsize = 3;
      Map<String, String> m = fromJson(response, Map.class);
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertNotNull(documents.get(0).get("documentId"));
      assertNotNull(documents.get(0).get("insertedDate"));
      assertNotNull(documents.get(0).get("userId"));
      assertEquals("1111.0", documents.get(0).get("contentLength").toString());
    }
  }

  /**
   * Test user with no roles with siteid.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments08() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, "");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("403.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiResponseError resp = fromJson(m.get("body"), ApiResponseError.class);
    assertEquals("Access Denied", resp.getMessage());
  }

  /**
   * Test user with User role with siteid.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments09() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("403.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiResponseError resp = fromJson(m.get("body"), ApiResponseError.class);
    assertEquals("Access Denied", resp.getMessage());
  }

  /**
   * Test user with 'Finance' role with no siteid.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments10() throws Exception {
    // given
    String siteId = null;

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, "Finance");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    List<DynamicObject> documents = resp.getList("documents");
    assertEquals(0, documents.size());
  }

  /**
   * Test user with 'Finance' role with no siteid and belongs to multiple groups.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments11() throws Exception {
    // given
    String siteId = null;

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, "Finance", "Bleh");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    List<DynamicObject> documents = resp.getList("documents");
    assertEquals(0, documents.size());
  }

  /**
   * Test user with 'Finance' role with siteid.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments12() throws Exception {
    // given
    String siteId = "Finance";

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    List<DynamicObject> documents = resp.getList("documents");
    assertEquals(0, documents.size());
  }

  /**
   * Test IAM user API Gateway access.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments13() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents04.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(0, documents.size());
    }
  }

  /**
   * Options /documents request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleOptionsDocuments01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, username);

      ApiGatewayRequestEvent event = toRequestEvent("/request-options-documents.json");
      addParameter(event, "siteId", siteId);

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    }
  }

  /**
   * POST /documents request non Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments01() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      // when
      DynamicObject obj =
          handleRequest("/request-post-documents-documentid01.json", siteId, null, null);

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNull(body.getString("next"));
      assertNull(body.getString("previous"));

      String key = getKey(siteId, body);
      String documentId = body.getString("documentId");

      assertTrue(
          getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

      assertNotNull(documentId);

      verifyS3(key, true, "a0dac80d-18b3-472b-88da-79e75082b662@formkiq.com");
    }
  }

  /**
   * POST /documents request Base64 body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("201.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      assertNotNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));

      String key = siteId != null ? siteId + "/" + resp.get("documentId") + ".fkb64"
          : resp.get("documentId") + ".fkb64";

      assertTrue(
          getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

      assertNotNull(UUID.fromString(resp.getString("documentId")));

      try (S3Client s3 = getS3().buildClient()) {
        assertTrue(getS3().getObjectMetadata(s3, STAGE_BUCKET_NAME, key).isObjectExists());
        String content = getS3().getContentAsString(s3, STAGE_BUCKET_NAME, key, null);
        DynamicObject obj = new DynamicObject(fromJson(content, Map.class));
        assertTrue(obj.hasString("documentId"));
        assertTrue(obj.hasString("content"));
        assertEquals("7b87c0e2-5f20-403b-bcc6-d35cb491d3b7@formkiq.com", obj.getString("userId"));
      }
    }
  }

  /**
   * POST /documents request NO body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid03.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"request body is required\\\"}\",\"statusCode\":400}";

      assertEquals(expected, response);
    }
  }

  /**
   * POST /documents request Invalid JSON body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid04.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      assertNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * POST /documents request Invalid JSON body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid05.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      assertNull(resp.get("documentId"));
      assertNull(resp.get("next"));
      assertNull(resp.get("previous"));
    }
  }

  /**
   * POST /documents request Invalid body.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments06() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid06.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      String expected = "{" + getHeaders() + ",\"body\":\""
          + "{\\\"message\\\":\\\"invalid JSON body\\\"}\",\"statusCode\":400}";

      assertEquals(expected, response);
    }
  }

  /**
   * POST /documents without Group but has siteId set.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments07() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-documentid07.json");
    addParameter(event, "siteId", "demo");

    // when
    String response = handleRequest(event);

    // then
    String expected = "{" + getHeaders() + ",\"body\":\""
        + "{\\\"message\\\":\\\"Access Denied\\\"}\",\"statusCode\":403}";

    assertEquals(expected, response);
  }

  /**
   * POST /documents with default & default_read role.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments08() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents01.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

    assertNotNull(resp.get("documentId"));
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    String key = resp.get("documentId") + ".fkb64";

    assertTrue(
        getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

    assertNotNull(UUID.fromString(resp.getString("documentId")));

    try (S3Client s3 = getS3().buildClient()) {
      assertTrue(getS3().getObjectMetadata(s3, STAGE_BUCKET_NAME, key).isObjectExists());
    }
  }

  /**
   * POST /documents with default_read role.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments09() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents02.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("403.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  /**
   * POST /documents with sub documents.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments10() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      // when
      DynamicObject obj =
          handleRequest("/request-post-documents03.json", siteId, username, "Admins");

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNotNull(body.getString("uploadUrl"));

      List<DynamicObject> documents = body.getList("documents");
      final int count = 3;
      assertEquals(count, documents.size());

      int i = 0;
      assertNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i++).getString("documentId"));
      assertNotNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i++).getString("documentId"));
      assertNotNull(documents.get(i).getString("uploadUrl"));
      assertNotNull(documents.get(i++).getString("documentId"));

      String key = getKey(siteId, body);
      String documentId = body.getString("documentId");

      assertTrue(
          getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

      assertNotNull(documentId);

      verifyS3(key, false, username);
    }
  }

  /**
   * POST /documents with sub documents.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments11() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      // when
      DynamicObject obj =
          handleRequest("/request-post-documents04.json", siteId, username, "Admins");

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNotNull(body.getString("uploadUrl"));

      List<DynamicObject> documents = body.getList("documents");
      assertEquals(1, documents.size());

      assertNull(documents.get(0).getString("uploadUrl"));
      assertNotNull(documents.get(0).getString("documentId"));

      String key = getKey(siteId, body);
      String documentId = body.getString("documentId");

      assertTrue(
          getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

      assertNotNull(documentId);

      DynamicObject verifyS3 = verifyS3(key, false, username);
      assertNotNull(verifyS3.getString("documentId"));
      assertNull(verifyS3.getString("uploadUrl"));

      List<DynamicObject> list = verifyS3.getList("documents");
      assertEquals(1, list.size());
      assertNotNull(list.get(0).getString("documentId"));
      assertNotNull(list.get(0).getString("userId"));
    }
  }

  /**
   * POST /documents gutenburg content-type with IAM Role.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments12() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given

      // when
      DynamicObject obj = handleRequest("/request-post-documents05.json", siteId, null, null);

      // then
      DynamicObject body = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(body.getString("documentId"));
      assertNull(body.getString("uploadUrl"));

      assertEquals("[]", body.getList("documents").toString());

      String key = getKey(siteId, body);
      String documentId = body.getString("documentId");

      assertTrue(
          getLogger().containsString("s3 putObject " + key + " into bucket " + STAGE_BUCKET_NAME));

      assertNotNull(documentId);

      DynamicObject verifyS3 = verifyS3(key, false, null);
      assertNotNull(verifyS3.getString("documentId"));
      assertNotNull(verifyS3.getString("userId"));
      assertNull(verifyS3.getString("uploadUrl"));
      assertEquals("text/html", verifyS3.getString("contentType"));

      try (S3Client s3 = getS3().buildClient()) {
        assertEquals("text/html",
            getS3().getObjectMetadata(s3, STAGE_BUCKET_NAME, key).getContentType());
      }
    }
  }

  /**
   * Verify S3 File.
   * 
   * @param key {@link String}
   * @param hasContent boolean
   * @param userId {@link String}
   * @return {@link DynamicObject}
   */
  @SuppressWarnings("unchecked")
  private DynamicObject verifyS3(final String key, final boolean hasContent, final String userId) {
    try (S3Client s3 = getS3().buildClient()) {
      assertTrue(getS3().getObjectMetadata(s3, STAGE_BUCKET_NAME, key).isObjectExists());
      String content = getS3().getContentAsString(s3, STAGE_BUCKET_NAME, key, null);
      DynamicObject obj = new DynamicObject(fromJson(content, Map.class));

      assertTrue(obj.hasString("documentId"));

      if (userId != null) {
        assertEquals(userId, obj.getString("userId"));
      } else {
        assertNotNull(obj.getString("userId"));
      }

      if (hasContent) {
        assertTrue(obj.hasString("content"));
      }

      return obj;
    }
  }
}
