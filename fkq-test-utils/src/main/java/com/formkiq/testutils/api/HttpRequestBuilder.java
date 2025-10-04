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
package com.formkiq.testutils.api;

import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Generic Http Request Builder.
 * 
 * @param <T> Type of response object
 */
public interface HttpRequestBuilder<T> {

  /** Max number of reties. */
  int MAX_RETRIES = 5;

  /**
   * Run the API request.
   * 
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return ApiHttpResponse
   */
  ApiHttpResponse<T> submit(ApiClient apiClient, String siteId);

  /**
   * Polls {@link #submit(ApiClient, String)} until {@code isDone} is true, or attempts are
   * exhausted.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @param isDone {@link Predicate}
   * @return the last response (which may or may not satisfy the condition).
   */
  default ApiHttpResponse<T> submitUntil(ApiClient apiClient, String siteId,
      Predicate<ApiHttpResponse<T>> isDone) throws InterruptedException {
    ApiHttpResponse<T> last;

    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      last = submit(apiClient, siteId);
      if (isDone.test(last)) {
        return last;
      }
      TimeUnit.SECONDS.sleep(1);
    }

    throw new RuntimeException("Reached maximum number of retries");
  }

  default ApiHttpResponse<T> executeApiCall(ApiCallable<T> callable) {
    T response = null;
    ApiException ex = null;
    try {
      response = callable.call();
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(response, ex);
  }

  @FunctionalInterface
  interface ApiCallable<R> {
    R call() throws ApiException;
  }
}
