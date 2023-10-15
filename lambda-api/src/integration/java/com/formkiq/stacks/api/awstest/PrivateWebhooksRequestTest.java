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

import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
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
import com.formkiq.stacks.client.HttpService;
import com.formkiq.stacks.client.HttpServiceJava;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import software.amazon.awssdk.core.sync.RequestBody;

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

  /**
   * Test POST /private/webhooks.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPublicWebhooks01() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(siteId)) {
      WebhooksApi api = new WebhooksApi(client);
      AddWebhookResponse addWebhook =
          api.addWebhook(new AddWebhookRequest().name("paypal").enabled("private"), siteId);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/private/webhooks/" + id;

      Map<String, List<String>> headers = Map.of("Authorization",
          Arrays.asList(getAdminToken().idToken()), "Content-Type", Arrays.asList("text/plain"));

      Optional<HttpHeaders> o =
          Optional.of(HttpHeaders.of(headers, new BiPredicate<String, String>() {
            @Override
            public boolean test(final String t, final String u) {
              return true;
            }
          }));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJava();
      HttpResponse<String> response = hs.post(urlpath, o, RequestBody.fromString(content));

      // then
      assertEquals(STATUS_OK, response.statusCode());
      Map<String, Object> map = GsonUtil.getInstance().fromJson(response.body(), Map.class);

      GetDocumentResponse document =
          waitForDocument(client, siteId, map.get("documentId").toString());
      assertNotNull(document);

      GetWebhooksResponse webhooks = api.getWebhooks(siteId);
      List<GetWebhookResponse> list = webhooks.getWebhooks();
      assertFalse(list.isEmpty());
      assertEquals("default", list.get(0).getSiteId());
      assertEquals("paypal", list.get(0).getName());
      assertNotNull(list.get(0).getUrl());
      assertNotNull(list.get(0).getInsertedDate());
      assertNotNull(list.get(0).getWebhookId());
      assertNotNull("testadminuser@formkiq.com", list.get(0).getUrl());

      api.deleteWebhook(id, siteId);
    }
  }

  private String getRootHttpUrl() {
    return getCognito().getRootJwtUrl();
  }

  /**
   * Test POST /private/webhooks missing Authorization token.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPublicWebhooks02() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(siteId)) {
      WebhooksApi api = new WebhooksApi(client);

      AddWebhookResponse addWebhook =
          api.addWebhook(new AddWebhookRequest().name("paypal").enabled("private"), siteId);
      String id = addWebhook.getWebhookId();
      String urlpath = getRootHttpUrl() + "/private/webhooks/" + id;

      Map<String, List<String>> headers = Map.of("Content-Type", Arrays.asList("text/plain"));

      Optional<HttpHeaders> o =
          Optional.of(HttpHeaders.of(headers, new BiPredicate<String, String>() {
            @Override
            public boolean test(final String t, final String u) {
              return true;
            }
          }));

      String content = "{\"name\":\"John Smith\"}";

      // when
      HttpService hs = new HttpServiceJava();
      HttpResponse<String> response = hs.post(urlpath, o, RequestBody.fromString(content));

      // then
      assertEquals(STATUS_UNAUTHORIZED, response.statusCode());
    }
  }
}
