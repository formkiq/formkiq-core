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

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Schema Attributes.
 */
@Reflectable
public class SchemaAttributes {

  /** {@link List} {@link SchemaAttributesCompositeKey}. */
  private List<SchemaAttributesCompositeKey> compositeKeys;
  /** {@link List} {@link SchemaAttributesRequired}. */
  private List<SchemaAttributesRequired> required;
  /** {@link List} {@link SchemaAttributesOptional}. */
  private List<SchemaAttributesOptional> optional;
  /** Allow additional attributes. */
  private boolean allowAdditionalAttributes;

  /**
   * constructor.
   */
  public SchemaAttributes() {

  }

  /**
   * Get Composite Keys.
   * 
   * @return {@link List} {@link SchemaAttributesCompositeKey}
   */
  public List<SchemaAttributesCompositeKey> getCompositeKeys() {
    return this.compositeKeys;
  }

  /**
   * Set Composite Keys.
   * 
   * @param schemaCompositeKeys {@link List} {@link SchemaAttributesCompositeKey}
   * @return {@link SchemaAttributes}
   */
  public SchemaAttributes compositeKeys(
      final List<SchemaAttributesCompositeKey> schemaCompositeKeys) {
    this.compositeKeys = schemaCompositeKeys;
    return this;
  }

  /**
   * Get {@link List} {@link SchemaAttributesRequired}.
   * 
   * @return {@link List} {@link SchemaAttributesRequired}
   */
  public List<SchemaAttributesRequired> getRequired() {
    return this.required;
  }

  /**
   * Set Required Schema Attributes.
   * 
   * @param schemaRequired {@link List} {@link SchemaAttributesRequired}
   * @return {@link SchemaAttributes}
   */
  public SchemaAttributes required(final List<SchemaAttributesRequired> schemaRequired) {
    this.required = schemaRequired;
    return this;
  }

  /**
   * Get {@link List} {@link SchemaAttributesOptional}.
   * 
   * @return {@link List} {@link SchemaAttributesOptional}
   */
  public List<SchemaAttributesOptional> getOptional() {
    return this.optional;
  }

  /**
   * Set {@link List} {@link SchemaAttributesOptional}.
   * 
   * @param schemaOptional {@link List} {@link SchemaAttributesOptional}
   * @return {@link SchemaAttributes}
   */
  public SchemaAttributes optional(final List<SchemaAttributesOptional> schemaOptional) {
    this.optional = schemaOptional;
    return this;
  }

  /**
   * Is Additional Attributes allowed.
   * 
   * @return boolean
   */
  public boolean isAllowAdditionalAttributes() {
    return this.allowAdditionalAttributes;
  }

  /**
   * Set Additional Attributes allowed.
   * 
   * @param schemaAllowAdditionalAttributes boolean
   */
  public void setAllowAdditionalAttributes(final boolean schemaAllowAdditionalAttributes) {
    this.allowAdditionalAttributes = schemaAllowAdditionalAttributes;
  }
}
