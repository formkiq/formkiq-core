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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToDocumentItem;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToPresignedUrls;
import com.formkiq.stacks.api.transformers.DocumentAttributeToDocumentAttributeRecord;
import com.formkiq.stacks.api.transformers.PresignedUrlsToS3Bucket;
import com.formkiq.stacks.api.validators.DocumentEntityValidator;
import com.formkiq.stacks.api.validators.DocumentEntityValidatorImpl;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}". */
public class DocumentIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link DocumentEntityValidator}. */
  private final DocumentEntityValidator documentEntityValidator = new DocumentEntityValidatorImpl();
  /** {@link DocumentValidator}. */
  private final DocumentValidator documentValidator = new DocumentValidatorImpl();

  /**
   * constructor.
   *
   */
  public DocumentIdRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String documentBucket = awsservice.environment("DOCUMENTS_S3_BUCKET");

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    if (awsservice.debug()) {
      logger.log("deleting object " + documentId + " from bucket '" + documentBucket + "'");
    }

    boolean softDelete = "true".equals(event.getQueryStringParameter("softDelete"));

    DocumentService service = awsservice.getExtension(DocumentService.class);

    try {

      if (!softDelete) {
        S3Service s3Service = awsservice.getExtension(S3Service.class);

        String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId);
        S3ObjectMetadata md = s3Service.getObjectMetadata(documentBucket, s3Key, null);

        if (md.isObjectExists()) {
          s3Service.deleteObject(documentBucket, s3Key, null);
        }
      }

      if (!service.deleteDocument(siteId, documentId, softDelete)) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      ApiResponse resp = new ApiMessageResponse("'" + documentId + "' object deleted");
      return new ApiRequestHandlerResponse(SC_OK, resp);

    } catch (S3Exception e) {

      if (e.statusCode() == SC_NOT_FOUND.getStatusCode()) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      throw e;
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    int limit = getLimit(logger, event);

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    ApiPagination token = getPagination(cacheService, event);
    String documentId = event.getPathParameters().get("documentId");
    ApiPagination pagination = getPagination(cacheService, event);

    PaginationResult<DocumentItem> presult = documentService.findDocument(siteId, documentId, true,
        token != null ? token.getStartkey() : null, limit);

    DocumentItem item = presult.getResult();
    throwIfNull(item, new DocumentNotFoundException(documentId));

    ApiPagination current =
        createPagination(cacheService, event, pagination, presult.getToken(), limit);

    DynamicDocumentItem ditem = new DocumentItemToDynamicDocumentItem().apply(item);
    ditem.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    ditem.put("previous", current.getPrevious());
    ditem.put("next", current.hasNext() ? current.getNext() : null);

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(ditem));
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    AddDocumentRequest request = fromBodyToObject(event, AddDocumentRequest.class);
    request.setDocumentId(documentId);

    DocumentService docService = awsservice.getExtension(DocumentService.class);
    DocumentItem existingItem = docService.findDocument(siteId, documentId);
    if (existingItem == null) {
      throw new DocumentNotFoundException(documentId);
    }

    DocumentItem item =
        new AddDocumentRequestToDocumentItem(existingItem, authorization.getUsername(), null)
            .apply(request);

    validatePatch(awsservice, siteId, documentId, item);

    logger.log("setting userId: " + item.getUserId() + " contentType: " + item.getContentType());

    List<DocumentTag> tags =
        this.documentEntityValidator.validate(authorization, awsservice, siteId, request, true);

    DocumentService service = awsservice.getExtension(DocumentService.class);

    List<DocumentAttributeRecord> documentAttributes =
        getDocumentAttributeRecords(request, authorization);

    SaveDocumentOptions options = new SaveDocumentOptions()
        .validationAccess(getAttributeValidationAccess(authorization, siteId));
    service.saveDocument(siteId, item, tags, documentAttributes, options);

    AddDocumentRequestToPresignedUrls addDocumentRequestToPresignedUrls =
        new AddDocumentRequestToPresignedUrls(awsservice, siteId, null, Optional.empty());

    Map<String, Object> uploadUrls = addDocumentRequestToPresignedUrls.apply(request);
    new PresignedUrlsToS3Bucket(request).apply(uploadUrls);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);
    List<Action> actions = notNull(request.getActions());
    actions.forEach(a -> a.userId(authorization.getUsername()));
    actionsService.saveNewActions(siteId, documentId, request.getActions());

    if (!Strings.isEmpty(item.getDeepLinkPath()) && !actions.isEmpty()) {
      ActionsNotificationService notificationService =
          awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId);
    }

    uploadUrls.put("siteId", siteId);
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(uploadUrls));
  }

  /**
   * Validate Patch Request.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param doc {@link DocumentItem}
   * @throws Exception Exception
   */
  private void validatePatch(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final DocumentItem doc) throws Exception {

    DocumentService docService = awsservice.getExtension(DocumentService.class);
    DocumentItem item = docService.findDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    Collection<DocumentMetadata> metadata =
        item.getMetadata() != null ? new ArrayList<>(item.getMetadata()) : new ArrayList<>();
    if (doc.getMetadata() != null) {
      metadata.addAll(doc.getMetadata());
    }

    Collection<ValidationError> errors = this.documentValidator.validate(metadata);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  /**
   * Get Document Attribute Records.
   *
   * @param request {@link AddDocumentRequest}
   * @param authorization {@link ApiAuthorization}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  private List<DocumentAttributeRecord> getDocumentAttributeRecords(
      final AddDocumentRequest request, final ApiAuthorization authorization) {

    DocumentAttributeToDocumentAttributeRecord tr = new DocumentAttributeToDocumentAttributeRecord(
        request.getDocumentId(), authorization.getUsername());

    List<DocumentAttribute> attributes = notNull(request.getAttributes());
    return attributes.stream().flatMap(a -> tr.apply(a).stream()).toList();
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.getPermissions(siteId).contains(ApiPermission.ADMIN);
    return isAdmin ? AttributeValidationAccess.ADMIN_UPDATE : AttributeValidationAccess.UPDATE;
  }
}
