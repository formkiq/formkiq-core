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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Map;

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

  @Test
  void incrementsKey01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      assertEquals(0, this.service.getIncrements(siteId).size());

      this.service.increment(siteId, ConfigService.DOCUMENT_COUNT);
      this.service.increment(siteId, "OcrCount");
      this.service.increment(siteId, "OcrCount");

      // when
      Map<String, Long> increments = this.service.getIncrements(siteId);

      // then
      assertEquals(2, increments.size());

      assertEquals(1, increments.get(ConfigService.DOCUMENT_COUNT));
      assertEquals(2, increments.get("OcrCount"));
    }
  }

  /**
   * Test Finding Config.
   *
   */
  @Test
  public void testConfig01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      SiteConfiguration siteConfig = SiteConfiguration.builder().documentTimeToLive(ID.uuid())
          .maxWebhooks(ID.uuid()).maxDocuments(ID.uuid()).build(siteId);
      this.service.save(siteId, siteConfig);

      // when
      SiteConfiguration config = this.service.get(siteId);

      // then
      assertEquals(siteConfig.documentTimeToLive(), config.documentTimeToLive());
      assertEquals(siteConfig.maxWebhooks(), config.maxWebhooks());
      assertEquals(siteConfig.maxDocuments(), config.maxDocuments());
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
      SiteConfiguration config = this.service.get(siteId);

      // then
      assertNull(config.documentTimeToLive());
      assertNull(config.maxWebhooks());
      assertNull(config.maxDocuments());
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

      SiteConfiguration siteConfig = SiteConfiguration.builder().build(siteId);
      this.service.save(siteId, siteConfig);

      // when
      SiteConfiguration config = this.service.get(siteId);

      // then
      assertNull(config.documentTimeToLive());
      assertNull(config.maxWebhooks());
      assertNull(config.maxDocuments());
    }
  }

  /**
   * Test Finding Default Config.
   *
   */
  @Test
  public void testConfig04() {
    // given
    SiteConfiguration siteConfig = SiteConfiguration.builder().documentTimeToLive(ID.uuid())
        .maxWebhooks(ID.uuid()).maxDocuments(ID.uuid()).build(null);

    // DynamicObject obj = new DynamicObject(map);
    this.service.save(null, siteConfig);

    for (String siteId : Arrays.asList(null, DEFAULT_SITE_ID)) {

      // when
      SiteConfiguration config = this.service.get(siteId);

      // then
      assertEquals(siteConfig.documentTimeToLive(), config.documentTimeToLive());
      assertEquals(siteConfig.maxWebhooks(), config.maxWebhooks());
      assertEquals(siteConfig.maxDocuments(), config.maxDocuments());
    }

    assertNull(this.service.get(ID.uuid()).maxWebhooks());
  }

  /**
   * Test Delete Config.
   *
   */
  @Test
  public void testDelete01() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid(), DEFAULT_SITE_ID)) {

      SiteConfiguration siteConfig = SiteConfiguration.builder().documentTimeToLive(ID.uuid())
          .maxWebhooks(ID.uuid()).maxDocuments(ID.uuid()).build(siteId);
      this.service.save(siteId, siteConfig);

      // when
      SiteConfiguration config = this.service.get(siteId);

      // then
      assertEquals(siteConfig.documentTimeToLive(), config.documentTimeToLive());
      assertEquals(siteConfig.maxWebhooks(), config.maxWebhooks());
      assertEquals(siteConfig.maxDocuments(), config.maxDocuments());

      this.service.delete(siteId);
      assertNull(this.service.get(siteId).maxDocuments());
      assertNull(this.service.get(siteId).maxWebhooks());
    }
  }
}
