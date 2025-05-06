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
package com.formkiq.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Validation Builder.
 */
public class ValidationBuilder {

  /** {@link Collection} {@link ValidationError}. */
  private final Collection<ValidationError> errors = new ArrayList<>();

  /**
   * constructor.
   */
  public ValidationBuilder() {}

  /**
   * Validate Field is required.
   * 
   * @param key {@link String}
   * @param value {@link String}
   */
  public void isRequired(final String key, final String value) {
    if (value == null || value.isEmpty()) {
      String error = "'" + key + "' is required";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value boolean
   */
  public void isRequired(final String key, final boolean value) {
    if (!value) {
      String error = "'" + key + "' is invalid";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }

  /**
   * Check Validation.
   */
  public void check() throws ValidationException {
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  /**
   * Is {@link String} equals.
   *
   * @param key {@link String}
   * @param value String
   * @param expectedValues {@link List} {@link String}
   */
  public void isEquals(final String key, final String value, final List<String> expectedValues) {
    if (errors.isEmpty() && !expectedValues.contains(value)) {
      String error = "'" + key + "' unexpected value '" + value + "' must be one of "
          + String.join(", ", expectedValues);
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }

  /**
   * Is {@link String} equals.
   * 
   * @param key {@link String}
   * @param value String
   * @param expectedValue {@link String}
   */
  public void isEquals(final String key, final String value, final String expectedValue) {
    if (errors.isEmpty() && !value.equals(expectedValue)) {
      String error = "'" + key + "' unexpected value '" + value + "'";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }
}
