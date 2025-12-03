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

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.services.lambda.AdminOrGovernRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.validation.ValidationBuilder;

import java.util.List;
import java.util.Optional;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/retention/mustRetain". */
public class DocumentIdRetentionMustRetainRequestHandler
    implements ApiGatewayRequestHandler, AdminOrGovernRequestHandler {

  /**
   * constructor.
   *
   */
  public DocumentIdRetentionMustRetainRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/retention/mustRetain";
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    String siteId = authorization.getSiteId();
    boolean access = authorization.isAdminOrGovern(siteId);
    return Optional.of(access);
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameter("documentId");

    SetMustRetainRequest request =
        JsonToObject.fromJson(awsservice, event, SetMustRetainRequest.class);
    validate(request);

    DocumentService service = awsservice.getExtension(DocumentService.class);

    if (Boolean.TRUE.equals(request.mustRetain())) {
      String userId = authorization.getUsername();
      DocumentAttributeRecord attribute = new DocumentAttributeRecord()
          .setKey(AttributeKeyReserved.MUST_RETAIN.getKey()).setUserId(userId)
          .setDocumentId(documentId).setBooleanValue(request.mustRetain()).updateValueType();
      service.saveDocumentAttributes(siteId, documentId, List.of(attribute),
          AttributeValidationType.FULL, AttributeValidationAccess.ADMIN_CREATE);

      return ApiRequestHandlerResponse.builder().ok()
          .body("message", "Must Retain attribute set on document").build();
    }

    service.deleteDocumentAttribute(siteId, documentId, AttributeKeyReserved.MUST_RETAIN.getKey(),
        AttributeValidationType.FULL, AttributeValidationAccess.ADMIN_DELETE);
    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Must Retain attribute removed from document").build();
  }

  private void validate(final SetMustRetainRequest request) {
    ValidationBuilder vb = new ValidationBuilder();
    if (request.mustRetain() == null) {
      vb.addError("mustRetain", "Must Retain attribute is required");
    }
    vb.check();
  }
}
