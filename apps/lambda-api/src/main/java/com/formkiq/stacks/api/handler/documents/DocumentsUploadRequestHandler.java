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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.ConflictException;
import com.formkiq.aws.dynamodb.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.stacks.dynamodb.documents.AddDocumentRequest;
import com.formkiq.stacks.dynamodb.documents.AddDocumentRequestToDocumentRecordSet;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/** {@link ApiGatewayRequestHandler} for GET "/documents/upload". */
public class DocumentsUploadRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Default Duration Hours. */
  private static final int DEFAULT_DURATION_HOURS = 48;

  private static void setDocumentIdIfMissing(final AddDocumentRequest request) {
    if (isEmpty(request.getDocumentId())) {
      request.setDocumentId(ID.uuid());
    }

    notNull(request.getDocuments()).forEach(d -> {
      if (isEmpty(d.getDocumentId())) {
        d.setDocumentId(ID.uuid());
      }
    });
  }

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

  /**
   * Build Presigned Url Response.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param request {@link AddDocumentRequest}
   * @param documentRecordSet {@link List} {@link DocumentRecordSet}
   * @param config {@link SiteConfiguration}
   * @return {@link ApiRequestHandlerResponse.Builder}
   * @throws BadException BadException
   * @throws ValidationException ValidationException
   */
  private ApiRequestHandlerResponse.Builder buildPresignedResponse(
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice, final String siteId, final AddDocumentRequest request,
      final DocumentRecordSet documentRecordSet, final SiteConfiguration config)
      throws BadException, ValidationException {

    String documentId = documentRecordSet.documentRecord().documentId();
    String artifactId = documentRecordSet.documentRecord().artifactId();

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccess(authorization, siteId);
    SaveDocumentOptions options =
        new SaveDocumentOptions().saveDocumentDate(true).validationAccess(validationAccess);

    Optional<Long> documentContentLength =
        calculateContentLength(awsservice, event.getQueryStringParameters(), siteId, config);

    DocumentService service = awsservice.getExtension(DocumentService.class);
    service.saveDocument(siteId, documentRecordSet, options);

    Duration urlDuration = caculateDuration(event.getQueryStringParameters());
    AddDocumentRequestToPresignedUrls addDocumentRequestToPresignedUrls =
        new AddDocumentRequestToPresignedUrls(awsservice, authorization, siteId, urlDuration,
            documentContentLength);
    final Map<String, Object> map = addDocumentRequestToPresignedUrls.apply(request, artifactId);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);
    List<Action> actions = createActions(siteId, request, documentId, artifactId);
    actionsService.saveNewActions(actions);

    var deepLinkPath = documentRecordSet.documentRecord().deepLinkPath();
    if (!Strings.isEmpty(deepLinkPath) && !actions.isEmpty()) {
      ActionsNotificationService notificationService =
          awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId, artifactId);
    }

    return ApiRequestHandlerResponse.builder().created().body(map);
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
   * @param siteConfiguration {@link SiteConfiguration}
   * @return {@link Optional} {@link Long}
   * @throws BadException BadException
   */
  private Optional<Long> calculateContentLength(final AwsServiceCache awsservice,
      final Map<String, String> query, final String siteId,
      final SiteConfiguration siteConfiguration) throws BadException {

    Long contentLength = query != null && query.containsKey("contentLength")
        ? Long.valueOf(query.get("contentLength"))
        : null;

    if (!Strings.isEmpty(siteConfiguration.maxContentLengthBytes())) {

      if (contentLength == null) {
        throw new BadException(
            "'contentLength' is required when MaxContentLengthBytes is configured");
      } else {
        DocumentItem item = new DocumentItemDynamoDb();
        item.setContentLength(contentLength);

        if (this.restrictionMaxContentLength.isViolated(awsservice, siteConfiguration, siteId,
            item)) {
          throw new BadException("'contentLength' cannot exceed "
              + siteConfiguration.maxContentLengthBytes() + " bytes");
        }
      }
    }

    return contentLength != null ? Optional.of(contentLength) : Optional.empty();
  }

  private List<Action> createActions(final String siteId, final AddDocumentRequest request,
      final String documentId, final String artifactId) {
    DocumentArtifact document = DocumentArtifact.of(documentId, artifactId);
    return notNull(request.getActions()).stream()
        .map(a -> new AddActionToActionFunction(document).apply(siteId, a)).toList();
  }

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

    ValidationBuilder vb = new ValidationBuilder();
    validateMaxDocuments(vb, awsservice, config, siteId);
    vb.check();

    var documentRecordSet = getDocumentRecordSet(authorization, awsservice, item, siteId);
    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, item, documentRecordSet, config).ok().build();

    if (!Strings.isEmpty(config.maxDocuments())) {
      configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    }

    return response;
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {
    boolean isAdmin = authorization.isAdminOrGovern(siteId);
    return isAdmin ? AttributeValidationAccess.ADMIN_CREATE : AttributeValidationAccess.CREATE;
  }

  private DocumentRecordSet getDocumentRecordSet(final ApiAuthorization authorization,
      final AwsServiceCache awsservice, final AddDocumentRequest request, final String siteId) {
    var transformAddDocumentRequestToDocumentRecordSet =
        new AddDocumentRequestToDocumentRecordSet(awsservice, null, authorization.getUsername());
    return transformAddDocumentRequestToDocumentRecordSet.apply(siteId, request);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/upload";
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    String siteId = authorization.getSiteId();
    boolean access = authorization.getPermissions(siteId).contains(ApiPermission.WRITE);
    return Optional.of(access);
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {
    AddDocumentRequest request = JsonToObject.fromJson(awsservice, event, AddDocumentRequest.class);
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

    setDocumentIdIfMissing(request);

    ConfigService configService = awsservice.getExtension(ConfigService.class);
    SiteConfiguration config = configService.get(siteId);

    this.documentEntityValidator.validate(awsservice, config, siteId, request);
    validatePost(awsservice, config, siteId, request);

    var documentRecordSet = getDocumentRecordSet(authorization, awsservice, request, siteId);

    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, request, documentRecordSet, config).build();

    if (!isEmpty(config.maxDocuments())) {
      configService.increment(siteId, ConfigService.DOCUMENT_COUNT);
    }

    return response;
  }

  private void validate(final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest request)
      throws ConflictException, BadException, ValidationException {

    ValidationBuilder vb = new ValidationBuilder();

    if (!isEmpty(request.getChecksumType())) {
      vb.isRequired("checksum", request.getChecksum());
    }

    if (request.isArtifacts()) {

      if (!notNull(request.getDocuments()).isEmpty()) {
        vb.addError("documents", "'documents' are not allowed when 'artifacts' is true");
      }
    }

    validateDocumentIds(awsservice, vb, siteId, request);

    vb.check();
  }

  private void validateDocumentIds(final AwsServiceCache awsservice, final ValidationBuilder vb,
      final String siteId, final AddDocumentRequest request)
      throws ConflictException, BadException {

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
        vb.addError("documentId", "invalid documentId '" + documentId + "'");
      }

      DocumentArtifact document = DocumentArtifact.of(documentId, null);
      var documentExists = service.exists(siteId, document);

      if (request.isArtifacts() && !documentExists) {
        vb.addError("documentId", "Document '" + documentId + "' does not exist");
      } else if (!request.isArtifacts() && documentExists) {
        throw new ConflictException("documentId '" + documentId + "' already exists");
      }
    }
  }

  private void validateMaxDocuments(final ValidationBuilder vb, final AwsServiceCache awsservice,
      final SiteConfiguration config, final String siteId) throws BadException {

    if (this.restrictionMaxDocuments.isViolated(awsservice, config, siteId, null)) {
      vb.addError(null, "Max Number of Documents reached");
    }
  }

  private void validatePost(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final AddDocumentRequest item) throws BadException, ValidationException {

    ValidationBuilder vb = new ValidationBuilder();
    Collection<ValidationError> errors = this.documentValidator.validate(item.getMetadata());
    vb.addErrors(errors);
    this.documentValidator.validateContentType(config, item.getContentType(), vb);

    validateMaxDocuments(vb, awsservice, config, siteId);

    vb.check();
  }
}
