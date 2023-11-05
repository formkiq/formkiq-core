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
package com.formkiq.aws.dynamodb.schema;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;

/**
 * DocumentSchema.
 */
public class DocumentSchema {

  /** DynamoDb Capacity. */
  private final Long capacity = Long.valueOf(100);
  /** {@link DynamoDbClient}. */
  private DynamoDbClient db;

  /**
   * constructor.
   * 
   * @param dbClient {@link DynamoDbClient}
   */
  public DocumentSchema(final DynamoDbClient dbClient) {
    this.db = dbClient;
  }

  /**
   * Create Cache Table.
   * 
   * @param tableName {@link String}
   */
  public void createCacheTable(final String tableName) {

    if (!isTableExists(tableName)) {
      KeySchemaElement pk =
          KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
      KeySchemaElement sk =
          KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

      AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
          .attributeType(ScalarAttributeType.S).build();

      AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
          .attributeType(ScalarAttributeType.S).build();

      CreateTableRequest table =
          CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
              .attributeDefinitions(a1, a2)
              .provisionedThroughput(ProvisionedThroughput.builder()
                  .writeCapacityUnits(this.capacity).readCapacityUnits(this.capacity).build())
              .build();

      this.db.createTable(table);
    }
  }

  /**
   * Create Documents Table.
   * 
   * @param tableName {@link String}
   * @return {@link CreateTableResponse}
   */
  public CreateTableResponse createDocumentsTable(final String tableName) {

    CreateTableResponse response = null;

    if (!isTableExists(tableName)) {
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
          .keySchema(
              KeySchemaElement.builder().attributeName("GSI1PK").keyType(KeyType.HASH).build(),
              KeySchemaElement.builder().attributeName("GSI1SK").keyType(KeyType.RANGE).build())
          .projection(Projection.builder().projectionType(ProjectionType.INCLUDE)
              .nonKeyAttributes("inserteddate", "documentId", "tagKey", "tagValue").build())
          .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(this.capacity)
              .readCapacityUnits(this.capacity).build())
          .build();

      GlobalSecondaryIndex si2 = GlobalSecondaryIndex.builder().indexName("GSI2")
          .keySchema(
              KeySchemaElement.builder().attributeName("GSI2PK").keyType(KeyType.HASH).build(),
              KeySchemaElement.builder().attributeName("GSI2SK").keyType(KeyType.RANGE).build())
          .projection(Projection.builder().projectionType(ProjectionType.INCLUDE)
              .nonKeyAttributes("inserteddate", "documentId", "tagKey", "tagValue").build())
          .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(this.capacity)
              .readCapacityUnits(this.capacity).build())
          .build();

      CreateTableRequest table = CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
          .attributeDefinitions(a1, a2, a3, a4, a5, a6).globalSecondaryIndexes(si1, si2)
          .streamSpecification(StreamSpecification.builder().streamEnabled(Boolean.TRUE)
              .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES).build())
          .provisionedThroughput(ProvisionedThroughput.builder().writeCapacityUnits(this.capacity)
              .readCapacityUnits(this.capacity).build())
          .build();

      response = this.db.createTable(table);
    }

    return response;
  }

  /**
   * Create Documents Syncs Table.
   * 
   * @param tableName {@link String}
   */
  public void createDocumentSyncsTable(final String tableName) {

    if (!isTableExists(tableName)) {
      KeySchemaElement pk =
          KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
      KeySchemaElement sk =
          KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

      AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
          .attributeType(ScalarAttributeType.S).build();

      AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
          .attributeType(ScalarAttributeType.S).build();

      CreateTableRequest table =
          CreateTableRequest.builder().tableName(tableName).keySchema(pk, sk)
              .attributeDefinitions(a1, a2)
              .provisionedThroughput(ProvisionedThroughput.builder()
                  .writeCapacityUnits(this.capacity).readCapacityUnits(this.capacity).build())
              .build();

      this.db.createTable(table);
    }
  }

  /**
   * Is Documents Table Exist.
   * 
   * @param tableName {@link String}
   * 
   * @return boolean
   */
  private boolean isTableExists(final String tableName) {
    try {
      return this.db.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
          .table() != null;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }
}
