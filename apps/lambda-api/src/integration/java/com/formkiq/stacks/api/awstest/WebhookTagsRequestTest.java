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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.WebhooksApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddWebhookRequest;
import com.formkiq.client.model.AddWebhookResponse;
import com.formkiq.client.model.AddWebhookTagRequest;
import com.formkiq.client.model.GetWebhookTagsResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * Process Urls.
 * <p>
 * POST /webhooks/{webhookId}/tags tests
 * </p>
 *
 */
public class WebhookTagsRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test POST /webhooks/{webhookId}/tags.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testwebhookTags01() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(siteId)) {
      WebhooksApi api = new WebhooksApi(client);
      AddWebhookResponse addWebhook =
          api.addWebhook(new AddWebhookRequest().name("paypal"), siteId);
      String webhookId = addWebhook.getWebhookId();

      AddWebhookTagRequest req = new AddWebhookTagRequest().key("category").value("person");

      api.addWebhookTag(webhookId, req, siteId);

      // when
      GetWebhookTagsResponse response = api.getWebhookTags(webhookId, siteId);

      // then
      assertEquals(1, response.getTags().size());
      com.formkiq.client.model.WebhookTag tag = response.getTags().get(0);

      assertEquals("category", tag.getKey());
      assertNotNull(tag.getInsertedDate());
      assertEquals("USERDEFINED", tag.getType());
      assertNotNull(tag.getUserId());
      assertEquals("person", tag.getValue());
      assertEquals(webhookId, tag.getWebhookId());
    }
  }
}
