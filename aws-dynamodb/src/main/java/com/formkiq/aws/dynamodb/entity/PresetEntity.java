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

import java.util.List;

/**
 * Present Entity.
 */
public enum PresetEntity {
  /** LLM Prompt entity. */
  LLM_PROMPT("LlmPrompt", List.of("userPrompt")),
  /** Checkout Lock Entity. */
  CHECKOUT("Checkout", List.of("LockedBy", "LockedDate")),
  /** Lock Entity. */
  CHECKOUT_FOR_LEGAL_HOLD("CheckoutForLegalHold", List.of("LockedBy", "LockedDate"));

  /** Entity Name. */
  private final String name;
  /** Attribute Keys. */
  private final List<String> attributeKeys;

  PresetEntity(final String entityName, final List<String> entityAttributeKeys) {
    this.name = entityName;
    this.attributeKeys = entityAttributeKeys;
  }

  /**
   * Get the string value for this preset entity.
   *
   * @return string representation
   */
  public String getName() {
    return name;
  }

  /**
   * Get Attribute Keys.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getAttributeKeys() {
    return attributeKeys;
  }

  /**
   * Convert a string to the matching enum constant. Defaults to {@link #LLM_PROMPT} if no match is
   * found.
   *
   * @param value string to convert (case-insensitive)
   * @return matching enum constant, or {@code LLM_PROMPT} if none match
   */
  public static PresetEntity fromString(final String value) {
    if (value != null) {
      for (PresetEntity e : values()) {
        if (e.name.equalsIgnoreCase(value) || e.name().equalsIgnoreCase(value)) {
          return e;
        }
      }
    }
    return null;
  }
}
