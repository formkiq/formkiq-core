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

import java.util.Arrays;

/**
 * Defines the source used to determine the start date for retention calculations.
 */
public enum RetentionStartDateSourceType {

  /**
   * Uses the date when the entity was last modified as the retention start date.
   */
  DATE_LAST_MODIFIED,

  /**
   * Uses the date when the entity was initially inserted/created as the retention start date.
   */
  DATE_INSERTED;

  /**
   * Convert a string value to a {@link RetentionStartDateSourceType}.
   * <p>
   * The comparison is case-insensitive and ignores leading/trailing whitespace.
   *
   * @param value the string value to convert
   * @return the matching {@link RetentionStartDateSourceType}
   * @throws IllegalArgumentException if the value does not match any enum constant
   */
  public static RetentionStartDateSourceType fromString(final String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "RetentionStartDateSourceType value must not be null or blank");
    }

    try {
      return RetentionStartDateSourceType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid 'RetentionStartDateSourceType' must be "
          + String.join(",", Arrays.stream(values()).sorted().map(Enum::name).toList()));
    }
  }
}
