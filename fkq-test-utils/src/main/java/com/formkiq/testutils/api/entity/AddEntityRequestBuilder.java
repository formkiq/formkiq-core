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
package com.formkiq.testutils.api.entity;

import com.formkiq.client.api.EntityApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddEntity;
import com.formkiq.client.model.AddEntityAttribute;
import com.formkiq.client.model.AddEntityRequest;
import com.formkiq.client.model.AddEntityResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;

/**
 * Builder for {@link AddEntityRequest}.
 */
public class AddEntityRequestBuilder implements HttpRequestBuilder<AddEntityResponse> {

  /** {@link AddEntityRequest}. */
  private final AddEntityRequest request;
  /** {@link AddEntity}. */
  private final AddEntity addEntity = new AddEntity();
  /** Entity Namespace. */
  private final String namespace;
  /** Entity Type. */
  private final String entityType;

  /**
   * constructor.
   * @param entityTypeId {@link String}
   */
  public AddEntityRequestBuilder(final String entityTypeId) {
    this(entityTypeId, null);
  }

  /**
   * constructor.
   * @param entityTypeName {@link String}
   * @param entityNamespace {@link String}
   */
  public AddEntityRequestBuilder(final String entityTypeName, final String entityNamespace) {
    this.entityType = entityTypeName;
    this.namespace = entityNamespace;
    this.request = new AddEntityRequest().entity(addEntity);
  }

  public AddEntityRequestBuilder name(final String entityName) {
    addEntity.name(entityName);
    return this;
  }

  public AddEntityRequestBuilder addAttribute(final String attributeKey, final String stringValue) {
    addEntity.addAttributesItem(new AddEntityAttribute().key(attributeKey).stringValue(stringValue));
    return this;
  }

  public AddEntityRequestBuilder addAttribute(final String attributeKey, final BigDecimal numberValue) {
    addEntity.addAttributesItem(new AddEntityAttribute().key(attributeKey).numberValue(numberValue));
    return this;
  }

  public AddEntityRequestBuilder addAttribute(final String attributeKey, final Boolean booleanValue) {
    addEntity.addAttributesItem(new AddEntityAttribute().key(attributeKey).booleanValue(booleanValue));
    return this;
  }

  @Override
  public ApiHttpResponse<AddEntityResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).addEntity(entityType, this.request, siteId, namespace));
  }
}
