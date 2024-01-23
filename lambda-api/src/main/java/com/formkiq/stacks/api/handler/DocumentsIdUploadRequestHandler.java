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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/upload". */
public class DocumentsIdUploadRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 48;
  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private DocumentsRestrictionsMaxContentLength restrictionMaxContentLength =
      new DocumentsRestrictionsMaxContentLength();
  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private DocumentsRestrictionsMaxDocuments restrictionMaxDocuments =
      new DocumentsRestrictionsMaxDocuments();

  /**
   * constructor.
   *
   */
  public DocumentsIdUploadRequestHandler() {}

  /**
   * Calculate Duration.
   * 
   * @param query {@link Map}
   * @return {@link Duration}
   */
  private Duration caculateDuration(final Map<String, String> query) {

    Integer durationHours =
        query != null && query.containsKey("duration") ? Integer.valueOf(query.get("duration"))
            : Integer.valueOf(DEFAULT_DURATION_HOURS);

    Duration duration = Duration.ofHours(durationHours.intValue());
    return duration;
  }

  /**
   * Calculate Content Length.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param query {@link Map}
   * @param siteId {@link String}
   * @return {@link Optional} {@link Long}
   * @throws BadException BadException
   */
  private Optional<Long> calculateContentLength(final AwsServiceCache awsservice,
      final Map<String, String> query, final String siteId) throws BadException {

    Long contentLength = query != null && query.containsKey("contentLength")
        ? Long.valueOf(query.get("contentLength"))
        : null;

    String value = this.restrictionMaxContentLength.getValue(awsservice, siteId);
    if (value != null
        && this.restrictionMaxContentLength.enforced(awsservice, siteId, value, contentLength)) {

      if (contentLength == null) {
        throw new BadException("'contentLength' is required");
      }

      String maxContentLengthBytes = this.restrictionMaxContentLength.getValue(awsservice, siteId);
      throw new BadException("'contentLength' cannot exceed " + maxContentLengthBytes + " bytes");
    }

    return contentLength != null ? Optional.of(contentLength) : Optional.empty();
  }

  /**
   * Generate Presigned URL.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param query {@link Map}
   * @return {@link String}
   * @throws BadException BadException
   */
  private String generatePresignedUrl(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final Map<String, String> query) throws BadException {

    String key = !isDefaultSiteId(siteId) ? siteId + "/" + documentId : documentId;
    Duration duration = caculateDuration(query);
    Optional<Long> contentLength = calculateContentLength(awsservice, query, siteId);
    S3PresignerService s3Service = awsservice.getExtension(S3PresignerService.class);

    Map<String, String> map = Map.of("checksum", UUID.randomUUID().toString());
    URL url = s3Service.presignPutUrl(awsservice.environment("DOCUMENTS_S3_BUCKET"), key, duration,
        contentLength, map);

    String urlstring = url.toString();
    return urlstring;
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    boolean documentExists = false;

    Date date = new Date();
    String documentId = UUID.randomUUID().toString();
    String username = authorization.getUsername();
    DocumentItem item = new DocumentItemDynamoDb(documentId, date, username);

    List<DocumentTag> tags = new ArrayList<>();

    Map<String, String> map = event.getPathParameters();
    Map<String, String> query = event.getQueryStringParameters();

    String siteId = authorization.getSiteId();
    DocumentService service = awsservice.getExtension(DocumentService.class);

    if (map != null && map.containsKey("documentId")) {

      documentId = map.get("documentId");

      item = service.findDocument(siteId, documentId);
      throwIfNull(item, new DocumentNotFoundException(documentId));

      documentExists = item != null;

    } else if (query != null && query.containsKey("path")) {

      String path = query.get("path");
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());

      item.setPath(path);
    }

    String urlstring = generatePresignedUrl(awsservice, siteId, documentId, query);
    logger.log("generated presign url: " + urlstring + " for document " + documentId);

    if (!documentExists && item != null) {

      tags.add(new DocumentTag(documentId, "untagged", "true", date, username,
          DocumentTagType.SYSTEMDEFINED));

      String value = this.restrictionMaxDocuments.getValue(awsservice, siteId);
      if (!this.restrictionMaxDocuments.enforced(awsservice, siteId, value)) {

        logger.log("saving document: " + item.getDocumentId() + " on path " + item.getPath());
        service.saveDocument(siteId, item, tags);

        incrementDocumentCount(awsservice, siteId, value);
      }
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiUrlResponse(urlstring, documentId));
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/upload";
  }

  /**
   * Increment Document Count.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param value {@link String}
   * @throws BadException BadException
   */
  private void incrementDocumentCount(final AwsServiceCache awsservice, final String siteId,
      final String value) throws BadException {
    if (value != null) {
      DocumentCountService countService = awsservice.getExtension(DocumentCountService.class);
      countService.incrementDocumentCount(siteId);
    } else {
      throw new BadException("Max Number of Documents reached");
    }
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.WRITE);
    return Optional.of(Boolean.valueOf(access));
  }
}
