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
import com.formkiq.client.model.MappingAttribute;
import com.formkiq.client.model.MappingAttributeContent;
import com.formkiq.client.model.MappingAttributeLabelMatchingType;
import com.formkiq.client.model.MappingAttributeSourceType;
import com.formkiq.client.model.SetMappingRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link SetMappingRequest}.
 */
public class SetMappingRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** {@link SetMappingRequest}. */
  private final SetMappingRequest request;
  /** {@link AddMapping}. */
  private final AddMapping addMapping = new AddMapping();
  /** {@link String}. */
  private final String id;
  /** Create if missing. */
  private boolean createIfMissing = false;


  /**
   * constructor.
   *
   * @param mappingId {@link String}
   * @param name {@link String}
   * @param description {@link String}
   */
  public SetMappingRequestBuilder(final String mappingId, final String name,
      final String description) {
    this.id = mappingId;
    this.request = new SetMappingRequest();
    this.request.setMapping(addMapping);
    addMapping.setName(name);
    addMapping.setDescription(description);
  }

  /**
   * Set Mapping Attribute.
   *
   * @param attributeKey {@link String}
   * @param sourceType {@link MappingAttributeSourceType}
   * @param labelMatchingType {@link MappingAttributeLabelMatchingType}
   * @param labelText {@link String}
   * @return SetMappingRequestBuilder
   */
  public SetMappingRequestBuilder addAttribute(final String attributeKey,
      final MappingAttributeSourceType sourceType,
      final MappingAttributeLabelMatchingType labelMatchingType, final String labelText) {
    addMapping.addAttributesItem(
        createMappingAttribute(attributeKey, sourceType, labelMatchingType, labelText));
    return this;
  }

  /**
   * Set create if missing.
   *
   * @param value boolean
   * @return {@link SetMappingRequestBuilder}
   */
  public SetMappingRequestBuilder createIfMissing(final boolean value) {
    this.createIfMissing = value;
    return this;
  }

  private MappingAttribute createMappingAttribute(final String attributeKey,
      final MappingAttributeSourceType sourceType,
      final MappingAttributeLabelMatchingType labelMatchingType, final String labelText) {
    return switch (sourceType) {
      case CONTENT -> new MappingAttribute(new MappingAttributeContent().attributeKey(attributeKey)
          .sourceType(MappingAttributeSourceType.CONTENT).labelMatchingType(labelMatchingType)
          .addLabelTextsItem(labelText));
      case CONTENT_KEY_VALUE -> new MappingAttribute(new MappingAttributeContent()
          .attributeKey(attributeKey).sourceType(MappingAttributeSourceType.CONTENT_KEY_VALUE)
          .labelMatchingType(labelMatchingType).addLabelTextsItem(labelText));
      default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    };
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return SetResponse
   */
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new MappingsApi(apiClient).setMapping(id, this.request, siteId,
        this.createIfMissing));
  }
}
