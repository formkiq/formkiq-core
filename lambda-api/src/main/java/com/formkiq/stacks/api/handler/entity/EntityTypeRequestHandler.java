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
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.eventsourcing.DynamoDbKey;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/entityTypes/{entityTypeId}". */
public class EntityTypeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public EntityTypeRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);

    EntityTypeRecord.Builder builder =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace("").name("");

    if (!Strings.isUuid(entityTypeId)) {
      updateBuilderFromEntityName(event, awsservice, builder, siteId);
    }

    Map<String, AttributeValue> attributes = db.get(builder.buildKey(siteId));

    if (attributes.isEmpty()) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityTypeId").build();
    Map<String, Object> values = new AttributeValueToMap(config).apply(attributes);
    return ApiRequestHandlerResponse.builder().status(SC_OK).data("entityType", values).build();
  }

  private void updateBuilderFromEntityName(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final EntityTypeRecord.Builder builder, final String siteId)
      throws ValidationException {
    String entityTypeId = event.getPathParameter("entityTypeId");
    String namespace = event.getQueryStringParameter("namespace");
    validateNamespace(namespace);

    String tableName = awsservice.environment("DOCUMENTS_TABLE");
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);

    DynamoDbKey key = builder.name(entityTypeId).namespace(namespace).buildKey(siteId);
    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).limit("1").build(tableName);
    QueryResponse response = db.query(q);
    if (!response.items().isEmpty()) {
      builder.documentId(response.items().get(0).get("documentId").s());
    }
  }

  private void validateNamespace(final String namespace) throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("namespace", namespace);
    vb.check();

    vb.isEquals("namespace", namespace, "CUSTOM");
    vb.check();
  }

  @Override
  public String getRequestUrl() {
    return "/entityTypes/{entityTypeId}";
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");

    validateDelete(awsservice, siteId, entityTypeId);

    DynamoDbKey entityTypeKey = EntityTypeRecord.builder().documentId(entityTypeId).namespace("")
        .name("").build(siteId).key();

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    if (!db.deleteItem(entityTypeKey)) {
      throw new NotFoundException("entityType '" + entityTypeId + "' not found");
    }

    return ApiRequestHandlerResponse.builder().status(SC_OK).data("message", "EntityType deleted")
        .build();
  }

  private void validateDelete(final AwsServiceCache awsservice, final String siteId,
      final String entityTypeId) throws ValidationException {
    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    EntityRecord entity =
        EntityRecord.builder().entityTypeId(entityTypeId).documentId("").name("").build(siteId);
    DynamoDbKey key = entity.key();

    String tableName = awsservice.environment("DOCUMENTS_TABLE");
    QueryRequest q = DynamoDbQueryBuilder.builder().projectionExpression(PK).indexName(GSI1)
        .pk(key.gsi1Pk()).beginsWith(key.gsi1Sk()).limit("1").build(tableName);

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityId", !db.exists(q), "Entities attached to Entity type");
    vb.check();
  }
}
