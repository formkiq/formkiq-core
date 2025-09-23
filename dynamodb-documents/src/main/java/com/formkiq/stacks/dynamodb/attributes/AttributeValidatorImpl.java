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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesOptional;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesRequired;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;

/**
 * {@link AttributeValidator} implementation.
 */
public class AttributeValidatorImpl implements AttributeValidator, DbKeys {

  /** {@link AttributeService}. */
  private final AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public AttributeValidatorImpl(final DynamoDbService dbService) {
    this.db = dbService;
    this.attributeService = new AttributeServiceDynamodb(dbService);
  }

  @Override
  public Map<String, AttributeRecord> getAttributeRecordMap(final String siteId,
      final Collection<DocumentAttributeRecord> documentAttributes) {

    List<String> attributeKeys = documentAttributes.stream().map(DocumentAttributeRecord::getKey)
        .filter(key -> !isEmpty(key)).toList();

    return this.attributeService.getAttributes(siteId, attributeKeys);
  }

  private boolean isEmptyDefaultValue(final SchemaAttributesRequired attribute) {
    return isEmpty(attribute.getDefaultValue()) && notNull(attribute.getDefaultValues()).isEmpty();
  }

  private boolean isKeyOnlyValues(final DocumentAttributeRecord da) {
    return isEmpty(da.getStringValue()) && da.getNumberValue() == null
        && da.getBooleanValue() == null;
  }

  private boolean validateAllowedValues(final List<String> allowedValues,
      final DocumentAttributeRecord r) {

    boolean matchString = DocumentAttributeValueType.STRING.equals(r.getValueType())
        && allowedValues.contains(r.getStringValue());

    boolean matchBoolean = DocumentAttributeValueType.BOOLEAN.equals(r.getValueType())
        && allowedValues.contains(r.getBooleanValue().toString());

    boolean matchNumber = DocumentAttributeValueType.NUMBER.equals(r.getValueType())
        && allowedValues.contains(r.getNumberValue().toString());

    return matchString || matchBoolean || matchNumber;
  }

  private void validateAllowedValues(
      final Map<String, List<DocumentAttributeRecord>> documentAttributeMap,
      final String attributeKey, final List<String> allowedValues, final ValidationBuilder vb) {

    if (!notNull(allowedValues).isEmpty() && documentAttributeMap.containsKey(attributeKey)) {

      List<DocumentAttributeRecord> records = documentAttributeMap.get(attributeKey);
      for (DocumentAttributeRecord r : records) {

        if (!validateAllowedValues(allowedValues, r)) {
          vb.addError(r.getKey(), "invalid attribute value '" + r.getKey()
              + "', only allowed values are " + String.join(",", allowedValues));
        }
      }
    }
  }

  private void validateAllowedValues(final SchemaAttributes attributes,
      final Collection<DocumentAttributeRecord> documentAttributes, final ValidationBuilder vb) {

    Map<String, List<DocumentAttributeRecord>> documentAttributeMap =
        documentAttributes.stream().collect(Collectors.groupingBy(DocumentAttributeRecord::getKey));

    notNull(attributes.getRequired()).forEach(a -> {
      String attributeKey = a.getAttributeKey();
      List<String> allowedValues = a.getAllowedValues();
      validateAllowedValues(documentAttributeMap, attributeKey, allowedValues, vb);
      validateNumberOfValues(documentAttributeMap, attributeKey, a.minNumberOfValues(),
          a.maxNumberOfValues(), vb);
    });

    notNull(attributes.getOptional()).forEach(a -> {
      String attributeKey = a.getAttributeKey();
      List<String> allowedValues = a.getAllowedValues();
      validateAllowedValues(documentAttributeMap, attributeKey, allowedValues, vb);
      validateNumberOfValues(documentAttributeMap, attributeKey, a.minNumberOfValues(),
          a.maxNumberOfValues(), vb);
    });
  }

  private void validateNumberOfValues(
      final Map<String, List<DocumentAttributeRecord>> documentAttributeMap,
      final String attributeKey, final Double minNumberOfValues, final Double maxNumberOfValues,
      final ValidationBuilder vb) {

    if (documentAttributeMap.containsKey(attributeKey) && minNumberOfValues != null
        && maxNumberOfValues != null) {

      double max = maxNumberOfValues < 0 ? Double.MAX_VALUE : maxNumberOfValues;
      int count = documentAttributeMap.get(attributeKey).size();
      if (count < minNumberOfValues) {
        vb.addError("minNumberOfValues",
            String.format("number of attributes %d is less than minimum of %d", count,
                minNumberOfValues.intValue()));
      } else if (count > max) {
        vb.addError("maxNumberOfValues",
            String.format("number of attributes %d is more than maximum of %d", count,
                maxNumberOfValues.intValue()));
      }
    }
  }

