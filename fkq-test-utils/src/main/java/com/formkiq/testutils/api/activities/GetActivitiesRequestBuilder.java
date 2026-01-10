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
import com.formkiq.client.model.GetActivitesResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Builder for Get Document Request.
 */
public class GetActivitiesRequestBuilder implements HttpRequestBuilder<GetActivitesResponse> {

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
  /** Start {@link OffsetDateTime}. */
  private OffsetDateTime start;
  /** End {@link OffsetDateTime}. */
  private OffsetDateTime end;
  /** Sort. */
  private String sort;
  /** Workflow Id. */
  private String workflowId;
  /** Ruleset Id. */
  private String rulesetId;
  /** Attribute Key. */
  private String attributeKey;
  /** {@link String}. */
  private String ruleId;
  /** {@link String}. */
  private String schema;
  /** {@link String}. */
  private String classificationId;
  /** {@link String}. */
  private String mappingId;
  /** {@link String}. */
  private String apiKey;
  /** {@link String}. */
  private String controlPolicy;

  /**
   * constructor.
   */
  public GetActivitiesRequestBuilder() {}

  /**
   * Set Api Key.
   *
   * @param activitiesApiKey {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder apiKey(final String activitiesApiKey) {
    this.apiKey = activitiesApiKey;
    return this;
  }

  /**
   * Set Attribute Key.
   *
   * @param activitiesAttributeKey {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder attributeKey(final String activitiesAttributeKey) {
    this.attributeKey = activitiesAttributeKey;
    return this;
  }

  /**
   * Set ClassificationId.
   *
   * @param activitiesClassificationId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder classificationId(final String activitiesClassificationId) {
    this.classificationId = activitiesClassificationId;
    return this;
  }

  /**
   * Set Control Policy.
   *
   * @param activitiesControlPolicy {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder controlPolicy(final String activitiesControlPolicy) {
    this.controlPolicy = activitiesControlPolicy;
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
   * Set the End Date.
   *
   * @param date {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder end(final Instant date) {
    this.end = date.atOffset(ZoneOffset.UTC);
    return this;
  }

  /**
   * Set the End Date.
   *
   * @param date {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder end(final String date) {
    this.end = Instant.parse(date).atOffset(ZoneOffset.UTC);
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
   * Set MappingId.
   *
   * @param activitiesMappingId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder mappingId(final String activitiesMappingId) {
    this.mappingId = activitiesMappingId;
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
   * Set the pagination cursor for the activitiesNext set of results.
   *
   * @param activitiesNext {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder next(final String activitiesNext) {
    this.next = activitiesNext;
    return this;
  }

  /**
   * Set Rule Id.
   *
   * @param activitiesRuleId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder ruleId(final String activitiesRuleId) {
    this.ruleId = activitiesRuleId;
    return this;
  }

  /**
   * Set Ruleset Id.
   *
   * @param activitiesRulesetId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder rulesetId(final String activitiesRulesetId) {
    this.rulesetId = activitiesRulesetId;
    return this;
  }

  /**
   * Set Schema.
   *
   * @param activitiesSchema {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder schema(final String activitiesSchema) {
    this.schema = activitiesSchema;
    return this;
  }

  /**
   * Set the Sort.
   *
   * @param sortOrder {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder sort(final String sortOrder) {
    this.sort = sortOrder;
    return this;
  }

  /**
   * Set the Start Date.
   *
   * @param date {@link Instant}
   * @return this builder
   */
  public GetActivitiesRequestBuilder start(final Instant date) {
    this.start = date.atOffset(ZoneOffset.UTC);
    return this;
  }

  /**
   * Set the Start Date.
   *
   * @param date {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder start(final String date) {
    this.start = Instant.parse(date).atOffset(ZoneOffset.UTC);
    return this;
  }

  @Override
  public ApiHttpResponse<GetActivitesResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new UserActivitiesApi(apiClient).getResourceActivities(siteId,
        this.documentId, this.entityTypeId, this.namespace, this.entityId, rulesetId, ruleId,
        workflowId, attributeKey, schema, classificationId, mappingId, apiKey, controlPolicy, start,
        end, sort, next, limit, this.userId));
  }

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
   * Set Workflow Id.
   *
   * @param activitiesWorkflowId {@link String}
   * @return this builder
   */
  public GetActivitiesRequestBuilder workflowId(final String activitiesWorkflowId) {
    this.workflowId = activitiesWorkflowId;
    return this;
  }
}
