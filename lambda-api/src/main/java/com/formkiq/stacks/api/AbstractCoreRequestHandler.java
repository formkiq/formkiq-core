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
package com.formkiq.stacks.api;

import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.services.lambda.AbstractRestApiRequestHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.AwsServiceCache;
import com.formkiq.aws.services.lambda.LambdaInputRecord;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.stacks.api.handler.DocumentIdContentRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdUrlRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagValueRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsIdUploadRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOcrRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOptionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsUploadRequestHandler;
import com.formkiq.stacks.api.handler.PrivateWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.PublicDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.PublicWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasIdRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksIdRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksTagsRequestHandler;

/**
 * 
 * {@link AbstractCoreRequestHandler}.
 *
 */
public abstract class AbstractCoreRequestHandler extends AbstractRestApiRequestHandler {

  /** Is Public Urls Enabled. */
  private static boolean isEnablePublicUrls;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
  /** Url Class Map. */
  private static final Map<String, ApiGatewayRequestHandler> URL_MAP = new HashMap<>();

  /**
   * Add Url Request Handler Mapping.
   * 
   * @param handler {@link ApiGatewayRequestHandler}
   */
  public static void addRequestHandler(final ApiGatewayRequestHandler handler) {
    URL_MAP.put(handler.getRequestUrl(), handler);
  }

  /**
   * Build Core UrlMap.
   */
  public static void buildUrlMap() {
    URL_MAP.put("options", new DocumentsOptionsRequestHandler());
    addRequestHandler(new VersionRequestHandler());
    addRequestHandler(new SitesRequestHandler());
    addRequestHandler(new DocumentsRequestHandler());
    addRequestHandler(new DocumentIdRequestHandler());
    addRequestHandler(new DocumentVersionsRequestHandler());
    addRequestHandler(new DocumentTagsRequestHandler());
    addRequestHandler(new DocumentTagValueRequestHandler());
    addRequestHandler(new DocumentTagRequestHandler());
    addRequestHandler(new DocumentIdUrlRequestHandler());
    addRequestHandler(new DocumentIdContentRequestHandler());
    addRequestHandler(new SearchRequestHandler());
    addRequestHandler(new DocumentsUploadRequestHandler());
    addRequestHandler(new DocumentsIdUploadRequestHandler());
    addRequestHandler(new DocumentsOcrRequestHandler());
    addRequestHandler(new TagSchemasRequestHandler());
    addRequestHandler(new TagSchemasIdRequestHandler());
    addRequestHandler(new WebhooksTagsRequestHandler());
    addRequestHandler(new WebhooksIdRequestHandler());
    addRequestHandler(new WebhooksRequestHandler());
  }

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sqs {@link SqsConnectionBuilder}
   * @param schemaEvents {@link DocumentTagSchemaPlugin}
   */
  public static void configureHandler(final Map<String, String> map,
      final DynamoDbConnectionBuilder builder, final S3ConnectionBuilder s3,
      final SsmConnectionBuilder ssm, final SqsConnectionBuilder sqs,
      final DocumentTagSchemaPlugin schemaEvents) {

    awsServices = new CoreAwsServiceCache().environment(map).dbConnection(builder).s3Connection(s3)
        .sqsConnection(sqs).ssmConnection(ssm).debug("true".equals(map.get("DEBUG")))
        .documentTagSchemaPlugin(schemaEvents);

    awsServices.init();

    isEnablePublicUrls = isEnablePublicUrls(map);
  }

  /**
   * Whether to enable public urls.
   * 
   * @param map {@link Map}
   * @return boolean
   */
  private static boolean isEnablePublicUrls(final Map<String, String> map) {
    return "true".equals(map.getOrDefault("ENABLE_PUBLIC_URLS", "false"));
  }

  /** constructor. */
  public AbstractCoreRequestHandler() {}

  @Override
  @SuppressWarnings("returncount")
  public ApiGatewayRequestHandler findRequestHandler(
      final Map<String, ApiGatewayRequestHandler> urlMap, final String method,
      final String resource) throws NotFoundException {

    String s = "options".equals(method) ? method : resource;

    if (isEnablePublicUrls && "/public/documents".equals(s)) {
      return new PublicDocumentsRequestHandler();
    }

    if (s.startsWith("/public/webhooks")) {
      return new PublicWebhooksRequestHandler();
    }

    if (s.startsWith("/private/webhooks")) {
      return new PrivateWebhooksRequestHandler();
    }

    ApiGatewayRequestHandler hander = URL_MAP.get(s);
    if (hander != null) {
      return hander;
    }

    throw new NotFoundException(resource + " not found");
  }

  @Override
  public AwsServiceCache getAwsServices() {
    return awsServices;
  }

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public static AwsServiceCache getAwsServicesCache() {
    return awsServices;
  }

  @Override
  public Map<String, ApiGatewayRequestHandler> getUrlMap() {
    return URL_MAP;
  }

  @Override
  public void handleSqsRequest(final LambdaLogger logger, final AwsServiceCache services,
      final LambdaInputRecord record) {
    // empty
  }
}
