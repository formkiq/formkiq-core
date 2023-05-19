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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.stacks.dynamodb.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request POST /public/webhooks. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiPublicWebhooksRequestTest extends AbstractRequestHandler {

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_DOC_EXT = ".fkb64";

  /**
   * Post /public/webhooks without authentication.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks01() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      String documentId = verifyDocumentId(m);

      verifyS3File(id, siteId, documentId, name, null, false);
    }
  }

  /**
   * Post /public/webhooks without authentication BASE 64 ENCODED.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks02() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();
      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks02.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      String documentId = verifyDocumentId(m);

      // verify s3 file
      verifyS3File(id, siteId, documentId, name, "application/json", false);
    }
  }

  /**
   * Post /public/webhooks with NO BODY.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks03() throws Exception {
    // givens
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks03.json");
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    verifyHeaders(m, "400.0");
  }

  /**
   * Post /public/webhooks to expired webhook.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks04() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String name = UUID.randomUUID().toString();

      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong("-1000"));
      Date ttl = Date.from(now.toInstant());
      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", ttl, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "400.0");
    }
  }

  /**
   * Post /public/webhooks to ttl webhook NOT expired.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks05() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong("1000"));
      Date ttl = Date.from(now.toInstant());
      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", ttl, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      Map<String, Object> body = fromJson(m.get("body"), Map.class);
      String documentId = body.get("documentId").toString();
      assertNotNull(documentId);

      verifyS3File(id, siteId, documentId, name, null, true);
    }
  }

  /**
   * Post /public/webhooks without authentication and with redirect_uri.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks06() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks04.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      final int mapsize = 2;
      assertEquals(mapsize, m.size());

      assertEquals("301.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> headers = (Map<String, Object>) m.get("headers");
      assertEquals("https://www.google.ca", headers.get("Location"));
    }
  }

  /**
   * Post /public/webhooks without authentication and with redirect_uri and responseFields.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks07() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks05.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      final int mapsize = 2;
      assertEquals(mapsize, m.size());

      assertEquals("301.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> headers = (Map<String, Object>) m.get("headers");
      assertEquals("https://www.google.ca?client=safari&fname=John&lname=Doe&kdhfsd=null",
          headers.get("Location"));
    }
  }

  /**
   * Post /public/webhooks without authentication and with redirect_uri and responseFields.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks08() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks06.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      final int mapsize = 2;
      assertEquals(mapsize, m.size());

      assertEquals("301.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> headers = (Map<String, Object>) m.get("headers");
      assertEquals("https://www.google.ca?fname=John&lname=Doe&kdhfsd=null",
          headers.get("Location"));
    }
  }

  /**
   * Post /public/webhooks without authentication and with redirect_uri and responseFields.
   * Content-Type: application/json
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks09() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks06.json", siteId, id);

      HashMap<String, String> headers = new HashMap<>(event.getHeaders());
      headers.put("Content-Type", "application/json");
      event.setHeaders(headers);
      event.setBody("{\"name\":\"john smith\"}");
      event.setIsBase64Encoded(Boolean.FALSE);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);
      final int mapsize = 2;
      assertEquals(mapsize, m.size());

      assertEquals("301.0", String.valueOf(m.get("statusCode")));

      assertEquals("https://www.google.ca",
          ((Map<String, Object>) m.get("headers")).get("Location"));
    }
  }

  /**
   * Post /public/webhooks to disabled webhook.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks10() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "false");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "429.0");
    }
  }

  /**
   * Post /public/webhooks application/json, INVALID JSON body.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks11() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks06.json", siteId, id);
      Map<String, String> headers = new HashMap<>(event.getHeaders());
      headers.put("Content-Type", "application/json");
      event.setHeaders(headers);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "400.0");
      assertEquals("{\"message\":\"body isn't valid JSON\"}", m.get("body"));
    }
  }

  /**
   * Post /public/webhooks with Config 'DocumentTimeToLive'.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks12() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      ConfigService configService = getAwsServices().getExtension(ConfigService.class);
      configService.save(siteId, new DynamicObject(Map.of(DOCUMENT_TIME_TO_LIVE, "1000")));

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      Map<String, Object> body = fromJson(m.get("body"), Map.class);
      String documentId = body.get("documentId").toString();
      assertNotNull(documentId);

      // verify s3 file
      String key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);
      String json = getS3().getContentAsString(STAGE_BUCKET_NAME, key, null);
      Map<String, Object> map = fromJson(json, Map.class);
      assertEquals(documentId, map.get("documentId"));
      assertEquals("webhook/" + name, map.get("userId"));
      assertEquals("webhooks/" + id, map.get("path"));
      assertEquals("{\"name\":\"john smith\"}", map.get("content"));
      assertEquals("1000", map.get("TimeToLive"));
    }
  }

  /**
   * Post /public/webhooks with Idempotency-Key.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks13() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String idempotencyKey = UUID.randomUUID().toString();

      String name = UUID.randomUUID().toString();

      String id = getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "true");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);
      event.addHeader("Idempotency-Key", idempotencyKey);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      String documentId = verifyDocumentId(m);

      verifyS3File(id, siteId, documentId, name, null, false);

      String key = SiteIdKeyGenerator.createDatabaseKey(siteId, "idkey#" + idempotencyKey);

      CacheService cacheService = getAwsServices().getExtension(CacheService.class);
      assertEquals(documentId, cacheService.read(key));

      // given
      // when
      response = handleRequest(event);

      // then
      m = fromJson(response, Map.class);
      verifyHeaders(m, "200.0");

      String s3key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);
      S3ObjectMetadata om = getS3().getObjectMetadata(STAGE_BUCKET_NAME, s3key, null);
      assertFalse(om.isObjectExists());
    }
  }

  /**
   * Post /public/webhooks with enabled=private .
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks14() throws Exception {
    // given
    createApiRequestHandler(getMap());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String id =
          getAwsServices().webhookService().saveWebhook(siteId, name, "joe", null, "private");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-post-public-webhooks01.json", siteId, id);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      verifyHeaders(m, "401.0");
    }
  }

  private ApiGatewayRequestEvent toRequestEvent(final String file, final String siteId,
      final String webhookId) throws IOException {

    ApiGatewayRequestEvent event = toRequestEvent(file);

    if (webhookId != null) {
      setPathParameter(event, "webhooks", webhookId);
    }

    if (siteId != null) {
      addParameter(event, "siteId", siteId);
    }

    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    return event;
  }

  @SuppressWarnings("unchecked")
  private String verifyDocumentId(final Map<String, String> m) {
    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    String documentId = body.get("documentId").toString();
    assertNotNull(documentId);
    return documentId;
  }

  private void verifyHeaders(final Map<String, String> map, final String statusCode) {
    final int mapsize = 3;
    assertEquals(mapsize, map.size());
    assertEquals(statusCode, String.valueOf(map.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(map.get("headers")));
  }

  @SuppressWarnings("unchecked")
  private void verifyS3File(final String webhookId, final String siteId, final String documentId,
      final String name, final String contentType, final boolean hasTimeToLive) throws Exception {

    // verify s3 file
    String key = createDatabaseKey(siteId, documentId + FORMKIQ_DOC_EXT);
    String json = getS3().getContentAsString(STAGE_BUCKET_NAME, key, null);

    Map<String, Object> map = fromJson(json, Map.class);
    assertEquals(documentId, map.get("documentId"));
    assertEquals("webhook/" + name, map.get("userId"));
    assertEquals("webhooks/" + webhookId, map.get("path"));
    assertEquals("{\"name\":\"john smith\"}", map.get("content"));

    if (contentType != null) {
      assertEquals("application/json", map.get("contentType"));
    }

    if (hasTimeToLive) {
      DynamicObject obj = getAwsServices().webhookService().findWebhook(siteId, webhookId);
      assertNotNull(obj.get("TimeToLive"));
      assertEquals(obj.get("TimeToLive"), map.get("TimeToLive"));
    }

    getS3().deleteObject(STAGE_BUCKET_NAME, key, null);
  }
}
