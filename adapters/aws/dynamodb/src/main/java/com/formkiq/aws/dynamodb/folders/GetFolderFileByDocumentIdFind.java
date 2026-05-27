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

import com.formkiq.aws.dynamodb.DynamoDbFind;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;


import static com.formkiq.aws.dynamodb.DbKeys.GSI1;

/**
 * {@link DynamoDbFind} for finding {@link FolderIndexRecord} by DocumentId.
 */
public class GetFolderFileByDocumentIdFind implements DynamoDbFind<FolderIndexRecord, String> {

  private QueryRequest build(final String tableName, final String siteId, final String documentId) {

    var shardKey = new FolderIndexRecord().parentDocumentId("").documentId(documentId)
        .type(FolderType.FILE.getValue()).path("").buildKey(siteId);
    var key = shardKey.key();

    return DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk()).eq(key.gsi1Sk()).limit(1)
        .build(tableName);
  }

  @Override
  public FolderIndexRecord find(final DynamoDbService db, final String tableName,
      final String siteId, final String documentId) {

    QueryRequest query = build(tableName, siteId, documentId);
    var queryItems = db.query(query).items();

    return !queryItems.isEmpty()
        ? new FolderIndexRecord().getFromAttributes(siteId, queryItems.get(0))
        : null;
  }
}
