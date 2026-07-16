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
import com.formkiq.aws.dynamodb.folderpermissions.FolderPermissionPredicate;
import com.formkiq.aws.dynamodb.folderpermissions.FolderPermissionRecord;
import com.formkiq.aws.dynamodb.folderpermissions.FolderRolePermission;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    List<DynamoDbKey> pathKeys = attrs.stream().filter(this::isFolder)
        .map(a -> DynamoDbTypes.toString(a.get("documentId"))).filter(folderId -> folderId != null)
        .map(folderId -> FolderPermissionRecord.builder().folderId(folderId).buildKey(siteId))
        .toList();

    List<FolderPermissionRecord> perm = db.get(db.getTableName(), pathKeys).stream()
        .map(FolderPermissionRecord::fromAttributeMap).toList();

    Map<String, Collection<FolderRolePermission>> permMap = perm.stream().collect(Collectors
        .toMap(FolderPermissionRecord::folderId, FolderPermissionRecord::rolePermissions));

    return !permMap.isEmpty() ? attrs.stream().filter(a -> {
      Collection<FolderRolePermission> perms =
          isFolder(a) ? permMap.get(DynamoDbTypes.toString(a.get("documentId"))) : null;
      return pred.test(siteId, perms);
    }).toList() : attrs;
  }

  private boolean isFolder(final Map<String, AttributeValue> a) {
    return "folder".equals(DynamoDbTypes.toString(a.get("type")));
  }
}
