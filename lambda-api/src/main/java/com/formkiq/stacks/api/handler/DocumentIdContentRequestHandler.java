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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.CoreAwsServiceCache;
import com.formkiq.stacks.common.formats.MimeType;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/content". */
public class DocumentIdContentRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentIdContentRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {
    String siteId = authorizer.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    String versionId = getParameter(event, "versionId");

    CoreAwsServiceCache serviceCache = CoreAwsServiceCache.cast(awsservice);

    DocumentItem item = serviceCache.documentService().findDocument(siteId, documentId);

    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    ApiResponse response = null;

    String s3key = createS3Key(siteId, documentId);
    S3Service s3Service = awsservice.getExtension(S3Service.class);

    if (MimeType.isPlainText(item.getContentType())) {

      String content = s3Service.getContentAsString(awsservice.environment("DOCUMENTS_S3_BUCKET"),
          s3key, versionId);

      response = new ApiMapResponse(Map.of("content", content, "contentType", item.getContentType(),
          "isBase64", Boolean.FALSE));

    } else {

      Duration duration = Duration.ofHours(1);
      URL url = s3Service.presignGetUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), s3key,
          duration, versionId);

      response = new ApiMapResponse(Map.of("contentUrl", url.toString(), "contentType",
          item.getContentType() != null ? item.getContentType() : "application/octet-stream"));
    }

    return new ApiRequestHandlerResponse(SC_OK, response);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/content";
  }
}
