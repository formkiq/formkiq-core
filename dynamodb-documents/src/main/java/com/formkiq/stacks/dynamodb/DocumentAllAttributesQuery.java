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
package com.formkiq.stacks.dynamodb;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.Map;

/**
 * {@link DynamoDbQuery} for finding {@link DocumentSyncRecord} by {@link DocumentSyncStatus}.
 */
public class DocumentAllAttributesQuery implements DynamoDbQuery, DbKeys {

  /** Document Id. */
  private final String id;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public DocumentAllAttributesQuery(final String documentId) {
    this.id = documentId;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {
    Map<String, AttributeValue> key = keysDocument(siteId, this.id);
    String pk = key.get(PK).s();
    return DynamoDbQueryBuilder.builder().pk(pk).projectionExpression("PK,SK").beginsWith("attr#")
        .build(tableName);
  }
}
