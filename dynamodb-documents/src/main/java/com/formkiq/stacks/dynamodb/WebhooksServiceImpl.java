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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/** Implementation of the {@link WebhooksService}. */
public class WebhooksServiceImpl implements WebhooksService, DbKeys {

  /** MilliSeconds per Second. */
  private static final int MILLISECONDS = 1000;
  /** Documents Table Name. */
  private String documentTableName;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df;
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public WebhooksServiceImpl(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = connection.build();
    this.documentTableName = documentsTable;
    this.df = DateUtil.getIsoDateFormatter();
  }

  @Override
  public void addTags(final String siteId, final String webhookId,
      final Collection<DocumentTag> tags, final Date ttl) {

    if (tags != null) {

      DocumentTagToAttributeValueMap mapper =
          new DocumentTagToAttributeValueMap(this.df, PREFIX_WEBHOOK, siteId, webhookId);

      List<List<Map<String, AttributeValue>>> valueList =
          tags.stream().map(mapper).collect(Collectors.toList());

      if (ttl != null) {
        valueList.forEach(l -> l.forEach(v -> addTimeToLive(v, ttl)));
      }

      List<Put> putitems = valueList.stream().flatMap(List::stream)
          .map(values -> Put.builder().tableName(this.documentTableName).item(values).build())
          .collect(Collectors.toList());

      List<TransactWriteItem> writes = putitems.stream()
          .map(i -> TransactWriteItem.builder().put(i).build()).collect(Collectors.toList());

      if (!writes.isEmpty()) {
        this.dbClient
            .transactWriteItems(TransactWriteItemsRequest.builder().transactItems(writes).build());
      }
    }
  }

  private void addTimeToLive(final Map<String, AttributeValue> values, final Date datettl) {
    long timeout = datettl.getTime() / MILLISECONDS;
    values.put("TimeToLive", AttributeValue.builder().n(String.valueOf(timeout)).build());
  }

  private void addTimeToLiveUpdate(final Map<String, AttributeValueUpdate> values,
      final Date datettl) {
    long timeout = datettl.getTime() / MILLISECONDS;
    values.put("TimeToLive", AttributeValueUpdate.builder()
        .value(AttributeValue.builder().n(String.valueOf(timeout)).build()).build());
  }

  @Override
  public void deleteWebhook(final String siteId, final String id) {

    deleteWebhookTags(siteId, id, null);

    Map<String, AttributeValue> key = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");
    this.dbClient
        .deleteItem(DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build());
  }

  private void deleteWebhookTags(final String siteId, final String id,
      final PaginationMapToken token) {

    PaginationResults<DynamicObject> tags = findTags(siteId, id, token);

    for (DynamicObject t : tags.getResults()) {
      String pk = t.getString("PK");
      String sk = t.getString("SK");

      Map<String, AttributeValue> key = Map.of("PK", AttributeValue.builder().s(pk).build(), "SK",
          AttributeValue.builder().s(sk).build());

      this.dbClient.deleteItem(
          DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build());
    }

    if (tags.getToken() != null) {
      deleteWebhookTags(siteId, id, tags.getToken());
    }
  }

  @Override
  public DynamicObject findTag(final String siteId, final String webhookId, final String tagKey) {
    Map<String, AttributeValue> pkvalues =
        keysGeneric(siteId, PREFIX_WEBHOOK + webhookId, PREFIX_TAGS + tagKey);
    GetItemResponse response = this.dbClient
        .getItem(GetItemRequest.builder().tableName(this.documentTableName).key(pkvalues).build());
    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();
    return !response.item().isEmpty() ? transform.apply(response.item()) : null;
  }

  @Override
  public PaginationResults<DynamicObject> findTags(final String siteId, final String webhookId,
      final PaginationMapToken token) {

    String expression = PK + " = :pk and begins_with(" + SK + ", :sk)";

    Map<String, AttributeValue> values =
        queryKeys(keysGeneric(siteId, PREFIX_WEBHOOK + webhookId, PREFIX_TAGS));

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryRequest q =
        QueryRequest.builder().tableName(this.documentTableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).exclusiveStartKey(startkey).build();

    QueryResponse result = this.dbClient.query(q);
    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();

    List<DynamicObject> objs =
        result.items().stream().map(i -> transform.apply(i)).collect(Collectors.toList());

    return new PaginationResults<>(objs, new QueryResponseToPagination().apply(result));
  }

  @Override
  public DynamicObject findWebhook(final String siteId, final String id) {

    Map<String, AttributeValue> pkvalues = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");
    GetItemResponse response = this.dbClient
        .getItem(GetItemRequest.builder().tableName(this.documentTableName).key(pkvalues).build());
    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();

    DynamicObject ob = null;

    if (!response.item().isEmpty()) {
      ob = transform.apply(response.item());
      updateWebhookTimeToLive(ob);
    }

    return ob;
  }

