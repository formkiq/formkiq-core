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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.WebhookTag;
import com.formkiq.stacks.client.models.WebhookTags;
import com.formkiq.stacks.client.requests.AddWebhookRequest;
import com.formkiq.stacks.client.requests.AddWebhookTagRequest;
import com.formkiq.stacks.client.requests.GetWebhookTagsRequest;
import com.formkiq.stacks.client.requests.OptionsWebhookTagsRequest;

/**
 * Process Urls.
 * <p>
 * POST /webhooks/{webhookId}/tags tests
 * </p>
 *
 */
public class WebhookTagsRequestTest extends AbstractApiTest {

  /** Http Status OK. */
  private static final int STATUS_NO_CONTENT = 204;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * /webhooks/{webhookId}/tags Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      OptionsWebhookTagsRequest req =
          new OptionsWebhookTagsRequest().webhookId(UUID.randomUUID().toString());
      assertEquals(STATUS_NO_CONTENT, client.optionsWebhookTags(req).statusCode());
    }
  }

  /**
   * Test POST /webhooks/{webhookId}/tags.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPublicWebhooks01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients(null)) {
      // given
      String id = client.addWebhook(new AddWebhookRequest().name("paypal")).id();

      AddWebhookTagRequest req =
          new AddWebhookTagRequest().tagKey("category").tagValue("person").webhookId(id);

      // when
      boolean result = client.addWebhookTag(req);

      // then
      assertTrue(result);

      // given
      GetWebhookTagsRequest get = new GetWebhookTagsRequest().webhookId(id);

      // when
      WebhookTags tags = client.getWebhookTags(get);

      // then
      assertEquals(1, tags.tags().size());
      WebhookTag tag = tags.tags().get(0);

      assertEquals("category", tag.key());
      assertNotNull(tag.insertedDate());
      assertEquals("USERDEFINED", tag.type());
      assertNotNull(tag.userId());
      assertEquals("person", tag.value());
      assertEquals(id, tag.webhookId());
    }
  }
}
