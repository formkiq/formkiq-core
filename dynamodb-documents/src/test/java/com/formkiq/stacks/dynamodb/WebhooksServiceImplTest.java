/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.formkiq.stacks.common.objects.DynamicObject;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** 
 * Unit Tests for {@link DocumentServiceImpl}. 
 */
public class WebhooksServiceImplTest {

  /** MilliSeconds per Second. */
  private static final int MILLISECONDS = 1000;
  
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder adb;

  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbhelper;

  /**
   * Generate Test Data.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));
    adb = new DynamoDbConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:8000");

    dbhelper = new DynamoDbHelper(adb);

    if (!dbhelper.isDocumentsTableExists()) {
      dbhelper.createDocumentsTable();
    }
  }

  /** Document Table. */
  private WebhooksService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {

    this.service = dbhelper.getWebhookService();

    dbhelper.truncateDocumentsTable();
    dbhelper.truncateDocumentDates();
    dbhelper.truncateWebhooks();

    assertEquals(0, dbhelper.getDocumentItemCount());
  }

  /**
   * Test Finding webhooks.
   */
  @Test
  public void testFindWebhooks01() {
    //given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      this.service.saveWebhook(siteId, "test", "joe", null);
      this.service.saveWebhook(siteId, "abc", "joe", null);

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
  
  /**
   * Test Finding webhooks, empty list.
   */
  @Test
  public void testFindWebhooks02() {
    //given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      List<DynamicObject> list = this.service.findWebhooks(siteId);
      
      // then
      assertEquals(0, list.size());      
    }
  }
  
  /**
   * Test Finding webhook.
   */
  @Test
  public void testFindWebhook01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      String id = this.service.saveWebhook(siteId, "test", "joe", Date.from(now.toInstant()));

      // when
      DynamicObject o = this.service.findWebhook(siteId, id);

      // then
      assertNotNull(o.getString("documentId"));
      assertNotNull(o.getString("path"));
      assertNotNull(o.getString("userId"));
      assertNotNull(o.getString("inserteddate"));
      assertNotNull(o.getString("TimeToLive"));
      
      LocalDateTime time =
          LocalDateTime.ofEpochSecond(Long.parseLong(o.getString("TimeToLive")), 0, ZoneOffset.UTC);
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
  
  /**
   * Test Finding missing webhook.
   */
  @Test
  public void testFindWebhook02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String id = UUID.randomUUID().toString();

      // when
      DynamicObject o = this.service.findWebhook(siteId, id);

      // then
      assertNull(o);
    }
  }
  
  /**
   * Test Updating webhooks.
   */
  @Test
  public void testUpdateWebhooks01() {
    //given
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    Date ttl = Date.from(now.toInstant());
    
    ZonedDateTime tomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
    Date tomorrowttl = Date.from(tomorrow.toInstant());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      
      String webhookId = this.service.saveWebhook(siteId, "test", "joe", ttl);
      DynamicObject obj = new DynamicObject(Map.of("name", "test2", "TimeToLive", tomorrowttl));

      // when
      this.service.updateWebhook(siteId, webhookId, obj);
      
      // then
      DynamicObject result = this.service.findWebhook(siteId, webhookId);
      assertEquals("test2", result.getString("name"));
      assertNotNull(result.getString("documentId"));
      assertNotNull(result.getString("path"));
      assertNotNull(result.getString("userId"));
      assertNotNull(result.getString("inserteddate"));
      assertEquals(String.valueOf(tomorrowttl.getTime() / MILLISECONDS),
          result.getString("TimeToLive"));
    }
  }
}