  /**
   * Validate Attribute exists and has the correct data type.
   *
   * @param siteId {@link String}
   * @param attributesMap {@link Map} {@link AttributeRecord}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param access {@link AttributeValidationAccess}
   * @param vb {@link ValidationBuilder}
   */
  private void validateAttributeExistsAndDataType(final String siteId,
      final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final AttributeValidationAccess access, final ValidationBuilder vb) {

    Collection<String> savedReservedKeys = new HashSet<>();

    for (DocumentAttributeRecord da : documentAttributes) {

      if (isProcessAttribute(da, savedReservedKeys)) {

        if (!attributesMap.containsKey(da.getKey())) {

          AttributeKeyReserved reserved = AttributeKeyReserved.find(da.getKey());

          if (reserved != null) {

            attributeService.addAttribute(access, siteId, da.getKey(), AttributeDataType.STRING,
                AttributeType.STANDARD, true);

            savedReservedKeys.add(da.getKey());

          } else {
            String errorMsg = "attribute '" + da.getKey() + "' not found";
            vb.addError(da.getKey(), errorMsg);
          }

        } else {

          AttributeRecord attribute = attributesMap.get(da.getKey());
          AttributeDataType dataType = attribute.getDataType();
          validateDataType(siteId, da, dataType, vb);

          validateAttributeTypeOpaOrGoverance(attribute, access, vb);
        }
      }
    }
  }

  private void validateAttributeTypeOpaOrGoverance(final AttributeRecord attribute,
      final AttributeValidationAccess access, final ValidationBuilder vb) {
    boolean restrictedType = AttributeType.OPA.equals(attribute.getType())
        || AttributeType.GOVERNANCE.equals(attribute.getType());

    if (restrictedType) {
      vb.isRequired(attribute.getKey(), access.isAdminOrGovernRole(),
          "attribute can only be changed by GOVERN or ADMIN role");
    }
  }

  private boolean isProcessAttribute(final DocumentAttributeRecord da) {
    return isProcessAttribute(da, Collections.emptyList());
  }

  private boolean isProcessAttribute(final DocumentAttributeRecord da,
      final Collection<String> savedReservedKeys) {
    return !DocumentAttributeValueType.COMPOSITE_STRING.equals(da.getValueType())
        && !DocumentAttributeValueType.CLASSIFICATION.equals(da.getValueType())
        && !savedReservedKeys.contains(da.getKey());
  }

  private void validateDataType(final String siteId, final DocumentAttributeRecord a,
      final AttributeDataType dataType, final ValidationBuilder vb) {
    switch (dataType) {
      case STRING ->
        vb.isRequired(a.getKey(), a.getStringValue(), "attribute only support string value");
      case ENTITY -> validateEntity(siteId, a, vb);
      case NUMBER ->
        vb.isRequired(a.getKey(), a.getNumberValue(), "attribute only support number value");
      case BOOLEAN -> vb.isRequired(a.getKey(), a.getBooleanValue() != null,
          "attribute only support boolean value");
      case KEY_ONLY -> {
        if (!isKeyOnlyValues(a)) {
          String errorMsg = "attribute does not support a value";
          vb.addError(a.getKey(), errorMsg);
        }
      }
      default -> {
      }
    }
  }

  private void validateEntity(final String siteId, final DocumentAttributeRecord a,
      final ValidationBuilder vb) {
    vb.isRequired("entityTypeId", a.getStringValue());
    vb.isRequired("entityId", a.getStringValue());

    if (vb.isEmpty()) {
      if (a.getStringValue().endsWith("#null")) {
        vb.addError("entityId", "'entityId' is required");
      }
      if (a.getStringValue().startsWith("null#")) {
        vb.addError("entityTypeId", "'entityTypeId' is required");
      }
    }

    if (vb.isEmpty()) {
      String[] s = a.getStringValue().split("#");
      DynamoDbKey entityType = EntityTypeRecord.builder().nameEmpty()
          .namespace(EntityTypeNamespace.CUSTOM).documentId(s[0]).buildKey(siteId);
      vb.isRequired("entityTypeId", this.db.exists(entityType), "EntityTypeId does not exist");

      DynamoDbKey entity =
          EntityRecord.builder().name("").entityTypeId(s[0]).documentId(s[1]).buildKey(siteId);
      vb.isRequired("entityId", this.db.exists(entity), "EntityId does not exist");
    }
  }

