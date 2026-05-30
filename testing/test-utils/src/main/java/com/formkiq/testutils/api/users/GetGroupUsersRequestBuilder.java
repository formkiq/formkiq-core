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
package com.formkiq.testutils.api.users;

import com.formkiq.client.api.UserManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetUsersInGroupResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get /groups/{groupName}/users.
 */
public class GetGroupUsersRequestBuilder implements HttpRequestBuilder<GetUsersInGroupResponse> {

  /** Group Name. */
  private final String name;
  /** {@link String}. */
  private String limit;
  /** {@link String}. */
  private String next;

  /**
   * constructor.
   * 
   * @param groupName {@link String}
   */
  public GetGroupUsersRequestBuilder(final String groupName) {
    this.name = groupName;
  }

  /**
   * Set the maximum number of results to return.
   * 
   * @param folderLimit {@link String}
   * @return this builder
   */
  public GetGroupUsersRequestBuilder limit(final String folderLimit) {
    this.limit = folderLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the folderNext set of results.
   * 
   * @param folderNext {@link String}
   * @return this builder
   */
  public GetGroupUsersRequestBuilder next(final String folderNext) {
    this.next = folderNext;
    return this;
  }

  @Override
  public ApiHttpResponse<GetUsersInGroupResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new UserManagementApi(apiClient).getUsersInGroup(name, limit, next));
  }
}
