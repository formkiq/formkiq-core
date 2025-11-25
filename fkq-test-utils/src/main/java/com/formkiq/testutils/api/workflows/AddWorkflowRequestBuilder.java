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
package com.formkiq.testutils.api.workflows;

import com.formkiq.client.api.DocumentWorkflowsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddWorkflowRequest;
import com.formkiq.client.model.AddWorkflowResponse;
import com.formkiq.client.model.AddWorkflowStep;
import com.formkiq.client.model.AddWorkflowStepDecision;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.WorkflowStatus;
import com.formkiq.client.model.WorkflowStepDecisionType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for {@link AddWorkflowRequest}.
 */
public class AddWorkflowRequestBuilder implements HttpRequestBuilder<AddWorkflowResponse> {

  /** {@link AddWorkflowRequest}. */
  private final AddWorkflowRequest request;
  /** {@link AddWorkflowStep}. */
  private final Map<String, AddWorkflowStep> workflowSteps = new HashMap<>();

  /**
   * constructor.
   * 
   * @param name {@link String}
   */
  public AddWorkflowRequestBuilder(final String name) {
    this(name, "description", WorkflowStatus.ACTIVE);
  }

  /**
   * constructor.
   *
   * @param name {@link String}
   * @param description {@link String}
   * @param status {@link WorkflowStatus}
   */
  public AddWorkflowRequestBuilder(final String name, final String description,
      final WorkflowStatus status) {
    this.request = new AddWorkflowRequest();
    this.request.setName(name);
    this.request.setDescription(description);
    this.request.setStatus(status);
  }

  /**
   * Add Document Action.
   *
   * @param stepId {@link String}
   * @param type {@link DocumentActionType}
   * @return AddWorkflowRequestBuilder
   */
  public AddWorkflowRequestBuilder addAction(final String stepId, final DocumentActionType type) {
    AddWorkflowStep step = getWorkflowStep(stepId);
    step.action(new AddAction().type(type));
    return this;
  }

  /**
   * Add Document Action.
   *
   * @param stepId {@link String}
   * @param type {@link WorkflowStepDecisionType}
   * @param nextStepId {@link String}
   * @return AddWorkflowRequestBuilder
   */
  public AddWorkflowRequestBuilder addDecision(final String stepId,
      final WorkflowStepDecisionType type, final String nextStepId) {
    AddWorkflowStep step = getWorkflowStep(stepId);
    step.addDecisionsItem(new AddWorkflowStepDecision().type(type).nextStepId(nextStepId));
    return this;
  }

  private AddWorkflowStep getWorkflowStep(final String stepId) {
    AddWorkflowStep step = workflowSteps.get(stepId);
    if (step == null) {
      step = new AddWorkflowStep().stepId(stepId);
      workflowSteps.put(stepId, step);
      this.request.addStepsItem(step);
    }

    return step;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddWorkflowResponse
   */
  public ApiHttpResponse<AddWorkflowResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new DocumentWorkflowsApi(apiClient).addWorkflow(this.request, siteId));
  }
}
