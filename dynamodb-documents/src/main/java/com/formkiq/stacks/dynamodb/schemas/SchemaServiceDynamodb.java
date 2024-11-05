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

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * DynamoDB implementation for {@link SchemaService}.
 */
public class SchemaServiceDynamodb implements SchemaService, DbKeys {

  /** {@link AttributeService}. */
  private final AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

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

  @Override
  public Schema getSitesSchema(final String siteId) {

    Schema schema = null;
    SitesSchemaRecord record = getSitesSchemaRecord(siteId);

    if (record != null) {
      schema = gson.fromJson(record.getSchema(), Schema.class);
    }

    return schema;
  }

  @Override
  public SitesSchemaRecord getSitesSchemaRecord(final String siteId) {

    SitesSchemaRecord r = new SitesSchemaRecord();
    AttributeValue pk = r.fromS(r.pk(siteId));
    AttributeValue sk = r.fromS(r.sk());
    Map<String, AttributeValue> attr = this.db.get(pk, sk);

    if (!attr.isEmpty()) {
      r = r.getFromAttributes(siteId, attr);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public SchemaCompositeKeyRecord getCompositeKey(final String siteId,
      final List<String> attributeKeys) {

    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);
    SchemaCompositeKeyRecord r = new SchemaCompositeKeyRecord().keys(attributeKeys);

    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    AttributeValue sk = r.fromS(r.skGsi1());

    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, 1);
    List<Map<String, AttributeValue>> items = response.items();

    if (!items.isEmpty()) {
      Map<String, AttributeValue> attrs = items.get(0);
      attrs = this.db.get(attrs.get(PK), attrs.get(SK));
      r = r.getFromAttributes(siteId, attrs);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public Collection<ValidationError> setSitesSchema(final String siteId, final String name,
      final Schema schema) {

    Collection<ValidationError> errors = validate(siteId, null, name, schema);

    if (errors.isEmpty()) {

      String schemaJson = gson.toJson(schema);
      SitesSchemaRecord r = new SitesSchemaRecord().name(name).schema(schemaJson);

      deleteSchemaCompositeKeys(siteId, null);
      deleteSchemaAttribute(siteId, null);

      List<Map<String, AttributeValue>> list = new ArrayList<>();
      list.add(r.getAttributes(siteId));

      List<SchemaCompositeKeyRecord> compositeKeys =
          createCompositeKeys(schema.getAttributes().getCompositeKeys());
      list.addAll(compositeKeys.stream().map(a -> a.getAttributes(siteId)).toList());

      List<SchemaAttributeAllowedValueRecord> allowedValues =
          createAllowedValues(null, schema.getAttributes());
      list.addAll(allowedValues.stream().map(a -> a.getAttributes(siteId)).toList());

      List<SchemaAttributeKeyRecord> attributeKeys =
          createAttributeKeys(null, schema.getAttributes());
      list.addAll(attributeKeys.stream().map(a -> a.getAttributes(siteId)).toList());

      this.db.putItems(list);
    }

    return errors;
  }

  @Override
  public PaginationResults<ClassificationRecord> findAllClassifications(final String siteId,
      final Map<String, AttributeValue> startkey, final int limit) {

    ClassificationRecord r = new ClassificationRecord();
    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(true);
    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    AttributeValue sk = r.fromS("attr#");

    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startkey, limit);

    List<Map<String, AttributeValue>> attrs =
        this.db.getBatch(new BatchGetConfig(), response.items());

    List<ClassificationRecord> list =
        attrs.stream().map(a -> new ClassificationRecord().getFromAttributes(siteId, a)).toList();

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(response));
  }

