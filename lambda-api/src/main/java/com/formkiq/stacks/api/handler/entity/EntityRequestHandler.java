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
import com.formkiq.aws.dynamodb.AttributeValueToMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.dynamodb.useractivities.AttributeValuesToChangeRecordFunction;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/entities/{entityTypeId}/{entityId}". */
public class EntityRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public EntityRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityId = event.getPathParameter("entityId");
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    String entityTypeId = event.getPathParameter("entityTypeId");

    EntityRecord.Builder builder =
        EntityRecord.builder().documentId(entityId).entityTypeId(entityTypeId).name("");

    Map<String, AttributeValue> attributes = db.get(builder.buildKey(siteId));

    if (attributes.isEmpty()) {
      throw new NotFoundException("entity '" + entityId + "' not found");
    }

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityId").build();

    Map<String, Object> values = new AttributeValueToMap(config).apply(attributes);
    new AddEntityAttributeTransformer().apply(values);

    return ApiRequestHandlerResponse.builder().status(SC_OK).body("entity", values).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entities/{entityTypeId}/{entityId}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    new AddEntityRequestToEntityRecordTransformer(awsservice, authorization, true).apply(event);

    return ApiRequestHandlerResponse.builder().ok().body("message", "Entity updated").build();
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    String entityId = event.getPathParameter("entityId");

    DynamoDbKey key = EntityRecord.builder().documentId(entityId).entityTypeId(entityTypeId)
        .name("").build(siteId).key();

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    Map<String, AttributeValue> attributes = db.get(key);
    if (attributes.isEmpty()) {
      throw new NotFoundException("entity '" + entityTypeId + "' not found");
    }

    UserActivityContext
        .set(new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityId"))
            .apply(attributes, null));

    return ApiRequestHandlerResponse.builder().status(SC_OK).body("message", "Entity deleted")
        .build();
  }
}
