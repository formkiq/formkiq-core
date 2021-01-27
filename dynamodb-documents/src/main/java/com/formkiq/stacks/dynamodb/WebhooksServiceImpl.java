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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.formkiq.stacks.common.objects.DynamicObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/** Implementation of the {@link WebhooksService}. */
public class WebhooksServiceImpl implements WebhooksService, DbKeys {

  /** Documents Table Name. */
  private String documentTableName;

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dynamoDB;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;
  
  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public WebhooksServiceImpl(final DynamoDbConnectionBuilder builder, final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;
    this.df = new SimpleDateFormat(DocumentService.DATE_FORMAT);
  }

  @Override
  public void deleteWebhook(final String siteId, final String id) {
    Map<String, AttributeValue> key = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");
    this.dynamoDB
        .deleteItem(DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build());
  }

  @Override
  public DynamicObject findWebhook(final String siteId, final String id) {

    Map<String, AttributeValue> pkvalues = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");
    GetItemResponse response =
        this.dynamoDB.getItem(
            GetItemRequest.builder().tableName(this.documentTableName).key(pkvalues).build());
    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();
    return !response.item().isEmpty() ? transform.apply(response.item()) : null;
  }

  @Override
  public List<DynamicObject> findWebhooks(final String siteId) {
    Map<String, AttributeValue> key = queryKeys(keysGeneric(siteId, PREFIX_WEBHOOKS, null));
    
    String expr = GSI1_PK + " = :pk";
    
    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(GSI1)
        .keyConditionExpression(expr).expressionAttributeValues(key).build();

    QueryResponse result = this.dynamoDB.query(q);
    
    Collection<? extends Map<String, AttributeValue>> keys = result.items().stream()
        .map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).collect(Collectors.toList());    
        
    if (!keys.isEmpty()) {
      
      Map<String, KeysAndAttributes> items =
          Map.of(this.documentTableName, KeysAndAttributes.builder().keys(keys).build());

      BatchGetItemResponse batch =
          this.dynamoDB.batchGetItem(BatchGetItemRequest.builder().requestItems(items).build());

      Map<String, List<Map<String, AttributeValue>>> responses = batch.responses();

      List<Map<String, AttributeValue>> list = responses.get(this.documentTableName);

      AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();

      return list.stream().map(m -> transform.apply(m)).collect(Collectors.toList());
    }
    
    return Collections.emptyList();
  }

  @Override
  public String saveWebhook(final String siteId, final String name, final String userId) {

    final String id = UUID.randomUUID().toString();
    final String fulldate = this.df.format(new Date());
    
    Map<String, AttributeValue> pkvalues = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");
    
    addS(pkvalues, "documentId", id);
    addS(pkvalues, "path", name);
    addS(pkvalues, "userId", userId);
    addS(pkvalues, "inserteddate", fulldate);
    
    addS(pkvalues, GSI1_PK, createDatabaseKey(siteId, PREFIX_WEBHOOKS));
    addS(pkvalues, GSI1_SK, name + TAG_DELIMINATOR + fulldate);
    
    PutItemRequest put =
        PutItemRequest.builder().tableName(this.documentTableName).item(pkvalues).build();

    this.dynamoDB.putItem(put);
    
    return id;
  }
}