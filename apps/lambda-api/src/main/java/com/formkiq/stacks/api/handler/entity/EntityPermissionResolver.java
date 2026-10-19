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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.entity.EntityTypeName;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.GetPresetEntity;
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Resolves entity permissions using the current request's cached entity type identity. */
public class EntityPermissionResolver implements Function<String, Optional<ApiPermission>> {

  /** Registered preset lookup. */
  private final GetPresetEntity presets;

  /**
   * Constructor.
   *
   * @param awsServiceCache registered services
   */
  public EntityPermissionResolver(final AwsServiceCache awsServiceCache) {
    this.presets = new GetPresetEntity(awsServiceCache);
  }

  @Override
  public Optional<ApiPermission> apply(final String method) {
    EntityTypeName entityType =
        ApiAuthorization.getAuthorization().getCacheObject(EntityTypeName.class.getName());
    return resolve(method, entityType);
  }

  /**
   * Resolve the operation permission for an entity type.
   *
   * @param method HTTP method
   * @param entityType resolved entity type, or null for generic entity operations
   * @return required permission, or empty for methods without a permission requirement
   */
  private Optional<ApiPermission> resolve(final String method, final EntityTypeName entityType) {
    if (entityType != null && EntityTypeNamespace.PRESET.equals(entityType.namespace())
        && entityType.name() != null) {
      Optional<PresetEntity> preset = presets.apply(entityType.name());
      if (preset.isPresent()) {
        return preset.get().getRequiredPermission(method);
      }
    }
    return switch (method.toLowerCase(Locale.ROOT)) {
      case "get", "head" -> Optional.of(ApiPermission.READ_ENTITY);
      case "post", "put", "patch" -> Optional.of(ApiPermission.WRITE_ENTITY);
      case "delete" -> Optional.of(ApiPermission.DELETE_ENTITY);
      default -> Optional.empty();
    };
  }
}
