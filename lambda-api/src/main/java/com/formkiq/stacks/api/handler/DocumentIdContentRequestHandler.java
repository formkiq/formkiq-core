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
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityPlugin;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentVersionService;

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
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    String versionKey = getParameter(event, "versionKey");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentItem item = documentService.findDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
    DynamoDbConnectionBuilder connection = awsservice.getExtension(DynamoDbConnectionBuilder.class);
    String versionId = versionService.getVersionId(connection, siteId, documentId, versionKey);

    if (versionKey != null) {
      throwIfNull(versionId, new BadException("invalid 'versionKey'"));
    }

    ApiResponse response = null;

    String s3key = createS3Key(siteId, documentId);

    if (MimeType.isPlainText(item.getContentType())) {

      S3Service s3Service = awsservice.getExtension(S3Service.class);
      String content = s3Service.getContentAsString(awsservice.environment("DOCUMENTS_S3_BUCKET"),
          s3key, versionId);

      response = new ApiMapResponse(Map.of("content", content, "contentType", item.getContentType(),
          "isBase64", Boolean.FALSE));

    } else {

      String contentType =
          item.getContentType() != null ? item.getContentType() : "application/octet-stream";

      PresignGetUrlConfig config =
          new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false)
              .contentType(s3key).contentType(contentType);

      S3PresignerService s3Service = awsservice.getExtension(S3PresignerService.class);
      Duration duration = Duration.ofHours(1);
      URL url = s3Service.presignGetUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), s3key,
          duration, versionId, config);

      response =
          new ApiMapResponse(Map.of("contentUrl", url.toString(), "contentType", contentType));
    }

    if (awsservice.containsExtension(UserActivityPlugin.class)) {
      UserActivityPlugin plugin = awsservice.getExtension(UserActivityPlugin.class);
      plugin.addViewActivity(siteId, documentId, versionKey, authorization.getUsername());
    }

    return new ApiRequestHandlerResponse(SC_OK, response);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/content";
  }
}
