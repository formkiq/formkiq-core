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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_PAYMENT;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchResponseFields;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiPermission;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.typesense.TypeSenseService;
import com.formkiq.module.typesense.TypeSenseServiceImpl;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.stacks.api.QueryRequest;
import com.formkiq.stacks.api.QueryRequestValidator;
import com.formkiq.stacks.dynamodb.DocumentItemToDynamicDocumentItem;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/** {@link ApiGatewayRequestHandler} for "/search". */
public class SearchRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Maximum number of Document Ids that can be sent. */
  private static final int MAX_DOCUMENT_IDS = 100;

  /**
   * constructor.
   *
   */
  public SearchRequestHandler() {}

  @Override
  public String getRequestUrl() {
    return "/search";
  }

  /**
   * Get Response Tags.
   * 
   * @param documentService {@link DocumentService}
   * @param siteId {@link String}
   * @param responseFields {@link SearchResponseFields}
   * @param documents {@link List} {@link DynamicDocumentItem}
   * @return {@link Map}
   */
  private Map<String, Collection<DocumentTag>> getResponseTags(
      final DocumentService documentService, final String siteId,
      final SearchResponseFields responseFields, final List<DynamicDocumentItem> documents) {

    Map<String, Collection<DocumentTag>> map = Collections.emptyMap();

    if (responseFields != null && !Objects.notNull(responseFields.tags()).isEmpty()) {

      Set<String> documentIds =
          documents.stream().map(d -> d.getDocumentId()).collect(Collectors.toSet());

      map = documentService.findDocumentsTags(siteId, documentIds, responseFields.tags());
    }

    return map;
  }

  @Override
  public Optional<Boolean> isAuthorized(final AwsServiceCache awsservice, final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {
    boolean access = authorization.getPermissions().contains(ApiPermission.READ);
    return Optional.of(Boolean.valueOf(access));
  }

  private boolean isEnterpriseFeature(final AwsServiceCache serviceCache, final QueryRequest q) {

    return q.query().tag() == null && q.query().meta() == null && isEmpty(q.query().text())
        && !serviceCache.getExtension(DocumentTagSchemaPlugin.class).isActive();
  }

  /**
   * Merge Response Tags into Response.
   * 
   * @param documents {@link List} {@link DynamicDocumentItem}
   * @param responseTags {@link Map} {@link DocumentTag}
   */
  private void mergeResponseTags(final List<DynamicDocumentItem> documents,
      final Map<String, Collection<DocumentTag>> responseTags) {

    if (!responseTags.isEmpty()) {

      documents.forEach(doc -> {

        Collection<DocumentTag> tags = Objects.notNull(responseTags.get(doc.getDocumentId()));

        Map<String, Object> map = new HashMap<>();

        tags.forEach(tag -> {
          if (tag.getValues() != null) {
            map.put(tag.getKey(), tag.getValues());
          } else {
            map.put(tag.getKey(), tag.getValue());
          }
        });

        doc.put("tags", map);
      });
    }
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    ApiRequestHandlerResponse response = null;

    QueryRequest q = fromBodyToObject(event, QueryRequest.class);

    validatePost(q);

    DocumentService documentService = awsservice.getExtension(DocumentService.class);

    if (isEnterpriseFeature(awsservice, q)) {

      ApiMapResponse resp = new ApiMapResponse();
      resp.setMap(Map.of("message", "Feature only available in FormKiQ Enterprise"));
      response = new ApiRequestHandlerResponse(SC_PAYMENT, resp);

    } else {

      CacheService cacheService = awsservice.getExtension(CacheService.class);
      ApiPagination pagination = getPagination(cacheService, event);
      int limit = pagination != null ? pagination.getLimit() : getLimit(logger, event);
      PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

      Collection<String> documentIds = q.query().documentIds();
      if (documentIds != null) {
        if (documentIds.size() > MAX_DOCUMENT_IDS) {
          throw new BadException("Maximum number of DocumentIds is " + MAX_DOCUMENT_IDS);
        }

        if (!getQueryParameterMap(event).containsKey("limit")) {
          limit = documentIds.size();
        }
      }

      String siteId = authorization.getSiteId();
      DocumentSearchService documentSearchService =
          awsservice.getExtension(DocumentSearchService.class);

      PaginationResults<DynamicDocumentItem> results =
          query(awsservice, documentSearchService, siteId, q, ptoken, limit);

      ApiPagination current =
          createPagination(cacheService, event, pagination, results.getToken(), limit);

      List<DynamicDocumentItem> documents = subList(results.getResults(), limit);

      Map<String, Collection<DocumentTag>> responseTags =
          getResponseTags(documentService, siteId, q.responseFields(), documents);
      mergeResponseTags(documents, responseTags);

      Map<String, Object> map = new HashMap<>();
      map.put("documents", documents);
      map.put("previous", current.getPrevious());
      map.put("next", current.hasNext() ? current.getNext() : null);

      ApiMapResponse resp = new ApiMapResponse(map);
      response = new ApiRequestHandlerResponse(SC_OK, resp);
    }

    return response;
  }

  /**
   * Query Typesense or DynamoDb.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param documentSearchService {@link DocumentSearchService}
   * @param siteId {@link String}
   * @param q {@link QueryRequest}
   * @param ptoken {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   * @throws IOException IOException
   * @throws BadException BadException
   */
  private PaginationResults<DynamicDocumentItem> query(final AwsServiceCache awsservice,
      final DocumentSearchService documentSearchService, final String siteId, final QueryRequest q,
      final PaginationMapToken ptoken, final int limit) throws IOException, BadException {

    String text = q.query().text();
    PaginationResults<DynamicDocumentItem> results = null;

    if (!isEmpty(text)) {

      if (isEmpty(awsservice.environment("TYPESENSE_HOST"))
          || isEmpty(awsservice.environment("TYPESENSE_API_KEY"))) {
        throw new BadException("Fulltext search is not Enabled");
      }

      Region region = Region.of(awsservice.environment("AWS_REGION"));

      AwsCredentials awsCredentials = awsservice.getExtension(AwsCredentials.class);
      DocumentService docService = awsservice.getExtension(DocumentService.class);
      TypeSenseService ts = new TypeSenseServiceImpl(awsservice.environment("TYPESENSE_HOST"),
          awsservice.environment("TYPESENSE_API_KEY"), region, awsCredentials);

      List<String> documentIds = ts.searchFulltext(siteId, text, limit);

      List<DocumentItem> list = docService.findDocuments(siteId, documentIds);

      List<DynamicDocumentItem> docs =
          list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
              .collect(Collectors.toList()) : Collections.emptyList();

      results = new PaginationResults<>(docs, null);

    } else {

      results = documentSearchService.search(siteId, q.query(), ptoken, limit);
    }

    return results;
  }

  private void validatePost(final QueryRequest q) throws ValidationException {
    QueryRequestValidator validator = new QueryRequestValidator();
    Collection<ValidationError> errors = validator.validation(q);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }
}
