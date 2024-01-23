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
package com.formkiq.aws.services.lambda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 * Api Authorization Token.
 *
 */
public class ApiAuthorization {

  /** {@link Object} Cache. */
  private Map<String, Object> cache = new HashMap<>();
  /** Get Default SiteId. */
  private String defaultSiteId;
  /** {@link ApiPermission} by SiteId. */
  private Map<String, Collection<ApiPermission>> permissionsBySiteId = new HashMap<>();
  /** Authorization Roles. */
  private Collection<String> roles;
  /** {@link String}. */
  private String username;

  /**
   * Add Object to Cache.
   * 
   * @param key {@link String}
   * @param o {@link Object}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization addCacheObject(final String key, final Object o) {
    this.cache.put(key, o);
    return this;
  }

  /**
   * Add Site Permission.
   * 
   * @param siteId {@link String}
   * @param permissions {@link Collection} {@link ApiPermission}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization addPermission(final String siteId,
      final Collection<ApiPermission> permissions) {

    if (this.permissionsBySiteId.containsKey(siteId)) {
      Collection<ApiPermission> c = new HashSet<>(this.permissionsBySiteId.get(siteId));
      c.addAll(permissions);
      this.permissionsBySiteId.put(siteId, c);
    } else {
      this.permissionsBySiteId.put(siteId, permissions);
    }

    return this;
  }

  /**
   * Clear Permissions Map.
   */
  public void clearPermissions() {
    this.permissionsBySiteId = new HashMap<>();
  }

  /**
   * Return Access Summary.
   * 
   * @return {@link String}
   */
  public String getAccessSummary() {
    List<String> sites = getSiteIds();

    String s =
        this.permissionsBySiteId.keySet().stream().sorted()
            .map(e -> e + " ("
                + this.permissionsBySiteId.get(e).stream().map(p -> p.name())
                    .sorted(String::compareTo).collect(Collectors.joining(","))
                + ")")
            .collect(Collectors.joining(", "));

    return !sites.isEmpty() ? "groups: " + s : "no groups";
  }

  /**
   * Get Object from Cache.
   * 
   * @param <T> Type of Object.
   * @param key {@link String}
   * @return {@link Object}
   */
  @SuppressWarnings("unchecked")
  public <T> T getCacheObject(final String key) {
    return (T) this.cache.get(key);
  }

  /**
   * Get {@link ApiPermission}.
   * 
   * @return {@link Collection} {@link ApiPermission}
   */
  public Collection<ApiPermission> getPermissions() {
    return getPermissions(this.defaultSiteId);
  }

  /**
   * Get {@link ApiPermission}.
   * 
   * @param siteId {@link String}
   * @return {@link Collection} {@link ApiPermission}
   */
  public Collection<ApiPermission> getPermissions(final String siteId) {
    return this.permissionsBySiteId.containsKey(siteId) ? this.permissionsBySiteId.get(siteId)
        : Collections.emptyList();
  }

  /**
   * Get Roles.
   * 
   * @return {@link Collection} {@link String}
   */
  public Collection<String> getRoles() {
    return this.roles;
  }

  /**
   * Get Default SiteId.
   * 
   * @return {@link String}
   */
  public String getSiteId() {
    return this.defaultSiteId;
  }

  /**
   * Get SiteIds with Permissions.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getSiteIds() {
    List<String> siteIds = new ArrayList<>(this.permissionsBySiteId.keySet());
    Collections.sort(siteIds);
    return siteIds;
  }

  /**
   * Get Calling Username.
   * 
   * @return {@link String}
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Set Roles.
   * 
   * @param apiRoles {@link Collection} {@link String}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization roles(final Collection<String> apiRoles) {
    this.roles = apiRoles;
    return this;
  }

  /**
   * Set Default SiteId.
   * 
   * @param siteId {@link String}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization siteId(final String siteId) {
    this.defaultSiteId = siteId;
    return this;
  }

  /**
   * Set Calling Username.
   * 
   * @param callingUsername {@link String}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization username(final String callingUsername) {
    this.username = callingUsername;
    return this;
  }
}
