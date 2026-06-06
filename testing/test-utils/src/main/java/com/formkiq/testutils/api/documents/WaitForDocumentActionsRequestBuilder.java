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

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.DocumentActionStatus;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Builder for waiting on Get Document Actions Request.
 */
public class WaitForDocumentActionsRequestBuilder
    implements HttpRequestBuilder<GetDocumentActionsResponse> {
  /** Default Max Attempts. */
  private static final int DEFAULT_MAX_ATTEMPTS = 120;

  /** Terminal failure statuses. */
  private static final Collection<DocumentActionStatus> FAILURE_STATUSES =
      List.of(DocumentActionStatus.FAILED, DocumentActionStatus.FAILED_RETRY,
          DocumentActionStatus.MAX_RETRIES_REACHED);

  /** {@link GetDocumentActionsRequestBuilder}. */
  private final GetDocumentActionsRequestBuilder actionReq;
  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** Expected Number Of Actions. */
  private Integer expectedNumberOfActions;
  /** Max Attempts. */
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  /**
   * constructor.
   *
   * @param document {@link DocumentArtifact}
   */
  public WaitForDocumentActionsRequestBuilder(final DocumentArtifact document) {
    this.document = document;
    this.actionReq = new GetDocumentActionsRequestBuilder(document).limit("1000");
  }

  /**
   * Set Expected Number Of Actions.
   *
   * @param numberOfActions {@link Integer}
   * @return {@link WaitForDocumentActionsRequestBuilder}
   */
  public WaitForDocumentActionsRequestBuilder expectedNumberOfActions(
      final int numberOfActions) {
    this.expectedNumberOfActions = numberOfActions;
    return this;
  }

  /**
   * Set Max Attempts.
   *
   * @param attempts {@link Integer}
   * @return {@link WaitForDocumentActionsRequestBuilder}
   */
  public WaitForDocumentActionsRequestBuilder maxAttempts(final int attempts) {
    this.maxAttempts = attempts;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentActionsResponse> submit(final ApiClient apiClient,
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
   * Wait for document actions.
   *
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return {@link GetDocumentActionsResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public ApiHttpResponse<GetDocumentActionsResponse> waitFor(final ApiClient apiClient, final String siteId)
      throws ApiException, InterruptedException {

    ApiHttpResponse<GetDocumentActionsResponse> lastResponse = null;

    for (int i = 0; i < this.maxAttempts; i++) {

      ApiHttpResponse<GetDocumentActionsResponse> response =
          this.actionReq.submit(apiClient, siteId);
      lastResponse = response;

      if (!response.isError()) {
        List<DocumentAction> actions = notNull(response.response().getActions());

        if (hasFailure(actions)) {
          throw new ApiException("Found failed document action for site " + siteId + " documentId "
              + this.document.documentId() + ". Last statuses: " + describeStatuses(actions));
        }

        if (isMatch(actions)) {
          return response;
        }
      }

      if (response.is5XX()) {
        return response;
      }

      TimeUnit.SECONDS.sleep(1);
    }

    throw new ApiException("Timed out waiting for document actions after " + this.maxAttempts
        + " attempts for site " + siteId + " documentId " + this.document.documentId()
        + ". Last statuses: " + describeStatuses(lastResponse));
  }

  private String describeStatuses(final ApiHttpResponse<GetDocumentActionsResponse> response) {
    if (response == null) {
      return "none";
    }

    if (response.isError()) {
      return "error " + response.exception().getCode() + " "
          + response.exception().getMessage();
    }

    return describeStatuses(notNull(response.response().getActions()));
  }

  private String describeStatuses(final List<DocumentAction> actions) {
    if (actions.isEmpty()) {
      return "none";
    }

    return actions.stream()
        .map(a -> a.getType() + "=" + a.getStatus())
        .collect(Collectors.joining(", "));
  }

  private boolean hasFailure(final List<DocumentAction> actions) {
    return actions.stream().anyMatch(a -> FAILURE_STATUSES.contains(a.getStatus()));
  }

  private boolean isMatch(final List<DocumentAction> actions) {
    return !actions.isEmpty() && isExpectedNumberOfActions(actions)
        && actions.stream().allMatch(a -> DocumentActionStatus.COMPLETE.equals(a.getStatus())
            || DocumentActionStatus.SKIPPED.equals(a.getStatus())
            || DocumentActionStatus.IN_QUEUE.equals(a.getStatus()));
  }

  private boolean isExpectedNumberOfActions(final List<DocumentAction> actions) {
    return this.expectedNumberOfActions == null
        || this.expectedNumberOfActions.equals(actions.size());
  }
}
