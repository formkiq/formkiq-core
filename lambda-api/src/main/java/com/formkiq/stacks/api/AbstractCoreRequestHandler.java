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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.services.lambda.AbstractRestApiRequestHandler;
import com.formkiq.aws.services.lambda.ApiAuthorizerType;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.LambdaInputRecord;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.aws.services.lambda.services.DynamoDbCacheServiceExtension;
import com.formkiq.aws.sns.SnsConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.aws.sqs.SqsServiceExtension;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.actions.services.ActionsNotificationService;
import com.formkiq.module.actions.services.ActionsNotificationServiceExtension;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.ocr.DocumentOcrService;
import com.formkiq.module.ocr.DocumentOcrServiceExtension;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginExtension;
import com.formkiq.stacks.api.handler.ConfigsRequestHandler;
import com.formkiq.stacks.api.handler.ConfigurationApiKeysRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdContentRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.DocumentIdUrlRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagValueRequestHandler;
import com.formkiq.stacks.api.handler.DocumentTagsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsKeyRequestHandler;
import com.formkiq.stacks.api.handler.DocumentVersionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsActionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestTagsKeyHandler;
import com.formkiq.stacks.api.handler.DocumentsFulltextRequestTagsKeyValueHandler;
import com.formkiq.stacks.api.handler.DocumentsIdUploadRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOcrRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsOptionsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsSyncsRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsSyncsServiceRequestHandler;
import com.formkiq.stacks.api.handler.DocumentsUploadRequestHandler;
import com.formkiq.stacks.api.handler.EsignatureDocusignConfigRequestHandler;
import com.formkiq.stacks.api.handler.EsignatureDocusignDocumentIdRequestHandler;
import com.formkiq.stacks.api.handler.IndicesFolderMoveRequestHandler;
import com.formkiq.stacks.api.handler.IndicesRequestHandler;
import com.formkiq.stacks.api.handler.IndicesSearchRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeEditRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeNewRequestHandler;
import com.formkiq.stacks.api.handler.OnlyOfficeSaveRequestHandler;
import com.formkiq.stacks.api.handler.PrivateWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.PublicDocumentsRequestHandler;
import com.formkiq.stacks.api.handler.PublicWebhooksRequestHandler;
import com.formkiq.stacks.api.handler.SearchFulltextRequestHandler;
import com.formkiq.stacks.api.handler.SearchRequestHandler;
import com.formkiq.stacks.api.handler.SitesRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasIdRequestHandler;
import com.formkiq.stacks.api.handler.TagSchemasRequestHandler;
import com.formkiq.stacks.api.handler.VersionRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksIdRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksRequestHandler;
import com.formkiq.stacks.api.handler.WebhooksTagsRequestHandler;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.stacks.dynamodb.ApiKeysServiceExtension;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.ConfigServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentCountService;
import com.formkiq.stacks.dynamodb.DocumentCountServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSearchService;
import com.formkiq.stacks.dynamodb.DocumentSearchServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentSyncService;
import com.formkiq.stacks.dynamodb.DocumentSyncServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.IndexProcessorExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

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
    addRequestHandler(new ConfigsRequestHandler());
    addRequestHandler(new ConfigurationApiKeysRequestHandler());
    addRequestHandler(new DocumentVersionsRequestHandler());
    addRequestHandler(new DocumentVersionsKeyRequestHandler());
    addRequestHandler(new DocumentTagsRequestHandler());
    addRequestHandler(new DocumentsActionsRequestHandler());
    addRequestHandler(new DocumentTagValueRequestHandler());
    addRequestHandler(new DocumentTagRequestHandler());
    addRequestHandler(new DocumentIdUrlRequestHandler());
    addRequestHandler(new DocumentIdContentRequestHandler());
    addRequestHandler(new SearchRequestHandler());
    addRequestHandler(new SearchFulltextRequestHandler());
    addRequestHandler(new DocumentsFulltextRequestTagsKeyHandler());
    addRequestHandler(new DocumentsFulltextRequestTagsKeyValueHandler());
    addRequestHandler(new DocumentsUploadRequestHandler());
    addRequestHandler(new DocumentsIdUploadRequestHandler());
    addRequestHandler(new DocumentsOcrRequestHandler());
    addRequestHandler(new DocumentsSyncsRequestHandler());
    addRequestHandler(new DocumentsSyncsServiceRequestHandler());
    addRequestHandler(new DocumentsFulltextRequestHandler());
    addRequestHandler(new TagSchemasRequestHandler());
    addRequestHandler(new TagSchemasIdRequestHandler());
    addRequestHandler(new WebhooksTagsRequestHandler());
    addRequestHandler(new WebhooksIdRequestHandler());
    addRequestHandler(new WebhooksRequestHandler());
    addRequestHandler(new DocumentsRequestHandler());
    addRequestHandler(new DocumentIdRequestHandler());
    addRequestHandler(new OnlyOfficeNewRequestHandler());
    addRequestHandler(new OnlyOfficeSaveRequestHandler());
    addRequestHandler(new OnlyOfficeEditRequestHandler());
    addRequestHandler(new IndicesFolderMoveRequestHandler());
    addRequestHandler(new IndicesRequestHandler());
    addRequestHandler(new IndicesSearchRequestHandler());
    addRequestHandler(new EsignatureDocusignDocumentIdRequestHandler());
    addRequestHandler(new EsignatureDocusignConfigRequestHandler());
  }

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param region {@link Region}
   * @param credentialsProvider {@link AwsCredentialsProvider}
   * @param awsServiceEndpoints {@link Map} {@link URI}
   * @param schemaEvents {@link DocumentTagSchemaPlugin}
   */
  public static void configureHandler(final Map<String, String> map, final Region region,
      final AwsCredentialsProvider credentialsProvider, final Map<String, URI> awsServiceEndpoints,
      final DocumentTagSchemaPlugin schemaEvents) {

    final boolean enableAwsXray = "true".equals(map.get("ENABLE_AWS_X_RAY"));

    DynamoDbConnectionBuilder db = new DynamoDbConnectionBuilder(enableAwsXray).setRegion(region)
        .setCredentials(credentialsProvider)
        .setEndpointOverride(awsServiceEndpoints.get("dynamodb"));
    AwsServiceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(db));

    SsmConnectionBuilder ssm = new SsmConnectionBuilder(enableAwsXray).setRegion(region)
        .setCredentials(credentialsProvider).setEndpointOverride(awsServiceEndpoints.get("ssm"));
    AwsServiceCache.register(SsmConnectionBuilder.class,
        new ClassServiceExtension<SsmConnectionBuilder>(ssm));

    SqsConnectionBuilder sqs = new SqsConnectionBuilder(enableAwsXray).setRegion(region)
        .setCredentials(credentialsProvider).setEndpointOverride(awsServiceEndpoints.get("sqs"));
    AwsServiceCache.register(SqsConnectionBuilder.class,
        new ClassServiceExtension<SqsConnectionBuilder>(sqs));

    AwsServiceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());

    if (credentialsProvider != null) {
      AwsServiceCache.register(AwsCredentials.class,
          new ClassServiceExtension<>(credentialsProvider.resolveCredentials()));
    }

    SnsConnectionBuilder sns = new SnsConnectionBuilder(enableAwsXray).setRegion(region)
        .setCredentials(credentialsProvider).setEndpointOverride(awsServiceEndpoints.get("sns"));

    AwsServiceCache.register(ActionsNotificationService.class,
        new ActionsNotificationServiceExtension(sns));

    S3ConnectionBuilder s3 = new S3ConnectionBuilder(enableAwsXray).setRegion(region)
        .setCredentials(credentialsProvider).setEndpointOverride(awsServiceEndpoints.get("s3"));

    AwsServiceCache.register(ActionsService.class, new ActionsServiceExtension());
    AwsServiceCache.register(SsmService.class, new SsmServiceExtension());
    AwsServiceCache.register(S3Service.class, new S3ServiceExtension(s3));
    AwsServiceCache.register(SqsService.class, new SqsServiceExtension());
    AwsServiceCache.register(DocumentTagSchemaPlugin.class,
        new DocumentTagSchemaPluginExtension(schemaEvents));
    AwsServiceCache.register(CacheService.class, new DynamoDbCacheServiceExtension());
    AwsServiceCache.register(DocumentService.class, new DocumentServiceExtension());
    AwsServiceCache.register(DocumentSearchService.class, new DocumentSearchServiceExtension());
    AwsServiceCache.register(DocumentCountService.class, new DocumentCountServiceExtension());
    AwsServiceCache.register(FolderIndexProcessor.class, new IndexProcessorExtension());
    AwsServiceCache.register(ConfigService.class, new ConfigServiceExtension());
    AwsServiceCache.register(ApiKeysService.class, new ApiKeysServiceExtension());
    AwsServiceCache.register(DocumentSyncService.class, new DocumentSyncServiceExtension());
    AwsServiceCache.register(DocumentOcrService.class, new DocumentOcrServiceExtension());

    awsServices = new CoreAwsServiceCache().environment(map).debug("true".equals(map.get("DEBUG")));

    isEnablePublicUrls = isEnablePublicUrls(map);

    setAuthorizerType(ApiAuthorizerType.valueOf(map.get("USER_AUTHENTICATION").toUpperCase()));
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
