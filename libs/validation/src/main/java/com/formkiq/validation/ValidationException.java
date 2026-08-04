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
import java.util.stream.Collectors;

/** {@link Exception} that will return a 400 error. */
public class ValidationException extends RuntimeException {

  /** Builder for {@link ValidationException}. */
  public static class Builder {

    /** {@link ValidationError}. */
    private final List<ValidationError> errors = new ArrayList<>();

    /**
     * Build {@link ValidationException}.
     *
     * @return {@link ValidationException}
     */
    public ValidationException build() {
      return new ValidationException(this.errors);
    }

    /**
     * Add {@link ValidationError}.
     *
     * @param error {@link String}
     * @return {@link Builder}
     */
    public Builder error(final String error) {
      this.errors.add(new ValidationErrorImpl().error(error));
      return this;
    }

    /**
     * Add {@link ValidationError}.
     *
     * @param key {@link String}
     * @param error {@link String}
     * @return {@link Builder}
     */
    public Builder error(final String key, final String error) {
      this.errors.add(new ValidationErrorImpl().key(key).error(error));
      return this;
    }

    /**
     * Add {@link ValidationError}.
     *
     * @param validationErrors {@link Collection} {@link ValidationError}
     * @return {@link Builder}
     */
    public Builder errors(final Collection<ValidationError> validationErrors) {
      this.errors.addAll(validationErrors);
      return this;
    }
  }

  /** serialVersionUID. */
  private static final long serialVersionUID = -3307615320614370509L;

  /**
   * Create {@link Builder}.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /** {@link ValidationError}. */
  private final Collection<ValidationError> errors;

  /**
   * constructor.
   * 
   * @param validationErrors {@link Collection} {@link ValidationError}
   */
  public ValidationException(final Collection<ValidationError> validationErrors) {
    super(validationErrors.stream().map(ValidationError::error).collect(Collectors.joining(",")));
    this.errors = validationErrors;
  }

  /**
   * Get {@link ValidationError}.
   * 
   * @return {@link Collection} {@link ValidationError}
   */
  public Collection<ValidationError> errors() {
    return this.errors;
  }
}
