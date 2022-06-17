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
package com.formkiq.aws.services.lambda.services;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Implementation of the {@link ConfigService}. */
public class ConfigServiceImpl implements ConfigService, DbKeys {

  /** Config Keys. */
  private static final List<String> KEYS = Arrays.asList(DOCUMENT_TIME_TO_LIVE, MAX_WEBHOOKS,
      MAX_DOCUMENTS, MAX_DOCUMENT_SIZE_BYTES, WEBHOOK_TIME_TO_LIVE);

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
  public ConfigServiceImpl(final DynamoDbConnectionBuilder builder, final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;
  }

  @Override
  public void delete(final String siteId) {
    String s = siteId != null ? siteId : DEFAULT_SITE_ID;
    Map<String, AttributeValue> keys = keysGeneric(null, PREFIX_CONFIG, s);
    this.dynamoDB.deleteItem(
        DeleteItemRequest.builder().tableName(this.documentTableName).key(keys).build());
  }

  @Override
  public DynamicObject get(final String siteId) {

    Collection<Map<String, AttributeValue>> keys = new ArrayList<>();

    if (isDefaultSiteId(siteId)) {
      keys.add(keysGeneric(null, PREFIX_CONFIG, DEFAULT_SITE_ID));
    } else {
      keys.add(keysGeneric(null, PREFIX_CONFIG, siteId));
      keys.add(keysGeneric(null, PREFIX_CONFIG, DEFAULT_SITE_ID));
    }

    Map<String, KeysAndAttributes> items =
        Map.of(this.documentTableName, KeysAndAttributes.builder().keys(keys).build());

    BatchGetItemResponse response =
        this.dynamoDB.batchGetItem(BatchGetItemRequest.builder().requestItems(items).build());

    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();

    Optional<Map<String, AttributeValue>> map = Optional.empty();
    List<Map<String, AttributeValue>> list = response.responses().get(this.documentTableName);

    if (!list.isEmpty()) {
      map = list.stream().filter(s -> s.get(SK).s().equals(siteId)).findFirst();
      if (map.isEmpty()) {
        map = list.stream().filter(s -> s.get(SK).s().equals(DEFAULT_SITE_ID)).findFirst();
      }
    }

    return !map.isEmpty() ? transform.apply(map.get()) : new DynamicObject(Map.of());
  }

  @Override
  public void save(final String siteId, final DynamicObject obj) {
    Map<String, AttributeValue> item =
        keysGeneric(null, PREFIX_CONFIG, siteId != null ? siteId : DEFAULT_SITE_ID);

    KEYS.forEach(key -> {
      if (obj.containsKey(key)) {
        item.put(key, AttributeValue.builder().s(obj.getString(key)).build());
      }
    });

    this.dynamoDB
        .putItem(PutItemRequest.builder().tableName(this.documentTableName).item(item).build());
  }
}
