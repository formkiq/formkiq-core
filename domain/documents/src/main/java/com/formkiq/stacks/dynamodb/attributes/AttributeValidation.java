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

import com.formkiq.aws.dynamodb.attributes.AttributeAccessApproval;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;

/**
 * Attribute Validation Wrapper.
 */
public class AttributeValidation {

  /** {@link AttributeValidationType}. */
  private final AttributeValidationType validationType;
  /** {@link AttributeAccessApproval}. */
  private final AttributeAccessApproval accessApproval;

  /**
   * constructor.
   *
   * @param attributeValidationType {@link AttributeValidationType}
   * @param attributeAccessApproval {@link AttributeAccessApproval}
   */
  public AttributeValidation(final AttributeValidationType attributeValidationType,
      final AttributeAccessApproval attributeAccessApproval) {
    this.validationType = attributeValidationType;
    this.accessApproval = attributeAccessApproval;
  }

  /**
   * constructor.
   * 
   * @param attributeValidationType {@link AttributeValidationType}
   * @param attributeValidationAccess {@link AttributeValidationAccess}
   */
  public AttributeValidation(final AttributeValidationType attributeValidationType,
      final AttributeValidationAccess attributeValidationAccess) {
    this(attributeValidationType, new AttributeAccessApproval(attributeValidationAccess));
  }

  /**
   * Get {@link AttributeAccessApproval}.
   *
   * @return {@link AttributeAccessApproval}
   */
  public AttributeAccessApproval getAccessApproval() {
    return this.accessApproval;
  }

  /**
   * Get {@link AttributeValidationAccess}.
   * 
   * @return {@link AttributeValidationAccess}
   */
  public AttributeValidationAccess getValidationAccess() {
    return this.accessApproval.defaultAccess();
  }

  /**
   * Get {@link AttributeValidationType}.
   * 
   * @return {@link AttributeValidationType}
   */
  public AttributeValidationType getValidationType() {
    return this.validationType;
  }
}
