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
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DynamicDocumentItem;

/** {@link RequestHandler} for GET "/documents/{documentId}". */
public class DocumentIdGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentIdGetRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = getSiteId(event);
    String documentId = event.getPathParameters().get("documentId");
    DocumentItem result = awsservice.documentService().findDocument(siteId, documentId, true);

    if (result == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    DynamicDocumentItem item = new DocumentItemToDynamicDocumentItem().apply(result);
    item.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(item));
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
