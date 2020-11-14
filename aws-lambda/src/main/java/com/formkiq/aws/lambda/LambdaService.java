/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
