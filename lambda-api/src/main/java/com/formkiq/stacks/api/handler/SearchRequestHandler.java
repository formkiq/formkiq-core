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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_PAYMENT;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.AwsServiceCache;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheService;
import com.formkiq.stacks.api.CoreAwsServiceCache;
import com.formkiq.stacks.api.QueryRequest;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import software.amazon.awssdk.utils.StringUtils;

/** {@link ApiGatewayRequestHandler} for "/search". */
public class SearchRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Maximum number of Document Ids that can be sent. */
  private static final int MAX_DOCUMENT_IDS = 100;

  /**
   * constructor.
   *
   */
  public SearchRequestHandler() {}

  /**
   * Is {@link QueryRequest} valid.
   * 
   * @param q {@link QueryRequest}
   * @throws BadException BadException
   */
  private void checkIsRequestValid(final QueryRequest q) throws BadException {
    if (q == null || q.query() == null || (q.query().tag() == null && q.query().tags() == null)) {
      throw new BadException("Invalid JSON body.");
    }

    if (q.query().tag() != null) {
      if (StringUtils.isEmpty(q.query().tag().key())) {
        throw new BadException("'tag' attribute is required.");
      }
    }

    for (SearchTagCriteria tag : Objects.notNull(q.query().tags())) {
      if (StringUtils.isEmpty(tag.key())) {
        throw new BadException("'tag' attribute is required.");
      }
    }
  }

  @Override
  public String getRequestUrl() {
    return "/search";
  }

  @Override
  public boolean isReadonly(final String method) {
    return "post".equals(method) || "get".equals(method) || "head".equals(method);
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    ApiRequestHandlerResponse response = null;
    CoreAwsServiceCache serviceCache = CoreAwsServiceCache.cast(awsservice);

    QueryRequest q = fromBodyToObject(logger, event, QueryRequest.class);

    checkIsRequestValid(q);

    DocumentSearchService documentSearchService = serviceCache.documentSearchService();

    if (q.query().tag() == null && !serviceCache.documentTagSchemaPlugin().isActive()) {
      ApiMapResponse resp = new ApiMapResponse();
      resp.setMap(Map.of("message", "Feature only available in FormKiQ Enterprise"));
      response = new ApiRequestHandlerResponse(SC_PAYMENT, resp);

    } else {

      DynamoDbCacheService cacheService = awsservice.documentCacheService();
      ApiPagination pagination = getPagination(cacheService, event);
      int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
      PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

      Collection<String> documentIds = q.query().documentIds();
      if (documentIds != null) {
        if (documentIds.size() > MAX_DOCUMENT_IDS) {
          throw new BadException("Maximum number of DocumentIds is " + MAX_DOCUMENT_IDS);
        }

        if (!getQueryParameterMap(event).containsKey("limit")) {
          limit = documentIds.size();
        }
      }

      String siteId = authorizer.getSiteId();
      PaginationResults<DynamicDocumentItem> results =
          documentSearchService.search(siteId, q.query(), ptoken, limit);

      ApiPagination current =
          createPagination(cacheService, event, pagination, results.getToken(), limit);

      List<DynamicDocumentItem> documents = subList(results.getResults(), limit);

      Map<String, Object> map = new HashMap<>();
      map.put("documents", documents);
      map.put("previous", current.getPrevious());
      map.put("next", current.hasNext() ? current.getNext() : null);

      ApiMapResponse resp = new ApiMapResponse(map);
      response = new ApiRequestHandlerResponse(SC_OK, resp);
    }

    return response;
  }
}
