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
package com.formkiq.stacks.dynamodb.locale;

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamodbRecordToKeys;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.stacks.dynamodb.schemas.ClassificationRecord;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Dynamodb implementation for {@link LocaleService}.
 */
public class LocaleServiceDynamodb implements LocaleService {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link SchemaService}. */
  private final SchemaService schema;

  /**
   * constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param schemaService {@link SchemaService}
   */
  public LocaleServiceDynamodb(final DynamoDbService dbService, final SchemaService schemaService) {
    this.db = dbService;
    this.schema = schemaService;
  }

  private static Map<String, AttributeValue> toMap(final String siteId, final String sk) {
    return Map.of(PK, AttributeValue.fromS(LocaleRecord.createPk(siteId)), SK,
        AttributeValue.fromS(sk));
  }

  @Override
  public List<ValidationError> save(final String siteId, final Collection<LocaleRecord> records) {
    List<ValidationError> errors = validate(siteId, records);
    if (errors.isEmpty()) {
      List<Map<String, AttributeValue>> attrs =
          records.stream().map(r -> r.getAttributes(siteId)).toList();
      this.db.putItems(attrs);
    }

    return errors;
  }

  /**
   * Validate {@link LocaleRecord}.
   *
   * @param siteId {@link String}
   * @param records {@link Collection} {@link LocaleRecord}
   * @return List {@link ValidationError}
   */
  private List<ValidationError> validate(final String siteId,
      final Collection<LocaleRecord> records) {
    List<ValidationError> errors = new ArrayList<>();

    if (records == null || records.isEmpty()) {
      errors.add(new ValidationErrorImpl().error("no LocaleRecords found"));
    } else {
      notNull(records).forEach(record -> validateLocaleRecord(record, errors));
    }

    if (errors.isEmpty()) {
      validateAttribute(siteId, records, errors);
    }

    validateAllowedValues(siteId, records, errors);

    return errors;
  }

  /**
   * Validate Allowed Values.
   * 
   * @param siteId {@link String}
   * @param records {@link Collection} {@link LocaleRecord}
   * @param errors {@link List} {@link ValidationError}
   */
  private void validateAllowedValues(final String siteId, final Collection<LocaleRecord> records,
      final List<ValidationError> errors) {

    if (errors.isEmpty()) {

      Map<String, List<String>> allowedValues = findAllowedValues(siteId, records);

      Map<String, LocaleRecord> attributeKeys = getAttributeKeysByResourceTypes(records,
          List.of(LocaleResourceType.SCHEMA, LocaleResourceType.CLASSIFICATION));

      attributeKeys.forEach((key, value) -> {

        String keyval =
            !isEmpty(value.getClassificationId()) ? value.getClassificationId() : "schema";
        List<String> values = allowedValues.getOrDefault(keyval + "#" + key, null);

        if (values != null) {

          String allowedValue = value.getAllowedValue();

          if (!values.contains(allowedValue)) {
            errors.add(new ValidationErrorImpl().key(key)
                .error("invalid allowed value '" + allowedValue + "' on key '" + key + "'"));
          }

        } else {
          String msg =
              "schema".equals(keyval) ? "is not used in schema" : "is not used in classification";
          errors.add(new ValidationErrorImpl().key(key).error("AttributeKey '" + key + "' " + msg));
        }
      });
    }
  }

  /**
   * Validate {@link LocaleRecord}.
   *
   * @param record {@link LocaleRecord}
   * @param errors {@link List} {@link ValidationError}
   */
  private void validateLocaleRecord(final LocaleRecord record, final List<ValidationError> errors) {

    if (record.getItemType() == null) {
      errors.add(new ValidationErrorImpl().key("itemType").error("'itemType' is required"));
    } else {
      switch (record.getItemType()) {
        case INTERFACE -> {
          if (isEmpty(record.getInterfaceKey())) {
            errors.add(
                new ValidationErrorImpl().key("interfaceKey").error("'interfaceKey' is required"));
          }
          if (isEmpty(record.getLocalizedValue())) {
            errors.add(new ValidationErrorImpl().key("localizedValue")
                .error("'localizedValue' is required"));
          }
        }
        case SCHEMA -> validateAttribute(record, errors);
        case CLASSIFICATION -> {
          if (isEmpty(record.getClassificationId())) {
            errors.add(new ValidationErrorImpl().key("classificationId")
                .error("'classificationId' is required"));
          }

          validateAttribute(record, errors);
        }
        default -> throw new IllegalStateException("Unexpected value: " + record.getItemType());
      }
    }
  }

  private Map<String, List<String>> findAllowedValues(final String siteId,
      final Collection<LocaleRecord> records) {

    Map<String, List<String>> map = new HashMap<>();

    Schema sitesSchema = schema.getSitesSchema(siteId);

    if (sitesSchema != null && sitesSchema.getAttributes() != null) {

      notNull(sitesSchema.getAttributes().getRequired())
          .forEach(a -> map.put("schema#" + a.getAttributeKey(), notNull(a.getAllowedValues())));

      notNull(sitesSchema.getAttributes().getOptional())
          .forEach(a -> map.put("schema#" + a.getAttributeKey(), notNull(a.getAllowedValues())));
    }

    notNull(records).forEach(record -> {

      if (!isEmpty(record.getClassificationId())) {
        ClassificationRecord classificationRecord =
            schema.findClassification(siteId, record.getClassificationId());

        if (classificationRecord != null) {
          Schema classification = schema.getSchema(classificationRecord);
          String classificationId = classificationRecord.getDocumentId();
          notNull(classification.getAttributes().getRequired()).forEach(a -> map
              .put(classificationId + "#" + a.getAttributeKey(), notNull(a.getAllowedValues())));

          notNull(classification.getAttributes().getOptional()).forEach(a -> map
              .put(classificationId + "#" + a.getAttributeKey(), notNull(a.getAllowedValues())));
        }
      }
    });

    return map;
  }

  private Map<String, LocaleRecord> getAttributeKeysByResourceTypes(
      final Collection<LocaleRecord> records, final List<LocaleResourceType> allowedTypes) {
    return records.stream().filter(record -> allowedTypes.contains(record.getItemType()))
        .collect(Collectors.toMap(LocaleRecord::getAttributeKey, Function.identity()));
  }

  private void validateAttribute(final String siteId, final Collection<LocaleRecord> records,
      final List<ValidationError> errors) {

    Collection<String> attributeKeys = getAttributeKeysByResourceTypes(records,
        List.of(LocaleResourceType.SCHEMA, LocaleResourceType.CLASSIFICATION)).keySet();

    Collection<AttributeRecord> attributes = attributeKeys.stream()
        .map(k -> new AttributeRecord().documentId(k)).collect(Collectors.toSet());

    List<Map<String, AttributeValue>> keys =
        attributes.stream().map(new DynamodbRecordToKeys(siteId)).toList();

    List<Map<String, AttributeValue>> batch =
        this.db.getBatch(new BatchGetConfig().projectionExpression("PK,SK,documentId"), keys);

    attributeKeys.removeAll(batch.stream().map(a -> a.get("documentId").s()).toList());
    attributeKeys.forEach(
        a -> errors.add(new ValidationErrorImpl().key(a).error("missing attribute '" + a + "'")));
  }

  private void validateAttribute(final LocaleRecord record, final List<ValidationError> errors) {
    if (isEmpty(record.getAttributeKey())) {
      errors.add(new ValidationErrorImpl().key("attributeKey").error("'attributeKey' is required"));
    }
    if (isEmpty(record.getAllowedValue())) {
      errors.add(new ValidationErrorImpl().key("allowedValue").error("'allowedValue' is required"));
    }
  }

  @Override
  public List<LocaleRecord> getLocaleInterface(final String siteId, final String locale,
      final Collection<String> interfaceKeys) {
    List<Map<String, AttributeValue>> keys = notNull(interfaceKeys).stream().map(i -> {
      String sk = LocaleRecord.getInterfaceSk(locale, LocaleResourceType.INTERFACE, i);
      return toMap(siteId, sk);
    }).toList();

    return toLocaleRecords(siteId, keys);
  }

  private List<LocaleRecord> toLocaleRecords(final String siteId,
      final List<Map<String, AttributeValue>> keys) {
    List<Map<String, AttributeValue>> batch = this.db.getBatch(new BatchGetConfig(), keys);
    return batch.stream().map(a -> new LocaleRecord().getFromAttributes(siteId, a)).toList();
  }

  @Override
  public List<LocaleRecord> getLocaleSchema(final String siteId, final String locale,
      final Map<String, Collection<String>> attributeKeys) {

    List<Map<String, AttributeValue>> keys =
        notNull(attributeKeys).entrySet().stream().flatMap(a -> {
          List<String> sks = a.getValue().stream()
              .map(v -> LocaleRecord.getSchemaSk(locale, LocaleResourceType.SCHEMA, a.getKey(), v))
              .toList();
          return sks.stream().map(sk -> toMap(siteId, sk)).toList().stream();
        }).toList();

    return toLocaleRecords(siteId, keys);
  }

  @Override
  public List<LocaleRecord> getLocaleSchema(final String siteId, final String locale,
      final String classificationId, final Map<String, Collection<String>> attributeKeys) {

    List<Map<String, AttributeValue>> keys =
        notNull(attributeKeys).entrySet().stream().flatMap(a -> {
          List<String> sks = a.getValue().stream().map(v -> LocaleRecord.getClassificationSk(locale,
              LocaleResourceType.SCHEMA, classificationId, a.getKey(), v)).toList();
          return sks.stream().map(sk -> toMap(siteId, sk)).toList().stream();
        }).toList();

    return toLocaleRecords(siteId, keys);
  }

  @Override
  public Pagination<LocaleRecord> findAll(final String siteId, final String locale,
      final LocaleResourceType itemType, final String nextToken, final int limit) {

    String sk = locale + "#" + (itemType != null ? itemType.name() : "");
    Map<String, AttributeValue> key = toMap(siteId, sk);
    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);

    QueryResponse response =
        this.db.queryBeginsWith(config, key.get(PK), key.get(SK), startKey, limit);
    List<LocaleRecord> list = response.items().stream()
        .map(r -> new LocaleRecord().getFromAttributes(siteId, r)).toList();
    return new Pagination<>(list, response.lastEvaluatedKey());
  }

  @Override
  public LocaleRecord find(final String siteId, final String locale, final String itemKey) {
    LocaleRecord record = new LocaleRecord();
    String pk = record.pk(siteId);
    String sk = locale + "#" + itemKey;
    Map<String, AttributeValue> attr =
        this.db.get(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
    return record.getFromAttributes(siteId, attr);
  }

  @Override
  public boolean delete(final String siteId, final String locale, final String itemKey) {
    LocaleRecord record = new LocaleRecord();
    String pk = record.pk(siteId);
    String sk = locale + "#" + itemKey;
    return this.db.deleteItem(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
  }
}
