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
package com.formkiq.aws.services.lambda;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Return an {@link Object} in response.
 */
@Reflectable
public class ApiObjectResponse implements ApiResponse {
  /** {@link Object}. */
  private final Object object;
  /** Next. */
  private final String next;
  /** Previous. */
  private final String previous;

  /**
   * constructor.
   *
   * @param o {@link Object}
   * @param nextToken {@link String}
   * @param previousToken {@link String}
   */
  public ApiObjectResponse(final Object o, final String nextToken, final String previousToken) {
    this.object = o;
    this.next = nextToken;
    this.previous = previousToken;
  }

  @Override
  public String getNext() {
    return this.next;
  }

  @Override
  public String getPrevious() {
    return this.previous;
  }

  /**
   * Get {@link Object}.
   *
   * @return {@link Object}
   */
  public Object getObject() {
    return this.object;
  }
}
