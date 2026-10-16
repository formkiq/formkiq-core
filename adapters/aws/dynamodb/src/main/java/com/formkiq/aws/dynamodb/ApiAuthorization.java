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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.attributes.AttributeAccessApproval;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * 
 * Api Authorization Token.
 *
 */
public class ApiAuthorization {

  /**
   * {@link ThreadLocal}.
   */
  private static final ThreadLocal<ApiAuthorization> CURRENT_AUTHORIZATION = new ThreadLocal<>();

  /**
   * Get Current User.
   *
   * @return ApiAuthorization
   */
  public static ApiAuthorization getAuthorization() {
    ApiAuthorization authorization = CURRENT_AUTHORIZATION.get();
    if (authorization == null) {
      authorization = new ApiAuthorization().username("System");
    }
    return authorization;
  }

  /**
   * Login user.
   *
   * @param authorization {@link ApiAuthorization}
   */
  public static void login(final ApiAuthorization authorization) {
    CURRENT_AUTHORIZATION.set(authorization);
  }

  /**
   * Logout current User.
   */
  public static void logout() {
    CURRENT_AUTHORIZATION.remove();
  }

  /** Request-scoped attribute access approval. */
  private AttributeAccessApproval attributeAccessApproval;

  /**
   * {@link Object} Cache.
   */
  private final Map<String, Object> cache = new HashMap<>();
  /** Custom email claim. */
  private String customEmail;
  /**
   * Get Default SiteId.
   */
  private String defaultSiteId;

  /** {@link ApiPermissionRole} by SiteId. */
  private final Map<String, ApiPermissionRole> permissionRoleMap = new HashMap<>();

  /**
   * Authorization Roles.
   */
  private Collection<String> roles;

  /**
   * custom user claims.
   */
  private Map<String, Object> userClaims;
  /**
   * role to site mapping from sitesClaims.
   */
  private Map<String, String> roleSiteMap;

  /**
   * Authorization Saml Groups.
   */
  private Collection<String> samlGroups;

  /**
   * {@link String}.
   */
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
   * Add {@link SiteApiPermissionRole} Permission.
   * 
   * @param role {@link SiteApiPermissionRole}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization addPermission(final SiteApiPermissionRole role) {
    permissionRoleMap.put(role.siteId(), role.role());
    return this;
  }

  /**
   * Sets request-scoped attribute access approval.
   *
   * @param approval {@link AttributeAccessApproval}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization attributeAccessApproval(final AttributeAccessApproval approval) {
    this.attributeAccessApproval = approval;
    return this;
  }

  /**
   * Set the custom email claim.
   *
   * @param email {@link String}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization customEmail(final String email) {
    this.customEmail = email;
    return this;
  }

  /**
   * Return Access Summary.
   * 
   * @return {@link String}
   */
  public String getAccessSummary() {
    List<String> sites = getSiteIds();

    String s = this.permissionRoleMap.keySet().stream().sorted()
        .map(e -> e + " ("
            + this.permissionRoleMap.get(e).getPermissions().stream().map(Enum::name)
                .sorted(String::compareTo).collect(Collectors.joining(","))
            + ")")
        .collect(Collectors.joining(", "));

    return !sites.isEmpty() ? "groups: " + s : "no groups";
  }

  /**
   * All permissions across ALL sites.
   * 
   * @return {@link Collection} {@link ApiPermission}
   */
  public Collection<ApiPermission> getAllPermissions() {
    return this.permissionRoleMap.keySet().stream()
        .flatMap(siteId -> getPermissions(siteId).stream()).collect(Collectors.toSet());
  }

  /**
   * Gets the request-scoped attribute access approval using the supplied default access.
   *
   * @param defaultAccess {@link AttributeValidationAccess}
   * @return {@link AttributeAccessApproval}
   */
  public AttributeAccessApproval getAttributeAccessApproval(
      final AttributeValidationAccess defaultAccess) {
    return this.attributeAccessApproval != null
        ? this.attributeAccessApproval.withDefaultAccess(defaultAccess)
        : new AttributeAccessApproval(defaultAccess);
  }

