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
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.dynamodb.DocumentItem;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;

/** {@link ApiGatewayRequestHandler} for GET "/documents/upload". */
public class DocumentsUploadRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 48;
  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private DocumentsRestrictionsMaxDocuments restrictionMaxDocuments =
      new DocumentsRestrictionsMaxDocuments();
  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private DocumentsRestrictionsMaxContentLength restrictionMaxContentLength =
      new DocumentsRestrictionsMaxContentLength();

  /**
   * constructor.
   *
   */
  public DocumentsUploadRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    boolean documentExists = false;

    Date date = new Date();
    String documentId = UUID.randomUUID().toString();
    String username = getCallingCognitoUsername(event);
    DocumentItem item = new DocumentItemDynamoDb(documentId, date, username);

    List<DocumentTag> tags = new ArrayList<>();

    Map<String, String> query = event.getQueryStringParameters();

    String siteId = getSiteId(event);
    DocumentService service = awsservice.documentService();

    if (query != null && query.containsKey("path")) {

      String path = query.get("path");
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());

      item.setPath(path);
      tags.add(
          new DocumentTag(documentId, "path", path, date, username, DocumentTagType.SYSTEMDEFINED));
    }

    String urlstring = generatePresignedUrl(awsservice, siteId, documentId, query);
    logger.log("generated presign url: " + urlstring + " for document " + documentId);

    if (!documentExists) {

      tags.add(new DocumentTag(documentId, "untagged", null, date, username,
          DocumentTagType.SYSTEMDEFINED));

      String value = this.restrictionMaxDocuments.getSsmValue(awsservice, siteId);
      if (!this.restrictionMaxDocuments.enforced(awsservice, siteId, value)) {

        logger.log("saving document: " + item.getDocumentId() + " on path " + item.getPath());
        service.saveDocument(siteId, item, tags);

        if (value != null) {
          awsservice.documentCountService().incrementDocumentCount(siteId);
        }
      } else {
        throw new BadException("Max Number of Documents reached");
      }
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiUrlResponse(urlstring, documentId));
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

    String key = siteId != null ? siteId + "/" + documentId : documentId;
    Duration duration = caculateDuration(query);
    Optional<Long> contentLength = calculateContentLength(awsservice, query, siteId);
    URL url = awsservice.s3Service().presignPostUrl(awsservice.documents3bucket(), key, duration,
        contentLength);

    String urlstring = url.toString();
    return urlstring;
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

    String value = this.restrictionMaxContentLength.getSsmValue(awsservice, siteId);
    if (value != null
        && this.restrictionMaxContentLength.enforced(awsservice, siteId, value, contentLength)) {

      if (contentLength == null) {
        throw new BadException("'contentLength' is required");
      }

      String maxContentLengthBytes =
          this.restrictionMaxContentLength.getSsmValue(awsservice, siteId);
      throw new BadException("'contentLength' cannot exceed " + maxContentLengthBytes + " bytes");
    }

    return contentLength != null ? Optional.of(contentLength) : Optional.empty();
  }

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

  @Override
  public String getRequestUrl() {
    return "/documents/upload";
  }
}
