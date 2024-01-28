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
package com.formkiq.module.lambda.ocr.handlers;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3PresignerService;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.module.lambdaservices.AwsServiceCache;

/** {@link ApiGatewayRequestHandler} for "/objects/examine/pdf". */
public class ObjectExaminePdfHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link ObjectExaminePdfHandler} URL. */
  public static final String URL = "/objects/examine/pdf";

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    S3PresignerService service = awsservice.getExtension(S3PresignerService.class);

    String siteId = authorization.getSiteId();
    String id = UUID.randomUUID().toString();

    String bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");
    String s3key = String.format("tempfiles/%s", createS3Key(siteId, id));

    URL url = service.presignPutUrl(bucket, s3key, Duration.ofDays(1), Optional.empty(), null);

    Map<String, Object> map = new HashMap<>();
    map.put("id", id);
    map.put("uploadUrl", url);

    ApiMapResponse resp = new ApiMapResponse(map);
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }
}
