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
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.documents.DocumentDeleteMoveAttributeFunction.SOFT_DELETE;

/**
 * {@link DynamoDbDeleteQuery} to delete a soft deleted document.
 */
public class DeleteSoftDeletedDocumentQuery implements DynamoDbDeleteQuery, DbKeys {

  /** Document Id. */
  private final String id;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public DeleteSoftDeletedDocumentQuery(final String documentId) {
    this.id = documentId;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    var pk = SOFT_DELETE + DynamoDbTypes.toString(keysDocument(siteId, this.id).get(PK));
    return DynamoDbQueryBuilder.builder().pk(pk).limit(limit).build(tableName);
  }

  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {

    var result = DynamoDbDeleteQuery.super.query(db, tableName, siteId, nextToken, limit);
    List<Map<String, AttributeValue>> items = new ArrayList<>(result.items());

    // delete document softdelete record
    String pk = keysGeneric(siteId, SOFT_DELETE + PREFIX_DOCS, null).get(PK).s();
    String sk = SOFT_DELETE + "document#" + this.id;
    var key = new DynamoDbKey(pk, sk, null, null, null, null);
    Map<String, AttributeValue> attr = db.get(key);
    if (!attr.isEmpty()) {
      items.add(attr);
    }

    return new QueryResult(items, result.lastEvaluatedKey());
  }
}
