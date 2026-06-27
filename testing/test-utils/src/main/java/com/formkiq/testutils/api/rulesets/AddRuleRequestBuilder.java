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
import com.formkiq.client.model.AddRule;
import com.formkiq.client.model.AddRuleRequest;
import com.formkiq.client.model.AddRuleResponse;
import com.formkiq.client.model.RuleCondition;
import com.formkiq.client.model.RulesetStatus;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;

/**
 * Builder for {@link AddRuleRequest}.
 */
public class AddRuleRequestBuilder implements HttpRequestBuilder<AddRuleResponse> {

  /** {@link AddRuleRequest}. */
  private final AddRuleRequest request;
  /** Ruleset Id. */
  private final String ruleset;
  /** {@link AddRule}. */
  private final AddRule rule;

  /**
   * constructor.
   * 
   * @param rulesetId {@link String}
   */
  public AddRuleRequestBuilder(final String rulesetId) {
    this.ruleset = rulesetId;
    this.rule = new AddRule();
    this.request = new AddRuleRequest().rule(rule);
  }

  /**
   * Set {@link RuleCondition}.
   *
   * @param conditions {@link RuleCondition}
   * @return AddDocumentRequestBuilder
   */
  public AddRuleRequestBuilder setConditions(final RuleCondition conditions) {
    this.rule.setConditions(conditions);
    return this;
  }

  /**
   * Set {@link RuleCondition}.
   *
   * @param priority {@link BigDecimal}
   * @return AddDocumentRequestBuilder
   */
  public AddRuleRequestBuilder setPriority(final BigDecimal priority) {
    this.rule.setPriority(priority);
    return this;
  }

  /**
   * Set Description.
   *
   * @param description {@link String}
   * @return AddDocumentRequestBuilder
   */
  public AddRuleRequestBuilder setRuleset(final String description) {
    this.rule.setDescription(description);
    return this;
  }

  /**
   * Set {@link RulesetStatus}.
   *
   * @param status {@link RulesetStatus}
   * @return AddDocumentRequestBuilder
   */
  public AddRuleRequestBuilder setStatus(final RulesetStatus status) {
    this.rule.setStatus(status);
    return this;
  }

  /**
   * Set WorkflowId.
   *
   * @param workflowId {@link String}
   * @return AddDocumentRequestBuilder
   */
  public AddRuleRequestBuilder setWorkflowId(final String workflowId) {
    this.rule.setWorkflowId(workflowId);
    return this;
  }

  @Override
  public ApiHttpResponse<AddRuleResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new RulesetsApi(apiClient).addRule(ruleset, this.request, siteId));
  }
}
