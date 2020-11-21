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
package com.formkiq.aws.ssm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

/**
 * 
 * Unit Tests for {@link SsmServiceCache}.
 *
 */
public class SsmServiceCacheTest {

  /** {@link SsmServiceCache}. */
  private static SsmServiceCache cache;
  /** {@link SsmConnectionBuilder}. */
  private static SsmConnectionBuilder ssmConnection;

  /**
   * Before Class.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException, InterruptedException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    ssmConnection = new SsmConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:4566");

    cache = new SsmServiceCache(ssmConnection, 1, TimeUnit.SECONDS);
  }

  /**
   * Test Get Parameter with Cache.
   */
  @Test
  public void testGetParameterValue01() {
    // given
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    cache.putParameter(key, value);

    // when
    String result = cache.getParameterValue(key);

    // then
    assertEquals(value, result);
    assertTrue(cache.isCached(key));
    assertFalse(cache.isExpired(key));
  }

  /**
   * Test Put Parameter in Cache.
   */
  @Test
  public void testPutParameter01() {
    // given
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    cache.putParameter(key, value);

    // when
    String result = cache.getParameterValue(key);

    // then
    assertEquals(value, result);
    assertTrue(cache.isCached(key));
    assertFalse(cache.isExpired(key));
  }

  /**
   * Test Put Parameter in Expired Cache.
   * 
   * @throws InterruptedException InterruptedException
   */
  @Test
  public void testPutParameter02() throws InterruptedException {
    // given
    final long sleep = TimeUnit.SECONDS.toMillis(2);
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    // when
    cache.putParameter(key, value);

    // then
    assertTrue(cache.isCached(key));
    assertFalse(cache.isExpired(key));

    // when
    Thread.sleep(sleep);

    // then
    assertTrue(cache.isCached(key));
    assertTrue(cache.isExpired(key));
  }

  /**
   * Remove Parameter.
   */
  @Test
  public void testRemoveParameter01() {
    // given
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    cache.putParameter(key, value);

    assertTrue(cache.isCached(key));
    assertFalse(cache.isExpired(key));

    // when
    cache.removeParameter(key);

    // then
    assertFalse(cache.isCached(key));
    assertTrue(cache.isExpired(key));

    // when
    try {
      cache.getParameterValue(key);
    } catch (ParameterNotFoundException e) {
      assertTrue(true);
    }
  }
}
