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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.attributes.AttributeValidationAccess;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.attributes.AttributeValidationType;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequenceService;
import com.formkiq.stacks.dynamodb.numbering.NumberingSequenceValue;

/** Handler for generating a document attribute from a numbering sequence. */
public class DocumentAttributeGenerateRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Constructor. */
  public DocumentAttributeGenerateRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes/{attributeKey}/generate";
  }

  private AttributeValidationAccess getValidationAccess(final ApiAuthorization authorization,
      final String siteId) {
    return authorization.isAdminOrGovern(siteId) ? AttributeValidationAccess.ADMIN_SET_ITEM
        : AttributeValidationAccess.SET_ITEM;
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {
    String siteId = authorization.getSiteId();
    String documentId = getPathParameter(event, "documentId");
    String attributeKey = getPathParameter(event, "attributeKey");
    DocumentArtifact document = DocumentArtifact.of(documentId, null);
    DocumentService documentService = awsServices.getExtension(DocumentService.class);

    if (!documentService.exists(siteId, document)) {
      throw new DocumentNotFoundException(documentId);
    }

    NumberingSequenceService sequenceService =
        awsServices.getExtension(NumberingSequenceService.class);
    if (sequenceService.find(siteId, attributeKey) == null) {
      throw new NotFoundException("Numbering sequence '" + attributeKey + "' not found");
    }

    List<DocumentAttributeRecord> existing =
        documentService.findDocumentAttribute(siteId, document, attributeKey);
    DocumentAttributeRecord attribute;
    NumberingSequenceValue generated;
    if (!existing.isEmpty()) {
      attribute = existing.getFirst();
      generated = sequenceService.parse(siteId, attributeKey, attribute.getStringValue());
      if (generated.sequence() == null) {
        throw new BadException("Existing attribute '" + attributeKey
            + "' does not match its numbering sequence pattern");
      }
    } else {
      generated = sequenceService.next(siteId, attributeKey);
      attribute = new DocumentAttributeRecord().setDocument(document).setKey(attributeKey)
          .setStringValue(generated.value()).setValueType(DocumentAttributeValueType.STRING)
          .setInsertedDate(new Date()).setUserId(authorization.getUsername());
      documentService.saveDocumentAttributes(siteId, document, List.of(attribute),
          AttributeValidationType.PARTIAL, getValidationAccess(authorization, siteId));
    }

    Collection<Map<String, Object>> attributes =
        new DocumentAttributeRecordToMap(true).apply(siteId, List.of(attribute));
    Map<String, Object> response = new HashMap<>();
    response.put("attribute", attributes.iterator().next());
    response.put("sequence", generated.sequence());
    response.put("period", generated.period());
    return ApiRequestHandlerResponse.builder().ok().body(response).build();
  }
}
