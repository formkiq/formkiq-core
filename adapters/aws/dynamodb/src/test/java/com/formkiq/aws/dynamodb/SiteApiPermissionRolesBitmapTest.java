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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests the versioned wire format, exact grant restoration and invalid payload rejection. */
class SiteApiPermissionRolesBitmapTest {

  /** Zero grants and zero sites have distinct round-trip representations. */
  @Test
  void testEmptyPermissions() {
    // given
    SiteApiPermissionRoles empty = new SiteApiPermissionRoles(List.of());
    final Map<String, String> zeroGrants = Map.of("default", "v1:AAAAAAAAAAAA");

    // when
    Map<String, String> exported = empty.exportPermissions();
    SiteApiPermissionRoles noSites = SiteApiPermissionRoles.importPermissions(exported);
    SiteApiPermissionRoles noGrants = SiteApiPermissionRoles.importPermissions(zeroGrants);

    // then
    assertTrue(exported.isEmpty());
    assertTrue(noSites.roles().isEmpty());
    assertEquals(Set.of("default"),
        noGrants.roles().stream().map(SiteApiPermissionRole::siteId).collect(Collectors.toSet()));
    assertTrue(noGrants.roles().iterator().next().role().getPermissions().isEmpty());
    assertFalse(noGrants.hasPermission(null, ApiPermission.ADMIN));
    assertEquals(zeroGrants, noGrants.exportPermissions());
  }

  /** Import copies its input and export cannot mutate the original permission model. */
  @Test
  void testImmutableImportExport() {
    // given
    Map<String, String> source = new HashMap<>(Map.of("site-a", "v1:AAgAAAAAAAAA"));

    // when
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(source);
    source.clear();
    final Map<String, String> exported = imported.exportPermissions();
    Executable mutation = () -> exported.put("site-b", "v1:AQAAAAAAAAAA");

    // then
    assertTrue(imported.hasPermission("site-a", ApiPermission.READ_DOCUMENT_ATTRIBUTES));
    assertEquals(Map.of("site-a", "v1:AAgAAAAAAAAA"), exported);
    assertThrows(UnsupportedOperationException.class, mutation);
    assertFalse(imported.hasPermission("site-b", ApiPermission.ADMIN));
  }

