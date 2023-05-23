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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static com.formkiq.stacks.dynamodb.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/**
 * Unit Tests for {@link ConfigServiceDynamoDb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class ConfigServiceDynamoDbTest {

  /** {@link ConfigService}. */
  private ConfigService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.service =
        new ConfigServiceDynamoDb(DynamoDbTestServices.getDynamoDbConnection(), "Documents");
  }

  /**
   * Test Finding Config.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testConfig01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      Map<String, Object> map = new HashMap<>();
      map.put(DOCUMENT_TIME_TO_LIVE, "" + UUID.randomUUID().toString());
      map.put(MAX_WEBHOOKS, "" + UUID.randomUUID().toString());
      map.put(MAX_DOCUMENTS, "" + UUID.randomUUID().toString());

      DynamicObject obj = new DynamicObject(map);
      this.service.save(siteId, obj);

      // when
      DynamicObject config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.getString("PK"));
      if (siteId != null) {
        assertEquals(siteId, config.getString("SK"));
      } else {
        assertEquals("default", config.getString("SK"));
      }

      assertEquals(map.get(DOCUMENT_TIME_TO_LIVE), config.getString(DOCUMENT_TIME_TO_LIVE));
      assertEquals(map.get(MAX_WEBHOOKS), config.getString(MAX_WEBHOOKS));
      assertEquals(map.get(MAX_DOCUMENTS), config.getString(MAX_DOCUMENTS));
    }
  }

  /**
   * Test Finding missing Config.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testConfig02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      // when
      DynamicObject config = this.service.get(siteId);

      // then
      assertEquals(0, config.size());
    }
  }

  /**
   * Test Finding Config.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testConfig03() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      DynamicObject obj = new DynamicObject(Map.of());
      this.service.save(siteId, obj);

      // when
      DynamicObject config = this.service.get(siteId);

      // then
      final int count = 2;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.getString("PK"));
      if (siteId != null) {
        assertEquals(siteId, config.getString("SK"));
      } else {
        assertEquals("default", config.getString("SK"));
      }

      assertNull(config.getString(DOCUMENT_TIME_TO_LIVE));
      assertNull(config.getString(MAX_WEBHOOKS));
      assertNull(config.getString(MAX_DOCUMENTS));
    }
  }

  /**
   * Test Finding Default Config.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testConfig04() throws Exception {
    // given
    Map<String, Object> map = new HashMap<>();
    map.put(DOCUMENT_TIME_TO_LIVE, "" + UUID.randomUUID().toString());
    map.put(MAX_WEBHOOKS, "" + UUID.randomUUID().toString());
    map.put(MAX_DOCUMENTS, "" + UUID.randomUUID().toString());

    DynamicObject obj = new DynamicObject(map);
    this.service.save(null, obj);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      // when
      DynamicObject config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.getString("PK"));
      assertEquals("default", config.getString("SK"));

      assertEquals(map.get(DOCUMENT_TIME_TO_LIVE), config.getString(DOCUMENT_TIME_TO_LIVE));
      assertEquals(map.get(MAX_WEBHOOKS), config.getString(MAX_WEBHOOKS));
      assertEquals(map.get(MAX_DOCUMENTS), config.getString(MAX_DOCUMENTS));
    }
  }

  /**
   * Test Delete Config.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testDelete01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      Map<String, Object> map = new HashMap<>();
      map.put(DOCUMENT_TIME_TO_LIVE, "" + UUID.randomUUID().toString());
      map.put(MAX_WEBHOOKS, "" + UUID.randomUUID().toString());
      map.put(MAX_DOCUMENTS, "" + UUID.randomUUID().toString());

      DynamicObject obj = new DynamicObject(map);
      this.service.save(siteId, obj);

      // when
      DynamicObject config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.getString("PK"));

      if (isDefaultSiteId(siteId)) {
        assertEquals("default", config.getString("SK"));
      } else {
        assertEquals(siteId, config.getString("SK"));
      }

      assertEquals(map.get(DOCUMENT_TIME_TO_LIVE), config.getString(DOCUMENT_TIME_TO_LIVE));
      assertEquals(map.get(MAX_WEBHOOKS), config.getString(MAX_WEBHOOKS));
      assertEquals(map.get(MAX_DOCUMENTS), config.getString(MAX_DOCUMENTS));

      this.service.delete(siteId);
      assertEquals(0, this.service.get(siteId).size());
    }
  }
}
