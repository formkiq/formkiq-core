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
package com.formkiq.stacks.api.handler.indices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.utils.StringUtils;

/** {@link ApiGatewayRequestHandler} for "/indices/search". */
public class IndicesSearchRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public IndicesSearchRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/indices/search";
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    String siteId = authorization.getSiteId();
    boolean access = authorization.getPermissions(siteId).contains(ApiPermission.READ);
    return Optional.of(access);
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);
    int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsservice.getLogger(), event);
    String nextToken = event.getQueryStringParameter("next");

    Map<String, Object> body = JsonToObject.fromJson(awsservice, event, Map.class);

    validatePost(body);

    DocumentSearchService documentSearchService =
        awsservice.getExtension(DocumentSearchService.class);

    String siteId = authorization.getSiteId();
    SearchQuery q = new SearchQueryBuilder()
        .meta(new SearchMetaCriteria(null, null, null, body.get("indexType").toString(), null))
        .build();

    Pagination<DynamicDocumentItem> results =
        documentSearchService.search(siteId, q, null, nextToken, limit);

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getNextToken(), limit);

    List<DynamicDocumentItem> documents = subList(results.getResults(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("values", documents);
    map.put("previous", current.getPrevious());
    map.put("next", current.hasNext() ? current.getNext() : null);

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  private void validatePost(final Map<String, Object> q) throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    if (!q.containsKey("indexType") || StringUtils.isBlank(q.get("indexType").toString())) {
      errors.add(new ValidationErrorImpl().error("invalid body"));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
