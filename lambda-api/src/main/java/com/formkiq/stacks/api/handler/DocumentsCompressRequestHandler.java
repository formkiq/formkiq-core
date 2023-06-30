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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.s3.PresignGetUrlConfig;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static java.util.Map.entry;

public class DocumentsCompressRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public String getRequestUrl() {
    return "/documents/compress";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {
    DynamicObject requestBodyObject = fromBodyToDynamicObject(logger, event);

    final String siteId = authorizer.getSiteId();
    final String compressionId = UUID.randomUUID().toString();
    final String compressionTaskS3Key = getS3Key(siteId, compressionId, false);
    S3Service s3 = awsServices.getExtension(S3Service.class);
    final String stagingBucket = awsServices.environment("STAGE_DOCUMENTS_S3_BUCKET");
    final String downloadUrl =
        getArchiveDownloadUrl(s3, stagingBucket, getS3Key(siteId, compressionId, true));
    final DynamicObject taskObject = getS3TaskObject(requestBodyObject, compressionId, downloadUrl);

    putObjectToStaging(s3, stagingBucket, compressionTaskS3Key, GSON.toJson(taskObject));
    ApiMapResponse response =
        new ApiMapResponse(Map.of("compressionId", compressionId, "downloadUrl", downloadUrl));
    return new ApiRequestHandlerResponse(SC_CREATED, response);
  }

  private String getS3Key(final String siteId, final String compressionId, final boolean isZip) {
    final String tempPrefix = String.format("tempfiles/%s", siteId == null ? "" : siteId + "/");
    final String fileType = isZip ? ".zip" : ".json";
    return tempPrefix + compressionId + fileType;
  }

  private DynamicObject getS3TaskObject(final DynamicObject requestBodyObject,
      final String compressionId, final String downloadUrl) {
    final String documentIdsKey = "documentIds";
    final Object documentIds = requestBodyObject.get(documentIdsKey); // Validate whether JSON
    // array?
    final String compressionIdKey = "compressionId";
    final String downloadUrlKey = "downloadUrl";
    return new DynamicObject(Map.ofEntries(entry(documentIdsKey, documentIds),
        entry(compressionIdKey, compressionId), entry(downloadUrlKey, downloadUrl)));
  }

  private String getArchiveDownloadUrl(final S3Service s3, final String stagingBucket,
      final String objectPath) {
    final String zipContentType = "application/zip";
    Duration duration = Duration.ofHours(1);
    PresignGetUrlConfig config = new PresignGetUrlConfig()
        .contentDispositionByPath(objectPath, false).contentType(zipContentType);
    URL url = s3.presignGetUrl(stagingBucket, objectPath, duration, "1", config);
    return url.toString();
  }

  private void putObjectToStaging(final S3Service s3, final String bucket, final String key,
      final String content) {
    final String jsonContentType = "application/json";
    final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    s3.putObject(bucket, key, bytes, jsonContentType);
  }
}
