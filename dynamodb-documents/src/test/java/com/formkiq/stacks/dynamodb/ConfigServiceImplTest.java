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

import static com.formkiq.stacks.dynamodb.ConfigService.DOCUMENT_TIME_TO_LIVE;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
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
 * Unit Tests for {@link ConfigServiceImpl}. 
 */
public class ConfigServiceImplTest {
  
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

  /** {@link ConfigService}. */
  private ConfigService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {

    this.service = new ConfigServiceImpl(adb, "Documents");

    dbhelper.truncateDocumentsTable();
    dbhelper.truncateDocumentDates();
    dbhelper.truncateWebhooks();
    dbhelper.truncateConfig();

    assertEquals(0, dbhelper.getDocumentItemCount());
  }
  
  /**
   * Test Finding Config.
   */
  @Test
  public void testConfig01() {
    //given
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
   */
  @Test
  public void testConfig02() {
    //given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString(), DEFAULT_SITE_ID)) {

      // when
      DynamicObject config = this.service.get(siteId);
      
      // then
      assertEquals(0, config.size());
    }
  }
  
  /**
   * Test Finding Config.
   */
  @Test
  public void testConfig03() {
    //given
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
   */
  @Test
  public void testConfig04() {
    //given
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
   */
  @Test
  public void testDelete01() {
    //given
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
