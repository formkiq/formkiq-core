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
package com.formkiq.stacks.api.awstest;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.WebhooksApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddWebhookRequest;
import com.formkiq.client.model.AddWebhookResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetWebhookResponse;
import com.formkiq.client.model.GetWebhooksResponse;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * Process Urls.
 * <p>
 * POST /private/webhooks tests
 * </p>
 *
 */
public class PrivateWebhooksRequestTest extends AbstractAwsIntegrationTest {

  /** Http Status OK. */
  private static final int STATUS_OK = 200;
  /** Http Status Unauthorized. */
  private static final int STATUS_UNAUTHORIZED = 401;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  private String getRootHttpUrl() {
    return getCognito().getRootJwtUrl();
  }

  /**
   * Test POST /private/webhooks.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPrivateWebhooks01() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {

      WebhooksApi api = new WebhooksApi(client);
      AddWebhookResponse addWebhook =
          api.addWebhook(new AddWebhookRequest().name("paypal").enabled("private"), null);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/private/webhooks/" + id;

      Optional<HttpHeaders> o = Optional.of(new HttpHeaders()
          .add("Authorization", getAdminToken().idToken()).add("Content-Type", "text/plain"));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJdk11();
      HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      Map<String, Object> map = GsonUtil.getInstance().fromJson(response.body(), Map.class);

      GetDocumentResponse document =
          waitForDocument(client, null, map.get("documentId").toString());
      assertNotNull(document);

      GetWebhooksResponse webhooks = api.getWebhooks(null, null, null);
      List<GetWebhookResponse> list = notNull(webhooks.getWebhooks());
      assertFalse(list.isEmpty());
      assertEquals(DEFAULT_SITE_ID, list.get(0).getSiteId());
      assertEquals("paypal", list.get(0).getName());
      assertNotNull(list.get(0).getUrl());
      assertNotNull(list.get(0).getInsertedDate());
      assertNotNull(list.get(0).getWebhookId());
      assertNotNull(list.get(0).getUrl());

      api.deleteWebhook(id, null);
    }
  }

  /**
   * Test POST /private/webhooks missing Authorization token.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks02() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {
      WebhooksApi api = new WebhooksApi(client);

      AddWebhookResponse addWebhook =
          api.addWebhook(new AddWebhookRequest().name("paypal").enabled("private"), null);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/private/webhooks/" + id;

      Optional<HttpHeaders> o = Optional.of(new HttpHeaders().add("Content-Type", "text/plain"));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJdk11();
      HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

      // then
      assertEquals(STATUS_UNAUTHORIZED, response.statusCode());
    }
  }

  /**
   * Test POST /private/webhooks with next token.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks03() throws Exception {
    // given
    final int count = 5;
    String siteId = ID.uuid();
    ApiClient client = getApiClients(siteId).get(0);
    WebhooksApi api = new WebhooksApi(client);

    for (int i = 0; i < count; i++) {
      api.addWebhook(new AddWebhookRequest().name("test_" + i), siteId);
    }

    // when
    GetWebhooksResponse response = api.getWebhooks(siteId, null, "2");

    // then
    List<GetWebhookResponse> webhooks = notNull(response.getWebhooks());
    assertEquals(2, webhooks.size());
    assertEquals("test_0", webhooks.get(0).getName());
    assertEquals("test_1", webhooks.get(1).getName());

    response = api.getWebhooks(siteId, response.getNext(), "2");
    webhooks = notNull(response.getWebhooks());
    assertEquals(2, webhooks.size());
    assertEquals("test_2", webhooks.get(0).getName());
    assertEquals("test_3", webhooks.get(1).getName());
  }
}
