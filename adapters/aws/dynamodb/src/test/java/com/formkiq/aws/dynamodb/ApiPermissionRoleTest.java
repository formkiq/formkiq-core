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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests role permissions, custom grants and immutable builder results. */
class ApiPermissionRoleTest {

  /** ADMIN grants administrative access along with every operation category. */
  @Test
  void testAdminRole() {
    // given
    ApiPermissionRoleType type = ApiPermissionRoleType.ADMIN;

    // when
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // then
    assertEquals(type, role.getType());
    assertTrue(role.getPermissions().contains(ApiPermission.ADMIN));
    assertTrue(role.getPermissions().contains(ApiPermission.GOVERN));
    assertTrue(role.hasPermission(ApiPermission.READ_DOCUMENT_ATTRIBUTES));
    assertTrue(role.hasPermission(ApiPermission.WRITE_DOCUMENTS));
    assertTrue(role.hasPermission(ApiPermission.DELETE_DOCUMENTS));
  }

  /** Custom grants remain scoped and duplicate grants are removed. */
  @Test
  void testCustomRole() {
    // given
    List<ApiPermission> grants = List.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES,
        ApiPermission.WRITE_DOCUMENT_ACTIONS, ApiPermission.READ_DOCUMENT_ATTRIBUTES);

    // when
    ApiPermissionRole role =
        ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(grants).build();

