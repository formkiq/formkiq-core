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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;

/**
 * Unit Tests for {@link ApiKeysServiceDynamoDb}.
 */
@ExtendWith(DynamoDbExtension.class)
public class ApiKeysServiceDynamoDbTest {

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
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      String apiKey0 = this.service.createApiKey(siteId, "test1");
      String apiKey1 = this.service.createApiKey(siteId, "test2");

      // then
      assertNotEquals(apiKey0, apiKey1);

      List<DynamicObject> list = this.service.list(siteId);
      List<String> keys =
          list.stream().map(i -> i.getString("apiKey")).collect(Collectors.toList());

      assertEquals(2, keys.size());
      assertTrue(keys.contains(this.service.mask(apiKey0)));
      assertTrue(keys.contains(this.service.mask(apiKey1)));

      assertTrue(this.service.isApiKeyValid(siteId, apiKey0));
      assertTrue(this.service.isApiKeyValid(siteId, apiKey1));
      assertFalse(this.service.isApiKeyValid(siteId, UUID.randomUUID().toString()));

      // when
      this.service.deleteApiKey(siteId, this.service.mask(apiKey0));
      this.service.deleteApiKey(siteId, this.service.mask(apiKey1));

      // then
      assertFalse(this.service.isApiKeyValid(siteId, apiKey0));
      assertFalse(this.service.isApiKeyValid(siteId, apiKey1));
    }
  }
}
