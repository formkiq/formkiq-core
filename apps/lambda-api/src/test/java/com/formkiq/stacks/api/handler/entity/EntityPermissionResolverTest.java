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
import com.formkiq.aws.dynamodb.entity.PresetEntity;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests preset permission lookup and generic entity permissions. */
class EntityPermissionResolverTest {
  /** Clear request authorization after each test. */
  @AfterEach
  void afterEach() {
    ApiAuthorization.logout();
  }

  /** Registered presets supply permissions only for the preset namespace. */
  @Test
  void testRegisteredPreset() {
    // given
    AwsServiceCache services = new AwsServiceCache();
    PresetEntity preset = new PresetEntity() {
      @Override
      public List<String> getAttributeKeys() {
        return List.of();
      }

      @Override
      public String getName() {
        return "Policy";
      }

      @Override
      public Optional<ApiPermission> getRequiredPermission(final String method) {
        return "get".equals(method) ? Optional.of(ApiPermission.READ_ATTRIBUTES) : Optional.empty();
      }
    };
    services.registerAppend(PresetEntity.class, new ClassServiceExtension<>(preset));
    EntityPermissionResolver resolver = new EntityPermissionResolver(services);
    EntityTypeName type = new EntityTypeName(EntityTypeNamespace.PRESET, "Policy");
    ApiAuthorization authorization =
        new ApiAuthorization().addCacheObject(EntityTypeName.class.getName(), type);
    ApiAuthorization.login(authorization);

    // when
    Optional<ApiPermission> permission = resolver.apply("get");
    Optional<ApiPermission> noPermission = resolver.apply("post");

    // then
    assertEquals(Optional.of(ApiPermission.READ_ATTRIBUTES), permission);
    assertEquals(Optional.empty(), noPermission);

    // given
    EntityTypeName custom = new EntityTypeName(EntityTypeNamespace.CUSTOM, "Policy");
    authorization.addCacheObject(EntityTypeName.class.getName(), custom);

    // when
    Optional<ApiPermission> customPermission = resolver.apply("get");

    // then
    assertEquals(Optional.of(ApiPermission.READ_ENTITY), customPermission);
  }

  /** Missing registrations use generic entity permissions. */
  @Test
  void testUnregisteredPreset() {
    // given
    EntityPermissionResolver resolver = new EntityPermissionResolver(new AwsServiceCache());
    EntityTypeName type = new EntityTypeName(EntityTypeNamespace.PRESET, "UnregisteredPolicy");
    ApiAuthorization
        .login(new ApiAuthorization().addCacheObject(EntityTypeName.class.getName(), type));

    // when
    Optional<ApiPermission> permission = resolver.apply("get");

    // then
    assertEquals(Optional.of(ApiPermission.READ_ENTITY), permission);
  }
}
