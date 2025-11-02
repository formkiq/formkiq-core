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
package com.formkiq.testutils.api.accesscontrol;

import com.formkiq.client.api.AccessControlApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.OpaPolicyItem;
import com.formkiq.client.model.SetOpaAccessPolicyItemsRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

/**
 * Builder for Set Access Policy Items.
 */
public class SetAccessPolicyItemsRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** {@link SetOpaAccessPolicyItemsRequest}. */
  private final SetOpaAccessPolicyItemsRequest req;

  /**
   * constructor.
   */
  public SetAccessPolicyItemsRequestBuilder() {
    req = new SetOpaAccessPolicyItemsRequest();
  }

  /**
   * Add Opa Policy Item.
   *
   * @param item {@link OpaPolicyItem}
   * @return this builder
   */
  public SetAccessPolicyItemsRequestBuilder addPolicyItem(final OpaPolicyItem item) {
    this.req.addPolicyItemsItem(item);
    return this;
  }

  /**
   * Add Opa Policy Item.
   *
   * @param items {@link OpaPolicyItem}
   * @return this builder
   */
  public SetAccessPolicyItemsRequestBuilder setPolicyItems(final List<OpaPolicyItem> items) {
    this.req.setPolicyItems(items);
    return this;
  }

  @Override
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(
        () -> new AccessControlApi(apiClient).setOpaAccessPolicyItems(siteId, req));
  }
}