  @Override
  public ClassificationRecord setClassification(final String siteId, final String classificationId,
      final String name, final Schema schema, final String userId) throws ValidationException {

    Schema sitesSchema = getSitesSchema(siteId);
    Collection<ValidationError> errors = validate(siteId, sitesSchema, name, schema);
    errors.addAll(validateClassification(siteId, classificationId, name));

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    String documentId = classificationId != null ? classificationId : UUID.randomUUID().toString();

    String schemaJson = gson.toJson(schema);
    ClassificationRecord r = new ClassificationRecord().setName(name).setSchema(schemaJson)
        .setDocumentId(documentId).setInsertedDate(new Date()).setUserId(userId);

    deleteSchemaCompositeKeys(siteId, documentId);
    deleteSchemaAttribute(siteId, documentId);

    List<Map<String, AttributeValue>> list = new ArrayList<>();
    list.add(r.getAttributes(siteId));

    List<SchemaCompositeKeyRecord> compositeKeys =
        createCompositeKeys(schema.getAttributes().getCompositeKeys());
    compositeKeys.forEach(c -> c.setDocumentId(documentId));
    list.addAll(compositeKeys.stream().map(a -> a.getAttributes(siteId)).toList());

    List<SchemaAttributeAllowedValueRecord> allowedValues =
        createAllowedValues(documentId, schema.getAttributes());
    list.addAll(allowedValues.stream().map(a -> a.getAttributes(siteId)).toList());

    List<SchemaAttributeKeyRecord> attributeKeys =
        createAttributeKeys(documentId, schema.getAttributes());
    list.addAll(attributeKeys.stream().map(a -> a.getAttributes(siteId)).toList());

    this.db.putItems(list);

    return r;
  }

  @Override
  public ClassificationRecord findClassification(final String siteId,
      final String classificationId) {

    ClassificationRecord r = new ClassificationRecord().setDocumentId(classificationId);
    AttributeValue pk = r.fromS(r.pk(siteId));
    AttributeValue sk = r.fromS(r.sk());

    Map<String, AttributeValue> attr = this.db.get(pk, sk);
    if (!attr.isEmpty()) {
      r = r.getFromAttributes(siteId, attr);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public boolean deleteClassification(final String siteId, final String classificationId) {
    ClassificationRecord r = new ClassificationRecord().setDocumentId(classificationId);
    AttributeValue pk = r.fromS(r.pk(siteId));
    return this.db.deleteItemsBeginsWith(pk, null);
  }

  @Override
  public Schema getSchema(final ClassificationRecord classification) {
    return gson.fromJson(classification.getSchema(), Schema.class);
  }

  @Override
  public Schema mergeSchemaIntoClassification(final Schema from, final Schema to) {

    if (from != null) {

      SchemaAttributes fromAttributes = from.getAttributes();

      Map<String, SchemaAttributesRequired> required =
          getRequiredAttributeMap(fromAttributes.getRequired());

      List<SchemaAttributesRequired> requiredMerged =
          merge(required, notNull(to.getAttributes().getRequired()));
      to.getAttributes().required(requiredMerged);

      Map<String, SchemaAttributesOptional> optional =
          getOptionalAttributeMap(fromAttributes.getOptional());

      if (to.getAttributes() != null) {
        List<SchemaAttributesOptional> optionalMerged =
            merge(required, optional, notNull(to.getAttributes().getOptional()));
        to.getAttributes().optional(optionalMerged);
      }

      // merge composite keys
      List<SchemaAttributesCompositeKey> cks =
          mergeCompositeKeys(notNull(from.getAttributes().getCompositeKeys()),
              notNull(to.getAttributes().getCompositeKeys()));
      to.getAttributes().compositeKeys(cks);
    }

    return to;
  }

  @Override
  public List<String> getSitesSchemaAttributeAllowedValues(final String siteId,
      final String attributeKey) {
    return getClassificationAttributeAllowedValues(siteId, null, attributeKey);
  }

  @Override
  public List<String> getClassificationAttributeAllowedValues(final String siteId,
      final String documentId, final String attributeKey) {

    SchemaAttributeAllowedValueRecord r =
        new SchemaAttributeAllowedValueRecord().setDocumentId(documentId).setKey(attributeKey);

    AttributeValue pk = r.fromS(r.pk(siteId));
    AttributeValue sk =
        r.fromS(SchemaAttributeAllowedValueRecord.SK + attributeKey + "#allowedvalue#");

    final int limit = 100;
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, limit);
    List<String> classification = response.items().stream().map(i -> i.get("value").s()).toList();

    if (documentId != null) {
      List<String> siteSchema = getSitesSchemaAttributeAllowedValues(siteId, attributeKey);
      classification =
          Stream.concat(classification.stream(), siteSchema.stream()).distinct().sorted().toList();
    }

    return classification;
  }

  @Override
  public List<String> getAttributeAllowedValues(final String siteId, final String attributeKey) {
    SchemaAttributeAllowedValueRecord r =
        new SchemaAttributeAllowedValueRecord().setKey(attributeKey);

    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    AttributeValue sk = r.fromS(SchemaAttributeAllowedValueRecord.GSI_SK);

    final int limit = 100;
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE).indexName(GSI1);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, limit);

