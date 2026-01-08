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
import com.formkiq.client.model.DeleteRuleResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Delete Ruleset.
 */
public class DeleteRuleRequestBuilder implements HttpRequestBuilder<DeleteRuleResponse> {

  /** {@link String}. */
  private final String ruleset;
  /** {@link String}. */
  private final String rule;

  /**
   * constructor.
   *
   * @param rulesetId {@link String}
   * @param ruleId {@link String}
   */
  public DeleteRuleRequestBuilder(final String rulesetId, final String ruleId) {
    ruleset = rulesetId;
    rule = ruleId;
  }

  @Override
  public ApiHttpResponse<DeleteRuleResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new RulesetsApi(apiClient).deleteRule(ruleset, rule, siteId));
  }
}
