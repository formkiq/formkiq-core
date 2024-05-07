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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributesRequired;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;

/**
 * {@link AttributeValidator} implementation.
 */
public class AttributeValidatorImpl implements AttributeValidator, DbKeys {

  /** {@link AttributeService}. */
  private AttributeService attributeService;
  /** {@link SchemaService}. */
  private SchemaService schemaService;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public AttributeValidatorImpl(final DynamoDbService dbService) {
    this.attributeService = new AttributeServiceDynamodb(dbService);
    this.schemaService = new SchemaServiceDynamodb(dbService);
  }

  private Double convertToDouble(final String value) {
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Collection<DocumentAttributeRecord> createRequiredAttributes(
      final SchemaAttributesRequired attribute, final String documentId, final String attributeKey,
      final AttributeDataType dataType) {

    Collection<DocumentAttributeRecord> records = new ArrayList<>();

    List<String> values =
        !notNull(attribute.getDefaultValues()).isEmpty() ? attribute.getDefaultValues()
            : Arrays.asList(attribute.getDefaultValue());

    for (String value : values) {

      String stringValue = AttributeDataType.STRING.equals(dataType) ? value : null;
      Boolean booleanValue =
          AttributeDataType.BOOLEAN.equals(dataType) ? Boolean.valueOf(value) : null;
      Double numberValue =
          AttributeDataType.NUMBER.equals(dataType) ? convertToDouble(value) : null;

      DocumentAttributeRecord a =
          new DocumentAttributeRecord().key(attributeKey).documentId(documentId)
              .stringValue(stringValue).booleanValue(booleanValue).numberValue(numberValue);
      a.updateValueType();

      records.add(a);
    }

    return records;
  }

  private Map<String, AttributeRecord> getAttributeRecordMap(final String siteId,
      final Collection<DocumentAttributeRecord> searchAttributes) {
    List<String> attributeKeys = searchAttributes.stream().map(a -> a.getKey()).toList();

    Map<String, AttributeRecord> attributesMap =
        this.attributeService.getAttributes(siteId, attributeKeys);
    return attributesMap;
  }

  private boolean isEmptyDefaultValue(final SchemaAttributesRequired attribute) {
    return isEmpty(attribute.getDefaultValue()) && notNull(attribute.getDefaultValues()).isEmpty();
  }

  private boolean isKeyOnlyValues(final DocumentAttributeRecord da) {
    return isEmpty(da.getStringValue()) && da.getNumberValue() == null
        && da.getBooleanValue() == null;
  }

  @Override
  public Collection<ValidationError> validate(final String siteId, final String documentId,
      final Collection<DocumentAttributeRecord> documentAttributes) {

    Collection<ValidationError> errors = new ArrayList<>();

    validateRequired(documentAttributes, errors);

    if (errors.isEmpty()) {

      Map<String, AttributeRecord> attributesMap =
          getAttributeRecordMap(siteId, documentAttributes);

      validateAttributeExistsAndDataType(attributesMap, documentAttributes, errors);

      if (errors.isEmpty()) {
        validateSitesSchema(siteId, documentId, attributesMap, documentAttributes, errors);
      }
    }

    return errors;
  }

  /**
   * Validate Attribute exists and has the correct data type.
   * 
   * @param attributesMap {@link Map} {@link AttributeRecord}
   * @param searchAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateAttributeExistsAndDataType(final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> searchAttributes,
      final Collection<ValidationError> errors) {

    for (DocumentAttributeRecord da : searchAttributes) {

      if (!DocumentAttributeValueType.COMPOSITE_STRING.equals(da.getValueType())) {

        if (!attributesMap.containsKey(da.getKey())) {

          String errorMsg = "attribute '" + da.getKey() + "' not found";
          errors.add(new ValidationErrorImpl().key(da.getKey()).error(errorMsg));

        } else {

          AttributeDataType dataType = attributesMap.get(da.getKey()).getDataType();
          validateDataType(da, dataType, errors);
        }
      }
    }
  }

  private void validateDataType(final DocumentAttributeRecord a, final AttributeDataType dataType,
      final Collection<ValidationError> errors) {

    if (AttributeDataType.STRING.equals(dataType)) {

      if (isEmpty(a.getStringValue())) {

        String errorMsg = "attribute only support string value";
        errors.add(new ValidationErrorImpl().key(a.getKey()).error(errorMsg));
      }

    } else if (AttributeDataType.NUMBER.equals(dataType)) {

      if (a.getNumberValue() == null) {

        String errorMsg = "attribute only support number value";
        errors.add(new ValidationErrorImpl().key(a.getKey()).error(errorMsg));
      }

    } else if (AttributeDataType.BOOLEAN.equals(dataType)) {

      if (a.getBooleanValue() == null) {

        String errorMsg = "attribute only support boolean value";
        errors.add(new ValidationErrorImpl().key(a.getKey()).error(errorMsg));
      }

    } else if (AttributeDataType.KEY_ONLY.equals(dataType)) {

      if (!isKeyOnlyValues(a)) {

        String errorMsg = "attribute does not support a value";
        errors.add(new ValidationErrorImpl().key(a.getKey()).error(errorMsg));
      }
    }
  }

  private void validateRequired(final Collection<DocumentAttributeRecord> searchAttributes,
      final Collection<ValidationError> errors) {

    for (DocumentAttributeRecord a : searchAttributes) {
      if (Strings.isEmpty(a.getKey())) {
        errors.add(new ValidationErrorImpl().error("'key' is missing from attribute"));
      }
    }
  }

  /**
   * Validate Site Schema.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param attributesMap {@link Map}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateSitesSchema(final String siteId, final String documentId,
      final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Collection<ValidationError> errors) {

    Schema schema = this.schemaService.getSitesSchema(siteId);

    if (schema != null) {

      SchemaAttributes attributes = schema.getAttributes();
      validateAndAddRequiredAttributes(siteId, documentId, attributes, attributesMap,
          documentAttributes, errors);

      if (errors.isEmpty()) {
        validateOptionalAttributes(siteId, documentId, attributes, attributesMap,
            documentAttributes, errors);
      }

      if (errors.isEmpty()) {
        validateAllowedValues(attributes, documentAttributes, errors);
      }
    }
  }

  private void validateOptionalAttributes(final String siteId, final String documentId,
      final SchemaAttributes attributes, final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Collection<ValidationError> errors) {

    if (!attributes.isAllowAdditionalAttributes()) {

      List<String> requiredKeys =
          attributes.getRequired().stream().map(a -> a.getAttributeKey()).toList();
      List<String> optionalKeys =
          attributes.getOptional().stream().map(a -> a.getAttributeKey()).toList();

      for (DocumentAttributeRecord documentAttribute : documentAttributes) {

        if (!DocumentAttributeValueType.COMPOSITE_STRING.equals(documentAttribute.getValueType())) {
          String attributeKey = documentAttribute.getKey();

          if (!requiredKeys.contains(attributeKey) && !optionalKeys.contains(attributeKey)) {
            errors.add(new ValidationErrorImpl().key(attributeKey).error("attribute '"
                + attributeKey + "' is not listed as a required or optional attribute"));
          }
        }
      }
    }
  }

  private void validateAllowedValues(final SchemaAttributes attributes,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Collection<ValidationError> errors) {

    Map<String, List<DocumentAttributeRecord>> documentAttributeMap =
        documentAttributes.stream().collect(Collectors.groupingBy(DocumentAttributeRecord::getKey));

    notNull(attributes.getRequired()).forEach(a -> {
      String attributeKey = a.getAttributeKey();
      List<String> allowedValues = a.getAllowedValues();
      validateAllowedValues(documentAttributeMap, attributeKey, allowedValues, errors);
    });

    notNull(attributes.getOptional()).forEach(a -> {
      String attributeKey = a.getAttributeKey();
      List<String> allowedValues = a.getAllowedValues();
      validateAllowedValues(documentAttributeMap, attributeKey, allowedValues, errors);
    });
  }

  private void validateAllowedValues(
      final Map<String, List<DocumentAttributeRecord>> documentAttributeMap,
      final String attributeKey, final List<String> allowedValues,
      final Collection<ValidationError> errors) {

    if (!notNull(allowedValues).isEmpty() && documentAttributeMap.containsKey(attributeKey)) {

      List<DocumentAttributeRecord> records = documentAttributeMap.get(attributeKey);
      for (DocumentAttributeRecord r : records) {

        if (!validateAllowedValues(allowedValues, r)) {
          errors.add(new ValidationErrorImpl().key(r.getKey()).error("invalid attribute value '"
              + r.getKey() + "', only allowed values are " + String.join(",", allowedValues)));
        }
      }
    }
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

  private void validateAndAddRequiredAttributes(final String siteId, final String documentId,
      final SchemaAttributes attributes, final Map<String, AttributeRecord> attributesMap,
      final Collection<DocumentAttributeRecord> documentAttributes,
      final Collection<ValidationError> errors) {

    List<SchemaAttributesRequired> missingRequiredAttributes = notNull(attributes.getRequired())
        .stream().filter(s -> !attributesMap.containsKey(s.getAttributeKey())).toList();

    List<String> missingAttributeKeys =
        missingRequiredAttributes.stream().map(a -> a.getAttributeKey()).toList();

    Map<String, AttributeRecord> missingAttributesMap =
        this.attributeService.getAttributes(siteId, missingAttributeKeys);

    for (SchemaAttributesRequired missingAttribute : missingRequiredAttributes) {

      String attributeKey = missingAttribute.getAttributeKey();
      AttributeDataType dataType = missingAttributesMap.get(attributeKey).getDataType();

      if (!isEmptyDefaultValue(missingAttribute) || AttributeDataType.KEY_ONLY.equals(dataType)) {

        Collection<DocumentAttributeRecord> requiredAttributes =
            createRequiredAttributes(missingAttribute, documentId, attributeKey, dataType);

        documentAttributes.addAll(requiredAttributes);

      } else {
        errors.add(new ValidationErrorImpl().key(attributeKey)
            .error("missing required attribute '" + attributeKey + "'"));
      }
    }
  }
}
