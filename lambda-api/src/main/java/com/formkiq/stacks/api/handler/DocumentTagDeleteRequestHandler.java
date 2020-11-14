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
import java.util.Arrays;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** {@link RequestHandler} for DELETE "/documents/{documentId}/tags/{tag}". */
public class DocumentTagDeleteRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentTagDeleteRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = getSiteId(event);
    Map<String, String> map = event.getPathParameters();
    String documentId = map.get("documentId");
    String tagKey = map.get("tagKey");
    DocumentService documentService = awsservice.documentService();

    DocumentTag docTag = documentService.findDocumentTag(siteId, documentId, tagKey);
    if (docTag == null) {
      throw new NotFoundException("Tag '" + tagKey + "' not found.");
    }

    documentService.removeTags(siteId, documentId, Arrays.asList(tagKey));

    ApiResponse resp =
        new ApiMessageResponse("Removed '" + tagKey + "' from document '" + documentId + "'.");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }
}
