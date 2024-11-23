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

import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.MapToAction;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.validators.ApiValidator;
import com.formkiq.stacks.dynamodb.DocumentService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/actions". */
public class DocumentsActionsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, ApiValidator {

  /**
   * constructor.
   */
  public DocumentsActionsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    DocumentItem item = getDocument(awsservice, siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    PaginationToAttributeValue pav = new PaginationToAttributeValue();
    Map<String, AttributeValue> startKey = pav.apply(ptoken);

    ActionsService service = awsservice.getExtension(ActionsService.class);
    PaginationResults<Action> results = service.getActions(siteId, documentId, startKey, limit);

    List<Map<String, Object>> list = new ArrayList<>();
    for (Action action : results.getResults()) {
      Map<String, Object> map = new HashMap<>();
      map.put("userId", action.userId());
      map.put("status", action.status().name());
      map.put("type", action.type().name());
      map.put("parameters", action.parameters());
      map.put("metadata", action.metadata());
      map.put("insertedDate", action.insertedDate());
      map.put("startDate", action.startDate());
      map.put("completedDate", action.completedDate());
      map.put("message", action.message());
      map.put("workflowId", action.workflowId());
      map.put("queueId", action.queueId());
      map.put("workflowStepId", action.workflowStepId());

      list.add(map);
    }

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("actions", list);
    map.put("next", current.hasNext() ? current.getNext() : null);
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  private DocumentItem getDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) {
    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    return documentService.findDocument(siteId, documentId);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/actions";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    DocumentItem item = getDocument(awsservice, siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    Map<String, Object> body = fromBodyToMap(event);

    List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("actions");
    List<Action> actions = list.stream().map(new MapToAction()).toList();
    validateActions(awsservice, siteId, actions);

    ActionsService service = awsservice.getExtension(ActionsService.class);
    int idx = service.getActions(siteId, documentId).size();

    for (Action a : actions) {
      service.saveAction(siteId, documentId, a, idx);
      idx++;
    }

    ActionsNotificationService notificationService =
        awsservice.getExtension(ActionsNotificationService.class);
    notificationService.publishNextActionEvent(actions, siteId, documentId);

    ApiMapResponse resp = new ApiMapResponse();
    resp.setMap(Map.of("message", "Actions saved"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }
}
