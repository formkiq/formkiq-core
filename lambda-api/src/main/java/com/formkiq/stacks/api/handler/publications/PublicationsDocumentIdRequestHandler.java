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
package com.formkiq.stacks.api.handler.publications;

import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityPlugin;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.documents.DocumentPublicationRecord;

import java.net.URL;
import java.time.Duration;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_TEMPORARY_REDIRECT;

/** {@link ApiGatewayRequestHandler} for "/publications/{documentId}". */
public class PublicationsDocumentIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public PublicationsDocumentIdRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    documentService.deletePublishDocument(siteId, documentId);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "'" + documentId + "' object deleted").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentPublicationRecord item = documentService.findPublishDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    String s3key = createS3Key(siteId, documentId);
    String s3VersionKey = item.getS3version();

    String contentType =
        item.getContentType() != null ? item.getContentType() : "application/octet-stream";

    PresignGetUrlConfig config =
        new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false).contentType(s3key)
            .contentType(contentType);

    S3PresignerService s3Service = awsservice.getExtension(S3PresignerService.class);
    Duration duration = Duration.ofHours(1);
    URL url = s3Service.presignGetUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), s3key,
        duration, s3VersionKey, config);

    if (awsservice.containsExtension(UserActivityPlugin.class)) {
      UserActivityPlugin plugin = awsservice.getExtension(UserActivityPlugin.class);
      plugin.addDocumentViewActivity(siteId, documentId, s3VersionKey);
    }

    return ApiRequestHandlerResponse.builder().status(SC_TEMPORARY_REDIRECT)
        .header("Location", url.toString()).build();
  }

  @Override
  public String getRequestUrl() {
    return "/publications/{documentId}";
  }
}
