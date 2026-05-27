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
import com.formkiq.client.model.UpdateEntityRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;

/**
 * Builder for {@link UpdateEntityRequest}.
 */
public class UpdateEntityRequestBuilder implements HttpRequestBuilder<UpdateResponse> {

  /** {@link UpdateEntityRequest}. */
  private final UpdateEntityRequest request;
  /** {@link AddEntity}. */
  private final AddEntity addEntity = new AddEntity();
  /** Entity Namespace. */
  private final String namespace;
  /** Entity Type. */
  private final String entityType;
  /** Entity. */
  private final String entity;

  /**
   * constructor.
   * 
   * @param entityTypeId {@link String}
   * @param entityId {@link String}
   */
  public UpdateEntityRequestBuilder(final String entityTypeId, final String entityId) {
    this(entityTypeId, null, entityId);
  }

  /**
   * constructor.
   * 
   * @param entityTypeName {@link String}
   * @param entityNamespace {@link String}
   * @param entityId {@link String}
   */
  public UpdateEntityRequestBuilder(final String entityTypeName, final String entityNamespace,
      final String entityId) {
    this.entityType = entityTypeName;
    this.namespace = entityNamespace;
    this.entity = entityId;
    this.request = new UpdateEntityRequest().entity(addEntity);
  }

  public UpdateEntityRequestBuilder addAttribute(final String attributeKey,
      final BigDecimal numberValue) {
    addEntity
        .addAttributesItem(new AddEntityAttribute().key(attributeKey).numberValue(numberValue));
    return this;
  }

  public UpdateEntityRequestBuilder addAttribute(final String attributeKey,
      final Boolean booleanValue) {
    addEntity
        .addAttributesItem(new AddEntityAttribute().key(attributeKey).booleanValue(booleanValue));
    return this;
  }

  public UpdateEntityRequestBuilder addAttribute(final String attributeKey,
      final String stringValue) {
    addEntity
        .addAttributesItem(new AddEntityAttribute().key(attributeKey).stringValue(stringValue));
    return this;
  }

  public UpdateEntityRequestBuilder name(final String entityName) {
    addEntity.name(entityName);
    return this;
  }

  @Override
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).updateEntity(entityType, entity,
        this.request, siteId, namespace));
  }
}
