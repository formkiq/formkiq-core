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

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentTag;
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
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
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
    String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact documentArtifact = new DocumentArtifact(documentId, artifactId);

    String tagKey = event.getPathParameter("tagKey");

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentTag docTag = documentService.findDocumentTag(siteId, documentArtifact, tagKey);
    if (docTag == null) {
      throw new NotFoundException("Tag '" + tagKey + "' not found.");
    }

    DocumentRecord document = documentService.findDocument(siteId, documentArtifact);
    throwIfNull(document, new DocumentNotFoundException(documentId));

    List<String> tags = List.of(tagKey);

    documentService.removeTags(siteId, documentArtifact, tags);

    return ApiRequestHandlerResponse.builder().ok()
        .body("message", "Removed '" + tagKey + "' from document '" + documentId + "'.").build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact document = new DocumentArtifact(documentId, artifactId);

    String tagKey = event.getPathParameter("tagKey");
    String siteId = authorization.getSiteId();

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    DocumentRecord item = documentService.findDocument(siteId, document);
    throwIfNull(item, new DocumentNotFoundException(documentId));

    DocumentTag tag = documentService.findDocumentTag(siteId, document, tagKey);

    if (tag == null) {
      throw new NotFoundException("Tag " + tagKey + " not found.");
    }

    Collection<ValidationError> tagErrors = new DocumentTagValidatorImpl().validate(tag);
    if (!tagErrors.isEmpty()) {
      throw new ValidationException(tagErrors);
    }

    Map<String, Object> values = new HashMap<>();
    values.put("key", tagKey);
    values.put("value", tag.getValue());
    values.put("values", tag.getValues());
    values.put("insertedDate", tag.getInsertedDate());
    values.put("userId", tag.getUserId());
    values.put("type", tag.getType() != null ? tag.getType().name().toLowerCase() : null);
    values.put("documentId", tag.getDocumentId());

    return ApiRequestHandlerResponse.builder().ok().body(values).build();
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

    final String documentId = event.getPathParameter("documentId");
    String artifactId = event.getQueryStringParameter("artifactId");
    DocumentArtifact documentArtifact = new DocumentArtifact(documentId, artifactId);

    final String tagKey = event.getPathParameter("tagKey");

    Map<String, Object> body = JsonToObject.fromJson(awsservice, event, Map.class);
    String value = body != null ? (String) body.getOrDefault("value", null) : null;
    List<String> values = body != null ? (List<String>) body.getOrDefault("values", null) : null;

    if (value == null && values == null) {
      throw new BadException("request body is invalid");
    }

    String siteId = authorization.getSiteId();

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentRecord document = documentService.findDocument(siteId, documentArtifact);
    throwIfNull(document, new DocumentNotFoundException(documentId));

    DocumentTag tag = documentService.findDocumentTag(siteId, documentArtifact, tagKey);
    throwIfNull(tag, new NotFoundException("Tag " + tagKey + " not found."));

    // if trying to change from tag VALUE to VALUES or VALUES to VALUE
    if (isTagValueTypeChanged(tag, value, values)) {
      documentService.removeTags(siteId, documentArtifact, List.of(tagKey));
    }

    Date now = new Date();
    String userId = authorization.getUsername();

    tag = new DocumentTag(null, tagKey, value, now, userId);
    if (!notNull(values).isEmpty()) {
      tag.setValue(null);
      tag.setValues(values);
    }

    List<DocumentTag> tags = new ArrayList<>(List.of(tag));

    validateTags(tags);

    documentService.addTags(siteId, documentArtifact, tags, null);

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
