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
import com.formkiq.validation.ValidationError;

/**
 * Attribute Validator.
 */
public interface AttributeValidator {

  /**
   * Validates Deleting Attribute.
   * 
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateDeleteAttribute(String siteId, String attributeKey);

  /**
   * Validate Delete Attribute and Value.
   * 
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @param attributeValue {@link String}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateDeleteAttributeValue(String siteId, String attributeKey,
      String attributeValue);

  /**
   * Validate {@link DocumentAttributeRecord}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @param isDocumentUpdate is updating document
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validateFullAttribute(String siteId, String documentId,
      Collection<DocumentAttributeRecord> documentAttributes, boolean isDocumentUpdate);

  /**
   * Validate {@link DocumentAttributeRecord}.
   * 
   * @param siteId {@link String}
   * @param documentAttributes {@link DocumentAttributeRecord}
   * @return {@link Collection} {@link ValidationError}
   */
  Collection<ValidationError> validatePartialAttribute(String siteId,
      Collection<DocumentAttributeRecord> documentAttributes);
}
