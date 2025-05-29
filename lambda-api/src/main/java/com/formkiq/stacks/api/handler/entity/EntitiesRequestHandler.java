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
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityAttribute;
import com.formkiq.aws.dynamodb.eventsourcing.entity.EntityRecord;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
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
    String tableName = awsservice.environment("DOCUMENTS_TABLE");

    DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
    String entityTypeId = new EntityTypeIdTransformer(awsservice, siteId).apply(event);

    DynamoDbKey key =
        EntityRecord.builder().documentId("").entityTypeId(entityTypeId).name("").buildKey(siteId);

    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).nextToken(event.getQueryStringParameter("next"))
        .limit(event.getQueryStringParameter("limit")).build(tableName);

    QueryResponse response = db.query(q);

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityId").build();

    List<Map<String, Object>> items =
        new AttributeValueListToListMap(config).apply(response.items());

    items.forEach(i -> new AddEntityAttributeTransformer().apply(i));

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

    EntityRecord entity =
        new AddEntityRequestToEntityRecordTransformer(awsservice, authorization, false)
            .apply(event);

    return new ApiRequestHandlerResponse(SC_CREATED,
        new ApiMapResponse(Map.of("entityId", entity.documentId())));
  }
}
