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
import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Schema Attributes Required.
 */
@Reflectable
public class SchemaAttributesRequired {

  /** Allowed Values. */
  private List<String> allowedValues;
  /** Attribute Key. */
  private String attributeKey;
  /** Default Value. */
  private String defaultValue;
  /** Default Values. */
  private List<String> defaultValues;
  /** Localized Allowed Values. */
  private Map<String, String> localizedAllowedValues;
  /** Min Number of values. */
  private Double minNumberOfValues;
  /** Max Number of values. */
  private Double maxNumberOfValues;

  /**
   * constructor.
   */
  public SchemaAttributesRequired() {

  }

  /**
   * Get Max Number of values.
   * 
   * @return Double
   */
  public Double maxNumberOfValues() {
    return this.maxNumberOfValues;
  }

  /**
   * Set Max Number of Values.
   *
   * @param numberOfValues Double
   * @return SchemaAttributesRequired
   */
  public SchemaAttributesRequired maxNumberOfValues(final Double numberOfValues) {
    this.maxNumberOfValues = numberOfValues;
    return this;
  }

  /**
   * Get Min Number of values.
   * 
   * @return Double
   */
  public Double minNumberOfValues() {
    return this.minNumberOfValues;
  }

  /**
   * Set Min Number of values.
   * 
   * @param numberOfValues Double
   * @return SchemaAttributesRequired
   */
  public SchemaAttributesRequired minNumberOfValues(final Double numberOfValues) {
    this.minNumberOfValues = numberOfValues;
    return this;
  }

  /**
   * Get Localized Allowed Values.
   * 
   * @return Map
   */
  public Map<String, String> localizedAllowedValues() {
    return this.localizedAllowedValues;
  }

  /**
   * Set Localized Allowed Values.
   * 
   * @param values {@link Map}
   * @return SchemaAttributesRequired
   */
  public SchemaAttributesRequired localizedAllowedValues(final Map<String, String> values) {
    this.localizedAllowedValues = values;
    return this;
  }

  /**
   * Set Allowed Values.
   * 
   * @param requiredAllowedValues {@link List} {@link String}
   * @return {@link SchemaAttributesRequired}
   */
  public SchemaAttributesRequired allowedValues(final List<String> requiredAllowedValues) {
    this.allowedValues = requiredAllowedValues;
    return this;
  }

  /**
   * Set Attribute Key.
   * 
   * @param requiredAttributeKey {@link String}
   * @return {@link SchemaAttributesRequired}
   */
  public SchemaAttributesRequired attributeKey(final String requiredAttributeKey) {
    this.attributeKey = requiredAttributeKey;
    return this;
  }

  /**
   * Set Default Value.
   * 
   * @param requiredDefaultValue {@link String}
   * @return {@link SchemaAttributesRequired}
   */
  public SchemaAttributesRequired defaultValue(final String requiredDefaultValue) {
    this.defaultValue = requiredDefaultValue;
    return this;
  }

  /**
   * Set Default Values.
   * 
   * @param requiredDefaultValues {@link List} {@link String}
   * @return {@link SchemaAttributesRequired}
   */
  public SchemaAttributesRequired defaultValues(final List<String> requiredDefaultValues) {
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
