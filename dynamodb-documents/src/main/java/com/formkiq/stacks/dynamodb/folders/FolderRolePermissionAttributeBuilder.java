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
package com.formkiq.stacks.dynamodb.folders;

import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.builder.CustomDynamoDbAttributeBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbAttributeMapBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link CustomDynamoDbAttributeBuilder} for {@link FolderRolePermission}.
 */
public class FolderRolePermissionAttributeBuilder implements CustomDynamoDbAttributeBuilder {

  /** Attribute Prefix. */
  private static final String ATTRIBUTE_KEY_PREFIX = "role#";

  @Override
  public <T> T decode(final String name, final Map<String, AttributeValue> attrs) {

    List<FolderRolePermission> list = null;
    List<String> keys =
        attrs.keySet().stream().filter(k -> k.startsWith(ATTRIBUTE_KEY_PREFIX)).toList();

    if (!keys.isEmpty()) {
      list = keys.stream().map(k -> {
        String roleName = k.substring(ATTRIBUTE_KEY_PREFIX.length());
        List<ApiPermission> permissions =
            attrs.get(k).l().stream().map(a -> ApiPermission.valueOf(a.s())).toList();
        return new FolderRolePermission(roleName, permissions);
      }).toList();
    }

    return (T) list;
  }

  @Override
  public Map<String, AttributeValue> encode(final String name, final Object value) {

    boolean encodedValue = false;
    DynamoDbAttributeMapBuilder builder = DynamoDbAttributeMapBuilder.builder();

    if (value instanceof Collection<?> c) {
      for (Object o : c) {
        if (o instanceof FolderRolePermission p) {
          Collection<ApiPermission> permissions =
              Objects.requireNonNullElse(p.permissions(), Collections.emptyList());
          builder.withEnumList(ATTRIBUTE_KEY_PREFIX + p.roleName(), permissions);
          encodedValue = true;
        }
      }
    }

    if (!encodedValue) {
      throw new IllegalArgumentException("Folder Permissions are missing");
    }
    return builder.build();
  }
}
