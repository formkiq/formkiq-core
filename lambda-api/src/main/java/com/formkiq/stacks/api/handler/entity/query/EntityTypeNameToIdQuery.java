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
package com.formkiq.stacks.api.handler.entity.query;

import com.formkiq.aws.dynamodb.DynamoDbFind;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;

/**
 * {@link DynamoDbQuery} for Converting an EntityTypeName to EntityTypeId.
 */
public class EntityTypeNameToIdQuery implements DynamoDbFind<String, EntityTypeRecord> {

  /** {@link ValidationBuilder}. */
  private final ValidationBuilder vb;

  /**
   * constructor.
   */
  public EntityTypeNameToIdQuery() {
    vb = new ValidationBuilder();
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId,
      final EntityTypeRecord record) {

    validateNamespace(record);

    DynamoDbKey key = EntityTypeRecord.builder().documentId("").name(record.documentId())
        .namespace(record.namespace()).buildKey(siteId);
    return DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk()).beginsWith(key.gsi1Sk())
        .limit("1").build(tableName);
  }

  @Override
  public String find(final DynamoDbService db, final String tableName, final String siteId,
      final EntityTypeRecord record) {

    String id = record.documentId();
    if (!Strings.isUuid(id)) {
      QueryRequest query = build(tableName, siteId, record);
      Map<String, AttributeValue> attributes = db.getByQuery(query);
      vb.isRequired("entityTypeId", !attributes.isEmpty() && attributes.containsKey("documentId"),
          "'entityTypeId' attribute is not found");
      vb.check();

      id = attributes.get("documentId").s();
    }

    return id;
  }

  private void validateNamespace(final EntityTypeRecord record) {

    vb.isRequired("entityTypeId", record.documentId());
    vb.isRequired("namespace", record.namespace());
    vb.check();

    vb.isEquals("namespace", record.namespace(), "CUSTOM");
    vb.check();
  }
}