  @Override
  public Collection<ValidationError> validateDeleteAttributes(
      final List<SchemaAttributes> schemaAttributes, final Collection<String> attributeKeys,
      final Map<String, AttributeRecord> attributeRecordMap,
      final AttributeValidationAccess validationAccess) {

    ValidationBuilder vb = new ValidationBuilder();

    for (String attributeKey : attributeKeys) {

      vb.isRequired("key", attributeKey);

      if (vb.isEmpty()) {
        AttributeRecord attributeRecord = attributeRecordMap.get(attributeKey);
        validateAttributeTypeOpaOrGoverance(attributeRecord, validationAccess, vb);

        if (vb.isEmpty()) {
          for (SchemaAttributes schemaAttribute : schemaAttributes) {
            validateRequiredAttribute(schemaAttribute, attributeKey, vb);
          }
        }
      }
    }

    return vb.getErrors();
  }

  @Override
  public Collection<ValidationError> validateDeleteAttribute(final Schema schema,
      final String siteId, final String attributeKey,
      final AttributeValidationAccess validationAccess) {

    ValidationBuilder vb = new ValidationBuilder();

    vb.isRequired("key", attributeKey);

    if (vb.isEmpty()) {
      validateOpaGoveranceAttribute(siteId, attributeKey, validationAccess, vb);

      if (vb.isEmpty() && schema != null) {
        validateRequiredAttribute(schema.getAttributes(), attributeKey, vb);
      }
    }

    return vb.getErrors();
  }

  private void validateOpaGoveranceAttribute(final String siteId, final String attributeKey,
      final AttributeValidationAccess validationAccess, final ValidationBuilder vb) {

    if (AttributeValidationAccess.DELETE.equals(validationAccess)) {
      AttributeRecord attribute = this.attributeService.getAttribute(siteId, attributeKey);
      validateOpaGoveranceAttribute(attribute, validationAccess, vb);
    }
  }

  @Deprecated
  private void validateOpaGoveranceAttribute(final AttributeRecord attribute,
      final AttributeValidationAccess validationAccess, final ValidationBuilder vb) {

    if (isUpdateDeleteOrSet(validationAccess)) {

      if (attribute != null && AttributeType.OPA.equals(attribute.getType())) {
        String attributeKey = attribute.getKey();
        String errorMsg =
            "attribute '" + attributeKey + "' is an access attribute, can only be changed by Admin";
        vb.addError(attributeKey, errorMsg);
      }
    }
  }

  private boolean isUpdateDeleteOrSet(final AttributeValidationAccess validationAccess) {
    return AttributeValidationAccess.DELETE.equals(validationAccess)
        || AttributeValidationAccess.UPDATE.equals(validationAccess)
        || AttributeValidationAccess.SET.equals(validationAccess)
        || AttributeValidationAccess.SET_ITEM.equals(validationAccess);
  }

  @Override
  public Collection<ValidationError> validateDeleteAttributeValue(final Schema schema,
      final String siteId, final String attributeKey, final String attributeValue,
      final AttributeValidationAccess validationAccess) {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("key", attributeKey);
    vb.isRequired("key", attributeValue);

    if (vb.isEmpty()) {
      validateOpaGoveranceAttribute(siteId, attributeKey, validationAccess, vb);

      if (vb.isEmpty() && schema != null) {
        validateRequiredAttribute(schema.getAttributes(), attributeKey, vb);
      }
    }

    return vb.getErrors();
  }

  @Override
  public Collection<ValidationError> validateFullAttribute(
      final Collection<SchemaAttributes> schemaAttributes, final String siteId,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Map<String, AttributeRecord> attributesMap, final AttributeValidationAccess access) {

    ValidationBuilder vb = new ValidationBuilder();

    validateRequired(documentAttributes, vb);

    if (vb.isEmpty()) {
      validateAttributeExistsAndDataType(siteId, attributesMap, documentAttributes, access, vb);

      notNull(schemaAttributes).forEach(schemaAttribute -> validateSitesSchema(schemaAttribute,
          siteId, attributesMap, documentAttributes, vb));
    }

    return vb.getErrors();
  }

