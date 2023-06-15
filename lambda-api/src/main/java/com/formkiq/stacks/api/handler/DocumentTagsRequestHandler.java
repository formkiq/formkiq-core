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

import static com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.stacks.api.ApiDocumentTagItemResponse;
import com.formkiq.stacks.api.ApiDocumentTagsItemResponse;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentTagValidatorImpl;
import com.formkiq.stacks.dynamodb.DocumentTags;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/tags". */
public class DocumentTagsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentTagsRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    DocumentService documentService = awsservice.getExtension(DocumentService.class);
    CacheService cacheService = awsservice.getExtension(CacheService.class);

    ApiPagination pagination = getPagination(cacheService, event);
    int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);

    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String siteId = authorizer.getSiteId();
    String documentId = event.getPathParameters().get("documentId");

    PaginationResults<DocumentTag> results =
        documentService.findDocumentTags(siteId, documentId, ptoken, limit);

    results.getResults().forEach(r -> r.setDocumentId(null));

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);
    List<DocumentTag> tags = subList(results.getResults(), limit);

    List<ApiDocumentTagItemResponse> list = tags.stream().map(t -> {
      ApiDocumentTagItemResponse r = new ApiDocumentTagItemResponse();

      r.setDocumentId(t.getDocumentId());
      r.setInsertedDate(t.getInsertedDate());
      r.setKey(t.getKey());
      r.setValue(t.getValue());
      r.setValues(t.getValues());
      r.setUserId(t.getUserId());
      r.setType(t.getType() != null ? t.getType().name().toLowerCase() : null);

      return r;
    }).collect(Collectors.toList());

    ApiDocumentTagsItemResponse resp = new ApiDocumentTagsItemResponse();
    resp.setTags(list);
    resp.setPrevious(current.getPrevious());
    resp.setNext(current.hasNext() ? current.getNext() : null);

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/tags";
  }

  /**
   * Is Valid {@link DocumentTag}.
   * 
   * @param tag {@link DocumentTag}
   * @return boolean
   */
  private boolean isValid(final DocumentTag tag) {
    return tag.getKey() != null && tag.getKey().length() > 0;
  }

  /**
   * Is Valid {@link DocumentTags}.
   * 
   * @param tags {@link DocumentTags}
   * @return boolean
   */
  private boolean isValid(final DocumentTags tags) {
    List<DocumentTag> list =
        tags != null && tags.getTags() != null ? tags.getTags() : Collections.emptyList();
    return !list.isEmpty()
        ? !list.stream().map(t -> Boolean.valueOf(isValid(t))).filter(b -> b.equals(Boolean.FALSE))
            .findFirst().isPresent()
        : false;
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    final String siteId = authorizer.getSiteId();
    final String documentId = event.getPathParameters().get("documentId");

    DocumentTag tag = fromBodyToObject(logger, event, DocumentTag.class);
    DocumentTags tags = fromBodyToObject(logger, event, DocumentTags.class);

    boolean tagValid = isValid(tag);
    boolean tagsValid = isValid(tags);

    if (!tagValid && !tagsValid) {
      throw new BadException("invalid json body");
    }

    if (tagsValid) {
      int size = tags.getTags().stream().map(t -> t.getKey()).collect(Collectors.toSet()).size();
      if (size != tags.getTags().size()) {
        throw new BadException("Tag key can only be included once in body; "
            + "please use 'values' to assign multiple tag values to that key");
      }
    }

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    DocumentItem item = documentService.findDocument(siteId, documentId);
    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    if (!tagsValid) {
      tags = new DocumentTags();
      tags.setTags(Arrays.asList(tag));
    }

    String userId = getCallingCognitoUsername(event);

    tags.getTags().forEach(t -> {
      t.setType(DocumentTagType.USERDEFINED);
      t.setInsertedDate(new Date());
      t.setUserId(userId);
    });

    Collection<ValidationError> tagErrors = new DocumentTagValidatorImpl().validate(tags);
    if (!tagErrors.isEmpty()) {
      throw new ValidationException(tagErrors);
    }

    documentService.deleteDocumentTag(siteId, documentId, "untagged");

    Collection<DocumentTag> newTags = tagSchemaValidation(awsservice, siteId, tags, item, userId);

    List<DocumentTag> allTags = new ArrayList<>(tags.getTags());
    allTags.addAll(newTags);

    documentService.addTags(siteId, documentId, allTags, null);

    ApiResponse resp = tagsValid ? new ApiMessageResponse("Created Tags.")
        : new ApiMessageResponse("Created Tag '" + tag.getKey() + "'.");

    return new ApiRequestHandlerResponse(SC_CREATED, resp);
  }

  /**
   * Added Any TagSchema Composite Keys and Validate.
   * 
   * @param coreServices {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param tags {@link DocumentTags}
   * @param item {@link DocumentItem}
   * @param userId {@link String}
   * @return {@link Collection} {@link DocumentTag}
   * @throws ValidationException ValidationException
   */
  private Collection<DocumentTag> tagSchemaValidation(final AwsServiceCache coreServices,
      final String siteId, final DocumentTags tags, final DocumentItem item, final String userId)
      throws ValidationException {

    DocumentTagSchemaPlugin plugin = coreServices.getExtension(DocumentTagSchemaPlugin.class);

    Collection<ValidationError> errors = new ArrayList<>();

    Collection<DocumentTag> newTags =
        plugin.addCompositeKeys(siteId, item, tags.getTags(), userId, false, errors);

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return newTags;
  }
}
