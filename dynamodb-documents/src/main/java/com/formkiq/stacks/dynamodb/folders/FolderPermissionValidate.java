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
import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Validate Folder Permissions to Role.
 */
public class FolderPermissionValidate implements BiFunction<String, String, Void> {

  /** {@link FolderPermissionPredicate}. */
  private final FolderPermissionPredicate pred;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   * @param apiPermission {@link ApiPermission}
   */
  public FolderPermissionValidate(final DynamoDbService dbService,
      final ApiPermission apiPermission) {
    pred = new FolderPermissionPredicate(apiPermission);
    this.db = dbService;
  }

  @Override
  public Void apply(final String siteId, final String path) {

    DynamoDbKey key = FolderPermissionRecord.builder().path(path).buildKey(siteId);
    Map<String, AttributeValue> attributes = db.get(key);

    if (!attributes.isEmpty()) {
      FolderPermissionRecord folderPermissions =
          FolderPermissionRecord.fromAttributeMap(attributes);

      ValidationBuilder vb = new ValidationBuilder();
      vb.authorized(pred.test(siteId, folderPermissions.rolePermissions()));
      vb.check();
    }

    return null;
  }
}
