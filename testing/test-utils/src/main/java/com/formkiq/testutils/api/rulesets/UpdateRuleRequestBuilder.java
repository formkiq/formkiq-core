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
package com.formkiq.testutils.api.rulesets;

import com.formkiq.client.api.RulesetsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.RuleCondition;
import com.formkiq.client.model.RulesetStatus;
import com.formkiq.client.model.UpdateRule;
import com.formkiq.client.model.UpdateRuleRequest;
import com.formkiq.client.model.UpdateRulesetRequest;
import com.formkiq.client.model.UpdateRuleResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;

/**
 * Builder for {@link UpdateRulesetRequest}.
 */
public class UpdateRuleRequestBuilder implements HttpRequestBuilder<UpdateRuleResponse> {

  /** {@link UpdateRuleRequest}. */
  private final UpdateRuleRequest request;
  /** Ruleset Id. */
  private final String ruleset;
  /** {@link UpdateRule}. */
  private final UpdateRule update;
  /** {@link String}. */
  private final String rule;

  /**
   * constructor.
   *
   * @param rulesetId {@link String}
   * @param ruleId {@link String}
   */
  public UpdateRuleRequestBuilder(final String rulesetId, final String ruleId) {
    ruleset = rulesetId;
    rule = ruleId;
    update = new UpdateRule();
    this.request = new UpdateRuleRequest().rule(update);
  }

  /**
   * Set conditions.
   *
   * @param conditions {@link RuleCondition}
   * @return AddDocumentRequestBuilder
   */
  public UpdateRuleRequestBuilder setConditions(final RuleCondition conditions) {
    update.setConditions(conditions);
    return this;
  }

  /**
   * Set Description.
   *
   * @param description {@link String}
   * @return AddDocumentRequestBuilder
   */
  public UpdateRuleRequestBuilder setDescription(final String description) {
    update.setDescription(description);
    return this;
  }

  /**
   * Set Priority.
   *
   * @param priority {@link BigDecimal}
   * @return AddDocumentRequestBuilder
   */
  public UpdateRuleRequestBuilder setPriority(final BigDecimal priority) {
    update.setPriority(priority);
    return this;
  }

  /**
   * Set Status.
   *
   * @param status {@link RulesetStatus}
   * @return AddDocumentRequestBuilder
   */
  public UpdateRuleRequestBuilder setStatus(final RulesetStatus status) {
    update.setStatus(status);
    return this;
  }

  /**
   * Set Workflow Id.
   *
   * @param workflowId {@link String}
   * @return AddDocumentRequestBuilder
   */
  public UpdateRuleRequestBuilder setWorkflowId(final String workflowId) {
    update.setWorkflowId(workflowId);
    return this;
  }

  @Override
  public ApiHttpResponse<UpdateRuleResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new RulesetsApi(apiClient).updateRule(ruleset, rule, this.request, siteId));
  }
}
