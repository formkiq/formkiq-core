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

/**
 * Mapping Classification Condition.
 *
 * @param sourceType {@link MappingClassificationConditionSourceType}
 * @param resultKey Result Key
 * @param resultValue Result Value
 * @param llmPromptEntityName LLM Prompt Entity Name
 * @param matchingType {@link MappingClassificationConditionMatchingType}
 * @param text Text
 */
@Reflectable
public record MappingClassificationCondition(MappingClassificationConditionSourceType sourceType,
    String resultKey, String resultValue, String llmPromptEntityName,
    MappingClassificationConditionMatchingType matchingType, String text) {
}
