/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiPagination;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.api.QueryRequest;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamoDbCacheService;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;

/** {@link ApiGatewayRequestHandler} for "/search". */
public class SearchRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public SearchRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    DynamoDbCacheService cacheService = awsservice.documentCacheService();
    ApiPagination pagination = getPagination(cacheService, event);
    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    QueryRequest q = fromBodyToObject(logger, event, QueryRequest.class);

    if (q == null || q.getQuery() == null || q.getQuery().getTag() == null) {
      throw new BadException("Invalid JSON body.");
    }

    String siteId = getSiteId(event);
    PaginationResults<DynamicDocumentItem> results =
        awsservice.documentSearchService().search(siteId, q.getQuery().getTag(), ptoken, limit);

    ApiPagination current = createPagination(cacheService, event, pagination, results, limit);

    List<DynamicDocumentItem> documents = subList(results.getResults(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("documents", documents);
    map.put("previous", current.getPrevious());
    map.put("next", current.hasNext() ? current.getNext() : null);

    ApiMapResponse resp = new ApiMapResponse(map);
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return "post".equals(method) || "get".equals(method) || "head".equals(method);
  }

  @Override
  public String getRequestUrl() {
    return "/search";
  }
}
