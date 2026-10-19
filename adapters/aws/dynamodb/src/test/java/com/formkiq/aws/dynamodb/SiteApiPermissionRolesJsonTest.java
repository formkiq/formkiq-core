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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests the JSON string representation of versioned site permission bitmaps. */
class SiteApiPermissionRolesJsonTest {

  /** An empty collection uses an empty JSON object. */
  @Test
  void testEmptyJson() {
    // given
    SiteApiPermissionRoles original = new SiteApiPermissionRoles(List.of());

    // when
    String json = original.exportPermissionsAsString();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(json);

    // then
    assertEquals("{}", json);
    assertTrue(imported.roles().isEmpty());
    assertFalse(imported.hasPermission(null, ApiPermission.ADMIN));
  }

  /** JSON escaping preserves Unicode, quotes, backslashes and control characters in site IDs. */
  @Test
  void testEscapedSiteId() {
    // given
    final String siteId = "team/é\"\\\n<>&=";
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    final SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole(siteId, role)));

    // when
    String json = original.exportPermissionsAsString();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(json);
    boolean allowed = imported.hasPermission(siteId, ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    final boolean siblingAllowed = imported.hasPermission(siteId, ApiPermission.READ_DOCUMENTS);

    // then
    assertEquals(Set.of(siteId),
        imported.roles().stream().map(SiteApiPermissionRole::siteId).collect(Collectors.toSet()));
    assertEquals(original.exportPermissions(), imported.exportPermissions());
    assertTrue(allowed);
    assertFalse(siblingAllowed);
  }

  /** Compact JSON has deterministic site ordering and the same grants as the map form. */
  @Test
  void testExportImportJson() {
    // given
    final Map<String, String> bitmaps =
        Map.of("site-b", "v1:AQAAAAAAAAAA", "default", "v1:AAgAAAAAAAAA");
    SiteApiPermissionRoles roles = SiteApiPermissionRoles.importPermissions(bitmaps);
    final String expected = "{\"default\":\"v1:AAgAAAAAAAAA\"," + "\"site-b\":\"v1:AQAAAAAAAAAA\"}";

    // when
    String exported = roles.exportPermissionsAsString();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(expected);
    boolean defaultRead = imported.hasPermission(null, ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    final boolean defaultAdmin = imported.hasPermission("default", ApiPermission.ADMIN);
    final boolean namedAdmin = imported.hasPermission("site-b", ApiPermission.ADMIN);

    // then
    assertEquals(expected, exported);
    assertEquals(bitmaps, imported.exportPermissions());
    assertTrue(defaultRead);
    assertFalse(defaultAdmin);
    assertTrue(namedAdmin);
  }

  /**
   * Invalid JSON shapes, lenient syntax, duplicate sites and invalid bitmaps reject import.
   *
   * @param json invalid JSON string
   */
  @ParameterizedTest
  @ValueSource(strings = {"", " ", "null", "[]", "\"value\"", "true", "123", "{",
      "{\"site-a\":null}", "{\"site-a\":123}", "{\"site-a\":true}", "{\"site-a\":[]}",
      "{\"site-a\":{}}", "{site-a:\"v1:AQAAAAAAAAAA\"}", "{'site-a':'v1:AQAAAAAAAAAA'}",
      "{\"site-a\":\"v1:AQAAAAAAAAAA\",}", "{/*comment*/\"site-a\":\"v1:AQAAAAAAAAAA\"}", "{} {}",
      "{} trailing", "{\"site-a\":\"v2:AQAAAAAAAAA\"}", "{\"site-a\":\"v1:%%%\"}",
      "{\"site-a\":\"v1:AQAAAAAAAAAA\",\"site-a\":\"v1:AAgAAAAAAAAA\"}",
      "{\"site-a\":\"v1:AQAAAAAAAAAA\",\"site-a\":\"v1:AQAAAAAAAAAA\"}"})
  void testInvalidJson(final String json) {
    // given
    String encoded = json;

    // when
    Executable importing = () -> SiteApiPermissionRoles.importPermissions(encoded);

    // then
    assertThrows(IllegalArgumentException.class, importing);
  }

  /** A null JSON string fails explicitly. */
  @Test
  void testNullJson() {
    // given
    String json = null;

    // when
    Executable importing = () -> SiteApiPermissionRoles.importPermissions(json);

    // then
    assertThrows(NullPointerException.class, importing);
  }

  /**
   * String serialization preserves predefined and custom role grants exactly.
   *
   * @param type role type
   */
  @ParameterizedTest
  @EnumSource(ApiPermissionRoleType.class)
  void testRoleRoundTrip(final ApiPermissionRoleType type) {
    // given
    final ApiPermissionRole role = ApiPermissionRole.builder(type).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole(null, role)));

    // when
    String json = original.exportPermissionsAsString();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(json);

    // then
    assertEquals(role.getPermissions(), imported.roles().iterator().next().role().getPermissions());
    assertEquals(ApiPermissionRoleType.CUSTOM, imported.roles().iterator().next().role().getType());
    assertEquals(original.exportPermissions(), imported.exportPermissions());
  }

  /** Strict JSON accepts insignificant whitespace around and within the object. */
  @Test
  void testWhitespaceJson() {
    // given
    String json = " \n { \"default\" : \"v1:AAgAAAAAAAAA\" } \t ";

    // when
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(json);

    // then
    assertEquals(Map.of("default", "v1:AAgAAAAAAAAA"), imported.exportPermissions());
    assertTrue(imported.hasPermission(null, ApiPermission.READ_DOCUMENT_ATTRIBUTES));
  }
}

