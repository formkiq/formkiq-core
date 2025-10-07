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
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributeKeyRecord;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
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
  public void addAttribute(final AttributeValidationAccess validationAccess, final String siteId,
      final String key, final AttributeDataType dataType, final AttributeType type) {
    addAttribute(validationAccess, siteId, key, dataType, type, false);
  }

  @Override
  public void addAttribute(final AttributeValidationAccess validationAccess, final String siteId,
      final String key, final AttributeDataType dataType, final AttributeType type,
      final boolean allowReservedAttributeKey) {
    addAttribute(validationAccess, siteId, key, dataType, type, allowReservedAttributeKey, null);
  }

  private void addAttribute(final AttributeValidationAccess validationAccess, final String siteId,
      final String key, final AttributeDataType dataType, final AttributeType type,
      final boolean allowReservedAttributeKey, final Watermark watermark) {

    WatermarkPosition position = watermark != null ? watermark.position() : null;

    AttributeRecord a = new AttributeRecord().documentId(key).key(key)
        .type(type != null ? type : AttributeType.STANDARD)
        .setWatermarkText(watermark != null ? watermark.text() : null)
        .setWatermarkImageDocumentId(watermark != null ? watermark.imageDocumentId() : null)
        .setWatermarkRotation(watermark != null ? watermark.rotation() : null)
        .setWatermarkScale(watermark != null ? watermark.scale() : null)
        .setWatermarkFontSize(watermark != null ? watermark.fontSize() : null)
        .dataType(dataType != null ? dataType : AttributeDataType.STRING);

    if (position != null) {
      updateWatermarkPosition(a, position);
    }

    validate(validationAccess, siteId, allowReservedAttributeKey, a);

    Map<String, AttributeValue> attrs = a.getAttributes(siteId);
    this.db.putItem(attrs);
  }

  @Override
  public void addWatermarkAttribute(final String siteId, final String key,
      final Watermark watermark) {
    addAttribute(AttributeValidationAccess.CREATE, siteId, key, AttributeDataType.WATERMARK,
        AttributeType.STANDARD, false, watermark);
  }

  @Override
  public Collection<ValidationError> deleteAttribute(
      final AttributeValidationAccess validationAccess, final String siteId, final String key) {

    boolean deleted = false;
    ValidationBuilder vb = new ValidationBuilder();
    validateDeleteAttribute(siteId, key, vb);

    if (vb.isEmpty()) {

      AttributeRecord r = new AttributeRecord().documentId(key);

      validateAttributeType(validationAccess, siteId, key, vb);

      if (vb.isEmpty()) {
        deleted = this.db.deleteItem(Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk())));
      }
    }

    if (!deleted && vb.isEmpty()) {
      vb.addError("key", "attribute 'key' not found");
    }

    return vb.getErrors();
  }

  @Override
  public boolean existsAttribute(final String siteId, final String key) {
    AttributeRecord r = new AttributeRecord().documentId(key);
    return this.db.exists(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
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
  public void setAttributeType(final AttributeValidationAccess validationAccess,
      final String siteId, final String key, final AttributeType type) {

    AttributeRecord r = new AttributeRecord().key(key).documentId(key).type(type);

    ValidationBuilder vb = new ValidationBuilder();
    validateAttributeType(validationAccess, r, vb);
    vb.check();

    Map<String, AttributeValueUpdate> attributes = Map.of("type",
        AttributeValueUpdate.builder().value(AttributeValue.fromS(type.name())).build());
    this.db.updateItem(r.fromS(r.pk(siteId)), r.fromS(r.sk()), attributes);
  }

  @Override
  public void updateAttribute(final AttributeValidationAccess validationAccess, final String siteId,
      final String key, final AttributeType type, final Watermark watermark) {

    ValidationBuilder vb = new ValidationBuilder();

    AttributeRecord r = new AttributeRecord().key(key).documentId(key);
    Map<String, AttributeValue> attributes = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    vb.isRequired(null, !attributes.isEmpty(), "Attribute not found");
    vb.check();

    vb.isRequired(null, !(type == null && watermark == null),
        "Attribute Type or Watermark is required");
    vb.isRequired(null, !(type != null && !validationAccess.isAdminOrGovernRole()),
        "Access denied to attribute");
    vb.check();

    r = r.getFromAttributes(siteId, attributes);
    validateWatermark(siteId, r, vb);
    vb.check();

    if (type != null) {
      r.type(type);
    }

    if (watermark != null) {
      updateWatermark(r, watermark);
    }

    this.db.putItem(r.getAttributes(siteId));
  }

  private void updateWatermark(final AttributeRecord record, final Watermark watermark) {
    record.setWatermarkScale(watermark.scale());
    record.setWatermarkRotation(watermark.rotation());
    record.setWatermarkText(watermark.text());
    record
        .setWatermarkxAnchor(watermark.position() != null ? watermark.position().xAnchor() : null);
    record
        .setWatermarkxOffset(watermark.position() != null ? watermark.position().xOffset() : null);
    record
        .setWatermarkyOffset(watermark.position() != null ? watermark.position().yOffset() : null);
    record
        .setWatermarkyAnchor(watermark.position() != null ? watermark.position().yAnchor() : null);
    record.setWatermarkImageDocumentId(watermark.imageDocumentId());
  }

  private void updateWatermarkPosition(final AttributeRecord a, final WatermarkPosition position) {
    WatermarkXanchor xanchor =
        position != null && position.xAnchor() != null ? position.xAnchor() : null;
    WatermarkYanchor yanchor =
        position != null && position.yAnchor() != null ? position.yAnchor() : null;

    a.setWatermarkxOffset(position != null ? position.xOffset() : null)
        .setWatermarkyOffset(position != null ? position.yOffset() : null)
        .setWatermarkxAnchor(xanchor).setWatermarkyAnchor(yanchor);
  }

  private void validate(final AttributeValidationAccess validationAccess, final String siteId,
      final boolean allowReservedAttributeKey, final AttributeRecord a) {

    String key = a.getKey();
    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("key", key);
    vb.check();

    if (!allowReservedAttributeKey) {
      AttributeKeyReserved r = AttributeKeyReserved.find(key);
      vb.isRequired("key", r == null, "'" + key + "' is a reserved attribute name");
    }

    AttributeRecord attribute = getAttribute(siteId, key);
    vb.isRequired("key", attribute == null, "attribute '" + key + "' already exists");

    validateWatermark(siteId, a, vb);
    validateAttributeType(validationAccess, a, vb);

    vb.check();
  }

  private void validateAttributeType(final AttributeValidationAccess validationAccess,
      final AttributeRecord r, final ValidationBuilder vb) {

    if (!validationAccess.isAdminOrGovernRole()) {

      final AttributeType attributeType = r != null && r.getType() != null ? r.getType() : null;

      if (attributeType != null) {
        Collection<AttributeType> types = List.of(AttributeType.GOVERNANCE, AttributeType.OPA);
        types.forEach(type -> vb.isRequired(r.getKey(), !type.equals(attributeType),
            "Access denied to attribute"));
      }
    }
  }

  private void validateAttributeType(final AttributeValidationAccess validationAccess,
      final String siteId, final String key, final ValidationBuilder vb) {

    if (!validationAccess.isAdminOrGovernRole()) {

      AttributeRecord r = new AttributeRecord().documentId(key);
      Map<String, AttributeValue> attrs = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
      if (!attrs.isEmpty()) {
        r = r.getFromAttributes(siteId, attrs);
      }

      validateAttributeType(validationAccess, r, vb);
    }
  }

  private void validateDeleteAttribute(final String siteId, final String key,
      final ValidationBuilder vb) {

    QueryConfig config = new QueryConfig().indexName(GSI1);

    // check for Schema / Classification Key
    SchemaAttributeKeyRecord r = new SchemaAttributeKeyRecord().setKey(key);
    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    QueryResponse response = this.db.queryBeginsWith(config, pk, null, null, 1);
    if (!response.items().isEmpty()) {
      vb.addError(key,
          "attribute '" + key + "' is used in a Schema / Classification, cannot be deleted");
    }

    // check for DocumentAttributeRecords
    DocumentAttributeRecord dar = new DocumentAttributeRecord().setKey(key);
    pk = dar.fromS(dar.pkGsi1(siteId));
    response = this.db.queryBeginsWith(config, pk, null, null, 1);

    if (!response.items().isEmpty()) {
      vb.addError(key, "attribute '" + key + "' is in use, cannot be deleted");
    }
  }

  private void validateWatermark(final String siteId, final AttributeRecord a,
      final ValidationBuilder vb) {

    boolean hasWatermark =
        !isEmpty(a.getWatermarkText()) || !isEmpty(a.getWatermarkImageDocumentId());

    if (AttributeDataType.WATERMARK.equals(a.getDataType())) {

      String watermarkImageDocumentId = a.getWatermarkImageDocumentId();
      if (isEmpty(a.getWatermarkText()) && isEmpty(watermarkImageDocumentId)) {
        vb.addError("watermark", "'watermark.text' or 'watermark.imageDocumentId' is required");
      } else if (!isEmpty(watermarkImageDocumentId)) {

        Map<String, AttributeValue> keys = keysDocument(siteId, watermarkImageDocumentId);
        if (!this.db.exists(keys.get(PK), keys.get(SK))) {
          vb.addError("watermark.imageDocumentId", "watermark.imageDocumentId' does not exist");
        }
      }

    } else if (hasWatermark) {
      vb.addError("watermark.text", "'watermark' only allowed on dataType 'WATERMARK'");
    }
  }
}
