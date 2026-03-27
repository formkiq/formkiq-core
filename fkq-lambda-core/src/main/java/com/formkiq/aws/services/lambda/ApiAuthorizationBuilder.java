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
  private static final Set<String> EXCLUDED_CLAIMS =
      Set.of("sub", "cognito:groups", "iss", "client_id", "origin_jti", "event_id", "token_use",
          "scope", "auth_time", "exp", "iat", "jti", "username", "sitesClaims", "cognito:username");
  /** Cognito Admin Group Name. */
  private static final String COGNITO_ADMIN_GROUP = "Admins";
  /** The suffix for the 'readonly' Cognito group. */
  public static final String COGNITO_READ_SUFFIX = "_read";
  /** The suffix for the 'readonly' Cognito group. */
  public static final String COGNITO_GOVERN_SUFFIX = "_govern";
  /** Global Site Urls. */
  private static final Collection<String> GLOBAL_SITE_URLS = Set.of("/changePassword",
      "/confirmRegistration", "/forgotPassword", "/forgotPasswordConfirm", "/login");

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

  /**
   * Add Permissions.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param groups {@link Collection} {@link String}
   * @param admin boolean
   */
  private void addPermissions(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final Collection<String> groups, final boolean admin) {

    Map<String, Object> claims = getAuthorizerClaimsOrSitesClaims(event);
    Map<String, List<String>> permissionsMap = getPermissionsMap(event);

    if (permissionsMap != null) {

      String siteId = event.getQueryStringParameter("siteId");

      permissionsMap.forEach((group, perms) -> {
        List<ApiPermission> permissions = perms.stream()
            .map(ApiAuthorizationBuilder::toApiPermission).filter(Objects::nonNull).toList();
        authorization.addPermission(group, permissions);
      });

      if (permissionsMap.size() == 1 && siteId == null) {
        authorization.siteId(permissionsMap.keySet().iterator().next());
      }

    } else {

      for (String group : groups) {

        if (!COGNITO_ADMIN_GROUP.equalsIgnoreCase(group)) {
          if (group.endsWith(COGNITO_READ_SUFFIX)) {
            authorization.addPermission(group.replace(COGNITO_READ_SUFFIX, ""),
                List.of(ApiPermission.READ));
          } else if (group.endsWith(COGNITO_GOVERN_SUFFIX)) {
            authorization.addPermission(group.replace(COGNITO_GOVERN_SUFFIX, ""),
                List.of(ApiPermission.GOVERN, ApiPermission.READ, ApiPermission.WRITE,
                    ApiPermission.DELETE));
          } else if (admin) {
            authorization.addPermission(group, List.of(ApiPermission.values()));
          } else if (claims.containsKey("permissions")) {

            String[] list = claims.get("permissions").toString().split(",");
            List<ApiPermission> permissions = Arrays.stream(list)
                .map(ApiAuthorizationBuilder::toApiPermission).collect(Collectors.toList());
            authorization.addPermission(group, permissions);

          } else {
            authorization.addPermission(group,
                Arrays.asList(ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE));
          }
        }
      }
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

    if (GLOBAL_SITE_URLS.contains(getPath(event))) {
      return new ApiAuthorization().siteId(ReservedSiteId.GLOBAL.getSiteId());
    }

    Collection<String> groups = getGroups(event);
    boolean admin = isAdmin(groups);

    String defaultSiteId = getDefaultSiteId(event, groups, admin);

    Collection<String> roles = getRoles(event);

    Collection<String> samlGroups = getSamlGroups(event);
    Map<String, Object> userClaims = getUserCustomClaims(event);

    ApiAuthorization authorization = new ApiAuthorization().siteId(defaultSiteId)
        .username(getUsername(event)).samlGroups(samlGroups).roles(roles).userClaims(userClaims);

    addPermissions(event, authorization, groups, admin);

    if (this.interceptors != null) {
      for (ApiAuthorizationInterceptor i : this.interceptors) {
        i.update(event, authorization);
      }
    }

    defaultSiteId = authorization.getSiteId();

    validate(event, defaultSiteId, authorization);

    return authorization;
  }

  private String findDefaultSiteIdFromEventAndGroups(final ApiGatewayRequestEvent event,
      final Collection<String> groups) {

    Map<String, List<String>> permissionsMap = getPermissionsMap(event);

    if (permissionsMap != null) {
      return permissionsMap.size() == 1 ? permissionsMap.keySet().iterator().next() : null;
    }

    Collection<String> filteredGroups =
        groups.stream().filter(g -> !g.equalsIgnoreCase(COGNITO_ADMIN_GROUP))
            .map(g -> g.endsWith(COGNITO_READ_SUFFIX) ? g.replace(COGNITO_READ_SUFFIX, "") : g)
            .map(g -> g.endsWith(COGNITO_GOVERN_SUFFIX) ? g.replace(COGNITO_GOVERN_SUFFIX, "") : g)
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
      Object obj = claims.get(key);
      if (obj != null) {

        String s = obj.toString().trim();
        if (s.startsWith("[")) {
          s = s.substring(1);
        }

        if (s.endsWith("]")) {
          s = s.substring(0, s.length() - 1);
        }

        String delimiterRegex = s.indexOf(',') >= 0 ? "," : " ";
        for (String token : s.split(delimiterRegex)) {
          if (!token.isEmpty()) {
            groups.add(token);
          }
        }
      }
    }

    return groups;
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
