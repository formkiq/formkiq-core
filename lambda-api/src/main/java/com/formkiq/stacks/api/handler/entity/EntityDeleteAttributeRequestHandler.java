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
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/**
 * {@link ApiGatewayRequestHandler} for
 * "/entities/{entityTypeId}/{entityId}/attributes/{attributeKey}".
 */
public class EntityDeleteAttributeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public EntityDeleteAttributeRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();

    final String entityTypeId = event.getPathParameter("entityTypeId");
    final String entityId = event.getPathParameter("entityId");
    final String attributeKey = event.getPathParameter("attributeKey");

    DynamoDbKey key = EntityRecord.builder().documentId(entityId).entityTypeId(entityTypeId)
        .name("").build(siteId).key();

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    Map<String, AttributeValue> previous = db.get(key);

    if (previous.isEmpty()) {
      throw new NotFoundException("entity '" + entityId + "' not found");
    }

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId).name("")
        .namespace(EntityTypeNamespace.CUSTOM).buildKey(siteId);

    Map<String, AttributeValue> entityTypeAttributes = db.get(entityTypeKey);
    if (entityTypeAttributes.isEmpty()) {
      throw new NotFoundException("entityType '" + entityTypeId + "' in namespace "
          + EntityTypeNamespace.CUSTOM + " not found");
    }

    EntityTypeRecord entityTypeRecord = EntityTypeRecord.fromAttributeMap(entityTypeAttributes);
    if (EntityTypeNamespace.PRESET.equals(entityTypeRecord.namespace())) {
      throw new BadException(
          EntityTypeNamespace.PRESET + " Entities cannot have attributes removed");
    }

      Map<String, AttributeValue> attributes = new HashMap<>(db.get(key));
    boolean removed =
        attributes.entrySet().removeIf(e -> e.getKey().startsWith("attr#" + attributeKey));

    if (!removed) {
      throw new NotFoundException(
          "attribute " + attributeKey + " on entity '" + entityId + "' not found");
    }

    db.putItem(attributes);

     UserActivityContext.setUpdate(ActivityResourceType.ENTITY, previous, attributes);

    return ApiRequestHandlerResponse.builder().status(SC_OK).body("message", "Entity deleted")
        .build();
  }

  @Override
  public String getRequestUrl() {
    return "/entities/{entityTypeId}/{entityId}/attributes/{attributeKey}";
  }
}
