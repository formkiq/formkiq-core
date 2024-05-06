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
package com.formkiq.stacks.dynamodb.attributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


/**
 * DynamoDB implementation for {@link AttributeService}.
 */
public class AttributeServiceDynamodb implements AttributeService, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   * 
   */
  public AttributeServiceDynamodb(final DynamoDbService dbService) {
    this.db = dbService;
  }

  @Override
  public Collection<ValidationError> addAttribute(final String siteId, final String key,
      final AttributeDataType dataType) {

    Collection<ValidationError> errors = Collections.emptyList();

    if (key == null || key.isEmpty()) {
      errors = Arrays.asList(new ValidationErrorImpl().key("key").error("'key' is required"));
    } else {

      AttributeRecord a =
          new AttributeRecord().documentId(key).key(key).type(AttributeType.STANDARD)
              .dataType(dataType != null ? dataType : AttributeDataType.STRING);
      this.db.putItem(a.getAttributes(siteId));
    }

    return errors;
  }

  @Override
  public Collection<ValidationError> deleteAttribute(final String siteId, final String key) {
    AttributeRecord r = new AttributeRecord().documentId(key);
    Map<String, AttributeValue> dbKey = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    boolean deleted = this.db.deleteItem(dbKey);
    return !deleted ? Arrays.asList(new ValidationErrorImpl().key("key").error("'key' not found"))
        : Collections.emptyList();
  }

  @Override
  public PaginationResults<AttributeRecord> findAttributes(final String siteId,
      final PaginationMapToken token, final int limit) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);
    QueryConfig config = new QueryConfig().indexName(DbKeys.GSI1).scanIndexForward(Boolean.TRUE);
    AttributeValue pk = AttributeValue.fromS("attr#");
    AttributeValue sk = AttributeValue.fromS("attr#");
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startkey, limit);

    List<Map<String, AttributeValue>> keys =
        response.items().stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    List<Map<String, AttributeValue>> attrs = this.db.getBatch(new BatchGetConfig(), keys);

    List<AttributeRecord> list =
        attrs.stream().map(a -> new AttributeRecord().getFromAttributes(siteId, a)).toList();

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(response));
  }

  @Override
  public AttributeRecord getAttribute(final String siteId, final String key) {

    AttributeRecord r = new AttributeRecord().documentId(key);
    Map<String, AttributeValue> attrs = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    if (!attrs.isEmpty()) {
      r = r.getFromAttributes(siteId, attrs);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public Map<String, AttributeRecord> getAttributes(final String siteId,
      final Collection<String> attributeKeys) {

    List<Map<String, AttributeValue>> keys =
        attributeKeys.stream().map(key -> new AttributeRecord().documentId(key))
            .map(a -> Map.of(PK, a.fromS(a.pk(siteId)), SK, a.fromS(a.sk()))).distinct().toList();

    List<Map<String, AttributeValue>> values = this.db.getBatch(new BatchGetConfig(), keys);

    return values.stream().map(a -> new AttributeRecord().getFromAttributes(siteId, a))
        .collect(Collectors.toMap(a -> a.getKey(), a -> a));
  }
}
