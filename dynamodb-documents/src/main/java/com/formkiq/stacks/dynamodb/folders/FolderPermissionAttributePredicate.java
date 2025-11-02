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
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@link Predicate} to test {@link FolderRolePermission}.
 */
public class FolderPermissionAttributePredicate implements
    BiFunction<String, List<Map<String, AttributeValue>>, List<Map<String, AttributeValue>>> {

  /** {@link FolderPermissionPredicate}. */
  private final FolderPermissionPredicate pred;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** Root Folder. */
  private final String root;

  /**
   * constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param apiPermission {@link ApiPermission}
   * @param rootFolder {@link String}
   */
  public FolderPermissionAttributePredicate(final DynamoDbService dbService,
      final ApiPermission apiPermission, final String rootFolder) {
    pred = new FolderPermissionPredicate(apiPermission);
    this.db = dbService;
    this.root = rootFolder;
  }

  @Override
  public List<Map<String, AttributeValue>> apply(final String siteId,
      final List<Map<String, AttributeValue>> attrs) {

    List<DynamoDbKey> pathKeys = attrs.stream()
        .map(a -> FolderPermissionRecord.builder()
            .path(root + "/" + DynamoDbTypes.toString(a.get("path"))).buildKey(siteId))
        .filter(Objects::nonNull).toList();

    List<FolderPermissionRecord> perm = db.get(db.getTableName(), pathKeys).stream()
        .map(FolderPermissionRecord::fromAttributeMap).toList();

    Map<String, Collection<FolderRolePermission>> permMap = perm.stream()
        .collect(Collectors.toMap(p -> new StringToFolder().apply(Strings.getFilename(p.path())),
            FolderPermissionRecord::rolePermissions));

    return !permMap.isEmpty() ? attrs.stream().filter(a -> {
      Collection<FolderRolePermission> perms =
          permMap.get(new StringToFolder().apply(DynamoDbTypes.toString(a.get("path"))));
      return pred.test(siteId, perms);
    }).toList() : attrs;
  }
}
