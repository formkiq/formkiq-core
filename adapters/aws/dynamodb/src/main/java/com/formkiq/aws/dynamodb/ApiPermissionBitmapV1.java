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
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Version 1 permission bitmap codec using permanent permission indices and a fixed index range. */
final class ApiPermissionBitmapV1 {

  /** Version 1 wire-format prefix. */
  private static final String VERSION_PREFIX = "v1:";
  /** Bits per encoded byte. */
  private static final int BITS_PER_BYTE = Byte.SIZE;
  /** Highest permission index supported by version 1. Never increase for this version. */
  private static final int VERSION_ONE_MAX_INDEX = 66;
  /** Version 1 permissions keyed by their permanent bitmap indices. */
  private static final Map<Integer, ApiPermission> VERSION_ONE_PERMISSIONS =
      Arrays.stream(ApiPermission.values())
          .filter(permission -> permission.getBitmapIndex() >= 0
              && permission.getBitmapIndex() <= VERSION_ONE_MAX_INDEX)
          .collect(
              Collectors.toUnmodifiableMap(ApiPermission::getBitmapIndex, Function.identity()));
  /** Fixed number of bytes in a version 1 bitmap. */
  private static final int VERSION_ONE_BYTES =
      (VERSION_ONE_MAX_INDEX + BITS_PER_BYTE) / BITS_PER_BYTE;

  /**
   * Decode a versioned bitmap, rejecting malformed encodings and undefined bits.
   *
   * @param encoded versioned Base64URL bitmap
   * @return exact granted permissions
   * @throws IllegalArgumentException if the version, encoding, length or bits are invalid
   * @throws NullPointerException if the encoded value is null
   */
  static Set<ApiPermission> decode(final String encoded) {
    Objects.requireNonNull(encoded, "encoded permissions must not be null");
    if (!encoded.startsWith(VERSION_PREFIX)) {
      throw new IllegalArgumentException("Unsupported permission bitmap version");
    }
    String payload = encoded.substring(VERSION_PREFIX.length());
    byte[] bitmap = Base64.getUrlDecoder().decode(payload);
    if (bitmap.length != VERSION_ONE_BYTES
        || !Base64.getUrlEncoder().withoutPadding().encodeToString(bitmap).equals(payload)) {
      throw new IllegalArgumentException("Invalid permission bitmap encoding or length");
    }
    EnumSet<ApiPermission> permissions = EnumSet.noneOf(ApiPermission.class);
    for (int index = 0; index < bitmap.length * BITS_PER_BYTE; index++) {
      if ((bitmap[index / BITS_PER_BYTE] & (1 << (index % BITS_PER_BYTE))) != 0) {
        ApiPermission permission = VERSION_ONE_PERMISSIONS.get(index);
        if (permission == null) {
          throw new IllegalArgumentException("Undefined permission bitmap bit");
        }
        permissions.add(permission);
      }
    }
    return permissions;
  }

  /**
   * Encode the exact permission set as a version 1 bitmap.
   *
   * @param permissions granted permissions
   * @return versioned Base64URL bitmap without padding
   * @throws IllegalArgumentException if a permission is absent from the version 1 catalog
   * @throws NullPointerException if the set or a grant is null
   */
  static String encode(final Set<ApiPermission> permissions) {
    Objects.requireNonNull(permissions, "permissions must not be null");
    byte[] bitmap = new byte[VERSION_ONE_BYTES];
    for (ApiPermission permission : permissions) {
      Objects.requireNonNull(permission, "permission must not be null");
      int index = permission.getBitmapIndex();
      if (VERSION_ONE_PERMISSIONS.get(index) != permission) {
        throw new IllegalArgumentException("Permission is absent from the version 1 catalog");
      }
      bitmap[index / BITS_PER_BYTE] |= (byte) (1 << (index % BITS_PER_BYTE));
    }
    return VERSION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bitmap);
  }

  /** Utility class constructor. */
  private ApiPermissionBitmapV1() {}
}

