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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests permission union, site isolation and immutable merger results. */
class SiteApiPermissionRolesMergerTest {

  /** Null and explicit default site IDs share a single permission union. */
  @Test
  void testDefaultSiteAliases() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    ApiPermissionRole deleter = ApiPermissionRole.builder(ApiPermissionRoleType.DELETE).build();
    List<SiteApiPermissionRole> roles = List.of(new SiteApiPermissionRole(null, reader),
        new SiteApiPermissionRole("default", deleter));

    // when
    Collection<SiteApiPermissionRole> result = new SiteApiPermissionRolesMerger().apply(roles);
    SiteApiPermissionRole merged = result.iterator().next();

    // then
    assertEquals(1, result.size());
    assertEquals("default", merged.siteId());
    assertEquals(Set.of(ApiPermission.READ, ApiPermission.DELETE), merged.role().getPermissions());
    assertFalse(merged.role().hasPermission(ApiPermission.WRITE_DOCUMENTS));
  }

  /** Empty input produces an empty collection. */
  @Test
  void testEmptyRoles() {
    // given
    List<SiteApiPermissionRole> roles = List.of();

    // when
    Collection<SiteApiPermissionRole> result = new SiteApiPermissionRolesMerger().apply(roles);

    // then
    assertTrue(result.isEmpty());
  }

  /** Result mutations and later input edits cannot change the merged collection. */
  @Test
  void testImmutableResult() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    List<SiteApiPermissionRole> roles =
        new ArrayList<>(List.of(new SiteApiPermissionRole("site-a", reader)));

    // when
    Collection<SiteApiPermissionRole> result = new SiteApiPermissionRolesMerger().apply(roles);
    roles.clear();
    Executable mutation = result::clear;

    // then
    assertEquals(1, result.size());
    assertSame(reader, result.iterator().next().role());
    assertThrows(UnsupportedOperationException.class, mutation);
  }

  /**
   * Duplicate-site grants combine regardless of their insertion order.
   *
   * @param reverse whether to insert the read role last
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testMergeOrder(final boolean reverse) {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    ApiPermissionRole writer = ApiPermissionRole.builder(ApiPermissionRoleType.WRITE).build();
    SiteApiPermissionRole readRole = new SiteApiPermissionRole("default", reader);
    SiteApiPermissionRole writeRole = new SiteApiPermissionRole("default", writer);
    List<SiteApiPermissionRole> roles =
        reverse ? List.of(writeRole, readRole) : List.of(readRole, writeRole);
    Function<Collection<SiteApiPermissionRole>, Collection<SiteApiPermissionRole>> merger =
        new SiteApiPermissionRolesMerger();

    // when
    Collection<SiteApiPermissionRole> result = merger.apply(roles);
    ApiPermissionRole merged = result.iterator().next().role();

    // then
    assertEquals(1, result.size());
    assertEquals(ApiPermissionRoleType.CUSTOM, merged.getType());
    assertEquals(Set.of(ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE),
        merged.getPermissions());
    assertTrue(merged.hasPermission(ApiPermission.READ_DOCUMENT_ATTRIBUTES));
    assertTrue(merged.hasPermission(ApiPermission.WRITE_DOCUMENT_ATTRIBUTES));
    assertTrue(merged.hasPermission(ApiPermission.DELETE_DOCUMENT_ATTRIBUTES));
    assertFalse(merged.hasPermission(ApiPermission.GOVERN_DOCUMENT_FOLDERS));
    assertEquals(Set.of(ApiPermission.READ), reader.getPermissions());
    assertEquals(ApiPermissionRoleType.WRITE, writer.getType());
  }

  /** Null input collections, entries and permission roles are rejected. */
  @Test
  void testNullRoles() {
    // given
    SiteApiPermissionRolesMerger merger = new SiteApiPermissionRolesMerger();
    List<SiteApiPermissionRole> nullEntry = Arrays.asList((SiteApiPermissionRole) null);
    List<SiteApiPermissionRole> nullRole = List.of(new SiteApiPermissionRole("site-a", null));

    // when
    Executable missingCollection = () -> merger.apply(null);
    Executable missingEntry = () -> merger.apply(nullEntry);
    Executable missingRole = () -> merger.apply(nullRole);

    // then
    assertThrows(NullPointerException.class, missingCollection);
    assertThrows(NullPointerException.class, missingEntry);
    assertThrows(NullPointerException.class, missingRole);
  }

  /**
   * Combining privileged and read roles retains their effective grants.
   *
   * @param type privileged role type
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermissionRoleType.class, names = {"ADMIN", "GOVERN"})
  void testPrivilegedRoles(final ApiPermissionRoleType type) {
    // given
    ApiPermissionRole privileged = ApiPermissionRole.builder(type).build();
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    List<SiteApiPermissionRole> roles = List.of(new SiteApiPermissionRole("site-a", privileged),
        new SiteApiPermissionRole("site-a", reader));

    // when
    ApiPermissionRole merged =
        new SiteApiPermissionRolesMerger().apply(roles).iterator().next().role();

    // then
    assertEquals(ApiPermissionRoleType.CUSTOM, merged.getType());
    assertEquals(privileged.getPermissions(), merged.getPermissions());
    assertTrue(merged.hasPermission(ApiPermission.READ_DOCUMENTS));
    assertTrue(merged.hasPermission(ApiPermission.WRITE_DOCUMENTS));
    assertTrue(merged.hasPermission(ApiPermission.DELETE_DOCUMENTS));
    assertTrue(merged.hasPermission(ApiPermission.GOVERN_DOCUMENTS));
    assertEquals(type == ApiPermissionRoleType.ADMIN, merged.hasPermission(ApiPermission.ADMIN));
  }

  /** Scoped grants remain exact and overlapping permissions appear only once. */
  @Test
  void testScopedGrants() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    ApiPermissionRole writer = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(
            Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES, ApiPermission.WRITE_DOCUMENT_ACTIONS))
        .build();
    List<SiteApiPermissionRole> roles = List.of(new SiteApiPermissionRole("site-a", reader),
        new SiteApiPermissionRole("site-a", writer), new SiteApiPermissionRole("site-a", reader));

    // when
    Collection<SiteApiPermissionRole> result = new SiteApiPermissionRolesMerger().apply(roles);
    ApiPermissionRole merged = result.iterator().next().role();

    // then
    assertEquals(1, result.size());
    assertEquals(
        Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES, ApiPermission.WRITE_DOCUMENT_ACTIONS),
        merged.getPermissions());
    assertTrue(merged.hasPermission(ApiPermission.READ_DOCUMENT_ATTRIBUTES));
    assertTrue(merged.hasPermission(ApiPermission.WRITE_DOCUMENT_ACTIONS));
    assertFalse(merged.hasPermission(ApiPermission.READ_DOCUMENTS));
    assertFalse(merged.hasPermission(ApiPermission.WRITE_DOCUMENT_ATTRIBUTES));
    assertFalse(merged.hasPermission(ApiPermission.READ));
  }

  /** Single roles retain their type and grants while sites are sorted and isolated. */
  @Test
  void testSingleRolesAndSiteIsolation() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    ApiPermissionRole writer = ApiPermissionRole.builder(ApiPermissionRoleType.WRITE).build();
    List<SiteApiPermissionRole> roles = List.of(new SiteApiPermissionRole("site-b", writer),
        new SiteApiPermissionRole("site-a", reader));

    // when
    List<SiteApiPermissionRole> result =
        List.copyOf(new SiteApiPermissionRolesMerger().apply(roles));

    // then
    assertEquals(List.of("site-a", "site-b"),
        result.stream().map(SiteApiPermissionRole::siteId).toList());
    assertSame(reader, result.get(0).role());
    assertSame(writer, result.get(1).role());
    assertFalse(result.get(0).role().hasPermission(ApiPermission.WRITE_DOCUMENTS));
    assertTrue(result.get(1).role().hasPermission(ApiPermission.WRITE_DOCUMENTS));
  }
}
