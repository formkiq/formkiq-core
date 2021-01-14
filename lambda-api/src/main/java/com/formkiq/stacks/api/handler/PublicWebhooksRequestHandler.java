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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.common.objects.DynamicObject;
import software.amazon.awssdk.services.s3.S3Client;

/** {@link ApiGatewayRequestHandler} for "/public/webhooks". */
public class PublicWebhooksRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_DOC_EXT = ".fkb64";

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    final String siteId = getSiteId(event);
    final String documentId = UUID.randomUUID().toString();
    
    DynamicObject item = new DynamicObject(new HashMap<>());
    
    String contentType = getContentType(event);
    if (contentType != null) {
      item.put("contentType", contentType);
    }
    
    String webhookId = getPathParameter(event, "webhooks");
    DynamicObject hook = awsservice.webhookService().findWebhook(siteId, webhookId);
    
    if (hook == null) {
      throw new BadException("invalid webhook url");
    }
    
    String body = getBodyAsString(event);
    item.put("content", body);
    item.put("documentId", documentId);
    item.put("userId", "webhook");
    item.put("path", "webhooks/" + webhookId);
    
    if (siteId != null) {
      item.put("siteId", siteId);
    }

    putObjectToStaging(logger, awsservice, item, siteId);

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMapResponse(Map.of("documentId", documentId)));
  }

  /**
   * Put Object to Staging Bucket.
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param item {@link DynamicObject}
   * @param siteId {@link String}
   */
  private void putObjectToStaging(final LambdaLogger logger, final AwsServiceCache awsservice,
      final DynamicObject item, final String siteId) {
    
    String s = GSON.toJson(item);

    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

    String key = createDatabaseKey(siteId, item.getString("documentId") + FORMKIQ_DOC_EXT);
    logger.log("s3 putObject " + key + " into bucket " + awsservice.stages3bucket());

    S3Service s3 = awsservice.s3Service();
    try (S3Client client = s3.buildClient()) {
      s3.putObject(client, awsservice.stages3bucket(), key, bytes, item.getString("contentType"));
    }
  }

  @Override
  public String getRequestUrl() {
    return "/public/webhooks";
  }
}
