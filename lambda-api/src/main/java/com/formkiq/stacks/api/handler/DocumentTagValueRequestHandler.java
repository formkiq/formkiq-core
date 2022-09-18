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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.validation.ValidationError;
import com.formkiq.plugins.validation.ValidationException;
import com.formkiq.stacks.api.CoreAwsServiceCache;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.DeleteFulltextTag;
import com.formkiq.stacks.client.requests.DeleteFulltextTagsRequest;
import com.formkiq.stacks.dynamodb.DocumentService;

/** {@link ApiGatewayRequestHandler} for "/documents/{documentId}/tags/{tagKey}/{tagValue}". */
public class DocumentTagValueRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public DocumentTagValueRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    Map<String, String> map = event.getPathParameters();
    String documentId = map.get("documentId");
    String tagKey = map.get("tagKey");
    String tagValue = map.get("tagValue");

    CoreAwsServiceCache cacheService = CoreAwsServiceCache.cast(awsservice);
    DocumentService documentService = cacheService.documentService();

    DocumentItem item = documentService.findDocument(siteId, documentId);
    if (item == null) {
      throw new NotFoundException("Document " + documentId + " not found.");
    }

    DocumentTagSchemaPlugin plugin = awsservice.getExtension(DocumentTagSchemaPlugin.class);
    Collection<ValidationError> errors =
        plugin.validateRemoveTags(siteId, item, Arrays.asList(tagKey));
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    boolean removed = documentService.removeTag(siteId, documentId, tagKey, tagValue);
    if (!removed) {
      throw new NotFoundException("Tag/Value combination not found.");
    }

    deleteFulltextTags(awsservice, siteId, documentId, tagKey, tagValue);

    ApiResponse resp = new ApiMessageResponse("Removed Tag from document '" + documentId + "'.");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  /**
   * Update Fulltext index if Module available.
   * 
   * @param awsservice {@link AwsServiceCache}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   */
  private void deleteFulltextTags(final AwsServiceCache awsservice, final String siteId,
      final String documentId, final String tagKey, final String tagValue)
      throws IOException, InterruptedException {

    if (awsservice.hasModule("fulltext")) {
      FormKiqClientV1 client = awsservice.getExtension(FormKiqClientV1.class);

      client.deleteFulltextTags(new DeleteFulltextTagsRequest().siteId(siteId)
          .documentId(documentId).tag(new DeleteFulltextTag().key(tagKey).value(tagValue)));
    }
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/tags/{tagKey}/{tagValue}";
  }
}
