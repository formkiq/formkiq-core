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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.transformers.DocumentAttributeRecordToMap;
import com.formkiq.stacks.api.transformers.DocumentAttributeToDocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidation;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationErrorImpl;
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
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccessDelete(authorization, siteId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    if (documentService.deleteDocumentAttribute(siteId, documentId, attributeKey,
        AttributeValidation.FULL, validationAccess).isEmpty()) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document '" + documentId + "'");
    }

    ApiResponse resp = new ApiMessageResponse(
        "attribute '" + attributeKey + "' removed from document '" + documentId + "'");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

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

    ApiMapResponse resp = new ApiMapResponse(Map.of("attribute", map.iterator().next()));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  private AttributeValidationAccess getAttributeValidationAccess(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.getPermissions(siteId).contains(ApiPermission.ADMIN);
    return isAdmin ? AttributeValidationAccess.ADMIN_UPDATE : AttributeValidationAccess.UPDATE;
  }

  private AttributeValidationAccess getAttributeValidationAccessDelete(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.getPermissions(siteId).contains(ApiPermission.ADMIN);
    return isAdmin ? AttributeValidationAccess.ADMIN_DELETE : AttributeValidationAccess.DELETE;
  }

  private Collection<DocumentAttributeRecord> getDocumentAttributesFromRequest(
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final String documentId, final String attributeKey)
      throws BadException, IOException, ValidationException {

    DocumentAttributeValueRequest request =
        fromBodyToObject(event, DocumentAttributeValueRequest.class);
    if (request.getAttribute() == null) {
      throw new ValidationException(
          Collections.singletonList(new ValidationErrorImpl().error("no attribute values found")));
    }

    Collection<DocumentAttributeRecord> documentAttributes =
        new DocumentAttributeToDocumentAttributeRecord(documentId, authorization.getUsername())
            .apply(request.getAttribute());
    documentAttributes.forEach(a -> a.setKey(attributeKey));

    return documentAttributes;
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes/{attributeKey}";
  }

  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");
    String siteId = authorization.getSiteId();

    verifyDocument(awsservice, siteId, documentId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    Collection<DocumentAttributeRecord> documentAttributes =
        getDocumentAttributesFromRequest(event, authorization, documentId, attributeKey);

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccess(authorization, siteId);

    documentService.saveDocumentAttributes(siteId, documentId, documentAttributes, true,
        AttributeValidation.PARTIAL, validationAccess);

    ApiResponse resp = new ApiMessageResponse(
        "Updated attribute '" + attributeKey + "' on document '" + documentId + "'");
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  private void verifyDocument(final AwsServiceCache awsservice, final String siteId,
      final String documentId) throws Exception {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    if (!ds.exists(siteId, documentId)) {
      throw new DocumentNotFoundException(documentId);
    }
  }
}
