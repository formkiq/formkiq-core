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
package com.formkiq.testutils.api.opensearch;

import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentFulltextResponse;
import com.formkiq.client.model.DocumentFulltextRequest;
import com.formkiq.client.model.DocumentFulltextSearch;
import com.formkiq.client.model.FulltextSearchItem;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Builder for {@link DocumentFulltextRequest}.
 */
public class FulltextSearchDocumentRequestBuilder implements HttpRequestBuilder {

  /** {@link DocumentFulltextRequest}. */
  private final DocumentFulltextRequest request;
  /** Limit. */
  private String limit;
  /** Expected Results. */
  private Integer expected = null;

  /**
   * constructor.
   */
  public FulltextSearchDocumentRequestBuilder() {
    this.request = new DocumentFulltextRequest();
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param text {@link String}
   * @return this builder
   */
  public FulltextSearchDocumentRequestBuilder text(final String text) {
    this.request.query(new DocumentFulltextSearch().text(text));
    return this;
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param docsLimit {@link String}
   * @return this builder
   */
  public FulltextSearchDocumentRequestBuilder limit(final String docsLimit) {
    this.limit = docsLimit;
    return this;
  }

  /**
   * Set the expected number of results.
   *
   * @param expectedCount {@link Integer}
   * @return this builder
   */
  public FulltextSearchDocumentRequestBuilder expectedCount(final Integer expectedCount) {
    this.expected = expectedCount;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return DocumentFulltextResponse
   */
  public ApiHttpResponse<DocumentFulltextResponse> submit(final ApiClient apiClient,
      final String siteId) {
    DocumentFulltextResponse obj = null;
    ApiException ex = null;
    try {
      obj = new AdvancedDocumentSearchApi(apiClient).searchFulltext(this.request, siteId, null,
          limit);
      List<FulltextSearchItem> documents = notNull(obj.getDocuments());

      while (expected != null && expected > 0 && expected != documents.size()) {
        if (documents.size() != expected) {
          TimeUnit.SECONDS.sleep(1);
          obj = new AdvancedDocumentSearchApi(apiClient).searchFulltext(this.request, siteId, null,
              limit);
          documents = notNull(obj.getDocuments());
        }
      }

    } catch (ApiException e) {
      ex = e;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return new ApiHttpResponse<>(obj, ex);
  }
}
