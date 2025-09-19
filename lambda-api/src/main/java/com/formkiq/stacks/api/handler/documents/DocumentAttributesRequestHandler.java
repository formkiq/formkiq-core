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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/attributes". */
public class DocumentAttributesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentAttributesRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);
    int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsservice.getLogger(), event);

    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    verifyDocument(awsservice, siteId, documentId);

    PaginationResults<DocumentAttributeRecord> results =
        documentService.findDocumentAttributes(siteId, documentId, ptoken, limit);

    Collection<Map<String, Object>> list =
        new DocumentAttributeRecordToMap(true).apply(results.getResults());

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    Map<String, Object> m = new HashMap<>();
    m.put("attributes", list);

    if (current.hasNext()) {
      m.put("next", current.getNext());
    }

    return ApiRequestHandlerResponse.builder().ok().body(m).build();
  }

  private List<DocumentAttributeRecord> getDocumentAttributesFromRequest(
      final ApiGatewayRequestEvent event, final AwsServiceCache awsservice, final String siteId,
      final String documentId) throws BadException, ValidationException {

    AddDocumentAttributesRequest request =
        JsonToObject.fromJson(awsservice, event, AddDocumentAttributesRequest.class);
    request.validate();

    List<DocumentAttributeRecord> records = request.attributes().stream()
        .flatMap(
            a -> new AddDocumentAttributeToDocumentAttributeRecord(awsservice, siteId, documentId)
                .apply(a).stream())
        .toList();

    if (notNull(records).isEmpty()) {
      throw new ValidationException(
          Collections.singletonList(new ValidationErrorImpl().error("no attributes found")));
    }

    return records;
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    verifyDocument(awsservice, siteId, documentId);

    List<DocumentAttributeRecord> attributes =
        getDocumentAttributesFromRequest(event, awsservice, siteId, documentId);

    AttributeValidationAccess validationAccess = getAttributeValidationAccess(authorization, siteId,
        AttributeValidationAccess.ADMIN_CREATE, AttributeValidationAccess.CREATE);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    documentService.saveDocumentAttributes(siteId, documentId, attributes,
        AttributeValidationType.FULL, validationAccess);

    return ApiRequestHandlerResponse.builder().created()
        .body("message", "added attributes to documentId '" + documentId + "'").build();
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId,
      final AttributeValidationAccess admin, final AttributeValidationAccess regular) {
    return authorization.isAdminOrGovern(siteId) ? admin : regular;
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    verifyDocument(awsservice, siteId, documentId);

    List<DocumentAttributeRecord> attributes =
        getDocumentAttributesFromRequest(event, awsservice, siteId, documentId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    AttributeValidationAccess validationAccess = getAttributeValidationAccess(authorization, siteId,
        AttributeValidationAccess.ADMIN_SET, AttributeValidationAccess.SET);

    documentService.saveDocumentAttributes(siteId, documentId, attributes,
        AttributeValidationType.FULL, validationAccess);

    return ApiRequestHandlerResponse.builder().created()
        .body("message", "set attributes on documentId '" + documentId + "'").build();
  }

  private void verifyDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    if (!ds.exists(siteId, documentId)) {
      throw new DocumentNotFoundException(documentId);
    }
  }
}
