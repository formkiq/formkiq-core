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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Implementation of the {@link ConfigService}. */
public class ConfigServiceImpl implements ConfigService, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ConfigServiceImpl(final DynamoDbConnectionBuilder connection,
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
  public DynamicObject get(final String siteId) {

    Collection<Map<String, AttributeValue>> keys = new ArrayList<>();

    if (isDefaultSiteId(siteId)) {
      keys.add(keysGeneric(null, PREFIX_CONFIG, DEFAULT_SITE_ID));
    } else {
      keys.add(keysGeneric(null, PREFIX_CONFIG, siteId));
      keys.add(keysGeneric(null, PREFIX_CONFIG, DEFAULT_SITE_ID));
    }

    List<Map<String, AttributeValue>> list = this.db.getBatch(keys);

    Optional<Map<String, AttributeValue>> map = Optional.empty();
    if (!list.isEmpty()) {
      map = list.stream().filter(s -> s.get(SK).s().equals(siteId)).findFirst();
      if (map.isEmpty()) {
        map = list.stream().filter(s -> s.get(SK).s().equals(DEFAULT_SITE_ID)).findFirst();
      }
    }

    AttributeValueToDynamicObject transform = new AttributeValueToDynamicObject();
    return !map.isEmpty() ? transform.apply(map.get()) : new DynamicObject(Map.of());
  }

  @Override
  public void save(final String siteId, final DynamicObject obj) {
    Map<String, AttributeValue> item =
        keysGeneric(null, PREFIX_CONFIG, siteId != null ? siteId : DEFAULT_SITE_ID);

    for (Entry<String, Object> e : obj.entrySet()) {
      item.put(e.getKey(), AttributeValue.builder().s(e.getValue().toString()).build());
    }

    if (this.db.exists(item.get(PK), item.get(SK))) {
      HashMap<String, AttributeValue> fields = new HashMap<>(item);
      fields.remove(PK);
      fields.remove(SK);

      this.db.updateFields(item.get(PK), item.get(SK), fields);
    } else {
      this.db.putItem(item);
    }
  }
}
