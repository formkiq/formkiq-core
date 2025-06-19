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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.eventsourcing.DynamoDbKey;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityTypeRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;

public class EntityTypeNameToIdTransformer {

  /**
   * Find Entity Type Id.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param entityTypeId {@link String}
   * @param namespace {@link String}
   * @return String
   * @throws ValidationException ValidationException
   */
  public String findEntityTypeId(final AwsServiceCache awsservice, final String siteId,
      final String entityTypeId, final String namespace) throws ValidationException {

    String id = entityTypeId;
    validateNamespace(namespace);

    String tableName = awsservice.environment("DOCUMENTS_TABLE");
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);

    DynamoDbKey key = EntityTypeRecord.builder().documentId("").name(entityTypeId)
        .namespace(namespace).buildKey(siteId);
    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).limit("1").build(tableName);
    QueryResponse response = db.query(q);
    if (!response.items().isEmpty()) {
      id = response.items().get(0).get("documentId").s();
    }

    return id;
  }

  private void validateNamespace(final String namespace) throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("namespace", namespace);
    vb.check();

    vb.isEquals("namespace", namespace, "CUSTOM");
    vb.check();
  }
}
