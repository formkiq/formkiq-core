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

import com.formkiq.aws.dynamodb.ApiPermissionRole;
import com.formkiq.aws.dynamodb.ApiPermissionRoleType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.base64.Pagination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
   */
  @Test
  public void testCreateApiKey01() {
    // given
    String userId = "joe";
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(List.of(ApiPermission.READ)).build();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      // when
      String apiKey0 = this.service.createApiKey(siteId, "test1", role, List.of());
      String apiKey1 = this.service.createApiKey(siteId, "test2", role, List.of());

      // then
      assertNotEquals(apiKey0, apiKey1);

      Pagination<ApiKey> list = this.service.list(siteId, null, LIMIT);
      List<String> keys = list.getResults().stream().map(ApiKey::apiKey).toList();

      assertEquals(2, keys.size());
      assertTrue(keys.contains(this.service.mask(apiKey0)));
      assertTrue(keys.contains(this.service.mask(apiKey1)));

      assertNotNull(this.service.get(apiKey0, false));
      assertNotNull(this.service.get(apiKey1, false));
      assertNull(this.service.get(ID.uuid(), false));

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
   */
  @Test
  public void testCreateApiKey02() {
    // given
    final int start = 100;
    final int limit = 200;
    final int max = 203;
    String userId = "joe";
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(List.of(ApiPermission.READ)).build();

    for (String siteId : Arrays.asList(null, ID.uuid())) {

      for (int i = start; i < start + max; i++) {
        // when
        this.service.createApiKey(siteId, "test_" + i, role, List.of());
      }

      // then
      Pagination<ApiKey> list = this.service.list(siteId, null, limit);
      assertEquals(limit, list.getResults().size());
      assertEquals("test_100", list.getResults().get(0).name());
      assertEquals("test_101", list.getResults().get(1).name());
      assertEquals("test_102", list.getResults().get(2).name());

      list = this.service.list(siteId, list.getNextToken(), limit);
      assertEquals(max - limit, list.getResults().size());
      assertEquals("test_300", list.getResults().get(0).name());
      assertEquals("test_301", list.getResults().get(1).name());
      assertEquals("test_302", list.getResults().get(2).name());
    }
  }

  /** Existing records without a role type preserve legacy grants exactly. */
  @Test
  public void testLegacyRecord() {
    // given
    ApiKey original = ApiKey.builder().name("legacy").apiKey(ID.uuid())
        .permissions(List.of(ApiPermission.WRITE)).build("default");
    var attributes = new java.util.HashMap<>(original.getAttributes());
    attributes.remove("roleType");
    // when
    ApiKey legacy = ApiKey.fromAttributeMap(attributes);
    // then
    assertEquals(ApiPermissionRoleType.CUSTOM, legacy.roleType());
    assertEquals(List.of(ApiPermission.WRITE), legacy.permissions());
    assertFalse(legacy.permissionRole().hasPermission(ApiPermission.READ_DOCUMENTS));
  }

  /** Role metadata survives storage and masked list responses. */
  @Test
  public void testStoredRole() {
    // given
    String siteId = ID.uuid();
    var role = ApiPermissionRole.builder(ApiPermissionRoleType.SYSTEM_MANAGEMENT).build();
    // when
    String value = service.createApiKey(siteId, "role", role, List.of());
    ApiKey key = service.get(value, false);
    ApiKey masked = service.list(siteId, null, LIMIT).getResults().get(0);
    // then
    assertEquals(ApiPermissionRoleType.SYSTEM_MANAGEMENT, key.roleType());
    assertEquals(role.getPermissions(), key.permissionRole().getPermissions());
    assertEquals(key.roleType(), masked.roleType());
    assertEquals(key.permissions(), masked.permissions());
  }

}
