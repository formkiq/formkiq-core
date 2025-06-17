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

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/**
 * Exception thrown when a DynamoDB Query operation fails.
 */
public class DynamoDbQueryException extends RuntimeException {

  /** {@link DynamoDbError}. */
  private DynamoDbError error;

  /**
   * Constructs a new exception wrapping the underlying DynamoDbException.
   *
   * @param cause original DynamoDbException
   */
  public DynamoDbQueryException(final DynamoDbException cause) {
    super(cause);
    AwsErrorDetails awsErrorDetails = cause.awsErrorDetails();
    this.error = DynamoDbError.OTHER;

    if (awsErrorDetails != null) {
      if (awsErrorDetails.errorMessage().contains("starting key")) {
        this.error = DynamoDbError.INVALID_START_KEY;
      }
    }
  }

  /**
   * Get {@link DynamoDbError}.
   *
   * @return {@link DynamoDbError}
   */
  public DynamoDbError getError() {
    return error;
  }
}
