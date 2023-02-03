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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ReadRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Unit Tests for {@link ReadRequestBuilder}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
public class ReadRequestBuilderTest implements DbKeys {

  /** {@link DynamoDbService}. */
  private static DynamoDbService service;
  /** {@link DynamoDbClient}. */
  private static DynamoDbClient dbClient;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    dbClient = dynamoDbConnection.build();
    service = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
  }

  /**
   * Test paritioning keys.
   */
  @Test
  void testDocuments01() {
    // given
    final int count = 200;
    ReadRequestBuilder builder = new ReadRequestBuilder();
    Collection<Map<String, AttributeValue>> keys = new ArrayList<>();

    // when
    for (int i = 0; i < count; i++) {
      Map<String, AttributeValue> key =
          Map.of(PK, AttributeValue.fromS("data"), SK, AttributeValue.fromS("count_" + i));
      keys.add(key);
      service.putItem(key);
    }

    builder.append(DOCUMENTS_TABLE, keys);

    // when
    Map<String, List<Map<String, AttributeValue>>> items = builder.batchReadItems(dbClient);

    // then
    assertEquals(count, items.get(DOCUMENTS_TABLE).size());
  }

  @Test
  void testDocuments02() {
    // given
    final int count = 50;
    ReadRequestBuilder builder = new ReadRequestBuilder();
    Collection<Map<String, AttributeValue>> keys = new ArrayList<>();

    // when
    for (int i = 0; i < count; i++) {
      Map<String, AttributeValue> key =
          Map.of(PK, AttributeValue.fromS("data"), SK, AttributeValue.fromS("count_" + i));
      keys.add(key);
      service.putItem(key);
    }

    builder.append(DOCUMENTS_TABLE, keys);

    // when
    Map<String, List<Map<String, AttributeValue>>> items = builder.batchReadItems(dbClient);

    // then
    assertEquals(count, items.get(DOCUMENTS_TABLE).size());
  }

  /**
   * Duplicate Keys.
   */
  @Test
  void testDocuments03() {
    // given
    final int count = 10;
    ReadRequestBuilder builder = new ReadRequestBuilder();
    Collection<Map<String, AttributeValue>> keys = new ArrayList<>();
    Map<String, AttributeValue> key = null;

    // when
    for (int i = 0; i < count; i++) {
      key = Map.of(PK, AttributeValue.fromS("data"), SK, AttributeValue.fromS("count"));
      keys.add(key);
    }

    service.putItem(key);

    builder.append(DOCUMENTS_TABLE, keys);

    // when
    Map<String, List<Map<String, AttributeValue>>> items = builder.batchReadItems(dbClient);

    // then
    assertEquals(1, items.get(DOCUMENTS_TABLE).size());
  }
}
