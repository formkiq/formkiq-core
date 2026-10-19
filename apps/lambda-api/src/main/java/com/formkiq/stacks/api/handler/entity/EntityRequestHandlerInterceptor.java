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
package com.formkiq.stacks.api.handler.entity;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityTypeName;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiRequestHandlerInterceptor;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Strings.isUuid;

/**
 * Resolves entity type identifiers and identity before entity request authorization.
 */
public class EntityRequestHandlerInterceptor implements ApiRequestHandlerInterceptor {

  /** {@link AwsServiceCache}. */
  private final AwsServiceCache awsservice;

  /**
   * Constructor.
   *
   * @param awsServiceCache service cache
   */
  public EntityRequestHandlerInterceptor(final AwsServiceCache awsServiceCache) {
    this.awsservice = awsServiceCache;
  }

  @Override
  public ApiRequestHandlerResponse afterProcessRequest(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final ApiRequestHandlerResponse response) {
    return response;
  }

  @Override
  public void beforeProcessRequest(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) {
    if (event == null) {
      return;
    }

    authorization.addCacheObject(EntityTypeName.class.getName(), null);
    String entityTypeId = event.getPathParameter("entityTypeId");
    if (entityTypeId != null) {
      resolveExistingEntityType(event, authorization, entityTypeId);
    } else if ("/entityTypes".equals(event.getResource())
        && "post".equalsIgnoreCase(event.getHttpMethod())) {
      authorization.addCacheObject(EntityTypeName.class.getName(), getRequestedEntityType(event));
    }
  }

  private EntityTypeName getRequestedEntityType(final ApiGatewayRequestEvent event) {
    AddEntityTypeRequest request =
        JsonToObject.fromJson(awsservice, event, AddEntityTypeRequest.class);
    if (request == null || request.entityType() == null) {
      throw new BadException("Missing required parameter 'entityType'");
    }
    AddEntityType entityType = request.entityType();
    return new EntityTypeName(entityType.namespace(), entityType.name());
  }

  private void resolveExistingEntityType(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final String requestedId) {
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);
    if (namespace == null && isUuid(requestedId)) {
      namespace = EntityTypeNamespace.CUSTOM;
      event.addQueryParameter("namespace", namespace.name());
    }

    String siteId = authorization.getSiteId();
    String entityTypeId = new EntityTypeIdTransformer(awsservice).apply(siteId, event);
    event.getPathParameters().put("entityTypeId", entityTypeId);

    DynamoDbKey key = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(namespace, EntityTypeNamespace.CUSTOM).nameEmpty().buildKey(siteId);
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    Map<String, AttributeValue> attributes = db.get(key);
    if (attributes.isEmpty()) {
      resolveMissingEntityType(event, authorization);
      return;
    }

    EntityTypeRecord entityType = EntityTypeRecord.fromAttributeMap(attributes);
    authorization.addCacheObject(EntityTypeName.class.getName(),
        new EntityTypeName(entityType.namespace(), entityType.name()));
    event.addQueryParameter("namespace", entityType.namespace().name());
  }

  private void resolveMissingEntityType(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) {
    if ("/entityTypes/{entityTypeId}".equals(event.getResource())
        && "put".equalsIgnoreCase(event.getHttpMethod())
        && "true".equalsIgnoreCase(event.getQueryStringParameter("createIfMissing"))) {
      authorization.addCacheObject(EntityTypeName.class.getName(), getRequestedEntityType(event));
    }
  }
}
