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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** {@link RequestHandler} for PUT "/documents/{documentId}/tags/{tag}". */
public class DocumentTagPutRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentTagPutRequestHandler() {}

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    Map<String, String> map = event.getPathParameters();
    final String documentId = map.get("documentId");
    String tagKey = map.get("tagKey");

    Map<String, String> body = fromBodyToObject(logger, event, Map.class);
    String value = body != null ? body.getOrDefault("value", null) : null;

    if (value == null) {
      throw new BadException("request body is invalid");
    }

    String siteId = getSiteId(event);
    DocumentService documentService = awsservice.documentService();

    if (event.getHttpMethod().equalsIgnoreCase("put")) {
      if (documentService.findDocument(siteId, documentId) == null) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }
    }

    Date now = new Date();
    String userId = getCallingCognitoUsername(event);

    if (documentService.findDocumentTag(siteId, documentId, tagKey) == null) {
      throw new NotFoundException("Tag " + tagKey + " not found.");
    }

    DocumentTag tag = new DocumentTag(null, tagKey, value, now, userId);

    documentService.addTags(siteId, documentId, Arrays.asList(tag));

    ApiResponse resp = new ApiMessageResponse(
        "Updated tag '" + tagKey + "' to '" + value + "' for document '" + documentId + "'.");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }
}
