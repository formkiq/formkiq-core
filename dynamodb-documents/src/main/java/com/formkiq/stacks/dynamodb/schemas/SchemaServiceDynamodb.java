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
package com.formkiq.stacks.dynamodb.schemas;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * DynamoDB implementation for {@link SchemaService}.
 */
public class SchemaServiceDynamodb implements SchemaService, DbKeys {

  /** {@link AttributeService}. */
  private AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   * 
   */
  public SchemaServiceDynamodb(final DynamoDbService dbService) {
    this.db = dbService;
    this.attributeService = new AttributeServiceDynamodb(dbService);
  }

  private List<SiteSchemaCompositeKeyRecord> createCompositeKeys(
      final List<SchemaAttributesCompositeKey> compositeKeys) {

    List<SiteSchemaCompositeKeyRecord> records = new ArrayList<>();

    for (SchemaAttributesCompositeKey compositeKey : notNull(compositeKeys)) {
      SiteSchemaCompositeKeyRecord r =
          new SiteSchemaCompositeKeyRecord().keys(compositeKey.getAttributeKeys());
      records.add(r);
    }

    return records;
  }

  @Override
  public Schema getSitesSchema(final String siteId, final Integer version) {

    Schema schema = null;
    SitesSchemaRecord record = getSitesSchemaRecord(siteId, version);

    if (record != null) {
      Gson gson = new GsonBuilder().create();
      schema = gson.fromJson(record.getSchema(), Schema.class);
    }

    return schema;
  }

  @Override
  public SitesSchemaRecord getSitesSchemaRecord(final String siteId, final Integer version) {

    SitesSchemaRecord r = new SitesSchemaRecord();
    AttributeValue pk = r.fromS(r.pk(siteId));
    Map<String, AttributeValue> attr = null;

    if (version == null) {
      AttributeValue sk = r.fromS(SitesSchemaRecord.PREFIX_SK);
      QueryConfig config = new QueryConfig().scanIndexForward(Boolean.FALSE);
      QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, 1);
      attr = response.items().size() == 1 ? response.items().get(0) : Collections.emptyMap();

    } else {

      r.version(version);
      attr = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    }

