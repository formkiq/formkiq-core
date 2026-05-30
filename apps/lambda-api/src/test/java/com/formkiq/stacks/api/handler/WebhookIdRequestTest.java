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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Date;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddWebhookRequest;
import com.formkiq.client.model.AddWebhookResponse;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetWebhookResponse;
import com.formkiq.client.model.GetWebhookTagsResponse;
import com.formkiq.client.model.UpdateResponse;
import org.junit.jupiter.api.Test;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** Unit Tests for request /webhooks/{webhookId}. */
public class WebhookIdRequestTest extends AbstractApiClientRequestTest {

  private void putSsmParameter() {
    SsmService ssm = getAwsServices().getExtension(SsmService.class);
    ssm.putParameter("/formkiq/test/api/DocumentsPublicHttpUrl", "http://localhost:8080");
  }

  /**
   * Delete /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteWebhooks01() throws Exception {
    // given
    setBearerToken(DEFAULT_SITE_ID);

    AddWebhookResponse response =
        this.webhooksApi.addWebhook(new AddWebhookRequest().name("test"), null);

    assertEquals(DEFAULT_SITE_ID, response.getSiteId());
    assertNotNull(response.getWebhookId());

    String webhookId = response.getWebhookId();

    // when
    DeleteResponse deleteResponse = this.webhooksApi.deleteWebhook(webhookId, null);

    // then
    assertEquals("'" + webhookId + "' object deleted", deleteResponse.getMessage());
  }

  /**
   * Delete /webhooks/{webhookId} not found.
   *
   */
  @Test
  public void testDeleteWebhooks02() {
    // given
    String id = ID.uuid();
    setBearerToken(DEFAULT_SITE_ID);

    // when
    try {
      this.webhooksApi.deleteWebhook(id, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_NOT_FOUND.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"Webhook 'id' not found\"}", e.getResponseBody());
    }
  }

  /**
   * Delete /webhooks/{webhookId} and webhook tags.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testDeleteWebhooks03() throws Exception {
    // given
    setBearerToken(DEFAULT_SITE_ID);

    AddWebhookResponse response = this.webhooksApi.addWebhook(new AddWebhookRequest()
        .name("john smith").addTagsItem(new AddDocumentTag().key("dynamodb")), null);

    String webhookId = response.getWebhookId();

    // when
    DeleteResponse deleteResponse = this.webhooksApi.deleteWebhook(webhookId, null);

    // then
    assertEquals("'" + webhookId + "' object deleted", deleteResponse.getMessage());

    GetWebhookTagsResponse webhookTags = this.webhooksApi.getWebhookTags(webhookId, null);
    assertEquals(0, notNull(webhookTags.getTags()).size());
  }

  /**
   * GET /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWebhook01() throws Exception {
    putSsmParameter();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      AddWebhookResponse response =
          this.webhooksApi.addWebhook(new AddWebhookRequest().name("john smith"), null);

      String webhookId = response.getWebhookId();

      // when
      GetWebhookResponse webhook = this.webhooksApi.getWebhook(webhookId, null);

      // then
      if (siteId == null) {
        assertEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      } else {
        assertNotNull(webhook.getSiteId());
        assertNotEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      }

      webhookId = webhook.getWebhookId();

      assertNotNull(webhook.getInsertedDate());
      assertEquals("john smith", webhook.getName());
      assertEquals("joesmith", webhook.getUserId());

      verifyUrl(siteId, webhookId, webhook, true);
    }
  }

  /**
   * GET /webhooks/{webhookId} with PRIVATE.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testGetWebhook02() throws Exception {
    putSsmParameter();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      AddWebhookResponse result = this.webhooksApi
          .addWebhook(new AddWebhookRequest().name("john smith").enabled("private"), siteId);

      assertNotNull(result.getWebhookId());
      String webhookId = result.getWebhookId();

      // when
      GetWebhookResponse webhook = this.webhooksApi.getWebhook(webhookId, null);

      // then
      if (siteId == null) {
        assertEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      } else {
        assertNotNull(webhook.getSiteId());
        assertNotEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      }

      assertNotNull(webhook.getWebhookId());
      webhookId = webhook.getWebhookId();

      assertNotNull(webhook.getInsertedDate());
      assertEquals("john smith", webhook.getName());
      assertEquals("joesmith", webhook.getUserId());

      verifyUrl(siteId, webhookId, webhook, false);
    }
  }

  /**
   * PATCH /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testPatchWebhook01() throws Exception {
    Date date = new Date();
    putSsmParameter();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String id = getAwsServices().getExtension(WebhooksService.class).saveWebhook(siteId, "test",
          "joe", date, "true");

      // when
      UpdateResponse updateResponse = this.webhooksApi.updateWebhook(id,
          new AddWebhookRequest().name("john smith2").enabled("false"), siteId);

      // then
      assertEquals("'" + id + "' object updated", updateResponse.getMessage());

      GetWebhookResponse webhook = this.webhooksApi.getWebhook(id, siteId);

      if (siteId == null) {
        assertEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      } else {
        assertNotNull(webhook.getSiteId());
        assertNotEquals(DEFAULT_SITE_ID, webhook.getSiteId());
      }

      id = webhook.getWebhookId();

      assertNotNull(webhook.getInsertedDate());
      assertEquals("john smith2", webhook.getName());
      assertEquals("joe", webhook.getUserId());
      assertEquals("false", webhook.getEnabled());

      verifyUrl(siteId, id, webhook, true);
    }
  }

  private void verifyUrl(final String siteId, final String id, final GetWebhookResponse result,
      final boolean publicUrl) {
    String path = publicUrl ? "/public" : "/private";
    if (siteId == null) {
      assertEquals("http://localhost:8080" + path + "/webhooks/" + id, result.getUrl());
      assertEquals(DEFAULT_SITE_ID, result.getSiteId());
    } else {
      assertNotNull(result.getSiteId());
      assertNotEquals(DEFAULT_SITE_ID, result.getSiteId());
      assertEquals("http://localhost:8080" + path + "/webhooks/" + id + "?siteId=" + siteId,
          result.getUrl());
    }
  }
}
