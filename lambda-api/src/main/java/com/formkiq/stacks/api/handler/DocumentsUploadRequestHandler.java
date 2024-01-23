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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.DynamicObjectToAction;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.stacks.api.ApiUrlResponse;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.stacks.dynamodb.DynamicObjectToDocumentTag;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for GET "/documents/upload". */
public class DocumentsUploadRequestHandler
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
  public DocumentsUploadRequestHandler() {}

  /**
   * Build Presigned Url Response.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param item {@link DynamicDocumentItem}
   * @return {@link ApiRequestHandlerResponse}
   * @throws UnsupportedEncodingException UnsupportedEncodingException
   * @throws BadException BadException
   * @throws ValidationException ValidationException
   */
  private ApiRequestHandlerResponse buildPresignedResponse(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final AwsServiceCache awsservice, final String siteId,
      final DynamicDocumentItem item)
      throws UnsupportedEncodingException, BadException, ValidationException {

    Date date = item.getInsertedDate();
    String documentId = item.getDocumentId();
    String username = item.getUserId();

    List<DocumentTag> tags = new ArrayList<>();
    if (item.containsKey("tags")) {
      tags = item.getList("tags").stream().map(new DynamicObjectToDocumentTag(null))
          .map(t -> t.setInsertedDate(date)).collect(Collectors.toList());
    }

    if (tags.isEmpty()) {
      tags.add(new DocumentTag(documentId, "untagged", "true", date, username,
          DocumentTagType.SYSTEMDEFINED));
    }

    validateTagSchema(awsservice, siteId, item, username, tags);

    String urlstring = generatePresignedUrl(event, awsservice, logger, siteId, documentId);

    if (awsservice.debug()) {
      logger.log("generated presign url: " + urlstring + " for document " + documentId);
    }

    String value = this.restrictionMaxDocuments.getValue(awsservice, siteId);

    if (!this.restrictionMaxDocuments.enforced(awsservice, siteId, value)) {

      DocumentService service = awsservice.getExtension(DocumentService.class);

      if (awsservice.debug()) {
        logger.log("saving document: " + item.getDocumentId() + " on path " + item.getPath());
      }

      SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(true);
      service.saveDocument(siteId, item, tags, options);

      if (item.containsKey("actions")) {
        ActionsService actionsService = awsservice.getExtension(ActionsService.class);
        List<Action> actions = item.getList("actions").stream().map(new DynamicObjectToAction())
            .collect(Collectors.toList());
        actions.forEach(a -> a.userId(username));
        actionsService.saveNewActions(siteId, documentId, actions);
      }

      if (value != null) {

        DocumentCountService countService = awsservice.getExtension(DocumentCountService.class);
        countService.incrementDocumentCount(siteId);
      }

    } else {
      throw new BadException("Max Number of Documents reached");
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiUrlResponse(urlstring, documentId));
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

  /**
   * Calculate Content Length.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param logger {@link LambdaLogger}
   * @param query {@link Map}
   * @param siteId {@link String}
   * @return {@link Optional} {@link Long}
   * @throws BadException BadException
   */
  private Optional<Long> calculateContentLength(final AwsServiceCache awsservice,
      final LambdaLogger logger, final Map<String, String> query, final String siteId)
      throws BadException {

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
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsservice {@link AwsServiceCache}
   * @param logger {@link LambdaLogger}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link String}
   * @throws BadException BadException
   */
  private String generatePresignedUrl(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final LambdaLogger logger, final String siteId,
      final String documentId) throws BadException {

    Map<String, String> query = event.getQueryStringParameters();
    String key = !isDefaultSiteId(siteId) ? siteId + "/" + documentId : documentId;
    Duration duration = caculateDuration(query);
    Optional<Long> contentLength = calculateContentLength(awsservice, logger, query, siteId);

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

    DynamicDocumentItem item = new DynamicDocumentItem(new HashMap<>());
    item.setInsertedDate(new Date());
    item.setDocumentId(UUID.randomUUID().toString());
    item.setUserId(authorization.getUsername());

    Map<String, String> query = event.getQueryStringParameters();

    String siteId = authorization.getSiteId();

    String path = query != null && query.containsKey("path") ? query.get("path") : null;
    item.setPath(path);

    return buildPresignedResponse(logger, event, awsservice, siteId, item);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/upload";
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.WRITE);
    return Optional.of(Boolean.valueOf(access));
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    DynamicDocumentItem item = new DynamicDocumentItem(fromBodyToMap(event));
    item.setDocumentId(UUID.randomUUID().toString());
    item.setUserId(authorization.getUsername());
    item.setInsertedDate(new Date());

    List<DynamicObject> tags = item.getList("tags");
    validateTags(tags);

    String siteId = authorization.getSiteId();
    return buildPresignedResponse(logger, event, awsservice, siteId, item);
  }

  /**
   * Validate Document Tags.
   * 
   * @param tags {@link List} {@link DynamicObject}
   * @throws ValidationException ValidationException
   */
  private void validateTags(final List<DynamicObject> tags) throws ValidationException {

    List<String> tagKeys = tags.stream().map(t -> t.getString("key")).collect(Collectors.toList());

    DocumentTagValidator validator = new DocumentTagValidatorImpl();
    Collection<ValidationError> errors = validator.validateKeys(tagKeys);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  /**
   * Validate {@link DynamicDocumentItem} against a TagSchema.
   * 
   * @param cacheService {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param item {@link DynamicDocumentItem}
   * @param userId {@link String}
   * @param tags {@link List} {@link DocumentTag}
   * @throws ValidationException ValidationException
   * @throws BadException BadException
   */
  private void validateTagSchema(final AwsServiceCache cacheService, final String siteId,
      final DynamicDocumentItem item, final String userId, final List<DocumentTag> tags)
      throws ValidationException, BadException {

    DocumentTagSchemaPlugin plugin = cacheService.getExtension(DocumentTagSchemaPlugin.class);

    Collection<ValidationError> errors = new ArrayList<>();

    List<DocumentTag> compositeTags =
        plugin.addCompositeKeys(siteId, item, tags, userId, true, errors).stream().map(t -> t)
            .collect(Collectors.toList());

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    tags.addAll(compositeTags);
  }
}
