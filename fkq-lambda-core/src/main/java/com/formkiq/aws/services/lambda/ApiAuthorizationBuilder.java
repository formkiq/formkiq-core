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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.strings.Strings.isEmpty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.ApiPermissionRole;
import com.formkiq.aws.dynamodb.ApiPermissionRoleType;
import com.formkiq.aws.dynamodb.SiteApiPermissionRole;
import com.formkiq.aws.dynamodb.SiteApiPermissionRoles;
import com.formkiq.aws.dynamodb.SiteApiPermissionRolesMerger;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * Api Authorization Token.
 *
 */
public class ApiAuthorizationBuilder {

  /** Standard and Cognito-managed claims to exclude. */
  private static final Set<String> EXCLUDED_CLAIMS = Set.of("sub", "cognito:groups", "iss",
      "client_id", "origin_jti", "event_id", "token_use", "scope", "sitePermissions", "auth_time",
      "exp", "iat", "jti", "username", "sitesClaims", "cognito:username");
  /** Cognito Admin Group Name. */
  private static final String COGNITO_ADMIN_GROUP = "Admins";
  /** The suffix for the 'readonly' Cognito group. */
  public static final String COGNITO_READ_SUFFIX = "_read";
  /** Global Site Urls. */
  private static final Collection<String> GLOBAL_SITE_URLS =
      Set.of("/changePassword", "/confirmRegistration", "/forgotPassword", "/forgotPasswordConfirm",
          "/login", "/login/refresh", "/respondToAuthChallenge", "/s/{slug}");

  private static Map<String, Object> getAuthorizerClaims(final Map<String, Object> authorizer) {
    Map<String, Object> claims = Collections.emptyMap();

    if (authorizer != null && authorizer.containsKey("claims")) {
      claims = (Map<String, Object>) authorizer.get("claims");
    }

    if (authorizer != null && authorizer.containsKey("apiKeyClaims")) {
      claims = (Map<String, Object>) authorizer.get("apiKeyClaims");
    }

    return claims;
  }

