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

import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.validation.ValidationError;

/**
 * Attribute Validator.
 */
public interface AttributeValidator {

  /**
   * Get {@link AttributeRecord} {@link Map}.
   * 
   * @param siteId {@link String}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link Map} {@link String} {@link AttributeRecord}
   */
  Map<String, AttributeRecord> getAttributeRecordMap(String siteId,
      Collection<DocumentAttributeRecord> documentAttributes);

  /**
   * Validates Deleting Attribute.
   *
   * @param schemaAttributes {@link List} {@link SchemaAttributes}
   * @param attributeKeys {@link Collection} {@link String}
   * @param attributeRecordMap {@link Map}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateDeleteAttributes(List<SchemaAttributes> schemaAttributes,
      Collection<String> attributeKeys, Map<String, AttributeRecord> attributeRecordMap,
      AttributeValidationAccess validationAccess);

  /**
   * Validates Deleting Attribute.
   *
   * @param schema {@link Schema}
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateDeleteAttribute(Schema schema, String siteId,
      String attributeKey, AttributeValidationAccess validationAccess);

  /**
   * Validate Delete Attribute and Value.
   *
   * @param schema {@link Schema}
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @param attributeValue {@link String}
   * @param validationAccess {@link AttributeValidationAccess}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateDeleteAttributeValue(Schema schema, String siteId,
      String attributeKey, String attributeValue, AttributeValidationAccess validationAccess);

  /**
   * Validate {@link DocumentAttributeRecord}.
   *
   * @param schemaAttributes {@link SchemaAttributes}
   * @param siteId {@link String}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param attributesMap {@link Map}
   * @param access {@link AttributeValidationAccess}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateFullAttribute(Collection<SchemaAttributes> schemaAttributes,
      String siteId, Collection<DocumentAttributeRecord> documentAttributes,
      Map<String, AttributeRecord> attributesMap, AttributeValidationAccess access);

  /**
   * Validate {@link DocumentAttributeRecord}.
   *
   * @param schemaAttributes {@link SchemaAttributes}
   * @param siteId {@link String}
   * @param documentAttributes {@link DocumentAttributeRecord}
   * @param attributesMap {@link Map}
   * @param access {@link AttributeValidationAccess}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validatePartialAttribute(
      Collection<SchemaAttributes> schemaAttributes, String siteId,
      Collection<DocumentAttributeRecord> documentAttributes,
      Map<String, AttributeRecord> attributesMap, AttributeValidationAccess access);
}