  private void validateOptionalAttributes(final SchemaAttributes attributes,
      final Collection<DocumentAttributeRecord> documentAttributes, final ValidationBuilder vb) {

    if (!attributes.isAllowAdditionalAttributes()) {

      List<String> requiredKeys =
          attributes.getRequired().stream().map(SchemaAttributesRequired::getAttributeKey).toList();
      List<String> optionalKeys =
          attributes.getOptional().stream().map(SchemaAttributesOptional::getAttributeKey).toList();

      for (DocumentAttributeRecord documentAttribute : documentAttributes) {

        if (isProcessAttribute(documentAttribute)) {
          String attributeKey = documentAttribute.getKey();

          if (!requiredKeys.contains(attributeKey) && !optionalKeys.contains(attributeKey)) {
            vb.addError(attributeKey, "attribute '" + attributeKey
                + "' is not listed as a required or optional attribute");
          }
        }
      }
    }
  }

  @Override
  public Collection<ValidationError> validatePartialAttribute(
      final Collection<SchemaAttributes> schemaAttributes, final String siteId,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Map<String, AttributeRecord> attributesMap, final AttributeValidationAccess access) {

    ValidationBuilder vb = new ValidationBuilder();

    validateRequired(documentAttributes, vb);

    if (vb.isEmpty()) {
      validateAttributeExistsAndDataType(siteId, attributesMap, documentAttributes, access, vb);

      if (schemaAttributes != null) {
        notNull(schemaAttributes).forEach(
            schemaAttribute -> validateAllowedValues(schemaAttribute, documentAttributes, vb));
      }
    }

    return vb.getErrors();
  }

  private void validateRequired(final Collection<DocumentAttributeRecord> documentAttributes,
      final ValidationBuilder vb) {

    for (DocumentAttributeRecord a : documentAttributes) {

      if (!DocumentAttributeValueType.CLASSIFICATION.equals(a.getValueType())
          && !DocumentAttributeValueType.RELATIONSHIPS.equals(a.getValueType())
          && Strings.isEmpty(a.getKey())) {
        vb.addError("key", "'key' is missing from attribute");
      }

      if (Strings.isEmpty(a.getUserId())) {
        vb.addError("userId", "'userId' is missing from attribute");
      }
    }
  }

  private void validateRequiredAttribute(final SchemaAttributes attributes,
      final String attributeKey, final ValidationBuilder vb) {

    Optional<SchemaAttributesRequired> o = notNull(attributes.getRequired()).stream()
        .filter(r -> r.getAttributeKey().equals(attributeKey)).findAny();

    vb.isRequired(attributeKey, o.isEmpty(), "'" + attributeKey + "' is a required attribute");
  }

  private void validateRequiredAttributes(final String siteId, final SchemaAttributes attributes,
      final Map<String, AttributeRecord> attributesMap, final ValidationBuilder vb) {

    List<SchemaAttributesRequired> missingRequiredAttributes = notNull(attributes.getRequired())
        .stream().filter(s -> !attributesMap.containsKey(s.getAttributeKey())).toList();

    List<String> missingAttributeKeys =
        missingRequiredAttributes.stream().map(SchemaAttributesRequired::getAttributeKey).toList();

    Map<String, AttributeRecord> missingAttributesMap =
        this.attributeService.getAttributes(siteId, missingAttributeKeys);

    for (SchemaAttributesRequired missingAttribute : missingRequiredAttributes) {

      String attributeKey = missingAttribute.getAttributeKey();
      AttributeDataType dataType = missingAttributesMap.get(attributeKey).getDataType();

      if (isEmptyDefaultValue(missingAttribute) && !AttributeDataType.KEY_ONLY.equals(dataType)) {
        vb.addError(attributeKey, "missing required attribute '" + attributeKey + "'");
      }
    }
  }

  /**
   * Validate Site Schema.
   *
   * @param schemaAttributes {@link SchemaAttributes}
   * @param siteId {@link String}
   * @param attributesMap {@link Map}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param vb {@link ValidationBuilder}
   */
  private void validateSitesSchema(final SchemaAttributes schemaAttributes, final String siteId,
      final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> documentAttributes, final ValidationBuilder vb) {

    if (schemaAttributes != null) {

      validateRequiredAttributes(siteId, schemaAttributes, attributesMap, vb);

      validateOptionalAttributes(schemaAttributes, documentAttributes, vb);

      validateAllowedValues(schemaAttributes, documentAttributes, vb);
    }
  }
}
