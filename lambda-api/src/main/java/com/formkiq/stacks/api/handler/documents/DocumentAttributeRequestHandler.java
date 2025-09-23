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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/attributes/{attributeKey}". */
public class DocumentAttributeRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentAttributeRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccessDelete(authorization, siteId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    if (documentService.deleteDocumentAttribute(siteId, documentId, attributeKey,
        AttributeValidationType.FULL, validationAccess).isEmpty()) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document '" + documentId + "'");
    }

    return ApiRequestHandlerResponse.builder().ok().body("message",
        "attribute '" + attributeKey + "' removed from document '" + documentId + "'").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");
    String siteId = authorization.getSiteId();

    verifyDocument(awsservice, siteId, documentId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    List<DocumentAttributeRecord> list =
        documentService.findDocumentAttribute(siteId, documentId, attributeKey);

    Collection<Map<String, Object>> map = new DocumentAttributeRecordToMap(true).apply(list);

    if (map.isEmpty()) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document '" + documentId + "'");
    }

    return ApiRequestHandlerResponse.builder().ok().body("attribute", map.iterator().next())
        .build();
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.isAdminOrGovern(siteId);
    return isAdmin ? AttributeValidationAccess.ADMIN_SET_ITEM : AttributeValidationAccess.SET_ITEM;
  }

  private AttributeValidationAccess getAttributeValidationAccessDelete(
      final ApiAuthorization authorization, final String siteId) {
    boolean isAdmin = authorization.isAdminOrGovern(siteId);
    return isAdmin ? AttributeValidationAccess.ADMIN_DELETE : AttributeValidationAccess.DELETE;
  }

  private Collection<DocumentAttributeRecord> getDocumentAttributesFromRequest(
      final ApiGatewayRequestEvent event, final AwsServiceCache awsservice, final String siteId,
      final String documentId, final String attributeKey) throws BadException, ValidationException {

    DocumentAttributeValueRequest request =
        JsonToObject.fromJson(awsservice, event, DocumentAttributeValueRequest.class);
    request.validate();

    AddDocumentAttributeValue a = request.attribute();
    AddDocumentAttribute addAttribute = new AddDocumentAttributeStandard(attributeKey,
        a.stringValue(), a.stringValues(), a.numberValue(), a.numberValues(), a.booleanValue());

    return new AddDocumentAttributeToDocumentAttributeRecord(awsservice, siteId, documentId)
        .apply(addAttribute);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes/{attributeKey}";
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");
    String siteId = authorization.getSiteId();

    verifyDocument(awsservice, siteId, documentId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    Collection<DocumentAttributeRecord> documentAttributes =
        getDocumentAttributesFromRequest(event, awsservice, siteId, documentId, attributeKey);

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccess(authorization, siteId);

    AttributeValidationType type = getValidationType(documentAttributes);
    documentService.saveDocumentAttributes(siteId, documentId, documentAttributes, type,
        validationAccess);

    return ApiRequestHandlerResponse.builder().ok().body("message",
        "Updated attribute '" + attributeKey + "' on document '" + documentId + "'").build();
  }

  private static AttributeValidationType getValidationType(
      final Collection<DocumentAttributeRecord> documentAttributes) {
    Optional<DocumentAttributeRecord> o = documentAttributes.stream()
        .filter(a -> DocumentAttributeValueType.CLASSIFICATION.equals(a.getValueType())).findAny();
    return o.isPresent() ? AttributeValidationType.FULL : AttributeValidationType.PARTIAL;
  }

  private void verifyDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    if (!ds.exists(siteId, documentId)) {
      throw new DocumentNotFoundException(documentId);
    }
  }
}
