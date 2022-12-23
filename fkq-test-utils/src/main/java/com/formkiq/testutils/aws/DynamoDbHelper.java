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
package com.formkiq.testutils.aws;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/** Test Helper utility class for DynamoDB. */
public class DynamoDbHelper {

  /** {@link DynamoDbClient}. */
  private DynamoDbClient db;

  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  public DynamoDbHelper(final DynamoDbConnectionBuilder builder)
      throws IOException, URISyntaxException {
    this.db = builder.build();
  }

  /**
   * Create Cache Table.
   * 
   * @param tableName {@link String}
   */
  public void createCacheTable(final String tableName) {
    final Long capacity = Long.valueOf(10);

    KeySchemaElement pk =
        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
    KeySchemaElement sk =
        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

    AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
        .attributeType(ScalarAttributeType.S).build();

    CreateTableRequest table = CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
        .attributeDefinitions(a1, a2).provisionedThroughput(ProvisionedThroughput.builder()
            .writeCapacityUnits(capacity).readCapacityUnits(capacity).build())
        .build();

    this.db.createTable(table);
  }

  /**
   * Create Documents Table.
   * 
   * @param tableName {@link String}
   */
  public void createDocumentsTable(final String tableName) {
    final Long capacity = Long.valueOf(10);

    KeySchemaElement pk =
        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
    KeySchemaElement sk =
        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

    AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a3 = AttributeDefinition.builder().attributeName("GSI1PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a4 = AttributeDefinition.builder().attributeName("GSI1SK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a5 = AttributeDefinition.builder().attributeName("GSI2PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a6 = AttributeDefinition.builder().attributeName("GSI2SK")
        .attributeType(ScalarAttributeType.S).build();

    GlobalSecondaryIndex si1 = GlobalSecondaryIndex.builder().indexName("GSI1")
        .keySchema(KeySchemaElement.builder().attributeName("GSI1PK").keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName("GSI1SK").keyType(KeyType.RANGE).build())
        .projection(Projection.builder().projectionType(ProjectionType.INCLUDE)
            .nonKeyAttributes("inserteddate", "documentId", "tagKey", "tagValue").build())
        .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(capacity)
            .readCapacityUnits(capacity).build())
        .build();

    GlobalSecondaryIndex si2 = GlobalSecondaryIndex.builder().indexName("GSI2")
        .keySchema(KeySchemaElement.builder().attributeName("GSI2PK").keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName("GSI2SK").keyType(KeyType.RANGE).build())
        .projection(Projection.builder().projectionType(ProjectionType.INCLUDE)
            .nonKeyAttributes("inserteddate", "documentId", "tagKey", "tagValue").build())
        .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(capacity)
            .readCapacityUnits(capacity).build())
        .build();

    CreateTableRequest table = CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
        .attributeDefinitions(a1, a2, a3, a4, a5, a6).globalSecondaryIndexes(si1, si2)
        .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(capacity)
            .readCapacityUnits(capacity).build())
        .build();

    this.db.createTable(table);
  }

  /**
   * Create Documents Syncs Table.
   * 
   * @param tableName {@link String}
   */
  public void createDocumentSyncsTable(final String tableName) {
    final Long capacity = Long.valueOf(10);

    KeySchemaElement pk =
        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
    KeySchemaElement sk =
        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

    AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
        .attributeType(ScalarAttributeType.S).build();

    CreateTableRequest table = CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
        .attributeDefinitions(a1, a2).provisionedThroughput(ProvisionedThroughput.builder()
            .writeCapacityUnits(capacity).readCapacityUnits(capacity).build())
        .build();

    this.db.createTable(table);
  }

  /**
   * Get Document Item Count.
   * 
   * @param tableName {@link String}
   *
   * @return int
   */
  public int getDocumentItemCount(final String tableName) {
    final int maxresults = 1000;

    ScanRequest sr =
        ScanRequest.builder().tableName(tableName).limit(Integer.valueOf(maxresults)).build();

    ScanResponse result = this.db.scan(sr);
    return result.count().intValue();
  }

  /**
   * Is Documents Table Exist.
   * 
   * @param tableName {@link String}
   * 
   * @return boolean
   */
  public boolean isTableExists(final String tableName) {
    try {
      return this.db.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
          .table() != null;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  /**
   * Truncate Documents Table.
   * 
   * @param tableName {@link String}
   * 
   */
  public void truncateTable(final String tableName) {
    final int maxPageSize = 100;
    Map<String, AttributeValue> startkey = null;

    int iterations = 0;

    while (true) {

      ScanRequest sr = ScanRequest.builder().tableName(tableName).exclusiveStartKey(startkey)
          .limit(Integer.valueOf(maxPageSize)).build();

      ScanResponse result = this.db.scan(sr);

      for (Map<String, AttributeValue> item : result.items()) {

        AttributeValue pk = item.get("PK");
        AttributeValue sk = item.get("SK");
        this.db.deleteItem(DeleteItemRequest.builder().tableName(tableName)
            .key(Map.of("PK", pk, "SK", sk)).build());
      }

      startkey = result.lastEvaluatedKey();

      if (result.items().isEmpty()) {
        break;

      }

      iterations++;

      if (iterations > maxPageSize) {
        throw new RuntimeException("endless loop break");
      }
    }
  }
}
