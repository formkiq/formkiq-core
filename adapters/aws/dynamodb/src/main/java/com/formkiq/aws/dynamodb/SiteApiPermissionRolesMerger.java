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

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/** Combines site permission roles without expanding scoped grants. */
public final class SiteApiPermissionRolesMerger
    implements Function<Collection<SiteApiPermissionRole>, Collection<SiteApiPermissionRole>> {

  /**
   * Group roles by normalized site ID and combine duplicate-site grants into CUSTOM roles. Single
   * roles retain their original role type. Null and "default" site IDs identify the same site. The
   * returned collection is immutable and ordered by site ID; input roles are unchanged.
   *
   * @param roles site permission roles
   * @return one permission role per normalized site ID
   * @throws NullPointerException if the collection, an entry or a permission role is null
   */
  @Override
  public Collection<SiteApiPermissionRole> apply(final Collection<SiteApiPermissionRole> roles) {
    Objects.requireNonNull(roles, "roles must not be null");
    Map<String, SiteApiPermissionRole> mergedRoles = new TreeMap<>();
    for (SiteApiPermissionRole siteRole : roles) {
      Objects.requireNonNull(siteRole, "siteRole must not be null");
      Objects.requireNonNull(siteRole.role(), "role must not be null");
      String siteId = SiteIdKeyGenerator.getSiteIdName(siteRole.siteId());
      SiteApiPermissionRole normalized = new SiteApiPermissionRole(siteId, siteRole.role());
      mergedRoles.merge(siteId, normalized, this::merge);
    }
    return List.copyOf(mergedRoles.values());
  }

  /**
   * Combine the grants of two roles for the same normalized site.
   *
   * @param existing existing site role
   * @param incoming additional site role
   * @return site role with the combined CUSTOM grants
   */
  private SiteApiPermissionRole merge(final SiteApiPermissionRole existing,
      final SiteApiPermissionRole incoming) {
    EnumSet<ApiPermission> permissions = EnumSet.noneOf(ApiPermission.class);
    permissions.addAll(existing.role().getPermissions());
    permissions.addAll(incoming.role().getPermissions());
    ApiPermissionRole role =
        ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM).permissions(permissions).build();
    return new SiteApiPermissionRole(existing.siteId(), role);
  }
}
