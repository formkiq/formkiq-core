/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.dynamodb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Service for keeping track of number of documents.
 *
 */
public class DocumentCountServiceDynamoDb implements DocumentCountService {

  /** Documents Table Name. */
  private String documentTableName;

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dynamoDB;

  /**
   * constructor.
   * 
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public DocumentCountServiceDynamoDb(final DynamoDbConnectionBuilder builder,
      final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;
  }

  @Override
  public long getDocumentCount(final String siteId) {

    String pk = getPk(siteId);
    String sk = "all";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder().s(pk).build());
    values.put(":sk", AttributeValue.builder().s(sk).build());

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression("PK = :pk and SK = :sk").expressionAttributeValues(values).build();

    QueryResponse result = this.dynamoDB.query(q);
    List<Map<String, AttributeValue>> items = result.items();

    long documentscount = 0;

    if (!items.isEmpty()) {
      String value = items.get(0).get("MetricValue").n();
      documentscount = Long.parseLong(value);
    }

    return documentscount;
  }

  /**
   * get Document Count PK.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  private String getPk(final String siteId) {
    return siteId != null ? "documentscount_" + siteId : "documentcount_default";
  }

  /**
   * Get PK {@link AttributeValue} {@link Map}.
   * 
   * @param siteId {@link String}
   * @return {@link Map}
   */
  private Map<String, AttributeValue> getPkAttributeMap(final String siteId) {
    String pk = getPk(siteId);
    String sk = "all";

    Map<String, AttributeValue> key = new HashMap<>();
    key.put("PK", AttributeValue.builder().s(pk).build());
    key.put("SK", AttributeValue.builder().s(sk).build());
    return key;
  }

  @Override
  public void incrementDocumentCount(final String siteId) {

    Map<String, String> expAttrs = new HashMap<>();
    expAttrs.put("#val", "MetricValue");

    Map<String, AttributeValue> expVals = new HashMap<>();
    expVals.put(":incr", AttributeValue.builder().n("1").build());
    expVals.put(":zero", AttributeValue.builder().n("0").build());

    String updateExpression = "SET #val = if_not_exists(#val, :zero) + :incr";

    Map<String, AttributeValue> key = getPkAttributeMap(siteId);

    UpdateItemRequest utr = UpdateItemRequest.builder().tableName(this.documentTableName).key(key)
        .updateExpression(updateExpression).expressionAttributeNames(expAttrs)
        .expressionAttributeValues(expVals).build();

    this.dynamoDB.updateItem(utr);
  }

  @Override
  public void removeDocumentCount(final String siteId) {

    Map<String, AttributeValue> key = getPkAttributeMap(siteId);

    DeleteItemRequest deleteItemRequest =
        DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

    this.dynamoDB.deleteItem(deleteItemRequest);
  }
}
