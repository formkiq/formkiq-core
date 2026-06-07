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

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentWorkflow;
import com.formkiq.client.model.DocumentWorkflowStatus;
import com.formkiq.client.model.GetDocumentWorkflowResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Builder for waiting on Get Document Workflow Request.
 */
public class WaitForDocumentWorkflowRequestBuilder
    implements HttpRequestBuilder<GetDocumentWorkflowResponse> {
  /** Default Max Attempts. */
  private static final int DEFAULT_MAX_ATTEMPTS = 120;

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** {@link GetDocumentWorkflowRequestBuilder}. */
  private final GetDocumentWorkflowRequestBuilder workflowReq;
  /** Workflow Id. */
  private final String workflowId;
  /** Max Attempts. */
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   * @param documentWorkflowId {@link String}
   */
  public WaitForDocumentWorkflowRequestBuilder(final DocumentArtifact documentArtifact,
      final String documentWorkflowId) {
    this.document = documentArtifact;
    this.workflowId = documentWorkflowId;
    this.workflowReq = new GetDocumentWorkflowRequestBuilder(documentArtifact, documentWorkflowId);
  }

  private String describeStatus(final ApiHttpResponse<GetDocumentWorkflowResponse> response) {
    if (response == null) {
      return "none";
    }

    if (response.isError()) {
      return "error " + response.exception().getCode() + " " + response.exception().getMessage();
    }

    return describeStatus(response.response().getWorkflow());
  }

  private String describeStatus(final DocumentWorkflow workflow) {
    if (workflow == null) {
      return "none";
    }

    return String.valueOf(workflow.getStatus());
  }

  /**
   * Set Max Attempts.
   *
   * @param attempts {@link Integer}
   * @return {@link WaitForDocumentWorkflowRequestBuilder}
   */
  public WaitForDocumentWorkflowRequestBuilder maxAttempts(final int attempts) {
    this.maxAttempts = attempts;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentWorkflowResponse> submit(final ApiClient apiClient,
      final String siteId) {
    try {
      return waitFor(apiClient, siteId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ApiHttpResponse<>(null, new ApiException(e));
    } catch (ApiException e) {
      return new ApiHttpResponse<>(null, e);
    }
  }

  /**
   * Wait for document workflow.
   *
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return {@link GetDocumentWorkflowResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public ApiHttpResponse<GetDocumentWorkflowResponse> waitFor(final ApiClient apiClient,
      final String siteId) throws ApiException, InterruptedException {

    ApiHttpResponse<GetDocumentWorkflowResponse> lastResponse = null;

    for (int i = 0; i < this.maxAttempts; i++) {

      ApiHttpResponse<GetDocumentWorkflowResponse> response =
          this.workflowReq.submit(apiClient, siteId);
      lastResponse = response;

      if (!response.isError()) {
        DocumentWorkflow workflow = response.response().getWorkflow();

        if (workflow != null && DocumentWorkflowStatus.FAILED.equals(workflow.getStatus())) {
          throw new ApiException("Found failed document workflow for site " + siteId
              + " documentId " + this.document.documentId() + " workflowId " + this.workflowId
              + ". Last status: " + describeStatus(workflow));
        }

        if (workflow != null && DocumentWorkflowStatus.COMPLETE.equals(workflow.getStatus())) {
          return response;
        }
      }

      if (response.is5XX()) {
        return response;
      }

      TimeUnit.SECONDS.sleep(1);
    }

    throw new ApiException("Timed out waiting for document workflow after " + this.maxAttempts
        + " attempts for site " + siteId + " documentId " + this.document.documentId()
        + " workflowId " + this.workflowId + ". Last status: " + describeStatus(lastResponse));
  }
}
