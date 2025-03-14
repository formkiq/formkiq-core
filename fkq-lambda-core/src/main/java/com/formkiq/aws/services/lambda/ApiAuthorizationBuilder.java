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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Api Authorization Token.
 *
 */
public class ApiAuthorizationBuilder {

  /** Cognito Admin Group Name. */
  private static final String COGNITO_ADMIN_GROUP = "Admins";
  /** The suffix for the 'readonly' Cognito group. */
  public static final String COGNITO_READ_SUFFIX = "_read";
  /** The suffix for the 'readonly' Cognito group. */
  public static final String COGNITO_GOVERN_SUFFIX = "_govern";
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

    Map<String, Object> claims = getAuthorizerClaims(event);

    for (String group : groups) {

      if (!COGNITO_ADMIN_GROUP.equalsIgnoreCase(group)) {
        if (group.endsWith(COGNITO_READ_SUFFIX)) {
          authorization.addPermission(group.replace(COGNITO_READ_SUFFIX, ""),
              List.of(ApiPermission.READ));
        } else if (group.endsWith(COGNITO_GOVERN_SUFFIX)) {
          authorization.addPermission(group.replace(COGNITO_GOVERN_SUFFIX, ""), List.of(
              ApiPermission.GOVERN, ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE));
        } else if (admin) {
          authorization.addPermission(group, Arrays.asList(ApiPermission.READ, ApiPermission.WRITE,
              ApiPermission.DELETE, ApiPermission.ADMIN));
        } else if (claims.containsKey("permissions")) {

          String[] list = claims.get("permissions").toString().split(",");
          List<ApiPermission> permissions = Arrays.stream(list)
              .map(ApiAuthorizationBuilder::toApiPermission).collect(Collectors.toList());
          authorization.addPermission(group, permissions);

        } else if (claims.containsKey("permissionsMap")) {

          Map<String, List<String>> map = (Map<String, List<String>>) claims.get("permissionsMap");

          if (map.containsKey(group)) {
            List<String> strs = map.get(group);
            List<ApiPermission> permissions = strs.stream()
                .map(ApiAuthorizationBuilder::toApiPermission).filter(Objects::nonNull).toList();
            authorization.addPermission(group, permissions);
          }

        } else {
          authorization.addPermission(group,
              Arrays.asList(ApiPermission.READ, ApiPermission.WRITE, ApiPermission.DELETE));
        }
      }
    }
  }

  private static ApiPermission toApiPermission(final String val) {
    try {
      return ApiPermission.valueOf(val.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
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

    Collection<String> groups = getGroups(event);
    boolean admin = isAdmin(groups);

    String defaultSiteId = getDefaultSiteId(event, groups);

    Collection<String> roles = getRoles(event);

    ApiAuthorization authorization =
        new ApiAuthorization().siteId(defaultSiteId).username(getUsername(event)).roles(roles);

    addPermissions(event, authorization, groups, admin);

    if (this.interceptors != null) {
      for (ApiAuthorizationInterceptor i : this.interceptors) {
        i.update(event, authorization);
      }
    }

    if (defaultSiteId != null && !isValidSiteId(defaultSiteId, groups)
        && !event.getPath().startsWith("/public/")) {
      String s = String.format("fkq access denied to siteId (%s)", defaultSiteId);
      throw new ForbiddenException(s);
    }

    return authorization;
  }

  /**
   * Get AuthorizerClaims from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   */
  private Map<String, Object> getAuthorizerClaims(final ApiGatewayRequestEvent event) {

    Map<String, Object> claims = Collections.emptyMap();

    ApiGatewayRequestContext requestContext = event != null ? event.getRequestContext() : null;

    if (requestContext != null) {
      Map<String, Object> authorizer = requestContext.getAuthorizer();

      claims = getAuthorizerClaims(authorizer);
    }

    return claims;
  }

  private Map<String, Object> getAuthorizerClaims(final Map<String, Object> authorizer) {
    Map<String, Object> claims = Collections.emptyMap();

    if (authorizer != null && authorizer.containsKey("claims")) {
      claims = (Map<String, Object>) authorizer.get("claims");
    }

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

  private String getDefaultSiteId(final ApiGatewayRequestEvent event,
      final Collection<String> groups) {

    String siteId = getSiteIdRequestParameter(event);

    if (siteId == null) {
      Collection<String> filteredGroups =
          groups.stream().filter(g -> !g.equalsIgnoreCase(COGNITO_ADMIN_GROUP))
              .map(g -> g.endsWith(COGNITO_READ_SUFFIX) ? g.replace(COGNITO_READ_SUFFIX, "") : g)
              .map(
                  g -> g.endsWith(COGNITO_GOVERN_SUFFIX) ? g.replace(COGNITO_GOVERN_SUFFIX, "") : g)
              .collect(Collectors.toSet());

      siteId = filteredGroups.size() == 1 ? filteredGroups.iterator().next() : null;
    }

    return siteId;
  }

  private boolean isValidSiteId(final String siteId, final Collection<String> groups) {
    return groups.contains(siteId) || groups.contains(siteId + COGNITO_READ_SUFFIX)
        || groups.contains(siteId + COGNITO_GOVERN_SUFFIX);
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
   * Get {@link ApiGatewayRequestEvent} roles.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Collection} {@link String}
   */
  private Collection<String> getRoles(final ApiGatewayRequestEvent event) {
    Collection<String> groups = new HashSet<>();

    Map<String, Object> claims = getAuthorizerClaims(event);

    if (claims.containsKey("cognito:groups")) {
      Object obj = claims.get("cognito:groups");
      if (obj != null) {
        String s = obj.toString().replaceFirst("^\\[", "").replaceAll("\\]$", "");
        groups = new HashSet<>(Arrays.asList(s.split(" ")));
        groups.removeIf(String::isEmpty);
      }
    }
    return groups;
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

    groups.remove("authentication_only");

    return groups;
  }
}
