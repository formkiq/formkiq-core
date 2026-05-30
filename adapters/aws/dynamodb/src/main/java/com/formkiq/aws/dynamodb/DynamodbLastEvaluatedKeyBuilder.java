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
package com.formkiq.aws.dynamodb;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Builds a DynamoDB LastEvaluatedKey map from an item and index name.
 */
public class DynamodbLastEvaluatedKeyBuilder implements DbKeys {

  /** Item {@link AttributeValue}. */
  private final Map<String, AttributeValue> item;
  /** Index Name. */
  private final String index;

  /**
   * constructor.
   * 
   * @param map {@link Map}
   * @param indexName {@link String}
   */
  public DynamodbLastEvaluatedKeyBuilder(final Map<String, AttributeValue> map,
      final String indexName) {
    this.item = map;
    this.index = indexName;
  }

  /**
   * Build the LastEvaluatedKey for the given item and index.
   * <ul>
   * <li>PK, SK are always included</li>
   * <li>GSI1: PK, SK, GSI1PK, GSI1SK</li>
   * <li>GSI2: PK, SK, GSI2PK, GSI2SK</li>
   * <li>Other / null: PK, SK only</li>
   * </ul>
   *
   * @param isShardKey is key shard.
   *
   * @return map suitable for QueryRequest#exclusiveStartKey or ScanRequest#exclusiveStartKey
   */
  public Map<String, AttributeValue> build(final boolean isShardKey) {

    Map<String, AttributeValue> m = null;

    if (item != null) {

      m = new HashMap<>();

      copyIfPresent(m, item, PK, isShardKey);
      copyIfPresent(m, item, SK, false);

      if (GSI1.equals(index)) {
        copyIfPresent(m, item, GSI1_PK, isShardKey);
        copyIfPresent(m, item, GSI1_SK, false);
      } else if (GSI2.equals(index)) {
        copyIfPresent(m, item, GSI2_PK, isShardKey);
        copyIfPresent(m, item, GSI2_SK, false);
      }
    }

    return m;
  }

  private void copyIfPresent(final Map<String, AttributeValue> target,
      final Map<String, AttributeValue> source, final String key, final boolean isShardKey) {
    AttributeValue value = source.get(key);
    if (value != null) {
      if (isShardKey) {
        String s = DynamoDbShardKey.removeShardSuffix(value.s());
        target.put(key, fromS(s));
      } else {
        target.put(key, value);
      }
    }
  }
}
