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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/** API permission role types. */
public enum ApiPermissionRoleType {
  /** Admin role. */
  ADMIN,
  /** Delete role. */
  DELETE,
  /** Read role. */
  READ,
  /** Write role. */
  WRITE,
  /** Govern role. */
  GOVERN,
  /** System management role. */
  SYSTEM_MANAGEMENT,
  /** User management role. */
  USER_MANAGEMENT,
  /** Custom role. */
  CUSTOM;

  /**
   * Find the role type matching an underscore-delimited suffix, ignoring case. For example,
   * "default_read" matches READ and "default_user_management" matches USER_MANAGEMENT. The longest
   * matching suffix wins.
   *
   * @param value string ending with a role suffix
   * @return matching role type, or empty for null, blank or unmatched values
   */
  public static Optional<ApiPermissionRoleType> fromSuffix(final String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    String normalized = value.toUpperCase();
    return Arrays.stream(values()).filter(type -> normalized.endsWith("_" + type.name()))
        .max(Comparator.comparingInt(type -> type.name().length()));
  }

  /**
   * Check whether a permission belongs to this role type. ADMIN grants every non-null permission.
   *
   * @param permission permission to check
   * @return true if this is ADMIN or the permission has this role type; false for null permissions
   */
  public boolean hasPermission(final ApiPermission permission) {
    return permission != null && (this == ADMIN || this == permission.getType());
  }
}
