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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.base64.StringToBase64Encoder;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiPagination;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.dynamodb.cache.CacheService;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.FolderIndexRecord;
import com.formkiq.stacks.dynamodb.FolderIndexRecordExtended;

/** {@link ApiGatewayRequestHandler} for "/folders". */
public class FoldersRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public FoldersRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    DynamicObject o = fromBodyToDynamicObject(event);
    String path = o.getString("path");
    if (Strings.isEmpty(path)) {
      throw new BadException("missing 'path' parameters");
    }

    if (!path.endsWith("/")) {
      path += "/";
    }

    String siteId = authorization.getSiteId();
    FolderIndexProcessor indexProcessor = awsservice.getExtension(FolderIndexProcessor.class);
    List<FolderIndexRecord> record =
        indexProcessor.createFolders(siteId, path, authorization.getUsername());

    StringToBase64Encoder encoder = new StringToBase64Encoder();
    List<Map<String, String>> list = record.stream().map(r -> {
      String indexKey = encoder.apply(r.createIndexKey(siteId));
      return Map.of("folder", r.path(), "indexKey", indexKey);
    }).toList();

    ApiMapResponse resp = new ApiMapResponse(
        Map.of("message", "created folder", "indexKey", list.get(list.size() - 1).get("indexKey")));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice) throws Exception {

    CacheService cacheService = awsservice.getExtension(CacheService.class);
    ApiPagination pagination = getPagination(cacheService, event);
    int limit =
        pagination != null ? pagination.getLimit() : getLimit(awsservice.getLogger(), event);
    PaginationMapToken ptoken = pagination != null ? pagination.getStartkey() : null;

    String siteId = authorization.getSiteId();
    DocumentSearchService documentSearchService =
        awsservice.getExtension(DocumentSearchService.class);

    String indexKey = getIndexKey(event, awsservice, siteId);

    PaginationResults<DynamicDocumentItem> results =
        documentSearchService.findInFolder(siteId, indexKey, ptoken, limit);

    ApiPagination current =
        createPagination(cacheService, event, pagination, results.getToken(), limit);

    List<DynamicDocumentItem> documents = subList(results.getResults(), limit);

    Map<String, Object> map = new HashMap<>();
    map.put("documents", documents);
    map.put("previous", current.getPrevious());
    map.put("next", current.hasNext() ? current.getNext() : null);

    ApiMapResponse resp = new ApiMapResponse(map);
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  private String getIndexKey(final ApiGatewayRequestEvent event, final AwsServiceCache awsservice,
      final String siteId) throws NotFoundException {

    String path = event.getQueryStringParameter("path");
    String indexKey = new IndexKeyToString().apply(event.getQueryStringParameter("indexKey"));

    if (!Strings.isEmpty(path)) {
      FolderIndexProcessor indexProcessor = awsservice.getExtension(FolderIndexProcessor.class);

      List<FolderIndexRecordExtended> folders =
          indexProcessor.get(siteId, path, "folder", "", new Date());
      FolderIndexRecordExtended folder = Objects.last(folders);

      if (folder != null && folder.record() != null) {
        indexKey = folder.record().createIndexKey(siteId);
      }
    }

    if (indexKey == null) {
      indexKey = "";
    }

    return indexKey;
  }

  @Override
  public String getRequestUrl() {
    return "/folders";
  }
}
