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

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * {@link DynamoDbClient} Implementation of {@link CacheService}.
 *
 */
public class DynamoDbCacheService implements CacheService {

  /** MilliSeconds per Second. */
  private static final int MILLISECONDS = 1000;
  /** Date Format. */
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  /** Partition Key of Table. */
  private static final String PK = "PK";

  /** Sort Key of Table. */
  private static final String SK = "SK";

  /** {@link SimpleDateFormat} format. */
  private SimpleDateFormat df;

  /** Cache Table Name. */
  private String cacheTableName;
  /** UTC {@link TimeZone}. */
  private TimeZone tz = TimeZone.getTimeZone("UTC");
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param table {@link String}
   */
  public DynamoDbCacheService(final DynamoDbConnectionBuilder connection, final String table) {

    if (table == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = connection.build();
    this.cacheTableName = table;

    this.df = new SimpleDateFormat(DATE_FORMAT);
    this.df.setTimeZone(this.tz);
  }

  @Override
  public Date getExpiryDate(final String key) {

    Date date = null;
    Map<String, AttributeValue> map = getFromCache(key);

    if (map.containsKey("TimeToLive")) {
      Long ttl = Long.valueOf(map.get("TimeToLive").n());
      date = new Date(ttl.longValue() * MILLISECONDS);
    }

    return date;
  }

  /**
   * Get Expiry Time.
   * 
   * @param cacheInDays int
   * 
   * @return long
   */
  private Date getExpiryTime(final int cacheInDays) {
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(cacheInDays);
    return Date.from(now.toInstant());
  }

  /**
   * Get Cache Value.
   * 
   * @param key {@link String}
   * @return {@link Map}
   */
  private Map<String, AttributeValue> getFromCache(final String key) {
    Map<String, AttributeValue> keyMap = new HashMap<>();
    keyMap.put(PK, AttributeValue.builder().s(key).build());
    keyMap.put(SK, AttributeValue.builder().s("cache").build());

    GetItemRequest r = GetItemRequest.builder().tableName(this.cacheTableName).key(keyMap).build();

    Map<String, AttributeValue> result = this.dbClient.getItem(r).item();
    return result;
  }

  @Override
  public String read(final String key) {
    Map<String, AttributeValue> result = getFromCache(key);
    return !result.isEmpty() ? result.get("Data").s() : null;
  }

  @Override
  public void write(final String key, final String data, final int cacheInDays) {

    Date now = new Date();
    long timeout = getExpiryTime(cacheInDays).getTime() / MILLISECONDS;

    String fulldate = this.df.format(now);

    Map<String, AttributeValue> pkvalues = new HashMap<String, AttributeValue>();
    pkvalues.put(PK, AttributeValue.builder().s(key).build());
    pkvalues.put(SK, AttributeValue.builder().s("cache").build());
    pkvalues.put("InsertedDate", AttributeValue.builder().s(fulldate).build());
    pkvalues.put("TimeToLive", AttributeValue.builder().n(String.valueOf(timeout)).build());
    pkvalues.put("Data", AttributeValue.builder().s(data).build());

    PutItemRequest putItemRequest =
        PutItemRequest.builder().tableName(this.cacheTableName).item(pkvalues).build();

    this.dbClient.putItem(putItemRequest);
  }
}
