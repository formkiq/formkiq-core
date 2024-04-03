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
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.GlobalIndexService;

/** {@link ApiGatewayRequestHandler} for "/indices/{type}/{key}". */
public class IndicesRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil, DbKeys {

  /** {@link IndicesRequestHandler} URL. */
  public static final String URL = "/indices/{indexType}/{indexKey}";

  /** {@link GlobalIndexService}. */
  private GlobalIndexService writer;

  /**
   * constructor.
   *
   */
  public IndicesRequestHandler() {}

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String type = event.getPathParameters().get("indexType");
    String indexKey = event.getPathParameters().get("indexKey");

    if ("folder".equals(type)) {

      int pos = indexKey.indexOf(TAG_DELIMINATOR);
      if (pos > -1) {

        String parentId = indexKey.substring(0, pos);
        String path = indexKey.substring(pos + 1);

        FolderIndexProcessor ip = awsServices.getExtension(FolderIndexProcessor.class);

        try {
          ip.deleteEmptyDirectory(siteId, parentId, path);
        } catch (IOException e) {
          throw new BadException("Folder not empty");
        }

      } else {
        throw new BadException("invalid indexKey");
      }

    } else if ("tags".equals(type)) {

      initIndexService(awsServices);
      this.writer.deleteTagIndex(siteId, indexKey);

    } else {
      throw new BadException("invalid 'indexType' parameter");
    }

    ApiMapResponse resp = new ApiMapResponse();
    resp.setMap(Map.of("message", "Folder deleted"));
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  private void initIndexService(final AwsServiceCache awsServiceCache) {

    if (this.writer == null) {
      DynamoDbConnectionBuilder connection =
          awsServiceCache.getExtension(DynamoDbConnectionBuilder.class);
      this.writer =
          new GlobalIndexService(connection, awsServiceCache.environment("DOCUMENTS_TABLE"));
    }
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }
}
