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
package com.formkiq.aws.dynamodb.documentattributes;

import java.util.Objects;

/**
 * Document Attribute Entity Key Value.
 * 
 * @param entityTypeId {@link String}
 * @param entityId {@link String}
 */
public record DocumentAttributeEntityKeyValue(String entityTypeId, String entityId) {

  /** Delimiter. */
  private static final String DELIMITER = "#";

  public DocumentAttributeEntityKeyValue {
    Objects.requireNonNull(entityTypeId, "entityTypeId must not be null");
    Objects.requireNonNull(entityId, "entityId must not be null");
  }

  /**
   * Compact representation: "type#id".
   * 
   * @return {@link String}
   */
  public String getStringValue() {
    return entityTypeId + DELIMITER + entityId;
  }

  /**
   * Parse back from stringValue into DocumentAttributeEntityKeyValue.
   * 
   * @param stringValue {@link String}
   * @return {@link DocumentAttributeEntityKeyValue}
   */
  public static DocumentAttributeEntityKeyValue fromString(final String stringValue) {
    String[] parts = stringValue.split(DELIMITER, 2);
    return new DocumentAttributeEntityKeyValue(parts[0], parts[1]);
  }
}
