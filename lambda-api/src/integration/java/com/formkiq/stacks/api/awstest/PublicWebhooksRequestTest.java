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

import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.aws.services.lambda.GsonUtil;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.WebhooksApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddWebhookRequest;
import com.formkiq.client.model.AddWebhookResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetWebhookResponse;
import com.formkiq.client.model.GetWebhooksResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * Process Urls.
 * <p>
 * POST /public/webhooks tests
 * </p>
 *
 */
public class PublicWebhooksRequestTest extends AbstractAwsIntegrationTest {

  /** Http Status OK. */
  private static final int STATUS_OK = 200;
  /** Http Status BAD. */
  private static final int STATUS_BAD = 400;
  /** Http Status Unauthorized. */
  private static final int STATUS_UNAUTHORIZED = 401;
  /** Http Status NOT_FOUND. */
  private static final int STATUS_NOT_FOUND = 404;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test POST /public/webhooks.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks01() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {
      WebhooksApi api = new WebhooksApi(client);
      AddWebhookResponse addWebhook = api.addWebhook(new AddWebhookRequest().name("paypal"), null);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/public/webhooks/" + id;

      Optional<HttpHeaders> o = Optional.of(new HttpHeaders().add("Content-Type", "text/plain"));
      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJdk11();
      HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      Map<String, Object> map = GsonUtil.getInstance().fromJson(response.body(), Map.class);
      waitForDocument(client, null, map.get("documentId").toString());

      DocumentsApi docApi = new DocumentsApi(client);
      GetDocumentResponse document =
          docApi.getDocument(map.get("documentId").toString(), null, null);
      assertNotNull(document);

      GetWebhooksResponse webhooks = api.getWebhooks(null, null, null);
      List<GetWebhookResponse> list = notNull(webhooks.getWebhooks());
      assertFalse(list.isEmpty());
      assertEquals(DEFAULT_SITE_ID, list.get(0).getSiteId());
      assertEquals("paypal", list.get(0).getName());
      assertNotNull(list.get(0).getUrl());
      assertNotNull(list.get(0).getInsertedDate());
      assertNotNull(list.get(0).getWebhookId());
      assertEquals("testadminuser@formkiq.com", list.get(0).getUserId());

      api.deleteWebhook(id, null);
    }
  }

  private String getRootHttpUrl() {
    return getCognito().getRootJwtUrl();
  }

  /**
   * Test POST /public/webhooks missing endpoint UUID.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks02() throws Exception {
    // given
    String urlpath = getRootHttpUrl() + "/public/webhooks";

    Optional<HttpHeaders> o = Optional.of(new HttpHeaders().add("Content-Type", "text/plain"));

    String content = "{\"name\":\"John Smith\"}";

    // when
    HttpService hs = new HttpServiceJdk11();
    HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

    // then
    assertEquals(STATUS_NOT_FOUND, response.statusCode());
    assertEquals("{\"message\":\"Not Found\"}", response.body());
  }

  /**
   * Test POST /public/webhook/invalid endpoint UUID.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks03() throws Exception {
    // given
    String urlpath = getRootHttpUrl() + "/public/webhooks/asdffgdfg";

    Optional<HttpHeaders> o = Optional.of(new HttpHeaders().add("Content-Type", "text/plain"));

    String content = "{\"name\":\"John Smith\"}";

    // when
    HttpService hs = new HttpServiceJdk11();
    HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

    // then
    assertEquals(STATUS_BAD, response.statusCode());
    assertEquals("{\"message\":\"invalid webhook url\"}", response.body());
  }

  /**
   * Test POST /public/webhooks with private webhook.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPublicWebhooks04() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      WebhooksApi api = new WebhooksApi(client);
      AddWebhookResponse addWebhook = api.addWebhook(
          new com.formkiq.client.model.AddWebhookRequest().name("paypal").enabled("private"), null);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/public/webhooks/" + id;

      Optional<HttpHeaders> o = Optional.of(new HttpHeaders().add("Content-Type", "text/plain"));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJdk11();
      HttpResponse<String> response = hs.post(urlpath, o, Optional.empty(), content);

      // then
      assertEquals(STATUS_UNAUTHORIZED, response.statusCode());
    }
  }
}
