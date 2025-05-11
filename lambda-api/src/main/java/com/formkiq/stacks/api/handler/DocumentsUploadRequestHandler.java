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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.ConflictException;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToDocumentItem;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToPresignedUrls;
import com.formkiq.stacks.api.transformers.DocumentAttributeToDocumentAttributeRecord;
import com.formkiq.stacks.api.validators.DocumentEntityValidator;
import com.formkiq.stacks.api.validators.DocumentEntityValidatorImpl;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

/** {@link ApiGatewayRequestHandler} for GET "/documents/upload". */
public class DocumentsUploadRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 48;
  /** {@link DocumentsRestrictionsMaxContentLength}. */
  private final DocumentsRestrictionsMaxContentLength restrictionMaxContentLength =
      new DocumentsRestrictionsMaxContentLength();
  /** {@link DocumentsRestrictionsMaxDocuments}. */
  private final DocumentsRestrictionsMaxDocuments restrictionMaxDocuments =
      new DocumentsRestrictionsMaxDocuments();
  /** {@link DocumentEntityValidator}. */
  private final DocumentEntityValidator documentEntityValidator = new DocumentEntityValidatorImpl();
  /** {@link DocumentValidator}. */
  private final DocumentValidator documentValidator = new DocumentValidatorImpl();

  /**
   * constructor.
   *
   */
  public DocumentsUploadRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    AddDocumentRequest item = new AddDocumentRequest();
    item.setDocumentId(ID.uuid());
    item.setChecksum(event.getQueryStringParameter("checksum"));
    item.setChecksumType(event.getQueryStringParameter("checksumType"));
    item.setPath(event.getQueryStringParameter("path"));

    String siteId = authorization.getSiteId();

    validate(awsservice, siteId, item);

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);

    validateMaxDocuments(awsservice, config, siteId);

    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, item, new ArrayList<>(), null);

    if (!Strings.isEmpty(config.getMaxDocuments())) {
      configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    }

    return response;
  }

  @Override
  public String getRequestUrl() {
    return "/documents/upload";
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.WRITE);
    return Optional.of(access);
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {
    AddDocumentRequest request = fromBodyToObject(event, AddDocumentRequest.class);
    return post(event, authorization, awsservice, request);
  }

  /**
   * Handle POST event interally.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsservice {@link AwsServiceCache}
   * @param request {@link AddDocumentRequest}
   * @return ApiRequestHandlerResponse
   * @throws Exception Exception
   */
  protected ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice,
      final AddDocumentRequest request) throws Exception {

    String siteId = authorization.getSiteId();
    validate(awsservice, siteId, request);

    if (isEmpty(request.getDocumentId())) {
      request.setDocumentId(ID.uuid());
    }

    notNull(request.getDocuments()).forEach(d -> {
      if (isEmpty(d.getDocumentId())) {
        d.setDocumentId(ID.uuid());
      }
    });

    String documentId = request.getDocumentId();

    List<DocumentAttribute> attributes = notNull(request.getAttributes());

    DocumentAttributeToDocumentAttributeRecord tr = new DocumentAttributeToDocumentAttributeRecord(
        awsservice, siteId, documentId, authorization.getUsername());

    List<DocumentAttributeRecord> documentAttributes =
        attributes.stream().flatMap(a -> tr.apply(a).stream()).toList();

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);

    List<DocumentTag> tags = this.documentEntityValidator.validate(authorization, awsservice,
        config, siteId, request, false);

    validatePost(awsservice, config, siteId, request);

    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, request, tags, documentAttributes);

    if (!isEmpty(config.getMaxDocuments())) {
      configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    }

    return response;
  }

  private void validate(final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest request)
      throws ConflictException, BadException, ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    if (!isEmpty(request.getChecksumType())) {

      if (isEmpty(request.getChecksum())) {
        errors.add(new ValidationErrorImpl().key("checksum").error("'checksum' is required"));
      }
    }

    validateDocumentIds(awsservice, siteId, request);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private void validateDocumentIds(final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest request) throws ConflictException, BadException {

    List<String> documentIds = new ArrayList<>();
    if (!isEmpty(request.getDocumentId())) {
      documentIds.add(request.getDocumentId());
    }

    List<String> childDocIds = notNull(request.getDocuments()).stream()
        .map(AddDocumentRequest::getDocumentId).filter(documentId -> !isEmpty(documentId)).toList();
    documentIds.addAll(childDocIds);

    DocumentService service = awsservice.getExtension(DocumentService.class);

    for (String documentId : documentIds) {

      if (!Strings.isUuid(documentId)) {
        throw new BadException("invalid documentId '" + documentId + "'");
      }

      if (service.exists(siteId, documentId)) {
        throw new ConflictException("documentId '" + documentId + "' already exists");
      }
    }
  }

  private void validatePost(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final AddDocumentRequest item) throws BadException, ValidationException {

    Collection<ValidationError> errors = this.documentValidator.validate(item.getMetadata());

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    validateMaxDocuments(awsservice, config, siteId);
  }

  private void validateMaxDocuments(final AwsServiceCache awsservice,
      final SiteConfiguration config, final String siteId) throws BadException {

    if (this.restrictionMaxDocuments.isViolated(awsservice, config, siteId, null)) {
      throw new BadException("Max Number of Documents reached");
    }
  }

  /**
   * Build Presigned Url Response.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param request {@link AddDocumentRequest}
   * @param tags {@link List} {@link DocumentTag}
   * @param documentAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   * @throws ValidationException ValidationException
   */
  private ApiRequestHandlerResponse buildPresignedResponse(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest request, final List<DocumentTag> tags,
      final Collection<DocumentAttributeRecord> documentAttributes)
      throws BadException, ValidationException {

    String documentId = request.getDocumentId();

    AddDocumentRequestToPresignedUrls addDocumentRequestToPresignedUrls =
        new AddDocumentRequestToPresignedUrls(awsservice, authorization, siteId,
            caculateDuration(event.getQueryStringParameters()),
            calculateContentLength(awsservice, event.getQueryStringParameters(), siteId));
    final Map<String, Object> map = addDocumentRequestToPresignedUrls.apply(request);

    DocumentItem item =
        new AddDocumentRequestToDocumentItem(null, authorization.getUsername(), null)
            .apply(request);

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccess(authorization, siteId);
    SaveDocumentOptions options =
        new SaveDocumentOptions().saveDocumentDate(true).validationAccess(validationAccess);

    DocumentService service = awsservice.getExtension(DocumentService.class);
    service.saveDocument(siteId, item, tags, documentAttributes, options);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);
    List<Action> actions = notNull(request.getActions());
    actions.forEach(a -> a.userId(authorization.getUsername()));
    actionsService.saveNewActions(siteId, documentId, actions);

    if (!Strings.isEmpty(item.getDeepLinkPath()) && !actions.isEmpty()) {
      ActionsNotificationService notificationService =
          awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId);
    }

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
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

    return Duration.ofHours(durationHours);
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

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration siteConfiguration = configService.get(siteId);

    Long contentLength = query != null && query.containsKey("contentLength")
        ? Long.valueOf(query.get("contentLength"))
        : null;

    if (!Strings.isEmpty(siteConfiguration.getMaxContentLengthBytes())) {

      if (contentLength == null) {
        throw new BadException(
            "'contentLength' is required when MaxContentLengthBytes is configured");
      } else {
        DocumentItem item = new DocumentItemDynamoDb();
        item.setContentLength(contentLength);

        if (this.restrictionMaxContentLength.isViolated(awsservice, siteConfiguration, siteId,
            item)) {
          throw new BadException("'contentLength' cannot exceed "
              + siteConfiguration.getMaxContentLengthBytes() + " bytes");
        }
      }
    }

    return contentLength != null ? Optional.of(contentLength) : Optional.empty();
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {

    Collection<ApiPermission> permissions = authorization.getPermissions(siteId);
    boolean isAdmin =
        permissions.contains(ApiPermission.ADMIN) || permissions.contains(ApiPermission.GOVERN);
    return isAdmin ? AttributeValidationAccess.ADMIN_CREATE : AttributeValidationAccess.CREATE;
  }
}
