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

import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDb implementation of {@link ApiKeysService}.
 *
 */
public class ApiKeysServiceDynamoDb implements ApiKeysService, DbKeys {

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
  public String createApiKey(final String siteId, final String name,
      final Collection<ApiKeyPermission> permissions, final String userId) {

    String apiKey = generateRandomString(ApiKey.API_KEY_LENGTH);

    ApiKey key = new ApiKey().apiKey(apiKey).name(name).insertedDate(new Date()).userId(userId)
        .permissions(permissions).siteId(siteId);

    this.db.putItem(key.getAttributes(siteId));
    return apiKey;
  }

  @Override
  public boolean deleteApiKey(final String siteId, final String apiKey) {

    boolean deleted = false;
    ApiKey key = new ApiKey().apiKey(apiKey);

    QueryConfig config = new QueryConfig().indexName(GSI2);

    QueryResponse response =
        this.db.queryBeginsWith(config, fromS(key.pkGsi2(siteId)), fromS(key.skGsi2()), null, 2);

    if (!response.items().isEmpty()) {
      Map<String, AttributeValue> map = response.items().get(0);
      deleted = this.db.deleteItem(map.get(PK), map.get(SK));
    }

    return deleted;
  }

  @Override
  public ApiKey get(final String apiKey, final boolean masked) {

    String k = apiKey != null ? apiKey : "";
    ApiKey key = new ApiKey().apiKey(k);

    Map<String, AttributeValue> map = this.db.get(fromS(key.pk(null)), fromS(key.sk()));

    if (!map.isEmpty()) {
      key = new ApiKey().getFromAttributes(null, map);

      if (masked) {
        key.apiKey(mask(key.apiKey()));
      }

    } else {
      key = null;
    }

    return key;
  }

  @Override
  public PaginationResults<ApiKey> list(final String siteId, final PaginationMapToken token,
      final int limit) {

    ApiKey apiKey = new ApiKey().siteId(siteId);
    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);

    AttributeValue pk = fromS(apiKey.pkGsi1(siteId));
    AttributeValue sk = fromS("apikey" + TAG_DELIMINATOR);

    Map<String, AttributeValue> startKey = new PaginationToAttributeValue().apply(token);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startKey, limit);

    List<Map<String, AttributeValue>> attrs = response.items().stream()
        .map(m -> Map.of(PK, m.get(PK), SK, m.get(SK))).collect(Collectors.toList());

    BatchGetConfig batchConfig = new BatchGetConfig();
    List<ApiKey> apiKeys = this.db.getBatch(batchConfig, attrs).stream()
        .map(a -> new ApiKey().getFromAttributes(siteId, a)).map(a -> a.apiKey(mask(a.apiKey())))
        .collect(Collectors.toList());

    return new PaginationResults<ApiKey>(apiKeys, new QueryResponseToPagination().apply(response));
  }

  @Override
  public String mask(final String apiKey) {
    return new ApiKey().apiKey(apiKey).mask();
  }
}