  /**
   * Get Object from Cache.
   * 
   * @param <T> Type of Object.
   * @param key {@link String}
   * @return {@link Object}
   */
  public <T> T getCacheObject(final String key) {
    return (T) this.cache.get(key);
  }

  /**
   * Get the custom email claim.
   *
   * @return {@link String}
   */
  public String getCustomEmail() {
    return this.customEmail;
  }

  /**
   * Get {@link ApiPermission}.
   * 
   * @param siteId {@link String}
   * @return {@link Collection} {@link ApiPermission}
   */
  public Collection<ApiPermission> getPermissions(final String siteId) {

    var permissionRole = this.permissionRoleMap.get(siteId);
    Collection<ApiPermission> permissions = Collections.emptyList();

    if (permissionRole != null) {
      permissions = permissionRole.getPermissions();
    } else if (hasAdminPermission()) {
      permissions = Arrays.stream(ApiPermission.values())
          .filter(permission -> permission.getGroup() == null).sorted().toList();
    }

    return permissions;
  }

  /**
   * Get roleSiteMap.
   *
   * @return {@link Map}
   */
  public Map<String, String> getRoleSiteMap() {
    return this.roleSiteMap;
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
   * Get Saml Groups.
   *
   * @return {@link Collection} {@link String}
   */
  public Collection<String> getSamlGroups() {
    return this.samlGroups;
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
    return permissionRoleMap.keySet().stream().sorted().toList();
  }

  /**
   * Get custom user claims.
   *
   * @return {@link Map}
   */
  public Map<String, Object> getUserClaims() {
    return this.userClaims;
  }

  /**
   * Get Calling Username.
   * 
   * @return {@link String}
   */
  public String getUsername() {
    return !isEmpty(this.username) ? this.username : "System";
  }

  private boolean hasAdminPermission() {
    return this.permissionRoleMap.values().stream()
        .anyMatch(p -> p.hasPermission(ApiPermission.ADMIN));
  }

  /**
   * Returns whether the user has Admin permission.
   *
   * @param siteId {@link String}
   * @return boolean
   */
  public boolean isAdmin(final String siteId) {
    Collection<ApiPermission> permissions = getPermissions(siteId);
    return permissions.contains(ApiPermission.ADMIN);
  }

  /**
   * Returns whether the user has Admin or Govern permission.
   * 
   * @param siteId {@link String}
   * @return boolean
   */
  public boolean isAdminOrGovern(final String siteId) {
    Collection<ApiPermission> permissions = getPermissions(siteId);
    return permissions.contains(ApiPermission.ADMIN) || permissions.contains(ApiPermission.GOVERN);
  }

  /**
   * Replace all site grants, for example when restricting a request to a shared document.
   *
   * @param permissions replacement site roles
   * @return this authorization
   */
  public ApiAuthorization replacePermissions(final SiteApiPermissionRoles permissions) {
    permissionRoleMap.clear();
    new SiteApiPermissionRolesMerger().apply(permissions.roles()).forEach(this::addPermission);
    return this;
  }

  /**
   * Set roleSiteMap.
   *
   * @param map {@link Map}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization roleSiteMap(final Map<String, String> map) {
    this.roleSiteMap = map;
    return this;
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
   * Set Saml Groups.
   *
   * @param apiSamlGroups {@link Collection} {@link String}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization samlGroups(final Collection<String> apiSamlGroups) {
    this.samlGroups = apiSamlGroups;
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
   * Set JWT custom claims.
   *
   * @param claims {@link Map}
   * @return {@link ApiAuthorization}
   */
  public ApiAuthorization userClaims(final Map<String, Object> claims) {
    this.userClaims = claims;
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
