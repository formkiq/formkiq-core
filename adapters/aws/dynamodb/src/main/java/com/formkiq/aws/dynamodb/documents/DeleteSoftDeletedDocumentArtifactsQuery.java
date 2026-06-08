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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbDeleteQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.List;
import java.util.Map;

/**
 * {@link DynamoDbDeleteQuery} for soft-deleted document artifacts.
 */
public class DeleteSoftDeletedDocumentArtifactsQuery implements DynamoDbDeleteQuery, DbKeys {

  /** Sort key prefix for soft-deleted artifacts. */
  private static final String SOFT_DELETED_ARTIFACT_PREFIX = "softdelete#document#art#";

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public DeleteSoftDeletedDocumentArtifactsQuery(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    var key = new DocumentRecordBuilder().document(this.document).buildSoftDeleteKey(siteId);
    DynamoDbQueryBuilder builder =
        DynamoDbQueryBuilder.builder().pk(key.pk()).nextToken(nextToken).limit(limit);

    if (this.document.artifactId() != null) {
      builder.eq(key.sk());
    } else {
      builder.beginsWith(SOFT_DELETED_ARTIFACT_PREFIX);
    }

    return builder.build(tableName).toBuilder().consistentRead(Boolean.TRUE).build();
  }

  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {

    QueryResult result = DynamoDbDeleteQuery.super.query(db, tableName, siteId, nextToken, limit);
    List<Map<String, AttributeValue>> items = result.items().stream()
        .filter(i -> this.document.documentId().equals(DynamoDbTypes.toString(i.get("documentId"))))
        .filter(i -> this.document.artifactId() == null
            || this.document.artifactId().equals(DynamoDbTypes.toString(i.get("artifactId"))))
        .toList();

    return new QueryResult(items, result.lastEvaluatedKey());
  }
}
