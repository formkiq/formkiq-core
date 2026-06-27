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

import com.formkiq.aws.dynamodb.ApiAuthorization;

/**
 * 
 * Interceptor to allow injecting extra actions before and after {@link ApiAuthorization} is built.
 *
 */
public interface ApiAuthorizationInterceptor {

  /**
   * Hook invoked after {@link ApiAuthorization} is built and before it is validated.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @throws Exception Exception
   */
  default void afterBuildAuthorization(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws Exception {
    // default empty implementation
  }

  /**
   * Hook invoked before {@link ApiAuthorization} is built.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @throws Exception Exception
   */
  default void beforeBuildAuthorization(final ApiGatewayRequestEvent event) throws Exception {
    // default empty implementation
  }
}
