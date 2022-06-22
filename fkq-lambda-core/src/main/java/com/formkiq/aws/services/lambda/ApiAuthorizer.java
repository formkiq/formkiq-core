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

  /** Calling User Arn. */
  private String userArn = null;
  /** {@link List} SiteIds. */
  private List<String> siteIds = Collections.emptyList();

  /**
   * constructor.
   * 
   * @param requestEvent {@link ApiGatewayRequestEvent}
   */
  public ApiAuthorizer(final ApiGatewayRequestEvent requestEvent) {

    this.event = requestEvent;

    List<String> cognitoGroups = getCognitoGroups();
    this.siteIds = getPossibleSiteId();

    this.userArn = getUserRoleArn();
    this.isUserAdmin = cognitoGroups.contains(COGNITO_ADMIN_GROUP);
    this.siteId = getSiteIdFromQuery();
    this.isUserReadAccess =
        this.siteId != null ? cognitoGroups.contains(this.siteId + COGNITO_READ_SUFFIX) : false;
    this.isUserWriteAccess = this.siteId != null ? cognitoGroups.contains(this.siteId) : false;
  }

  /**
   * Get the Cognito Groups of the calling Cognito Username.
   *
   * @return {@link List} {@link String}
   */
  @SuppressWarnings("unchecked")
  private List<String> getCognitoGroups() {

    List<String> groups = Collections.emptyList();

    ApiGatewayRequestContext requestContext =
        this.event != null ? this.event.getRequestContext() : null;

    if (requestContext != null) {

      Map<String, Object> authorizer = requestContext.getAuthorizer();

      if (authorizer != null && authorizer.containsKey("claims")) {

        Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
        if (claims.containsKey("cognito:groups")) {
          Object obj = claims.get("cognito:groups");
          if (obj != null) {
            String s = obj.toString().replaceFirst("^\\[", "").replaceAll("\\]$", "");
            groups = new ArrayList<>(Arrays.asList(s.split(" ")));
            groups.removeIf(g -> g.length() == 0);
          }
        }
      }
    }

    return groups;
  }

  /**
   * Get the List of Possible SiteIds for User.
   * 
   * @return {@link Collection} {@link String}
   */
  private List<String> getPossibleSiteId() {

    List<String> cognitoGroups = getCognitoGroups();
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

    List<String> cognitoGroups = getCognitoGroups();
    cognitoGroups.remove(COGNITO_ADMIN_GROUP);

    List<String> sites = cognitoGroups.stream().filter(s -> s.endsWith(COGNITO_READ_SUFFIX))
        .map(s -> s.substring(0, s.indexOf(COGNITO_READ_SUFFIX))).collect(Collectors.toList());
    Collections.sort(sites);

    return sites;
  }

  /**
   * Get Read SiteIds.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getWriteSiteIds() {

    List<String> cognitoGroups = getCognitoGroups();
    cognitoGroups.remove(COGNITO_ADMIN_GROUP);

    List<String> sites = cognitoGroups.stream().filter(s -> !s.endsWith(COGNITO_READ_SUFFIX))
        .collect(Collectors.toList());
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
   * Get Site Id, including DEFAULT.
   * 
   * @return {@link String}
   */
  public String getSiteIdIncludeDefault() {
    return this.siteId;
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

  private boolean isIamCaller() {
    return this.isCallerAssumeRole() || this.isCallerIamUser();
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
