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
package com.formkiq.testutils.api.folders;

import com.formkiq.client.api.DocumentFoldersApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetFoldersResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document Folders.
 */
public class GetFoldersRequestBuilder implements HttpRequestBuilder {

  /** {@link String}. */
  private String indexKey;
  /** {@link String}. */
  private String path;
  /** {@link String}. */
  private String limit;
  /** {@link String}. */
  private String next;

  /**
   * constructor.
   */
  public GetFoldersRequestBuilder() {}

  /**
   * Set the index key for filtering results.
   * 
   * @param folderIndexKey {@link String}
   * @return this builder
   */
  public GetFoldersRequestBuilder indexKey(final String folderIndexKey) {
    this.indexKey = folderIndexKey;
    return this;
  }

  /**
   * Set the folder folderPath to retrieve documents from.
   * 
   * @param folderPath {@link String}
   * @return this builder
   */
  public GetFoldersRequestBuilder path(final String folderPath) {
    this.path = folderPath;
    return this;
  }

  /**
   * Set the maximum number of results to return.
   * 
   * @param folderLimit {@link String}
   * @return this builder
   */
  public GetFoldersRequestBuilder limit(final String folderLimit) {
    this.limit = folderLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the folderNext set of results.
   * 
   * @param folderNext {@link String}
   * @return this builder
   */
  public GetFoldersRequestBuilder next(final String folderNext) {
    this.next = folderNext;
    return this;
  }

  @Override
  public ApiHttpResponse<GetFoldersResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentFoldersApi(apiClient).getFolderDocuments(siteId,
        indexKey, path, limit, null, next));
  }
}
