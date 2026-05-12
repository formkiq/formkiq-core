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

import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for {@link LlmPromptPresetEntity}. */
class LlmPromptPresetEntityTest {

  /**
   * Get LLM Prompt attribute keys.
   */
  @Test
  void testGetAttributeKeys01() {
    // given
    LlmPromptPresetEntity entity = new LlmPromptPresetEntity();

    // when
    List<String> keys = entity.getAttributeKeys();

    // then
    assertEquals(List.of("UserPrompt", "LlmSystemPrompt", "LlmResponsePresetEntityTypes",
        "LlmResponseCustomEntityTypes", "LlmResponseFieldKey", "LlmAnalysisCategory"), keys);
  }

  /**
   * LLM Prompt attribute keys are reserved string attributes.
   */
  @Test
  void testReservedAttributeKeys01() {
    // given
    List<String> keys = new LlmPromptPresetEntity().getAttributeKeys().stream().skip(1).toList();

    // when / then
    for (String key : keys) {
      AttributeKeyReserved reserved = AttributeKeyReserved.find(key.toLowerCase());
      assertNotNull(reserved);
      assertEquals(key, reserved.getKey());
      assertEquals(AttributeDataType.STRING, reserved.getDataType());
    }
  }

  /**
   * UserPrompt is deprecated.
   *
   * @throws Exception an error has occurred
   */
  @Test
  void testUserPromptDeprecated01() throws Exception {
    // expect
    assertTrue(
        LlmPromptPresetEntity.class.getField("USER_PROMPT").isAnnotationPresent(Deprecated.class));
  }
}
