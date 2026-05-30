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
package com.formkiq.aws.dynamodb.folders;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.folderpermissions.FolderPermissionValidate;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.formkiq.aws.dynamodb.objects.Objects.last;
import static com.formkiq.strings.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Convert {@link String} Path to {@link List} {@link FolderIndexRecord}.
 */
public class PathToFolderIndexRecords
    implements BiFunction<String, String, List<FolderIndexRecord>> {

  /** Lock Timeout in MS. */
  private static final long LOCK_ACQUIRE_TIMEOUT_IN_MS = 10000;
  /** Lock Expiration in MS. */
  private static final long LOCK_EXPIRATION_IN_MS = 20000;

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** Create Missing folders. */
  private final boolean createFolders;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public PathToFolderIndexRecords(final DynamoDbService dbService) {
    this(dbService, false);
  }

  /**
   * constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param createMissingFolders boolean
   */
  public PathToFolderIndexRecords(final DynamoDbService dbService,
      final boolean createMissingFolders) {
    this.db = dbService;
    this.createFolders = createMissingFolders;
  }

  @Override
  public List<FolderIndexRecord> apply(final String siteId, final String path) {

    if (isEmpty(path)) {
      return Collections.emptyList();
    }

    String[] tokens = new StringToFolderTokens().apply(path);
    String fileToken = getFileToken(path, tokens);
    String lastUuid = "";
    List<FolderIndexRecord> records = new ArrayList<>();
    Date now = new Date();

    StringBuilder parentPath = new StringBuilder();

    for (String folder : tokens) {

      String pk = new FolderIndexRecord().parentDocumentId(lastUuid).pk(siteId);

      boolean isFile = folder.equals(fileToken);

      if (createFolders && isFile) {
        continue;
      }

      String sk = new FolderIndexRecord().path(folder).type(isFile ? "file" : "folder").sk();
      DynamoDbKey key = new DynamoDbKey(pk, sk, null, null, null, null);

      Map<String, AttributeValue> attr = this.db.get(key);

      if (!attr.isEmpty()) {
        FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, attr);
        records.add(record);
        lastUuid = record.documentId();

      } else {

        if (createFolders) {

          if (!parentPath.isEmpty()) {
            String parentPathString = parentPath.toString();
            new FolderPermissionValidate(db, ApiPermission.WRITE).apply(siteId, parentPathString);
          }

          FolderIndexRecord record = createFolder(siteId, lastUuid, folder, now);
          records.add(record);
          lastUuid = record.documentId();

        } else {
          throw new IllegalArgumentException("Cannot find folder '" + path + "'");
        }
      }

      parentPath.append(folder).append("/");
    }

    return records;
  }

  /**
   * Create Folder.
   *
   * @param siteId {@link String}
   * @param parentId {@link String}
   * @param folder {@link String}
   * @param insertedDate {@link Date}
   * @return {@link FolderIndexRecord}
   */
  private FolderIndexRecord createFolder(final String siteId, final String parentId,
      final String folder, final Date insertedDate) {

    String uuid = ID.uuid();
    String userId = ApiAuthorization.getAuthorization().getUsername();

    FolderIndexRecord record = new FolderIndexRecord().parentDocumentId(parentId).documentId(uuid)
        .insertedDate(insertedDate).lastModifiedDate(insertedDate).userId(userId).path(folder)
        .type("folder");

    AttributeValue pk = fromS(record.pk(siteId));
    AttributeValue sk = fromS(record.sk());
    DynamoDbKey key = new DynamoDbKey(pk.s(), sk.s(), null, null, null, null);

    Map<String, AttributeValue> attrs = this.db.get(key);
    if (!attrs.isEmpty()) {

      record = record.getFromAttributes(siteId, attrs);

    } else {

      boolean acquireLock = false;

      try {
        acquireLock = this.db.acquireLock(key, LOCK_ACQUIRE_TIMEOUT_IN_MS, LOCK_EXPIRATION_IN_MS);

        attrs = this.db.get(key);

        if (!attrs.isEmpty()) {

          record = record.getFromAttributes(siteId, attrs);

        } else {

          String conditionExpression = "attribute_not_exists(" + DbKeys.PK + ")";
          Put put = Put.builder().tableName(db.getTableName())
              .conditionExpression(conditionExpression).item(record.getAttributes(siteId))
              .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
              .build();

          db.putInTransaction(put);
        }

      } finally {

        if (acquireLock) {
          this.db.releaseLock(key);
        }
      }
    }

    return record;
  }

  private String getFileToken(final String path, final String[] tokens) {
    if (path.endsWith("/")) {
      return null;
    }

    return last(tokens);
  }
}
