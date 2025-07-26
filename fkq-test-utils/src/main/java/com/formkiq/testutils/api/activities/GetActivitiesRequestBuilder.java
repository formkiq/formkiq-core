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
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetActivitesResponse;
import com.formkiq.client.model.GetUserActivitesResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document Request.
 */
public class GetActivitiesRequestBuilder implements HttpRequestBuilder {

  /** {@link String}. */
  private String documentId;
  /** Next. */
  private String next;
  /** Limit. */
  private String limit;
  /** Entity Type Id. */
  private String entityTypeId;
  /** Entity Type Namespace. */
  private String namespace;
  /** Entity Id. */
  private String entityId;
  /** User Id. */
  private String userId;

  /**
   * constructor.
   */
  public GetActivitiesRequestBuilder() {}

  /**
   * Set User Id.
   *
   * @param activitiesUserId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder userId(final String activitiesUserId) {
    this.userId = activitiesUserId;
    return this;
  }

  /**
   * Set Document Id.
   *
   * @param activitiesDocumentId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder documentId(final String activitiesDocumentId) {
    this.documentId = activitiesDocumentId;
    return this;
  }

  /**
   * Set Entity Id.
   *
   * @param activitiesEntityId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder entityId(final String activitiesEntityId) {
    this.entityId = activitiesEntityId;
    return this;
  }

  /**
   * Set Entity Type Id.
   *
   * @param activitiesEntityTypeId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder entityTypeId(final String activitiesEntityTypeId) {
    this.entityTypeId = activitiesEntityTypeId;
    return this;
  }

  /**
   * Set Entity Type Namespace.
   *
   * @param activitiesNamespace {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder namespace(final String activitiesNamespace) {
    this.namespace = activitiesNamespace;
    return this;
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param activitiesLimit {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder limit(final String activitiesLimit) {
    this.limit = activitiesLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the activitiesNext set of results.
   *
   * @param activitiesNext {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder next(final String activitiesNext) {
    this.next = activitiesNext;
    return this;
  }

  @Override
  public ApiHttpResponse<GetActivitesResponse> submit(final ApiClient apiClient,
      final String siteId) {
    ApiException ex = null;
    GetActivitesResponse response = null;

    try {
      // response = new UserActivitiesApi(apiClient).getDocumentUserActivities(this.documentId,
      // siteId, next, limit);
      response = new UserActivitiesApi(apiClient).getResourceActivities(siteId, this.documentId,
          this.entityTypeId, this.namespace, this.entityId, next, limit, this.userId);
    } catch (ApiException e) {
      ex = e;
    }

    return new ApiHttpResponse<>(response, ex);
  }

  /**
   * Submit Get Document Activities.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return ApiHttpResponse
   */
  public ApiHttpResponse<GetUserActivitesResponse> submitDocumentActivities(
      final ApiClient apiClient, final String siteId) {
    ApiException ex = null;
    GetUserActivitesResponse response = null;

    try {
      response = new UserActivitiesApi(apiClient).getDocumentUserActivities(this.documentId, siteId,
          next, limit);
    } catch (ApiException e) {
      ex = e;
    }

    return new ApiHttpResponse<>(response, ex);
  }

  /**
   * Submit GET User Activities.
   * 
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return ApiHttpResponse
   */
  public ApiHttpResponse<GetUserActivitesResponse> submitUserActivities(final ApiClient apiClient,
      final String siteId) {
    ApiException ex = null;
    GetUserActivitesResponse response = null;

    try {
      response = new UserActivitiesApi(apiClient).getUserActivities(siteId, next, limit, userId);
    } catch (ApiException e) {
      ex = e;
    }

    return new ApiHttpResponse<>(response, ex);
  }
}
