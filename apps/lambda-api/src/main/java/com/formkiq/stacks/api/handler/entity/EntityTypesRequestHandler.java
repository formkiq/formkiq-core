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
import com.formkiq.aws.dynamodb.entity.EntityTypeNamespace;
import com.formkiq.aws.dynamodb.entity.EntityTypeRecord;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/entityTypes". */
public class EntityTypesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, DbKeys {

  /**
   * constructor.
   *
   */
  public EntityTypesRequestHandler() {}


  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    EntityTypeNamespace namespace = new QueryParameterNamespace().apply(event);

    String tableName = awsservice.environment("DOCUMENTS_TABLE");
    DynamoDbKey key = EntityTypeRecord.builder().documentId("").namespace(namespace).name("")
        .validateNamespace().buildKey(siteId);

    QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI1).pk(key.gsi1Pk())
        .beginsWith(key.gsi1Sk()).nextToken(event.getQueryStringParameter("next"))
        .limit(event.getQueryStringParameter("limit")).build(tableName);

    QueryResponse response = awsservice.getExtension(DynamoDbService.class).query(q);

    AttributeValueToMapConfig config = AttributeValueToMapConfig.builder().removeDbKeys(true)
        .addRenameKeys("documentId", "entityTypeId").build();
    List<Map<String, Object>> items =
        new AttributeValueListToListMap(config).apply(response.items());
    String nextToken = new MapAttributeValueToString().apply(response.lastEvaluatedKey());

    return ApiRequestHandlerResponse.builder().status(SC_OK).body("entityTypes", items)
        .body("next", nextToken).build();
  }

  @Override
  public String getRequestUrl() {
    return "/entityTypes";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();

    EntityTypeRecord entityType =
        new SetEntityTypeRecordFunction(awsservice, false).apply(siteId, event);

    return ApiRequestHandlerResponse.builder().status(SC_CREATED)
        .body("entityTypeId", entityType.documentId()).build();
  }
}
