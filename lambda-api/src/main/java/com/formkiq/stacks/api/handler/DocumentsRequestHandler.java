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
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.api.transformers.PresignedUrlsToS3Bucket;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;

/** {@link ApiGatewayRequestHandler} for "/documents". */
public class DocumentsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    ActionStatus actionStatus = getActionStatus(event);

    String siteId = authorization.getSiteId();

    ApiPagination current;
    Map<String, Object> map = new HashMap<>();

    if (isSoftDelete(event)) {

      current = getSoftDeletedDocument(event, awsservice, siteId, map);

    } else if (actionStatus != null) {

      current = getActionStatus(event, awsservice, siteId, actionStatus, map);

    } else {

      current = getDocuments(event, awsservice, siteId, map);
    }

    map.put("next", current.hasNext() ? current.getNext() : null);
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  private ActionStatus getActionStatus(final ApiGatewayRequestEvent event) throws BadException {

    ActionStatus status = null;
    String actionStatus = getParameter(event, "actionStatus");

    if (!Strings.isEmpty(actionStatus)) {
      try {
        status = ActionStatus.valueOf(actionStatus.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BadException("invalid actionStatus '" + actionStatus + "'");
      }
    }

    if (ActionStatus.COMPLETE.equals(status)) {
      throw new BadException("invalid actionStatus '" + actionStatus + "'");
    }

    return status;
  }

  private ApiPagination getActionStatus(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final String siteId, final ActionStatus actionStatus,
      final Map<String, Object> map) {

    Logger logger = awsservice.getLogger();
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    ActionsService actions = awsservice.getExtension(ActionsService.class);

    PaginationToAttributeValue pav = new PaginationToAttributeValue();
    Map<String, AttributeValue> token = pav.apply(ptoken);

    PaginationResults<String> results =
        actions.findDocumentsWithStatus(siteId, actionStatus, token, limit);

    List<Map<String, String>> documents = results.getResults().stream()
        .map(r -> Map.of("documentId", r)).collect(Collectors.toList());

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    map.put("documents", documents);
    return current;
  }

  private ApiPagination getDocuments(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final String siteId, final Map<String, Object> map)
      throws BadException {

    Logger logger = awsservice.getLogger();
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String tz = getParameter(event, "tz");
    String dateString = getParameter(event, "date");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    ZonedDateTime date = transformToDate(awsservice, documentService, dateString, tz);
    logger.trace("search for document using date: " + date);

    final PaginationResults<DocumentItem> results =
        documentService.findDocumentsByDate(siteId, date, ptoken, limit);

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    List<DocumentItem> documents = subList(results.getResults(), limit);

    List<DynamicDocumentItem> items = documents.stream()
        .map(m -> new DocumentItemToDynamicDocumentItem().apply(m)).collect(Collectors.toList());
    items.forEach(i -> i.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID));

    map.put("documents", items);
    map.put("previous", current.getPrevious());
    return current;
  }

  @Override
  public String getRequestUrl() {
    return "/documents";
  }

  private ApiPagination getSoftDeletedDocument(final ApiGatewayRequestEvent event,
      final AwsServiceCache awsservice, final String siteId, final Map<String, Object> map) {

    Logger logger = awsservice.getLogger();
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);

    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    PaginationToAttributeValue pav = new PaginationToAttributeValue();
    Map<String, AttributeValue> token = pav.apply(ptoken);

    DocumentService service = awsservice.getExtension(DocumentService.class);
    PaginationResults<DocumentItem> results =
        service.findSoftDeletedDocuments(siteId, token, limit);

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    map.put("documents", results.getResults());
    return current;
  }

  private boolean isFolder(final AddDocumentRequest item) {
    return !isEmpty(item.getPath()) && item.getPath().endsWith("/");
  }

  private boolean isSoftDelete(final ApiGatewayRequestEvent event) {
    return "true".equals(event.getQueryStringParameter("deleted"));
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    ApiMapResponse apiMapResponse;
    DocumentsUploadRequestHandler handler = new DocumentsUploadRequestHandler();

    String siteId = authorization.getSiteId();
    AddDocumentRequest request = fromBodyToObject(event, AddDocumentRequest.class);

    validatePost(request);

    if (isFolder(request)) {

      DocumentService docService = awsservice.getExtension(DocumentService.class);

      if (!docService.isFolderExists(siteId, request.getPath())) {
        docService.addFolderIndex(siteId, request.getPath(), authorization.getUsername());
        apiMapResponse = new ApiMapResponse(Map.of("message", "folder created"));

      } else {
        throw new ValidationException(Collections
            .singletonList(new ValidationErrorImpl().key("folder").error("already exists")));
      }

    } else {

      updatePost(awsservice, request);

      ApiRequestHandlerResponse response = handler.post(event, authorization, awsservice, request);

      Map<String, Object> mapResponse = ((ApiMapResponse) response.getResponse()).getMap();

      new PresignedUrlsToS3Bucket(request).apply(mapResponse);

      apiMapResponse = new ApiMapResponse(mapResponse);
    }

    Map<String, Object> hashMap = new HashMap<>(apiMapResponse.getMap());
    hashMap.remove("headers");
    hashMap.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    return new ApiRequestHandlerResponse(SC_CREATED, new ApiMapResponse(hashMap));
  }

  /**
   * Update {@link AddDocumentRequest} request object.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param o {@link AddDocumentRequest}
   */
  private void updatePost(final AwsServiceCache awsservice, final AddDocumentRequest o) {

    if (!isEmpty(o.getContent()) && isEmpty(o.getChecksum()) && !isEmpty(o.getChecksumType())) {
      S3PresignerService s3PresignerService = awsservice.getExtension(S3PresignerService.class);
      ChecksumAlgorithm checksumAlgorithm =
          s3PresignerService.getChecksumAlgorithm(o.getChecksumType());

      byte[] bytes =
          o.isBase64() ? Base64.getDecoder().decode(o.getContent().getBytes(StandardCharsets.UTF_8))
              : o.getContent().getBytes(StandardCharsets.UTF_8);
      String checksum = s3PresignerService.calculateChecksumAsHex(checksumAlgorithm, bytes);
      o.setChecksum(checksum);
    }
  }

  /**
   * Transform {@link String} to {@link ZonedDateTime}.
   *
   * @param awsservice {@link AwsServiceCache}
   * @param documentService {@link DocumentService}
   * @param dateString {@link String}
   * @param tz {@link String}
   * @return {@link Date}
   * @throws BadException BadException
   */
  private ZonedDateTime transformToDate(final AwsServiceCache awsservice,
      final DocumentService documentService, final String dateString, final String tz)
      throws BadException {

    ZonedDateTime date;
    Logger logger = awsservice.getLogger();

    if (dateString != null) {
      try {

        date = DateUtil.toDateTimeFromString(dateString, tz);
        logger.trace("searching using date parameter: " + dateString + " and tz " + tz);

      } catch (ZoneRulesException e) {
        throw new BadException("Invalid date string: " + dateString);
      }
    } else {

      date = documentService.findMostDocumentDate();

      if (date == null) {
        date = ZonedDateTime.now();
        logger.trace("searching using default date: " + date);

      } else {
        logger.trace("searching using Most Recent Document Date: " + date);
      }
    }

    return date;
  }

  private void validatePost(final AddDocumentRequest item) throws ValidationException {

    boolean isFolder = isFolder(item);

    boolean emptyContent = isEmpty(item.getContent());
    boolean emptyDeepLink = isEmpty(item.getDeepLinkPath());
    Collection<ValidationError> errors = new ArrayList<>();

    if (!isFolder && emptyContent && notNull(item.getDocuments()).isEmpty() && emptyDeepLink) {
      errors.add(new ValidationErrorImpl()
          .error("either 'content', 'documents', or 'deepLinkPath' are required"));
    } else if (!emptyDeepLink && !emptyContent) {
      errors
          .add(new ValidationErrorImpl().error("both 'content', and 'deepLinkPath' cannot be set"));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
