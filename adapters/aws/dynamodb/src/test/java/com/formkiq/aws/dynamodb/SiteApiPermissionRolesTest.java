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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests immutable site roles and site-scoped permission checks. */
class SiteApiPermissionRolesTest {

  /**
   * Null and explicit default site IDs refer to the same site.
   *
   * @param storedSiteId site identifier used in the stored role
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "default")
  void testDefaultSite(final String storedSiteId) {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    SiteApiPermissionRoles roles =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole(storedSiteId, reader)));

    // when
    boolean implicitDefault = roles.hasPermission(null, ApiPermission.READ_DOCUMENTS);
    boolean explicitDefault = roles.hasPermission("default", ApiPermission.READ_DOCUMENTS);
    boolean namedSite = roles.hasPermission("site-a", ApiPermission.READ_DOCUMENTS);

    // then
    assertTrue(implicitDefault);
    assertTrue(explicitDefault);
    assertFalse(namedSite);
  }

  /** An empty collection grants no permissions. */
  @Test
  void testEmptyRoles() {
    // given
    SiteApiPermissionRoles roles = new SiteApiPermissionRoles(List.of());

    // when
    boolean allowed = roles.hasPermission("site-a", ApiPermission.READ_DOCUMENTS);
    boolean defaultAllowed = roles.hasPermission(null, ApiPermission.READ_DOCUMENTS);

    // then
    assertFalse(allowed);
    assertFalse(defaultAllowed);
  }

  /** Source edits and accessor mutations cannot change site permissions. */
  @Test
  void testImmutableRoles() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.READ).build();
    final SiteApiPermissionRole siteRole = new SiteApiPermissionRole("site-a", reader);
    Collection<SiteApiPermissionRole> source = new ArrayList<>(List.of(siteRole));

    // when
    SiteApiPermissionRoles roles = new SiteApiPermissionRoles(source);
    source.clear();
    boolean allowed = roles.hasPermission("site-a", ApiPermission.READ_DOCUMENTS);
    Executable mutation = () -> roles.roles().clear();

    // then
    assertEquals(List.of(siteRole), roles.roles());
    assertTrue(allowed);
    assertThrows(UnsupportedOperationException.class, mutation);
  }

  /** Multiple roles for a site combine grants and preserve them during bitmap export. */
  @Test
  void testMultipleRolesForSite() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    ApiPermissionRole writer = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.WRITE_DOCUMENT_ATTRIBUTES)).build();
    SiteApiPermissionRoles source = new SiteApiPermissionRoles(List
        .of(new SiteApiPermissionRole(null, reader), new SiteApiPermissionRole("default", writer)));

    // when
    final SiteApiPermissionRoles imported =
        SiteApiPermissionRoles.importPermissions(source.exportPermissions());
    boolean readAllowed = source.hasPermission("default", ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    boolean writeAllowed = source.hasPermission(null, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);
    final boolean unknownAllowed =
        source.hasPermission("unknown", ApiPermission.READ_DOCUMENT_ATTRIBUTES);

    // then
    assertTrue(readAllowed);
    assertTrue(writeAllowed);
    assertFalse(unknownAllowed);
    assertEquals(
        Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES, ApiPermission.WRITE_DOCUMENT_ATTRIBUTES),
        imported.roles().iterator().next().role().getPermissions());
    assertEquals("v1:ABgAAAAAAAAA", source.exportPermissions().get("default"));
  }

  /** Missing collections and entries are rejected on construction. */
  @Test
  void testNullRoles() {
    // given
    Collection<SiteApiPermissionRole> nullEntry = Arrays.asList((SiteApiPermissionRole) null);

    // when
    Executable missingCollection = () -> new SiteApiPermissionRoles(null);
    Executable missingEntry = () -> new SiteApiPermissionRoles(nullEntry);

    // then
    assertThrows(NullPointerException.class, missingCollection);
    assertThrows(NullPointerException.class, missingEntry);
  }

  /** Permission grants, including ADMIN, cannot cross site boundaries. */
  @Test
  void testSitePermissions() {
    // given
    ApiPermissionRole reader = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();
    ApiPermissionRole admin = ApiPermissionRole.builder(ApiPermissionRoleType.ADMIN).build();
    SiteApiPermissionRoles roles =
        new SiteApiPermissionRoles(List.of(new SiteApiPermissionRole("site-a", reader),
            new SiteApiPermissionRole("site-b", admin)));

    // when
    boolean allowed = roles.hasPermission("site-a", ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    boolean siblingAllowed = roles.hasPermission("site-a", ApiPermission.READ_DOCUMENTS);
    boolean writeAllowed = roles.hasPermission("site-a", ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);
    final boolean adminAllowed =
        roles.hasPermission("site-b", ApiPermission.WRITE_DOCUMENT_ATTRIBUTES);
    final boolean unknownAllowed =
        roles.hasPermission("unknown", ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    final boolean nullAllowed = roles.hasPermission("site-b", null);

    // then
    assertTrue(allowed);
    assertFalse(siblingAllowed);
    assertFalse(writeAllowed);
    assertTrue(adminAllowed);
    assertFalse(unknownAllowed);
    assertFalse(nullAllowed);
  }
}

