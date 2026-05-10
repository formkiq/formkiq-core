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
package com.formkiq.stacks.dynamodb.mappings;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DeleteResult;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.PutResult;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.model.MappingRecord;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * {@link MappingService} implementation for Dynamodb.
 */
public class MappingServiceDynamodb implements MappingService, DbKeys {

  private static boolean isMissingDefaultValue(final MappingAttribute attribute) {
    return isEmpty(attribute.getDefaultValue()) && notNull(attribute.getDefaultValues()).isEmpty();
  }

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /** {@link AttributeService}. */
  private final AttributeService attributeService;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public MappingServiceDynamodb(final DynamoDbService dbService) {
    this.db = dbService;
    this.attributeService = new AttributeServiceDynamodb(dbService);
  }

  @Override
  public boolean deleteMapping(final String siteId, final String mappingId) {

    MappingRecord r = new MappingRecord().setDocumentId(mappingId);
    DynamoDbKey key = new DynamoDbKey(r.pk(siteId), r.sk(), null, null, null, null);
    DeleteResult deleteResult = this.db.deleteItem(key);
    UserActivityContext.setDelete(ActivityResourceType.MAPPING, deleteResult.attributes());
    return deleteResult.isDelete();
  }

  @Override
  public Pagination<MappingRecord> findMappings(final String siteId, final String nextToken,
      final int limit) {

    Map<String, AttributeValue> startkey = new StringToMapAttributeValue().apply(nextToken);

    QueryConfig config = new QueryConfig().indexName(DbKeys.GSI1).scanIndexForward(Boolean.TRUE);
    AttributeValue pk = AttributeValue.fromS(createDatabaseKey(siteId, MappingRecord.PREFIX_PK));
    AttributeValue sk = AttributeValue.fromS(MappingRecord.PREFIX_SK_GSI1);
    QueryResponse response = this.db.queryBeginsWith(config, pk, sk, startkey, limit);

    List<Map<String, AttributeValue>> keys =
        response.items().stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    List<Map<String, AttributeValue>> attrs = this.db.getBatch(new BatchGetConfig(), keys);

    List<MappingRecord> list =
        attrs.stream().map(a -> new MappingRecord().getFromAttributes(siteId, a)).toList();

    return new Pagination<>(list, response.lastEvaluatedKey());
  }

  @Override
  public List<MappingAttribute> getAttributes(final MappingRecord mapping) {
    Gson gson = new GsonBuilder().create();
    Type listType = new TypeToken<ArrayList<MappingAttribute>>() {}.getType();
    List<MappingAttribute> attributes = gson.fromJson(mapping.getAttributes(), listType);
    return notNull(attributes);
  }

  @Override
  public List<MappingClassification> getClassifications(final MappingRecord mapping) {
    Gson gson = new GsonBuilder().create();
    Type listType = new TypeToken<ArrayList<MappingClassification>>() {}.getType();
    List<MappingClassification> classifications =
        gson.fromJson(mapping.getClassifications(), listType);
    return notNull(classifications);
  }

