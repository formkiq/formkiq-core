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
package com.formkiq.stacks.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Unit Tests for {@link DocumentServiceImpl}.
 */
@ExtendWith(DynamoDbExtension.class)
public class WebhooksServiceImplTest {

  /** MilliSeconds per Second. */
  private static final int MILLISECONDS = 1000;

  /** Document Table. */
  private WebhooksService service;
  /** {@link DynamoDbConnectionBuilder}. */
  private DynamoDbConnectionBuilder db;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.db = DynamoDbTestServices.getDynamoDbConnection();
    this.service = new WebhooksServiceImpl(this.db, "Documents");
  }

  /**
   * Add Webhook Tag.
   */
  @Test
  public void testAddTags01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      String hook0 = this.service.saveWebhook(null, "test", "joe", null, "true");
      String hook1 = this.service.saveWebhook(null, "test2", "joe2", null, "true");
      DocumentTag tag0 = new DocumentTag(hook0, "category", "person", new Date(), "joe");
      DocumentTag tag1 = new DocumentTag(hook0, "type", "person2", new Date(), "joe");

      // when
      this.service.addTags(null, hook0, Arrays.asList(tag0, tag1), null);

      // then
      assertNull(this.service.findTag(null, hook1, "category"));
      DynamicObject tag = this.service.findTag(null, hook0, "category");
      assertEquals("category", tag.getString("tagKey"));
      assertEquals("person", tag.getString("tagValue"));

      assertEquals(2, this.service.findTags(null, hook0, null).getResults().size());
      assertEquals(0, this.service.findTags(null, hook1, null).getResults().size());
    }
  }

  /**
   * Test Delete Webhook and Tags.
   */
  @Test
  public void testDeleteWebhooksAndTags01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      final int numberOfTags = 100;
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        String id0 = this.service.saveWebhook(siteId, "test", "joe", null, "true");

        for (int i = 0; i < numberOfTags; i++) {
          DocumentTag tag =
              new DocumentTag(id0, UUID.randomUUID().toString(), null, new Date(), "joe");
          this.service.addTags(siteId, id0, Arrays.asList(tag), null);
        }

        // when
        this.service.deleteWebhook(siteId, id0);

        // then
        PaginationResults<DynamicObject> tags = this.service.findTags(siteId, id0, null);
        assertEquals(0, tags.getResults().size());
      }
    }
  }

  /**
   * Test Finding webhook.
   */
  @Test
  public void testFindWebhook01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String id =
            this.service.saveWebhook(siteId, "test", "joe", Date.from(now.toInstant()), "true");

        // when
        DynamicObject o = this.service.findWebhook(siteId, id);

        // then
        assertNotNull(o.getString("documentId"));
        assertNotNull(o.getString("path"));
        assertNotNull(o.getString("userId"));
        assertNotNull(o.getString("inserteddate"));
        assertNotNull(o.getString("TimeToLive"));

        LocalDateTime time = LocalDateTime.ofEpochSecond(Long.parseLong(o.getString("TimeToLive")),
            0, ZoneOffset.UTC);
        assertEquals(now.getYear(), time.getYear());
        assertEquals(now.getMonth(), time.getMonth());
        assertEquals(now.getDayOfMonth(), time.getDayOfMonth());
        assertEquals(now.getHour(), time.getHour());
        assertEquals(now.getMinute(), time.getMinute());
        assertEquals(now.getSecond(), time.getSecond());

        // when
        this.service.deleteWebhook(siteId, id);

        // then
        assertEquals(0, this.service.findWebhooks(siteId).size());
      }
    }
  }

  /**
   * Test Finding missing webhook.
   */
  @Test
  public void testFindWebhook02() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        String id = UUID.randomUUID().toString();

        // when
        DynamicObject o = this.service.findWebhook(siteId, id);

        // then
        assertNull(o);
      }
    }
  }

  /**
   * Test Finding webhooks.
   */
  @Test
  public void testFindWebhooks01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        this.service.saveWebhook(siteId, "test", "joe", null, "true");
        this.service.saveWebhook(siteId, "abc", "joe", null, "true");

        // when
        List<DynamicObject> list = this.service.findWebhooks(siteId);

        // then
        assertEquals(2, list.size());

        list.forEach(l -> {
          assertNotNull(l.getString("documentId"));
          assertNotNull(l.getString("path"));
          assertNotNull(l.getString("userId"));
          assertNotNull(l.getString("inserteddate"));
          assertNull(l.getString("TimeToLive"));
        });

        // when
        this.service.deleteWebhook(siteId, list.get(0).getString("documentId"));

        // then
        assertEquals(1, this.service.findWebhooks(siteId).size());
      }
    }
  }

  /**
   * Test Finding webhooks, empty list.
   */
  @Test
  public void testFindWebhooks02() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        // when
        List<DynamicObject> list = this.service.findWebhooks(siteId);

        // then
        assertEquals(0, list.size());
      }
    }
  }

  /**
   * Test Updating webhooks Time To Live.
   */
  @Test
  public void testUpdateTimeToLive01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      Date ttl = Date.from(now.toInstant());

      ZonedDateTime tomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
      Date tomorrowttl = Date.from(tomorrow.toInstant());

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        String webhookId = this.service.saveWebhook(siteId, "test", "joe", ttl, "true");
        DocumentTag tag0 = new DocumentTag(webhookId, "category", "person", new Date(), "joe");

        this.service.addTags(siteId, webhookId, Arrays.asList(tag0), null);

        DynamicObject tag = this.service.findTag(siteId, webhookId, "category");
        assertNull(tag.getString("TimeToLive"));

        // when
        this.service.updateTimeToLive(siteId, webhookId, tomorrowttl);

        // then
        DynamicObject result = this.service.findWebhook(siteId, webhookId);

        assertEquals("test", result.getString("path"));
        assertNotNull(result.getString("documentId"));
        assertNotNull(result.getString("path"));
        assertNotNull(result.getString("userId"));
        assertNotNull(result.getString("inserteddate"));
        assertEquals(String.valueOf(tomorrowttl.getTime() / MILLISECONDS),
            result.getString("TimeToLive"));

        tag = this.service.findTag(siteId, webhookId, "category");
        assertNotNull(tag.getString("TimeToLive"));
        assertEquals(String.valueOf(tomorrowttl.getTime() / MILLISECONDS),
            tag.getString("TimeToLive"));
      }
    }
  }

  /**
   * Test Updating webhooks.
   */
  @Test
  public void testUpdateWebhooks01() {
    // given
    try (DynamoDbClient client = this.db.build()) {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      Date ttl = Date.from(now.toInstant());

      ZonedDateTime tomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
      Date tomorrowttl = Date.from(tomorrow.toInstant());

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        String webhookId = this.service.saveWebhook(siteId, "test", "joe", ttl, "true");
        DynamicObject obj = new DynamicObject(Map.of("name", "test2", "TimeToLive", tomorrowttl));

        // when
        this.service.updateWebhook(siteId, webhookId, obj);

        // then
        DynamicObject result = this.service.findWebhook(siteId, webhookId);
        assertEquals("test2", result.getString("path"));
        assertNotNull(result.getString("documentId"));
        assertNotNull(result.getString("path"));
        assertNotNull(result.getString("userId"));
        assertNotNull(result.getString("inserteddate"));
        assertEquals(String.valueOf(tomorrowttl.getTime() / MILLISECONDS),
            result.getString("TimeToLive"));
      }
    }
  }
}
