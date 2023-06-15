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
package com.formkiq.aws.services.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/** Unit Tests for {@link DynamoDbCacheService}. */
@ExtendWith(DynamoDbExtension.class)
public class DynamoDbCacheServiceTest {

  /** Document Table. */
  private CacheService service;
  /** Cache Table. */
  private static String cacheTable = "Cache";

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.service =
        new DynamoDbCacheService(DynamoDbTestServices.getDynamoDbConnection(), cacheTable);
  }

  /**
   * Test Write to Cache.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testWrite01() throws Exception {
    // given
    final Date before = Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(1).toInstant());
    final Date after = Date.from(ZonedDateTime.now(ZoneOffset.UTC).plusDays(1).toInstant());

    String key = "testkey";
    String value = UUID.randomUUID().toString();

    // when
    this.service.write(key, value, 1);

    // then
    assertEquals(value, this.service.read(key));
    Date date = this.service.getExpiryDate(key);

    assertTrue(before.before(date));
    assertTrue(after.after(date));
  }
}
