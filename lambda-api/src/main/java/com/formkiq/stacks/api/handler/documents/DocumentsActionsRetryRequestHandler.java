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
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamodbRecordToDynamoDbKeys;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
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

  private Action clone(final String siteId, final Action action) {
    Action na = new Action().getFromAttributes(siteId, action.getAttributes(siteId));
    na.status(ActionStatus.PENDING);
    na.indexUlid();
    na.message(null);
    return na;
  }

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

    actions.stream().filter(a -> ActionStatus.RUNNING.equals(a.status()))
        .forEach(a -> a.status(ActionStatus.FAILED));

    int index = IntStream.range(0, actions.size()).map(i -> actions.size() - 1 - i)
        .filter(i -> ActionStatus.FAILED.equals(actions.get(i).status())).findFirst().orElse(-1);

    String userId = authorization.getUsername();

    if (index >= 0) {

      List<Action> toSave = new ArrayList<>();
      Action ac = actions.get(index);
      ac.status(ActionStatus.FAILED_RETRY);
      toSave.add(ac);

      Action cloned = clone(siteId, ac);
      toSave.add(cloned);

      List<Action> pending = actions.subList(index, actions.size()).stream()
          .filter(a -> ActionStatus.PENDING.equals(a.status())).toList();

      var keysToBeDeleted =
          pending.stream().map(a -> new DynamodbRecordToDynamoDbKeys().apply(siteId, a)).toList();

      for (Action action : pending) {
        Action a = new Action().getFromAttributes(siteId, action.getAttributes(siteId));
        a.indexUlid();
        a.userId(userId);
        a.insertedDate(new Date());
        a.message(null);

        toSave.add(a);
      }

      DynamoDbService db = awsservice.getExtension(DynamoDbService.class);
      db.deleteItems(db.getTableName(), keysToBeDeleted);
      service.saveActions(siteId, toSave);

      ActionsNotificationService notificationService =
          awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId);

      return ApiRequestHandlerResponse.builder().ok().body("message", "Actions retrying").build();
    }

    throw new NotFoundException("Failed action not found");
  }
}