  private static ApiPermission toApiPermission(final String val) {
    try {
      return ApiPermission.valueOf(val.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** {@link List} {@link ApiAuthorizationInterceptor}. */
  private List<ApiAuthorizationInterceptor> interceptors = null;

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   */
  public ApiAuthorizationBuilder() {}

  private void addClaimValue(final Collection<String> groups, final Object value) {
    if (value != null) {
      String group = value.toString().trim();
      if (!group.isEmpty()) {
        groups.add(group);
      }
    }
  }

  private void addClaimValues(final Collection<String> groups, final Object value) {
    if (value instanceof Collection<?> values) {
      values.forEach(v -> addClaimValue(groups, v));
    } else if (value != null) {
      addDelimitedClaimValues(groups, value);
    }
  }

  private void addDelimitedClaimValues(final Collection<String> groups, final Object value) {
    String claims = value.toString().trim();
    if (claims.startsWith("[")) {
      claims = claims.substring(1);
    }

    if (claims.endsWith("]")) {
      claims = claims.substring(0, claims.length() - 1);
    }

    String delimiterRegex = claims.indexOf(',') >= 0 ? "," : "\\s+";
    for (String token : claims.split(delimiterRegex)) {
      addClaimValue(groups, token);
    }
  }

  /**
   * Build {@link ApiAuthorization}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * 
   * @return {@link ApiAuthorization}
   * @throws Exception Exception
   */
  public ApiAuthorization build(final ApiGatewayRequestEvent event) throws Exception {

    if (GLOBAL_SITE_URLS.contains(getPath(event))
        || GLOBAL_SITE_URLS.contains(getResource(event))) {
      return new ApiAuthorization().siteId(ReservedSiteId.GLOBAL.getSiteId());
    }

    Collection<String> groups = getGroups(event);
    boolean admin = isAdmin(groups);

    String defaultSiteId = getDefaultSiteId(event, groups, admin);

    Collection<String> roles = getRoles(event);

    Collection<String> samlGroups = getSamlGroups(event);
    Map<String, String> roleSiteMap = getRoleSiteMap(event);
    Map<String, Object> userClaims = getUserCustomClaims(event);

    ApiAuthorization authorization = new ApiAuthorization().siteId(defaultSiteId)
        .username(getUsername(event)).customEmail(getCustomEmail(event)).samlGroups(samlGroups)
        .roles(roles).roleSiteMap(roleSiteMap).userClaims(userClaims);

    SiteApiPermissionRoles siteApiPermissionRoles = createPermissionRoles(event, groups, admin);

    siteApiPermissionRoles.roles().forEach(authorization::addPermission);

    if (this.interceptors != null) {
      for (ApiAuthorizationInterceptor i : this.interceptors) {
        i.afterBuildAuthorization(event, authorization);
      }
    }

    defaultSiteId = authorization.getSiteId();

    validate(event, defaultSiteId, authorization);

    return authorization;
  }

  /**
   * Create permission roles, combining grants for duplicate site IDs.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param groups {@link Collection} {@link String}
   * @param admin boolean
   * @return {@link SiteApiPermissionRoles}
   */
  private SiteApiPermissionRoles createPermissionRoles(final ApiGatewayRequestEvent event,
      final Collection<String> groups, final boolean admin) throws UnauthorizedException {

    SiteApiPermissionRoles compact = getSitePermissions(event);
    if (compact != null) {
      return compact;
    }
    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);
    Map<String, List<String>> permissionsMap = getPermissionsMap(event);

    Collection<SiteApiPermissionRole> siteApiPermissionRoles = new ArrayList<>();

    if (permissionsMap != null) {

      permissionsMap.forEach((group, perms) -> {
        List<ApiPermission> permissions = perms.stream()
            .map(ApiAuthorizationBuilder::toApiPermission).filter(Objects::nonNull).toList();
        var permissionRole = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
            .permissions(permissions).build();
        siteApiPermissionRoles.add(new SiteApiPermissionRole(group, permissionRole));
      });

    } else {

      for (String group : groups) {

        String siteId = group;
        ApiPermissionRole permissionRole;

        if (!COGNITO_ADMIN_GROUP.equalsIgnoreCase(group)) {

          var permissionRoleType = ApiPermissionRoleType.fromSuffix(group);
          if (permissionRoleType.isPresent()) {
            var type = permissionRoleType.get();
            siteId = group.substring(0, group.length() - type.name().length() - 1);
            permissionRole = ApiPermissionRole.builder(type).build();
          } else if (admin) {
            permissionRole = ApiPermissionRole.builder(ApiPermissionRoleType.ADMIN).build();
          } else if (claims.containsKey("permissions")) {

            String[] list = claims.get("permissions").toString().split(",");
            var permissions =
                Arrays.stream(list).map(ApiAuthorizationBuilder::toApiPermission).toList();
            permissionRole = ApiPermissionRole.builder(ApiPermissionRoleType.CUSTOM)
                .permissions(permissions).build();

          } else {
            permissionRole = ApiPermissionRole.builder(ApiPermissionRoleType.WRITE).build();
          }

          var siteApiPermissionRole = new SiteApiPermissionRole(siteId, permissionRole);
          siteApiPermissionRoles.add(siteApiPermissionRole);
        }
      }
    }

    return new SiteApiPermissionRoles(
        new SiteApiPermissionRolesMerger().apply(siteApiPermissionRoles));
  }

  private String findDefaultSiteIdFromEventAndGroups(final ApiGatewayRequestEvent event,
      final Collection<String> groups) throws UnauthorizedException {

    SiteApiPermissionRoles compact = getSitePermissions(event);
    if (compact != null) {
      return compact.roles().size() == 1 ? compact.roles().iterator().next().siteId() : null;
    }
    Map<String, List<String>> permissionsMap = getPermissionsMap(event);

    if (permissionsMap != null) {
      return permissionsMap.size() == 1 ? permissionsMap.keySet().iterator().next() : null;
    }

    Collection<String> filteredGroups =
        groups.stream().filter(group -> !group.equalsIgnoreCase(COGNITO_ADMIN_GROUP))
            .map(group -> ApiPermissionRoleType.fromSuffix(group)
                .map(type -> group.substring(0, group.length() - type.name().length() - 1))
                .orElse(group))
            .collect(Collectors.toSet());

    return filteredGroups.size() == 1 ? filteredGroups.iterator().next() : null;
  }

  /**
   * Get AuthorizerClaims from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   */
  private Map<String, Object> getAuthorizerClaimsOrSitesClaims(final ApiGatewayRequestEvent event) {

    Map<String, Object> claims = Collections.emptyMap();

    ApiGatewayRequestContext requestContext = event != null ? event.getRequestContext() : null;

    if (requestContext != null) {
      Map<String, Object> authorizer = requestContext.getAuthorizer();

      claims = getAuthorizerClaimsOrSitesClaims(authorizer);
    }

    return claims;
  }

  private Map<String, Object> getAuthorizerClaimsOrSitesClaims(
      final Map<String, Object> authorizer) {
    Map<String, Object> claims = getAuthorizerClaims(authorizer);

    if (claims != null && claims.containsKey("sitesClaims")) {
      String sitesClaims = (String) claims.get("sitesClaims");
      claims = (Map<String, Object>) this.gson.fromJson(sitesClaims, Map.class);
    } else if (notNull(claims).isEmpty() && authorizer != null
        && authorizer.containsKey("apiKeyClaims")) {
      claims = (Map<String, Object>) authorizer.get("apiKeyClaims");
    }

    return claims;
  }

  private String getCallingCognitoUsernameFromClaims(final Map<String, Object> claims) {
    String username = null;
    if (claims.containsKey("email")) {
      username = claims.get("email").toString();
    } else if (claims.containsKey("username")) {
      username = claims.get("username").toString();
    } else if (claims.containsKey("cognito:username")) {
      username = claims.get("cognito:username").toString();
    }
    return username;
  }

  private Collection<String> getClaimsList(final ApiGatewayRequestEvent event, final String key) {
    Collection<String> groups = new HashSet<>();

    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);

    if (claims.containsKey(key)) {
      addClaimValues(groups, claims.get(key));
    }

    return groups;
  }

  private String getCustomEmail(final ApiGatewayRequestEvent event) {
    ApiGatewayRequestContext requestContext = event != null ? event.getRequestContext() : null;
    Map<String, Object> authorizer = requestContext != null ? requestContext.getAuthorizer() : null;
    Map<String, Object> claims = getAuthorizerClaims(authorizer);
    Object customEmail = claims != null ? claims.get("custom:email") : null;
    return customEmail != null ? customEmail.toString() : null;
  }

  private String getDefaultSiteId(final ApiGatewayRequestEvent event,
      final Collection<String> groups, final boolean isAdmin) throws UnauthorizedException {

    String siteId = getSiteIdRequestParameter(event);

    if (siteId == null) {
      siteId = findDefaultSiteIdFromEventAndGroups(event, groups);
    }

    Optional<ReservedSiteId> reserved = ReservedSiteId.fromString(siteId);
    if (reserved.isPresent() && (!isAdmin || !ReservedSiteId.GLOBAL.equals(reserved.get()))) {
      throw new UnauthorizedException("'" + siteId + "' siteId is reserved");
    }

    if (isAdmin && reserved.isPresent()) {
      groups.add(siteId);
    }

    return siteId;
  }

  /**
   * Get the Groups of the calling Cognito Username.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link List} {@link String}
   */
  private Collection<String> getGroups(final ApiGatewayRequestEvent event) {
    return loadJwtGroups(event);
  }

  private String getPath(final ApiGatewayRequestEvent event) {
    return event != null && !isEmpty(event.getPath()) ? event.getPath() : "";
  }

  private Map<String, List<String>> getPermissionsMap(final ApiGatewayRequestEvent event) {
    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);

    Map<String, List<String>> map = null;
    if (claims.containsKey("permissionsMap")) {
      map = (Map<String, List<String>>) claims.get("permissionsMap");
    }

    return map;
  }

