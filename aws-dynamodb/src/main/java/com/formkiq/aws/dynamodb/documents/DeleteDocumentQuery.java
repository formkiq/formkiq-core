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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DeleteResults;
import com.formkiq.aws.dynamodb.DynamoDbDeleteQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.folders.PathToFolderIndexRecords;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.objects.Objects.last;

/**
 * {@link DynamoDbDeleteQuery} to delete a document.
 */
public class DeleteDocumentQuery implements DynamoDbDeleteQuery, DbKeys {

  /** Document Id. */
  private final String id;
  /** {@link DeleteSoftDeletedDocumentQuery}. */
  private final DeleteSoftDeletedDocumentQuery deleteSoft;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   */
  public DeleteDocumentQuery(final String documentId) {
    this(documentId, true);
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   * @param deleteSoftDeletedDocumentQuery {@link DeleteSoftDeletedDocumentQuery}
   */
  public DeleteDocumentQuery(final String documentId,
      final boolean deleteSoftDeletedDocumentQuery) {
    this.id = documentId;
    this.deleteSoft =
        deleteSoftDeletedDocumentQuery ? new DeleteSoftDeletedDocumentQuery(documentId) : null;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    var pk = DynamoDbTypes.toString(keysDocument(siteId, this.id).get(PK));
    return DynamoDbQueryBuilder.builder().pk(pk).limit(limit).build(tableName);
  }

  @Override
  public DeleteResults delete(final DynamoDbService db, final String tableName,
      final String siteId) {
    DeleteResults result0 = DynamoDbDeleteQuery.super.delete(db, tableName, siteId);
    DeleteResults result1 = deleteSoft != null ? deleteSoft.delete(db, tableName, siteId)
        : new DeleteResults(false, Collections.emptyList());

    var attributes =
        Stream.concat(result0.attributes().stream(), result1.attributes().stream()).toList();
    return new DeleteResults(result0.deleted() || result1.deleted(), attributes);
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.id;
  }

  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {

    var result = DynamoDbDeleteQuery.super.query(db, tableName, siteId, nextToken, limit);
    List<Map<String, AttributeValue>> items = result.items();

    if (nextToken == null) {

      items = new ArrayList<>(items);

      // Verify user has write permission to folder
      var o = items.stream().filter(a -> a.get("path") != null).findFirst();
      if (o.isPresent()) {
        String path = DynamoDbTypes.toString(o.get().get("path"));
        new ValidateDocumentFolderPermissions(db, ApiPermission.DELETE).apply(siteId, path);

        var folderIndexs = new PathToFolderIndexRecords(db).apply(siteId, path);
        var folderIndex = last(folderIndexs);
        if (folderIndex != null) {
          var folderIndexKey = folderIndex.buildKey(siteId);
          items.add(folderIndexKey.toMap());
        }
      }
    }

    return new QueryResult(items, result.lastEvaluatedKey());
  }
}
