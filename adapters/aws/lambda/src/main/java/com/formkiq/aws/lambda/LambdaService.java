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
package com.formkiq.aws.lambda;

import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * 
 * Lambda Services.
 *
 */
public class LambdaService {

  /** {@link LambdaConnectionBuilder}. */
  private LambdaConnectionBuilder builder;

  /**
   * Constructor.
   * 
   * @param s3connectionBuilder {@link LambdaConnectionBuilder}
   */
  public LambdaService(final LambdaConnectionBuilder s3connectionBuilder) {
    this.builder = s3connectionBuilder;
  }

  /**
   * Build {@link LambdaClient}.
   * 
   * @return {@link LambdaClient}
   */
  public LambdaClient buildClient() {
    return this.builder.build();
  }

  /**
   * Invoke Lambda.
   * 
   * @param client {@link LambdaClient}
   * @param functionName {@link String}
   * @return {@link InvokeResponse}
   */
  public InvokeResponse invoke(final LambdaClient client, final String functionName) {
    try (LambdaClient lambdaClient = this.buildClient()) {
      return lambdaClient.invoke(InvokeRequest.builder().functionName(functionName).build());
    }
  }
}
