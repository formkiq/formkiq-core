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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.getSiteIdName;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.module.events.document.DocumentEventType.SOFT_DELETE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3ObjectMetadata;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.events.EventService;
import com.formkiq.module.events.document.DocumentEvent;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationError;
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

  private List<Action> createActions(final String siteId, final AddDocumentRequest request,
      final DocumentItem item) {
    DocumentArtifact document = DocumentArtifact.of(item.getDocumentId(), item.getArtifactId());
    return notNull(request.getActions()).stream()
        .map(a -> new AddActionToActionFunction(document).apply(siteId, a)).toList();
  }

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentBucket = awsservice.environment("DOCUMENTS_S3_BUCKET");

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact document = new DocumentArtifact(documentId, artifactId);

    awsservice.getLogger()
        .debug("deleting object " + documentId + " from bucket '" + documentBucket + "'");

    boolean softDelete = "true".equals(event.getQueryStringParameter("softDelete"));

    DocumentService service = awsservice.getExtension(DocumentService.class);

    try {

      if (!softDelete) {
        S3Service s3Service = awsservice.getExtension(S3Service.class);

        String s3Key = SiteIdKeyGenerator.createS3Key(siteId, documentId, artifactId);
        S3ObjectMetadata md = s3Service.getObjectMetadata(documentBucket, s3Key, null);

        if (md.isObjectExists()) {
          s3Service.deleteObject(documentBucket, s3Key, null);
        }
      }

      if (!service.deleteDocument(siteId, document, softDelete)) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      if (softDelete) {
        DocumentEvent docEve = new DocumentEvent().siteId(getSiteIdName(siteId))
            .documentId(documentId).artifactId(artifactId).type(SOFT_DELETE);
        EventService documentEventService = awsservice.getExtension(EventService.class);
        documentEventService.publish(awsservice.getLogger(), docEve);
      }

      return ApiRequestHandlerResponse.builder().ok()
          .body("message", "'" + documentId + "' object" + (softDelete ? " soft" : "") + " deleted")
          .build();

    } catch (S3Exception e) {

      if (e.statusCode() == SC_NOT_FOUND.getStatusCode()) {
        throw new NotFoundException("Document " + documentId + " not found.");
      }

      throw e;
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    int limit = getLimit(awsservice.getLogger(), event);

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact document = new DocumentArtifact(documentId, artifactId);

    ApiPagination pagination = getPagination(cacheService, event);
    String nextToken = pagination != null ? pagination.getNextToken() : null;

    Pagination<DocumentItem> presult =
        documentService.findDocument(siteId, document, true, nextToken, limit);

    DocumentItem item =
        !notNull(presult.getResults()).isEmpty() ? presult.getResults().get(0) : null;
    throwIfNull(item, new DocumentNotFoundException(documentId));

    ApiPagination current =
        createPagination(cacheService, event, pagination, presult.getNextToken(), limit);

    DynamicDocumentItem ditem = new DocumentItemToDynamicDocumentItem().apply(item);
    ditem.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    ditem.put("previous", current.getPrevious());
    ditem.put("next", current.hasNext() ? current.getNext() : null);

    return ApiRequestHandlerResponse.builder().ok().body(ditem).build();
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {
    boolean isAdmin = authorization.isAdminOrGovern(siteId);
    return isAdmin ? AttributeValidationAccess.ADMIN_UPDATE : AttributeValidationAccess.UPDATE;
  }

  /**
   * Get Document Attribute Records.
   *
   * @param request {@link AddDocumentRequest}
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param artifactId {@link String}
   * @return {@link List} {@link DocumentAttributeRecord}
   */
  private List<DocumentAttributeRecord> getDocumentAttributeRecords(
      final AddDocumentRequest request, final AwsServiceCache awsservice, final String siteId,
      final String artifactId) {

    AddDocumentAttributeToDocumentAttributeRecord tr =
        new AddDocumentAttributeToDocumentAttributeRecord(awsservice, siteId,
            DocumentArtifact.of(request.getDocumentId(), artifactId));

    List<AddDocumentAttribute> attributes = notNull(request.getAttributes());
    return attributes.stream().flatMap(a -> tr.apply(a).stream()).toList();
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact document = new DocumentArtifact(documentId, artifactId);

    AddDocumentRequest request = JsonToObject.fromJson(awsservice, event, AddDocumentRequest.class);
    request.setDocumentId(documentId);

    DocumentService docService = awsservice.getExtension(DocumentService.class);
    DocumentRecord existingItem = docService.findDocument(siteId, document);
    if (existingItem == null) {
      throw new DocumentNotFoundException(documentId);
    }

    DocumentItem item =
        new AddDocumentRequestToDocumentItem(existingItem, authorization.getUsername(), null)
            .apply(request);

    SiteConfiguration config = awsservice.getExtension(ConfigService.class).get(siteId);
    validatePatch(awsservice, config, siteId, document, item, request);

    awsservice.getLogger()
        .trace("setting userId: " + item.getUserId() + " contentType: " + item.getContentType());

    List<DocumentTag> tags = this.documentEntityValidator.validate(authorization, awsservice,
        config, siteId, request, true);

    DocumentService service = awsservice.getExtension(DocumentService.class);

    List<DocumentAttributeRecord> documentAttributes =
        getDocumentAttributeRecords(request, awsservice, siteId, artifactId);

    SaveDocumentOptions options = new SaveDocumentOptions()
        .validationAccess(getAttributeValidationAccess(authorization, siteId));
    service.saveDocument(siteId, item, tags, documentAttributes, options);

    AddDocumentRequestToPresignedUrls addDocumentRequestToPresignedUrls =
        new AddDocumentRequestToPresignedUrls(awsservice, authorization, siteId, null,
            Optional.empty());

    Map<String, Object> uploadUrls = addDocumentRequestToPresignedUrls.apply(request, item);
    new PresignedUrlsToS3Bucket(request).apply(uploadUrls);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);
    List<Action> actions = createActions(siteId, request, item);
    actionsService.saveNewActions(actions);

    if (!Strings.isEmpty(item.getDeepLinkPath()) && !actions.isEmpty()) {
      ActionsNotificationService notificationService =
          awsservice.getExtension(ActionsNotificationService.class);
      notificationService.publishNextActionEvent(siteId, documentId, artifactId);
    }

    uploadUrls.put("siteId", siteId);
    return ApiRequestHandlerResponse.builder().ok().body(uploadUrls).build();
  }

  /**
   * Validate Patch Request.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param config {@link SiteConfiguration}
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param doc {@link DocumentItem}
   * @param request {@link AddDocumentRequest}
   * @throws Exception Exception
   */
  private void validatePatch(final AwsServiceCache awsservice, final SiteConfiguration config,
      final String siteId, final DocumentArtifact document, final DocumentItem doc,
      final AddDocumentRequest request) throws Exception {

    DocumentService docService = awsservice.getExtension(DocumentService.class);
    DocumentRecord item = docService.findDocument(siteId, document);
    throwIfNull(item, new DocumentNotFoundException(document.documentId()));

    Collection<DocumentMetadata> metadata =
        item.metadata() != null ? new ArrayList<>(item.metadata()) : new ArrayList<>();
    if (doc.getMetadata() != null) {
      metadata.addAll(doc.getMetadata());
    }

    Collection<ValidationError> errors = this.documentValidator.validate(metadata);
    ValidationBuilder vb = new ValidationBuilder();
    vb.addErrors(errors);

    this.documentValidator.validateContentType(config, request.getContentType(), vb);

    boolean emptyDeepLink = isEmpty(doc.getDeepLinkPath()) && isEmpty(item.deepLinkPath());
    boolean emptyContent = isEmpty(request.getContent());

    if (!emptyDeepLink && !emptyContent) {
      vb.addError(null, "both 'content', and 'deepLinkPath' cannot be set");
    }

    vb.check();
  }
}
