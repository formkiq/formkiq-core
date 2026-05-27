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
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Unit Test for {@link DynamoDbKey}.
 */
class DynamoDbKeyTest {

  @Test
  void constructorNullSk() {
    DynamoDbKey key = new DynamoDbKey("pk", null, null, null, null, null);
    assertEquals(2, key.toMap().size(), "only PK and SK should be present");
    assertEquals("pk", key.toMap().get(PK).s());
    assertNull(key.toMap().get(SK).s());
  }

  @Test
  void constructorRejectsNullPk() {
    assertThrows(NullPointerException.class,
        () -> new DynamoDbKey(null, "sk", null, null, null, null));
  }

  @Test
  void fromAttributeMapHandlesMissingOptionalGsiFields() {
    Map<String, AttributeValue> attrs = new HashMap<>();
    attrs.put(PK, fromS("pk-val"));
    attrs.put(SK, fromS("sk-val"));
    // GSI fields intentionally omitted

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attrs);

    assertEquals("pk-val", key.pk());
    assertEquals("sk-val", key.sk());
    assertNull(key.gsi1Pk());
    assertNull(key.gsi1Sk());
    assertNull(key.gsi2Pk());
    assertNull(key.gsi2Sk());
  }

  @Test
  void fromAttributeMapPopulatesAllFields() {
    Map<String, AttributeValue> attrs = new HashMap<>();
    attrs.put(PK, fromS("pk-val"));
    attrs.put(SK, fromS("sk-val"));
    attrs.put(GSI1_PK, fromS("g1pk-val"));
    attrs.put(GSI1_SK, fromS("g1sk-val"));
    attrs.put(GSI2_PK, fromS("g2pk-val"));
    attrs.put(GSI2_SK, fromS("g2sk-val"));

    DynamoDbKey key = DynamoDbKey.fromAttributeMap(attrs);

    assertEquals("pk-val", key.pk());
    assertEquals("sk-val", key.sk());
    assertEquals("g1pk-val", key.gsi1Pk());
    assertEquals("g1sk-val", key.gsi1Sk());
    assertEquals("g2pk-val", key.gsi2Pk());
    assertEquals("g2sk-val", key.gsi2Sk());
  }

  @Test
  void getAttributesBuilderIncludesAllNonNullKeys() {
    DynamoDbKey key =
        new DynamoDbKey("pk-val", "sk-val", "g1pk-val", "g1sk-val", "g2pk-val", "g2sk-val");

    Map<String, AttributeValue> attrs = key.getAttributesBuilder().build();

    assertEquals(fromS("pk-val"), attrs.get("PK"));
    assertEquals(fromS("sk-val"), attrs.get("SK"));
    assertEquals(fromS("g1pk-val"), attrs.get("GSI1PK"));
    assertEquals(fromS("g1sk-val"), attrs.get("GSI1SK"));
    assertEquals(fromS("g2pk-val"), attrs.get("GSI2PK"));
    assertEquals(fromS("g2sk-val"), attrs.get("GSI2SK"));
  }

  @Test
  void toMapContainsPkAndSkOnly() {
    DynamoDbKey key = new DynamoDbKey("tenant#doc#1", "activity#123", null, null, null, null);

    Map<String, AttributeValue> map = key.toMap();

    assertEquals(fromS("tenant#doc#1"), map.get(PK));
    assertEquals(fromS("activity#123"), map.get(SK));
    assertEquals(2, map.size(), "only PK and SK should be present");
  }
}
