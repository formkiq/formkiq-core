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
package com.formkiq.aws.dynamodb.llm;

import java.util.Locale;
import java.util.Objects;

/**
 * Result types for data extracted from an LLM prompt response.
 */
public enum DocumentAiPromptResultType {
  /** A list of simple key/value attributes. */
  KEY_VALUE,
  /** Attributes grouped under an entity type. */
  ENTITY;

  /**
   * Convert a string to a {@link DocumentAiPromptResultType}.
   *
   * @param value {@link String}
   * @return {@link DocumentAiPromptResultType}
   */
  public static DocumentAiPromptResultType fromString(final String value) {
    Objects.requireNonNull(value, "value must not be null");
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
