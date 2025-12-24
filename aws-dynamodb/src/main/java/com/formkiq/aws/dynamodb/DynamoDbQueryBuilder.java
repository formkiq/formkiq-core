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
import java.util.StringJoiner;

import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * Builder for DynamoDB QueryRequest, supporting primary and GSI queries with key conditions (eq,
 * between) on PK, SK, GSI1PK, GSI1SK, GSI2PK, GSI2SK.
 */
public class DynamoDbQueryBuilder implements DbKeys {
  /** Default Max Results. */
  private static final int MAX_RESULTS = 10;

  /**
   * Create a new builder.
   * 
   * @return builder instance
   */
  public static DynamoDbQueryBuilder builder() {
    return new DynamoDbQueryBuilder();
  }

  /** Index Name. */
  private String indexName;
  /** Projection Expression. */
  private String projectionExpression;
  /** {@link Map} of Expression Attribute Names. */
  private final Map<String, String> expressionAttributeNames = new HashMap<>();
  /** {@link Map} of Expression Attribute Values. */
  private final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
  /** Key Conditions. */
  private final StringJoiner keyConditions = new StringJoiner(" AND ");
  /** {@link Map} Start Key. */
  private Map<String, AttributeValue> startKey;
  /** Query limit. */
  private int limit = MAX_RESULTS;

  /** Scan Index Forward. */
  private Boolean scanIndexForward;

  /**
   * constructor.
   */
  private DynamoDbQueryBuilder() {}

  private String addName(final String name) {
    String placeholder = "#" + name;
    expressionAttributeNames.put(placeholder, name);
    return placeholder;
  }

  private String addValue(final String name, final AttributeValue value) {
    String placeholder = ":" + name;
    expressionAttributeValues.put(placeholder, value);
    return placeholder;
  }

  private void applyShardSuffix(final Map<String, AttributeValue> map, final String key,
      final String shard) {
    String val = DynamoDbTypes.toString(map.get(key));
    if (!isEmpty(val)) {
      map.put(key, fromS(DynamoDbShardKey.addShardSuffix(val, shard)));
    }
  }

  /**
   * Adds BEGINS_WITH condition on SK.
   *
   * @param value {@link String}
   * @return this builder
   */
  public DynamoDbQueryBuilder beginsWith(final String value) {
    String nameKey = addName(SK);
    String val = addValue("SK", AttributeValue.builder().s(value).build());
    keyConditions.add("begins_with(" + nameKey + "," + val + ")");
    return this;
  }

  /**
   * Adds a BETWEEN condition on SK.
   * 
   * @param low lower bound
   * @param high upper bound
   * @return this builder
   */
  public DynamoDbQueryBuilder betweenSK(final String low, final String high) {
    String nameKey = addName(SK);
    String lowKey = addValue("SK_low", AttributeValue.builder().s(low).build());
    String highKey = addValue("SK_high", AttributeValue.builder().s(high).build());
    keyConditions.add(nameKey + " BETWEEN " + lowKey + " AND " + highKey);
    return this;
  }

  /**
   * Builds the {@link QueryRequest}.
   *
   * @param tableName Sets the table name to query.
   * @return configured QueryRequest
   */
  public QueryRequest build(final String tableName) {

    if (indexName != null) {
      expressionAttributeNames.forEach((k, v) -> expressionAttributeNames.put(k, indexName + v));
    }

    QueryRequest.Builder req =
        QueryRequest.builder().tableName(tableName).keyConditionExpression(keyConditions.toString())
            .expressionAttributeNames(expressionAttributeNames)
            .projectionExpression(projectionExpression).scanIndexForward(scanIndexForward)
            .expressionAttributeValues(expressionAttributeValues).exclusiveStartKey(startKey)
            .limit(limit);

    if (indexName != null) {
      req = req.indexName(indexName);
    }

    return req.build();
  }

  /**
   * Adds an equality condition on SK.
   *
   * @param value sort key value
   * @return this builder
   */
  public DynamoDbQueryBuilder eq(final String value) {
    String nameKey = addName(SK);
    String valKey = addValue(SK, AttributeValue.builder().s(value).build());
    keyConditions.add(nameKey + " = " + valKey);
    return this;
  }

