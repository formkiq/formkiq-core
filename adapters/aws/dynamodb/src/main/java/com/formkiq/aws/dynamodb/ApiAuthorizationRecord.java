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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Persistable authorization context. Request caches and handler-specific attribute approvals remain
 * transient. Top-level collections are copied when capturing and restoring the context.
 *
 * @param key DynamoDB key, optional until persistence
 * @param siteId selected site
 * @param username caller identity
 * @param customEmail custom email claim
 * @param roles authorization groups
 * @param samlGroups SAML groups
 * @param roleSiteMap role-to-site mappings
 * @param userClaims custom user claims
 * @param sitePermissions explicit site permission roles
 * @param timeToLive expiration as epoch seconds
 */
public record ApiAuthorizationRecord(DynamoDbKey key, String siteId, String username,
    String customEmail, Collection<String> roles, Collection<String> samlGroups,
    Map<String, String> roleSiteMap, Map<String, Object> userClaims,
    SiteApiPermissionRoles sitePermissions, Long timeToLive) {

  /** Copy the top-level authorization collections. */
  public ApiAuthorizationRecord {
    roles = roles != null ? List.copyOf(roles) : null;
    samlGroups = samlGroups != null ? List.copyOf(samlGroups) : null;
    roleSiteMap =
        roleSiteMap != null ? Map.copyOf(roleSiteMap) : null;
    userClaims = userClaims != null ? Map.copyOf(userClaims) : null;
    sitePermissions =
        sitePermissions != null ? sitePermissions : new SiteApiPermissionRoles(List.of());
  }

  /**
   * Capture authorization without persistence metadata.
   *
   * @param authorization authorization to capture
   * @return authorization record
   */
  public static ApiAuthorizationRecord fromApiAuthorization(final ApiAuthorization authorization) {
    return fromApiAuthorization(authorization, null, null);
  }

  /**
   * Capture authorization with a caller-supplied DynamoDB key and expiration.
   *
   * @param authorization authorization to capture
   * @param recordKey DynamoDB key
   * @param expiration expiration as epoch seconds
   * @return authorization record
   */
  public static ApiAuthorizationRecord fromApiAuthorization(final ApiAuthorization authorization,
      final DynamoDbKey recordKey, final Long expiration) {
    Objects.requireNonNull(authorization, "authorization must not be null");
    return new ApiAuthorizationRecord(recordKey, authorization.getSiteId(),
        authorization.getUsername(), authorization.getCustomEmail(), authorization.getRoles(),
        authorization.getSamlGroups(), authorization.getRoleSiteMap(),
        authorization.getUserClaims(), authorization.getSitePermissions(), expiration);
  }

  /**
   * Restore authorization without changing the current thread's authorization.
   *
   * @return restored authorization
   */
  public ApiAuthorization toApiAuthorization() {
    ApiAuthorization authorization = new ApiAuthorization().siteId(siteId).username(username)
        .customEmail(customEmail).roles(roles != null ? new ArrayList<>(roles) : null)
        .samlGroups(samlGroups != null ? new ArrayList<>(samlGroups) : null)
        .roleSiteMap(roleSiteMap != null ? new HashMap<>(roleSiteMap) : null)
        .userClaims(userClaims != null ? new HashMap<>(userClaims) : null);
    sitePermissions.roles().forEach(authorization::addPermission);
    return authorization;
  }

  /**
   * Serialize the record using native DynamoDB attributes.
   *
   * @return item attributes including the key and optional TTL
   */
  public Map<String, AttributeValue> getAttributes() {
    Objects.requireNonNull(key, "key is required for persistence");
    List<AttributeValue> permissions = sitePermissions.roles().stream()
        .map(ApiAuthorizationRecord::toPermissionAttributes).toList();
    return key.getAttributesBuilder().withString("siteId", siteId).withString("username", username)
        .withString("customEmail", customEmail).withStrings("roles", roles)
        .withStrings("samlGroups", samlGroups)
        .withMap("roleSiteMap", roleSiteMap != null ? new HashMap<>(roleSiteMap) : null)
        .withMap("userClaims", userClaims)
        .withAttributeValue("sitePermissions", AttributeValue.fromL(permissions))
        .withLong("TimeToLive", timeToLive).build();
  }

  /**
   * Read a stored authorization record.
   *
   * @param attributes DynamoDB item attributes
   * @return authorization record
   */
  public static ApiAuthorizationRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {
    Map<String, String> mapping = null;
    if (attributes.containsKey("roleSiteMap")) {
      mapping = new HashMap<>();
      for (var entry : attributes.get("roleSiteMap").m().entrySet()) {
        mapping.put(entry.getKey(), DynamoDbTypes.toString(entry.getValue()));
      }
    }
    Map<String, Object> claims = attributes.containsKey("userClaims")
        ? (Map<String, Object>) DynamoDbTypes.toJson(attributes.get("userClaims"))
        : null;
    List<SiteApiPermissionRole> permissions =
        attributes.containsKey("sitePermissions")
            ? attributes.get("sitePermissions").l().stream()
                .map(ApiAuthorizationRecord::fromPermissionAttributes).toList()
            : List.of();
    return new ApiAuthorizationRecord(DynamoDbKey.fromAttributeMap(attributes),
        DynamoDbTypes.toString(attributes.get("siteId")),
        DynamoDbTypes.toString(attributes.get("username")),
        DynamoDbTypes.toString(attributes.get("customEmail")),
        DynamoDbTypes.toStrings(attributes.get("roles")),
        DynamoDbTypes.toStrings(attributes.get("samlGroups")), mapping, claims,
        new SiteApiPermissionRoles(permissions),
        DynamoDbTypes.toLong(attributes.get("TimeToLive")));
  }

  /**
   * Encode a site's role type and exact grants.
   *
   * @param permission site permission role
   * @return DynamoDB map value
   */
  private static AttributeValue toPermissionAttributes(final SiteApiPermissionRole permission) {
    return AttributeValue.fromM(DynamoDbAttributeMapBuilder.builder()
        .withString("siteId", permission.siteId()).withEnum("roleType", permission.role().getType())
        .withEnumList("permissions", permission.role().getPermissions()).build());
  }

  /**
   * Decode a site's role type and exact grants.
   *
   * @param value DynamoDB map value
   * @return site permission role
   */
  private static SiteApiPermissionRole fromPermissionAttributes(final AttributeValue value) {
    Map<String, AttributeValue> attributes = value.m();
    ApiPermissionRoleType type =
        ApiPermissionRoleType.valueOf(DynamoDbTypes.toString(attributes.get("roleType")));
    ApiPermissionRole.Builder builder = ApiPermissionRole.builder(type);
    if (type == ApiPermissionRoleType.CUSTOM) {
      builder.permissions(DynamoDbTypes.toStrings(attributes.get("permissions")).stream()
          .map(ApiPermission::valueOf).toList());
    }
    return new SiteApiPermissionRole(DynamoDbTypes.toString(attributes.get("siteId")),
        builder.build());
  }
}
