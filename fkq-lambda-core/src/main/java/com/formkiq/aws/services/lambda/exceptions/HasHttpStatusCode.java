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
package com.formkiq.aws.services.lambda.exceptions;

/**
 * Marker interface for objects that expose an HTTP status code.
 *
 * <p>
 * Implementations should return a valid HTTP status code as defined by
 * <a href="https://www.rfc-editor.org/rfc/rfc9110">RFC 9110</a>.
 *
 * <p>
 * This interface represents a capability rather than a type hierarchy and is intentionally designed
 * to avoid forcing inheritance from a common base class.
 */
public interface HasHttpStatusCode {
  /**
   * Returns the HTTP status code associated with this object.
   *
   * <p>
   * The returned value should be a valid three-digit HTTP status code.
   *
   * @return the HTTP status code
   */
  int getStatusCode();
}
