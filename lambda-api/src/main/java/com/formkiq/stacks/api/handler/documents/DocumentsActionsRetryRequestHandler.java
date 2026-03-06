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
import java.util.List;
import java.util.stream.IntStream;

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.services.ActionStatusPredicate;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/actions/retry". */
public class DocumentsActionsRetryRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentsActionsRetryRequestHandler() {}

  private DocumentItem getDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) {
    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    return documentService.findDocument(siteId, documentId);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/actions/retry";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    DocumentItem item = getDocument(awsservice, siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    ActionsService service = awsservice.getExtension(ActionsService.class);
    List<Action> actions = service.getActions(siteId, documentId);

    var toUpdatePred = new ActionStatusPredicate(ActionStatus.RUNNING,
        ActionStatus.MAX_RETRIES_REACHED, ActionStatus.WAITING_FOR_RETRY);
    var failedPred = new ActionStatusPredicate(ActionStatus.FAILED);

    List<Action> toUpdate = new ArrayList<>();
    List<Action> failedActions = new ArrayList<>();

    actions.forEach(a -> {
      if (toUpdatePred.test(a)) {
        var action = new ActionBuilder().action(a).status(ActionStatus.FAILED).build(siteId);
        toUpdate.add(action);
        failedActions.add(action);
      } else if (failedPred.test(a)) {
        failedActions.add(a);
      }
    });

    service.updateActions(toUpdate);

    int index = IntStream.range(0, failedActions.size()).map(i -> failedActions.size() - 1 - i)
        .filter(i -> ActionStatus.FAILED.equals(failedActions.get(i).status())).findFirst()
        .orElse(-1);

    String userId = authorization.getUsername();

    if (index >= 0) {

      List<Action> toSave = new ArrayList<>();
      Action action = failedActions.get(index);
      Action ac = new ActionBuilder().action(action).status(ActionStatus.FAILED_RETRY)
          .userId(userId).build(siteId);
      service.updateAction(ac);

      Action aa = new ActionBuilder().action(action).status(ActionStatus.PENDING).indexUlid()
          .userId(userId).message(null).build(siteId);
      toSave.add(aa);

      List<Action> pending =
          actions.stream().filter(a -> ActionStatus.PENDING.equals(a.status())).toList();
      service.deleteActions(pending);

      pending.stream()
          .map(a -> new ActionBuilder().action(a).userId(userId).indexUlid().build(siteId))
          .forEach(toSave::add);

      service.saveActions(siteId, toSave);

      var notificationService = awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId);

      return ApiRequestHandlerResponse.builder().ok().body("message", "Actions retrying").build();
    }

    throw new NotFoundException("Failed action not found");
  }
}
