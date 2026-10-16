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
package com.formkiq.stacks.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.ApiPermission;
import com.formkiq.client.model.ApiPermissionGroup;
import com.formkiq.client.model.ApiPermissionRoleType;
import com.formkiq.client.model.PermissionCatalogGroup;
import com.formkiq.client.model.PermissionCatalogPermission;
import com.formkiq.client.model.PermissionCatalogRoleType;
import com.formkiq.testutils.api.SetBearerPermissionMap;
import com.formkiq.testutils.api.systemmanagement.GetPermissionCatalogRequestBuilder;
import org.junit.jupiter.api.Test;

/** API tests for GET /permissions/catalog. */
public class PermissionCatalogRequestTest extends AbstractApiClientRequestTest {

  private Set<ApiPermission> expectedPermissions(final String... prefixes) {
    return Arrays.stream(ApiPermission.values())
        .filter(permission -> !permission.getValue().endsWith("_SYSTEM_MANAGEMENT")
            && !permission.getValue().endsWith("_USER_MANAGEMENT"))
        .filter(permission -> Arrays.stream(prefixes).anyMatch(permission.getValue()::startsWith))
        .collect(Collectors.toSet());
  }

  private Set<ApiPermission> permissions(final PermissionCatalogRoleType role) {
    return role.getGroups().stream().flatMap(group -> group.getPermissions().stream())
        .map(PermissionCatalogPermission::getName).collect(Collectors.toSet());
  }

  /** The catalog contains effective role grants, grouped with client tag labels. */
  @Test
  public void testGetPermissionCatalog() throws ApiException {
    // given
    setBearerToken("default");
    var request = new GetPermissionCatalogRequestBuilder();

    // when
    var response = request.submitOk(client, null).response();

    // then
    var roles = response.getRoleTypes().stream()
        .collect(Collectors.toMap(PermissionCatalogRoleType::getName, role -> role));
    assertEquals(EnumSet.allOf(ApiPermissionRoleType.class), roles.keySet());
    assertEquals(EnumSet.allOf(ApiPermission.class),
        permissions(roles.get(ApiPermissionRoleType.ADMIN)));
    assertEquals(EnumSet.allOf(ApiPermission.class),
        permissions(roles.get(ApiPermissionRoleType.CUSTOM)));
    assertEquals(expectedPermissions("READ_"), permissions(roles.get(ApiPermissionRoleType.READ)));
    assertEquals(expectedPermissions("DELETE_"),
        permissions(roles.get(ApiPermissionRoleType.DELETE)));
    assertEquals(expectedPermissions("READ_", "WRITE_", "DELETE_"),
        permissions(roles.get(ApiPermissionRoleType.WRITE)));
    assertEquals(expectedPermissions("READ_", "WRITE_", "DELETE_", "GOVERN_"),
        permissions(roles.get(ApiPermissionRoleType.GOVERN)));
    assertEquals(
        Set.of(ApiPermission.READ_SYSTEM_MANAGEMENT, ApiPermission.WRITE_SYSTEM_MANAGEMENT,
            ApiPermission.DELETE_SYSTEM_MANAGEMENT),
        permissions(roles.get(ApiPermissionRoleType.SYSTEM_MANAGEMENT)));
    assertEquals(
        Set.of(ApiPermission.READ_USER_MANAGEMENT, ApiPermission.WRITE_USER_MANAGEMENT,
            ApiPermission.DELETE_USER_MANAGEMENT, ApiPermission.GOVERN_USER_MANAGEMENT),
        permissions(roles.get(ApiPermissionRoleType.USER_MANAGEMENT)));

    var customGroups = roles.get(ApiPermissionRoleType.CUSTOM).getGroups().stream()
        .collect(Collectors.toMap(PermissionCatalogGroup::getName, group -> group));
    assertEquals(EnumSet.allOf(ApiPermissionGroup.class), customGroups.keySet());
    assertEquals("Document Attributes",
        customGroups.get(ApiPermissionGroup.DOCUMENT_ATTRIBUTES).getLabel());
    assertEquals("E-Signature", customGroups.get(ApiPermissionGroup.E_SIGNATURE).getLabel());
    var documentAttribute =
        customGroups.get(ApiPermissionGroup.DOCUMENT_ATTRIBUTES).getPermissions().stream()
            .filter(permission -> permission.getName() == ApiPermission.READ_DOCUMENT_ATTRIBUTES)
            .findFirst().orElseThrow();
    assertEquals("Read attributes assigned to documents.", documentAttribute.getDescription());
    for (var role : roles.values()) {
      assertEquals(role.getGroups().size(),
          role.getGroups().stream().map(PermissionCatalogGroup::getName).distinct().count());
      for (var group : role.getGroups()) {
        assertFalse(group.getPermissions().isEmpty());
        assertFalse(group.getLabel().isBlank());
        for (var permission : group.getPermissions()) {
          assertFalse(permission.getDescription().isBlank());
        }
        assertTrue(group.getPermissions().stream().allMatch(permission -> permission.getName()
            .getValue().endsWith("_" + group.getName().getValue())));
      }
    }
  }

  /** A scoped caller can fetch the catalog without selecting either of its sites. */
  @Test
  public void testGetPermissionCatalogWithMultipleSites() throws ApiException {
    // given
    new SetBearerPermissionMap().apply(client, List.of("first", "second"),
        Map.of("first", List.of("READ_DOCUMENT_ATTRIBUTES"), "second", List.of("READ_DOCUMENTS")),
        Map.of());
    var request = new GetPermissionCatalogRequestBuilder();

    // when
    var response = request.submitOk(client, null).response();

    // then
    assertEquals(ApiPermissionRoleType.values().length, response.getRoleTypes().size());
    var custom = response.getRoleTypes().stream()
        .filter(role -> role.getName() == ApiPermissionRoleType.CUSTOM).findFirst().orElseThrow();
    assertEquals(EnumSet.allOf(ApiPermission.class), permissions(custom));
  }

  /** Authentication is sufficient even when the caller has no site grants. */
  @Test
  public void testGetPermissionCatalogWithoutSitePermissions() throws ApiException {
    // given
    new SetBearerPermissionMap().apply(client, List.of(), Map.of(), Map.of());
    var request = new GetPermissionCatalogRequestBuilder();

    // when
    var response = request.submitOk(client, null).response();

    // then
    assertEquals(EnumSet.allOf(ApiPermissionRoleType.class), response.getRoleTypes().stream()
        .map(PermissionCatalogRoleType::getName).collect(Collectors.toSet()));
  }
}
