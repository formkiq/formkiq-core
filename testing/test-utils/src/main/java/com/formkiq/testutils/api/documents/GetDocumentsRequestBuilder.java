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
package com.formkiq.testutils.api.documents;

import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.time.OffsetDateTime;

/**
 * Builder for Get /documents Request.
 */
public class GetDocumentsRequestBuilder implements HttpRequestBuilder<GetDocumentsResponse> {

  /** Action Status. */
  private String actionStatus;
  /** Sync Status. */
  private String syncStatus;
  /** Deleted. */
  private Boolean deleted;
  /** Next Token. */
  private String next;
  /** Date. */
  private String date;
  /** Tz. */
  private String tz;
  /** Soft Deleted. */
  private Boolean softDeleted;
  /** Limit. */
  private String limit;
  /** Start Date. */
  private OffsetDateTime start;
  /** End Date. */
  private OffsetDateTime end;
  /** Sort. */
  private String sort;
  /** Projection. */
  private String projection;

  /**
   * constructor.
   *
   */
  public GetDocumentsRequestBuilder() {

  }

  /**
   * Set Action Status.
   * 
   * @param documentActionStatus {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder actionStatus(final String documentActionStatus) {
    this.actionStatus = documentActionStatus;
    return this;
  }

  /**
   * Set Date.
   * 
   * @param documentDate {@link java.util.Date}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder date(final String documentDate) {
    this.date = documentDate;
    return this;
  }

  /**
   * Set Sync Status.
   * 
   * @param documentDeleted {@link Boolean}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder deleted(final Boolean documentDeleted) {
    this.deleted = documentDeleted;
    return this;
  }

  /**
   * Set End.
   * 
   * @param documentEnd {@link OffsetDateTime}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder end(final OffsetDateTime documentEnd) {
    this.end = documentEnd;
    return this;
  }

  /**
   * Set Limit.
   * 
   * @param documentLimit int
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder limit(final int documentLimit) {
    this.limit = String.valueOf(documentLimit);
    return this;
  }

  /**
   * Set Next token.
   * 
   * @param nextToken {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  /**
   * Set Projection.
   * 
   * @param documentProjection {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder projection(final String documentProjection) {
    this.projection = documentProjection;
    return this;
  }

  /**
   * Set Soft Deleted.
   * 
   * @param documentSoftDeleted {@link Boolean}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder softDeleted(final Boolean documentSoftDeleted) {
    this.softDeleted = documentSoftDeleted;
    return this;
  }

  /**
   * Set Sort.
   * 
   * @param documentSort {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder sort(final String documentSort) {
    this.sort = documentSort;
    return this;
  }

  /**
   * Set Start.
   * 
   * @param documentStart {@link OffsetDateTime}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder start(final OffsetDateTime documentStart) {
    this.start = documentStart;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new DocumentsApi(apiClient).getDocuments(siteId, actionStatus, syncStatus,
            softDeleted, deleted, date, tz, start, end, sort, next, null, projection, limit));
  }

  /**
   * Set Sync Status.
   * 
   * @param documentSyncStatus {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder syncStatus(final String documentSyncStatus) {
    this.syncStatus = documentSyncStatus;
    return this;
  }

  /**
   * Set Tz.
   * 
   * @param documentTz {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder tz(final String documentTz) {
    this.tz = documentTz;
    return this;
  }
}
