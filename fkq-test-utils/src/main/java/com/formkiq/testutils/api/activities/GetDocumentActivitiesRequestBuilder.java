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
package com.formkiq.testutils.api.activities;

import com.formkiq.client.api.UserActivitiesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetUserActivitesResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document Request.
 */
public class GetDocumentActivitiesRequestBuilder
    implements HttpRequestBuilder<GetUserActivitesResponse> {

  /** {@link String}. */
  private String documentId;
  /** Next. */
  private String next;
  /** Limit. */
  private String limit;

  /**
   * constructor.
   * 
   * @param activitiesDocumentId {@link String}
   */
  public GetDocumentActivitiesRequestBuilder(final String activitiesDocumentId) {
    this.documentId = activitiesDocumentId;
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param activitiesLimit {@link String}
   * @return this builder
   */
  public GetDocumentActivitiesRequestBuilder limit(final String activitiesLimit) {
    this.limit = activitiesLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the activitiesNext set of results.
   *
   * @param activitiesNext {@link String}
   * @return this builder
   */
  public GetDocumentActivitiesRequestBuilder next(final String activitiesNext) {
    this.next = activitiesNext;
    return this;
  }

  /**
   * Submit Get Document Activities.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return ApiHttpResponse
   */
  public ApiHttpResponse<GetUserActivitesResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new UserActivitiesApi(apiClient)
        .getDocumentUserActivities(this.documentId, siteId, next, limit));
  }
}
