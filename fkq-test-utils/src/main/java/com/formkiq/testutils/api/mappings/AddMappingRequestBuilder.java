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
package com.formkiq.testutils.api.mappings;

import com.formkiq.client.api.MappingsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddMapping;
import com.formkiq.client.model.AddMappingRequest;
import com.formkiq.client.model.AddMappingResponse;
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link AddMappingRequest}.
 */
public class AddMappingRequestBuilder implements HttpRequestBuilder<AddMappingResponse> {

  /** {@link AddMappingRequest}. */
  private final AddMappingRequest request;
  /** {@link AddMapping}. */
  private final AddMapping mapping = new AddMapping();

  /**
   * constructor.
   *
   * @param name {@link String}
   * @param description {@link String}
   */
  public AddMappingRequestBuilder(final String name, final String description) {
    this.request = new AddMappingRequest();
    this.request.setMapping(mapping);
    mapping.setName(name);
    mapping.setDescription(description);
  }

  /**
   * Add Mapping Attribute.
   *
   * @param attributeKey {@link String}
   * @param sourceType {@link MappingAttributeSourceType}
   * @param labelMatchingType {@link MappingAttributeLabelMatchingType}
   * @param labelText {@link String}
   * @return AddMappingRequestBuilder
   */
  public AddMappingRequestBuilder addAttribute(final String attributeKey,
      final MappingAttributeSourceType sourceType,
      final MappingAttributeLabelMatchingType labelMatchingType, final String labelText) {
    mapping.addAttributesItem(new MappingAttribute().attributeKey(attributeKey)
        .sourceType(sourceType).labelMatchingType(labelMatchingType).addLabelTextsItem(labelText));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddMappingResponse
   */
  public ApiHttpResponse<AddMappingResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new MappingsApi(apiClient).addMapping(this.request, siteId));
  }
}
