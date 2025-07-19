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
package com.formkiq.stacks.api.handler.indices;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.folders.FolderIndexRecord.INDEX_FILE_SK;

import java.io.IOException;
import java.util.Map;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.handler.IndexKeyToString;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.GlobalIndexService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** {@link ApiGatewayRequestHandler} for "/indices/{type}/{indexKey}". */
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
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String type = event.getPathParameters().get("indexType");
    String indexKey = event.getPathParameters().get("indexKey");
    String message = "Folder deleted";

    boolean deleted = false;

    if ("folder".equals(type)) {

      FolderIndexProcessor ip = awsServices.getExtension(FolderIndexProcessor.class);

      indexKey = new IndexKeyToString().apply(indexKey);
      int pos = indexKey.indexOf(TAG_DELIMINATOR);
      if (pos > -1) {

        String parentId = indexKey.substring(0, pos);
        String path = indexKey.substring(pos + 1);

        try {
          deleted = ip.deleteEmptyDirectory(siteId, parentId, path);
        } catch (IOException e) {
          throw new BadException("Folder not empty");
        }

      } else {

        try {
          Map<String, Object> index = ip.getIndex(siteId, indexKey);

          String pk = (String) index.get(PK);
          String sk = (String) index.get(SK);

          if (!isEmpty(pk) && !isEmpty(sk) && sk.startsWith(INDEX_FILE_SK)) {
            DynamoDbService db = awsServices.getExtension(DynamoDbService.class);
            deleted = db.deleteItem(AttributeValue.fromS(pk), AttributeValue.fromS(sk));
            message = "File deleted";
          }
        } catch (IOException e) {
          // ignore
        }
      }

    } else if ("tags".equals(type)) {

      initIndexService(awsServices);
      this.writer.deleteTagIndex(siteId, indexKey);
      deleted = true;
    }

    if (!deleted) {
      throw new NotFoundException("invalid indexKey '" + indexKey + "'");
    }

    ApiMapResponse resp = new ApiMapResponse();
    resp.setMap(Map.of("message", message));
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
