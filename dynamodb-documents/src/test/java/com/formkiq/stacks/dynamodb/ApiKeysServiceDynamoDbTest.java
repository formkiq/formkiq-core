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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/**
 * Unit Tests for {@link ApiKeysServiceDynamoDb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class ApiKeysServiceDynamoDbTest {

  /** Limit. */
  private static final int LIMIT = 10;
  /** {@link ApiKeysService}. */
  private ApiKeysService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.service =
        new ApiKeysServiceDynamoDb(DynamoDbTestServices.getDynamoDbConnection(), "Documents");
  }

  /**
   * Test Create ApiKey.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCreateApiKey01() throws Exception {
    // given
    String userId = "joe";
    Collection<ApiKeyPermission> permissions = Arrays.asList(ApiKeyPermission.READ);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      String apiKey0 = this.service.createApiKey(siteId, "test1", permissions, userId);
      String apiKey1 = this.service.createApiKey(siteId, "test2", permissions, userId);

      // then
      assertNotEquals(apiKey0, apiKey1);

      PaginationResults<ApiKey> list = this.service.list(siteId, null, LIMIT);
      List<String> keys =
          list.getResults().stream().map(i -> i.apiKey()).collect(Collectors.toList());

      assertEquals(2, keys.size());
      assertTrue(keys.contains(this.service.mask(apiKey0)));
      assertTrue(keys.contains(this.service.mask(apiKey1)));

      assertNotNull(this.service.get(apiKey0, false));
      assertNotNull(this.service.get(apiKey1, false));
      assertNull(this.service.get(UUID.randomUUID().toString(), false));

      // when
      this.service.deleteApiKey(siteId, apiKey0);
      this.service.deleteApiKey(siteId, apiKey1);

      // then
      assertNull(this.service.get(apiKey0, false));
      assertNull(this.service.get(apiKey1, false));
    }
  }

  /**
   * Test Create ApiKey.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCreateApiKey02() throws Exception {
    // given
    final int start = 100;
    final int limit = 200;
    final int max = 300;
    String userId = "joe";
    Collection<ApiKeyPermission> permissions = Arrays.asList(ApiKeyPermission.READ);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (int i = start; i < start + max; i++) {
        // when
        this.service.createApiKey(siteId, "test_" + i, permissions, userId);
      }

      // then
      PaginationResults<ApiKey> list = this.service.list(siteId, null, limit);
      assertEquals(limit, list.getResults().size());
      assertEquals("test_100", list.getResults().get(0).name());
      assertEquals("test_101", list.getResults().get(1).name());
      assertEquals("test_102", list.getResults().get(2).name());

      list = this.service.list(siteId, list.getToken(), limit);
      assertEquals(max - limit, list.getResults().size());
      assertEquals("test_300", list.getResults().get(0).name());
      assertEquals("test_301", list.getResults().get(1).name());
      assertEquals("test_302", list.getResults().get(2).name());
    }
  }
}