    // then
    assertEquals(ApiPermissionRoleType.CUSTOM, role.getType());
    assertEquals(
        Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES, ApiPermission.WRITE_DOCUMENT_ACTIONS),
        role.getPermissions());
    assertFalse(role.getPermissions().contains(ApiPermission.READ_DOCUMENTS));
    assertFalse(role.getPermissions().contains(ApiPermission.READ));
  }

  /** Source list edits and builder reuse do not change previously built roles. */
  @Test
  void testCustomRoleDefensiveCopy() {
    // given
    List<ApiPermission> grants = new ArrayList<>(List.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES));
    ApiPermissionRole.Builder builder = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM);

    // when
    builder.permissions(grants);
    grants.clear();
    ApiPermissionRole original = builder.build();
    ApiPermissionRole replacement =
        builder.permissions(List.of(ApiPermission.WRITE_DOCUMENTS)).build();

    // then
    assertEquals(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES), original.getPermissions());
    assertEquals(Set.of(ApiPermission.WRITE_DOCUMENTS), replacement.getPermissions());
  }

  /**
   * A custom user management grant does not imply other user management operations.
   *
   * @param permission explicit user management grant
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class, names = {"READ_USER_MANAGEMENT", "WRITE_USER_MANAGEMENT",
      "DELETE_USER_MANAGEMENT", "GOVERN_USER_MANAGEMENT"})
  void testCustomUserManagementPermissionRemainsScoped(final ApiPermission permission) {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(permission)).build();
    List<ApiPermission> userPermissions =
        List.of(ApiPermission.READ_USER_MANAGEMENT, ApiPermission.WRITE_USER_MANAGEMENT,
            ApiPermission.DELETE_USER_MANAGEMENT, ApiPermission.GOVERN_USER_MANAGEMENT);

    // when
    List<ApiPermission> granted = userPermissions.stream().filter(role::hasPermission).toList();

    // then
    assertEquals(List.of(permission), granted);
  }

  /** A CUSTOM role has no implicit grants. */
  @Test
  void testEmptyCustomRole() {
    // given
    ApiPermissionRoleType type = ApiPermissionRoleType.CUSTOM;

    // when
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // then
    assertEquals(type, role.getType());
    assertTrue(role.getPermissions().isEmpty());
  }

  /** GOVERN retains read, write and delete access without granting administrative privileges. */
  @Test
  void testGovernRole() {
    // given
    ApiPermissionRoleType type = ApiPermissionRoleType.GOVERN;

    // when
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // then
    assertEquals(type, role.getType());
    assertTrue(role.getPermissions().containsAll(List.of(ApiPermission.GOVERN, ApiPermission.READ,
        ApiPermission.WRITE, ApiPermission.DELETE)));
    List.of(ApiPermission.GOVERN_ACCESS_CONTROL, ApiPermission.GOVERN_DOCUMENT_FOLDERS,
        ApiPermission.GOVERN_DOCUMENT_VERSIONS, ApiPermission.GOVERN_DOCUMENTS,
        ApiPermission.GOVERN_USER_ACTIVITIES).forEach(p -> assertTrue(role.hasPermission(p)));
    assertFalse(role.getPermissions().contains(ApiPermission.ADMIN));
  }

  /** Custom permission membership is limited to the explicitly supplied grants. */
  @Test
  void testHasCustomPermission() {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
        .permissions(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES)).build();

    // when
    boolean granted = role.hasPermission(ApiPermission.READ_DOCUMENT_ATTRIBUTES);
    boolean siblingGranted = role.hasPermission(ApiPermission.READ_DOCUMENTS);
    boolean umbrellaGranted = role.hasPermission(ApiPermission.READ);

    // then
    assertTrue(granted);
    assertFalse(siblingGranted);
    assertFalse(umbrellaGranted);
  }

  /**
   * Custom umbrella grants cover their scoped permissions without granting other categories.
   *
   * @param grant umbrella permission
   * @param permission permission to check
   * @param expected expected membership
   */
  @ParameterizedTest
  @CsvSource({"READ, READ_DOCUMENT_ATTRIBUTES, true", "READ, READ_DOCUMENTS, true",
      "READ, WRITE_DOCUMENT_ATTRIBUTES, false", "WRITE, WRITE_DOCUMENT_ATTRIBUTES, true",
      "WRITE, READ_DOCUMENT_ATTRIBUTES, false", "DELETE, DELETE_DOCUMENT_ATTRIBUTES, true",
      "DELETE, READ_DOCUMENT_ATTRIBUTES, false", "GOVERN, GOVERN_DOCUMENT_FOLDERS, true",
      "GOVERN, READ_DOCUMENT_ATTRIBUTES, false", "ADMIN, READ_DOCUMENT_ATTRIBUTES, true",
      "ADMIN, GOVERN_DOCUMENT_FOLDERS, true"})
  void testHasCustomUmbrellaPermission(final ApiPermission grant, final ApiPermission permission,
      final boolean expected) {
    // given
    ApiPermissionRole role =
        ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(Set.of(grant)).build();

    // when
    boolean granted = role.hasPermission(permission);

    // then
    assertEquals(expected, granted);
  }

  /**
   * A null permission is never granted, including by ADMIN.
   *
   * @param type role type
   */
  @ParameterizedTest
  @EnumSource(ApiPermissionRoleType.class)
  void testHasNullPermission(final ApiPermissionRoleType type) {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // when
    boolean granted = role.hasPermission(null);

    // then
    assertFalse(granted);
  }

  /**
   * Permission membership respects predefined role boundaries.
   *
   * @param type role type
   * @param permission permission to check
   * @param expected expected membership
   */
  @ParameterizedTest
  @CsvSource({"ADMIN, GOVERN_ACCESS_CONTROL, true", "ADMIN, WRITE_DOCUMENTS, true",
      "DELETE, DELETE_DOCUMENT_ATTRIBUTES, true", "DELETE, READ_DOCUMENT_ATTRIBUTES, false",
      "READ, READ_DOCUMENT_ATTRIBUTES, true", "READ, WRITE_DOCUMENT_ATTRIBUTES, false",
      "READ, GOVERN_DOCUMENT_FOLDERS, false", "WRITE, WRITE_DOCUMENT_ATTRIBUTES, true",
      "WRITE, READ_DOCUMENT_ATTRIBUTES, true", "WRITE, DELETE_DOCUMENT_ATTRIBUTES, true",
      "WRITE, GOVERN_DOCUMENT_FOLDERS, false", "GOVERN, GOVERN_DOCUMENTS, true",
      "GOVERN, READ_DOCUMENTS, true", "GOVERN, WRITE_DOCUMENT_ATTRIBUTES, true",
      "GOVERN, DELETE_DOCUMENTS, true", "GOVERN, ADMIN, false",
      "CUSTOM, READ_DOCUMENT_ATTRIBUTES, false"})
  void testHasPermission(final ApiPermissionRoleType type, final ApiPermission permission,
      final boolean expected) {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // when
    boolean granted = role.hasPermission(permission);

    // then
    assertEquals(expected, granted);
  }

  /**
   * A role permission set cannot be edited after construction.
   *
   * @param type role type
   */
  @ParameterizedTest
  @EnumSource(ApiPermissionRoleType.class)
  void testImmutablePermissions(final ApiPermissionRoleType type) {
    // given
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // when
    Executable mutation = () -> role.getPermissions().add(ApiPermission.ADMIN);

    // then
    assertThrows(UnsupportedOperationException.class, mutation);
  }

  /** A null grant is rejected before a custom role is built. */
  @Test
  void testNullCustomPermissionEntry() {
    // given
    ApiPermissionRole.Builder builder = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM);
    List<ApiPermission> grants = Arrays.asList(ApiPermission.READ_DOCUMENT_ATTRIBUTES, null);

    // when
    Executable assignment = () -> builder.permissions(grants);

    // then
    assertThrows(NullPointerException.class, assignment);
  }

  /** A missing custom permission collection is rejected. */
  @Test
  void testNullCustomPermissions() {
    // given
    ApiPermissionRole.Builder builder = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM);

    // when
    Executable assignment = () -> builder.permissions(null);

    // then
    assertThrows(NullPointerException.class, assignment);
  }

  /** A missing role type is rejected at the builder boundary. */
  @Test
  void testNullRoleType() {
    // given
    ApiPermissionRoleType type = null;

    // when
    Executable creation = () -> ApiPermissionRole.builder(type);

    // then
    assertThrows(NullPointerException.class, creation);
  }

  /**
   * An operation role grants its umbrella and all matching categories without other operations.
   *
   * @param type operation role type
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermissionRoleType.class, names = {"READ", "WRITE", "DELETE"})
  void testOperationRole(final ApiPermissionRoleType type) {
    // given
    ApiPermission umbrella = ApiPermission.valueOf(type.name());
    ApiPermission attributes = ApiPermission.valueOf(type.name() + "_DOCUMENT_ATTRIBUTES");
    final ApiPermission documents = ApiPermission.valueOf(type.name() + "_DOCUMENTS");

    // when
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();

    // then
    assertEquals(type, role.getType());
    assertTrue(role.getPermissions().contains(umbrella));
    assertTrue(role.hasPermission(attributes));
    assertTrue(role.hasPermission(documents));

    assertFalse(role.getPermissions().contains(ApiPermission.ADMIN));
    assertFalse(role.getPermissions().contains(ApiPermission.GOVERN));
  }

  /**
   * Predefined request roles accept omitted or empty generated-client arrays.
   * 
   * @param type role type
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermissionRoleType.class, mode = EnumSource.Mode.EXCLUDE, names = "CUSTOM")
  void testPredefinedRequestRole(final ApiPermissionRoleType type) {
    // given
    Set<ApiPermission> expected = ApiPermissionRole.builder(type).build().getPermissions();
    // when
    ApiPermissionRole omitted = ApiPermissionRole.fromRequest(type.name(), null);
    ApiPermissionRole empty = ApiPermissionRole.fromRequest(type.name(), List.of());
    // then
    assertEquals(type, omitted.getType());
    assertEquals(expected, omitted.getPermissions());
    assertEquals(expected, empty.getPermissions());
  }

  /**
   * Predefined roles cannot be overwritten with custom grants.
   *
   * @param type predefined role type
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermissionRoleType.class, mode = EnumSource.Mode.EXCLUDE, names = "CUSTOM")
  void testPredefinedRoleRejectsPermissionOverride(final ApiPermissionRoleType type) {
    // given
    ApiPermissionRole.Builder builder = ApiPermissionRole.builder(type);

    // when
    Executable override = () -> builder.permissions(List.of(ApiPermission.ADMIN));

    // then
    assertThrows(IllegalStateException.class, override);
  }

  /** Legacy arrays retain exact grants; CUSTOM deduplicates scoped names. */
  @Test
  void testRequestExplicitGrants() {
    // given
    List<String> grants = List.of("READ_DOCUMENT_ATTRIBUTES", "READ_DOCUMENT_ATTRIBUTES");
    // when
    ApiPermissionRole legacy = ApiPermissionRole.fromRequest(null, List.of("WRITE"));
    final ApiPermissionRole custom = ApiPermissionRole.fromRequest("CUSTOM", grants);
    // then
    assertEquals(ApiPermissionRoleType.CUSTOM, legacy.getType());
    assertEquals(Set.of(ApiPermission.WRITE), legacy.getPermissions());
    assertFalse(legacy.hasPermission(ApiPermission.READ_DOCUMENTS));
    assertEquals(Set.of(ApiPermission.READ_DOCUMENT_ATTRIBUTES), custom.getPermissions());
    assertFalse(custom.hasPermission(ApiPermission.READ_DOCUMENTS));
  }

  /** Invalid types, names and combinations never become broader roles. */
  @Test
  void testRequestInvalidGrants() {
    // given
    List<Executable> invalid = List.of(() -> ApiPermissionRole.fromRequest("UNKNOWN", null),
        () -> ApiPermissionRole.fromRequest("READ", List.of("READ_DOCUMENTS")),
        () -> ApiPermissionRole.fromRequest("CUSTOM", null),
        () -> ApiPermissionRole.fromRequest(null, List.of()),
        () -> ApiPermissionRole.fromRequest(null, List.of("UNKNOWN")),
        () -> ApiPermissionRole.fromRequest(null, Arrays.asList((String) null)));
    for (Executable request : invalid) {
      // when
      Executable operation = request;
      // then
      assertThrows(IllegalArgumentException.class, operation);
    }
  }

  /**
   * Ordinary role types and custom umbrella grants do not include user management operations.
   *
   * @param permission user management permission
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class, names = {"READ_USER_MANAGEMENT", "WRITE_USER_MANAGEMENT",
      "DELETE_USER_MANAGEMENT", "GOVERN_USER_MANAGEMENT"})
  void testUserManagementPermissionBoundaries(final ApiPermission permission) {
    // given
    List<ApiPermissionRoleType> ordinaryTypes = List.of(ApiPermissionRoleType.READ,
        ApiPermissionRoleType.WRITE, ApiPermissionRoleType.DELETE, ApiPermissionRoleType.GOVERN);
    ApiPermissionRole custom = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(
        Set.of(ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE, ApiPermission.GOVERN))
        .build();
    ApiPermissionRole admin = ApiPermissionRole.builder(ApiPermissionRoleType.ADMIN).build();
    ApiPermissionRole userManagement =
        ApiPermissionRole.builder(ApiPermissionRoleType.USER_MANAGEMENT).build();

    // when
    List<Boolean> ordinaryAccess = ordinaryTypes.stream()
        .map(type -> ApiPermissionRole.builder(type).build().hasPermission(permission)).toList();
    List<Boolean> typeAccess =
        ordinaryTypes.stream().map(type -> type.hasPermission(permission)).toList();
    boolean customAccess = custom.hasPermission(permission);
    final boolean adminAccess = admin.hasPermission(permission);
    final boolean userManagementAccess = userManagement.hasPermission(permission);
    final boolean adminTypeAccess = ApiPermissionRoleType.ADMIN.hasPermission(permission);
    final boolean userManagementTypeAccess =
        ApiPermissionRoleType.USER_MANAGEMENT.hasPermission(permission);

    // then
    assertTrue(ordinaryAccess.stream().noneMatch(Boolean::booleanValue));
    assertTrue(typeAccess.stream().noneMatch(Boolean::booleanValue));
    assertFalse(customAccess);
    assertTrue(adminAccess);
    assertTrue(userManagementAccess);
    assertTrue(adminTypeAccess);
    assertTrue(userManagementTypeAccess);
  }

  /** User management roles grant only user management operations. */
  @Test
  void testUserManagementRole() {
    // given
    ApiPermissionRoleType type = ApiPermissionRoleType.USER_MANAGEMENT;

    // when
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();
    boolean documentAccess = role.hasPermission(ApiPermission.READ_DOCUMENTS);
    final boolean adminAccess = role.hasPermission(ApiPermission.ADMIN);

    // then
    assertEquals(type, role.getType());
    assertEquals(
        Set.of(ApiPermission.READ_USER_MANAGEMENT, ApiPermission.WRITE_USER_MANAGEMENT,
            ApiPermission.DELETE_USER_MANAGEMENT, ApiPermission.GOVERN_USER_MANAGEMENT),
        role.getPermissions());
    assertFalse(documentAccess);
    assertFalse(adminAccess);
  }

}
