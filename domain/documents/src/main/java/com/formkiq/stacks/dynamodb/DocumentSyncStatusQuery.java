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

import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;

/**
 * {@link DynamoDbQuery} for finding {@link DocumentSyncRecord} by {@link DocumentSyncStatus}.
 */
public class DocumentSyncStatusQuery implements DynamoDbQuery {

  /** {@link DocumentSyncRecord}. */
  private final DocumentSyncRecord record;

  /**
   * constructor.
   * 
   * @param service {@link DocumentSyncServiceType}
   * @param status {@link DocumentSyncStatus}
   * @param type {@link DocumentSyncType}
   */
  public DocumentSyncStatusQuery(final DocumentSyncServiceType service,
      final DocumentSyncStatus status, final DocumentSyncType type) {
    record = new DocumentSyncRecord().setStatus(status).setService(service).setType(type);
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    String pk = record.pkGsi1(siteId);
    String sk = DocumentSyncRecord.SK_SYNCS + record.getType() + "#";

    return DynamoDbQueryBuilder.builder().scanIndexForward(Boolean.FALSE).indexName(GSI1).pk(pk)
        .beginsWith(sk).nextToken(nextToken).limit(limit).build(tableName);
  }
}
