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

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.AttributeValueToDynamicObject;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDb implementation of {@link ApiKeysService}.
 *
 */
public class ApiKeysServiceDynamoDb implements ApiKeysService, DbKeys {

  /** Api Key Length. */
  private static final int API_KEY_LENGTH = 51;
  /** API Query Limit. */
  private static final int LIMIT = 100;
  /** Mask Value, must be even number. */
  private static final int MASK = 8;

  /**
   * Generate Random String.
   * 
   * @param len int
   * @return {@link String}
   */
  private static String generateRandomString(final int len) {
    final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      int randomIndex = random.nextInt(chars.length());
      sb.append(chars.charAt(randomIndex));
    }

    return sb.toString();
  }

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ApiKeysServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.db = new DynamoDbServiceImpl(connection, documentsTable);
  }

  @Override
  public String createApiKey(final String siteId, final String name) {
    String apiKey = generateRandomString(API_KEY_LENGTH);
    Map<String, AttributeValue> keys = getKeys(siteId, apiKey);
    keys.put("apiKey", AttributeValue.fromS(apiKey));
    keys.put("name", AttributeValue.fromS(name));
    keys.put("insertedDate", AttributeValue.fromS(this.df.format(new Date())));
    this.db.putItem(keys);
    return apiKey;
  }

  @Override
  public void deleteApiKey(final String siteId, final String apiKey) {
    Map<String, AttributeValue> keys = getKeys(siteId, apiKey);

    QueryConfig config = new QueryConfig().projectionExpression("PK,SK");

    String apiKeyStart = apiKey.substring(0, MASK);
    String apiKeyEnd = apiKey.substring(apiKey.length() - MASK / 2);

    QueryResponse response = this.db.queryBeginsWith(config, keys.get(PK),
        AttributeValue.fromS(PREFIX_API_KEY + apiKeyStart), null, LIMIT);

    response.items().forEach(i -> {
      if (i.get(SK).s().endsWith(apiKeyEnd)) {
        this.db.deleteItem(i.get(PK), i.get(SK));
      }
    });

  }

  private Map<String, AttributeValue> getKeys(final String siteId, final String apiKey) {
    return keysGeneric(siteId, PREFIX_API_KEYS, PREFIX_API_KEY + apiKey);
  }

  @Override
  public boolean isApiKeyValid(final String siteId, final String apiKey) {
    Map<String, AttributeValue> keys = getKeys(siteId, apiKey);
    return this.db.exists(keys.get(PK), keys.get(SK));
  }

  @Override
  public List<DynamicObject> list(final String siteId) {
    Map<String, AttributeValue> keys = getKeys(siteId, "");
    QueryResponse response = this.db.query(keys.get(PK), null, LIMIT);

    List<DynamicObject> list =
        response.items().stream().map(new AttributeValueToDynamicObject()).map(o -> {
          String apiKey = mask(o.getString("apiKey"));
          o.put("apiKey", apiKey);
          o.remove(PK);
          o.remove(SK);
          return o;
        }).collect(Collectors.toList());

    return list;
  }

  @Override
  public String mask(final String apiKey) {
    return apiKey.subSequence(0, MASK) + "****************"
        + apiKey.substring(apiKey.length() - MASK / 2);
  }
}
