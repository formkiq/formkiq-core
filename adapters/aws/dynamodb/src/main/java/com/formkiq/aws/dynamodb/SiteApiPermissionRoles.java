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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Immutable API permission roles across multiple sites.
 *
 * @param roles site-specific permission roles; null and "default" identify the default site
 */
public record SiteApiPermissionRoles(Collection<SiteApiPermissionRole> roles) {

  /** JSON serializer for the site-to-bitmap map. */
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  /**
   * Copy the site roles into an immutable collection.
   *
   * @param roles site-specific permission roles
   * @throws NullPointerException if the collection or an entry is null
   */
  public SiteApiPermissionRoles {
    roles = List.copyOf(roles);
  }

  /**
   * Import a site-to-bitmap map produced by {@link #exportPermissions()}. Each site is restored
   * with a CUSTOM role containing exactly the encoded grants. Predefined role types are not
   * retained or expanded during import.
   *
   * @param permissions versioned permission bitmaps keyed by site ID
   * @return immutable site permission roles
   * @throws IllegalArgumentException if any bitmap version or payload is invalid
   * @throws NullPointerException if the map, a key or a value is null
   */
  public static SiteApiPermissionRoles importPermissions(final Map<String, String> permissions) {
    Map<String, String> encoded = new TreeMap<>(Map.copyOf(permissions));
    Collection<SiteApiPermissionRole> imported = new ArrayList<>();
    encoded.forEach((siteId, bitmap) -> {
      ApiPermissionRole role = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
          .permissions(ApiPermissionBitmapV1.decode(bitmap)).build();
      imported.add(new SiteApiPermissionRole(siteId, role));
    });
    return new SiteApiPermissionRoles(imported);
  }

  /**
   * Import a JSON object produced by {@link #exportPermissionsAsString()}. The object must contain
   * unique site IDs and string bitmap values. Every bitmap is validated by the map import.
   *
   * @param permissions JSON site-to-bitmap map
   * @return immutable site permission roles with exact CUSTOM grants
   * @throws IllegalArgumentException if the JSON or any bitmap is invalid
   * @throws NullPointerException if the JSON string is null
   */
  public static SiteApiPermissionRoles importPermissions(final String permissions) {
    Objects.requireNonNull(permissions, "permissions must not be null");
    Map<String, String> encoded = new TreeMap<>();
    try (JsonReader reader = new JsonReader(new StringReader(permissions))) {
      reader.setStrictness(Strictness.STRICT);
      reader.beginObject();
      while (reader.hasNext()) {
        String siteId = reader.nextName();
        if (reader.peek() != JsonToken.STRING) {
          throw new IllegalArgumentException("Permission bitmap values must be strings");
        }
        if (encoded.putIfAbsent(siteId, reader.nextString()) != null) {
          throw new IllegalArgumentException("Duplicate site ID in permission bitmap JSON");
        }
      }
      reader.endObject();
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw new IllegalArgumentException("Unexpected content after permission bitmap JSON");
      }
      return importPermissions(encoded);
    } catch (IOException | IllegalStateException e) {
      throw new IllegalArgumentException("Invalid permission bitmap JSON", e);
    }
  }

  /**
   * Export exact grants as an immutable site-to-bitmap map, ordered by site ID. Values use
   * {@code v1:<base64url>} with a fixed permission catalog and no Base64 padding. Multiple roles
   * for the same site combine their grants. Null site IDs are exported as "default"; role types are
   * not serialized.
   *
   * @return versioned permission bitmaps keyed by site ID
   * @throws IllegalArgumentException if a permission is absent from the current bitmap catalog
   * @throws NullPointerException if a site's permission role is null
   */
  public Map<String, String> exportPermissions() {
    Map<String, Set<ApiPermission>> permissionsBySite = new TreeMap<>();
    this.roles
        .forEach(siteRole -> permissionsBySite
            .computeIfAbsent(SiteIdKeyGenerator.getSiteIdName(siteRole.siteId()),
                siteId -> EnumSet.noneOf(ApiPermission.class))
            .addAll(
                Objects.requireNonNull(siteRole.role(), "role must not be null").getPermissions()));
    Map<String, String> exported = new TreeMap<>();
    permissionsBySite.forEach(
        (siteId, permissions) -> exported.put(siteId, ApiPermissionBitmapV1.encode(permissions)));
    return Collections.unmodifiableMap(exported);
  }

  /**
   * Export the complete site-to-bitmap map as compact JSON, ordered by site ID. This has the same
   * bitmap format and exact-grant semantics as {@link #exportPermissions()}.
   *
   * @return JSON site-to-bitmap map
   * @throws IllegalArgumentException if a permission is absent from the current bitmap catalog
   * @throws NullPointerException if a site's permission role is null
   */
  public String exportPermissionsAsString() {
    return GSON.toJson(exportPermissions());
  }

  /**
   * Check whether any role for the requested site grants a permission. A null site ID and "default"
   * both identify the default site.
   *
   * @param siteId site identifier
   * @param permission permission to check
   * @return true if a role for the site grants the permission, or false if absent or null
   */
  public boolean hasPermission(final String siteId, final ApiPermission permission) {
    String siteName = SiteIdKeyGenerator.getSiteIdName(siteId);
    return this.roles.stream()
        .filter(siteRole -> siteName.equals(SiteIdKeyGenerator.getSiteIdName(siteRole.siteId())))
        .anyMatch(siteRole -> siteRole.role().hasPermission(permission));
  }
}

