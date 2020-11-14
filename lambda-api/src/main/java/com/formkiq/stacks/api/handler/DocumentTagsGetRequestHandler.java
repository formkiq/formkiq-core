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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.createPagination;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getLimit;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPagination;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiDocumentTagItemResponse;
import com.formkiq.stacks.api.ApiDocumentTagsItemResponse;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.dynamodb.CacheService;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;

/** {@link RequestHandler} for GET "/documents/{documentId}/tags". */
public class DocumentTagsGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentTagsGetRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    CacheService cacheService = awsservice.documentCacheService();
    ApiPagination pagination = getPagination(cacheService, event);
    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);

    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String siteId = getSiteId(event);
    String documentId = event.getPathParameters().get("documentId");

    PaginationResults<DocumentTag> results =
        awsservice.documentService().findDocumentTags(siteId, documentId, ptoken, limit);

    results.getResults().forEach(r -> r.setDocumentId(null));

    ApiPagination current = createPagination(cacheService, event, pagination, results, limit);
    List<DocumentTag> tags = subList(results.getResults(), limit);

    List<ApiDocumentTagItemResponse> list = tags.stream().map(t -> {
      ApiDocumentTagItemResponse r = new ApiDocumentTagItemResponse();

      r.setDocumentId(t.getDocumentId());
      r.setInsertedDate(t.getInsertedDate());
      r.setKey(t.getKey());
      r.setValue(t.getValue());
      r.setUserId(t.getUserId());
      r.setType(t.getType() != null ? t.getType().name().toLowerCase() : null);

      return r;
    }).collect(Collectors.toList());


    ApiDocumentTagsItemResponse resp = new ApiDocumentTagsItemResponse();
    resp.setTags(list);
    resp.setPrevious(current.getPrevious());
    resp.setNext(current.hasNext() ? current.getNext() : null);

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
