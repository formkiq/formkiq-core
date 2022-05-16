/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  /** Documents DynamoDB Table Name. */
  public static final String DOCUMENTS_TABLE = "Documents";
  /** Cache DynamoDB Table Name. */
  public static final String CACHE_TABLE = "Cache";
  
  /** {@link DynamoDbClient}. */
  private DynamoDbClient db;
  /** {@link DocumentService}. */
  private DocumentService service;
  /** {@link ConfigService}. */
  private ConfigService configService;  
  /** {@link WebhooksService}. */
  private WebhooksService webhookService;
  /** {@link DocumentSearchService}. */
  private DocumentSearchService searchService;
  /** {@link CacheService}. */
  private CacheService cacheService;
  /** {@link String}. */
  private String documentTable;
  /** {@link String}. */
  private String cacheTable;

  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  public DynamoDbHelper(final DynamoDbConnectionBuilder builder)
      throws IOException, URISyntaxException {
    this(builder, DOCUMENTS_TABLE, CACHE_TABLE);
  }

  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentTableName {@link String}
   * @param cacheTableName {@link String}
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  public DynamoDbHelper(final DynamoDbConnectionBuilder builder, final String documentTableName,
      final String cacheTableName) throws IOException, URISyntaxException {
    this.db = builder.build();
    this.documentTable = documentTableName;
    this.cacheTable = cacheTableName;
    this.service = new DocumentServiceImpl(builder, documentTableName);
    this.cacheService = new DynamoDbCacheService(builder, cacheTableName);
    this.searchService = new DocumentSearchServiceImpl(this.service, builder, documentTableName);
    this.webhookService = new WebhooksServiceImpl(builder, documentTableName);
    this.configService = new ConfigServiceImpl(builder, documentTableName);
  }

  /**
   * Create Cache Table.
   */
  public void createCacheTable() {
    final Long capacity = Long.valueOf(10);

    KeySchemaElement pk =
        KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build();
    KeySchemaElement sk =
        KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build();

    AttributeDefinition a1 = AttributeDefinition.builder().attributeName("PK")
        .attributeType(ScalarAttributeType.S).build();

    AttributeDefinition a2 = AttributeDefinition.builder().attributeName("SK")
        .attributeType(ScalarAttributeType.S).build();

    CreateTableRequest table = CreateTableRequest.builder().tableName(this.cacheTable)
        .keySchema(pk, sk).attributeDefinitions(a1, a2).provisionedThroughput(ProvisionedThroughput
            .builder().writeCapacityUnits(capacity).readCapacityUnits(capacity).build())
        .build();

    this.db.createTable(table);
  }

  /**
   * Create Documents Table.
   */
  public void createDocumentsTable() {
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

    CreateTableRequest table = CreateTableRequest.builder().tableName(this.documentTable)
        .keySchema(pk, sk).attributeDefinitions(a1, a2, a3, a4, a5, a6)
        .globalSecondaryIndexes(si1, si2).provisionedThroughput(ProvisionedThroughput.builder()
            .writeCapacityUnits(capacity).readCapacityUnits(capacity).build())
        .build();

    this.db.createTable(table);
  }

  /**
   * Find All Documents.
   *
   * @param token {@link PaginationMapToken}
   * @param maxPageSize int
   * @return {@link PaginationResults} {@link DocumentItemDynamoDb}
   */
  private PaginationResults<String> findRecords(final PaginationMapToken token,
      final int maxPageSize) {

    ScanRequest sr = ScanRequest.builder().tableName(this.documentTable)
        .limit(Integer.valueOf(maxPageSize)).build();

    ScanResponse result = this.db.scan(sr);

    List<String> documents = result.items().stream().filter(i -> {
      String s = i.get("SK").s();
      return "document".equals(s) || "ocr#".equals(s);
    }).map(i -> i.get("PK").s()).collect(Collectors.toList());

    return new PaginationResults<String>(documents, token);
  }

  /**
   * Get {@link CacheService}.
   * 
   * @return {@link CacheService}
   */
  public CacheService getCacheService() {
    return this.cacheService;
  }

  /**
   * Get {@link ConfigService}.
   * @return {@link ConfigService}
   */
  public ConfigService getConfigService() {
    return this.configService;
  }

  /**
   * Get {@link DynamoDbClient}.
   * 
   * @return {@link DynamoDbClient}
   */
  public DynamoDbClient getDb() {
    return this.db;
  }

  /**
   * Get Document Item Count.
   *
   * @return int
   */
  public int getDocumentItemCount() {
    final int maxresults = 1000;

    ScanRequest sr = ScanRequest.builder().tableName(this.documentTable)
        .limit(Integer.valueOf(maxresults)).build();

    ScanResponse result = this.db.scan(sr);
    return result.count().intValue();
  }

  /**
   * Get Document Table Name.
   * @return {@link String}
   */
  public String getDocumentTable() {
    return this.documentTable;
  }

  /**
   * Get {@link DocumentSearchService}.
   * 
   * @return {@link DocumentSearchService}
   */
  public DocumentSearchService getSearchService() {
    return this.searchService;
  }

  /**
   * Get {@link DocumentService}.
   *
   * @return {@link DocumentService}
   */
  public DocumentService getService() {
    return this.service;
  }
  
  /**
   * Get {@link WebhooksService}.
   * 
   * @return {@link WebhooksService}
   */
  public WebhooksService getWebhookService() {
    return this.webhookService;
  }
  
  /**
   * Is Documents Table Exist.
   * 
   * @return boolean
   */
  public boolean isCacheTableExists() {
    try {
      return this.db
          .describeTable(DescribeTableRequest.builder().tableName(this.cacheTable).build())
          .table() != null;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }
  
  /**
   * Is Documents Table Exist.
   * 
   * @return boolean
   */
  public boolean isDocumentsTableExists() {
    try {
      return this.db
          .describeTable(DescribeTableRequest.builder().tableName(this.documentTable).build())
          .table() != null;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  /**
   * Truncate Config.
   */
  public void truncateConfig() {
    final int maxresults = 1000;
    ScanRequest sr = ScanRequest.builder().tableName(this.documentTable)
        .limit(Integer.valueOf(maxresults)).build();
    ScanResponse result = this.db.scan(sr);
    
    List<Map<String, AttributeValue>> list =
        result.items().stream().filter(i -> i.get("PK").s().contains(DbKeys.PREFIX_CONFIG))
            .map(m -> Map.of("PK", m.get("PK"), "SK", m.get("SK"))).collect(Collectors.toList());
    
    list.forEach(i -> this.db.deleteItem(DeleteItemRequest.builder().tableName(this.documentTable)
        .key(i).build()));
  }

  /**
   * Truncate Document Dates.
   * 
   */
  public void truncateDocumentDates() {
    final int maxresults = 1000;
    ScanRequest sr = ScanRequest.builder().tableName(this.documentTable)
        .limit(Integer.valueOf(maxresults)).build();
    ScanResponse result = this.db.scan(sr);
    
    List<Map<String, AttributeValue>> list =
        result.items().stream().filter(i -> i.get("PK").s().equals(DbKeys.PREFIX_DOCUMENT_DATE))
            .collect(Collectors.toList());
    
    list.forEach(i -> this.db.deleteItem(DeleteItemRequest.builder().tableName(this.documentTable)
        .key(i).build()));
  }

  /**
   * Truncate Documents Table.
   * 
   */
  public void truncateDocumentsTable() {
    final int maxPageSize = 100;
    PaginationMapToken startkey = null;

    while (true) {

      PaginationResults<String> results = findRecords(startkey, maxPageSize);

      for (String documentId : results.getResults()) {

        String siteId = SiteIdKeyGenerator.getSiteId(documentId);
        String pk = SiteIdKeyGenerator.resetDatabaseKey(siteId, documentId);
        String id = SiteIdKeyGenerator.getDeliminator(pk, 1);
        this.service.deleteDocument(siteId, id);
      }

      startkey = results.getToken();

      if (results.getResults().isEmpty()) {
        break;
      }
    }
  }
  
  /**
   * Truncate Webhooks.
   */
  public void truncateWebhooks() {
    final int maxresults = 1000;
    ScanRequest sr = ScanRequest.builder().tableName(this.documentTable)
        .limit(Integer.valueOf(maxresults)).build();
    ScanResponse result = this.db.scan(sr);
    
    List<Map<String, AttributeValue>> list =
        result.items().stream().filter(i -> i.get("PK").s().contains(DbKeys.PREFIX_WEBHOOK))
            .map(m -> Map.of("PK", m.get("PK"), "SK", m.get("SK"))).collect(Collectors.toList());
    
    list.forEach(i -> this.db.deleteItem(DeleteItemRequest.builder().tableName(this.documentTable)
        .key(i).build()));
  }
}
