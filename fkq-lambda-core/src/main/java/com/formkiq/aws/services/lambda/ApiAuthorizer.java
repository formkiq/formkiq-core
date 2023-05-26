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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Authorizer.
 *
 */
public class ApiAuthorizer {

  /** Cognito Admin Group Name. */
  private static final String COGNITO_ADMIN_GROUP = "Admins";
  /** The suffix for the 'readonly' Cognito group. */
  private static final String COGNITO_READ_SUFFIX = "_read";

  /** {@link ApiAuthorizerType}. */
  private ApiAuthorizerType authentication;

  /** {@link ApiGatewayRequestEvent}. */
  private ApiGatewayRequestEvent event;

  /** Is User Admin Request. */
  private boolean isUserAdmin = false;

  /** Is User Read Access. */
  private boolean isUserReadAccess = false;

  /** Is User Write Access. */
  private boolean isUserWriteAccess = false;

  /** Request SiteId. */
  private String siteId = null;
  /** {@link List} SiteIds. */
  private List<String> siteIds = Collections.emptyList();
  /** Calling User Arn. */
  private String userArn = null;

  /**
   * constructor.
   * 
   * @param requestEvent {@link ApiGatewayRequestEvent}
   * @param userAuthentication {@link ApiAuthorizerType}
   */
  public ApiAuthorizer(final ApiGatewayRequestEvent requestEvent,
      final ApiAuthorizerType userAuthentication) {

    this.event = requestEvent;
    this.authentication = userAuthentication;

    List<String> cognitoGroups = getCognitoGroups(this.authentication);
    this.siteIds = getPossibleSiteId();

    this.userArn = getUserRoleArn();
    this.isUserAdmin = cognitoGroups.contains(COGNITO_ADMIN_GROUP)
        || cognitoGroups.contains(COGNITO_ADMIN_GROUP.toLowerCase());
    this.siteId = getSiteIdFromQuery();
    this.isUserReadAccess =
        this.siteId != null ? cognitoGroups.contains(this.siteId + COGNITO_READ_SUFFIX) : false;
    this.isUserWriteAccess = this.siteId != null ? cognitoGroups.contains(this.siteId) : false;
  }

  /**
   * Return Access Summary.
   * 
   * @return {@link String}
   */
  public String accessSummary() {
    List<String> groups = getCognitoGroups(this.authentication);
    return !groups.isEmpty() ? "groups: " + String.join(",", groups) : "no groups";
  }