  /**
   * Sets the GSI index name to query; omit for primary index.
   * 
   * @param dbIndexName GSI name
   * @return this builder
   */
  public DynamoDbQueryBuilder indexName(final String dbIndexName) {
    this.indexName = dbIndexName;
    return this;
  }

  /**
   * Sets the GSI index name to query; omit for primary index.
   *
   * @param dbIndexName GSI name
   * @param key {@link DynamoDbKey}
   * @return this builder
   */
  public DynamoDbQueryBuilder indexName(final String dbIndexName, final DynamoDbKey key) {
    indexName(dbIndexName);

    if (GSI1.equals(dbIndexName)) {
      pk(key.gsi1Pk());
    } else if (GSI2.equals(dbIndexName)) {
      pk(key.gsi2Pk());
    } else {
      pk(key.pk());
    }

    return this;
  }

  /**
   * Set Start Key from Next token.
   *
   * @param s {@link String}
   * @return this builder
   */
  public DynamoDbQueryBuilder limit(final String s) {

    if (s != null) {
      try {
        this.limit = Integer.parseInt(s);
      } catch (NumberFormatException e) {
        this.limit = MAX_RESULTS;
      }
    }

    if (limit < 1) {
      this.limit = MAX_RESULTS;
    }

    return this;
  }

  /**
   * Set Query results limit.
   *
   * @param resultsLimit int
   * @return this builder
   */
  public DynamoDbQueryBuilder limit(final int resultsLimit) {
    this.limit = resultsLimit;
    return this;
  }

  /**
   * Add an <= condition on SK.
   * 
   * @param value sort key value
   * @return this builder
   */
  public DynamoDbQueryBuilder lte(final String value) {
    String nameKey = addName(SK);
    String valKey = addValue(SK, AttributeValue.builder().s(value).build());
    keyConditions.add(nameKey + " <= " + valKey);
    return this;
  }


  /**
   * Set Start Key from Next token.
   *
   * @param skey {@link DynamoDbShardKey}
   * @param nextToken {@link String}
   * @return this builder
   */
  public DynamoDbQueryBuilder nextToken(final DynamoDbShardKey skey, final String nextToken) {

    startKey = new StringToMapAttributeValue().apply(nextToken);

    if (startKey != null) {
      applyShardSuffix(this.startKey, PK, skey.pkShard());
      applyShardSuffix(this.startKey, GSI1_PK, skey.pkGsi1Shard());
      applyShardSuffix(this.startKey, GSI2_PK, skey.pkGsi2Shard());
    }

    return this;
  }

  /**
   * Set Start Key from Next token.
   * 
   * @param exclusiveStartKey {@link Map}
   * @return this builder
   */
  public DynamoDbQueryBuilder nextToken(final Map<String, AttributeValue> exclusiveStartKey) {
    startKey = exclusiveStartKey;
    return this;
  }

  /**
   * Set Start Key from Next token.
   *
   * @param nextToken {@link String}
   * @return this builder
   */
  public DynamoDbQueryBuilder nextToken(final String nextToken) {
    startKey = new StringToMapAttributeValue().apply(nextToken);
    return this;
  }

  /**
   * Adds an equality condition on PK. <<<<<<< HEAD
   *
   * @param value partition key value
   * 
   * @return this builder
   */
  public DynamoDbQueryBuilder pk(final String value) {
    String nameKey = addName(PK);
    String valKey = addValue(PK, AttributeValue.builder().s(value).build());
    keyConditions.add(nameKey + " = " + valKey);
    return this;
  }

  /**
   * Sets the GSI index name to query; omit for primary index.
   *
   * @param queryProjectionExpression {@link String}
   * @return this builder
   */
  public DynamoDbQueryBuilder projectionExpression(final String queryProjectionExpression) {
    this.projectionExpression = queryProjectionExpression;
    return this;
  }

  /**
   * Sets Scan Index Forward.
   * 
   * @param scanForward {@link Boolean}
   * @return DynamoDbQueryBuilder
   */
  public DynamoDbQueryBuilder scanIndexForward(final Boolean scanForward) {
    this.scanIndexForward = scanForward;
    return this;
  }
}