    List<Map<String, AttributeValue>> keys = new HashSet<>(response.items()).stream()
        .map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).toList();

    List<Map<String, AttributeValue>> batch = this.db.getBatch(new BatchGetConfig(), keys);

    return batch.stream().map(i -> i.get("value").s()).distinct().sorted().toList();
  }

  private List<SchemaAttributesCompositeKey> mergeCompositeKeys(
      final List<SchemaAttributesCompositeKey> from, final List<SchemaAttributesCompositeKey> to) {

    Set<String> keys =
        from.stream().map(c -> String.join(",", c.getAttributeKeys())).collect(Collectors.toSet());

    List<SchemaAttributesCompositeKey> toList = to.stream().filter(c -> {
      String key = String.join(",", c.getAttributeKeys());
      return !keys.contains(key);
    }).toList();

    return Objects.concat(from, toList);
  }

  private List<SchemaAttributesOptional> merge(
      final Map<String, SchemaAttributesRequired> fromRequired,
      final Map<String, SchemaAttributesOptional> from, final List<SchemaAttributesOptional> to) {

    List<SchemaAttributesOptional> updated = new ArrayList<>(to);

    // merge matching optional attributes
    updated.forEach(u -> {
      if (from.containsKey(u.getAttributeKey())) {
        SchemaAttributesOptional r = from.get(u.getAttributeKey());
        u.allowedValues(Objects.concat(r.getAllowedValues(), u.getAllowedValues()));
      }
    });

    // add missing optional attributes
    Set<String> attributeKeys =
        to.stream().map(SchemaAttributesOptional::getAttributeKey).collect(Collectors.toSet());
    List<SchemaAttributesOptional> missing =
        from.values().stream().filter(f -> !attributeKeys.contains(f.getAttributeKey())).toList();
    updated.addAll(missing);
    updated = updated.stream().filter(a -> !fromRequired.containsKey(a.getAttributeKey())).toList();

    return updated;
  }

  private List<SchemaAttributesRequired> merge(final Map<String, SchemaAttributesRequired> from,
      final List<SchemaAttributesRequired> to) {

    List<SchemaAttributesRequired> updated = new ArrayList<>(to);

    // merge matching required attributes
    updated.forEach(u -> {

      if (from.containsKey(u.getAttributeKey())) {
        SchemaAttributesRequired r = from.get(u.getAttributeKey());

        u.allowedValues(Objects.concat(r.getAllowedValues(), u.getAllowedValues()));

        if (isEmpty(u.getDefaultValue()) && !isEmpty(r.getDefaultValue())) {
          u.defaultValue(r.getDefaultValue());
        }
      }
    });

    // add missing required attributes
    Set<String> attributeKeys =
        to.stream().map(SchemaAttributesRequired::getAttributeKey).collect(Collectors.toSet());
    List<SchemaAttributesRequired> missing =
        from.values().stream().filter(f -> !attributeKeys.contains(f.getAttributeKey())).toList();
    updated.addAll(missing);

    return updated;
  }

  private Map<String, SchemaAttributesRequired> getRequiredAttributeMap(
      final List<SchemaAttributesRequired> attributes) {
    return notNull(attributes).stream()
        .collect(Collectors.toMap(SchemaAttributesRequired::getAttributeKey, Function.identity()));
  }

  private Map<String, SchemaAttributesOptional> getOptionalAttributeMap(
      final List<SchemaAttributesOptional> attributes) {
    return notNull(attributes).stream()
        .collect(Collectors.toMap(SchemaAttributesOptional::getAttributeKey, Function.identity()));
  }

  private List<SchemaAttributeKeyRecord> createAttributeKeys(final String documentId,
      final SchemaAttributes attributes) {

    Set<String> requiredKeys = notNull(attributes.getRequired()).stream()
        .map(SchemaAttributesRequired::getAttributeKey).collect(Collectors.toSet());
    Set<String> optionalKeys = notNull(attributes.getOptional()).stream()
        .map(SchemaAttributesOptional::getAttributeKey).collect(Collectors.toSet());

    List<String> keys = Stream.concat(requiredKeys.stream(), optionalKeys.stream()).toList();

    return keys.stream()
        .map(key -> new SchemaAttributeKeyRecord().setKey(key).setDocumentId(documentId)).toList();
  }

  private List<SchemaAttributeAllowedValueRecord> createAllowedValues(final String documentId,
      final SchemaAttributes attributes) {

    List<SchemaAttributeAllowedValueRecord> list = new ArrayList<>();

    List<SchemaAttributesRequired> required = notNull(attributes.getRequired()).stream()
        .filter(a -> !notNull(a.getAllowedValues()).isEmpty()).toList();
    required.forEach(a -> a.getAllowedValues().forEach(av -> {
      SchemaAttributeAllowedValueRecord v = new SchemaAttributeAllowedValueRecord()
          .setDocumentId(documentId).setKey(a.getAttributeKey()).setValue(av);
      list.add(v);
    }));

    List<SchemaAttributesOptional> optional = notNull(attributes.getOptional()).stream()
        .filter(a -> !notNull(a.getAllowedValues()).isEmpty()).toList();
    optional.forEach(a -> a.getAllowedValues().forEach(av -> {
      SchemaAttributeAllowedValueRecord v = new SchemaAttributeAllowedValueRecord()
          .setDocumentId(documentId).setKey(a.getAttributeKey()).setValue(av);
      list.add(v);
    }));

    return list;
  }

  /**
   * Validate Classification.
   * 
   * @param siteId {@link String}
   * @param classificationId {@link String}
   * @param name {@link String}
   * @return Collection {@link ValidationError}
   */
  private Collection<ValidationError> validateClassification(final String siteId,
      final String classificationId, final String name) {

    ClassificationRecord r = new ClassificationRecord().setName(name);
    AttributeValue pk = r.fromS(r.pkGsi1(siteId));
    AttributeValue sk = r.fromS(r.skGsi1());

    Collection<ValidationError> errors = Collections.emptyList();
    QueryConfig config = new QueryConfig().indexName(GSI1);
    QueryResponse response = this.db.query(config, pk, sk, null, 1);

    List<Map<String, AttributeValue>> items = response.items();
    if (!isEmpty(classificationId)) {
      items =
          items.stream().filter(i -> !classificationId.equals(i.get("documentId").s())).toList();
    }

    if (!items.isEmpty()) {
      errors = List.of(new ValidationErrorImpl().key("name").error("'name' is already used"));
    }

    return errors;
  }

  private Collection<ValidationError> validate(final String siteId, final Schema sitesSchema,
      final String name, final Schema schema) {

    Collection<ValidationError> errors = validateSchema(schema, name);

    if (errors.isEmpty()) {
      errors = validateAttributesAgainstSiteSchema(sitesSchema, schema);
    }

    if (errors.isEmpty()) {
      errors = validateAttributes(siteId, schema);
    }

    return errors;
  }

  /**
   * Validate Site Schema against Classification Schema.
   * 
   * @param sitesSchema {@link Schema}
   * @param schema {@link Schema}
   * @return {@link Collection} {@link ValidationError}
   */
  private Collection<ValidationError> validateAttributesAgainstSiteSchema(final Schema sitesSchema,
      final Schema schema) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (sitesSchema != null) {

      Set<String> siteSchemaRequiredKeys = notNull(sitesSchema.getAttributes().getRequired())
          .stream().map(SchemaAttributesRequired::getAttributeKey).collect(Collectors.toSet());

      Set<String> optionalKeys = notNull(schema.getAttributes().getOptional()).stream()
          .map(SchemaAttributesOptional::getAttributeKey).filter(siteSchemaRequiredKeys::contains)
          .collect(Collectors.toSet());

      optionalKeys.forEach(k -> errors.add(new ValidationErrorImpl().key(k)
          .error("attribute cannot override site schema attribute")));
    }

    return errors;
  }

  private List<SchemaCompositeKeyRecord> createCompositeKeys(
      final List<SchemaAttributesCompositeKey> compositeKeys) {

    List<SchemaCompositeKeyRecord> records = new ArrayList<>();

    for (SchemaAttributesCompositeKey compositeKey : notNull(compositeKeys)) {
      SchemaCompositeKeyRecord r =
          new SchemaCompositeKeyRecord().keys(compositeKey.getAttributeKeys());
      records.add(r);
    }

    return records;
  }

  private void deleteSchemaCompositeKeys(final String siteId, final String documentId) {

    SchemaCompositeKeyRecord r = new SchemaCompositeKeyRecord().setDocumentId(documentId);

    AttributeValue pk = AttributeValue.fromS(r.pk(siteId));
    AttributeValue sk = AttributeValue.fromS(SchemaCompositeKeyRecord.SK);

    this.db.deleteItemsBeginsWith(pk, sk);
  }

  private void deleteSchemaAttribute(final String siteId, final String documentId) {

    SchemaAttributeAllowedValueRecord r =
        new SchemaAttributeAllowedValueRecord().setDocumentId(documentId);

    AttributeValue pk = AttributeValue.fromS(r.pk(siteId));
    AttributeValue sk = AttributeValue.fromS(SchemaAttributeAllowedValueRecord.SK);

    this.db.deleteItemsBeginsWith(pk, sk);
  }

  private Collection<ValidationError> validateSchema(final Schema schema, final String name) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmpty(name)) {
      errors.add(new ValidationErrorImpl().key("name").error("'name' is required"));
    }

    if (schema.getAttributes() == null) {
      errors.add(new ValidationErrorImpl().key("schema").error("'schema' is required"));
    } else {

      SchemaAttributes schemaAttributes = schema.getAttributes();

      List<SchemaAttributesCompositeKey> compositeKeys =
          notNull(schemaAttributes.getCompositeKeys());

      List<SchemaAttributesRequired> required = notNull(schemaAttributes.getRequired());

      required.forEach(r -> {
        if (!isEmpty(r.getDefaultValue()) && !notNull(r.getAllowedValues()).isEmpty()) {

          if (!r.getAllowedValues().contains(r.getDefaultValue())) {
            errors.add(new ValidationErrorImpl().key("defaultValue")
                .error("defaultValue must be part of allowed values"));
          }
        }
      });

      validateCompositeKeys(compositeKeys, errors);
    }

    return errors;
  }

  private void validateCompositeKeys(final Collection<SchemaAttributesCompositeKey> compositeKeys,
      final Collection<ValidationError> errors) {
    compositeKeys.forEach(keys -> {
      if (notNull(keys.getAttributeKeys()).size() == 1) {
        errors.add(new ValidationErrorImpl().key("compositeKeys")
            .error("compositeKeys must have more than 1 value"));
      }
    });

    List<String> keys =
        compositeKeys.stream().map(k -> String.join(",", k.getAttributeKeys())).toList();
    if (keys.size() != new HashSet<>(keys).size()) {
      errors.add(new ValidationErrorImpl().key("compositeKeys").error("duplicate compositeKey"));
    }
  }

  private Collection<ValidationError> validateAttributes(final String siteId, final Schema schema) {

    Collection<ValidationError> errors = validateAttributes(schema.getAttributes());

    if (errors.isEmpty()) {

      List<String> requiredAttributes = notNull(schema.getAttributes().getRequired()).stream()
          .map(SchemaAttributesRequired::getAttributeKey).toList();

      List<String> optionalAttributes = notNull(schema.getAttributes().getOptional()).stream()
          .map(SchemaAttributesOptional::getAttributeKey).toList();

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

  private void validateAttributesExist(final Map<String, AttributeRecord> attributes,
      final List<String> attributeKeys, final Collection<ValidationError> errors) {

    for (String attributeKey : attributeKeys) {

      if (!attributes.containsKey(attributeKey)) {
        String errorMsg = "attribute '" + attributeKey + "' not found";
        errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
      }
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
        requiredAttributes.stream().filter(optionalAttributes::contains).toList();

    for (String attribyteKey : overlap) {
      errors.add(new ValidationErrorImpl().key(attribyteKey)
          .error("attribute '" + attribyteKey + "' is in both required & optional lists"));
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
}
