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
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.documents.DocumentCacheKey;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiRequestHandlerInterceptor;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.folders.FolderPermissionValidate;
import com.formkiq.stacks.dynamodb.folders.StringToFolder;
import com.formkiq.strings.Strings;


/**
 * {@link ApiRequestHandlerInterceptor} for /documents request handler.
 */
public class DocumentsRequestHandlerInterceptor implements ApiRequestHandlerInterceptor {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   *
   * @param awsServiceCache {@link AwsServiceCache}
   */
  public DocumentsRequestHandlerInterceptor(final AwsServiceCache awsServiceCache) {
    this.db = awsServiceCache.getExtension(DynamoDbService.class);
  }

  @Override
  public ApiRequestHandlerResponse afterProcessRequest(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final ApiRequestHandlerResponse response) {

    String siteId = authorization.getSiteId();
    if (!authorization.isAdmin(siteId) && isDocumentUrl(event)) {
      DocumentItem item = authorization.getCacheObject(DocumentCacheKey.CACHE_DOCUMENT.name());
      if (item != null && item.getPath() != null) {
        Strings.SplitResult r = Strings.lastIndexOf(item.getPath(), "/");
        String parent = r != null ? r.before() : "";
        new FolderPermissionValidate(db, ApiPermission.READ).apply(siteId,
            new StringToFolder().apply(parent));
      }
    }

    return response;
  }

  @Override
  public void beforeProcessRequest(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) {
    // empty
  }

  private boolean isDocumentUrl(final ApiGatewayRequestEvent event) {
    String resource = event.getResource();
    return "/documents/{documentId}/url".equals(resource)
        || "/documents/{documentId}/content".equals(resource);
  }
}
