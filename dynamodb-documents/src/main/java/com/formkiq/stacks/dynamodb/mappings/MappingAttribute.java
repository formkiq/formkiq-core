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
package com.formkiq.stacks.dynamodb.mappings;

import com.formkiq.graalvm.annotations.Reflectable;

import java.util.List;

/**
 * Mapping Attribute.
 */
@Reflectable
public class MappingAttribute {
  /** Attribute Key. */
  private String attributeKey;
  /** {@link MappingAttributeSourceType}. */
  private MappingAttributeSourceType sourceType;
  /** {@link MappingAttributeMetadataField}. */
  private MappingAttributeMetadataField metadataField;
  /** Validation Regex. */
  private String validationRegex;
  /** Label Texts. */
  private List<String> labelTexts;
  /** llmPromptEntityName. */
  private String llmPromptEntityName;
  /** {@link MappingAttributeLabelMatchingType}. */
  private MappingAttributeLabelMatchingType labelMatchingType;

  /** Default Value. */
  private String defaultValue;
  /** Default Values. */
  private List<String> defaultValues;

  /**
   * constructor.
   */
  public MappingAttribute() {}

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
   * @return String
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

  /**
   * Get {@link MappingAttributeLabelMatchingType}.
   * 
   * @return {@link MappingAttributeLabelMatchingType}
   */
  public MappingAttributeLabelMatchingType getLabelMatchingType() {
    return this.labelMatchingType;
  }

  /**
   * Get Label Text.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getLabelTexts() {
    return this.labelTexts;
  }

  /**
   * Get llmPromptEntityName.
   * 
   * @return {@link String}
   */
  public String getLlmPromptEntityName() {
    return llmPromptEntityName;
  }

  /**
   * Get {@link MappingAttributeMetadataField}.
   * 
   * @return {@link MappingAttributeMetadataField}
   */
  public MappingAttributeMetadataField getMetadataField() {
    return this.metadataField;
  }

  /**
   * Get Source Type.
   * 
   * @return {@link MappingAttributeSourceType}
   */
  public MappingAttributeSourceType getSourceType() {
    return this.sourceType;
  }

  /**
   * Get Validation Regex.
   * 
   * @return {@link String}
   */
  public String getValidationRegex() {
    return this.validationRegex;
  }

  /**
   * Set Attribute Key.
   * 
   * @param attribute {@link String}
   * @return {@link MappingAttribute}
   */
  public MappingAttribute setAttributeKey(final String attribute) {
    this.attributeKey = attribute;
    return this;
  }

  /**
   * Set Default Value.
   * 
   * @param attributeDefaultValue {@link String}
   * @return MappingAttribute
   */
  public MappingAttribute setDefaultValue(final String attributeDefaultValue) {
    this.defaultValue = attributeDefaultValue;
    return this;
  }

  /**
   * Set Default Values.
   * 
   * @param attributeDefaultValues {@link List} {@link String}
   * @return MappingAttribute
   */
  public MappingAttribute setDefaultValues(final List<String> attributeDefaultValues) {
    this.defaultValues = attributeDefaultValues;
    return this;
  }

  /**
   * Set {@link MappingAttributeLabelMatchingType}.
   * 
   * @param attributelabelMatchingType {@link MappingAttributeLabelMatchingType}
   * @return MappingAttribute
   */
  public MappingAttribute setLabelMatchingType(
      final MappingAttributeLabelMatchingType attributelabelMatchingType) {
    this.labelMatchingType = attributelabelMatchingType;
    return this;
  }

  /**
   * Set Label Texts.
   * 
   * @param attributeLabelTexts {@link List} {@link String}
   * @return MappingAttribute
   */
  public MappingAttribute setLabelTexts(final List<String> attributeLabelTexts) {
    this.labelTexts = attributeLabelTexts;
    return this;
  }

  /**
   * Set LLM Prompt Entity Name.
   *
   * @param promptEntityName {@link String}
   * @return {@link MappingAttribute}
   */
  public MappingAttribute setLlmPromptEntityName(final String promptEntityName) {
    this.llmPromptEntityName = promptEntityName;
    return this;
  }

  /**
   * Set Metadata Field.
   * 
   * @param attributeMetadataField {@link MappingAttributeMetadataField}
   * @return MappingAttribute
   */
  public MappingAttribute setMetadataField(
      final MappingAttributeMetadataField attributeMetadataField) {
    this.metadataField = attributeMetadataField;
    return this;
  }

  /**
   * Set Source Type.
   * 
   * @param attributeSourceType {@link MappingAttributeSourceType}
   * @return {@link MappingAttribute}
   */
  public MappingAttribute setSourceType(final MappingAttributeSourceType attributeSourceType) {
    this.sourceType = attributeSourceType;
    return this;
  }

  /**
   * Set Validation Regex.
   * 
   * @param attributeValidationRegex {@link String}
   * @return {@link MappingAttribute}
   */
  public MappingAttribute setValidationRegex(final String attributeValidationRegex) {
    this.validationRegex = attributeValidationRegex;
    return this;
  }
}