  @Override
  public MappingRecord getMapping(final String siteId, final String mappingId) {

    MappingRecord r = new MappingRecord().setDocumentId(mappingId);
    Map<String, AttributeValue> attr = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));
    return !attr.isEmpty() ? r.getFromAttributes(siteId, attr) : null;
  }

  private boolean isKeysOnlyAttribute(final Map<String, AttributeRecord> attributes,
      final MappingAttribute attribute) {
    boolean match = false;
    String attributeKey = attribute.getAttributeKey();

    if (!isEmpty(attributeKey)) {
      AttributeRecord a = attributes.get(attributeKey);
      match = a != null && AttributeDataType.KEY_ONLY.equals(a.getDataType());
    }

    return match;
  }

  private boolean isLabelRequired(final MappingAttributeSourceType type) {
    return !MappingAttributeSourceType.DATA_CLASSIFICATION.equals(type)
        && !MappingAttributeSourceType.METADATA_EXTRACTION_RESULT.equals(type)
        && !MappingAttributeSourceType.MALWARE_SCAN.equals(type);
  }

  @Override
  public MappingRecord saveMapping(final String siteId, final String mappingId,
      final Mapping mapping) throws ValidationException {

    boolean isNew = mappingId == null;
    validate(siteId, mapping);

    MappingRecord record = new MappingToMappingRecord().apply(mapping);

    if (!isEmpty(mappingId)) {
      record.setDocumentId(mappingId);
    }

    ReturnValue rv = isNew ? null : ReturnValue.ALL_OLD;
    Map<String, AttributeValue> attributes = record.getAttributes(siteId);
    PutResult putResult = this.db.putItem(attributes, rv);

    if (isNew) {
      UserActivityContext.setCreate(ActivityResourceType.MAPPING, attributes);
    } else {
      UserActivityContext.setUpdate(ActivityResourceType.MAPPING, putResult.attributes(),
          attributes);
    }

    return record;
  }

  private void validate(final String siteId, final Mapping record) {

    ValidationBuilder vb = new ValidationBuilder();

    vb.isRequired("name", record.name());

    boolean hasAttributes = !notNull(record.attributes()).isEmpty();
    boolean hasClassifications = !notNull(record.classifications()).isEmpty();

    if (!hasAttributes && !hasClassifications) {
      vb.addError("attributes", "'attributes' or 'classifications' is required");
    }

    if (hasAttributes && hasClassifications) {
      vb.addError("attributes", "'attributes' and 'classifications' cannot both be set");
    }

    vb.check();

    if (hasAttributes) {
      validateAttributes(vb, siteId, record);
    }

    if (hasClassifications) {
      validateClassifications(vb, record.classifications());
    }

    vb.check();
  }

  private void validate(final ValidationBuilder vb, final Map<String, AttributeRecord> attributes,
      final MappingAttribute attribute, final int index) {

    if (isEmpty(attribute.getAttributeKey())) {
      vb.addError("attribute[" + index + "].attributeKey", "'attributeKey' is required");
    }

    if (attribute.getSourceType() == null) {
      vb.addError("attribute[" + index + "].sourceType", "'sourceType' is required");
    }

    if (MappingAttributeSourceType.MANUAL.equals(attribute.getSourceType())) {

      validateManual(vb, attributes, attribute, index);

    } else if (MappingAttributeSourceType.METADATA_EXTRACTION_RESULT
        .equals(attribute.getSourceType())) {

      if (isEmpty(attribute.getLlmPromptEntityName())) {
        vb.addError("attribute[" + index + "].llmPromptEntityName",
            "'llmPromptEntityName' is required");
      }

    } else if (isLabelRequired(attribute.getSourceType())) {

      if (attribute.getLabelMatchingType() == null) {
        vb.addError("attribute[" + index + "].labelMatchingType",
            "'labelMatchingType' is required");
      }

      if (notNull(attribute.getLabelTexts()).isEmpty()) {
        vb.addError("attribute[" + index + "].labelTexts", "'labelTexts' is required");
      }
    }
  }

  private void validateAttributes(final ValidationBuilder vb, final String siteId,
      final Mapping record) {

    int size = record.attributes().size();

    List<String> attributeKeys =
        notNull(record.attributes()).stream().map(MappingAttribute::getAttributeKey)
            .filter(attributeKey -> !isEmpty(attributeKey)).toList();

    Map<String, AttributeRecord> attributes =
        this.attributeService.getAttributes(siteId, attributeKeys);

    for (int i = 0; i < size; i++) {

      MappingAttribute attribute = record.attributes().get(i);
      validate(vb, attributes, attribute, i);

      if (vb.isEmpty()) {
        validateMetadataField(vb, attribute, i);
      }
    }

    vb.check();

    for (String attributeKey : attributeKeys) {
      if (!attributes.containsKey(attributeKey)) {
        vb.addError(attributeKey, "invalid attribute '" + attributeKey + "'");
      }
    }

    if (attributeKeys.size() != new HashSet<>(attributeKeys).size()) {
      vb.addError(null, "duplicate attributes in mapping");
    }
  }

  private void validateClassificationCondition(final ValidationBuilder vb,
      final MappingClassificationCondition condition, final int classificationIndex,
      final int conditionIndex) {

    String key = "classification[" + classificationIndex + "].condition[" + conditionIndex + "]";

    vb.isRequired(key + ".sourceType", condition.sourceType());

    MappingClassificationConditionSourceType sourceType = condition.sourceType();

    if (MappingClassificationConditionSourceType.CONTENT.equals(sourceType)) {
      vb.isRequired(key + ".matchingType", condition.matchingType());
      vb.isRequired(key + ".text", condition.text());
    } else if (MappingClassificationConditionSourceType.DATA_CLASSIFICATION.equals(sourceType)) {
      vb.isRequired(key + ".resultKey", condition.resultKey());
      vb.isRequired(key + ".resultValue", condition.resultValue());
      vb.isRequired(key + ".matchingType", condition.matchingType());
    } else if (MappingClassificationConditionSourceType.METADATA_EXTRACTION_RESULT
        .equals(sourceType)) {
      vb.isRequired(key + ".resultKey", condition.resultKey());
      vb.isRequired(key + ".resultValue", condition.resultValue());
      vb.isRequired(key + ".matchingType", condition.matchingType());
      vb.isRequired(key + ".llmPromptEntityName", condition.llmPromptEntityName());
    }
  }

  private void validateClassifications(final ValidationBuilder vb,
      final List<MappingClassification> classifications) {

    int classificationsWithoutConditions = 0;

    for (int i = 0; i < classifications.size(); i++) {
      MappingClassification classification = classifications.get(i);

      if (isEmpty(classification.classificationId())) {
        vb.addError("classification[" + i + "].classificationId", "'classificationId' is required");
      }

      List<MappingClassificationCondition> conditions = notNull(classification.conditions());

      if (conditions.isEmpty()) {
        classificationsWithoutConditions++;
      }

      for (int j = 0; j < conditions.size(); j++) {
        validateClassificationCondition(vb, conditions.get(j), i, j);
      }
    }

    if (classifications.size() > 1 && classificationsWithoutConditions > 1) {
      vb.addError("classifications", "only one classification can omit conditions");
    }
  }

  private void validateManual(final ValidationBuilder vb,
      final Map<String, AttributeRecord> attributes, final MappingAttribute attribute,
      final int index) {

    if (isKeysOnlyAttribute(attributes, attribute)) {

      if (!isMissingDefaultValue(attribute)) {
        vb.addError("attribute[" + index + "].defaultValue",
            "'defaultValue' or 'defaultValues' cannot be used with KEY_ONLY attribute");
      }

    } else if (isMissingDefaultValue(attribute)) {
      vb.addError("attribute[" + index + "].defaultValue",
          "'defaultValue' or 'defaultValues' is required");
    }
  }

  private void validateMetadataField(final ValidationBuilder vb, final MappingAttribute attribute,
      final int index) {

    if (MappingAttributeSourceType.METADATA.equals(attribute.getSourceType())
        && attribute.getMetadataField() == null) {
      vb.addError("attribute[" + index + "].metadataField", "'metadataField' is required");
    }
  }
}
