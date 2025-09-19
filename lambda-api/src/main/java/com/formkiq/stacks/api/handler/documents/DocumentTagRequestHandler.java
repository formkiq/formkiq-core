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

import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.ApiDocumentTagItemResponse;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/tags/{tagKey}". */
public class DocumentTagRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentTagRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    Map<String, String> map = event.getPathParameters();
    String documentId = map.get("documentId");
    String tagKey = map.get("tagKey");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentTag docTag = documentService.findDocumentTag(siteId, documentId, tagKey);
    if (docTag == null) {
      throw new NotFoundException("Tag '" + tagKey + "' not found.");
    }

    DocumentItem document = documentService.findDocument(siteId, documentId);
    throwIfNull(document, new DocumentNotFoundException(documentId));

    List<String> tags = List.of(tagKey);

    documentService.removeTags(siteId, documentId, tags);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Removed '" + tagKey + "' from document '" + documentId + "'.").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameters().get("documentId");
    String tagKey = event.getPathParameters().get("tagKey");
    String siteId = authorization.getSiteId();

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    DocumentItem item = documentService.findDocument(siteId, documentId);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    DocumentTag tag = documentService.findDocumentTag(siteId, documentId, tagKey);

    if (tag == null) {
      throw new NotFoundException("Tag " + tagKey + " not found.");
    }

    Collection<ValidationError> tagErrors = new DocumentTagValidatorImpl().validate(tag);
    if (!tagErrors.isEmpty()) {
      throw new ValidationException(tagErrors);
    }

    ApiDocumentTagItemResponse resp = new ApiDocumentTagItemResponse();
    resp.setKey(tagKey);
    resp.setValue(tag.getValue());
    resp.setValues(tag.getValues());
    resp.setInsertedDate(tag.getInsertedDate());
    resp.setUserId(tag.getUserId());
    resp.setType(tag.getType() != null ? tag.getType().name().toLowerCase() : null);
    resp.setDocumentId(tag.getDocumentId());

    return ApiRequestHandlerResponse.builder().ok().body(resp).build();
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/tags/{tagKey}";
  }

  /**
   * Is Changing Tag from Value to Values or Values to Value.
   * 
   * @param tag {@link DocumentTag}
   * @param value {@link String}
   * @param values {@link List} {@link String}
   * @return boolean
   */
  private boolean isTagValueTypeChanged(final DocumentTag tag, final String value,
      final List<String> values) {
    return (tag.getValue() != null && values != null) || (tag.getValues() != null && value != null);
  }

  @Override
  public ApiRequestHandlerResponse put(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    Map<String, String> map = event.getPathParameters();
    final String documentId = map.get("documentId");
    final String tagKey = map.get("tagKey");

    Map<String, Object> body = fromBodyToMap(event);
    String value = body != null ? (String) body.getOrDefault("value", null) : null;
    List<String> values = body != null ? (List<String>) body.getOrDefault("values", null) : null;

    if (value == null && values == null) {
      throw new BadException("request body is invalid");
    }

    String siteId = authorization.getSiteId();

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentItem document = documentService.findDocument(siteId, documentId);
    throwIfNull(document, new DocumentNotFoundException(documentId));

    DocumentTag tag = documentService.findDocumentTag(siteId, documentId, tagKey);
    throwIfNull(document, new NotFoundException("Tag " + tagKey + " not found."));

    // if trying to change from tag VALUE to VALUES or VALUES to VALUE
    if (isTagValueTypeChanged(tag, value, values)) {
      documentService.removeTags(siteId, documentId, List.of(tagKey));
    }

    Date now = new Date();
    String userId = authorization.getUsername();

    tag = new DocumentTag(null, tagKey, value, now, userId);
    if (values != null) {
      tag.setValue(null);
      tag.setValues(values);
    }

    List<DocumentTag> tags = new ArrayList<>(List.of(tag));

    validateTags(tags);

    documentService.addTags(siteId, documentId, tags, null);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Updated tag '" + tagKey + "' on document '" + documentId + "'.").build();
  }

  /**
   * Validate {@link DocumentTag}.
   * 
   * @param tags {@link List} {@link DocumentTag}
   * @throws ValidationException ValidationException
   */
  private void validateTags(final List<DocumentTag> tags) throws ValidationException {
    DocumentTagValidator validate = new DocumentTagValidatorImpl();
    Collection<ValidationError> errors = validate.validate(tags);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
