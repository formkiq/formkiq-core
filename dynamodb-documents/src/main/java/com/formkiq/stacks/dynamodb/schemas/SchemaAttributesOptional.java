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
 * Schema Attributes Optional.
 */
@Reflectable
public class SchemaAttributesOptional {

  /** Allowed Values. */
  private List<String> allowedValues;
  /** Attribute Key. */
  private String attributeKey;
  /** Default Value. */
  private String defaultValue;
  /** Default Values. */
  private List<String> defaultValues;

  /**
   * constructor.
   */
  public SchemaAttributesOptional() {

  }

  /**
   * Set Allowed Values.
   * 
   * @param requiredAllowedValues {@link List} {@link String}
   * @return {@link SchemaAttributesOptional}
   */
  public SchemaAttributesOptional allowedValues(final List<String> requiredAllowedValues) {
    this.allowedValues = requiredAllowedValues;
    return this;
  }

  /**
   * Set Attribute Key.
   * 
   * @param requiredAttributeKey {@link String}
   * @return {@link SchemaAttributesOptional}
   */
  public SchemaAttributesOptional attributeKey(final String requiredAttributeKey) {
    this.attributeKey = requiredAttributeKey;
    return this;
  }

  /**
   * Set Default Value.
   * 
   * @param requiredDefaultValue {@link String}
   * @return {@link SchemaAttributesOptional}
   */
  public SchemaAttributesOptional defaultValue(final String requiredDefaultValue) {
    this.defaultValue = requiredDefaultValue;
    return this;
  }

  /**
   * Set Default Values.
   * 
   * @param requiredDefaultValues {@link List} {@link String}
   * @return {@link SchemaAttributesOptional}
   */
  public SchemaAttributesOptional defaultValues(final List<String> requiredDefaultValues) {
    this.defaultValues = requiredDefaultValues;
    return this;
  }

  /**
   * Get Allowed Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getAllowedValues() {
    return this.allowedValues;
  }

  /**
   * Get Attribute Key.
   * 
   * @return {@link String}
   */
  public String getAttributeKey() {
    return this.attributeKey;
  }

  /**
   * Get Default Value.
   * 
   * @return {@link String}
   */
  public String getDefaultValue() {
    return this.defaultValue;
  }

  /**
   * Get Default Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getDefaultValues() {
    return this.defaultValues;
  }
}
