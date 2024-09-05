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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationAccess;

/**
 * {@link ApiGatewayRequestHandler} for
 * 
 * "/documents/{documentId}/attributes/{attributeKey}/{attributeValue}".
 */
public class DocumentAttributesValueRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentAttributesValueRequestHandler() {}

  private AttributeValidationAccess getAttributeValidationAccessDelete(
      final ApiAuthorization authorization, final String siteId) {

    boolean isAdmin = authorization.getPermissions(siteId).contains(ApiPermission.ADMIN);
    return isAdmin ? AttributeValidationAccess.ADMIN_DELETE : AttributeValidationAccess.DELETE;
  }

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String documentId = event.getPathParameters().get("documentId");
    String attributeKey = event.getPathParameters().get("attributeKey");
    String attributeValue = event.getPathParameters().get("attributeValue");

    AttributeValidationAccess validationAccess =
        getAttributeValidationAccessDelete(authorization, siteId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    if (!documentService.deleteDocumentAttributeValue(siteId, documentId, attributeKey,
        attributeValue, validationAccess)) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document ' " + documentId + "'");
    }

    ApiResponse resp = new ApiMessageResponse("attribute value '" + attributeValue
        + "' removed from attribute '" + attributeKey + "', document '" + documentId + "'");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes/{attributeKey}/{attributeValue}";
  }
}
