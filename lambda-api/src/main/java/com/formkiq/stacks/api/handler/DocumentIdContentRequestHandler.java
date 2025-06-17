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

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.useractivity.UserActivityPlugin;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/content". */
public class DocumentIdContentRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentIdContentRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");
    String versionKey = event.getQueryStringParameter("versionKey");

    Map<String, AttributeValue> versionAttributes =
        getVersionAttributes(awsservice, siteId, documentId, versionKey);

    DocumentItem item =
        getDocumentItem(awsservice, siteId, documentId, versionKey, versionAttributes);
    String versionId = getVersionId(awsservice, versionAttributes, versionKey);

    ApiResponse response;

    String s3key = createS3Key(siteId, documentId);

    if (MimeType.isPlainText(item.getContentType())) {

      try {
        response = getPlainTextResponse(awsservice, s3key, versionId, item, documentId);
      } catch (NotFoundException e) {
        throw e;
      } catch (RuntimeException e) {
        response = getApiResponse(awsservice, item, s3key, versionId);
      }

    } else {
      response = getApiResponse(awsservice, item, s3key, versionId);
    }

    if (awsservice.containsExtension(UserActivityPlugin.class)) {
      UserActivityPlugin plugin = awsservice.getExtension(UserActivityPlugin.class);
      plugin.addDocumentViewActivity(siteId, documentId, versionKey);
    }

    return new ApiRequestHandlerResponse(SC_OK, response);
  }

  private ApiResponse getApiResponse(final AwsServiceCache awsservice, final DocumentItem item,
      final String s3key, final String versionId) {
    String contentType =
        item.getContentType() != null ? item.getContentType() : "application/octet-stream";

    PresignGetUrlConfig config =
        new PresignGetUrlConfig().contentDispositionByPath(item.getPath(), false).contentType(s3key)
            .contentType(contentType);

    S3PresignerService s3Service = awsservice.getExtension(S3PresignerService.class);
    Duration duration = Duration.ofHours(1);
    URL url = s3Service.presignGetUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), s3key,
        duration, versionId, config);

    return new ApiMapResponse(Map.of("contentUrl", url.toString(), "contentType", contentType));
  }

  private static ApiResponse getPlainTextResponse(final AwsServiceCache awsservice,
      final String s3key, final String versionId, final DocumentItem item, final String documentId)
      throws DocumentNotFoundException {

    S3Service s3Service = awsservice.getExtension(S3Service.class);

    try {
      String content = s3Service.getContentAsString(awsservice.environment("DOCUMENTS_S3_BUCKET"),
          s3key, versionId);

      return new ApiMapResponse(Map.of("content", content, "contentType", item.getContentType(),
          "isBase64", Boolean.FALSE));
    } catch (NoSuchKeyException e) {
      throw new DocumentNotFoundException(documentId);
    }
  }

  private Map<String, AttributeValue> getVersionAttributes(final AwsServiceCache awsservice,
      final String siteId, final String documentId, final String versionKey) {
    DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
    return versionService.get(siteId, documentId, versionKey);
  }

  private DocumentItem getDocumentItem(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final String versionKey,
      final Map<String, AttributeValue> versionAttributes) throws Exception {

    DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentItem item = versionService.getDocumentItem(documentService, siteId, documentId,
        versionKey, versionAttributes);
    throwIfNull(item, new DocumentNotFoundException(documentId));
    return item;
  }

  private String getVersionId(final AwsServiceCache awsservice,
      final Map<String, AttributeValue> versionAttributes, final String versionKey)
      throws Exception {

    String versionId = null;
    if (versionKey != null) {

      DocumentVersionService versionService = awsservice.getExtension(DocumentVersionService.class);
      versionId = versionService.getVersionId(versionAttributes);

      throwIfNull(versionId,
          new BadException("content versionId not found in versionKey '" + versionKey + "'"));
    }

    return versionId;
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/content";
  }
}
