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
package com.formkiq.stacks.api.handler.sites;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequence;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequenceService;

/** Handler for {@code /sites/{siteId}/numberingSequences}. */
public class NumberingSequencesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Constructor. */
  public NumberingSequencesRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    String siteId = getPathParameterSiteId(event);
    CacheService cacheService = awsServices.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);
    String nextToken = pagination != null ? pagination.getNextToken() : null;
    int limit = getLimit(awsServices.getLogger(), event);

    NumberingSequenceService service = awsServices.getExtension(NumberingSequenceService.class);
    Pagination<NumberingSequence> results = service.findAll(siteId, nextToken, limit);
    List<Map<String, Object>> sequences =
        results.getResults().stream().map(NumberingSequence::toMap).toList();

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getNextToken(), limit);
    Map<String, Object> response = new HashMap<>();
    response.put("numberingSequences", sequences);
    if (current.hasNext()) {
      response.put("next", current.getNext());
    }
    return ApiRequestHandlerResponse.builder().ok().body(response).build();
  }

  @Override
  public String getRequestUrl() {
    return "/sites/{siteId}/numberingSequences";
  }
}
