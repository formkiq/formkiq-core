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

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getParameter;
import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getSiteId;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createS3Key;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.stacks.api.ApiAuthorizer;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;
import com.formkiq.stacks.api.ApiMapResponse;
import com.formkiq.stacks.api.ApiResponse;
import com.formkiq.stacks.api.NotFoundException;
import com.formkiq.stacks.dynamodb.DocumentItem;
import software.amazon.awssdk.services.s3.S3Client;

/** {@link RequestHandler} for GET "/documents/{documentId}/content". */
public class DocumentIdContentGetRequestHandler implements RequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentIdContentGetRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse process(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = getSiteId(event);
    String documentId = event.getPathParameters().get("documentId");
    String versionId = getParameter(event, "versionId");

    DocumentItem item = awsservice.documentService().findDocument(getSiteId(event), documentId);

    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    ApiResponse response = null;

    String s3key = createS3Key(siteId, documentId);

    if (isPlainText(item.getContentType())) {

      try (S3Client s3 = awsservice.s3Service().buildClient()) {

        String content = awsservice.s3Service().getContentAsString(s3,
            awsservice.documents3bucket(), s3key, versionId);

        response = new ApiMapResponse(Map.of("content", content, "contentType",
            item.getContentType(), "isBase64", Boolean.FALSE));
      }

    } else {

      Duration duration = Duration.ofHours(1);
      URL url = awsservice.s3Service().presignGetUrl(awsservice.documents3bucket(), s3key, duration,
          versionId);

      response = new ApiMapResponse(Map.of("contentUrl", url.toString(), "contentType",
          item.getContentType() != null ? item.getContentType() : "application/octet-stream"));
    }

    return new ApiRequestHandlerResponse(SC_OK, response);
  }

  /**
   * Is Content Type plain text.
   * 
   * @param contentType {@link String}
   * @return boolean
   */
  private boolean isPlainText(final String contentType) {
    return contentType != null
        && (contentType.startsWith("text/") || "application/json".equals(contentType));
  }

  @Override
  public boolean isReadonly(final String method) {
    return true;
  }
}
