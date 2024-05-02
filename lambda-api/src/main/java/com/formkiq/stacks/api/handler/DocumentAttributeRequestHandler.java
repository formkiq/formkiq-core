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

import static com.formkiq.aws.dynamodb.objects.Objects.throwIfNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.TagSchemaInterface;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagValidator;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.validation.ValidationError;
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

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    if (!documentService.deleteDocumentAttribute(siteId, documentId, attributeKey)) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document ' " + documentId + "'");
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

    verifyDocument(awsservice, event, siteId, documentId);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    List<DocumentAttributeRecord> list =
        documentService.findDocumentAttribute(siteId, documentId, attributeKey);

    Collection<Map<String, Object>> map = new DocumentAttributeRecordToMap().apply(list);

    if (map.isEmpty()) {
      throw new NotFoundException(
          "attribute '" + attributeKey + "' not found on document ' " + documentId + "'");
    }

    ApiMapResponse resp = new ApiMapResponse(Map.of("attribute", map.iterator().next()));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/attributes/{attributeKey}";
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

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse put(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    Map<String, String> map = event.getPathParameters();
    final String documentId = map.get("documentId");
    final String tagKey = map.get("tagKey");

    Map<String, Object> body = fromBodyToObject(event, Map.class);
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
      documentService.removeTags(siteId, documentId, Arrays.asList(tagKey));
    }

    Date now = new Date();
    String userId = authorization.getUsername();

    tag = new DocumentTag(null, tagKey, value, now, userId);
    if (values != null) {
      tag.setValue(null);
      tag.setValues(values);
    }

    List<DocumentTag> tags = new ArrayList<>(Arrays.asList(tag));

    if (document.getTagSchemaId() != null) {
      Collection<ValidationError> errors = new ArrayList<>();

      DocumentTagSchemaPlugin plugin = awsservice.getExtension(DocumentTagSchemaPlugin.class);
      TagSchemaInterface tagSchema = plugin.getTagSchema(siteId, document.getTagSchemaId());

      throwIfNull(tagSchema,
          new BadException("TagschemaId " + document.getTagSchemaId() + " not found"));

      plugin.updateInUse(siteId, tagSchema);

      Collection<DocumentTag> newTags = plugin.addCompositeKeys(tagSchema, siteId,
          document.getDocumentId(), tags, userId, false, errors);

      if (!errors.isEmpty()) {
        throw new ValidationException(errors);
      }

      tags.addAll(newTags);
    }


    validateTags(tags);

    documentService.addTags(siteId, documentId, tags, null);

    ApiResponse resp =
        new ApiMessageResponse("Updated tag '" + tagKey + "' on document '" + documentId + "'.");

    return new ApiRequestHandlerResponse(SC_OK, resp);
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

  private void verifyDocument(final AwsServiceCache awsservice, final ApiGatewayRequestEvent event,
      final String siteId, final String documentId) throws Exception {
    DocumentService ds = awsservice.getExtension(DocumentService.class);
    if (!ds.exists(siteId, documentId)) {
      throw new DocumentNotFoundException(documentId);
    }
  }
}
