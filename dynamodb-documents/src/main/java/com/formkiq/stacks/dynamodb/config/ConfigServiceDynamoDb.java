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
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryConfig;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

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
  public SiteConfiguration get(final String siteId) {
    DynamoDbKey key = SiteConfiguration.builder().buildKey(siteId);
    Map<String, AttributeValue> attributes = this.db.get(key);
    return !attributes.isEmpty() ? SiteConfiguration.fromAttributeMap(attributes)
        : new SiteConfiguration(null, null, null, null, null, null, null, null, null, null, null,
            null);
  }

  @Override
  public long getIncrement(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    Map<String, AttributeValue> values = this.db.get(keys.get(PK), keys.get(SK));
    return !values.isEmpty() ? Long.parseLong(values.get("Number").n()) : -1;
  }

  private Map<String, AttributeValue> getIncrementKey(final String siteId, final String key) {
    String pk = "configvalues";
    String sk = "key#" + key;
    return keysGeneric(siteId, PK, pk, SK, sk);
  }

  @Override
  public Map<String, Long> getIncrements(final String siteId) {
    final int limit = 100;
    Map<String, AttributeValue> keys = getIncrementKey(siteId, "");
    QueryConfig config = new QueryConfig();
    QueryResponse response = this.db.queryBeginsWith(config, keys.get(PK), null, null, limit);

    Map<String, Long> map = new HashMap<>();
    response.items().forEach(i -> {
      String key = i.get(SK).s();
      key = key.substring(key.indexOf("#") + 1);
      long number = Long.parseLong(i.get("Number").n());
      map.put(key, number);
    });

    return map;
  }

  @Override
  public long increment(final String siteId, final String key) {
    Map<String, AttributeValue> keys = getIncrementKey(siteId, key);
    return this.db.getNextNumber(keys);
  }

  @Override
  public boolean save(final String siteId, final SiteConfiguration config) {

    Map<String, AttributeValue> map = config.getAttributes();

    if (!map.isEmpty()) {

      SiteConfiguration conf = SiteConfiguration.builder().build(siteId);
      DynamoDbKey key = conf.key();

      if (this.db.exists(key)) {
        map.remove(PK);
        map.remove(SK);
        this.db.updateValues(fromS(key.pk()), fromS(key.sk()), map);
      } else {
        map.putAll(key.toMap());
        this.db.putItem(map);
      }
    }

    return !map.isEmpty();
  }

}
