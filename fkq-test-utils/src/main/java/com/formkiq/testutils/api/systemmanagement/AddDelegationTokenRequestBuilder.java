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
package com.formkiq.testutils.api.systemmanagement;

import java.util.List;

import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.DelegationTokenPrincipal;
import com.formkiq.client.model.GenerateDelegationTokenRequest;
import com.formkiq.client.model.GenerateDelegationTokenRequest.PermissionsEnum;
import com.formkiq.client.model.GenerateDelegationTokenResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * POST /sites/{siteId}/delegationTokens.
 */
public class AddDelegationTokenRequestBuilder
    implements HttpRequestBuilder<GenerateDelegationTokenResponse> {

  /** {@link GenerateDelegationTokenRequest}. */
  private final GenerateDelegationTokenRequest req = new GenerateDelegationTokenRequest();

  /**
   * constructor.
   */
  public AddDelegationTokenRequestBuilder() {}

  /**
   * Add permission.
   *
   * @param permission {@link PermissionsEnum}
   * @return {@link AddDelegationTokenRequestBuilder}
   */
  public AddDelegationTokenRequestBuilder addPermission(final PermissionsEnum permission) {
    this.req.addPermissionsItem(permission);
    return this;
  }

  /**
   * Expires in seconds.
   *
   * @param seconds {@link Integer}
   * @return {@link AddDelegationTokenRequestBuilder}
   */
  public AddDelegationTokenRequestBuilder expiresInSeconds(final Integer seconds) {
    this.req.expiresInSeconds(seconds);
    return this;
  }

  /**
   * Set on behalf of username.
   *
   * @param username {@link String}
   * @return {@link AddDelegationTokenRequestBuilder}
   */
  public AddDelegationTokenRequestBuilder onBehalfOf(final String username) {
    this.req.onBehalfOf(new DelegationTokenPrincipal().username(username));
    return this;
  }

  /**
   * Set permissions.
   *
   * @param permissions {@link PermissionsEnum}
   * @return {@link AddDelegationTokenRequestBuilder}
   */
  public AddDelegationTokenRequestBuilder permissions(final List<PermissionsEnum> permissions) {
    this.req.permissions(permissions);
    return this;
  }

  /**
   * Set reason.
   *
   * @param reason {@link String}
   * @return {@link AddDelegationTokenRequestBuilder}
   */
  public AddDelegationTokenRequestBuilder reason(final String reason) {
    this.req.reason(reason);
    return this;
  }

  @Override
  public ApiHttpResponse<GenerateDelegationTokenResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new SystemManagementApi(apiClient).generateDelegationToken(siteId, this.req));
  }
}
