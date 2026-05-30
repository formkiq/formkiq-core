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

import com.formkiq.client.api.SystemManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;
import com.formkiq.client.model.SystemConfigurationWebUi;
import com.formkiq.client.model.UpdateSystemConfigurationRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link SetSitesSchemaRequest}.
 */
public class UpdateSystemConfigurationRequestBuilder implements HttpRequestBuilder<UpdateResponse> {
  /** {@link SetSchemaAttributes}. */
  private final UpdateSystemConfigurationRequest request = new UpdateSystemConfigurationRequest();

  /**
   * constructor.
   */
  public UpdateSystemConfigurationRequestBuilder() {}


  /**
   * Set SSO Login Redirect Enabled.
   *
   * @param ssoLoginRedirectEnabled {@link boolean}
   * @return {@link UpdateSystemConfigurationRequestBuilder}
   */
  public UpdateSystemConfigurationRequestBuilder ssoAutomaticSignIn(
      final boolean ssoLoginRedirectEnabled) {
    request.webui(new SystemConfigurationWebUi().ssoAutomaticSignIn(ssoLoginRedirectEnabled));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(
        () -> new SystemManagementApi(apiClient).updateSystemConfiguration(request));
  }
}
