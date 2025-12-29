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
package com.formkiq.stacks.api.handler.documents;

import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/actions". */
public class DocumentsActionsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, ApiValidator {

  /**
   * constructor.
   */
  public DocumentsActionsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    DocumentItem item = getDocument(awsservice, siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsservice.getLogger(), event);
    String nextToken = pagination != null ? pagination.getNextToken() : null;

    ActionsService service = awsservice.getExtension(ActionsService.class);
    Pagination<Action> results = service.getActions(siteId, documentId, nextToken, limit);

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
        createPagination(cacheService, event, pagination, results.getNextToken(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("actions", list);
    map.put("next", current.hasNext() ? current.getNext() : null);
    return ApiRequestHandlerResponse.builder().ok().body(map).build();
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
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    DocumentItem item = getDocument(awsservice, siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    AddDocumentActionsRequest body =
        JsonToObject.fromJson(awsservice, event, AddDocumentActionsRequest.class);

    List<Action> actions = body.actions().stream().map(new AddActionsToAction()).toList();

    SiteConfiguration config = awsservice.getExtension(ConfigService.class).get(siteId);
    validateActions(awsservice, config, siteId, actions);

    ActionsService service = awsservice.getExtension(ActionsService.class);
    service.saveNewActions(siteId, documentId, actions);

    ActionsNotificationService notificationService =
        awsservice.getExtension(ActionsNotificationService.class);
    notificationService.publishNextActionEvent(actions, siteId, documentId);

    return ApiRequestHandlerResponse.builder().ok().body("message", "Actions saved").build();
  }
}
