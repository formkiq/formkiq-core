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
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.entity.EntityRecord;
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.useractivities.ActivityResourceType;
import com.formkiq.aws.dynamodb.useractivities.AttributeValuesToChangeRecordFunction;
import com.formkiq.aws.dynamodb.useractivities.ChangeRecord;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.Collections;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.PK;

/** {@link ApiGatewayRequestHandler} for "/entityTypes/{entityTypeId}". */
public class EntityTypeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public EntityTypeRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);

    Map<String, AttributeValue> attributes =
        validateDelete(awsservice, siteId, entityTypeId, namespace);

    Map<String, ChangeRecord> changes =
        new AttributeValuesToChangeRecordFunction(Map.of("documentId", "entityTypeId"))
            .apply(attributes, null);
    UserActivityContext.set(ActivityResourceType.ENTITY_TYPE, UserActivityType.DELETE, changes,
        Collections.emptyMap());

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(namespace, EntityTypeNamespace.CUSTOM).name("").buildKey(siteId);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    if (!db.deleteItem(entityTypeKey)) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    return ApiRequestHandlerResponse.builder().ok().body("message", "EntityType deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);

    EntityTypeRecord.Builder builder =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace(namespace).name("");

    Map<String, AttributeValue> attributes = db.get(builder.buildKey(siteId));

    if (attributes.isEmpty()) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityTypeId").build();
    Map<String, Object> values = new AttributeValueToMap(config).apply(attributes);
    return ApiRequestHandlerResponse.builder().ok().body("entityType", values).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entityTypes/{entityTypeId}";
  }

  private Map<String, AttributeValue> validateDelete(final AwsServiceCache awsservice,
      final String siteId, final String entityTypeId, final EntityTypeNamespace namespace)
      throws ValidationException {

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    String tableName = awsservice.environment("DOCUMENTS_TABLE");

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId)
        .namespace(namespace, EntityTypeNamespace.CUSTOM).nameEmpty().buildKey(siteId);

    Map<String, AttributeValue> entityType = db.get(entityTypeKey);

    EntityRecord entity =
        EntityRecord.builder().entityTypeId(entityTypeId).documentId("").name("").build(siteId);
    DynamoDbKey entityKey = entity.key();

    QueryRequest q = DynamoDbQueryBuilder.builder().projectionExpression(PK).indexName(GSI1)
        .pk(entityKey.gsi1Pk()).beginsWith(entityKey.gsi1Sk()).limit("1").build(tableName);

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityTypeId", !entityType.isEmpty(), "EntityType not found");
    vb.isRequired("entityId", !db.exists(q), "Entities attached to Entity type");
    vb.check();

    return entityType;
  }
}
