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
import com.formkiq.aws.dynamodb.AttributeValueListToListMap;
import com.formkiq.aws.dynamodb.AttributeValueToMapConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.eventsourcing.DynamoDbKey;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityTypeRecord;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/entities/{entityTypeId}". */
public class EntitiesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, DbKeys {

  /**
   * constructor.
   *
   */
  public EntitiesRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");

    if (!Strings.isUuid(entityTypeId)) {
      String namespace = event.getQueryStringParameter("namespace");
      entityTypeId =
          new EntityTypeNameToId().findEntityTypeId(awsservice, siteId, entityTypeId, namespace);
    }

    String tableName = awsservice.environment("DOCUMENTS_TABLE");
    DynamoDbKey key =
        EntityRecord.builder().documentId("").entityTypeId(entityTypeId).name("").buildKey(siteId);

    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).nextToken(event.getQueryStringParameter("next"))
        .limit(event.getQueryStringParameter("limit")).build(tableName);

    QueryResponse response = awsservice.getExtension(DynamoDbService.class).query(q);

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityId").build();
    List<Map<String, Object>> items =
        new AttributeValueListToListMap(config).apply(response.items());
    String nextToken = new MapAttributeValueToString().apply(response.lastEvaluatedKey());

    return ApiRequestHandlerResponse.builder().status(SC_OK).data("entities", items)
        .data("next", nextToken).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entities/{entityTypeId}";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String entityTypeId = event.getPathParameter("entityTypeId");

    AddEntityRequest request = fromBodyToObject(event, AddEntityRequest.class);

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    validate(db, siteId, entityTypeId, request);

    AddEntity addEntity = request.getEntity();

    EntityRecord entity = EntityRecord.builder().documentId(ID.uuid()).name(addEntity.getName())
        .entityTypeId(entityTypeId).build(siteId);

    Map<String, AttributeValue> attributes = entity.getAttributes();
    db.putItem(attributes);

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("entityId", entity.documentId())));
  }

  private void validate(final DynamoDbService db, final String siteId, final String entityTypeId,
      final AddEntityRequest request) throws BadException, ValidationException {

    if (request == null || request.getEntity() == null) {
      throw new BadException("Missing 'entity'");
    }

    ValidationBuilder vb = new ValidationBuilder();
    vb.isRequired("entityTypeId", entityTypeId);
    vb.isRequired("name", request.getEntity().getName());
    vb.check();

    EntityTypeRecord entityTypeRecord =
        EntityTypeRecord.builder().documentId(entityTypeId).namespace("").name("").build(siteId);

    vb.isRequired("entityTypeId", db.exists(entityTypeRecord.key()));
    vb.check();
  }
}
