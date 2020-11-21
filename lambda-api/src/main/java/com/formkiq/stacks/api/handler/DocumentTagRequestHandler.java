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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.ApiResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.api.ApiDocumentTagItemResponse;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/tags/{tagKey}". */
public class DocumentTagRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentTagRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
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

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
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
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
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
  public String getRequestUrl() {
    return "/documents/{documentId}/tags/{tagKey}";
  }
}
