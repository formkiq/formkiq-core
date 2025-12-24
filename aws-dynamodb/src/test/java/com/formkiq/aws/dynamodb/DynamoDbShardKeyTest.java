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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Unit Test for {@link DynamoDbShardKey}.
 */
class DynamoDbShardKeyTest {

  @Test
  void fromAttributeMapReadsBaseKeyAndShards() {
    Map<String, AttributeValue> attrs = new HashMap<>();
    attrs.put(PK, fromS("pk-val"));
    attrs.put(SK, fromS("sk-val"));
    attrs.put(DbKeys.GSI1_PK, fromS("g1pk-val"));
    attrs.put(DbKeys.GSI1_SK, fromS("g1sk-val"));
    attrs.put(DbKeys.GSI2_PK, fromS("g2pk-val"));
    attrs.put(DbKeys.GSI2_SK, fromS("g2sk-val"));

    attrs.put(PK + "shard", fromS("s01"));
    attrs.put(GSI1_PK + "shard", fromS("s02"));
    attrs.put(GSI2_PK + "shard", fromS("s03"));

    DynamoDbShardKey shardKey = DynamoDbShardKey.fromAttributeMap(attrs);

    // base key
    assertEquals("pk-val", shardKey.key().pk());
    assertEquals("sk-val", shardKey.key().sk());
    assertEquals("g1pk-val", shardKey.key().gsi1Pk());
    assertEquals("g1sk-val", shardKey.key().gsi1Sk());
    assertEquals("g2pk-val", shardKey.key().gsi2Pk());
    assertEquals("g2sk-val", shardKey.key().gsi2Sk());

    // shards
    assertEquals("s01", shardKey.pkShard());
    assertEquals("s02", shardKey.pkGsi1Shard());
    assertEquals("s03", shardKey.pkGsi2Shard());
  }

  @Test
  void getAttributesBuilderAddsShardAttributes() {
    DynamoDbKey baseKey =
        new DynamoDbKey("pk-val", "sk-val", "g1pk-val", "g1sk-val", "g2pk-val", "g2sk-val");

    DynamoDbShardKey shardKey = new DynamoDbShardKey(baseKey, "s01", "s02", "s03");

    Map<String, AttributeValue> attrs = shardKey.getAttributesBuilder().build();

    // base key attributes
    assertEquals(fromS("pk-val"), attrs.get("PK"));
    assertEquals(fromS("sk-val"), attrs.get("SK"));

    // shard attributes
    assertEquals(fromS("s01"), attrs.get("PKshard"));
    assertEquals(fromS("s02"), attrs.get("GSI1PKshard"));
    assertEquals(fromS("s03"), attrs.get("GSI2PKshard"));
  }

  @Test
  void toMapIncludesShardAttributesWhenPkShardIsNotNull() {
    DynamoDbKey baseKey =
        new DynamoDbKey("pk-val", "sk-val", "g1pk-val", "g1sk-val", "g2pk-val", "g2sk-val");

    DynamoDbShardKey shardKey = new DynamoDbShardKey(baseKey, "s01", "s02", "s03");

    Map<String, AttributeValue> map = shardKey.toMap();

    // base key values
    assertEquals(fromS("pk-val"), map.get(PK));
    assertEquals(fromS("sk-val"), map.get(SK));

    // shard attributes
    assertEquals(fromS("s01"), map.get(PK + "shard"));
    assertEquals(fromS("s02"), map.get(GSI1_PK + "shard"));
    assertEquals(fromS("s03"), map.get(GSI2_PK + "shard"));
  }

  @Test
  void toMapOmitsShardAttributesWhenPkShardIsNull() {
    DynamoDbKey baseKey =
        new DynamoDbKey("pk-val", "sk-val", "g1pk-val", "g1sk-val", "g2pk-val", "g2sk-val");

    // pkShard null → all shard attrs should be omitted by toMap()
    DynamoDbShardKey shardKey = new DynamoDbShardKey(baseKey, null, null, null);

    Map<String, AttributeValue> map = shardKey.toMap();

    assertEquals(fromS("pk-val"), map.get(PK));
    assertEquals(fromS("sk-val"), map.get(SK));

    assertFalse(map.containsKey(PK + "shard"));
    assertFalse(map.containsKey(GSI1_PK + "shard"));
    assertFalse(map.containsKey(GSI2_PK + "shard"));
  }
}
