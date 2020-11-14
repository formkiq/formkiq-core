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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getContentType;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiEmptyResponse;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItem;

/** {@link RequestHandler} for GET "/documents/{documentId}/url". */
public class DocumentIdUrlGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentIdUrlGetRequestHandler() {}

  /**
   * Look at {@link ApiGatewayRequestEvent} for "duration".
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return int
   */
  private int getDurationHours(final ApiGatewayRequestEvent event) {
    final int defaultDurationHours = 48;

    Map<String, String> map =
        event.getQueryStringParameters() != null ? event.getQueryStringParameters()
            : new HashMap<>();
    String durationHours = map.getOrDefault("duration", "" + defaultDurationHours);

    try {
      return Integer.parseInt(durationHours);
    } catch (NumberFormatException e) {
      return defaultDurationHours;
    }
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String versionId = getParameter(event, "versionId");

    DocumentItem item = awsservice.documentService().findDocument(getSiteId(event), documentId);

    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    URL url = getS3Url(logger, awsservice, event, item, documentId, versionId, awsservice);

    return url != null
        ? new ApiRequestHandlerResponse(SC_OK, new ApiUrlResponse(url.toString(), documentId))
        : new ApiRequestHandlerResponse(SC_NOT_FOUND, new ApiEmptyResponse());
  }

  /**
   * Get S3 URL.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservices {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param documentId {@link String}
   * @param item {@link DocumentItem}
   * @param versionId {@link String}
   * @param awsservice {@link AwsServiceCache}
   * @return {@link URL}
   */
  private URL getS3Url(final LambdaLogger logger, final AwsServiceCache awsservices,
      final ApiGatewayRequestEvent event, final DocumentItem item, final String documentId,
      final String versionId, final AwsServiceCache awsservice) {

    URL url = null;
    String contentType = getContentType(event);
    String siteId = getSiteId(event);
    int hours = getDurationHours(event);
    Duration duration = Duration.ofHours(hours);

    if (awsservice.debug()) {
      logger.log("Finding S3 Url for 'Content-Type' " + contentType);
    }

    if (contentType == null || contentType.equals(item.getContentType())) {

      logger.log("Found default format " + contentType + " for siteId: " + siteId + " documentId: "
          + documentId);

      String s3key = createS3Key(siteId, documentId);
      url = awsservice.s3Service().presignGetUrl(awsservices.documents3bucket(), s3key, duration,
          versionId);

    } else {

      Optional<DocumentFormat> format =
          awsservice.documentService().findDocumentFormat(siteId, documentId, contentType);

      if (format.isPresent()) {

        if (awsservice.debug()) {
          logger.log("Found format " + contentType + " for siteId: " + siteId + " documentId: "
              + documentId);
        }

        String s3key = createS3Key(siteId, documentId, contentType);
        url = awsservice.s3Service().presignGetUrl(awsservices.documents3bucket(), s3key, duration,
            versionId);

      } else if (awsservice.debug()) {

        logger.log("Cannot find format " + contentType + " for siteId: " + siteId + " documentId: "
            + documentId);
      }
    }

    return url;
  }
}
