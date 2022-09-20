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

import java.util.Collection;

/** {@link Exception} that will return a 400 error. */
public class ValidationException extends Exception {

  /** serialVersionUID. */
  private static final long serialVersionUID = -3307615320614370509L;
  /** {@link ValidationError}. */
  private Collection<ValidationError> errors;

  /**
   * constructor.
   * 
   * @param validationErrors {@link Collection} {@link ValidationError}
   */
  public ValidationException(final Collection<ValidationError> validationErrors) {
    super("validation errors");
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
