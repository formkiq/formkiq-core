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
package com.formkiq.stacks.api.handler;

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.api.ApiEmptyResponse;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItem;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/url". */
public class DocumentIdUrlRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentIdUrlRequestHandler() {}

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
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
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

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/url";
  }
}
