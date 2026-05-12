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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_ANALYSIS_CATEGORY;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_RESPONSE_FIELD_KEY;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_RESPONSE_PRESET_ENTITY_TYPES;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_SYSTEM_PROMPT;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.LLM_USER_PROMPT;

/**
 * PresetEntity for LLM Prompt.
 */
public class LlmPromptPresetEntity implements PresetEntity {

  /** Entity Name. */
  public static final String ENTITY_NAME = "LlmPrompt";

  @Override
  public List<String> getAttributeKeys() {
    return List.of(LLM_USER_PROMPT.getKey(), LLM_SYSTEM_PROMPT.getKey(),
        LLM_RESPONSE_PRESET_ENTITY_TYPES.getKey(), LLM_RESPONSE_FIELD_KEY.getKey(),
        LLM_ANALYSIS_CATEGORY.getKey());
  }

  @Override
  public String getName() {
    return ENTITY_NAME;
  }

  @Override
  public List<String> getRequiredAttributeKeys() {
    return List.of(LLM_SYSTEM_PROMPT.getKey());
  }

  @Override
  public void validateAttributes(final List<EntityAttribute> attributes,
      final Map<String, AttributeValue> existing) {

    ValidationBuilder vb = new ValidationBuilder();
    Optional<EntityAttribute> systemPrompt = findAttribute(attributes, LLM_SYSTEM_PROMPT.getKey());
    Optional<EntityAttribute> userPrompt = findAttribute(attributes, LLM_USER_PROMPT.getKey());

    if (userPrompt.isEmpty()) {
      vb.isRequired(LLM_SYSTEM_PROMPT.getKey(), systemPrompt);
    }

    vb.check();
  }
}
