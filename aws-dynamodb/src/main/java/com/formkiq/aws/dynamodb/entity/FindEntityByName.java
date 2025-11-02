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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.DynamoDbFind;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


import static com.formkiq.aws.dynamodb.DbKeys.GSI1;

/**
 * Find Entity by Name.
 */
public class FindEntityByName implements DynamoDbFind<EntityRecord, FindEntityByName.EntityName> {

  public record EntityName(String entityTypeId, String name) {
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId,
      final FindEntityByName.EntityName record) {

    DynamoDbKey key = EntityRecord.builder().entityTypeId(record.entityTypeId()).documentId("1")
        .name(record.name()).buildKey(siteId);

    return DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith("entity#" + record.name()).limit(1).build(tableName);
  }

  @Override
  public EntityRecord find(final DynamoDbService db, final String tableName, final String siteId,
      final FindEntityByName.EntityName record) {
    QueryRequest query = build(tableName, siteId, record);
    QueryResponse response = db.query(query, true);
    return response.items().isEmpty() ? null
        : EntityRecord.fromAttributeMap(response.items().get(0));
  }
}
