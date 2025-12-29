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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.base64.Pagination;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDb implementation of {@link ApiKeysService}.
 *
 */
public final class ApiKeysServiceDynamoDb implements ApiKeysService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

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
      final Collection<ApiKeyPermission> permissions, final Collection<String> groups) {

    String apiKey = Strings.generateRandomString(ApiKey.API_KEY_LENGTH);
    String userId = ApiAuthorization.getAuthorization().getUsername();

    ApiKey key = ApiKey.builder().apiKey(apiKey).name(name).userId(userId).insertedDate(new Date())
        .permissions(permissions).groups(groups).build(siteId);

    this.db.putItem(key.getAttributes());
    return apiKey;
  }

  @Override
  public boolean deleteApiKey(final String siteId, final String apiKey) {

    boolean deleted = false;
    DynamoDbKey key = ApiKey.builder().apiKey(apiKey).name("").buildKey(siteId);

    QueryConfig config = new QueryConfig().indexName(GSI2);

    QueryResponse response =
        this.db.queryBeginsWith(config, fromS(key.gsi2Pk()), fromS(key.gsi2Sk()), null, 2);

    if (!response.items().isEmpty()) {
      Map<String, AttributeValue> map = response.items().get(0);
      deleted = this.db.deleteItem(map.get(PK), map.get(SK));
    }

    return deleted;
  }

  @Override
  public ApiKey get(final String apiKey, final boolean masked) {

    ApiKey key = null;
    DynamoDbKey k = ApiKey.builder().apiKey(apiKey != null ? apiKey : "").name("").buildKey(null);

    Map<String, AttributeValue> map = this.db.get(fromS(k.pk()), fromS(k.sk()));

    if (!map.isEmpty()) {
      key = ApiKey.fromAttributeMap(map);

      if (masked) {
        key = new ApiKey(key.key(), key.siteId(), mask(key.apiKey()), key.name(), key.userId(),
            key.insertedDate(), key.permissions(), key.groups());
      }
    }

    return key;
  }

  @Override
  public Pagination<ApiKey> list(final String siteId, final String nextToken, final int limit) {

    DynamoDbKey key = ApiKey.builder().apiKey("").name("").buildKey(siteId);
    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);

    AttributeValue pk = fromS(key.gsi1Pk());
    AttributeValue sk = fromS("apikey" + TAG_DELIMINATOR);

    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startKey, limit);

    List<Map<String, AttributeValue>> attrs = response.items().stream()
        .map(m -> Map.of(PK, m.get(PK), SK, m.get(SK))).collect(Collectors.toList());

    BatchGetConfig batchConfig = new BatchGetConfig();
    List<ApiKey> apiKeys = this.db.getBatch(batchConfig, attrs).stream()
        .map(ApiKey::fromAttributeMap).map(a -> new ApiKey(a.key(), a.siteId(), mask(a.apiKey()),
            a.name(), a.userId(), a.insertedDate(), a.permissions(), a.groups()))
        .collect(Collectors.toList());

    return new Pagination<>(apiKeys, response.lastEvaluatedKey());
  }

  @Override
  public String mask(final String apiKey) {
    return ApiKey.mask(apiKey);
  }
}
