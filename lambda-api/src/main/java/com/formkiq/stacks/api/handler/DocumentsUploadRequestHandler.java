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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToDocumentItem;
import com.formkiq.stacks.api.transformers.AddDocumentRequestToPresignedUrls;
import com.formkiq.stacks.dynamodb.DocumentAttributeSchema;
import com.formkiq.stacks.api.transformers.DocumentAttributeToDocumentAttributeRecord;
import com.formkiq.stacks.api.transformers.SchemaMissingRequiredAttributes;
import com.formkiq.stacks.api.validators.DocumentEntityValidator;
import com.formkiq.stacks.api.validators.DocumentEntityValidatorImpl;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentValidator;
import com.formkiq.stacks.dynamodb.DocumentValidatorImpl;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

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
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    AddDocumentRequest item = new AddDocumentRequest();
    item.setDocumentId(UUID.randomUUID().toString());

    Map<String, String> query = event.getQueryStringParameters();

    String siteId = authorization.getSiteId();

    String path = query != null && query.containsKey("path") ? query.get("path") : null;
    item.setPath(path);

    String maxDocumentCount = validateMaxDocuments(awsservice, siteId);

    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, item, new ArrayList<>(), null);

    if (maxDocumentCount != null) {

      DocumentCountService countService = awsservice.getExtension(DocumentCountService.class);
      countService.incrementDocumentCount(siteId);
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
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    AddDocumentRequest request = fromBodyToObject(event, AddDocumentRequest.class);
    request.setDocumentId(UUID.randomUUID().toString());

    notNull(request.getDocuments()).forEach(d -> d.setDocumentId(UUID.randomUUID().toString()));

    SchemaService schemaService = awsservice.getExtension(SchemaService.class);

    String siteId = authorization.getSiteId();
    String documentId = request.getDocumentId();
    Schema schema = schemaService.getSitesSchema(siteId, null);

    AttributeService attributeService = awsservice.getExtension(AttributeService.class);

    List<DocumentAttribute> attributes = notNull(request.getAttributes());

    DocumentAttributeToDocumentAttributeRecord tr =
        new DocumentAttributeToDocumentAttributeRecord(documentId);

    List<DocumentAttributeRecord> searchAttributes =
        attributes.stream().flatMap(a -> tr.apply(a).stream()).toList();

    Collection<DocumentAttributeRecord> missingRequiredAttributes =
        new SchemaMissingRequiredAttributes(attributeService, schema, siteId, documentId)
            .apply(searchAttributes);

    searchAttributes =
        Stream.concat(searchAttributes.stream(), missingRequiredAttributes.stream()).toList();

    List<DocumentTag> tags =
        this.documentEntityValidator.validate(authorization, awsservice, siteId, request, false);

    String maxDocumentCount = validatePost(awsservice, siteId, request);

    Collection<DocumentAttributeRecord> compositeKeys =
        new DocumentAttributeSchema(schema, documentId).apply(searchAttributes);

    searchAttributes = Stream.concat(searchAttributes.stream(), compositeKeys.stream()).toList();

    ApiRequestHandlerResponse response = buildPresignedResponse(event, authorization, awsservice,
        siteId, request, tags, searchAttributes);

    if (maxDocumentCount != null) {

      DocumentCountService countService = awsservice.getExtension(DocumentCountService.class);
      countService.incrementDocumentCount(siteId);
    }

    return response;
  }

  private String validatePost(final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest item) throws BadException, ValidationException {

    Collection<ValidationError> errors = this.documentValidator.validate(item.getMetadata());
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return validateMaxDocuments(awsservice, siteId);
  }

  private String validateMaxDocuments(final AwsServiceCache awsservice, final String siteId)
      throws BadException {
    String maxDocumentCount = this.restrictionMaxDocuments.getValue(awsservice, siteId);
    if (maxDocumentCount != null
        && this.restrictionMaxDocuments.enforced(awsservice, siteId, maxDocumentCount)) {
      throw new BadException("Max Number of Documents reached");
    }

    return maxDocumentCount;
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
   * @param searchAttributes {@link Collection} {@link DocumentAttributeRecord}
   * @return {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   * @throws ValidationException ValidationException
   */
  private ApiRequestHandlerResponse buildPresignedResponse(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice, final String siteId,
      final AddDocumentRequest request, final List<DocumentTag> tags,
      final Collection<DocumentAttributeRecord> searchAttributes)
      throws BadException, ValidationException {

    String documentId = request.getDocumentId();

    if (tags.isEmpty()) {
      tags.add(new DocumentTag(documentId, "untagged", "true", new Date(),
          authorization.getUsername(), DocumentTagType.SYSTEMDEFINED));
    }

    AddDocumentRequestToPresignedUrls addDocumentRequestToPresignedUrls =
        new AddDocumentRequestToPresignedUrls(awsservice, siteId,
            caculateDuration(event.getQueryStringParameters()),
            calculateContentLength(awsservice, event.getQueryStringParameters(), siteId));

    final Map<String, Object> map = addDocumentRequestToPresignedUrls.apply(request);

    DocumentService service = awsservice.getExtension(DocumentService.class);

    DocumentItem item =
        new AddDocumentRequestToDocumentItem(null, authorization.getUsername(), null)
            .apply(request);

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccess(authorization, siteId);
    SaveDocumentOptions options =
        new SaveDocumentOptions().saveDocumentDate(true).validationAccess(validationAccess);
    service.saveDocument(siteId, item, tags, searchAttributes, options);

    ActionsService actionsService = awsservice.getExtension(ActionsService.class);
    notNull(request.getActions()).forEach(a -> a.userId(authorization.getUsername()));
    actionsService.saveNewActions(siteId, documentId, request.getActions());

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

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.getPermissions(siteId).contains(ApiPermission.ADMIN);
    return isAdmin ? AttributeValidationAccess.ADMIN_CREATE : AttributeValidationAccess.CREATE;
  }
}
