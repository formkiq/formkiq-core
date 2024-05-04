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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.stacks.dynamodb.attributes.AttributeDataType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidator;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidatorImpl;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * DynamoDB implementation for {@link SchemaService}.
 */
public class SchemaServiceDynamodb implements SchemaService {

  /** {@link AttributeValidator}. */
  private AttributeValidator attributeValidation;
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
    this.attributeValidation = new AttributeValidatorImpl(dbService);
  }

  @Override
  public SiteSchemasRecord getSitesSchema(final String siteId) {
    SiteSchemasRecord r = new SiteSchemasRecord();

    Map<String, AttributeValue> attr = this.db.get(r.fromS(r.pk(siteId)), r.fromS(r.sk()));

    if (!attr.isEmpty()) {
      r = r.getFromAttributes(siteId, attr);
    } else {
      r = null;
    }

    return r;
  }

  @Override
  public Collection<ValidationError> setSitesSchema(final String siteId, final String name,
      final String schemaJson, final Schema schema) {

    Collection<ValidationError> errors = validate(siteId, name, schemaJson, schema);

    // TODO validation
    // Todo update all documents if changed

    // If you add a new attribute to an existing schema, it would require a default attribute,
    // maybe?
    // default value
    // If you want to remove an attribute on a schema that doesn't allow extra fields, I think it
    // may need to just not allow for now. Later, it could try and do some painful check

    if (errors.isEmpty()) {
      SiteSchemasRecord r = new SiteSchemasRecord().name(name).schema(schemaJson);
      this.db.putItem(r.getAttributes(siteId));
    }

    return errors;
  }

  private Collection<ValidationError> validate(final String siteId, final String name,
      final String schemaJson, final Schema schema) {

    Collection<ValidationError> errors = new ArrayList<>();

    if (isEmpty(name)) {
      errors.add(new ValidationErrorImpl().key("name").error("'name' is required"));
    }

    if (isEmpty(schemaJson) || schema.getAttributes() == null) {
      errors.add(new ValidationErrorImpl().key("schema").error("'schema' is required"));
    } else {

      SchemaAttributes schemaAttributes = schema.getAttributes();
      if (notNull(schemaAttributes.getRequired()).isEmpty()
          && notNull(schemaAttributes.getOptional()).isEmpty()) {
        errors.add(new ValidationErrorImpl()
            .error("either 'required' or 'optional' attributes list is required"));
      }
    }

    if (errors.isEmpty()) {
      errors = validateAttributes(siteId, schema);
    }

    return errors;
  }

  private Collection<ValidationError> validateAttributes(final String siteId, final Schema schema) {

    Collection<ValidationError> errors = new ArrayList<>();

    List<String> requiredAttributes = notNull(schema.getAttributes().getRequired()).stream()
        .map(a -> a.getAttributeKey()).toList();

    List<String> optionalAttributes = notNull(schema.getAttributes().getOptional()).stream()
        .map(a -> a.getAttributeKey()).toList();

    List<String> attributeKeys =
        Stream.concat(requiredAttributes.stream(), optionalAttributes.stream()).toList();

    Map<String, AttributeDataType> attributeDataTypes =
        this.attributeValidation.getAttributeDataType(siteId, attributeKeys);

    validateAttributesExist(attributeDataTypes, attributeKeys, errors);

    validateOverlap(requiredAttributes, optionalAttributes, errors);

    validateCompositeAttributes(schema, attributeKeys, errors);

    return errors;
  }

  private void validateAttributesExist(final Map<String, AttributeDataType> attributeDataTypes,
      final List<String> attributeKeys, final Collection<ValidationError> errors) {

    for (String attributeKey : attributeKeys) {

      if (!attributeDataTypes.containsKey(attributeKey)) {
        String errorMsg = "attribute '" + attributeKey + "' not found";
        errors.add(new ValidationErrorImpl().key(attributeKey).error(errorMsg));
      }
    }
  }

  private void validateCompositeAttributes(final Schema schema, final List<String> attributeKeys,
      final Collection<ValidationError> errors) {

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

  private void validateOverlap(final List<String> requiredAttributes,
      final List<String> optionalAttributes, final Collection<ValidationError> errors) {

    List<String> overlap =
        requiredAttributes.stream().filter(item -> optionalAttributes.contains(item)).toList();

    for (String attribyteKey : overlap) {
      errors.add(new ValidationErrorImpl().key(attribyteKey)
          .error("attribute '" + attribyteKey + "' is in both required & optional lists"));
    }
  }

}
