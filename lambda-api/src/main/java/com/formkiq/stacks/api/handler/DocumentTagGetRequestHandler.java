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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiDocumentTagItemResponse;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** {@link RequestHandler} for GET "/documents/{documentId}/tags/{tagKey}". */
public class DocumentTagGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentTagGetRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String tagKey = event.getPathParameters().get("tagKey");
    String siteId = getSiteId(event);

    DocumentTag docTag = awsservice.documentService().findDocumentTag(siteId, documentId, tagKey);
    if (docTag == null) {
      throw new NotFoundException("Tag " + tagKey + " not found.");
    }

    ApiDocumentTagItemResponse resp = new ApiDocumentTagItemResponse();
    resp.setKey(tagKey);
    resp.setValue(docTag.getValue());
    resp.setInsertedDate(docTag.getInsertedDate());
    resp.setUserId(docTag.getUserId());
    resp.setType(docTag.getType() != null ? docTag.getType().name().toLowerCase() : null);
    resp.setDocumentId(docTag.getDocumentId());

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
