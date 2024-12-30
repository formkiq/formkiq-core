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
import static com.formkiq.stacks.dynamodb.config.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.config.ConfigService.MAX_WEBHOOKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.ConfigServiceDynamoDb;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
   */
  @Test
  public void testConfig01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setDocumentTimeToLive(ID.uuid());
      siteConfig.setMaxWebhooks(ID.uuid());
      siteConfig.setMaxDocuments(ID.uuid());
      this.service.save(siteId, siteConfig);

      // when
      Map<String, Object> config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.get("PK"));
      assertEquals(Objects.requireNonNullElse(siteId, DEFAULT_SITE_ID), config.get("SK"));

      assertEquals(siteConfig.getDocumentTimeToLive(), config.get(DOCUMENT_TIME_TO_LIVE));
      assertEquals(siteConfig.getMaxWebhooks(), config.get(MAX_WEBHOOKS));
      assertEquals(siteConfig.getMaxDocuments(), config.get(MAX_DOCUMENTS));
    }
  }

  /**
   * Test Finding missing Config.
   *
   */
  @Test
  public void testConfig02() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      // when
      Map<String, Object> config = this.service.get(siteId);

      // then
      assertEquals(0, config.size());
    }
  }

  /**
   * Test Finding Config.
   *
   */
  @Test
  public void testConfig03() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      this.service.save(siteId, new SiteConfiguration());

      // when
      Map<String, Object> config = this.service.get(siteId);

      // then
      final int count = 0;
      assertEquals(count, config.keySet().size());

      assertNull(config.get(DOCUMENT_TIME_TO_LIVE));
      assertNull(config.get(MAX_WEBHOOKS));
      assertNull(config.get(MAX_DOCUMENTS));
    }
  }

  /**
   * Test Finding Default Config.
   *
   */
  @Test
  public void testConfig04() {
    // given
    SiteConfiguration siteConfig = new SiteConfiguration();
    siteConfig.setDocumentTimeToLive(ID.uuid());
    siteConfig.setMaxWebhooks(ID.uuid());
    siteConfig.setMaxDocuments(ID.uuid());

    // DynamicObject obj = new DynamicObject(map);
    this.service.save(null, siteConfig);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID)) {

      // when
      Map<String, Object> config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.get("PK"));
      assertEquals(DEFAULT_SITE_ID, config.get("SK"));

      assertEquals(siteConfig.getDocumentTimeToLive(), config.get(DOCUMENT_TIME_TO_LIVE));
      assertEquals(siteConfig.getMaxWebhooks(), config.get(MAX_WEBHOOKS));
      assertEquals(siteConfig.getMaxDocuments(), config.get(MAX_DOCUMENTS));
    }

    assertEquals(0, this.service.get(ID.uuid()).size());
  }

  /**
   * Test Delete Config.
   *
   */
  @Test
  public void testDelete01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      SiteConfiguration siteConfig = new SiteConfiguration();
      siteConfig.setDocumentTimeToLive(ID.uuid());
      siteConfig.setMaxWebhooks(ID.uuid());
      siteConfig.setMaxDocuments(ID.uuid());

      this.service.save(siteId, siteConfig);

      // when
      Map<String, Object> config = this.service.get(siteId);

      // then
      final int count = 5;
      assertEquals(count, config.keySet().size());

      assertEquals("configs#", config.get("PK"));

      if (isDefaultSiteId(siteId)) {
        assertEquals(DEFAULT_SITE_ID, config.get("SK"));
      } else {
        assertEquals(siteId, config.get("SK"));
      }

      assertEquals(siteConfig.getDocumentTimeToLive(), config.get(DOCUMENT_TIME_TO_LIVE));
      assertEquals(siteConfig.getMaxWebhooks(), config.get(MAX_WEBHOOKS));
      assertEquals(siteConfig.getMaxDocuments(), config.get(MAX_DOCUMENTS));

      this.service.delete(siteId);
      assertEquals(0, this.service.get(siteId).size());
    }
  }

  @Test
  void incrementKey01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      assertEquals(-1, this.service.getIncrement(siteId, "maxdocs"));

      // when
      assertEquals(1, this.service.increment(siteId, "maxdocs"));

      // then
      assertEquals(1, this.service.getIncrement(siteId, "maxdocs"));

      // when
      assertEquals(2, this.service.increment(siteId, "maxdocs"));

      // then
      assertEquals(2, this.service.getIncrement(siteId, "maxdocs"));
    }
  }
}
