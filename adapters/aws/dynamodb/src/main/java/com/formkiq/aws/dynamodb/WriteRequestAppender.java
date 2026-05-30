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
package com.formkiq.aws.dynamodb;

/**
 * Represents a unit of work that appends one or more write operations to a
 * {@link WriteRequestBuilder}.
 *
 * <p>
 * Implementations encapsulate the logic required to contribute DynamoDB write requests such as
 * updates, puts, or deletes to the {@link WriteRequestBuilder}. This abstraction allows write logic
 * to be structured into reusable components rather than embedding DynamoDB write construction
 * directly inside service methods.
 *
 * <p>
 * This pattern is useful when multiple parts of the application need to contribute write operations
 * to the same batch or transaction builder while keeping domain logic isolated and testable.
 *
 */
public interface WriteRequestAppender {

  /**
   * Append write operations to the provided {@link WriteRequestBuilder}.
   *
   * <p>
   * The implementation may add one or more DynamoDB write requests (such as updates, puts, or
   * deletes) to the builder.
   *
   * @param wrb {@link WriteRequestBuilder}
   */
  void appendTo(WriteRequestBuilder wrb);
}
