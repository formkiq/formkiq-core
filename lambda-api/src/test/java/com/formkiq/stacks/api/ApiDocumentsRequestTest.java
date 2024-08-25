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
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.aws.services.lambda.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for request GET / POST / DELETE /documents. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ApiDocumentsRequestTest extends AbstractRequestHandler {

  /** One Second. */
  private static final long ONE_SECOND = 1000L;

  /**
   * Create MAX_RESULTS + 2 Documents test data.
   * 
   * @param prefix {@link String}
   * @param testdatacount int
   * @throws Exception Exception
   */
  private void createTestData(final String prefix, final int testdatacount) throws Exception {
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
   * DELETE /documents request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteDocument01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      getDocumentService().saveDocument(siteId, item, null);

      String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      getS3().putObject(BUCKET_NAME, s3Key, "testdata".getBytes(StandardCharsets.UTF_8), null);

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      assertFalse(getS3().getObjectMetadata(BUCKET_NAME, s3Key, null).isObjectExists());
      assertNull(getDocumentService().findDocument(siteId, documentId));
    }
  }

  /**
   * DELETE /documents request that S3 file doesn't exist.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteDocument02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");

      getDocumentService().saveDocument(siteId, item, null);
      assertNotNull(getDocumentService().findDocument(siteId, documentId));

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid02.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      assertNull(getDocumentService().findDocument(siteId, documentId));

      Map<String, String> m = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * DELETE /documents request. Document doesn't exist.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteDocument03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event = toRequestEvent("/request-delete-documents-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
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
      assertNotNull(documents.get(0).get("lastModifiedDate"));
      assertEquals(documents.get(0).get("insertedDate"), documents.get(0).get("lastModifiedDate"));
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
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiResponseError resp = fromJson(m.get("body"), ApiResponseError.class);
    assertEquals("fkq access denied to siteId (" + siteId + ")", resp.getMessage());
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
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiResponseError resp = fromJson(m.get("body"), ApiResponseError.class);
    assertEquals("fkq access denied to siteId (" + siteId + ")", resp.getMessage());
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
    String siteId = "default";

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents03.json");
    addParameter(event, "siteId", siteId);
    setCognitoGroup(event, "Finance Bleh");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"message\":\"fkq access denied to siteId (default)\"}", m.get("body"));
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
   * Get /documents request for documents created in previous days.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments14() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int year = 2020;
      Date date =
          Date.from(LocalDate.of(year, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

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
      assertTrue(documents.get(0).getString("insertedDate").startsWith("" + year));
      assertTrue(documents.get(0).getString("lastModifiedDate").startsWith("" + year));
    }
  }

  /**
   * Get /documents request with "actionStatus".
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments15() throws Exception {

    ActionsService actions = getAwsServices().getExtension(ActionsService.class);

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
      addParameter(event, "actionStatus", "pending");

      getDocumentService().saveDocument(siteId, item, new ArrayList<>());
      actions.saveAction(siteId,
          new Action().index("0").type(ActionType.OCR).documentId(documentId).userId("joe"));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("documents");
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).get("documentId"));
      assertNull(documents.get(0).get("insertedDate"));
      assertNull(documents.get(0).get("lastModifiedDate"));
      assertNull(documents.get(0).get("userId"));
    }
  }

  /**
   * Get /documents request with invalid "actionStatus".
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocuments16() throws Exception {

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
      addParameter(event, "actionStatus", "nothing");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"invalid actionStatus 'nothing'\"}", m.get("body"));
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
      setCognitoGroup(event, siteId);

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
   * POST /documents request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent postDocumentsRequest(final String siteId, final String group,
      final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("post")
        .resource("/documents").path("/documents").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();

    return event;
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
      String body =
          "{\"isBase64\":true,\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
              + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
              + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body));

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));
      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNotNull(o.getString("documentId"));
      assertNull(o.getString("next"));
      assertNull(o.getString("previous"));

      String documentId = o.getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);

      DocumentItem item = getDocumentService().findDocument(siteId, documentId);
      assertEquals("joesmith", item.getUserId());

      String content = getS3().getContentAsString(BUCKET_NAME, key, null);
      assertEquals("this is a test", content);
    }
  }

  /**
   * POST /documents request webhook invalid action.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments19() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"webhook\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"parameters.url\","
          + "\"error\":\"action 'url' parameter is required\"}]}", obj.getString("body"));
    }
  }

  /**
   * POST /documents request webhook valid action.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments20() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"webhook\",\"parameters\":{\"url\":\"http://localhost\"}}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body));

      // then
      assertEquals("201.0", obj.getString("statusCode"));
    }
  }

  /**
   * POST /documents request webhook invalid type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments21() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type\":\"something\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"type\",\"error\":\"action 'type' is required\"}]}",
          obj.getString("body"));
    }
  }

  /**
   * POST /documents request missing type.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandlePostDocuments22() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"actions\":[{\"type2\":\"something\"}]}";

      // when
      DynamicObject obj = handleRequestDynamic(
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body));

      // then
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"type\",\"error\":\"action 'type' is required\"}]}",
          obj.getString("body"));
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

      String key = siteId != null ? siteId + "/" + resp.get("documentId")
          : resp.get("documentId").toString();

      assertNotNull(getDocumentService().findDocument(siteId, resp.getString("documentId")));
      assertNotNull(UUID.fromString(resp.getString("documentId")));

      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());
      String content = getS3().getContentAsString(BUCKET_NAME, key, null);
      assertEquals("dGhpcyBpcyBhIHRlc3Q=", content);
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
        + "{\\\"message\\\":\\\"fkq access denied to siteId (demo)\\\"}\",\"statusCode\":401}";

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

    String documentId = resp.get("documentId").toString();
    assertNull(resp.get("next"));
    assertNull(resp.get("previous"));

    assertNotNull(UUID.fromString(documentId));
    assertNotNull(getDocumentService().findDocument(null, documentId));

    assertTrue(getS3().getObjectMetadata(BUCKET_NAME, documentId, null).isObjectExists());
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
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
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

      String documentId = documents.get(0).getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());
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

      String key = SiteIdKeyGenerator.createS3Key(siteId, documents.get(0).getString("documentId"));
      assertTrue(getS3().getObjectMetadata(BUCKET_NAME, key, null).isObjectExists());

      assertNotNull(getDocumentService().findDocument(siteId, body.getString("documentId")));
      assertNotNull(
          getDocumentService().findDocument(siteId, documents.get(0).getString("documentId")));
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

      String documentId = body.getString("documentId");
      String key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
      assertNotNull(getDocumentService().findDocument(siteId, documentId));

      assertEquals("application/octet-stream",
          getS3().getObjectMetadata(BUCKET_NAME, key, null).getContentType());
    }
  }

  /**
   * POST /documents to create index "folder".
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments15() throws Exception {
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      // given
      String body = "{\"path\":\"something/bleh/\"}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body);

      // when
      DynamicObject obj = handleRequestDynamic(event);

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("201.0", obj.getString("statusCode"));

      assertNull(o.getString("documentId"));
      assertEquals("folder created", o.getString("message"));

      DocumentSearchService search = getAwsServices().getExtension(DocumentSearchService.class);

      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));
      PaginationResults<DynamicDocumentItem> results = search.search(siteId, q, null, null, 2);
      assertEquals(1, results.getResults().size());
      assertEquals("something", results.getResults().get(0).get("path"));

      q = new SearchQuery().meta(new SearchMetaCriteria().folder("something"));
      results = search.search(siteId, q, null, null, 2);
      assertEquals(1, results.getResults().size());
      assertEquals("bleh", results.getResults().get(0).get("path"));

      // given
      // when
      String response = handleRequest(event);
      obj = new DynamicObject(fromJson(response, Map.class));

      // then
      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{\"errors\":[{\"key\":\"folder\",\"error\":\"already exists\"}]}",
          obj.getString("body"));
    }
  }

  /**
   * POST /documents too many metadata.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments16() throws Exception {
    // given
    final int count = 30;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
          + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";
      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body);

      // when

      Map<String, Object> data = fromJson(event.getBody(), Map.class);
      List<Map<String, Object>> metadata = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        metadata.add(Map.of("key", "ad_" + i, "value", "some"));
      }
      data.put("metadata", metadata);
      event.setBody(GsonUtil.getInstance().toJson(data));

      // when
      String response = handleRequest(event);

      // then
      DynamicObject obj = new DynamicObject(fromJson(response, Map.class));
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=metadata, error=maximum number is 25}]}", o.toString());
    }
  }

  /**
   * POST /documents too large meta data.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments17() throws Exception {
    // given
    final int count = 1001;
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {
      String filled = StringUtils.repeat("*", count);

      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"firstname\",\"value\":\"john\"},"
          + "{\"key\":\"lastname\",\"value\":\"smith\"}]}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body);

      Map<String, Object> data = fromJson(event.getBody(), Map.class);
      List<Map<String, Object>> metadata = new ArrayList<>();
      metadata.add(Map.of("key", "ad1", "value", filled));
      metadata.add(Map.of("key", "ad2", "values", Arrays.asList(filled)));

      data.put("metadata", metadata);
      event.setBody(GsonUtil.getInstance().toJson(data));

      // when
      String response = handleRequest(event);

      // then
      DynamicObject obj = new DynamicObject(fromJson(response, Map.class));
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=ad1, error=value cannot exceed 1000}, "
          + "{key=ad2, error=value cannot exceed 1000}]}", o.toString());
    }
  }

  /**
   * POST /documents invalid tag.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocuments18() throws Exception {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, UUID.randomUUID().toString())) {

      String body = "{\"content\": \"dGhpcyBpcyBhIHRlc3Q=\",\"path\": \"/file/test.txt\","
          + "\"tags\":[{\"key\":\"CLAMAV_SCAN_STATUS\",\"value\":\"john\"}]}";

      ApiGatewayRequestEvent event =
          postDocumentsRequest(siteId, siteId != null ? siteId : "default", body);

      // when
      DynamicObject obj = handleRequestDynamic(event);

      // then
      DynamicObject o = new DynamicObject(fromJson(obj.getString("body"), Map.class));

      assertHeaders(obj.getMap("headers"));
      assertEquals("400.0", obj.getString("statusCode"));
      assertEquals("{errors=[{key=CLAMAV_SCAN_STATUS, error=unallowed tag key}]}", o.toString());
    }
  }
}
