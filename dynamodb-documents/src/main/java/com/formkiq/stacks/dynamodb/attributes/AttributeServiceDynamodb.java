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

import java.util.ArrayList;
import java.util.Collection;
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
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributeKeyRecord;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;


/**
 * DynamoDB implementation for {@link AttributeService}.
 */
public class AttributeServiceDynamodb implements AttributeService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

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
      final AttributeDataType dataType, final AttributeType type) {
    return addAttribute(siteId, key, dataType, type, false);
  }

  @Override
  public Collection<ValidationError> addAttribute(final String siteId, final String key,
      final AttributeDataType dataType, final AttributeType type,
      final boolean allowReservedAttributeKey) {
    return addAttribute(siteId, key, dataType, type, allowReservedAttributeKey, null);
  }

  private Collection<ValidationError> addAttribute(final String siteId, final String key,
      final AttributeDataType dataType, final AttributeType type,
      final boolean allowReservedAttributeKey, final Watermark watermark) {

    Collection<ValidationError> errors =
        validate(siteId, key, dataType, allowReservedAttributeKey, watermark);

    if (errors.isEmpty()) {

      AttributeRecord a = new AttributeRecord().documentId(key).key(key)
          .type(type != null ? type : AttributeType.STANDARD)
          .setWatermarkText(watermark != null ? watermark.getText() : null)
          .dataType(dataType != null ? dataType : AttributeDataType.STRING);

      this.db.putItem(a.getAttributes(siteId));
    }

    return errors;
  }

  @Override
  public Collection<ValidationError> addWatermarkAttribute(final String siteId, final String key,
      final Watermark watermark) {
    return addAttribute(siteId, key, AttributeDataType.WATERMARK, AttributeType.STANDARD, false,
        watermark);
  }

  private Collection<ValidationError> validate(final String siteId, final String key,
      final AttributeDataType dataType, final boolean allowReservedAttributeKey,
      final Watermark watermark) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmpty(key)) {
      errors.add(new ValidationErrorImpl().key("key").error("'key' is required"));
    } else if (!allowReservedAttributeKey) {

      AttributeKeyReserved r = AttributeKeyReserved.find(key);
      if (r != null) {
        errors.add(new ValidationErrorImpl().key("key")
            .error("'" + key + "' is a reserved attribute name"));
      }
    }

    if (errors.isEmpty()) {
      AttributeRecord attribute = getAttribute(siteId, key);
      if (attribute != null) {
        errors.add(
            new ValidationErrorImpl().key("key").error("attribute '" + key + "' already exists"));
      }
    }

    if (AttributeDataType.WATERMARK.equals(dataType)) {
      if (watermark == null || isEmpty(watermark.getText())) {
        errors.add(
            new ValidationErrorImpl().key("watermark.text").error("'watermark.text' is required"));
      }
    } else if (watermark != null) {
      errors.add(new ValidationErrorImpl().key("watermark.text")
          .error("'watermark' only allowed on dataType 'WATERMARK'"));
    }

    return errors;
  }

  @Override
  public Collection<ValidationError> deleteAttribute(final String siteId, final String key) {

    boolean deleted = false;
    Collection<ValidationError> errors = validateDeleteAttribute(siteId, key);

    if (errors.isEmpty()) {

      AttributeRecord r = new AttributeRecord().documentId(key);
      deleted = this.db.deleteItem(Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk())));
    }

    if (!deleted && errors.isEmpty()) {
      errors.add(new ValidationErrorImpl().key("key").error("attribute 'key' not found"));
    }

    return errors;
  }

  private Collection<ValidationError> validateDeleteAttribute(final String siteId,
      final String key) {
    Collection<ValidationError> errors = new ArrayList<>();

    QueryConfig config = new QueryConfig().indexName(GSI1);

    // check for Schema / Classification Key
    SchemaAttributeKeyRecord r = new SchemaAttributeKeyRecord().setKey(key);
    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    QueryResponse response = this.db.queryBeginsWith(config, pk, null, null, 1);
    if (!response.items().isEmpty()) {
      errors.add(new ValidationErrorImpl().error(
          "attribute '" + key + "' is used in a Schema / Classification, cannot be deleted"));
    }

    // check for DocumentAttributeRecords
    DocumentAttributeRecord dar = new DocumentAttributeRecord().setKey(key);
    pk = dar.fromS(dar.pkGsi1(siteId));
    response = this.db.queryBeginsWith(config, pk, null, null, 1);

    if (!response.items().isEmpty()) {
      errors.add(
          new ValidationErrorImpl().error("attribute '" + key + "' is in use, cannot be deleted"));
    }

    return errors;
  }

  @Override
  public PaginationResults<AttributeRecord> findAttributes(final String siteId,
      final PaginationMapToken token, final int limit) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);
    QueryConfig config = new QueryConfig().indexName(DbKeys.GSI1).scanIndexForward(Boolean.TRUE);
    AttributeValue pk = AttributeValue.fromS(createDatabaseKey(siteId, "attr#"));
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
  public boolean existsAttribute(final String siteId, final String key) {
    AttributeRecord r = new AttributeRecord().documentId(key);
    return this.db.exists(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
  }

  @Override
  public Map<String, AttributeRecord> getAttributes(final String siteId,
      final Collection<String> attributeKeys) {

    List<Map<String, AttributeValue>> keys =
        attributeKeys.stream().map(key -> new AttributeRecord().documentId(key))
            .map(a -> Map.of(PK, a.fromS(a.pk(siteId)), SK, a.fromS(a.sk()))).distinct().toList();

    List<Map<String, AttributeValue>> values = this.db.getBatch(new BatchGetConfig(), keys);

    return values.stream().map(a -> new AttributeRecord().getFromAttributes(siteId, a))
        .collect(Collectors.toMap(AttributeRecord::getKey, a -> a));
  }

  @Override
  public void setAttributeType(final String siteId, final String key, final AttributeType type) {
    AttributeRecord r = new AttributeRecord().key(key).documentId(key);

    Map<String, AttributeValueUpdate> attributes = Map.of("type",
        AttributeValueUpdate.builder().value(AttributeValue.fromS(type.name())).build());
    this.db.updateItem(r.fromS(r.pk(siteId)), r.fromS(r.sk()), attributes);
  }
}
