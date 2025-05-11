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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DynamoDbQueryBuilder}.
 */
public class DynamoDbQueryBuilderTest {

  @Test
  void testPkOnly() {
    QueryRequest req = DynamoDbQueryBuilder.builder().pk("val").build("MyTable");

    assertEquals("MyTable", req.tableName());
    assertNull(req.indexName());
    assertEquals("#PK = :PK", req.keyConditionExpression());
    assertEquals(Map.of("#PK", "PK"), req.expressionAttributeNames());
    assertEquals(Map.of(":PK", AttributeValue.builder().s("val").build()),
        req.expressionAttributeValues());
    assertTrue(req.exclusiveStartKey().isEmpty());
    assertEquals("10", req.limit().toString());
  }

  @Test
  void testPkAndEqSk() {
    QueryRequest req = DynamoDbQueryBuilder.builder().pk("p").eq("s").build("T");

    assertEquals("#PK = :PK AND #SK = :SK", req.keyConditionExpression());
    assertEquals(Map.of("#PK", "PK", "#SK", "SK"), req.expressionAttributeNames());
    assertEquals(Map.of(":PK", AttributeValue.builder().s("p").build(), ":SK",
        AttributeValue.builder().s("s").build()), req.expressionAttributeValues());
  }

  @Test
  void testBetweenSk() {
    QueryRequest req = DynamoDbQueryBuilder.builder().pk("p").betweenSK("low", "high").build("T");

    assertEquals("#PK = :PK AND #SK BETWEEN :SK_low AND :SK_high", req.keyConditionExpression());
    Map<String, AttributeValue> values = req.expressionAttributeValues();

    final int expected = 3;
    assertEquals(expected, values.size());
    assertEquals("p", values.get(":PK").s());
    assertEquals("low", values.get(":SK_low").s());
    assertEquals("high", values.get(":SK_high").s());
  }

  @Test
  void testBeginsWith() {
    QueryRequest req = DynamoDbQueryBuilder.builder().pk("p").beginsWith("prefix").build("T");

    assertEquals("#PK = :PK AND begins_with(#SK,:SK)", req.keyConditionExpression());
    assertTrue(req.expressionAttributeNames().containsKey("#SK"));
    assertTrue(req.expressionAttributeValues().containsKey(":SK"));
  }

  @Test
  void testLimitParsing() {
    QueryRequest req1 = DynamoDbQueryBuilder.builder().limit("5").build("T");
    assertEquals("5", req1.limit().toString());

    QueryRequest req2 = DynamoDbQueryBuilder.builder().limit("bad").build("T");
    assertEquals("10", req2.limit().toString());

    QueryRequest req3 = DynamoDbQueryBuilder.builder().limit("0").build("T");
    assertEquals("10", req3.limit().toString());
  }

  @Test
  void testIndexNameMapping() {
    QueryRequest req = DynamoDbQueryBuilder.builder().indexName("GSI1").pk("p").eq("s").build("T");

    assertEquals("GSI1", req.indexName());
    assertEquals("GSI1PK", req.expressionAttributeNames().get("#PK"));
    assertEquals("GSI1SK", req.expressionAttributeNames().get("#SK"));
  }
}
