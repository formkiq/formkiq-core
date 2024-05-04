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

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * {@link AttributeValidator} implementation.
 */
public class AttributeValidatorImpl implements AttributeValidator, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public AttributeValidatorImpl(final DynamoDbService dbService) {
    this.db = dbService;
  }

  private boolean isKeyOnlyValues(final DocumentAttributeRecord da) {
    return isEmpty(da.getStringValue()) && da.getNumberValue() == null
        && da.getBooleanValue() == null;
  }

  @Override
  public Collection<ValidationError> validate(final String siteId,
      final Collection<DocumentAttributeRecord> searchAttributes) {

    Collection<ValidationError> errors = new ArrayList<>();

    validateRequired(searchAttributes, errors);

    if (errors.isEmpty()) {
      validateAttributeExistsAndDataType(siteId, searchAttributes, errors);
    }

    return errors;
  }

  /**
   * Validate Attribute exists and has the correct data type.
   * 
   * @param siteId {@link String}
   * @param searchAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param errors {@link Collection} {@link ValidationError}
   */
  private void validateAttributeExistsAndDataType(final String siteId,
      final Collection<DocumentAttributeRecord> searchAttributes,
      final Collection<ValidationError> errors) {

    List<String> attributeKeys = searchAttributes.stream().map(a -> a.getKey()).toList();
    Map<String, AttributeDataType> attributesMap = getAttributeDataType(siteId, attributeKeys);

    for (DocumentAttributeRecord da : searchAttributes) {

      if (!attributesMap.containsKey(da.getKey())) {

        String errorMsg = "attribute '" + da.getKey() + "' not found";
        errors.add(new ValidationErrorImpl().key(da.getKey()).error(errorMsg));

      } else {

        AttributeDataType dataType = attributesMap.get(da.getKey());
        validateDataType(da, dataType, errors);
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

  @Override
  public Map<String, AttributeDataType> getAttributeDataType(final String siteId,
      final Collection<String> attributeKeys) {

    List<Map<String, AttributeValue>> keys = attributeKeys.stream().map(key -> {
      AttributeRecord r = new AttributeRecord().documentId(key);
      return Map.of(PK, r.fromS(r.pk(siteId)), SK, r.fromS(r.sk()));
    }).distinct().toList();

    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,#key,dataType")
        .expressionAttributeNames(Map.of("#key", "key"));

    List<Map<String, AttributeValue>> batch = this.db.getBatch(config, keys);
    Map<String, AttributeDataType> attributesMap = batch.stream()
        .map(a -> Map.of(a.get("key").s(), AttributeDataType.valueOf(a.get("dataType").s())))
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return attributesMap;
  }
}
