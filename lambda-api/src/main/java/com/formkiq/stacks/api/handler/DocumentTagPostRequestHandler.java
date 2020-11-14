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
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_CREATED;
import java.util.Arrays;
import java.util.Date;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;

/** {@link RequestHandler} for POST "/documents/{documentId}/tags". */
public class DocumentTagPostRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentTagPostRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    DocumentTag tag = fromBodyToObject(logger, event, DocumentTag.class);

    if (tag.getKey() == null || tag.getKey().length() == 0) {
      throw new BadException("invalid json body");
    }

    tag.setType(DocumentTagType.USERDEFINED);
    tag.setInsertedDate(new Date());
    tag.setUserId(getCallingCognitoUsername(event));

    String documentId = event.getPathParameters().get("documentId");
    String siteId = getSiteId(event);
    awsservice.documentService().addTags(siteId, documentId, Arrays.asList(tag));

    ApiResponse resp = new ApiMessageResponse("Created Tag '" + tag.getKey() + "'.");
    return new ApiRequestHandlerResponse(SC_CREATED, resp);
  }

  @Override
  public boolean isReadonly(final String method) {
    return false;
  }
}
