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

import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Soft {@link DeleteDocumentQuery}.
 */
public class SoftDeleteDocumentQuery extends DeleteDocumentQuery {

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public SoftDeleteDocumentQuery(final DocumentArtifact documentArtifact) {
    super(documentArtifact, false);
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    var key = new DocumentRecordBuilder().document(getDocument()).buildKey(siteId);
    // var pk = DynamoDbTypes.toString(keysDocument(siteId, getDocumentId()).get(PK));
    var builder = DynamoDbQueryBuilder.builder().pk(key.pk()).limit(limit);
    return builder.build(tableName);
  }

  @Override
  public boolean deleteItems(final DynamoDbService db, final String tableName, final String siteId,
      final QueryResult result) {
    return db.moveItems(result.items(),
        new DocumentDeleteMoveAttributeFunction(siteId, getDocument()));
  }
}