  private String getResource(final ApiGatewayRequestEvent event) {
    return event != null && !isEmpty(event.getResource()) ? event.getResource() : "";
  }

  private Map<String, String> getRoleSiteMap(final ApiGatewayRequestEvent event) {
    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);

    Map<String, String> map = null;
    if (claims.containsKey("roleSiteMap")) {
      map = (Map<String, String>) claims.get("roleSiteMap");
    }

    return map;
  }

  /**
   * Get {@link ApiGatewayRequestEvent} roles.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Collection} {@link String}
   */
  private Collection<String> getRoles(final ApiGatewayRequestEvent event) {
    return getClaimsList(event, "cognito:groups");
  }

  private Collection<String> getSamlGroups(final ApiGatewayRequestEvent event) {
    return getClaimsList(event, "samlGroups");
  }

  /**
   * Get Query Parameter from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   */
  private String getSiteIdRequestParameter(final ApiGatewayRequestEvent event) {
    String key = "siteId";

    String siteId = null;

    if (event != null) {

      siteId = notNull(event.getPathParameters()).get(key);

      if (siteId == null) {
        siteId = notNull(event.getQueryStringParameters()).get(key);
      }
    }

    return siteId;
  }

  private SiteApiPermissionRoles getSitePermissions(final ApiGatewayRequestEvent event)
      throws UnauthorizedException {
    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);
    if (!claims.containsKey("sitePermissions")) {
      return null;
    }
    try {
      Object encoded = claims.get("sitePermissions");
      if (!(encoded instanceof String value)) {
        throw new IllegalArgumentException("sitePermissions must be a JSON string");
      }
      return SiteApiPermissionRoles.importPermissions(value);
    } catch (IllegalArgumentException e) {
      throw new UnauthorizedException("invalid site permissions");
    }
  }

  /**
   * Return custom claims from a JWT as a map.
   *
   * @param event JWT token
   * @return Map of custom claims, or empty map if JWT is invalid
   */
  private Map<String, Object> getUserCustomClaims(final ApiGatewayRequestEvent event) {

    String jwt = event != null ? event.getHeaderValue("authorization") : null;
    if (jwt == null) {
      jwt = event != null ? event.getHeaderValue("Authorization") : null;
    }
    Map<String, Object> customClaims = Collections.emptyMap();

    try {
      String[] parts = jwt != null ? jwt.split("\\.") : new String[0];
      if (parts.length >= 2) {
        String payload =
            new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        Map<String, Object> claims =
            gson.fromJson(payload, new TypeToken<Map<String, Object>>() {}.getType());

        if (claims != null) {
          Map<String, Object> result = new LinkedHashMap<>();
          for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (!EXCLUDED_CLAIMS.contains(entry.getKey())) {
              result.put(entry.getKey(), entry.getValue());
            }
          }
          customClaims = result;
        }
      }
    } catch (IllegalArgumentException | JsonSyntaxException e) {
      // ignore
    }

    return customClaims;
  }

  /**
   * Get the Cognito Groups of the calling Cognito Username.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   */
  private String getUserRoleArn(final ApiGatewayRequestEvent event) {

    String arn = null;

    ApiGatewayRequestContext requestContext = event != null ? event.getRequestContext() : null;

    if (requestContext != null) {

      Map<String, Object> identity = requestContext.getIdentity();

      if (identity != null) {
        Object obj = identity.getOrDefault("userArn", null);
        arn = obj != null ? obj.toString() : null;
      }
    }

    return arn;
  }

  /**
   * Get the calling Cognito Username.
   *
   * @param event {@link ApiGatewayRequestEvent}.
   * @return {@link String}
   */
  private String getUsername(final ApiGatewayRequestEvent event) {

    String username = null;

    if (event != null) {
      ApiGatewayRequestContext requestContext = event.getRequestContext();

      if (requestContext != null) {

        Map<String, Object> authorizer = requestContext.getAuthorizer();
        Map<String, Object> identity = requestContext.getIdentity();

        if (identity != null) {

          Object user = identity.getOrDefault("user", null);
          if (user != null) {
            username = user.toString();
          }

          Object userArn = identity.getOrDefault("userArn", null);
          if (userArn != null && userArn.toString().contains(":user/")) {
            username = userArn.toString();
          }
        }

        Map<String, Object> claims = getAuthorizerClaims(authorizer);

        String u = getCallingCognitoUsernameFromClaims(claims);
        if (u != null) {
          username = u;
        }
      }
    }

    return username;
  }

  /**
   * Set {@link ApiAuthorizationInterceptor}.
   * 
   * @param apiAuthorizationInterceptors {@link ApiAuthorizationInterceptor}
   * @return {@link ApiAuthorizationBuilder}
   */
  public ApiAuthorizationBuilder interceptors(
      final List<ApiAuthorizationInterceptor> apiAuthorizationInterceptors) {
    this.interceptors = apiAuthorizationInterceptors;
    return this;
  }

  /**
   * Is Admin Group in list.
   * 
   * @param groups {@link Collection} {@link String}
   * @return boolean
   */
  private boolean isAdmin(final Collection<String> groups) {
    return groups.stream().anyMatch(g -> g.equalsIgnoreCase(COGNITO_ADMIN_GROUP));
  }

  /**
   * Is RoleArn a AWS IAM caller.
   * 
   * @param roleArn {@link String}
   * @return boolean
   */
  private boolean isIamCaller(final String roleArn) {
    return roleArn != null && (roleArn.contains(":assumed-role/") || roleArn.contains(":user/"));
  }

  private Collection<String> loadJwtGroups(final ApiGatewayRequestEvent event) {

    Collection<String> groups = getRoles(event);

    String userRoleArn = getUserRoleArn(event);
    if (isIamCaller(userRoleArn)) {
      groups.add(COGNITO_ADMIN_GROUP);
    }

    if (groups.contains(COGNITO_ADMIN_GROUP)) {
      String siteId = getSiteIdRequestParameter(event);
      if (siteId != null) {
        groups.add(siteId);
      } else if (groups.size() < 2) {
        groups.add(DEFAULT_SITE_ID);
      }
    }

    groups.removeIf(g -> ReservedSiteId.fromString(g).isPresent());
    groups.remove("authentication_only");

    return groups;
  }

  private void throwForbiddenException(final String defaultSiteId) throws ForbiddenException {
    String s = String.format("fkq access denied to siteId (%s)", defaultSiteId);
    throw new ForbiddenException(s);
  }

  private void validate(final ApiGatewayRequestEvent event, final String defaultSiteId,
      final ApiAuthorization authorization) throws ForbiddenException {

    if (defaultSiteId != null && notNull(authorization.getPermissions(defaultSiteId)).isEmpty()
        && !event.getPath().startsWith("/public/")) {
      throwForbiddenException(defaultSiteId);
    }
  }
}
