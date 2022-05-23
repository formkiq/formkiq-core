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
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.AwsServiceCache;
import com.formkiq.aws.services.lambda.NotFoundException;
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
    DocumentService documentService = awsservice.documentService();

    boolean removed = documentService.removeTag(siteId, documentId, tagKey, tagValue);
    if (!removed) {
      throw new NotFoundException("Tag/Value combination not found.");
    }

    ApiResponse resp = new ApiMessageResponse("Removed Tag from document '" + documentId + "'.");

    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public String getRequestUrl() {
    return "/documents/{documentId}/tags/{tagKey}/{tagValue}";
  }
}