    if (!attr.isEmpty()) {
      r = r.getFromAttributes(siteId, attr);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public SiteSchemaCompositeKeyRecord getCompositeKey(final String siteId,
      final List<String> attributeKeys) {
    SiteSchemaCompositeKeyRecord r = new SiteSchemaCompositeKeyRecord().keys(attributeKeys);
    Map<String, AttributeValue> map = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    return !map.isEmpty() ? r.getFromAttributes(siteId, map) : null;
  }

  @Override
  public Collection<ValidationError> setSitesSchema(final String siteId, final String name,
      final String schemaJson, final Schema schema) {

    Collection<ValidationError> errors = validate(siteId, name, schemaJson, schema);

    if (errors.isEmpty()) {

      SitesSchemaRecord schemaRecord = getSitesSchemaRecord(siteId, null);
      int version = schemaRecord != null ? schemaRecord.getVersion().intValue() + 1 : 1;

      SitesSchemaRecord r =
          new SitesSchemaRecord().name(name).schema(schemaJson).version(Integer.valueOf(version));

      List<SiteSchemaCompositeKeyRecord> compositeKeys =
          createCompositeKeys(schema.getAttributes().getCompositeKeys());

      deleteSiteSchemaCompositeKeys(siteId);

      List<Map<String, AttributeValue>> list = new ArrayList<>();
      list.add(r.getAttributes(siteId));
      list.addAll(compositeKeys.stream().map(a -> a.getAttributes(siteId)).toList());

      this.db.putItems(list);
    }

    return errors;
  }

  private boolean deleteSiteSchemaCompositeKeys(final String siteId) {

    SiteSchemaCompositeKeyRecord r = new SiteSchemaCompositeKeyRecord();

    final int limit = 100;
    QueryConfig config = new QueryConfig().projectionExpression("PK,SK");
    Map<String, AttributeValue> startkey = null;
    List<Map<String, AttributeValue>> list = new ArrayList<>();

    AttributeValue pk = AttributeValue.fromS(r.pk(siteId));
    AttributeValue sk = AttributeValue.fromS(SiteSchemaCompositeKeyRecord.PREFIX_SK);

    do {

      QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startkey, limit);

      List<Map<String, AttributeValue>> attrs =
          response.items().stream().collect(Collectors.toList());
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    List<Map<String, AttributeValue>> keys =
        list.stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    return this.db.deleteItems(keys);
  }

  private Collection<ValidationError> validate(final String siteId, final String name,
      final String schemaJson, final Schema schema) {

    Collection<ValidationError> errors = validateSchema(schema, name, schemaJson);

    if (errors.isEmpty()) {
      errors = validateAttributes(siteId, schema);
    }

    return errors;
  }

  private Collection<ValidationError> validateAttributes(final SchemaAttributes attributes) {

    Collection<ValidationError> errors = new ArrayList<>();

    notNull(attributes.getRequired()).forEach(a -> {
      if (isEmpty(a.getAttributeKey())) {
        String errorMsg = "required attribute missing attributeKey'";
        errors.add(new ValidationErrorImpl().error(errorMsg));
      }
    });

    notNull(attributes.getOptional()).forEach(a -> {
      if (isEmpty(a.getAttributeKey())) {
        String errorMsg = "optional attribute missing attributeKey'";
        errors.add(new ValidationErrorImpl().error(errorMsg));
      }
    });

    return errors;
  }

  private Collection<ValidationError> validateAttributes(final String siteId, final Schema schema) {

    Collection<ValidationError> errors = validateAttributes(schema.getAttributes());

    if (errors.isEmpty()) {

      List<String> requiredAttributes = notNull(schema.getAttributes().getRequired()).stream()
          .map(a -> a.getAttributeKey()).toList();

      List<String> optionalAttributes = notNull(schema.getAttributes().getOptional()).stream()
          .map(a -> a.getAttributeKey()).toList();

      List<String> attributeKeys =
          Stream.concat(requiredAttributes.stream(), optionalAttributes.stream()).toList();

      Map<String, AttributeRecord> attributeDataTypes =
          this.attributeService.getAttributes(siteId, attributeKeys);

      validateAttributesExist(attributeDataTypes, attributeKeys, errors);

      validateDefaultValues(schema, attributeDataTypes, errors);

      validateOverlap(requiredAttributes, optionalAttributes, errors);

      validateCompositeAttributes(schema, attributeKeys, errors);
    }

    return errors;
  }

  private void validateAttributesExist(final Map<String, AttributeRecord> attributes,
      final List<String> attributeKeys, final Collection<ValidationError> errors) {

    for (String attributeKey : attributeKeys) {

      if (!attributes.containsKey(attributeKey)) {
        String errorMsg = "attribute '" + attributeKey + "' not found";
        errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
      }
    }
  }

  private void validateCompositeAttributes(final Schema schema, final List<String> attributeKeys,
      final Collection<ValidationError> errors) {

    if (!schema.getAttributes().isAllowAdditionalAttributes()) {
      notNull(schema.getAttributes().getCompositeKeys()).forEach(a -> {

        List<String> overlapAttributeKeys =
            a.getAttributeKeys().stream().filter(item -> !attributeKeys.contains(item)).toList();

        for (String attributeKey : overlapAttributeKeys) {
          String errorMsg =
              "attribute '" + attributeKey + "' not listed in required/optional attributes";
          errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
        }
      });
    }
  }

  private void validateDefaultValues(final Schema schema,
      final Map<String, AttributeRecord> attributes, final Collection<ValidationError> errors) {

    if (errors.isEmpty()) {
      notNull(schema.getAttributes().getRequired()).forEach(a -> {

        String attributeKey = a.getAttributeKey();
        AttributeRecord ar = attributes.get(attributeKey);

        if (AttributeDataType.KEY_ONLY.equals(ar.getDataType())) {

          if (!notNull(a.getAllowedValues()).isEmpty()) {
            String errorMsg = "attribute '" + attributeKey + "' does not allow allowed values";
            errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
          }

          if (!notNull(a.getDefaultValues()).isEmpty() || a.getDefaultValue() != null) {
            String errorMsg = "attribute '" + attributeKey + "' does not allow default values";
            errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
          }
        }
      });
    }
  }

  private void validateOverlap(final List<String> requiredAttributes,
      final List<String> optionalAttributes, final Collection<ValidationError> errors) {

    List<String> overlap =
        requiredAttributes.stream().filter(item -> optionalAttributes.contains(item)).toList();

    for (String attribyteKey : overlap) {
      errors.add(new ValidationErrorImpl().key(attribyteKey)
          .error("attribute '" + attribyteKey + "' is in both required & optional lists"));
    }
  }

  private Collection<ValidationError> validateSchema(final Schema schema, final String name,
      final String schemaJson) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmpty(name)) {
      errors.add(new ValidationErrorImpl().key("name").error("'name' is required"));
    }

    if (isEmpty(schemaJson) || schema.getAttributes() == null) {
      errors.add(new ValidationErrorImpl().key("schema").error("'schema' is required"));
    } else {

      SchemaAttributes schemaAttributes = schema.getAttributes();

      List<SchemaAttributesCompositeKey> compositeKeys =
          notNull(schemaAttributes.getCompositeKeys());

      if (notNull(schemaAttributes.getRequired()).isEmpty()
          && notNull(schemaAttributes.getOptional()).isEmpty() && compositeKeys.isEmpty()) {
        errors.add(new ValidationErrorImpl()
            .error("either 'required', 'optional' or 'compositeKeys' attributes list is required"));
      }

      compositeKeys.forEach(keys -> {
        if (notNull(keys.getAttributeKeys()).size() == 1) {
          errors.add(new ValidationErrorImpl().key("compositeKeys")
              .error("compositeKeys must have more than 1 value"));
        }
      });
    }

    return errors;
  }

}
