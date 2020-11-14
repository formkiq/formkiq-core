/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link DynamoDbCacheService}. */
public class DynamoDbCacheServiceTest {

  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder adb;
  /** Document Table. */
  private CacheService service;
  /** Cache Table. */
  private static String cacheTable = "Cache";

  /**
   * Before Class.
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

    DynamoDbHelper dbhelper = new DynamoDbHelper(adb);

    if (!dbhelper.isDocumentsTableExists()) {
      dbhelper.createDocumentsTable();
      dbhelper.createCacheTable();
    }
  }

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {
    this.service = new DynamoDbCacheService(adb, cacheTable);
  }

  /**
   * Test Write to Cache.
   */
  @Test
  public void testWrite01() {
    // given
    final Date before = Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(1).toInstant());
    final Date after = Date.from(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(1).toInstant());

    String key = "testkey";
    String value = UUID.randomUUID().toString();

    // when
    this.service.write(key, value);

    // then
    assertEquals(value, this.service.read(key));
    Date date = this.service.getExpiryDate(key);

    assertTrue(before.before(date));
    assertTrue(after.after(date));
  }
}
