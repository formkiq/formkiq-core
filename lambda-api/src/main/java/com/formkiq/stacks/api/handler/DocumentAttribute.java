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
package com.formkiq.stacks.api.handler;

import java.util.List;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Document Attributes Request.
 */
@Reflectable
public class DocumentAttribute {

  /** Boolean value. */
  private Boolean booleanValue;
  /** Key of Attribute. */
  private String key;
  /** Number value. */
  private Double numberValue;
  /** Number value. */
  private List<Double> numberValues;
  /** String valueAttribute. */
  private String stringValue;
  /** String valueAttribute. */
  private List<String> stringValues;
  /** {@link String} Classification Id. */
  private String classificationId;

  /**
   * constructor.
   */
  public DocumentAttribute() {

  }

  /**
   * Get Classification Id.
   * 
   * @return String
   */
  public String getClassificationId() {
    return this.classificationId;
  }

  /**
   * Set Classification Id.
   * 
   * @param attributeClassificationId {@link String}
   * @return DocumentAttribute
   */
  public DocumentAttribute setClassificationId(final String attributeClassificationId) {
    this.classificationId = attributeClassificationId;
    return this;
  }

  /**
   * Set Boolean Value.
   * 
   * @param attributeValue {@link Boolean}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute booleanValue(final Boolean attributeValue) {
    this.booleanValue = attributeValue;
    return this;
  }

  /**
   * Get Boolean Value.
   * 
   * @return {@link Boolean}
   */
  public Boolean getBooleanValue() {
    return this.booleanValue;
  }

  /**
   * Get Key.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Number Value.
   * 
   * @return {@link Double}
   */
  public Double getNumberValue() {
    return this.numberValue;
  }

  /**
   * Get Number Values.
   * 
   * @return {@link List} {@link Double}
   */
  public List<Double> getNumberValues() {
    return this.numberValues;
  }

  /**
   * Get String Value.
   * 
   * @return {@link String}
   */
  public String getStringValue() {
    return this.stringValue;
  }

  /**
   * Get String Values.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getStringValues() {
    return this.stringValues;
  }

  /**
   * Set Key.
   * 
   * @param attributeKey {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute key(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Set Number Value.
   * 
   * @param attributeValue {@link Double}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute numberValue(final Double attributeValue) {
    this.numberValue = attributeValue;
    return this;
  }

  /**
   * Set Number Values.
   * 
   * @param attributeValues {@link List} {@link Double}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute numberValues(final List<Double> attributeValues) {
    this.numberValues = attributeValues;
    return this;
  }

  /**
   * Set String Value.
   * 
   * @param attributeValue {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute stringValue(final String attributeValue) {
    this.stringValue = attributeValue;
    return this;
  }

  /**
   * Set String Values.
   * 
   * @param attributeValues {@link List} {@link String}
   * @return {@link DocumentAttribute}
   */
  public DocumentAttribute stringValues(final List<String> attributeValues) {
    this.stringValues = attributeValues;
    return this;
  }
}
