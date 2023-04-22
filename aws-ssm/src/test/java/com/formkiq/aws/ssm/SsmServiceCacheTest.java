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
package com.formkiq.aws.ssm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
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

  /** LocalStack {@link DockerImageName}. */
  private static DockerImageName localStackImage =
      DockerImageName.parse("localstack/localstack:0.12.2");
  /** {@link LocalStackContainer}. */
  private static LocalStackContainer localstack =
      new LocalStackContainer(localStackImage).withServices(Service.SSM);

  /** {@link SsmServiceCache}. */
  private static SsmServiceCache cache;

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

    localstack.start();

    SsmConnectionBuilder connection =
        new SsmConnectionBuilder(false).setCredentials(cred).setRegion(Region.US_EAST_1)
            .setEndpointOverride(new URI(localstack.getEndpointOverride(Service.SSM).toString()));

    cache = new SsmServiceCache(connection, 1, TimeUnit.SECONDS);
  }

  /**
   * AfterClass.
   */
  @AfterClass
  public static void afterClass() {
    localstack.stop();
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
