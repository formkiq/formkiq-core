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
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getPathParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_ACCEPTED;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.ApiMessageResponse;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.api.BadException;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.api.NotImplementedException;
import com.formkiq.stacks.common.formats.MimeFinder;
import com.formkiq.stacks.common.formats.MimeType;
import com.formkiq.stacks.dynamodb.DocumentItem;

/** {@link RequestHandler} for POST "/documents/{documentId}/formats". */
public class DocumentIdFormatsPostRequestHandler implements RequestHandler {

  /** {@link DocumentIdContentGetRequestHandler}. */
  private DocumentIdUrlGetRequestHandler contentHandler;

  /**
   * constructor.
   *
   * @param gethandler {@link DocumentIdUrlGetRequestHandler}
   */
  public DocumentIdFormatsPostRequestHandler(final DocumentIdUrlGetRequestHandler gethandler) {
    this.contentHandler = gethandler;
  }

  /**
   * Build SQS Message.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param userId {@link String}
   * @param versionId {@link String}
   * @param sourceContentType {@link MimeType}
   * @param destinationContentType {@link MimeType}
   * @return {@link Map}
   */
  private Map<String, String> buildMessage(final String siteId, final String documentId,
      final String userId, final String versionId, final MimeType sourceContentType,
      final MimeType destinationContentType) {

    Map<String, String> map = new HashMap<>();
    map.put("siteId", siteId);
    map.put("documentId", documentId);
    map.put("sourceContentType", sourceContentType.getContentType());
    map.put("destinationContentType", destinationContentType.getContentType());
    map.put("userId", userId);

    if (versionId != null) {
      map.put("versionId", versionId);
    }
    return map;
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    Map<String, String> q = fromBodyToObject(logger, event, Map.class);

    if (q == null || !q.containsKey("mime")) {
      throw new BadException("Invalid JSON body.");
    }

    MimeType destinationContentType = MimeFinder.find(q.get("mime"));
    event.addHeader("Content-Type", destinationContentType.getContentType());

    if (awsservice.debug()) {
      logger.log("Setting 'Content-Type' header to " + destinationContentType.getContentType());
    }

    ApiRequestHandlerResponse response =
        this.contentHandler.process(logger, event, authorizer, awsservice);

    if (response.getResponse() instanceof ApiUrlResponse) {
      ApiUrlResponse apiresponse = (ApiUrlResponse) response.getResponse();
      String documentId = getPathParameter(event, "documentId");
      response = new ApiRequestHandlerResponse(ApiResponseStatus.SC_OK,
          new ApiMapResponse(Map.of("documentId", documentId, "url", apiresponse.getUrl())));

    } else {

      final String userId = getCallingCognitoUsername(event);
      final String siteId = getSiteId(event);
      final String documentId = event.getPathParameters().get("documentId");
      final String versionId = getParameter(event, "versionId");
      DocumentItem item = awsservice.documentService().findDocument(getSiteId(event), documentId);

      if (item == null) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      MimeType sourceContentType = MimeFinder.find(item.getContentType());

      if (awsservice.debug()) {
        logger.log("SiteId: " + siteId + " DocumentId: " + documentId + " has Content-Type "
            + item.getContentType());
        logger.log("converting " + sourceContentType + " to " + destinationContentType);
      }

      if (!MimeFinder.isSupported(sourceContentType, destinationContentType)) {
        throw new BadException("Unsupported Format");
      }

      Map<String, String> map = buildMessage(siteId, documentId, userId, versionId,
          sourceContentType, destinationContentType);

      String ssmkey = "/formkiq/" + awsservice.appEnvironment() + "/sqs/DocumentsFormatsUrl";
      String sqsQueueUrl = awsservice.ssmService().getParameterValue(ssmkey);
      if (sqsQueueUrl == null) {
        throw new NotImplementedException("Document format conversions is not installed");
      }

      awsservice.sqsService().sendMessage(sqsQueueUrl, GSON.toJson(map));

      response = new ApiRequestHandlerResponse(SC_ACCEPTED,
          new ApiMessageResponse("Sent for Document format conversion processing"));
    }

    return response;
  }
}
