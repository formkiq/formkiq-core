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
package com.formkiq.stacks.dynamodb.config;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.MapToAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Implementation of the {@link ConfigService}. */
public final class ConfigServiceDynamoDb implements ConfigService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ConfigServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.db = new DynamoDbServiceImpl(connection, documentsTable);
  }

  @Override
  public void delete(final String siteId) {
    String s = siteId != null ? siteId : DEFAULT_SITE_ID;
    Map<String, AttributeValue> keys = keysGeneric(null, PREFIX_CONFIG, s);
    this.db.deleteItem(keys.get(PK), keys.get(SK));
  }

  @Override
  public Map<String, Object> get(final String siteId) {

    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    String site = !isDefaultSiteId(siteId) ? siteId : DEFAULT_SITE_ID;
    keys.add(keysGeneric(null, PREFIX_CONFIG, site));

    BatchGetConfig config = new BatchGetConfig();
    List<Map<String, AttributeValue>> list = this.db.getBatch(config, keys);

    return !list.isEmpty() ? new AttributeValueToMap().apply(list.get(0)) : Map.of();
  }

  @Override
  public void save(final String siteId, final Map<String, Object> map) {
    Map<String, AttributeValue> item =
        keysGeneric(null, PREFIX_CONFIG, siteId != null ? siteId : DEFAULT_SITE_ID);

    Map<String, AttributeValue> values = new MapToAttributeValue().apply(map);
    item.putAll(values);

    if (this.db.exists(item.get(PK), item.get(SK))) {
      this.db.updateValues(item.get(PK), item.get(SK), values);
    } else {
      this.db.putItem(item);
    }
  }

  @Override
  public long increment(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    return this.db.getNextNumber(keys);
  }

  private Map<String, AttributeValue> getIncrementKey(final String siteId, final String key) {
    String pk = "configvalues";
    String sk = "key#" + key;
    return keysGeneric(siteId, PK, pk, SK, sk);
  }

  @Override
  public long getIncrement(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    Map<String, AttributeValue> values = this.db.get(keys.get(PK), keys.get(SK));
    return !values.isEmpty() ? Long.parseLong(values.get("Number").n()) : -1;
  }
}
