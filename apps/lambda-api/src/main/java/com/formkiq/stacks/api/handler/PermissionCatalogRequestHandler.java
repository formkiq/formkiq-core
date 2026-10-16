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
package com.formkiq.stacks.api.handler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.ApiPermissionGroup;
import com.formkiq.aws.dynamodb.ApiPermissionRole;
import com.formkiq.aws.dynamodb.ApiPermissionRoleType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/** Permission catalog for creating API keys and configuring user roles. */
public class PermissionCatalogRequestHandler implements ApiGatewayRequestHandler {

  /** Constructor. */
  public PermissionCatalogRequestHandler() {}

  private Optional<Map<String, Object>> createGroup(final ApiPermissionRoleType type,
      final ApiPermissionRole role, final ApiPermissionGroup group) {
    List<Map<String, String>> permissions = Arrays.stream(ApiPermission.values())
        .filter(permission -> permission.getGroup() == group)
        .filter(
            permission -> type == ApiPermissionRoleType.CUSTOM || role.hasPermission(permission))
        .map(permission -> Map.of("name", permission.name(), "description",
            permission.getDescription()))
        .toList();
    return permissions.isEmpty() ? Optional.empty()
        : Optional
            .of(Map.of("name", group.name(), "label", group.getTag(), "permissions", permissions));
  }

  private Map<String, Object> createRoleType(final ApiPermissionRoleType type) {
    ApiPermissionRole role = ApiPermissionRole.builder(type).build();
    List<Map<String, Object>> groups = Arrays.stream(ApiPermissionGroup.values())
        .map(group -> createGroup(type, role, group)).flatMap(Optional::stream).toList();
    return Map.of("name", type.name(), "groups", groups);
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {
    List<Map<String, Object>> roleTypes =
        Arrays.stream(ApiPermissionRoleType.values()).map(this::createRoleType).toList();
    return ApiRequestHandlerResponse.builder().ok().body(Map.of("roleTypes", roleTypes)).build();
  }

  @Override
  public String getRequestUrl() {
    return "/permissions/catalog";
  }

  @Override
  public Optional<ApiPermission> getRequiredPermission(final String method) {
    return Optional.empty();
  }

  private boolean hasAuthorizer(final Map<String, Object> authorizer) {
    return authorizer != null && (hasPrincipal(authorizer.get("principalId"))
        || Arrays.stream(new String[] {"claims", "apiKeyClaims", "jwt"})
            .anyMatch(key -> authorizer.get(key) instanceof Map<?, ?> claims && !claims.isEmpty()));
  }

  private boolean hasIdentity(final Map<String, Object> identity) {
    return identity != null
        && (hasPrincipal(identity.get("userArn")) || hasPrincipal(identity.get("user")));
  }

  private boolean hasPrincipal(final Object principal) {
    return principal instanceof String value && !value.isBlank();
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsServiceCache, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    // Authentication is established by API Gateway, independently of site-specific grants.
    var context = event.getRequestContext();
    boolean authenticated = context != null
        && (hasAuthorizer(context.getAuthorizer()) || hasIdentity(context.getIdentity()));
    return Optional.of(authenticated);
  }
}
