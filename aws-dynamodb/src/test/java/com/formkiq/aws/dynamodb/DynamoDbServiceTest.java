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
package com.formkiq.aws.dynamodb;

import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for {@link DynamoDbService}. */
@ExtendWith(DynamoDbExtension.class)
public class DynamoDbServiceTest {

  /** Timeout. */
  private static final long TIMEOUT = 20;

  /** {@link DynamoDbService}. */
  private DynamoDbService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.service =
        new DynamoDbServiceImpl(DynamoDbTestServices.getDynamoDbConnection(), DOCUMENTS_TABLE);
  }

  /**
   * Test aquire lock / release lock.
   *
   */
  @Test
  @Timeout(TIMEOUT)
  public void testAcquireLock01() {
    // given
    final long timeout = 2000;
    final long lockExpiry = 10000;
    AttributeValue pk = AttributeValue.fromS("test");
    AttributeValue sk = AttributeValue.fromS("test1");

    // when
    boolean locked = this.service.acquireLock(pk, sk, timeout, lockExpiry);

    // then
    assertTrue(locked);
    assertTrue(this.service.releaseLock(pk, sk));

    // when
    locked = this.service.acquireLock(pk, sk, timeout, lockExpiry);

    // then
    assertTrue(locked);
    assertTrue(this.service.releaseLock(pk, sk));
  }

  /**
   * Test aquire lock when already locked.
   */
  @Test
  @Timeout(TIMEOUT)
  public void testAcquireLock02() {
    // given
    final long timeout = 2000;
    final long lockExpiry = 10000;
    AttributeValue pk = AttributeValue.fromS("test");
    AttributeValue sk = AttributeValue.fromS("test1");

    // when
    boolean lock1 = this.service.acquireLock(pk, sk, timeout, lockExpiry);
    boolean lock2 = this.service.acquireLock(pk, sk, timeout, lockExpiry);

    // then
    assertFalse(lock2);
    assertTrue(lock1);
  }

  /**
   * Test aquire lock after expiration.
   */
  @Test
  @Timeout(TIMEOUT)
  public void testAcquireLock03() {
    // given
    final long timeout = 5000;
    final long lockExpiry = 1000;
    AttributeValue pk = AttributeValue.fromS("test");
    AttributeValue sk = AttributeValue.fromS("test1");

    // when
    boolean lock1 = this.service.acquireLock(pk, sk, timeout, lockExpiry);
    boolean lock2 = this.service.acquireLock(pk, sk, timeout, lockExpiry);

    // then
    assertTrue(lock1);
    assertTrue(lock2);
  }
}
