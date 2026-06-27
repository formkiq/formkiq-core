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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Unit tests for {@link ApiRequestHandlerResponse}. */
class ApiRequestHandlerResponseTest {

  private Exception dynamoDbException(final String errorCode) {
    return DynamoDbException.builder().message(errorCode)
        .awsErrorDetails(
            AwsErrorDetails.builder().errorCode(errorCode).serviceName("DynamoDb").build())
        .statusCode(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode()).build();
  }

  @Test
  void testException01() {
    // given
    Exception exception = dynamoDbException("ProvisionedThroughputExceededException");

    // when
    ApiRequestHandlerResponse response =
        ApiRequestHandlerResponse.builder().exception(null, exception).build();

    // then
    assertEquals(ApiResponseStatus.SC_TOO_MANY_REQUESTS.getStatusCode(), response.statusCode());
  }

  @Test
  void testException02() {
    // given
    ConditionalCheckFailedException exception =
        ConditionalCheckFailedException.builder().message("conditional check failed")
            .awsErrorDetails(AwsErrorDetails.builder().errorCode("ConditionalCheckFailedException")
                .serviceName("DynamoDb").build())
            .statusCode(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode()).build();

    // when
    ApiRequestHandlerResponse response =
        ApiRequestHandlerResponse.builder().exception(null, exception).build();

    // then
    assertEquals(ApiResponseStatus.SC_BAD_REQUEST.getStatusCode(), response.statusCode());
  }
}
