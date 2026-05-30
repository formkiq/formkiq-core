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

import java.util.concurrent.TimeUnit;

import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetDocumentContentResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for waiting on Get Document Content Request.
 */
public class WaitForDocumentContentRequestBuilder
    implements HttpRequestBuilder<GetDocumentContentResponse> {

  /** {@link GetDocumentContentRequestBuilder}. */
  private final GetDocumentContentRequestBuilder getDocumentContent;
  /** Expected Content. */
  private String content;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public WaitForDocumentContentRequestBuilder(final String documentId) {
    this.getDocumentContent = new GetDocumentContentRequestBuilder(documentId);
  }

  /**
   * Set expected document content.
   *
   * @param documentContent {@link String}
   * @return {@link WaitForDocumentContentRequestBuilder}
   */
  public WaitForDocumentContentRequestBuilder content(final String documentContent) {
    this.content = documentContent;
    return this;
  }

  private boolean isMatch(final ApiHttpResponse<GetDocumentContentResponse> response) {
    if (response.isError()) {
      return false;
    }

    GetDocumentContentResponse documentContent = response.response();
    if (documentContent == null) {
      return false;
    }

    if (this.content != null) {
      return this.content.equals(documentContent.getContent());
    }

    return true;
  }

  /**
   * Set expected matching text.
   *
   * @param text {@link String}
   * @return {@link WaitForDocumentContentRequestBuilder}
   */
  public WaitForDocumentContentRequestBuilder matchingText(final String text) {
    return content(text);
  }

  /**
   * Set Artifact Id.
   *
   * @param id {@link String}
   * @return {@link WaitForDocumentContentRequestBuilder}
   */
  public WaitForDocumentContentRequestBuilder setArtifactId(final String id) {
    this.getDocumentContent.setArtifactId(id);
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentContentResponse> submit(final ApiClient apiClient,
      final String siteId) {
    try {
      return new ApiHttpResponse<>(waitFor(apiClient, siteId), null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ApiHttpResponse<>(null, new ApiException(e));
    } catch (ApiException e) {
      return new ApiHttpResponse<>(null, e);
    }
  }

  /**
   * Set expected document text.
   *
   * @param text {@link String}
   * @return {@link WaitForDocumentContentRequestBuilder}
   */
  public WaitForDocumentContentRequestBuilder text(final String text) {
    return content(text);
  }

  /**
   * Set Version Key.
   *
   * @param documentVersionKey {@link String}
   * @return {@link WaitForDocumentContentRequestBuilder}
   */
  public WaitForDocumentContentRequestBuilder versionKey(final String documentVersionKey) {
    this.getDocumentContent.versionKey(documentVersionKey);
    return this;
  }

  /**
   * Wait for document content.
   *
   * @param apiClient {@link ApiClient}
   * @param siteId {@link String}
   * @return {@link GetDocumentContentResponse}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  public GetDocumentContentResponse waitFor(final ApiClient apiClient, final String siteId)
      throws ApiException, InterruptedException {

    while (true) {

      ApiHttpResponse<GetDocumentContentResponse> response =
          this.getDocumentContent.submit(apiClient, siteId);

      if (isMatch(response)) {
        return response.response();
      }

      if (response.is5XX()) {
        throw response.exception();
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }
}
