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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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
   * Add Error.
   *
   * @param key String
   * @param message String
   * @return ValidationBuilder
   */
  public ValidationBuilder addError(final String key, final String message) {
    this.errors.add(new ValidationErrorImpl().key(key).error(message));
    return this;
  }

  /**
   * Add multiple list.
   * 
   * @param list {@link Collection} {@link ValidationError}
   */
  public void addErrors(final Collection<ValidationError> list) {
    this.errors.addAll(list);
  }

  public void authorized(final boolean authorized) {
    if (!authorized) {
      errors.add(new UnAuthorizedValidationError());
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
   * Get {@link Collection} {@link ValidationError}.
   * 
   * @return {@link Collection} {@link ValidationError}
   */
  public Collection<ValidationError> getErrors() {
    return errors;
  }

  /**
   * Whether there are {@link ValidationError}.
   * 
   * @return boolean
   */
  public boolean isEmpty() {
    return errors.isEmpty();
  }

  /**
   * Is {@link String} equals.
   *
   * @param key {@link String}
   * @param value String
   * @param expectedValues {@link List} {@link String}
   */
  public void isEquals(final String key, final String value, final List<String> expectedValues) {
    if (!expectedValues.contains(value)) {
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
    if (!value.equals(expectedValue)) {
      String error = "'" + key + "' unexpected value '" + value + "'";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param values {@link String}
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final Collection<?> values, final String errorMessage) {
    if (values == null || values.isEmpty()) {
      errors.add(new ValidationErrorImpl().key(key).error(errorMessage));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value {@link Double}
   */
  public void isRequired(final String key, final Double value) {
    isRequired(key, value, null);
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value {@link Double}
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final Double value, final String errorMessage) {
    if (value == null) {
      String error = errorMessage != null ? errorMessage : "'" + key + "' is required";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param values {@link String}
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final Map<?, ?> values, final String errorMessage) {
    if (values == null || values.isEmpty()) {
      errors.add(new ValidationErrorImpl().key(key).error(errorMessage));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value {@link String}
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final Object value, final String errorMessage) {
    if (value == null) {
      errors.add(new ValidationErrorImpl().key(key).error(errorMessage));
    }
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link Optional}
   * @param value boolean
   */
  public void isRequired(final String key, final Optional<?> value) {
    isRequired(key, value.orElse(null), "'" + key + "' is required");
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value {@link String}
   */
  public void isRequired(final String key, final String value) {
    isRequired(key, value, null);
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value {@link String}
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final String value, final String errorMessage) {
    if (value == null || value.isEmpty()) {
      String error = errorMessage != null ? errorMessage : "'" + key + "' is required";
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
    isRequired(key, value, "'" + key + "' is invalid");
  }

  /**
   * Validate Field is required.
   *
   * @param key {@link String}
   * @param value boolean
   * @param errorMessage {@link String}
   */
  public void isRequired(final String key, final boolean value, final String errorMessage) {
    if (!value) {
      errors.add(new ValidationErrorImpl().key(key).error(errorMessage));
    }
  }

  /**
   * Is Only of the values not null or not empty if {@link Collection}.
   *
   * @param key {@link String}
   * @param values {@link String}
   * @param errorMessage {@link String}
   */
  public void isRequiredOnlyOne(final String key, final List<Object> values,
      final String errorMessage) {
    long objectCount =
        values.stream().filter(Objects::nonNull).filter(c -> !(c instanceof Collection)).count();

    long listCount = values.stream().filter(Objects::nonNull).filter(c -> c instanceof Collection)
        .filter(c -> !((Collection<?>) c).isEmpty()).count();

    if (objectCount + listCount != 1) {
      errors.add(new ValidationErrorImpl().key(key).error(errorMessage));
    }
  }

  /**
   * Is value valid by regex.
   * 
   * @param key {@link String}
   * @param value {@link String}
   * @param regex {@link String}
   */
  public void isValidByRegex(final String key, final String value, final String regex) {
    Pattern pattern = Pattern.compile(regex);
    if (!pattern.matcher(value).matches()) {
      String error = "'" + key + "' unexpected value '" + value + "'";
      errors.add(new ValidationErrorImpl().key(key).error(error));
    }
  }
}
