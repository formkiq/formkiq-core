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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamodbRecordToMap;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.handler.AddDocumentSync;
import com.formkiq.stacks.api.handler.AddDocumentSyncRequest;
import com.formkiq.stacks.api.handler.AddDocumentSyncServiceType;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/syncs". */
public class DocumentsSyncsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentsSyncsRequestHandler() {}

  private static void validate(final AddDocumentSync sync) throws BadException {
    if (sync == null) {
      throw new BadException("Invalid request");
    }

    if (sync.getService() == null) {
      throw new BadException("Invalid Sync Service");
    }

    if (sync.getType() == null) {
      throw new BadException("Invalid Sync Type");
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);

    final int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsservice.getLogger(), event);
    final PaginationMapToken token = pagination != null ? pagination.getStartkey() : null;

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    DocumentSyncService sync = awsservice.getExtension(DocumentSyncService.class);
    PaginationResults<DocumentSyncRecord> syncs = sync.getSyncs(siteId, documentId, token, limit);

    syncs.getResults().forEach(s -> s.setDocumentId(null));
    List<Map<String, Object>> list =
        syncs.getResults().stream().map(new DynamodbRecordToMap()).toList();

    if (list.isEmpty()) {
      verifyDocument(awsservice, siteId, documentId);
    }

    ApiPagination current =
        createPagination(cacheService, event, pagination, syncs.getToken(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("syncs", list);
    map.put("previous", current.getPrevious());
    map.put("next", current.hasNext() ? current.getNext() : null);

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/syncs";
  }

  private void verifyDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) throws Exception {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    DocumentItem item = ds.findDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    AddDocumentSyncRequest req = fromBodyToObject(event, AddDocumentSyncRequest.class);
    AddDocumentSync sync = req.getSync();
    validate(sync);

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    verifyDocument(awsservice, siteId, documentId);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);

    if (DocumentSyncType.CONTENT.equals(sync.getType()) && isActionService(sync.getService())) {

      actionsService.saveNewActions(siteId, documentId, List.of(new Action().documentId(documentId)
          .type(ActionType.FULLTEXT).userId(authorization.getUsername())));

    } else {
      DocumentSyncService service = awsservice.getExtension(DocumentSyncService.class);

      DocumentSyncServiceType serviceType = getService(awsservice, sync.getService());
      Collection<ValidationError> errors =
          service.addSync(siteId, documentId, serviceType, sync.getType());
      if (!errors.isEmpty()) {
        throw new ValidationException(errors);
      }
    }

    return ApiRequestHandlerResponse.builder().created().body("message", "Added Document sync")
        .build();
  }

  private DocumentSyncServiceType getService(final AwsServiceCache awsservice,
      final AddDocumentSyncServiceType service) throws BadException {
    return switch (service) {
      case FULLTEXT -> {
        if (awsservice.hasModule("opensearch")) {
          yield DocumentSyncServiceType.OPENSEARCH;
        } else if (awsservice.hasModule("typesense")) {
          yield DocumentSyncServiceType.TYPESENSE;
        } else {
          throw new BadException("No fulltext services enabled");
        }
      }
      default -> throw new BadException("Unknown service type: " + service);
    };
  }

  private boolean isActionService(final AddDocumentSyncServiceType service) {
    return AddDocumentSyncServiceType.FULLTEXT.equals(service);
  }
}
