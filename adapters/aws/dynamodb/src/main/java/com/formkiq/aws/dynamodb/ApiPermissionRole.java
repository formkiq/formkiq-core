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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** An immutable API permission role with its granted permissions. */
public final class ApiPermissionRole {

  /** Builds predefined roles or a custom set of permissions. */
  public static final class Builder {
    /** Selected permission role type. */
    private final ApiPermissionRoleType type;
    /** Permissions granted by the role. */
    private EnumSet<ApiPermission> permissions;

    /**
     * Constructor.
     *
     * @param roleType permission role type
     */
    private Builder(final ApiPermissionRoleType roleType) {
      this.type = Objects.requireNonNull(roleType, "roleType must not be null");
      this.permissions = switch (this.type) {
        case ADMIN -> EnumSet.of(ApiPermission.ADMIN, ApiPermission.GOVERN, ApiPermission.READ,
            ApiPermission.DELETE, ApiPermission.WRITE);
        case GOVERN -> EnumSet.of(ApiPermission.GOVERN, ApiPermission.READ, ApiPermission.DELETE,
            ApiPermission.WRITE);
        case READ -> EnumSet.of(ApiPermission.READ);
        case WRITE -> EnumSet.of(ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE);
        case DELETE -> EnumSet.of(ApiPermission.DELETE);
        case SYSTEM_MANAGEMENT -> EnumSet.of(ApiPermission.READ_SYSTEM_MANAGEMENT,
            ApiPermission.DELETE_SYSTEM_MANAGEMENT, ApiPermission.WRITE_SYSTEM_MANAGEMENT);
        case USER_MANAGEMENT ->
          EnumSet.of(ApiPermission.READ_USER_MANAGEMENT, ApiPermission.WRITE_USER_MANAGEMENT,
              ApiPermission.DELETE_USER_MANAGEMENT, ApiPermission.GOVERN_USER_MANAGEMENT);
        case CUSTOM -> EnumSet.noneOf(ApiPermission.class);
      };
    }

    /**
     * Build an immutable permission role.
     *
     * @return permission role
     */
    public ApiPermissionRole build() {
      return new ApiPermissionRole(this);
    }

    /**
     * Set the permissions of a CUSTOM role, removing duplicate grants. Predefined roles have a
     * fixed permission set.
     *
     * @param rolePermissions custom permissions
     * @return this builder
     * @throws IllegalStateException if the role type is not CUSTOM
     * @throws NullPointerException if the collection or a permission is null
     */
    public Builder permissions(final Collection<ApiPermission> rolePermissions) {
      if (this.type != ApiPermissionRoleType.CUSTOM) {
        throw new IllegalStateException("Permissions can only be set for a CUSTOM role");
      }
      Objects.requireNonNull(rolePermissions, "rolePermissions must not be null");
      EnumSet<ApiPermission> customPermissions = EnumSet.noneOf(ApiPermission.class);
      customPermissions.addAll(rolePermissions);
      this.permissions = customPermissions;
      return this;
    }
  }

  /**
   * Create a builder and populate its umbrella permissions for the selected role type. ADMIN
   * includes every operation category. GOVERN includes govern, read, write and delete grants. WRITE
   * includes read, write and delete grants. READ and DELETE include their matching grant. CUSTOM
   * starts with no permissions. SYSTEM_MANAGEMENT includes its read, write and delete permissions.
   * USER_MANAGEMENT includes only user management permissions. Other non-administrative predefined
   * roles do not include these management permissions.
   *
   * @param roleType permission role type
   * @return new role builder
   * @throws NullPointerException if the role type is null
   */
  public static Builder builder(final ApiPermissionRoleType roleType) {
    return new Builder(roleType);
  }

  /** Permission role type. */
  private final ApiPermissionRoleType type;

  /** Immutable permissions granted by the role. */
  private final Set<ApiPermission> permissions;

  /**
   * Constructor.
   *
   * @param builder role builder
   */
  private ApiPermissionRole(final Builder builder) {
    this.type = builder.type;
    this.permissions = Collections.unmodifiableSet(EnumSet.copyOf(builder.permissions));
  }

  /**
   * Get the immutable set of granted permissions.
   *
   * @return role permissions
   */
  public Set<ApiPermission> getPermissions() {
    return this.permissions;
  }

  /**
   * Get the role type.
   *
   * @return permission role type
   */
  public ApiPermissionRoleType getType() {
    return this.type;
  }

  /**
   * Check whether the permission is explicitly granted, belongs to this role type or is covered by
   * a granted umbrella permission. Scoped grants only match the exact permission.
   *
   * @param permission permission to check
   * @return true if the permission is granted, or false if absent or null
   */
  public boolean hasPermission(final ApiPermission permission) {
    return permission != null
        && (this.permissions.contains(permission) || this.type.hasPermission(permission)
            || this.permissions.stream().filter(grant -> grant.getGroup() == null)
                .anyMatch(grant -> grant.getType().hasPermission(permission)));
  }
}
