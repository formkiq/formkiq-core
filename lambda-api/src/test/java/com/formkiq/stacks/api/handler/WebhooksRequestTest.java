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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddWebhookRequest;
import com.formkiq.client.model.AddWebhookResponse;
import com.formkiq.client.model.GetWebhookResponse;
import com.formkiq.client.model.GetWebhooksResponse;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** Unit Tests for request /webhooks. */
public class WebhooksRequestTest extends AbstractApiClientRequestTest {

  /** To Milliseconds. */
  private static final long TO_MILLIS = 1000L;

  private void putSsmParameter() {
    SsmService ssm = getAwsServices().getExtension(SsmService.class);
    ssm.putParameter("/formkiq/test/api/DocumentsPublicHttpUrl", "http://localhost:8080");
  }

  /**
   * Get /webhooks empty.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWebhooks01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      // when
      GetWebhooksResponse response = this.webhooksApi.getWebhooks(siteId, null, null);

      // then
      assertTrue(notNull(response.getWebhooks()).isEmpty());
    }
  }

  /**
   * POST/Get /webhooks.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWebhooks02() throws Exception {
    // given
    putSsmParameter();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      // when
      AddWebhookResponse response =
          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), siteId);

      // then
      if (siteId == null) {
        assertEquals(DEFAULT_SITE_ID, response.getSiteId());
      } else {
        assertNotNull(response.getSiteId());
        assertNotEquals(DEFAULT_SITE_ID, response.getSiteId());
      }

      assertNotNull(response.getWebhookId());
      String webhookId = response.getWebhookId();

      // when
      GetWebhooksResponse getResponse = this.webhooksApi.getWebhooks(siteId, null, null);

      // then
      List<GetWebhookResponse> webhooks = notNull(getResponse.getWebhooks());
      Optional<GetWebhookResponse> o = webhooks.stream()
          .filter(l -> Objects.requireNonNull(l.getUrl()).contains(webhookId)).findFirst();
      assertTrue(o.isPresent());

      assertNotNull(o.get().getInsertedDate());
      assertNotNull(o.get().getWebhookId());
      assertEquals("john smith", o.get().getName());
      assertEquals("true", o.get().getEnabled());

      verifyUrl(siteId, webhookId, o.get());

      assertEquals("joesmith", o.get().getUserId());
    }
  }

  /**
   * Get /webhooks paginating.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWebhooks03() throws Exception {
    // given
    final int count = 9;
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      setBearerToken(siteId);

      for (int i = 0; i < count; i++) {
        this.webhooksApi.addWebhook(new AddWebhookRequest().name("test_" + i), siteId);
      }

      // when
      GetWebhooksResponse response = this.webhooksApi.getWebhooks(siteId, null, null);

      // then
      assertEquals(count, notNull(response.getWebhooks()).size());

      // when
      response = this.webhooksApi.getWebhooks(siteId, null, "2");

      // then
      List<GetWebhookResponse> webhooks = notNull(response.getWebhooks());

      assertEquals(2, webhooks.size());
      assertEquals("test_0", webhooks.get(0).getName());
      assertEquals("test_1", webhooks.get(1).getName());

      // when
      response = this.webhooksApi.getWebhooks(siteId, response.getNext(), "3");

      // then
      final int expected = 3;
      webhooks = notNull(response.getWebhooks());

      assertEquals(expected, webhooks.size());
      assertEquals("test_2", webhooks.get(0).getName());
      assertEquals("test_3", webhooks.get(1).getName());
      assertEquals("test_4", webhooks.get(2).getName());
    }
  }

  /**
   * POST /webhooks. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks01() throws Exception {
    // given
    putSsmParameter();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String ttl = "90000";
      setBearerToken(siteId);

      // when
      AddWebhookResponse result =
          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith").ttl(ttl), siteId);

      // then
      assertEquals(result.getSiteId(), siteId != null ? siteId : DEFAULT_SITE_ID);
      String webhookId = result.getWebhookId();

      WebhooksService webhookService = getAwsServices().getExtension(WebhooksService.class);
      DynamicObject obj = webhookService.findWebhook(siteId, webhookId);
      assertNotNull(obj);

      long epoch = Long.parseLong(obj.getString("TimeToLive"));
      ZonedDateTime ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1);
      assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

      // given
      DocumentTag tag = new DocumentTag(webhookId, "category", "person", new Date(), "joe");

      // when
      webhookService.addTags(siteId, webhookId, List.of(tag), null);

      // then
      assertNull(webhookService.findTag(siteId, webhookId, "category").getString("TimeToLive"));

      // update ttl/name
      // given
      ttl = "180000";

      // when
      UpdateResponse response = this.webhooksApi.updateWebhook(webhookId,
          new AddWebhookRequest().name("john smith2").ttl(ttl), siteId);

      // then
      assertEquals("'" + webhookId + "' object updated", response.getMessage());

      obj = webhookService.findWebhook(siteId, webhookId);
      assertEquals("john smith2", obj.get("path"));

      epoch = Long.parseLong(obj.getString("TimeToLive"));
      ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
      now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(2);
      assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

      DynamicObject dtag = webhookService.findTag(siteId, webhookId, "category");
      assertNotNull(dtag.getString("TimeToLive"));
      epoch = Long.parseLong(dtag.getString("TimeToLive"));
      ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
      now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(2);
      assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());
    }
  }

  /**
   * POST /webhooks with TAGS. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks02() throws Exception {
    // given
    putSsmParameter();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      setBearerToken(siteId);

      AddWebhookRequest req = new AddWebhookRequest().name("joe smith")
          .tags(List.of(new AddDocumentTag().key("category").value("person"),
              new AddDocumentTag().key("day").value("today")));

      // when
      AddWebhookResponse result = this.webhooksApi.addWebhook(req, siteId);

      // then
      assertEquals(result.getSiteId(), siteId != null ? siteId : DEFAULT_SITE_ID);

      assertNotNull(result.getWebhookId());
      final String webhookId = result.getWebhookId();

      WebhooksService webhookService = getAwsServices().getExtension(WebhooksService.class);
      DynamicObject obj = webhookService.findWebhook(siteId, webhookId);
      assertEquals("joe smith", obj.getString("path"));
      assertEquals("joesmith", obj.getString("userId"));

      PaginationResults<DynamicObject> tags = webhookService.findTags(siteId, webhookId, null);
      assertEquals(2, tags.getResults().size());

      DynamicObject tag = tags.getResults().get(0);

      assertEquals(webhookId, tag.getString("documentId"));
      assertEquals("USERDEFINED", tag.getString("type"));
      assertEquals("joesmith", tag.getString("userId"));
      assertEquals("category", tag.getString("tagKey"));
      assertEquals("person", tag.getString("tagValue"));
      assertNotNull(tag.getString("inserteddate"));

      tag = tags.getResults().get(1);
      assertEquals(webhookId, tag.getString("documentId"));
      assertEquals("USERDEFINED", tag.getString("type"));
      assertEquals("joesmith", tag.getString("userId"));
      assertEquals("day", tag.getString("tagKey"));
      assertEquals("today", tag.getString("tagValue"));
      assertNotNull(tag.getString("inserteddate"));
    }
  }

  /**
   * POST /webhooks with NO SiteId Set.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks03() throws Exception {
    // given
    putSsmParameter();

    String siteId = ID.uuid();
    setBearerToken(siteId);

    // when
    AddWebhookResponse result =
        this.webhooksApi.addWebhook(new AddWebhookRequest().name("joe smith"), siteId);

    // then
    assertEquals(siteId, result.getSiteId());

    assertNotNull(result.getWebhookId());
    final String webhookId = result.getWebhookId();

    WebhooksService webhookService = getAwsServices().getExtension(WebhooksService.class);
    DynamicObject obj = webhookService.findWebhook(siteId, webhookId);
    assertEquals("joe smith", obj.getString("path"));
    assertEquals("joesmith", obj.getString("userId"));
  }

  /**
   * POST /webhooks too many webhooks.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks04() throws Exception {
    // given
    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    for (String maxWebHooks : Arrays.asList("2", "0")) {

      for (String siteId : Arrays.asList(null, ID.uuid())) {

        setBearerToken(siteId);
        SiteConfiguration config = new SiteConfiguration().setMaxWebhooks(maxWebHooks);

        if (!"0".equals(maxWebHooks)) {
          configService.save(siteId, config);

          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), siteId);
          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), siteId);

        } else {
          configService.save(siteId, config);
        }

        try {
          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), siteId);
          fail();
        } catch (ApiException e) {
          assertEquals(ApiResponseStatus.SC_TOO_MANY_REQUESTS.getStatusCode(), e.getCode());
          assertEquals("{\"message\":\"Reached max number of webhooks\"}", e.getResponseBody());
        }
      }
    }
  }

  /**
   * POST /webhooks with Config WEBHOOK_TIME_TO_LIVE.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks05() throws Exception {
    // given
    putSsmParameter();

    setBearerToken(DEFAULT_SITE_ID);

    String ttl = "87400";

    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    SiteConfiguration config = new SiteConfiguration().setWebhookTimeToLive(ttl);
    configService.save(null, config);

    // when
    AddWebhookResponse result =
        this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), null);

    // then
    GetWebhooksResponse webhooks = this.webhooksApi.getWebhooks(null, null, null);

    assertEquals(result.getSiteId(), DEFAULT_SITE_ID);
    assertNotNull(result.getWebhookId());
    String webhookId = result.getWebhookId();

    List<GetWebhookResponse> list = notNull(webhooks.getWebhooks());
    assertEquals(1, list.size());

    WebhooksService webhookService = getAwsServices().getExtension(WebhooksService.class);
    DynamicObject obj = webhookService.findWebhook(null, webhookId);

    long epoch = Long.parseLong(obj.getString("TimeToLive"));
    ZonedDateTime ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

    // given
    ttl = "-87400";
    config.setDocumentTimeToLive(ttl);
    configService.save(null, config);

    // when
    this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), null);
  }

  /**
   * POST /webhooks. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPostWebhooks06() throws Exception {
    // given
    putSsmParameter();

    // when
    AddWebhookResponse response = this.webhooksApi
        .addWebhook(new AddWebhookRequest().name("john smith").enabled("private"), null);

    // then
    assertEquals(DEFAULT_SITE_ID, response.getSiteId());
    assertNotNull(response.getWebhookId());
  }

  private void verifyUrl(final String siteId, final String id, final GetWebhookResponse result) {
    if (siteId == null) {
      assertEquals("http://localhost:8080/public/webhooks/" + id, result.getUrl());
      assertEquals(DEFAULT_SITE_ID, result.getSiteId());
    } else {
      assertNotNull(result.getSiteId());
      assertNotEquals(DEFAULT_SITE_ID, result.getSiteId());
      assertEquals("http://localhost:8080/public/webhooks/" + id + "?siteId=" + siteId,
          result.getUrl());
    }
  }
}
