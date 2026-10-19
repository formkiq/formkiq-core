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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests API permission role metadata and FormKiQ client tag groups. */
class ApiPermissionTest {

  /**
   * Client tag metadata preserves labels, including punctuation and capitalization.
   *
   * @param permissionGroup client group
   * @param expectedTag exact client operation tag
   */
  @ParameterizedTest
  @CsvSource({"USER_ACTIVITIES, User Activities", "ATTRIBUTES, Attributes", "DOCUMENTS, Documents",
      "DOCUMENT_ATTRIBUTES, Document Attributes", "DOCUMENT_ACTIONS, Document Actions",
      "E_SIGNATURE, E-Signature", "RULESETS, Rulesets", "EXAMINE_OBJECTS, Examine Objects",
      "REMINDER_POLICY, Reminder Policy", "RETENTION_POLICY, Retention Policy"})
  void testClientTagLabels(final ApiPermissionGroup permissionGroup, final String expectedTag) {
    // given
    ApiPermissionGroup group = permissionGroup;

    // when
    String tag = group.getTag();

    // then
    assertEquals(expectedTag, tag);
  }

  /**
   * Scoped GOVERN permissions use the client tags of handlers with governance checks.
   *
   * @param permission scoped govern permission
   * @param group client tag group
   */
  @ParameterizedTest
  @CsvSource({"GOVERN_ACCESS_CONTROL, ACCESS_CONTROL", "GOVERN_DOCUMENT_FOLDERS, DOCUMENT_FOLDERS",
      "GOVERN_DOCUMENT_VERSIONS, DOCUMENT_VERSIONS", "GOVERN_DOCUMENTS, DOCUMENTS",
      "GOVERN_USER_ACTIVITIES, USER_ACTIVITIES"})
  void testGovernPermissionMetadata(final ApiPermission permission,
      final ApiPermissionGroup group) {
    // given
    ApiPermission governPermission = permission;

    // when
    ApiPermissionRoleType actualType = governPermission.getType();
    ApiPermissionGroup actualGroup = governPermission.getGroup();

    // then
    assertEquals(ApiPermissionRoleType.GOVERN, actualType);
    assertEquals(group, actualGroup);
  }

  /**
   * Every permission has a description suitable for display in the catalog.
   *
   * @param permission API permission
   */
  @ParameterizedTest
  @EnumSource(ApiPermission.class)
  void testPermissionDescription(final ApiPermission permission) {
    // given
    ApiPermission apiPermission = permission;

    // when
    String description = apiPermission.getDescription();

    // then
    assertFalse(description.isBlank());
  }

  /**
   * Scoped operation permissions have matching role types and client tag groups.
   *
   * @param permission scoped operation permission
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class, mode = EnumSource.Mode.EXCLUDE,
      names = {"ADMIN", "READ", "WRITE", "DELETE", "GOVERN", "READ_USER_MANAGEMENT",
          "WRITE_USER_MANAGEMENT", "DELETE_USER_MANAGEMENT", "GOVERN_USER_MANAGEMENT",
          "READ_SYSTEM_MANAGEMENT", "WRITE_SYSTEM_MANAGEMENT", "DELETE_SYSTEM_MANAGEMENT"})
  void testScopedPermissionMetadata(final ApiPermission permission) {
    // given
    int delimiter = permission.name().indexOf('_');
    ApiPermissionRoleType expectedType =
        ApiPermissionRoleType.valueOf(permission.name().substring(0, delimiter));
    ApiPermissionGroup expectedGroup =
        ApiPermissionGroup.valueOf(permission.name().substring(delimiter + 1));

    // when
    ApiPermissionGroup actualGroup = permission.getGroup();
    ApiPermissionRoleType actualType = permission.getType();

    // then
    assertEquals(expectedGroup, actualGroup);
    assertEquals(expectedType, actualType);
  }

  /**
   * System management operation permissions have a dedicated role type and client tag group.
   *
   * @param permission system management permission
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class,
      names = {"READ_SYSTEM_MANAGEMENT", "WRITE_SYSTEM_MANAGEMENT", "DELETE_SYSTEM_MANAGEMENT"})
  void testSystemManagementPermissionMetadata(final ApiPermission permission) {
    // given
    ApiPermission systemPermission = permission;

    // when
    ApiPermissionRoleType type = systemPermission.getType();
    ApiPermissionGroup group = systemPermission.getGroup();

    // then
    assertEquals(ApiPermissionRoleType.SYSTEM_MANAGEMENT, type);
    assertEquals(ApiPermissionGroup.SYSTEM_MANAGEMENT, group);
  }

  /**
   * Top-level permissions have their corresponding role type and no client tag group.
   *
   * @param permission top-level permission
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class, names = {"ADMIN", "DELETE", "READ", "WRITE", "GOVERN"})
  void testTopLevelPermissions(final ApiPermission permission) {
    // given
    ApiPermission topLevel = permission;

    // when
    ApiPermissionGroup permissionGroup = topLevel.getGroup();
    ApiPermissionRoleType permissionType = topLevel.getType();

    // then
    assertNull(permissionGroup);
    assertEquals(ApiPermissionRoleType.valueOf(permission.name()), permissionType);
  }

  /** Every permission has a unique non-negative bitmap index. */
  @Test
  void testUniqueBitmapIndices() {
    // given
    ApiPermission[] permissions = ApiPermission.values();

    // when
    Set<Integer> indices =
        Arrays.stream(permissions).map(ApiPermission::getBitmapIndex).collect(Collectors.toSet());

    // then
    assertEquals(permissions.length, indices.size());
    assertTrue(indices.stream().allMatch(index -> index >= 0));
  }

  /**
   * User management permissions have a dedicated role type regardless of their operation prefix.
   *
   * @param permission user management permission
   */
  @ParameterizedTest
  @EnumSource(value = ApiPermission.class, names = {"READ_USER_MANAGEMENT", "WRITE_USER_MANAGEMENT",
      "DELETE_USER_MANAGEMENT", "GOVERN_USER_MANAGEMENT"})
  void testUserManagementPermissionMetadata(final ApiPermission permission) {
    // given
    ApiPermission userPermission = permission;

    // when
    ApiPermissionRoleType type = userPermission.getType();
    ApiPermissionGroup group = userPermission.getGroup();

    // then
    assertEquals(ApiPermissionRoleType.USER_MANAGEMENT, type);
    assertEquals(ApiPermissionGroup.USER_MANAGEMENT, group);
  }
}