  /**
   * Unsupported versions, invalid encodings, wrong lengths and unused bits reject the whole import.
   *
   * @param bitmap invalid bitmap
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "v0:AAAAAAAAAAAA", "v2:AAAAAAAAAAAA", "AAAAAAAAAAAA", "v1:", "v1:%%%",
      "v1:AQ", "v1:AAAAAAAAAAA", "v1:AAAAAAAAAAAAAA", "v1:AAAAAAAAAAAA=", "v1:////////////",
      "v1:AAAAAAAAAAAI", "v1:AAAAAAAAAACA"})
  void testInvalidBitmap(final String bitmap) {
    // given
    Map<String, String> encoded = Map.of("site-a", "v1:AQAAAAAAAAAA", "site-b", bitmap);

    // when
    Executable importing = () -> SiteApiPermissionRoles.importPermissions(encoded);

    // then
    assertThrows(IllegalArgumentException.class, importing);
  }

  /** Site collections retain identifiers verbatim and keep grants isolated between sites. */
  @Test
  void testMultipleSites() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    ApiPermissionRole writer = ApiPermissionRole.builder(ApiPermissionRoleType.WRITE).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole(null, reader),
            new SiteApiPermissionRole("team/é:=one", writer)));

    // when
    SiteApiPermissionRoles imported =
        SiteApiPermissionRoles.importPermissions(original.exportPermissions());
    boolean defaultRead = imported.hasPermission(null, ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    boolean defaultWrite =
        imported.hasPermission("default", ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);
    final boolean namedWrite =
        imported.hasPermission("team/é:=one", ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);
    final boolean unknownRead =
        imported.hasPermission("unknown", ApiPermission.READ_DOCUMENT_ATTRIBUTES);

    // then
    assertEquals(Set.of("default", "team/é:=one"),
        imported.roles().stream().map(SiteApiPermissionRole::siteId).collect(Collectors.toSet()));
    assertTrue(defaultRead);
    assertFalse(defaultWrite);
    assertTrue(namedWrite);
    assertFalse(unknownRead);
  }

  /** Null maps, keys and bitmap values are rejected before any grants are restored. */
  @Test
  void testNullImport() {
    // given
    Map<String, String> nullKey = new HashMap<>();
    nullKey.put(null, "v1:AAAAAAAAAAAA");
    Map<String, String> nullValue = new HashMap<>();
    nullValue.put("site-a", null);

    // when
    Executable missingMap =
        () -> SiteApiPermissionRoles.importPermissions((Map<String, String>) null);
    Executable missingKey = () -> SiteApiPermissionRoles.importPermissions(nullKey);
    Executable missingValue = () -> SiteApiPermissionRoles.importPermissions(nullValue);

    // then
    assertThrows(NullPointerException.class, missingMap);
    assertThrows(NullPointerException.class, missingKey);
    assertThrows(NullPointerException.class, missingValue);
  }

  /** Invalid in-memory roles fail export instead of producing an incomplete bitmap. */
  @Test
  void testNullRoleExport() {
    // given
    SiteApiPermissionRoles roles =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole("site-a", null)));

    // when
    Executable exporting = roles::exportPermissions;

    // then
    assertThrows(NullPointerException.class, exporting);
  }

  /** Policy grants and other site permissions round trip using the version 1 catalog. */
  @Test
  void testPresetPolicyBitmap() {
    // given
    final Set<ApiPermission> grants = Set.of(ApiPermission.ADMIN, ApiPermission.DELETE_WEBHOOKS,
        ApiPermission.READ_REMINDER_POLICY, ApiPermission.WRITE_REMINDER_POLICY,
        ApiPermission.DELETE_REMINDER_POLICY, ApiPermission.READ_RETENTION_POLICY,
        ApiPermission.WRITE_RETENTION_POLICY, ApiPermission.DELETE_RETENTION_POLICY);
    ApiPermissionRole policies =
        ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(grants).build();
    ApiPermissionRole documents = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole("policies", policies),
            new SiteApiPermissionRole("documents", documents)));
    final Map<String, String> expected =
        Map.of("policies", "v1:AQAAAAAAAPAH", "documents", "v1:AAgAAAAAAAAA");

    // when
    Map<String, String> exported = original.exportPermissions();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(expected);
    final SiteApiPermissionRoles jsonImported =
        SiteApiPermissionRoles.importPermissions(original.exportPermissionsAsString());

    // then
    assertEquals(expected, exported);
    assertEquals(grants, imported.roles().stream().filter(role -> "policies".equals(role.siteId()))
        .findFirst().orElseThrow().role().getPermissions());
    assertEquals(expected, imported.exportPermissions());
    assertEquals(expected, jsonImported.exportPermissions());
    assertFalse(imported.hasPermission("documents", ApiPermission.READ_RETENTION_POLICY));
    assertFalse(imported.hasPermission("documents", ApiPermission.READ_REMINDER_POLICY));
  }

  /**
   * Predefined roles restore their exact grants as CUSTOM instead of expanding on import.
   *
   * @param type original role type
   */
  @ParameterizedTest
  @EnumSource(ApiPermissionRoleType.class)
  void testRoleRoundTrip(final ApiPermissionRoleType type) {
    // given
    final ApiPermissionRole role = ApiPermissionRole.builder(type).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole(null, role)));

    // when
    Map<String, String> exported = original.exportPermissions();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(exported);

    // then
    assertEquals(role.getPermissions(), imported.roles().iterator().next().role().getPermissions());
    assertEquals(ApiPermissionRoleType.CUSTOM, imported.roles().iterator().next().role().getType());
    assertEquals(exported, imported.exportPermissions());
    assertEquals(original.hasPermission(null, ApiPermission.READ_DOCUMENTS),
        imported.hasPermission(null, ApiPermission.READ_DOCUMENTS));
  }

  /**
   * Every permission survives serialization without adding sibling or parent grants.
   *
   * @param permission permission to round trip
   */
  @ParameterizedTest
  @EnumSource(ApiPermission.class)
  void testSinglePermissionRoundTrip(final ApiPermission permission) {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(permission)).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole("site-a", role)));

    // when
    SiteApiPermissionRoles imported =
        SiteApiPermissionRoles.importPermissions(original.exportPermissions());

    // then
    assertEquals(Set.of(permission), imported.roles().iterator().next().role().getPermissions());
    assertTrue(imported.hasPermission("site-a", permission));
    assertFalse(imported.hasPermission("site-b", permission));
  }

  /** Pinned bytes protect the bit order and Base64URL wire format. */
  @Test
  void testVersionOneGoldenBitmap() {
    // given
    final Set<ApiPermission> grants = Set.of(ApiPermission.ADMIN,
        ApiPermission.GOVERN_ACCESS_CONTROL, ApiPermission.READ_DOCUMENT_ATTRIBUTES,
        ApiPermission.WRITE_DOCUMENT_ATTRIBUTES, ApiPermission.DELETE_WEBHOOKS);
    ApiPermissionRole role =
        ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(grants).build();
    SiteApiPermissionRoles original =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole("site-a", role)));
    final Map<String, String> expected = Map.of("site-a", "v1:IRgAAAAAABAA");

    // when
    Map<String, String> exported = original.exportPermissions();
    SiteApiPermissionRoles imported = SiteApiPermissionRoles.importPermissions(expected);

    // then
    assertEquals(expected, exported);
    assertEquals(grants, imported.roles().iterator().next().role().getPermissions());
    assertEquals(expected, imported.exportPermissions());
  }
}

