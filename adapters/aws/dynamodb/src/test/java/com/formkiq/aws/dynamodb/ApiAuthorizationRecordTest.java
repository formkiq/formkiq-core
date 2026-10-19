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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Tests for storing and restoring authorization contexts. */
class ApiAuthorizationRecordTest {

  /** Expiration in epoch seconds. */
  private static final long EXPIRATION = 1800000000L;

  /**
   * Assert caller identity, claims and explicit permission role types are preserved.
   *
   * @param expected original authorization
   * @param actual restored authorization
   */
  private void assertAuthorization(final ApiAuthorization expected, final ApiAuthorization actual) {
    assertEquals(expected.getUsername(), actual.getUsername());
    assertEquals(expected.getSiteId(), actual.getSiteId());
    assertEquals(expected.getCustomEmail(), actual.getCustomEmail());
    assertEquals(expected.getRoles(), actual.getRoles());
    assertEquals(expected.getSamlGroups(), actual.getSamlGroups());
    assertEquals(expected.getRoleSiteMap(), actual.getRoleSiteMap());
    assertEquals(expected.getUserClaims(), actual.getUserClaims());
    assertEquals(expected.getSiteIds(), actual.getSiteIds());
    Map<String, ApiPermissionRole> actualRoles = new HashMap<>();
    actual.getSitePermissions().roles()
        .forEach(role -> actualRoles.put(role.siteId(), role.role()));
    for (SiteApiPermissionRole permission : expected.getSitePermissions().roles()) {
      assertEquals(permission.role().getType(), actualRoles.get(permission.siteId()).getType());
      assertEquals(permission.role().getPermissions(), actual.getPermissions(permission.siteId()));
    }
  }

  /**
   * Create authorization with multiple sites and mixed claim values.
   *
   * @return authorization fixture
   */
  private ApiAuthorization createAuthorization() {
    return new ApiAuthorization().siteId("finance").username("reviewer")
        .customEmail("reviewer@example.com").roles(List.of("finance", "API_KEY"))
        .samlGroups(List.of("reviewers")).roleSiteMap(Map.of("reviewers", "finance"))
        .userClaims(Map.of("custom:department", "Finance", "limit", 42.5, "active", true, "profile",
            Map.of("region", "north", "weights", List.of(0.0, -2.5)), "literal.key", "value"))
        .addPermission(new SiteApiPermissionRole("finance",
            ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
                .permissions(Set.of(ApiPermission.READ_DOCUMENTS)).build()))
        .addPermission(new SiteApiPermissionRole("legal",
            ApiPermissionRole.builder(ApiPermissionRoleType.READ).build()));
  }

  /** Admin and default-site roles survive persistence without creating extra explicit sites. */
  @Test
  void testAdminAndDefaultSitePermissions() {
    // given
    ApiAuthorization original = new ApiAuthorization()
        .addPermission(new SiteApiPermissionRole("admin",
            ApiPermissionRole.builder(ApiPermissionRoleType.ADMIN).build()))
        .addPermission(new SiteApiPermissionRole("default",
            ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(Set.of()).build()));
    ApiAuthorizationRecord record = ApiAuthorizationRecord.fromApiAuthorization(original,
        new DynamoDbKey("apiAuthorization#admin", "apiAuthorization"), null);

    // when
    ApiAuthorization restored =
        ApiAuthorizationRecord.fromAttributeMap(record.getAttributes()).toApiAuthorization();

    // then
    assertAuthorization(original, restored);
    assertTrue(restored.isAdmin("another-site"));
    assertEquals(original.getPermissions("another-site"), restored.getPermissions("another-site"));
    assertEquals(2, restored.getSitePermissions().roles().size());
  }

  /** Capturing and restoring retains identity and scoped grants without expanding permissions. */
  @Test
  void testAuthorizationRoundTrip() {
    // given
    ApiAuthorization original = createAuthorization();

    // when
    ApiAuthorizationRecord record = ApiAuthorizationRecord.fromApiAuthorization(original);
    ApiAuthorization restored = record.toApiAuthorization();

    // then
    assertAuthorization(original, restored);
    assertFalse(restored.isAdmin("finance"));
    assertEquals(Set.of(ApiPermission.READ_DOCUMENTS), restored.getPermissions("finance"));
    assertTrue(restored.getPermissions("unrelated").isEmpty());
  }

  /** Conversion leaves thread-local login unchanged and excludes cached request objects. */
  @Test
  void testConversionDoesNotLoginOrCopyRequestCache() {
    // given
    ApiAuthorization current = new ApiAuthorization().username("current");
    ApiAuthorization original = createAuthorization().addCacheObject("request", "cached");
    ApiAuthorization.login(current);
    try {
      // when
      final ApiAuthorization restored =
          ApiAuthorizationRecord.fromApiAuthorization(original).toApiAuthorization();

      // then
      assertSame(current, ApiAuthorization.getAuthorization());
      assertNull(restored.getCacheObject("request"));
    } finally {
      ApiAuthorization.logout();
    }
  }

  /** Optional authorization fields remain absent and no permissions are invented. */
  @Test
  void testDefaultAuthorizationRoundTrip() {
    // given
    ApiAuthorization original = new ApiAuthorization();
    ApiAuthorizationRecord record = ApiAuthorizationRecord.fromApiAuthorization(original,
        new DynamoDbKey("apiAuthorization#system", "apiAuthorization"), null);

    // when
    Map<String, AttributeValue> attributes = record.getAttributes();
    ApiAuthorizationRecord stored = ApiAuthorizationRecord.fromAttributeMap(attributes);
    ApiAuthorization restored = stored.toApiAuthorization();

    // then
    assertAuthorization(original, restored);
    assertNull(stored.timeToLive());
    assertFalse(attributes.containsKey("TimeToLive"));
    assertFalse(attributes.containsKey("userClaims"));
    assertTrue(restored.getAllPermissions().isEmpty());
  }

  /** DynamoDB serialization retains the key, TTL, literal claim keys and nested numeric claims. */
  @Test
  void testDynamoDbRoundTrip() {
    // given
    ApiAuthorization original = createAuthorization();
    DynamoDbKey key = new DynamoDbKey("apiAuthorization#request-1", "apiAuthorization");
    ApiAuthorizationRecord record =
        ApiAuthorizationRecord.fromApiAuthorization(original, key, EXPIRATION);

    // when
    Map<String, AttributeValue> attributes = record.getAttributes();
    ApiAuthorizationRecord stored = ApiAuthorizationRecord.fromAttributeMap(attributes);
    ApiAuthorization restored = stored.toApiAuthorization();

    // then
    assertAuthorization(original, restored);
    assertEquals(key, stored.key());
    assertEquals(EXPIRATION, stored.timeToLive());
    assertEquals(Long.toString(EXPIRATION), attributes.get("TimeToLive").n());
    assertEquals("42.5", attributes.get("userClaims").m().get("limit").n());
  }
}