  @Override
  public List<DynamicObject> findWebhooks(final String siteId) {
    Map<String, AttributeValue> key = queryKeys(keysGeneric(siteId, PREFIX_WEBHOOKS, null));

    String expr = GSI1_PK + " = :pk";

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(GSI1)
        .keyConditionExpression(expr).expressionAttributeValues(key).build();

    QueryResponse result = this.dbClient.query(q);

    Collection<? extends Map<String, AttributeValue>> keys = result.items().stream()
        .map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).collect(Collectors.toList());

    List<DynamicObject> retlist = Collections.emptyList();

    if (!keys.isEmpty()) {

      Map<String, KeysAndAttributes> items =
          Map.of(this.documentTableName, KeysAndAttributes.builder().keys(keys).build());

      BatchGetItemResponse batch =
          this.dbClient.batchGetItem(BatchGetItemRequest.builder().requestItems(items).build());

      Map<String, List<Map<String, AttributeValue>>> responses = batch.responses();

      List<Map<String, AttributeValue>> list = responses.get(this.documentTableName);

      AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();

      retlist = list.stream().map(m -> transform.apply(m)).collect(Collectors.toList());
      retlist.forEach(ob -> updateWebhookTimeToLive(ob));
    }

    return retlist;
  }

  @Override
  public String saveWebhook(final String siteId, final String name, final String userId,
      final Date ttl, final String enabled) {

    final String id = UUID.randomUUID().toString();
    final String fulldate = this.df.format(new Date());

    Map<String, AttributeValue> pkvalues = keysGeneric(siteId, PREFIX_WEBHOOK + id, "webhook");

    addS(pkvalues, "documentId", id);
    addS(pkvalues, "path", name);
    addS(pkvalues, "userId", userId);
    addS(pkvalues, "inserteddate", fulldate);
    addS(pkvalues, "enabled", enabled);

    addS(pkvalues, GSI1_PK, createDatabaseKey(siteId, PREFIX_WEBHOOKS));
    addS(pkvalues, GSI1_SK, name + TAG_DELIMINATOR + fulldate);

    if (ttl != null) {
      long timeout = ttl.getTime() / MILLISECONDS;
      addN(pkvalues, "TimeToLive", String.valueOf(timeout));
    }

    PutItemRequest put =
        PutItemRequest.builder().tableName(this.documentTableName).item(pkvalues).build();

    this.dbClient.putItem(put);

    return id;
  }

  @Override
  public void updateTimeToLive(final String siteId, final String webhookId, final Date ttl) {


    Map<String, AttributeValue> key = keysGeneric(siteId, PREFIX_WEBHOOK + webhookId, "webhook");

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    addTimeToLiveUpdate(values, ttl);

    this.dbClient.updateItem(UpdateItemRequest.builder().tableName(this.documentTableName).key(key)
        .attributeUpdates(values).build());

    PaginationResults<DynamicObject> result = findTags(siteId, webhookId, null);
    for (DynamicObject ob : result.getResults()) {

      key = new HashMap<>();

      key.put(PK, AttributeValue.builder().s(ob.getString(PK)).build());
      key.put(SK, AttributeValue.builder().s(ob.getString(SK)).build());

      this.dbClient.updateItem(UpdateItemRequest.builder().tableName(this.documentTableName)
          .key(key).attributeUpdates(values).build());
    }
  }

  @Override
  public void updateWebhook(final String siteId, final String webhookId, final DynamicObject obj) {
    Map<String, AttributeValue> key = keysGeneric(siteId, PREFIX_WEBHOOK + webhookId, "webhook");

    Map<String, AttributeValueUpdate> values = new HashMap<>();

    if (obj.containsKey("name")) {
      values.put("path", AttributeValueUpdate.builder()
          .value(AttributeValue.builder().s(obj.getString("name")).build()).build());
    }

    if (obj.containsKey("enabled")) {
      values.put("enabled", AttributeValueUpdate.builder()
          .value(AttributeValue.builder().s(obj.getString("enabled")).build()).build());
    }

    if (obj.containsKey("TimeToLive")) {
      Date datettl = obj.getDate("TimeToLive");
      addTimeToLiveUpdate(values, datettl);
    }

    if (!values.isEmpty()) {
      this.dbClient.updateItem(UpdateItemRequest.builder().tableName(this.documentTableName)
          .key(key).attributeUpdates(values).build());
    }
  }

  private void updateWebhookTimeToLive(final DynamicObject ob) {
    String epoch = ob.getString("TimeToLive");

    if (epoch != null) {
      long dateL = Long.parseLong(epoch) * MILLISECONDS;
      if (new Date().getTime() > dateL) {
        ob.put("enabled", "false");
      }

      ob.put("ttl", this.df.format(new Date(dateL)));
    }
  }
}
