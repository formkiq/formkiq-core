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
package com.formkiq.stacks.api.handler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.schemas.ClassificationRecord;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for "/sites/{siteId}/classifications". */
public class SitesClassificationRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link Gson}. */
  private final Gson gson = GsonUtil.getInstance();

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    CacheService cacheService = awsServices.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);
    PaginationMapToken token = pagination != null ? pagination.getStartkey() : null;
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    int limit = getLimit(logger, event);
    String siteId = authorization.getSiteId();

    SchemaService service = awsServices.getExtension(SchemaService.class);
    PaginationResults<ClassificationRecord> results =
        service.findAllClassifications(siteId, startkey, limit);

    List<?> data =
        results
            .getResults().stream().map(c -> Map.of("classificationId", c.getDocumentId(), "name",
                c.getName(), "userId", c.getUserId(), "insertedDate", c.getInsertedDate()))
            .toList();

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    Map<String, Object> m = new HashMap<>();
    m.put("classifications", data);

    if (current.hasNext()) {
      m.put("next", current.getNext());
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(m));
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/classifications";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorizer,
      final AwsServiceCache awsServices) throws Exception {

    SchemaService service = awsServices.getExtension(SchemaService.class);

    String siteId = authorizer.getSiteId();

    AddClassificationRequest request = fromBodyToObject(event, AddClassificationRequest.class);
    Schema schema = request.getClassification();
    ClassificationRecord classification =
        service.setClassification(siteId, null, schema.getName(), schema, authorizer.getUsername());

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("classificationId", classification.getDocumentId())));
  }
}
