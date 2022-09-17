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

import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
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

  /**
   * constructor.
   * 
   * @param documentsTable {@link String}
   */
  public DocumentCountServiceDynamoDb(final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.documentTableName = documentsTable;
  }

  @Override
  public long getDocumentCount(final DynamoDbClient client, final String siteId) {

    String pk = getPk(siteId);
    String sk = "all";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder().s(pk).build());
    values.put(":sk", AttributeValue.builder().s(sk).build());

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression("PK = :pk and SK = :sk").expressionAttributeValues(values).build();

    QueryResponse result = client.query(q);
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
    return siteId != null ? "documentscount" + TAG_DELIMINATOR + siteId
        : "documentcount" + TAG_DELIMINATOR + "default";
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
  public void incrementDocumentCount(final DynamoDbClient client, final String siteId) {

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

    client.updateItem(utr);
  }

  @Override
  public void removeDocumentCount(final DynamoDbClient client, final String siteId) {

    Map<String, AttributeValue> key = getPkAttributeMap(siteId);

    DeleteItemRequest deleteItemRequest =
        DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

    client.deleteItem(deleteItemRequest);
  }
}
