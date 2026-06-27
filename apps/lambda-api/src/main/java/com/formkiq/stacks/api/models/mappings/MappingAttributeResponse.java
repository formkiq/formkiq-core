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
package com.formkiq.stacks.api.models.mappings;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.mappings.MappingAttribute;

/**
 * Mapping Attribute API response.
 */
@Reflectable
public interface MappingAttributeResponse {

  /**
   * Create a response from a {@link MappingAttribute}.
   *
   * @param attribute {@link MappingAttribute}
   * @return {@link MappingAttributeResponse}
   */
  static MappingAttributeResponse from(final MappingAttribute attribute) {

    return switch (attribute.getSourceType()) {
      case CONTENT, CONTENT_KEY_VALUE ->
        new MappingAttributeContent(attribute.getAttributeKey(), attribute.getSourceType(),
            attribute.getDefaultValue(), attribute.getDefaultValues(), attribute.getLabelTexts(),
            attribute.getLabelMatchingType(), emptyToNull(attribute.getValidationRegex()));
      case METADATA ->
        new MappingAttributeMetadata(attribute.getAttributeKey(), attribute.getSourceType(),
            attribute.getDefaultValue(), attribute.getDefaultValues(), attribute.getLabelTexts(),
            attribute.getLabelMatchingType(), attribute.getMetadataField());
      case MANUAL -> new MappingAttributeManual(attribute.getAttributeKey(),
          attribute.getSourceType(), attribute.getDefaultValue(), attribute.getDefaultValues());
      case DATA_CLASSIFICATION -> new MappingAttributeDataClassification(
          attribute.getAttributeKey(), attribute.getSourceType());
      case METADATA_EXTRACTION_RESULT ->
        new MappingAttributeMetadataExtractionResult(attribute.getAttributeKey(),
            attribute.getSourceType(), attribute.getLlmPromptEntityName());
      case AI_PROMPT_RESULT -> new MappingAttributeAiPromptResult(attribute.getAttributeKey(),
          attribute.getSourceType(), attribute.getLlmPromptEntityName());
      case MALWARE_SCAN ->
        new MappingAttributeMalwareScan(attribute.getAttributeKey(), attribute.getSourceType());
    };
  }

  private static String emptyToNull(final String value) {
    return isEmpty(value) ? null : value;
  }
}