  /**
   * Get the Cognito Groups of the calling Cognito Username.
   * 
   * @param userAuthentication {@link ApiAuthorizerType}
   * @return {@link List} {@link String}
   */
  private List<String> getCognitoGroups(final ApiAuthorizerType userAuthentication) {

    List<String> groups = Collections.emptyList();

    ApiGatewayRequestContext requestContext =
        this.event != null ? this.event.getRequestContext() : null;

    if (requestContext != null) {

      Map<String, Object> authorizer = requestContext.getAuthorizer();

      Map<String, Object> claims = getAuthorizerClaims(authorizer);

      if (claims.containsKey("cognito:groups")) {
        Object obj = claims.get("cognito:groups");
        if (obj != null) {
          String s = obj.toString().replaceFirst("^\\[", "").replaceAll("\\]$", "");
          groups = new ArrayList<>(Arrays.asList(s.split(" ")));
          groups.removeIf(g -> g.length() == 0);
        }
      }
    }

    if (ApiAuthorizerType.SAML.equals(userAuthentication)) {
      groups = groups.stream().map(g -> g.replaceAll("^formkiq_", "")).collect(Collectors.toList());
    }

    if (groups.isEmpty()) {
      groups = Arrays.asList("default");
    }

    return groups;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getAuthorizerClaims(final Map<String, Object> authorizer) {
    Map<String, Object> claims = Collections.emptyMap();

    if (authorizer != null && authorizer.containsKey("claims")) {
      claims = (Map<String, Object>) authorizer.get("claims");
    } else if (authorizer != null && authorizer.containsKey("apiKeyClaims")) {
      claims = (Map<String, Object>) authorizer.get("apiKeyClaims");
    }
    return claims;
  }

  /**
   * Get the List of Possible SiteIds for User.
   * 
   * @return {@link Collection} {@link String}
   */
  private List<String> getPossibleSiteId() {

    List<String> cognitoGroups = getCognitoGroups(this.authentication);
    cognitoGroups.remove(COGNITO_ADMIN_GROUP);

    List<String> sites = new ArrayList<>(cognitoGroups.stream().map(
        s -> s.endsWith(COGNITO_READ_SUFFIX) ? s.substring(0, s.indexOf(COGNITO_READ_SUFFIX)) : s)
        .collect(Collectors.toSet()));

    Collections.sort(sites);
    return sites;
  }

  /**
   * Get Read SiteIds.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getReadSiteIds() {

    List<String> cognitoGroups = getCognitoGroups(this.authentication);
    cognitoGroups.remove(COGNITO_ADMIN_GROUP);

    List<String> sites = cognitoGroups.stream().filter(s -> s.endsWith(COGNITO_READ_SUFFIX))
        .map(s -> s.substring(0, s.indexOf(COGNITO_READ_SUFFIX))).collect(Collectors.toList());
    Collections.sort(sites);

    return sites;
  }

  /**
   * Get Site Id.
   * 
   * @return {@link String}
   */
  public String getSiteId() {
    return this.siteId != null && !this.siteId.equals(DEFAULT_SITE_ID) ? this.siteId : null;
  }

  /**
   * Set siteId in {@link ApiGatewayRequestEvent}, if user is in only 1 CognitoGroup.
   * 
   * @return {@link String}
   */
  private String getSiteIdFromQuery() {

    String site = null;

    String siteIdParam = this.event != null ? this.event.getQueryStringParameter("siteId") : null;
    if (siteIdParam != null && (this.siteIds.contains(siteIdParam)) || this.isUserAdmin
        || isIamCaller()) {
      site = siteIdParam;
    } else if (siteIdParam == null && !this.siteIds.isEmpty()) {
      site = this.siteIds.get(0);
    }

    if (site == null && this.isUserAdmin) {
      site = DEFAULT_SITE_ID;
    }

    return site;
  }

  /**
   * Get Site Id, including DEFAULT.
   * 
   * @return {@link String}
   */
  public String getSiteIdIncludeDefault() {
    return this.siteId;
  }

  /**
   * Get Site Ids.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getSiteIds() {
    return this.siteIds;
  }

  /**
   * Get the Cognito Groups of the calling Cognito Username.
   *
   * @return {@link String}
   */
  private String getUserRoleArn() {

    String arn = null;

    ApiGatewayRequestContext requestContext =
        this.event != null ? this.event.getRequestContext() : null;

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
   * Get Read SiteIds.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getWriteSiteIds() {

    List<String> cognitoGroups = getCognitoGroups(this.authentication);
    cognitoGroups.remove(COGNITO_ADMIN_GROUP);

    List<String> sites = cognitoGroups.stream().filter(s -> !s.endsWith(COGNITO_READ_SUFFIX))
        .collect(Collectors.toList());
    Collections.sort(sites);

    return sites;
  }

  /**
   * Is {@link ApiGatewayRequestEvent} is an assumed role.
   * 
   * @return boolean
   */
  public boolean isCallerAssumeRole() {
    return this.userArn != null && this.userArn.contains(":assumed-role/");
  }

  /**
   * Is {@link ApiGatewayRequestEvent} is an IAM user.
   * 
   * @return boolean
   */
  public boolean isCallerIamUser() {
    return this.userArn != null && this.userArn.contains(":user/");
  }

  private boolean isIamCaller() {
    return this.isCallerAssumeRole() || this.isCallerIamUser();
  }

  /**
   * Is User Admin.
   * 
   * @return boolean
   */
  public boolean isUserAdmin() {
    return this.isUserAdmin;
  }

  /**
   * Is User Read Access.
   * 
   * @return boolean
   */
  public boolean isUserReadAccess() {
    return this.isUserReadAccess;
  }

  /**
   * Is User Write Access.
   * 
   * @return boolean
   */
  public boolean isUserWriteAccess() {
    return this.isUserWriteAccess;
  }
}
